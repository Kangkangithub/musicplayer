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
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvMusicTitle, tvType, tvProgress, tvTotal;
    private SeekBar sb;
    private Button btnPlay, btnPause, btnContinuePlay, btnExit;
    private Button btnNavMusic, btnNavVideo;
    private ImageView ivMusic;

    private MusicService.MusicControl musicControl;
    private boolean isUnbind = false;
    private ObjectAnimator animator;

    private FragmentManager fragmentManager;
    private MusicFragment musicFragment;
    private VideoFragment videoFragment;

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
        btnNavMusic = findViewById(R.id.btn_nav_music);
        btnNavVideo = findViewById(R.id.btn_nav_video);
        ivMusic = findViewById(R.id.iv_music);

        btnPlay.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnContinuePlay.setOnClickListener(this);
        btnExit.setOnClickListener(this);
        btnNavMusic.setOnClickListener(this);
        btnNavVideo.setOnClickListener(this);

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

    private void initAnimator() {
        animator = ObjectAnimator.ofFloat(ivMusic, "rotation", 0f, 360f);
        animator.setDuration(12000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
    }

    private void initFragments() {
        fragmentManager = getSupportFragmentManager();
        musicFragment = new MusicFragment();
        videoFragment = new VideoFragment();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, musicFragment);
        transaction.commit();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_nav_music) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.fragment_container, musicFragment);
            ft.commit();
        } else if (id == R.id.btn_nav_video) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.fragment_container, videoFragment);
            ft.commit();
        } else if (id == R.id.btn_play) {
            // 当直接点击底部的播放键时，默认去读取并播放第一个本地raw文件
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

    // 修改：接口调整为接收本地资源 ID
    public void playMusic(int resId, String title) {
        if (musicControl != null) {
            tvMusicTitle.setText(title);
            tvType.setText("本地音乐");
            musicControl.playLocal(resId);
            animator.start();
        }
    }

    public void pauseMusicForVideo() {
        if (musicControl != null && musicControl.isPlaying()) {
            musicControl.pause();
            animator.pause();
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