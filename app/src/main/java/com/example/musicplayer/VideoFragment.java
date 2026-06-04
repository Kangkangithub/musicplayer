package com.example.musicplayer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class VideoFragment extends Fragment {
    private VideoView videoView;
    private ListView lvVideo;

    // 存储扫描到的本地视频名称和资源 ID
    private ArrayList<String> localVideoNames = new ArrayList<>();
    private ArrayList<Integer> localVideoResIds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        videoView = view.findViewById(R.id.video_view);
        lvVideo = view.findViewById(R.id.lv_video);

        // 1. 为视频播放器添加自带的控制条（播放、暂停、进度条）
        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // 2. 动态扫描本地 res/raw 文件夹中的视频
        scanRawVideoFiles();

        // 3. 绑定适配器展示列表
        MyVideoAdapter adapter = new MyVideoAdapter();
        lvVideo.setAdapter(adapter);

        // 4. 点击列表条目进行本地视频播放
        lvVideo.setOnItemClickListener((parent, view1, position, id) -> {
            int resId = localVideoResIds.get(position);
            if (resId != -1) {
                // 核心联动：播放视频前，先让 MainActivity 把后台正在播放的音乐暂停
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    mainActivity.pauseMusicForVideo();
                }

                // 计算本地视频的绝对路径 URI 并传给 VideoView 播放
                Uri videoUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + resId);
                videoView.setVideoURI(videoUri);
                videoView.start(); // 开始播放
            }
        });

        return view;
    }

    /**
     * 高级反射机制：动态扫描抓取 res/raw 中所有以 video_ 开头的视频文件
     */
    private void scanRawVideoFiles() {
        localVideoNames.clear();
        localVideoResIds.clear();

        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                String name = field.getName();
                // 核心过滤：只有名字以 video_ 开头的文件才放入视频列表
                if (name.startsWith("video_")) {
                    // 去掉 video_ 前缀作为干净的歌名/视频名显示
                    String displayName = name.substring(6);
                    int resId = field.getInt(null);

                    localVideoNames.add(displayName);
                    localVideoResIds.add(resId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 友好防错提示
        if (localVideoNames.isEmpty()) {
            localVideoNames.add("未检测到本地视频，请将视频以 video_ 开头命名放入 res/raw 文件夹");
            localVideoResIds.add(-1);
        }
    }

    // 内部列表适配器
    class MyVideoAdapter extends BaseAdapter {
        @Override
        public int getCount() { return localVideoNames.size(); }
        @Override
        public Object getItem(int position) { return localVideoNames.get(position); }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tv_item_name);
            tvName.setText(localVideoNames.get(position));
            // 改变一下视频列表的文字颜色以便和音乐列表做视觉区分
            tvName.setTextColor(android.graphics.Color.parseColor("#EEEEEE"));
            return convertView;
        }
    }
}