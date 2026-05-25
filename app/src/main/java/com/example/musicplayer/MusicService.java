package com.example.musicplayer;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
    }

    public class MusicControl extends Binder {
        // 新增：接收本地 raw 资源 ID 并播放的功能
        public void playLocal(int resId) {
            try {
                mediaPlayer.reset();
                // 核心：读取 res/raw 中的音频文件描述符
                AssetFileDescriptor afd = getResources().openRawResourceFd(resId);
                if (afd != null) {
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                    mediaPlayer.prepare(); // 本地文件直接同步准备即可
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void pause() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }

        public void resume() {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }

        public int getDuration() {
            return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        }

        public int getCurrentPosition() {
            return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        }

        public void seekTo(int progress) {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(progress);
            }
        }

        public boolean isPlaying() {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicControl();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}