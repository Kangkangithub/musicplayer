# 音乐播放器 APP — 项目说明文档

## 一、项目基本信息

| 项目 | 内容 |
|------|------|
| 项目名称 | allnewmusicplayer |
| 包名 | com.example.musicplayer |
| 开发语言 | Java + Kotlin |
| 最低 SDK | API 31 (Android 12) |
| 目标 SDK | API 36 |
| 编译 SDK | 36 |
| 构建工具 | Gradle (Kotlin DSL) |
| 依赖框架 | AndroidX, AppCompat |

---

## 二、项目结构
`
allnewmusicplayer/
├── app/
│   ├── build.gradle.kts              # 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml        # 应用清单
│       ├── assets/
│       │   └── lyrics/                # 歌词文件(.lrc)
│       │       ├── example.lrc
│       │       ├── lost_soul.lrc
│       │       └── you_rock_my_world.lrc
│       ├── java/com/example/musicplayer/
│       │   ├── MainActivity.java      # 主Activity
│       │   ├── MusicFragment.java     # 音乐列表Fragment
│       │   ├── VideoFragment.java     # 视频列表Fragment
│       │   ├── MusicService.java      # 音乐播放Service
│       │   ├── Song.java              # 歌曲数据模型
│       │   ├── MetadataHelper.java    # 元数据提取工具
│       │   └── ui/theme/              # Compose主题
│       ├── res/
│       │   ├── drawable/              # 图片资源
│       │   │   ├── btn_bg.png
│       │   │   ├── btn_bg_selector.xml
│       │   │   ├── btn_info_bg.xml
│       │   │   ├── img_music.png
│       │   │   ├── music_bg.png
│       │   │   └── title_bg.png
│       │   ├── layout/
│       │   │   ├── activity_main.xml  # 主界面布局
│       │   │   ├── fragment_music.xml # 音乐列表布局
│       │   │   ├── fragment_video.xml # 视频布局
│       │   │   └── list_item.xml      # 列表项布局
│       │   ├── raw/                   # 本地音视频
│       │   │   ├── example.mp3
│       │   │   ├── lost_soul.mp3
│       │   │   ├── you_rock_my_world.mp3
│       │   │   ├── video_4awpkills.mp4
│       │   │   ├── video_4kills.mp4
│       │   │   └── video_car_sim.mp4
│       │   └── values/
├── build.gradle.kts                   # 项目级构建
├── gradle.properties
└── settings.gradle.kts

---

## 三、实验要求对应说明

### 1. Fragment 碎片动态使用

> 实验要求: 在 Activity 中通过 FragmentManager 动态添加/替换 Fragment

**实现位置**: MainActivity.java - initFragments() 和 onClick()

- 获取 FragmentManager，开启 FragmentTransaction
- 	ransaction.add(R.id.fragment_container, musicFragment) 添加 Fragment 到 FrameLayout 容器
- 导航切换时使用 	ransaction.replace() 在音乐列表和视频列表之间切换

`java
// 初始化 - 添加音乐Fragment
FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
transaction.add(R.id.fragment_container, musicFragment);
transaction.commit();

// 切换 - 替换Fragment
FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
ft.replace(R.id.fragment_container, videoFragment);
ft.commit();
`

涉及文件:
- MainActivity.java - Fragment生命周期管理
- MusicFragment.java - 音乐列表Fragment
- VideoFragment.java - 视频列表Fragment
- ctivity_main.xml - FrameLayout容器: @+id/fragment_container


---

### 2. ListView + BaseAdapter 数据适配器

> 实验要求: ListView 列表展示数据 + 自定义 BaseAdapter

**实现位置**: MusicFragment.java 和 VideoFragment.java

每个 Fragment 内部定义了继承 BaseAdapter 的适配器类，重写 4 个核心方法:

| 方法 | 作用 |
|------|------|
| getCount() | 返回数据源条目数 |
| getItem(position) | 返回指定位置的数据对象 |
| getItemId(position) | 返回条目ID |
| getView(position, convertView, parent) | 创建/复用列表项视图，绑定数据 |

**MusicFragment.MyMusicAdapter** 关键代码:
`java
class MyMusicAdapter extends BaseAdapter {
    @Override
    public int getCount() { return songList.size(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // convertView 复用机制 - 性能优化
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.list_item, parent, false);
        }
        // 绑定数据: 歌曲名、播放状态、Info按钮
        TextView tvName = convertView.findViewById(R.id.tv_item_name);
        tvName.setText(songList.get(position).title);
        // 当前播放歌曲金色高亮
        return convertView;
    }
}
`

列表项布局 list_item.xml 包含: 播放指示图标 + 歌曲名称 + Info按钮

---

### 3. VideoView 视频播放

> 实验要求: 创建视频列表, VideoView 播放用户选择的视频文件

**实现位置**: VideoFragment.java

核心流程:
`java
// 1. 绑定 MediaController(自带播放/暂停/进度条)
MediaController mediaController = new MediaController(getContext());
mediaController.setAnchorView(videoView);
videoView.setMediaController(mediaController);

