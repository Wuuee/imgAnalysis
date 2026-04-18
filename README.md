# Image Focal Analyzer (Android)

这个 Android App 用于扫描系统相册里的照片 EXIF 信息，统计不同焦段（优先使用 35mm 等效焦段）出现频率。

## 功能

- 读取相册图片元数据（不上传照片内容）
- 提取 `FocalLengthIn35mmFilm` 或 `FocalLength`
- 按焦段分组并展示频次

## 本地构建

安装 Android SDK 后执行：

```bash
.\gradlew.bat assembleDebug
```

## GitHub Actions

提交后会自动执行 `.github/workflows/android-build.yml`，构建 `debug` APK。

