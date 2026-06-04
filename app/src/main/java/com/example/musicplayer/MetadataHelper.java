package com.example.musicplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

/**
 * Utility to extract embedded tags from raw-resource MP3 files.
 */
public class MetadataHelper {

    public static void fillMetadata(Context context, Song song) {
        if (song == null || song.resId < 0) return;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + song.resId);
            mmr.setDataSource(context, uri);

            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (artist != null && !artist.isEmpty()) song.artist = artist;

            String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (album != null && !album.isEmpty()) song.album = album;

            String genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            if (genre != null && !genre.isEmpty()) song.genre = genre;

            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                song.durationMs = Long.parseLong(duration);
            }
        } catch (Exception e) {
            // Metadata extraction is best-effort; keep defaults.
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }
}