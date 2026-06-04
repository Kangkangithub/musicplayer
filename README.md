# allnewmusicplayer

Android 音乐播放器 APP，基于 Fragment + ListView + VideoView + MediaPlayer(Service) 架构，支持本地音乐/视频播放、播放列表队列、LRC 歌词同步显示、歌曲元数据查看。

## Tech Stack

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

## Features

- 音乐列表播放 — ListView + 自定义 BaseAdapter
- 视频列表播放 — VideoView + MediaController
- 后台音乐服务 — MediaPlayer + Service Binder 通信
- Fragment 动态切换 — 音乐/视频页面 FragmentManager 导航
- 播放列表队列 — 上一首/下一首，播完自动切歌
- 歌词同步显示 — 点击专辑图标弹出半透明浮层，LRC 时间轴滚动
- 歌曲详情查看 — MediaMetadataRetriever 提取 ID3 标签

## Prerequisites

- Android Studio (Ladybug 2024.2+)
- JDK 11+
- Android SDK 36 (API 36)
- Min SDK 31 (Android 12)

## Getting Started

```bash
# Clone
git clone https://github.com/Kangkangithub/musicplayer.git
cd allnewmusicplayer

# Build
./gradlew assembleDebug

# Run: Open in Android Studio and run on device/emulator (API >= 31)
```

## Project Structure

```
app/src/main/
├── java/com/example/musicplayer/
│   ├── MainActivity.java         # 主Activity(控制台/播放列表/歌词/进度)
│   ├── MusicFragment.java         # 音乐列表Fragment
│   ├── VideoFragment.java         # 视频列表Fragment
│   ├── MusicService.java          # 音乐播放Service(MediaPlayer + Binder)
│   ├── Song.java                  # 歌曲数据模型
│   └── MetadataHelper.java        # 元数据提取(MediaMetadataRetriever)
├── res/
│   ├── layout/                    # activity_main, fragment_music, fragment_video, list_item
│   ├── drawable/                  # 按钮背景、专辑图、标题栏背景
│   ├── raw/                       # 本地音视频(ex: example.mp3, video_*.mp4)
│   └── values/                    # colors, strings, themes
├── assets/
│   └── lyrics/                    # LRC歌词文件
└── AndroidManifest.xml
```

## Architecture

| 组件 | 技术 | 说明 |
|------|------|------|
| UI 框架 | Fragment + FragmentManager | 音乐/视频页面动态切换 |
| 列表 | ListView + BaseAdapter | 复用 convertView 优化性能 |
| 视频播放 | VideoView + MediaController | 内置控制条(播放/暂停/进度) |
| 音频播放 | MediaPlayer + Service | Binder IPC 通信 |
| 进度更新 | Handler + Thread | 500ms 轮询更新 SeekBar |
| 数据扫描 | Java 反射 | 自动扫描 R.raw 资源 |
| 元数据 | MediaMetadataRetriever | 提取 MP3 ID3 标签 |

## License

MIT