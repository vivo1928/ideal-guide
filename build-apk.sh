#!/usr/bin/env bash
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-/usr/lib/android-sdk}"
ANDROID_JAR="$ANDROID_HOME/platforms/android-23/android.jar"

# 自动探测 build-tools 目录：优先 Debian 布局，否则取最高版本目录
if [ -d "$ANDROID_HOME/build-tools/debian" ]; then
  BUILD_TOOLS="$ANDROID_HOME/build-tools/debian"
else
  BUILD_TOOLS="$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -n 1)"
fi
if [ -z "$BUILD_TOOLS" ] || [ ! -d "$BUILD_TOOLS" ]; then
  echo "ERROR: no Android build-tools found under $ANDROID_HOME/build-tools" >&2
  exit 1
fi

AAPT="$BUILD_TOOLS/aapt"
ZIPALIGN="$(command -v zipalign || echo "$BUILD_TOOLS/zipalign")"
APKSIGNER="$(command -v apksigner || echo "$BUILD_TOOLS/apksigner")"

# dx 在新版 build-tools 中已被移除，存在时用 dx，否则回退到 d8
if [ -x "$BUILD_TOOLS/dx" ]; then
  DX="$BUILD_TOOLS/dx"
else
  DX="$BUILD_TOOLS/d8"
fi

rm -rf build/gen build/classes build/out
mkdir -p build/gen build/classes build/out

"$AAPT" package -f -m \
  -J build/gen \
  -M app/src/main/AndroidManifest.xml \
  -S app/src/main/res \
  -I "$ANDROID_JAR"

javac -source 1.8 -target 1.8 -Xlint:-options \
  -bootclasspath "$ANDROID_JAR" \
  -classpath "$ANDROID_JAR" \
  -d build/classes \
  build/gen/com/trae/domesticmusic/R.java \
  app/src/main/java/com/trae/domesticmusic/MainActivity.java

if [ "$(basename "$DX")" = "d8" ]; then
  CLASSES=$(find build/classes -name '*.class')
  "$DX" --release --min-api 23 --output build/out/ $CLASSES
else
  "$DX" --dex --output=build/out/classes.dex build/classes
fi

"$AAPT" package -f \
  -M app/src/main/AndroidManifest.xml \
  -S app/src/main/res \
  -A app/src/main/assets \
  -I "$ANDROID_JAR" \
  -F build/out/app-unsigned.apk

cp build/out/classes.dex build/classes.dex
(cd build && zip -q out/app-unsigned.apk classes.dex)

if [ ! -f build/debug.keystore ]; then
  keytool -genkeypair -v \
    -keystore build/debug.keystore \
    -storepass android \
    -keypass android \
    -alias domesticmusic \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Domestic Music Source, OU=TRAE, O=TRAE, L=Local, S=Local, C=CN" >/dev/null
fi

"$ZIPALIGN" -f 4 build/out/app-unsigned.apk build/out/domestic-music-source-aligned.apk

"$APKSIGNER" sign \
  --ks build/debug.keystore \
  --ks-key-alias domesticmusic \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out build/out/domestic-music-source.apk \
  build/out/domestic-music-source-aligned.apk

"$APKSIGNER" verify --verbose build/out/domestic-music-source.apk

OUT_DIR="${OUT_DIR:-build/out}"
mkdir -p "$OUT_DIR"
cp build/out/domestic-music-source.apk "$OUT_DIR/domestic-music-source-v2.0.3.apk"
echo "APK built successfully: $OUT_DIR/domestic-music-source-v2.0.3.apk"
