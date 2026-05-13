package com.jandergy.myjandergymusic;

import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;

    @Override
    public void onCreate() {
        super.onCreate();
        ExoPlayer player = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
