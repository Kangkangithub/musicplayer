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

public class VideoFragment extends Fragment {
    private VideoView videoView;
    private ListView lvVideo;

    // 模拟网络服务器提供的在线测试视频
    private String[] videoNames = {"网络推荐：大雄的动画测试短片", "网络推荐：极质超清样片"};
    private String[] videoUrls = {
            "https://www.w3school.com.cn/example/html5/mov_bbb.mp4",
            "https://media.w3.org/2010/05/sintel/trailer_hd.mp4"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        videoView = view.findViewById(R.id.video_view);
        lvVideo = view.findViewById(R.id.lv_video);

        // 核心要求3：为VideoView添加自带的视频播放控制器(快进/暂停控制条)
        MediaController mediaController = new MediaController(getContext());
        videoView.setMediaController(mediaController);

        MyVideoAdapter adapter = new MyVideoAdapter();
        lvVideo.setAdapter(adapter);

        // 点击视频列表某一项时播放对应的网络视频
        lvVideo.setOnItemClickListener((parent, view1, position, id) -> {
            // 联动处理：播放视频时，通知MainActivity先把正在播放的后台音乐暂停，防止声音重叠
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.pauseMusicForVideo();
            }
            // 核心要求3：运用VideoView播放网络视频文件
            videoView.setVideoURI(Uri.parse(videoUrls[position]));
            videoView.start();
        });
        return view;
    }

    class MyVideoAdapter extends BaseAdapter {
        @Override
        public int getCount() { return videoNames.length; }
        @Override
        public Object getItem(int position) { return videoNames[position]; }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tv_item_name);
            tvName.setText(videoNames[position]);
            return convertView;
        }
    }
}