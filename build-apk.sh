#!/usr/bin/env bash
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-/usr/lib/android-sdk}"
ANDROID_JAR="$ANDROID_HOME/platforms/android-23/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/debian"
AAPT="$BUILD_TOOLS/aapt"
DX="$BUILD_TOOLS/dx"
ZIPALIGN="zipalign"
APKSIGNER="apksigner"

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

"$DX" --dex --output=build/out/classes.dex build/classes

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

cp build/out/domestic-music-source.apk /workspace/domestic-music-source-v2.0.3.apk
echo "APK built successfully: /workspace/domestic-music-source-v2.0.3.apk"
