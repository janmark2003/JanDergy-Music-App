package com.jandergy.myjandergymusic;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.BitmapLoader;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jandergy.myjandergymusic.audio.BitPerfectRenderersFactory;
import com.jandergy.myjandergymusic.audio.Rhythm8DProcessor;
import com.jandergy.myjandergymusic.audio.RhythmBassBoostProcessor;
import com.jandergy.myjandergymusic.audio.RhythmSpatializationProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class PlaybackService extends MediaSessionService {
    private static final String TAG = "PlaybackService";

    private MediaSession mediaSession = null;
    private RhythmBassBoostProcessor bassBoostProcessor;
    private RhythmSpatializationProcessor spatializationProcessor;
    private Rhythm8DProcessor eightDProcessor;
    private Equalizer equalizer;
    private ExoPlayer player;
    private int activeSessionId = -1;
    private final ExecutorService bitmapExecutor = Executors.newSingleThreadExecutor();
    private volatile Bitmap fallbackCoverBitmap;

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();

        bassBoostProcessor = new RhythmBassBoostProcessor();
        spatializationProcessor = new RhythmSpatializationProcessor();
        eightDProcessor = new Rhythm8DProcessor();
        applySavedDspSettings();

        BitPerfectRenderersFactory renderersFactory = new BitPerfectRenderersFactory(
                this,
                false, // Bit-perfect disabled by default
                bassBoostProcessor,
                spatializationProcessor,
                eightDProcessor
        );

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        30000, // minBufferMs
                        60000, // maxBufferMs
                        1500,  // bufferForPlaybackMs
                        2500   // bufferForPlaybackAfterRebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this, renderersFactory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        applySavedPlaybackSpeed();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    ensureEqualizerInitialized();
                }
            }
        });

        // PendingIntent that launches MainActivity when user taps media controls
        Intent sessionIntent = new Intent(this, MainActivity.class);
        sessionIntent.putExtra("OPEN_PLAYER", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                sessionIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Bundle extras = new Bundle();
        extras.putInt("audio_session_id", player.getAudioSessionId());

        mediaSession = new MediaSession.Builder(this, player)
                .setExtras(extras)
                .setSessionActivity(pendingIntent)
                .setBitmapLoader(new CustomBitmapLoader())
                .build();
    }

    @Nullable
    private Bitmap getFallbackCoverBitmap() {
        Bitmap cached = fallbackCoverBitmap;
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.cover_ep, bounds);

            int sampleSize = 1;
            int maxDim = 512;
            while ((bounds.outWidth / sampleSize) > maxDim || (bounds.outHeight / sampleSize) > maxDim) {
                sampleSize *= 2;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap decoded = BitmapFactory.decodeResource(getResources(), R.drawable.cover_ep, opts);
            fallbackCoverBitmap = decoded;
            return decoded;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to decode fallback cover artwork", t);
            return null;
        }
    }

    private class CustomBitmapLoader implements BitmapLoader {
        @Override
        public boolean supportsMimeType(@Nullable String mimeType) {
            return mimeType == null || mimeType.startsWith("image/");
        }

        @NonNull
        @Override
        public ListenableFuture<Bitmap> decodeBitmap(@NonNull byte[] data) {
            try {
                Bitmap bitmap = null;
                if (data.length > 0) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.RGB_565;
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                }
                if (bitmap == null) {
                    bitmap = getFallbackCoverBitmap();
                }
                if (bitmap == null) {
                    return Futures.immediateFailedFuture(new IllegalStateException("Unable to decode bitmap"));
                }
                return Futures.immediateFuture(bitmap);
            } catch (Throwable t) {
                return Futures.immediateFailedFuture(t);
            }
        }

        @NonNull
        @Override
        public ListenableFuture<Bitmap> loadBitmap(@NonNull Uri uri) {
            if (bitmapExecutor.isShutdown()) {
                return Futures.immediateFailedFuture(new IllegalStateException("Service destroyed"));
            }
            SettableFuture<Bitmap> future = SettableFuture.create();
            try {
                bitmapExecutor.execute(() -> {
                    try {
                        Bitmap bitmap = ArtworkLoader.decodeBitmap(getContentResolver(), uri, 512);
                        if (bitmap == null || bitmap.isRecycled()) {
                            bitmap = getFallbackCoverBitmap();
                        }
                        if (bitmap != null && !bitmap.isRecycled()) {
                            future.set(bitmap);
                        } else {
                            future.setException(new IllegalStateException("Artwork unavailable"));
                        }
                    } catch (Throwable t) {
                        future.setException(t);
                    }
                });
            } catch (RejectedExecutionException e) {
                future.setException(e);
            }
            return future;
        }
    }

    @UnstableApi
    private void ensureEqualizerInitialized() {
        if (player == null) return;
        int currentSessionId = player.getAudioSessionId();
        if (currentSessionId > 0 && currentSessionId != C.AUDIO_SESSION_ID_UNSET && currentSessionId != activeSessionId) {
            try {
                if (equalizer != null) {
                    try {
                        equalizer.release();
                    } catch (Exception ignored) {}
                    equalizer = null;
                }
                Log.d(TAG, "Initializing hardware EQ for session: " + currentSessionId);
                equalizer = new Equalizer(0, currentSessionId);
                activeSessionId = currentSessionId;
                applySavedSettings();
                Log.d(TAG, "Hardware EQ attached for session " + currentSessionId);
            } catch (Throwable e) {
                Log.e(TAG, "Failed to initialize hardware EQ", e);
                equalizer = null;
                activeSessionId = -1;
            }
        }
    }

    private void applySavedDspSettings() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);

        int bbStrength = prefs.getInt("BassBoost_Strength", 0);
        if (bassBoostProcessor != null) {
            bassBoostProcessor.setEnabled(bbStrength > 0);
            bassBoostProcessor.setStrength((short) bbStrength);
        }

        int spStrength = prefs.getInt("Spatializer_Strength", 0);
        if (spatializationProcessor != null) {
            spatializationProcessor.setEnabled(spStrength > 0);
            spatializationProcessor.setStrength((short) spStrength);
        }

        boolean eightDEnabled = prefs.getBoolean("RHYTHM_8D_ENABLED", false);
        if (eightDProcessor != null) {
            eightDProcessor.setEnabled(eightDEnabled);
        }
    }

    private void applySavedPlaybackSpeed() {
        if (player == null) return;
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        float savedSpeed = prefs.getFloat("Playback_Speed", 1.0f);
        if (Float.isNaN(savedSpeed) || savedSpeed < 0.5f || savedSpeed > 2.0f) {
            savedSpeed = 1.0f;
        }
        boolean keepPitch = prefs.getBoolean("Playback_Keep_Pitch", true);
        float pitch = keepPitch ? 1.0f : savedSpeed;
        player.setPlaybackParameters(new PlaybackParameters(savedSpeed, pitch));
    }

    private void applySavedSettings() {
        SharedPreferences prefs = getSharedPreferences("MusicPrefs", MODE_PRIVATE);

        if (equalizer != null) {
            try {
                short numBands = equalizer.getNumberOfBands();
                short[] range = equalizer.getBandLevelRange();
                short minLevel = (range != null && range.length >= 2) ? range[0] : -1500;
                short maxLevel = (range != null && range.length >= 2) ? range[1] : 1500;

                for (short i = 0; i < numBands; i++) {
                    try {
                        int savedLevel = prefs.getInt("EQ_Band_" + i, 0);
                        short clamped = (short) Math.max(minLevel, Math.min(maxLevel, savedLevel));
                        equalizer.setBandLevel(i, clamped);
                    } catch (Exception ignored) {}
                }

                boolean eqEnabled = prefs.getBoolean("EQ_Enabled", true);
                equalizer.setEnabled(eqEnabled);
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply saved EQ settings", e);
            }
        }

        applySavedDspSettings();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case "ACTION_SET_BASS_BOOST": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    short strength = intent.getShortExtra("strength", (short) 0);
                    if (bassBoostProcessor != null) {
                        bassBoostProcessor.setEnabled(enabled && strength > 0);
                        bassBoostProcessor.setStrength(strength);
                    }
                    break;
                }
                case "ACTION_SET_SPATIALIZER": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    short strength = intent.getShortExtra("strength", (short) 0);
                    if (spatializationProcessor != null) {
                        spatializationProcessor.setEnabled(enabled && strength > 0);
                        spatializationProcessor.setStrength(strength);
                    }
                    break;
                }
                case "ACTION_SET_8D_AUDIO": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    if (eightDProcessor != null) {
                        eightDProcessor.setEnabled(enabled);
                    }
                    break;
                }
                case "ACTION_SET_EQ_ENABLED": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    ensureEqualizerInitialized();
                    if (equalizer != null) {
                        try {
                            equalizer.setEnabled(enabled);
                            Log.d(TAG, "Hardware EQ toggled: " + enabled);
                        } catch (Exception e) {
                            Log.e(TAG, "Error toggling hardware EQ", e);
                            activeSessionId = -1;
                        }
                    }
                    break;
                }
                case "ACTION_SET_EQ_BAND": {
                    short band = intent.getShortExtra("band", (short) -1);
                    short level = intent.getShortExtra("level", (short) 0);
                    ensureEqualizerInitialized();
                    if (equalizer != null && band >= 0) {
                        try {
                            short numBands = equalizer.getNumberOfBands();
                            if (band < numBands) {
                                short[] range = equalizer.getBandLevelRange();
                                short clamped = level;
                                if (range != null && range.length >= 2) {
                                    clamped = (short) Math.max(range[0], Math.min(range[1], level));
                                }
                                equalizer.setBandLevel(band, clamped);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting band level", e);
                        }
                    }
                    break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (player == null || !player.getPlayWhenReady() || player.getMediaItemCount() == 0) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        bitmapExecutor.shutdownNow();
        if (equalizer != null) {
            try {
                equalizer.release();
            } catch (Exception ignored) {}
            equalizer = null;
        }
        if (mediaSession != null) {
            try {
                mediaSession.getPlayer().release();
            } catch (Exception ignored) {}
            try {
                mediaSession.release();
            } catch (Exception ignored) {}
            mediaSession = null;
        }
        player = null;
        fallbackCoverBitmap = null;
        super.onDestroy();
    }
}