// 2. 反射扫描 res/raw 中以 "video_" 开头的文件
Field[] fields = R.raw.class.getFields();

// 3. 构建资源URI -> VideoView播放
Uri videoUri = Uri.parse("android.resource://"
    + requireContext().getPackageName() + "/" + resId);
videoView.setVideoURI(videoUri);
videoView.start();
`

涉及文件:
- VideoFragment.java - 视频扫描、列表绑定、播放控制
- ragment_video.xml - VideoView + ListView 布局


---

### 4. MediaPlayer + Service 后台音乐播放

> 实验要求: 通过 Service 的 IBinder 与 Activity 通信，使用 MediaPlayer 播放音频

#### 4.1 Service 端 (MusicService.java)

`java
public class MusicService extends Service {
    private MediaPlayer mediaPlayer;

    @Override public void onCreate() {
        mediaPlayer = new MediaPlayer();
    }

    // 自定义 Binder - 暴露控制接口给 Activity
    public class MusicControl extends Binder {
        public void playLocal(int resId) {
            // 从 res/raw 读取音频文件
            AssetFileDescriptor afd = getResources().openRawResourceFd(resId);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        public void pause()   { mediaPlayer.pause(); }
        public void resume()  { mediaPlayer.start(); }
        public void seekTo(int progress) { mediaPlayer.seekTo(progress); }
        public int  getDuration()        { return mediaPlayer.getDuration(); }
        public int  getCurrentPosition() { return mediaPlayer.getCurrentPosition(); }
        public boolean isPlaying()       { return mediaPlayer.isPlaying(); }
    }

    @Override public IBinder onBind(Intent intent) {
        return new MusicControl();  // 返回 Binder 给调用方
    }
}
`

#### 4.2 Activity 端 (MainActivity.java)

`java
// 1. 绑定 Service
Intent intent = new Intent(this, MusicService.class);
bindService(intent, connection, BIND_AUTO_CREATE);

// 2. ServiceConnection 获取 IBinder 引用
private ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        musicControl = (MusicService.MusicControl) service;
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {}
};

// 3. 通过 musicControl 操作 MediaPlayer
musicControl.playLocal(resId);    // 播放
musicControl.pause();             // 暂停
musicControl.seekTo(progress);    // 拖动进度条
`

#### 4.3 AndroidManifest.xml 注册

`xml
<service android:name=".MusicService" />
`

#### 4.4 进度条更新机制

使用 Handler + 子线程每 500ms 轮询播放进度:
`java
new Thread(() -> {
    while (!isUnbind) {
        Thread.sleep(500);
        handler.sendEmptyMessage(1);  // 通知UI更新进度条和时间
    }
}).start();
`


---

## 四、扩展功能

| 功能 | 实现方式 | 关键文件 |
|------|----------|----------|
| 播放列表队列 | 点击歌曲自动构建队列，上一首/下一首切换，播完自动切歌 | MainActivity.java (playNext/playPrev) |
| 歌词同步显示 | 点击旋转专辑图标弹出半透明浮层，LRC格式时间轴滚动 | MainActivity.java (lyrics模块) |
| 歌曲详情查看 | 列表项右侧 Info 按钮 -> MediaMetadataRetriever 提取 ID3 标签 | MusicFragment.java + MetadataHelper.java |
| 视频界面美化 | 复用项目 drawable 素材统一视觉风格 | fragment_video.xml |
| 正在播放高亮 | 当前歌曲金色文字 + 指示图标 | MusicFragment.java (getView) |

---

## 五、关键技术点速查

| 技术点 | 所在文件 |
|--------|----------|
| Fragment add/replace | MainActivity.java |
| Service Binder 通信 | MusicService.java (MusicControl) |
| bindService 绑定 | MainActivity.java |
| ListView + BaseAdapter | MusicFragment.java / VideoFragment.java |
| VideoView + MediaController | VideoFragment.java |
| MediaPlayer 本地播放 | MusicService.java (playLocal) |
| MediaMetadataRetriever | MetadataHelper.java |
| LRC 歌词解析 | MainActivity.java (parseLyrics) |
| ObjectAnimator 旋转动画 | MainActivity.java (initAnimator) |
| 反射扫描 res/raw | MusicFragment.java (scanRawMusicFiles) |
| SeekBar 进度控制 | MainActivity.java |
| Handler 定时更新UI | MainActivity.java |

---

## 六、演示流程建议

1. 展示"音乐列表" -> 点击歌曲播放 -> 展示底部控制台(暂停/继续/进度/上一首/下一首)
2. 点击旋转专辑图标 -> 展示歌词浮层(时间轴同步滚动) -> 再次点击关闭
3. 点击列表项右侧 **Info 按钮** -> 展示歌曲详情(艺术家/专辑/流派/时长)
4. 返回音乐列表 -> 点击其他歌曲 -> 演示播放队列自动切换
5. 切换到"视频列表" -> 点击视频 -> 展示 VideoView + MediaController 播放
6. 可选: 展示代码中 Fragment 动态切换、Service Binder 通信等核心实现
