# 国内聚合播放器 Android APK 项目

这是把前面整理出的聚合音源逻辑制作成可使用播放器 APK 的原生 Android WebView 项目。

## APK 信息

- 应用名：国内聚合播放器
- 包名：`com.trae.domesticmusic`
- 版本：`1.1.0`
- 最低 Android：6.0，API 23
- 主要功能：输入平台、歌曲 ID/hash/songmid、音质，解析真实播放链接并用内置播放器播放。

## 主要文件

- `app/src/main/AndroidManifest.xml`：Android 应用声明。
- `app/src/main/java/com/trae/domesticmusic/MainActivity.java`：WebView 入口和原生音源解析器。
- `app/src/main/assets/`：播放器页面和修复版插件文件。
- `build/out/domestic-music-source.apk`：构建出的签名 APK。

## 重新构建

当前项目使用系统 Android SDK 工具链手动构建，不依赖 Gradle 插件。

```bash
./build-apk.sh
```

构建完成后，APK 位于：

```text
build/out/domestic-music-source.apk
```

## 使用方式

安装 APK 后打开应用：

1. 选择音乐平台。
2. 选择音质。
3. 输入该平台对应的歌曲 ID、hash 或 songmid。
4. 点击“解析并播放”。

注意：不同平台的歌曲 ID 格式不同，部分歌曲可能受版权、接口或源站可用性影响而无法播放。
