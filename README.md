# ImageFocalAnalyzer

![Build](https://github.com/Wuuee/imgAnalysis/actions/workflows/android-build.yml/badge.svg)
![Platform](https://img.shields.io/badge/platform-Android-green)
![License](https://img.shields.io/badge/license-MIT-blue)

**ImageFocalAnalyzer** 是一款 Android 应用，用于扫描本地相册的照片 EXIF 信息，统计并可视化您最常使用的焦段分布——帮助摄影师了解自己的拍摄习惯。

> 所有分析均在本地完成，照片内容不会上传或离开设备。

## 功能

- 📷 读取相册图片元数据（不上传照片内容）
- 🔍 优先提取 `FocalLengthIn35mmFilm`，回退到 `FocalLength`
- 📊 焦段占比饼状图，直观展示拍摄习惯
- 🏆 默认展示前 5 个最常用焦段，支持展开查看可滚动的缩略图网格（该焦段全部照片）
- 📁 其余焦段自动归为“其他”并默认折叠，展开后可查看具体焦段列表

## 本地构建

安装 Android SDK 后执行：

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/`。

## GitHub Actions

提交后会自动执行 `.github/workflows/android-build.yml`，构建 `debug` APK 并上传为 Artifact。
