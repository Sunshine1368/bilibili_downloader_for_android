# 哔哩哔哩视频下载器 (BiliDownloader)

![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen.svg)
![Java](https://img.shields.io/badge/Language-Java-orange.svg)
![Target SDK](https://img.shields.io/badge/Target_SDK-36-blue.svg)

BiliDownloader 是一个基于 **Java** 原生开发的轻量级 Android B站视频/音频/弹幕下载工具。该项目采用 Material Design 3 设计风格，支持从 Bilibili 解析并提取最高画质的音视频流。

##  核心功能

-  **内置扫码/密码登录**：通过 WebView 登录 B 站，自动提取并持久化保存 Cookie（`SESSDATA`），支持解析需要高权限（1080P/大会员）的视频。
-  **视频下载**：通过 BV 号一键解析视频，提取 DASH 流直接下载原画质视频（MP4）。
-  **音频提取与转码**：内置 `FFmpegKit` 引擎，支持将 B 站音频流下载并直接在手机端本地无损转码为 **MP3 / AAC / WAV** 格式。
-  **弹幕下载**：一键拉取视频弹幕，并保存为标准的 `.xml` 格式文件。
-  **免权限文件存储**：全面适配 Android 11+ 现代存储规范（Scoped Storage），使用 `MediaStore` API 将文件无缝保存至系统公共目录（`Movies`、`Music`、`Download`），**无需申请任何存储权限**。
-  **后台前台服务**：采用 `Foreground Service` 进行后台静默下载，并在通知栏实时显示多任务下载进度。

## 🛠 技术栈与依赖库

本项目采用纯 **Groovy DSL** 进行构建，未使用 Kotlin。

*   **网络请求**：[Retrofit2](https://github.com/square/retrofit) + [OkHttp3](https://github.com/square/okhttp)
*   **JSON 解析**：[Gson](https://github.com/google/gson)
*   **音视频处理**：[FFmpegKit Full](https://github.com/arthenica/ffmpeg-kit) (用于移动端的强大 FFmpeg 跨平台库)
*   **UI 组件**：Material Components for Android + RecyclerView

##  编译与运行指南

### 环境要求
*   **IDE**: IntelliJ IDEA 或 Android Studio
*   **Java 版本**: Java 17
*   **Android SDK**: 最低 `API 24` (Android 7.0)，目标 `API 36` (Android 15)

### 快速启动
1. 克隆或下载本仓库代码。
2. 使用 IntelliJ IDEA 或 Android Studio 打开项目根目录。
3. 等待 Gradle 同步完成（如遇依赖下载失败，请检查网络或配置国内镜像源）。
5. 点击 IDE 右上角的 **Run ('app')** 编译并安装到手机或模拟器。

##  目录结构说明
```text
BiliDownloader/
├── app/src/main/java/uno/toolkit/bilibilidownloader/
│   ├── MainActivity.java        # 主界面（处理解析逻辑和UI展示）
│   ├── LoginWebViewActivity.java# 登录页面（WebView拦截Cookie）
│   ├── DownloadService.java     # 前台下载服务（包含 OkHttp 下载与 FFmpeg 转码）
│   ├── RetrofitClient.java      # 网络请求客户端封装
│   ├── BiliApi.java             # B站 API 接口定义
│   └── Utils.java               # 存储与 Cookie 工具类
├── get_icon.py                  # 一键抓取 B 站图标的 Python 脚本
├── build.gradle                 # 项目级 Gradle 配置 (Groovy)
└── settings.gradle              # 仓库与模块配置 (Groovy)
```
##  文件保存路径参考
文件下载完成后，请前往系统自带的文件管理器中查看：
视频 (.mp4) ➔ 内部存储/Movies/BiliDownloader/
音频 (.mp3/.aac/.wav) ➔ 内部存储/Music/BiliDownloader/
弹幕 (.xml) ➔ 内部存储/Download/BiliDownloader/
