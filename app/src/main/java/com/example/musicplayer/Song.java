package com.example.musicplayer;

import java.io.Serializable;

/**
 * Data model for a song, carrying raw-resource metadata plus
 * optional artist / album / genre extracted via MediaMetadataRetriever.
 */
public class Song implements Serializable {
    public String title;
    public int resId;
    public String artist;
    public String album;
    public String genre;
    public long durationMs;

    public Song(String title, int resId) {
        this.title = title;
        this.resId = resId;
        this.artist = "未知艺术家";
        this.album = "未知专辑";
        this.genre = "未知流派";
        this.durationMs = 0;
    }
}