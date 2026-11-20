package com.jandergy.myjandergymusic;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
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
import com.jandergy.myjandergymusic.audio.AudioTelemetry;
import com.jandergy.myjandergymusic.audio.BitPerfectRenderersFactory;
import com.jandergy.myjandergymusic.audio.Rhythm8DProcessor;
import com.jandergy.myjandergymusic.audio.RhythmBassBoostProcessor;
import com.jandergy.myjandergymusic.audio.RhythmBeatAnalyzerProcessor;
import com.jandergy.myjandergymusic.audio.RhythmSpatializationProcessor;
import com.jandergy.myjandergymusic.audio.RhythmTapeStopProcessor;
import com.jandergy.myjandergymusic.audio.RhythmVibeProcessor;
import com.jandergy.myjandergymusic.audio.RhythmVocalProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class PlaybackService extends MediaSessionService {
    private static final String TAG = "PlaybackService";
    private static final long SLEEP_FADE_DURATION_MS = 15_000L;

    public static final String ACTION_SLEEP_TIMER_UPDATED = "com.jandergy.myjandergymusic.SLEEP_TIMER_UPDATED";
    public static final String ACTION_DSP_PROFILE_APPLIED = "com.jandergy.myjandergymusic.DSP_PROFILE_APPLIED";

    private static volatile long sleepTimerEndElapsedMs = 0L;

    private MediaSession mediaSession = null;
    private RhythmTapeStopProcessor tapeStopProcessor;
    private RhythmVocalProcessor vocalProcessor;
    private RhythmVibeProcessor vibeProcessor;
    private RhythmBassBoostProcessor bassBoostProcessor;
    private RhythmSpatializationProcessor spatializationProcessor;
    private Rhythm8DProcessor eightDProcessor;
    private RhythmBeatAnalyzerProcessor beatAnalyzerProcessor;
    private Equalizer equalizer;
    private ExoPlayer player;
    private int activeSessionId = -1;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bitmapExecutor = Executors.newSingleThreadExecutor();
    private volatile Bitmap fallbackCoverBitmap;

    private final Runnable sleepTimerTick = new Runnable() {
        @Override
        public void run() {
            if (sleepTimerEndElapsedMs <= 0L || player == null) {
                return;
            }
            long remainingMs = sleepTimerEndElapsedMs - SystemClock.elapsedRealtime();
            if (remainingMs <= 0L) {
                sleepTimerEndElapsedMs = 0L;
                if (player.isPlaying()) {
                    player.pause();
                }
                player.setVolume(1.0f);
                broadcastSleepTimerUpdate(0L, true);
                return;
            }

            if (remainingMs <= SLEEP_FADE_DURATION_MS) {
                // Smooth equal-power cosine fade-out over the final 15 seconds
                float progress = 1.0f - (remainingMs / (float) SLEEP_FADE_DURATION_MS);
                float gain = (float) Math.cos(progress * Math.PI * 0.5);
                player.setVolume(Math.max(0f, Math.min(1f, gain)));
            } else {
                player.setVolume(1.0f);
            }

            broadcastSleepTimerUpdate(remainingMs, false);
            mainHandler.postDelayed(this, remainingMs <= SLEEP_FADE_DURATION_MS ? 150L : 500L);
        }
    };

    public static long getSleepTimerRemainingMs() {
        long end = sleepTimerEndElapsedMs;
        if (end <= 0L) return 0L;
        return Math.max(0L, end - SystemClock.elapsedRealtime());
    }

    @UnstableApi
    @Override
    public void onCreate() {
        super.onCreate();

        tapeStopProcessor = new RhythmTapeStopProcessor();
        vocalProcessor = new RhythmVocalProcessor();
        vibeProcessor = new RhythmVibeProcessor();
        bassBoostProcessor = new RhythmBassBoostProcessor();
        spatializationProcessor = new RhythmSpatializationProcessor();
        eightDProcessor = new Rhythm8DProcessor();
        beatAnalyzerProcessor = new RhythmBeatAnalyzerProcessor();
        applySavedDspSettings();

        BitPerfectRenderersFactory renderersFactory = new BitPerfectRenderersFactory(
                this,
                false, // Bit-perfect disabled by default
                tapeStopProcessor,
                vocalProcessor,
                vibeProcessor,
                bassBoostProcessor,
                spatializationProcessor,
                eightDProcessor,
                beatAnalyzerProcessor
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

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && player != null && sleepTimerEndElapsedMs <= 0L) {
                    player.setVolume(1.0f);
                }
                if (!isPlaying) {
                    AudioTelemetry.INSTANCE.resetAudioLevels();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                // Keep Vibe, Vocal, Speed/Pitch, and Studio FX active across song changes
                // unless the user explicitly turns them off.
                if (mediaItem != null) {
                    Intent intent = new Intent(ACTION_DSP_PROFILE_APPLIED);
                    intent.setPackage(getPackageName());
                    intent.putExtra("mediaId", mediaItem.mediaId);
                    sendBroadcast(intent);
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

    private void broadcastSleepTimerUpdate(long remainingMs, boolean finished) {
        Intent intent = new Intent(ACTION_SLEEP_TIMER_UPDATED);
        intent.setPackage(getPackageName());
        intent.putExtra("remainingMs", remainingMs);
        intent.putExtra("finished", finished);
        sendBroadcast(intent);
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

        boolean tapeStopEnabled = prefs.getBoolean("TapeStop_Enabled", false);
        if (tapeStopProcessor != null) {
            tapeStopProcessor.setFeatureEnabled(tapeStopEnabled);
        }

        int vocalMode = prefs.getInt("Vocal_Mode", RhythmVocalProcessor.MODE_OFF);
        if (vocalProcessor != null) {
            vocalProcessor.setMode(vocalMode);
        }

        int vibeMode = prefs.getInt("Vibe_Mode", RhythmVibeProcessor.MODE_OFF);
        if (vibeProcessor != null) {
            vibeProcessor.setMode(vibeMode);
        }

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
        int eightDMode = prefs.getInt("RHYTHM_8D_MODE", Rhythm8DProcessor.MODE_AUTO);
        float eightDSpeed = prefs.getFloat("RHYTHM_8D_SPEED", 0.12f);
        boolean eightDReverse = prefs.getBoolean("RHYTHM_8D_REVERSE", false);
        float eightDX = prefs.getFloat("RHYTHM_8D_X", 0f);
        float eightDY = prefs.getFloat("RHYTHM_8D_Y", 1f);
        if (eightDProcessor != null) {
            eightDProcessor.setSpatialMode(eightDMode);
            eightDProcessor.setRotationSpeed(eightDSpeed);
            eightDProcessor.setReverse(eightDReverse);
            eightDProcessor.setManualPosition(eightDX, eightDY);
            eightDProcessor.setEnabled(eightDEnabled);
        }

        boolean beatEnabled = prefs.getBoolean("Beat_Visuals_Enabled", true)
                || prefs.getBoolean("Haptic_Bass_Enabled", false);
        if (beatAnalyzerProcessor != null) {
            beatAnalyzerProcessor.setEnabled(beatEnabled);
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
                case "ACTION_SET_8D_CONFIG": {
                    if (eightDProcessor != null) {
                        if (intent.hasExtra("enabled")) {
                            eightDProcessor.setEnabled(intent.getBooleanExtra("enabled", false));
                        }
                        if (intent.hasExtra("mode")) {
                            eightDProcessor.setSpatialMode(intent.getIntExtra("mode", Rhythm8DProcessor.MODE_AUTO));
                        }
                        if (intent.hasExtra("speed")) {
                            eightDProcessor.setRotationSpeed(intent.getFloatExtra("speed", 0.12f));
                        }
                        if (intent.hasExtra("reverse")) {
                            eightDProcessor.setReverse(intent.getBooleanExtra("reverse", false));
                        }
                        if (intent.hasExtra("x") && intent.hasExtra("y")) {
                            eightDProcessor.setManualPosition(
                                    intent.getFloatExtra("x", 0f),
                                    intent.getFloatExtra("y", 1f)
                            );
                        }
                    }
                    break;
                }
                case "ACTION_SET_8D_POSITION": {
                    if (eightDProcessor != null) {
                        eightDProcessor.setManualPosition(
                                intent.getFloatExtra("x", 0f),
                                intent.getFloatExtra("y", 1f)
                        );
                    }
                    break;
                }
                case "ACTION_SET_VIBE_MODE": {
                    int mode = intent.getIntExtra("mode", RhythmVibeProcessor.MODE_OFF);
                    if (vibeProcessor != null) {
                        vibeProcessor.setMode(mode);
                    }
                    break;
                }
                case "ACTION_SET_VOCAL_MODE": {
                    int mode = intent.getIntExtra("mode", RhythmVocalProcessor.MODE_OFF);
                    if (vocalProcessor != null) {
                        vocalProcessor.setMode(mode);
                    }
                    break;
                }
                case "ACTION_SET_TAPE_STOP": {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    if (tapeStopProcessor != null) {
                        tapeStopProcessor.setFeatureEnabled(enabled);
                    }
                    break;
                }
                case "ACTION_TAPE_STOP_PAUSE": {
                    if (player != null && player.isPlaying()) {
                        if (tapeStopProcessor != null && !tapeStopProcessor.isStopping()) {
                            tapeStopProcessor.setFeatureEnabled(true);
                            tapeStopProcessor.triggerStop(() -> mainHandler.post(() -> {
                                if (player != null && player.isPlaying()) {
                                    player.pause();
                                }
                                if (tapeStopProcessor != null) {
                                    tapeStopProcessor.cancelEffect();
                                }
                            }));
                            // Fallback safety timeout in case audio buffer wasn't actively draining
                            mainHandler.postDelayed(() -> {
                                if (tapeStopProcessor != null && tapeStopProcessor.isStopping()) {
                                    if (player != null && player.isPlaying()) {
                                        player.pause();
                                    }
                                    tapeStopProcessor.cancelEffect();
                                }
                            }, 550L);
                        } else {
                            player.pause();
                        }
                    }
                    break;
                }
                case "ACTION_TAPE_SPIN_UP": {
                    if (player != null) {
                        if (tapeStopProcessor != null) {
                            tapeStopProcessor.setFeatureEnabled(true);
                            tapeStopProcessor.triggerSpinUp();
                        }
                        if (!player.isPlaying()) {
                            player.play();
                        }
                    }
                    break;
                }
                case "ACTION_SET_BEAT_ANALYZER": {
                    boolean enabled = intent.getBooleanExtra("enabled", true);
                    if (beatAnalyzerProcessor != null) {
                        beatAnalyzerProcessor.setEnabled(enabled);
                    }
                    break;
                }
                case "ACTION_SET_SLEEP_TIMER": {
                    long durationMs = intent.getLongExtra("durationMs", 0L);
                    mainHandler.removeCallbacks(sleepTimerTick);
                    if (durationMs <= 0L) {
                        sleepTimerEndElapsedMs = 0L;
                        if (player != null) {
                            player.setVolume(1.0f);
                        }
                        broadcastSleepTimerUpdate(0L, false);
                    } else {
                        sleepTimerEndElapsedMs = SystemClock.elapsedRealtime() + durationMs;
                        if (player != null) {
                            player.setVolume(1.0f);
                        }
                        mainHandler.post(sleepTimerTick);
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
        mainHandler.removeCallbacksAndMessages(null);
        sleepTimerEndElapsedMs = 0L;
        AudioTelemetry.INSTANCE.resetAudioLevels();
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
