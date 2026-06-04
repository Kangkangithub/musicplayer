package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvMusicTitle, tvType, tvProgress, tvTotal;
    private SeekBar sb;
    private Button btnPlay, btnPause, btnContinuePlay, btnExit;
    private Button btnPrev, btnNext;
    private Button btnNavMusic, btnNavVideo;
    private ImageView ivMusic;
    private ScrollView svLyrics;
    private TextView tvLyrics;

    private MusicService.MusicControl musicControl;
    private boolean isUnbind = false;
    private ObjectAnimator animator;

    private MusicFragment musicFragment;
    private VideoFragment videoFragment;

    private List<Song> playlist = new ArrayList<>();
    private int currentPlayIndex = -1;
    private boolean isLyricsVisible = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicControl = (MusicService.MusicControl) service;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (musicControl != null) {
                int progress = musicControl.getCurrentPosition();
                int total = musicControl.getDuration();
                if (total > 0) {
                    sb.setMax(total);
                    sb.setProgress(progress);
                    tvProgress.setText(timeToString(progress));
                    tvTotal.setText(timeToString(total));
                    if (isLyricsVisible) {
                        scrollLyricsToTime(progress);
                    }
                }
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initAnimator();
        initFragments();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

        new Thread(() -> {
            while (!isUnbind) {
                try {
                    Thread.sleep(500);
                    handler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (!isUnbind) {
                try {
                    Thread.sleep(1000);
                    if (musicControl != null && !musicControl.isPlaying()
                            && musicControl.getCurrentPosition() >= musicControl.getDuration()
                            && musicControl.getDuration() > 0) {
                        runOnUiThread(() -> playNext());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initView() {
        tvMusicTitle = findViewById(R.id.tv_music_title);
        tvType = findViewById(R.id.tv_type);
        tvProgress = findViewById(R.id.tv_progress);
        tvTotal = findViewById(R.id.tv_total);
        sb = findViewById(R.id.sb);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnContinuePlay = findViewById(R.id.btn_continue_play);
        btnExit = findViewById(R.id.btn_exit);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnNavMusic = findViewById(R.id.btn_nav_music);
        btnNavVideo = findViewById(R.id.btn_nav_video);
        ivMusic = findViewById(R.id.iv_music);
        svLyrics = findViewById(R.id.sv_lyrics);
        tvLyrics = findViewById(R.id.tv_lyrics);

        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnContinuePlay.setOnClickListener(this);
        btnExit.setOnClickListener(this);
        btnPrev.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnNavMusic.setOnClickListener(this);
        btnNavVideo.setOnClickListener(this);

        // 点击专辑图标 → 切换歌词浮层
        ivMusic.setOnClickListener(v -> toggleLyrics());

        // 点击歌词浮层本身 → 关闭
        svLyrics.setOnClickListener(v -> {
            if (isLyricsVisible) toggleLyrics();
        });

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicControl != null) {
                    musicControl.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void toggleLyrics() {
        if (isLyricsVisible) {
            svLyrics.setVisibility(View.GONE);
            isLyricsVisible = false;
        } else {
            loadLyricsForCurrentSong();
            svLyrics.setVisibility(View.VISIBLE);
            isLyricsVisible = true;
        }
    }

    private void initAnimator() {
        animator = ObjectAnimator.ofFloat(ivMusic, "rotation", 0f, 360f);
        animator.setDuration(12000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
    }

    private void initFragments() {
        musicFragment = new MusicFragment();
        videoFragment = new VideoFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, musicFragment);
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_nav_music) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, musicFragment);
            ft.commit();
        } else if (id == R.id.btn_nav_video) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, videoFragment);
            ft.commit();
        } else if (id == R.id.btn_play) {
            Field[] fields = R.raw.class.getFields();
            if (fields.length > 0 && musicControl != null) {
                try {
                    int defaultResId = fields[0].getInt(null);
                    String defaultTitle = fields[0].getName();
                    playMusic(defaultResId, defaultTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (id == R.id.btn_prev) {
            playPrev();
        } else if (id == R.id.btn_next) {
            playNext();
        } else if (id == R.id.btn_pause) {
            if (musicControl != null) {
                musicControl.pause();
                animator.pause();
            }
        } else if (id == R.id.btn_continue_play) {
            if (musicControl != null) {
                musicControl.resume();
                animator.resume();
            }
        } else if (id == R.id.btn_exit) {
            if (!isUnbind) {
                unbindService(connection);
                isUnbind = true;
            }
            finish();
        }
    }

    public void setPlaylist(List<Song> list, int index) {
        this.playlist = list;
        this.currentPlayIndex = index;
    }

    public void playMusic(int resId, String title) {
        if (musicControl != null) {
            tvMusicTitle.setText(title);
            tvType.setText("Local Music");
            musicControl.playLocal(resId);
            animator.start();
            if (isLyricsVisible) {
                loadLyricsForCurrentSong();
            }
        }
    }

    public void playNext() {
        if (playlist.isEmpty() || currentPlayIndex < 0) return;
        int next = currentPlayIndex + 1;
        if (next >= playlist.size()) {
            Toast.makeText(this, "End of playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPlayIndex = next;
        musicFragment.setCurrentPlayIndex(currentPlayIndex);
        Song song = playlist.get(currentPlayIndex);
        playMusic(song.resId, song.title);
    }

    public void playPrev() {
        if (playlist.isEmpty() || currentPlayIndex < 0) return;
        int prev = currentPlayIndex - 1;
        if (prev < 0) {
            Toast.makeText(this, "Already first song", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPlayIndex = prev;
        musicFragment.setCurrentPlayIndex(currentPlayIndex);
        Song song = playlist.get(currentPlayIndex);
        playMusic(song.resId, song.title);
    }

    public void pauseMusicForVideo() {
        if (musicControl != null && musicControl.isPlaying()) {
            musicControl.pause();
            animator.pause();
        }
    }

    // ==================== Lyrics ====================

    private String currentLyricsRaw = "";
    private ArrayList<Long> lyricTimes = new ArrayList<>();
    private ArrayList<String> lyricLines = new ArrayList<>();
    private int lastLyricLine = -1;

    private void loadLyricsForCurrentSong() {
        if (currentPlayIndex < 0 || currentPlayIndex >= playlist.size()) {
            tvLyrics.setText("No lyrics available");
            currentLyricsRaw = "";
            return;
        }
        Song song = playlist.get(currentPlayIndex);
        String lyrics = loadLyricsFromAssets(song.title);
        if (lyrics == null || lyrics.isEmpty()) {
            tvLyrics.setText("No lyrics available");
            currentLyricsRaw = "";
            return;
        }
        currentLyricsRaw = lyrics;
        parseLyrics(lyrics);
        tvLyrics.setText(lyrics);
    }

    private String loadLyricsFromAssets(String songFileName) {
        try {
            InputStream is = getAssets().open("lyrics/" + songFileName + ".lrc");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void parseLyrics(String raw) {
        lyricTimes.clear();
        lyricLines.clear();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[") && line.length() > 10) {
                try {
                    int min = Integer.parseInt(line.substring(1, 3));
                    int sec = Integer.parseInt(line.substring(4, 6));
                    int ms = 0;
                    if (line.indexOf('.', 6) > 0) {
                        String msStr = line.substring(7, line.indexOf(']'));
                        ms = Integer.parseInt(msStr);
                        if (msStr.length() == 2) ms *= 10;
                    }
                    long timeMs = (min * 60L + sec) * 1000L + ms;
                    String text = line.substring(line.indexOf(']') + 1).trim();
                    lyricTimes.add(timeMs);
                    lyricLines.add(text);
                } catch (Exception ignored) {}
            }
        }
    }

    private void scrollLyricsToTime(int currentMs) {
        if (lyricTimes.isEmpty()) return;
        int targetLine = 0;
        for (int i = 0; i < lyricTimes.size(); i++) {
            if (currentMs >= lyricTimes.get(i)) {
                targetLine = i;
            } else {
                break;
            }
        }
        if (targetLine != lastLyricLine) {
            lastLyricLine = targetLine;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lyricLines.size(); i++) {
                if (i == targetLine) {
                    sb.append("> ").append(lyricLines.get(i)).append("\n");
                } else {
                    sb.append("  ").append(lyricLines.get(i)).append("\n");
                }
            }
            tvLyrics.setText(sb.toString());
        }
    }

    private String timeToString(int time) {
        time /= 1000;
        int minute = time / 60;
        int second = time % 60;
        return String.format("%02d:%02d", minute, second);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isUnbind) {
            unbindService(connection);
            isUnbind = true;
        }
    }
}