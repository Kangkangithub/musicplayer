package com.example.musicplayer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {
    private ListView lvMusic;

    private ArrayList<Song> songList = new ArrayList<>();
    private ArrayList<Song> playlist = new ArrayList<>();
    private int currentPlayIndex = -1;

    private MyMusicAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        lvMusic = view.findViewById(R.id.lv_music);

        scanRawMusicFiles();

        adapter = new MyMusicAdapter();
        lvMusic.setAdapter(adapter);

        // 单击：播放歌曲
        lvMusic.setOnItemClickListener((parent, view1, position, id) -> {
            Song song = songList.get(position);
            if (song.resId == -1) return;

            playlist.clear();
            for (int i = position; i < songList.size(); i++) {
                playlist.add(songList.get(i));
            }
            currentPlayIndex = 0;
            adapter.notifyDataSetChanged();

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.setPlaylist(playlist, currentPlayIndex);
                mainActivity.playMusic(song.resId, song.title);
            }
        });

        return view;
    }

    public void setCurrentPlayIndex(int index) {
        currentPlayIndex = index;
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /**
     * 点击 Info 按钮弹出歌曲详情（专辑/艺术家/流派）
     */
    private void showSongDetailDialog(Song song) {
        // 先提取元数据
        MetadataHelper.fillMetadata(requireContext(), song);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(song.title);
        String msg = "Artist: " + song.artist + "\n"
                   + "Album: " + song.album + "\n"
                   + "Genre: " + song.genre + "\n"
                   + "Duration: " + formatDuration(song.durationMs);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Add to Queue", (dialog, which) -> {
            playlist.add(song);
            adapter.notifyDataSetChanged();
        });
        builder.show();
    }

    private String formatDuration(long ms) {
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public List<Song> getPlaylist() { return playlist; }
    public int getCurrentPlayIndex() { return currentPlayIndex; }

    private void scanRawMusicFiles() {
        songList.clear();

        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                String songName = field.getName();
                if (songName.startsWith("video_")) continue;
                if (songName.startsWith("lyrics_")) continue;
                int resId = field.getInt(null);
                songList.add(new Song(songName, resId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (songList.isEmpty()) {
            songList.add(new Song("No songs found in res/raw", -1));
        }
    }

    class MyMusicAdapter extends BaseAdapter {
        @Override
        public int getCount() { return songList.size(); }
        @Override
        public Object getItem(int position) { return songList.get(position); }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tv_item_name);
            ImageView ivPlaying = convertView.findViewById(R.id.iv_playing);
            Button btnInfo = convertView.findViewById(R.id.btn_info);

            Song song = songList.get(position);
            tvName.setText(song.title);

            // 点击 Info 按钮 → 弹出歌曲详情
            btnInfo.setOnClickListener(v -> {
                MetadataHelper.fillMetadata(requireContext(), song);
                showSongDetailDialog(song);
            });

            // 正在播放的歌曲高亮
            boolean isCurrent = (currentPlayIndex >= 0 && currentPlayIndex < playlist.size()
                    && song.resId == playlist.get(currentPlayIndex).resId);
            if (ivPlaying != null) {
                ivPlaying.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
            }
            if (isCurrent) {
                tvName.setTextColor(android.graphics.Color.parseColor("#FFD700"));
            } else {
                tvName.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
            return convertView;
        }
    }
}