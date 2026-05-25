package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class MusicFragment extends Fragment {
    private ListView lvMusic;

    // 用于动态装载扫描出来的歌曲名字和资源ID
    private ArrayList<String> localMusicNames = new ArrayList<>();
    private ArrayList<Integer> localMusicResIds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        lvMusic = view.findViewById(R.id.lv_music);

        // 调用自动扫描机制识别 res/raw 中的歌曲
        scanRawMusicFiles();

        MyMusicAdapter adapter = new MyMusicAdapter();
        lvMusic.setAdapter(adapter);

        // 点击列表条目，获取资源 ID 并联动控制台进行播放
        lvMusic.setOnItemClickListener((parent, view1, position, id) -> {
            int resId = localMusicResIds.get(position);
            String title = localMusicNames.get(position);

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null && resId != -1) {
                mainActivity.playMusic(resId, title);
            }
        });
        return view;
    }

    /**
     * 核心高级功能：通过反射自动扫描读取 res/raw 内的所有资源文件
     */
    private void scanRawMusicFiles() {
        localMusicNames.clear();
        localMusicResIds.clear();

        // 获取生成的 R.raw 类的所有公开属性字段
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                // 获取文件名（作为歌名）和对应的资源 ID
                String songName = field.getName();
                int resId = field.getInt(null);

                localMusicNames.add(songName);
                localMusicResIds.add(resId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 如果你还没来得及放文件，给个友好提示
        if (localMusicNames.isEmpty()) {
            localMusicNames.add("未在 res/raw 中检测到歌曲，请放入mp3文件");
            localMusicResIds.add(-1);
        }
    }

    class MyMusicAdapter extends BaseAdapter {
        @Override
        public int getCount() { return localMusicNames.size(); }
        @Override
        public Object getItem(int position) { return localMusicNames.get(position); }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tv_item_name);
            tvName.setText(localMusicNames.get(position));
            return convertView;
        }
    }
}