# Image Focal Analyzer (Android)

这个 Android App 用于扫描系统相册里的照片 EXIF 信息，统计不同焦段（优先使用 35mm 等效焦段）出现频率。

## 功能

- 读取相册图片元数据（不上传照片内容）
- 提取 `FocalLengthIn35mmFilm` 或 `FocalLength`
- 默认展示前 5 个最常用焦段，可展开查看可滚动的缩略图网格（展示该焦段全部照片）
- 其余焦段自动归为“其他”并默认折叠，展开后可看到“其他”内具体焦段列表
- 提供焦段占比饼状图

## 本地构建

安装 Android SDK 后执行：

```bash
.\gradlew.bat assembleDebug
```

## GitHub Actions

提交后会自动执行 `.github/workflows/android-build.yml`，构建 `debug` APK。

