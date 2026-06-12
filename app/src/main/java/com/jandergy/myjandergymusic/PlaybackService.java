package com.jandergy.myjandergymusic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.jandergy.myjandergymusic.audio.BitPerfectRenderersFactory;
import com.jandergy.myjandergymusic.audio.RhythmBassBoostProcessor;
import com.jandergy.myjandergymusic.audio.RhythmSpatializationProcessor;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;
    private RhythmBassBoostProcessor bassBoostProcessor;
    private RhythmSpatializationProcessor spatializationProcessor;
    private Equalizer equalizer;
    private ExoPlayer player;
    private int activeSessionId = -1;

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();
        
        bassBoostProcessor = new RhythmBassBoostProcessor();
        spatializationProcessor = new RhythmSpatializationProcessor();
        
        BitPerfectRenderersFactory renderersFactory = new BitPerfectRenderersFactory(
                this,
                false, // Bit-perfect disabled by default
                bassBoostProcessor,
                spatializationProcessor
        );
        
        player = new ExoPlayer.Builder(this, renderersFactory).build();
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    ensureEqualizerInitialized();
                }
            }
        });
        
        Bundle extras = new Bundle();
        extras.putInt("audio_session_id", player.getAudioSessionId());
        
        mediaSession = new MediaSession.Builder(this, player)
                .setExtras(extras)
                .build();
    }

    @UnstableApi
    private void ensureEqualizerInitialized() {
        int currentSessionId = player.getAudioSessionId();
        if (currentSessionId != 0 && currentSessionId != activeSessionId) {
            try {
                if (equalizer != null) {
                    equalizer.release();
                }
                Log.d("PlaybackService", "Initializing hardware EQ for session: " + currentSessionId);
                equalizer = new Equalizer(0, currentSessionId);
                
                applySavedSettings(); // Load and apply user settings
                
                equalizer.setEnabled(true);
                activeSessionId = currentSessionId;
                Log.d("PlaybackService", "Hardware EQ attached and enabled for session " + currentSessionId);
            } catch (Exception e) {
                Log.e("PlaybackService", "Failed to initialize hardware EQ", e);
            }
        }
    }

    private void applySavedSettings() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);

        // Apply native EQ bands directly (no interpolation)
        if (equalizer != null) {
            short numBands = equalizer.getNumberOfBands();
            for (short i = 0; i < numBands; i++) {
                int level = prefs.getInt("EQ_Band_" + i, 0);
                try {
                    equalizer.setBandLevel(i, (short) level);
                } catch (Exception ignored) {}
            }

            boolean eqEnabled = prefs.getBoolean("EQ_Enabled", true);
            equalizer.setEnabled(eqEnabled);
        }

        // Apply Bass Boost
        int bbStrength = prefs.getInt("BassBoost_Strength", 0);
        bassBoostProcessor.setEnabled(bbStrength > 0);
        bassBoostProcessor.setStrength((short) bbStrength);

        // Apply Spatializer
        int spStrength = prefs.getInt("Spatializer_Strength", 0);
        spatializationProcessor.setEnabled(spStrength > 0);
        spatializationProcessor.setStrength((short) spStrength);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case "ACTION_SET_BASS_BOOST": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    short strength = intent.getShortExtra("strength", (short) 0);
                    bassBoostProcessor.setEnabled(enabled);
                    bassBoostProcessor.setStrength(strength);
                    break;
                }
                case "ACTION_SET_SPATIALIZER": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    short strength = intent.getShortExtra("strength", (short) 0);
                    spatializationProcessor.setEnabled(enabled);
                    spatializationProcessor.setStrength(strength);
                    break;
                }
                case "ACTION_SET_EQ_ENABLED": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    ensureEqualizerInitialized();
                    if (equalizer != null) equalizer.setEnabled(enabled);
                    break;
                }
                case "ACTION_SET_EQ_BAND": {
                    short band = intent.getShortExtra("band", (short) -1);
                    short level = intent.getShortExtra("level", (short) 0);
                    ensureEqualizerInitialized();
                    if (equalizer != null && band != -1) {
                        try {
                            equalizer.setBandLevel(band, level);
                        } catch (Exception e) {
                            Log.e("PlaybackService", "Error setting band level", e);
                        }
                    }
                    break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        if (equalizer != null) {
            equalizer.release();
        }
        super.onDestroy();
    }
}
