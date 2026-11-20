package com.jandergy.myjandergymusic;

import android.animation.Animator;
import android.content.Intent;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.palette.graphics.Palette;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FullScreenPlayerActivity extends AppCompatActivity {

    private MediaController player;
    private Player.Listener playerListener;
    private ListenableFuture<MediaController> controllerFuture;

    private TextView currentTimeText, remainingTimeText;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat, btnFavNow;
    private ImageButton btnPrev, btnNext;

    // Feature Toolbar Views
    private ImageButton btnSleepTimer, btnSpeed, btnQuickEQ, btnBassToggle, btn8dToggle;
    private TextView sleepTimerLabel, speedLabel, eqLabel, bassLabel, eightDLabel;

    // A-B Loop & Track Position
    private ImageButton btnABLoop;
    private TextView abLoopStatus, trackPositionText;

    private ImageView fullAlbumArt;
    private TextView fullSongTitle, fullArtistName;
    private MovingBlurView backgroundBlur;
    private String activeArtworkRequestKey;

    private SharedPreferences sharedPreferences;
    private Set<String> favoriteIds = new HashSet<>();

    // Playback Speed & Pitch Mode
    private static final float MIN_SPEED = 0.50f;
    private static final float MAX_SPEED = 2.00f;
    private static final float SPEED_STEP = 0.05f;
    private float currentPlaybackSpeed = 1.0f;
    private boolean keepOriginalPitch = true;

    // Sleep Timer
    private CountDownTimer sleepTimer;
    private long sleepTimerRemainingMs = 0;

    // A-B Loop State
    private static final int AB_STATE_NONE = 0;
    private static final int AB_STATE_A_SET = 1;
    private static final int AB_STATE_LOOPING = 2;
    private int abLoopState = AB_STATE_NONE;
    private long abLoopPointA = -1;
    private long abLoopPointB = -1;

    // Local Equalizer for preset inspection
    private Equalizer localEqualizer;
    private boolean isSpinnerInitializing = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying() && !isFinishing() && !isDestroyed()) {
                long currentPos = Math.max(0L, player.getCurrentPosition());
                long duration = player.getDuration();

                if (duration > 0) {
                    int maxDuration = clampToInt(duration);
                    if (seekBar.getMax() != maxDuration) {
                        seekBar.setMax(maxDuration);
                    }
                }

                seekBar.setProgress(clampToInt(currentPos));
                updateTimers(currentPos, duration);

                // Check A-B Loop threshold
                if (abLoopState == AB_STATE_LOOPING && abLoopPointA >= 0 && abLoopPointB > abLoopPointA) {
                    if (currentPos >= abLoopPointB || currentPos < abLoopPointA) {
                        player.seekTo(abLoopPointA);
                    }
                }

                handler.postDelayed(this, 200);
            }
        }
    };

    private Set<String> loadFavoriteIds() {
        Set<String> stored = sharedPreferences.getStringSet("Favorites", null);
        return stored != null ? new HashSet<>(stored) : new HashSet<>();
    }

    private int clampToInt(long value) {
        if (value <= 0L)
            return 0;
        if (value > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) value;
    }

    private void sendServiceCommand(Intent intent) {
        try {
            startService(intent);
        } catch (IllegalStateException | SecurityException e) {
            Log.w("FullScreenPlayerActivity", "Unable to send service command: " + intent.getAction(), e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_full_screen_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.player_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        favoriteIds = loadFavoriteIds();
        currentPlaybackSpeed = clampSpeed(sharedPreferences.getFloat("Playback_Speed", 1.0f));
        keepOriginalPitch = sharedPreferences.getBoolean("Playback_Keep_Pitch", true);

        initUI();
        applyInitialState();
        animateEntrance();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void initUI() {
        backgroundBlur = findViewById(R.id.background_blur);
        fullAlbumArt = findViewById(R.id.full_album_art);
        fullSongTitle = findViewById(R.id.full_song_title);
        fullArtistName = findViewById(R.id.full_artist_name);

        currentTimeText = findViewById(R.id.current_time);
        remainingTimeText = findViewById(R.id.remaining_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnFavNow = findViewById(R.id.btn_fav_now);

        // Feature Toolbar Views
        btnSleepTimer = findViewById(R.id.btn_sleep_timer);
        btnSpeed = findViewById(R.id.btn_speed);
        btnQuickEQ = findViewById(R.id.btn_quick_eq);
        btnBassToggle = findViewById(R.id.btn_bass_toggle);
        btn8dToggle = findViewById(R.id.btn_8d_toggle);

        sleepTimerLabel = findViewById(R.id.sleep_timer_label);
        speedLabel = findViewById(R.id.speed_label);
        eqLabel = findViewById(R.id.eq_label);
        bassLabel = findViewById(R.id.bass_label);
        eightDLabel = findViewById(R.id.eight_d_label);

        // A-B Loop & Track Position
        btnABLoop = findViewById(R.id.btn_ab_loop);
        abLoopStatus = findViewById(R.id.ab_loop_status);
        trackPositionText = findViewById(R.id.track_position);

        fullAlbumArt.setOnClickListener(this::bubblyClick);

        btnPlayPause.setOnClickListener(v -> {
            if (player == null)
                return;
            if (player.isPlaying())
                player.pause();
            else
                player.play();
            bubblyClick(v);
        });

        btnNext.setOnClickListener(v -> {
            if (player != null)
                player.seekToNext();
            bubblyClick(v);
        });
        btnPrev.setOnClickListener(v -> {
            if (player != null)
                player.seekToPrevious();
            bubblyClick(v);
        });

        btnShuffle.setOnClickListener(v -> {
            if (player != null)
                player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
            bubblyClick(v);
        });

        btnRepeat.setOnClickListener(v -> {
            if (player != null) {
                int mode = player.getRepeatMode();
                if (mode == Player.REPEAT_MODE_OFF)
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                else if (mode == Player.REPEAT_MODE_ONE)
                    player.setRepeatMode(Player.REPEAT_MODE_ALL);
                else
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
            bubblyClick(v);
        });

        btnFavNow.setOnClickListener(v -> {
            MediaItem item = (player != null) ? player.getCurrentMediaItem() : null;
            if (item != null) {
                toggleFavorite(item.mediaId);
                bubblyClick(v);
            }
        });

        // Feature Toolbar Listeners
        btnSleepTimer.setOnClickListener(v -> {
            showSleepTimerDialog();
            bubblyClick(v);
        });

        btnSpeed.setOnClickListener(v -> {
            showSpeedDialog();
            bubblyClick(v);
        });
        btnSpeed.setOnLongClickListener(v -> {
            applyPlaybackSpeed(1.0f, keepOriginalPitch);
            bubblyClick(v);
            Toast.makeText(this, "Speed reset to 1.0x", Toast.LENGTH_SHORT).show();
            return true;
        });

        btnQuickEQ.setOnClickListener(v -> {
            showEqualizerDialog();
            bubblyClick(v);
        });

        btnBassToggle.setOnClickListener(v -> {
            toggleBassBoost();
            bubblyClick(v);
        });

        btn8dToggle.setOnClickListener(v -> {
            toggle8dAudio();
            bubblyClick(v);
        });

        btnABLoop.setOnClickListener(v -> {
            handleABLoopClick();
            bubblyClick(v);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    updateTimers(progress, player.getDuration());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        updateAudioEffectsVisualState();
        updateSpeedVisualState();
    }

    private void applyInitialState() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("TITLE")) {
            String title = extras.getString("TITLE");
            String artist = extras.getString("ARTIST");
            long pos = extras.getLong("POSITION");
            long duration = extras.getLong("DURATION");
            boolean isPlaying = extras.getBoolean("IS_PLAYING");
            int repeatMode = extras.getInt("REPEAT_MODE");
            boolean shuffleMode = extras.getBoolean("SHUFFLE_MODE");

            fullSongTitle.setText(FormatUtils.cleanTitle(title, null));
            fullArtistName.setText(FormatUtils.cleanArtist(artist));
            fullSongTitle.setSelected(true);
            fullArtistName.setSelected(true);

            if (duration > 0) {
                seekBar.setMax((int) duration);
                seekBar.setProgress((int) pos);
                updateTimers(pos, duration);
            }

            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_modern_pause : R.drawable.ic_modern_play);
            updateRepeatIcon(repeatMode);
            updateShuffleIcon(shuffleMode);

            String mediaId = extras.getString("MEDIA_ID");
            if (mediaId != null) {
                btnFavNow.setImageResource(
                        favoriteIds.contains(mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            }

            String uriStr = extras.getString("URI");
            if (uriStr != null) {
                loadAlbumArtAndPalette(Uri.parse(uriStr));
            }
        }
    }

    private void animateEntrance() {
        animateLogoPop();

        View albumCard = findViewById(R.id.album_art_card);
        View toolbar = findViewById(R.id.feature_toolbar);
        View controls = findViewById(R.id.player_controls);

        View[] views = { albumCard, toolbar, controls };
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(30f + i * 15f);
                view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(450)
                        .setStartDelay(40 + i * 50)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }

    private void animateLogoPop() {
        View logoView = findViewById(R.id.logo);
        if (logoView != null) {
            logoView.post(() -> {
                logoView.setPivotX(logoView.getWidth() / 2f);
                logoView.setPivotY(logoView.getHeight() / 2f);
                logoView.setScaleX(0.7f);
                logoView.setScaleY(0.7f);
                logoView.setAlpha(0f);
                logoView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(320)
                        .setInterpolator(new DecelerateInterpolator(1.8f))
                        .start();
            });
        }
    }

    private void springClick(View v) {
        if (v == null)
            return;
        v.animate().cancel();
        // Scale down on touch (inward, guaranteed zero overlap with neighbors)
        v.animate()
                .scaleX(0.88f)
                .scaleY(0.88f)
                .setDuration(90)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    // Rebound smoothly back to normal (1.0f)
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(160)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                })
                .start();
    }

    private void bubblyClick(View v) {
        springClick(v);
    }

    private void toggleFavorite(String mediaId) {
        if (mediaId == null || mediaId.isEmpty())
            return;
        if (favoriteIds.contains(mediaId)) {
            favoriteIds.remove(mediaId);
        } else {
            favoriteIds.add(mediaId);
        }
        sharedPreferences.edit().putStringSet("Favorites", new HashSet<>(favoriteIds)).apply();
        btnFavNow.setImageResource(
                favoriteIds.contains(mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        Intent intent = new Intent("com.jandergy.myjandergymusic.FAVORITE_CHANGED");
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    // ==========================================
    // NEW FEATURES LOGIC
    // ==========================================

    private void initLocalEqualizer() {
        if (localEqualizer != null)
            return;
        try {
            localEqualizer = new Equalizer(0, 0);
        } catch (Exception e) {
            Log.w("FullScreenPlayerActivity", "Equalizer session fallback", e);
            localEqualizer = null;
        }
    }

    private void updateAudioEffectsVisualState() {
        // Bass Boost state
        int savedBass = sharedPreferences.getInt("BassBoost_Strength", 0);
        boolean bassActive = savedBass > 0;
        int accentColor = ContextCompat.getColor(this, R.color.accent_neon);
        int defaultColor = Color.WHITE;

        btnBassToggle.setAlpha(bassActive ? 1.0f : 0.6f);
        btnBassToggle.setColorFilter(bassActive ? accentColor : defaultColor);
        bassLabel.setTextColor(bassActive ? accentColor : ContextCompat.getColor(this, R.color.glass_text_secondary));

        // 8D Audio state
        boolean eightDActive = sharedPreferences.getBoolean("RHYTHM_8D_ENABLED", false);
        btn8dToggle.setAlpha(eightDActive ? 1.0f : 0.6f);
        btn8dToggle.setColorFilter(eightDActive ? accentColor : defaultColor);
        eightDLabel
                .setTextColor(eightDActive ? accentColor : ContextCompat.getColor(this, R.color.glass_text_secondary));
    }

    private float clampSpeed(float speed) {
        if (Float.isNaN(speed)) return 1.0f;
        float clamped = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        return Math.round(clamped * 20f) / 20f;
    }

    private String formatSpeedLabel(float speed) {
        float rounded = Math.round(speed * 100f) / 100f;
        if (Math.abs(rounded * 10f - Math.round(rounded * 10f)) < 0.01f) {
            return String.format(Locale.US, "%.1fx", rounded);
        }
        return String.format(Locale.US, "%.2fx", rounded);
    }

    private void applyPlaybackSpeed(float speed, boolean keepPitch) {
        currentPlaybackSpeed = clampSpeed(speed);
        keepOriginalPitch = keepPitch;

        sharedPreferences.edit()
                .putFloat("Playback_Speed", currentPlaybackSpeed)
                .putBoolean("Playback_Keep_Pitch", keepOriginalPitch)
                .apply();

        if (player != null) {
            float targetPitch = keepOriginalPitch ? 1.0f : currentPlaybackSpeed;
            player.setPlaybackParameters(new PlaybackParameters(currentPlaybackSpeed, targetPitch));
        }

        updateSpeedVisualState();
    }

    private void updateSpeedVisualState() {
        if (speedLabel == null || btnSpeed == null) return;
        speedLabel.setText(formatSpeedLabel(currentPlaybackSpeed));
        int accentColor = ContextCompat.getColor(this, R.color.accent_neon);
        int defaultColor = Color.WHITE;
        boolean isNonStandard = Math.abs(currentPlaybackSpeed - 1.0f) >= 0.01f;

        btnSpeed.setAlpha(isNonStandard ? 1.0f : 0.6f);
        btnSpeed.setColorFilter(isNonStandard ? accentColor : defaultColor);
        speedLabel.setTextColor(isNonStandard ? accentColor : ContextCompat.getColor(this, R.color.glass_text_secondary));
    }

    private void showSpeedDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_speed, null);
        dialog.setContentView(dialogView);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView currentValueView = dialogView.findViewById(R.id.speed_current_value);
        TextView modeSubtitleView = dialogView.findViewById(R.id.speed_mode_subtitle);
        TextView btnReset = dialogView.findViewById(R.id.btn_speed_reset);
        SeekBar speedSeekBar = dialogView.findViewById(R.id.speed_seekbar);
        SwitchCompat keepPitchSwitch = dialogView.findViewById(R.id.keep_pitch_switch);

        int[] presetIds = {
                R.id.preset_050, R.id.preset_075, R.id.preset_085, R.id.preset_100,
                R.id.preset_115, R.id.preset_125, R.id.preset_150, R.id.preset_200
        };
        float[] presetValues = {0.50f, 0.75f, 0.85f, 1.00f, 1.15f, 1.25f, 1.50f, 2.00f};
        TextView[] presetViews = new TextView[presetIds.length];
        for (int i = 0; i < presetIds.length; i++) {
            presetViews[i] = dialogView.findViewById(presetIds[i]);
        }

        int accentColor = ContextCompat.getColor(this, R.color.accent_neon);
        Runnable refreshDialogUI = () -> {
            currentValueView.setText(String.format(Locale.US, "%.2fx", currentPlaybackSpeed));
            modeSubtitleView.setText(keepOriginalPitch
                    ? "Clean Time-Stretch (Keep Pitch)"
                    : "TikTok Sped Up / Slowed (Tape Pitch)");
            int targetProgress = Math.round((currentPlaybackSpeed - MIN_SPEED) / SPEED_STEP);
            if (speedSeekBar.getProgress() != targetProgress) {
                speedSeekBar.setProgress(Math.max(0, Math.min(speedSeekBar.getMax(), targetProgress)));
            }
            for (int i = 0; i < presetViews.length; i++) {
                boolean selected = Math.abs(currentPlaybackSpeed - presetValues[i]) < 0.01f;
                presetViews[i].setTextColor(selected ? accentColor : Color.WHITE);
                presetViews[i].setAlpha(selected ? 1.0f : 0.78f);
            }
        };

        keepPitchSwitch.setChecked(keepOriginalPitch);
        refreshDialogUI.run();

        for (int i = 0; i < presetViews.length; i++) {
            final float targetSpeed = presetValues[i];
            presetViews[i].setOnClickListener(v -> {
                springClick(v);
                applyPlaybackSpeed(targetSpeed, keepOriginalPitch);
                refreshDialogUI.run();
            });
        }

        btnReset.setOnClickListener(v -> {
            springClick(v);
            applyPlaybackSpeed(1.0f, keepOriginalPitch);
            refreshDialogUI.run();
        });

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = clampSpeed(MIN_SPEED + progress * SPEED_STEP);
                currentValueView.setText(String.format(Locale.US, "%.2fx", speed));
                if (fromUser) {
                    for (int i = 0; i < presetViews.length; i++) {
                        boolean selected = Math.abs(speed - presetValues[i]) < 0.01f;
                        presetViews[i].setTextColor(selected ? accentColor : Color.WHITE);
                        presetViews[i].setAlpha(selected ? 1.0f : 0.78f);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float speed = clampSpeed(MIN_SPEED + seekBar.getProgress() * SPEED_STEP);
                applyPlaybackSpeed(speed, keepOriginalPitch);
                refreshDialogUI.run();
            }
        });

        keepPitchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            applyPlaybackSpeed(currentPlaybackSpeed, isChecked);
            refreshDialogUI.run();
        });

        dialog.show();
    }

    private void toggleBassBoost() {
        int currentBass = sharedPreferences.getInt("BassBoost_Strength", 0);
        int newBass = (currentBass > 0) ? 0 : 500; // Toggle off or 50%
        sharedPreferences.edit().putInt("BassBoost_Strength", newBass).apply();

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction("ACTION_SET_BASS_BOOST");
        intent.putExtra("enabled", newBass > 0);
        intent.putExtra("strength", (short) newBass);
        sendServiceCommand(intent);

        updateAudioEffectsVisualState();
        Toast.makeText(this, newBass > 0 ? "Bass Boost ON" : "Bass Boost OFF", Toast.LENGTH_SHORT).show();
    }

    private void toggle8dAudio() {
        boolean current8d = sharedPreferences.getBoolean("RHYTHM_8D_ENABLED", false);
        boolean new8d = !current8d;
        sharedPreferences.edit().putBoolean("RHYTHM_8D_ENABLED", new8d).apply();

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction("ACTION_SET_8D_AUDIO");
        intent.putExtra("enabled", new8d);
        sendServiceCommand(intent);

        updateAudioEffectsVisualState();
        Toast.makeText(this, new8d ? "8D Spatializer ON" : "8D Spatializer OFF", Toast.LENGTH_SHORT).show();
    }

    private void showSleepTimerDialog() {
        String[] options = { "Turn Off", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "45 Minutes",
                "60 Minutes" };
        int[] minutes = { 0, 5, 10, 15, 30, 45, 60 };

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    if (sleepTimer != null) {
                        sleepTimer.cancel();
                        sleepTimer = null;
                    }
                    if (which == 0) {
                        sleepTimerRemainingMs = 0;
                        sleepTimerLabel.setText("Sleep");
                        btnSleepTimer.setAlpha(0.6f);
                        btnSleepTimer.setColorFilter(Color.WHITE);
                        sleepTimerLabel.setTextColor(ContextCompat.getColor(this, R.color.glass_text_secondary));
                        Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show();
                    } else {
                        long timerMs = (long) minutes[which] * 60 * 1000;
                        startSleepTimer(timerMs);
                        Toast.makeText(this, "Sleep timer set for " + minutes[which] + " min", Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .show();
    }

    private void startSleepTimer(long durationMs) {
        if (sleepTimer != null)
            sleepTimer.cancel();

        int accentColor = ContextCompat.getColor(this, R.color.accent_neon);
        btnSleepTimer.setAlpha(1.0f);
        btnSleepTimer.setColorFilter(accentColor);
        sleepTimerLabel.setTextColor(accentColor);

        sleepTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sleepTimerRemainingMs = millisUntilFinished;
                long mins = millisUntilFinished / (1000 * 60);
                long secs = (millisUntilFinished / 1000) % 60;
                sleepTimerLabel.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                sleepTimerRemainingMs = 0;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                sleepTimerLabel.setText("Sleep");
                btnSleepTimer.setAlpha(0.6f);
                btnSleepTimer.setColorFilter(Color.WHITE);
                sleepTimerLabel.setTextColor(
                        ContextCompat.getColor(FullScreenPlayerActivity.this, R.color.glass_text_secondary));

                if (player != null && player.isPlaying()) {
                    player.pause();
                }
                Toast.makeText(FullScreenPlayerActivity.this, "Sleep timer finished. Playback paused.",
                        Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void handleABLoopClick() {
        if (player == null)
            return;
        long currentPos = player.getCurrentPosition();
        int accentColor = ContextCompat.getColor(this, R.color.accent_neon);

        if (abLoopState == AB_STATE_NONE) {
            abLoopState = AB_STATE_A_SET;
            abLoopPointA = currentPos;
            btnABLoop.setAlpha(0.8f);
            btnABLoop.setColorFilter(accentColor);
            abLoopStatus.setText("A: " + formatTime(abLoopPointA));
            abLoopStatus.setTextColor(accentColor);
            Toast.makeText(this, "Point A set at " + formatTime(abLoopPointA), Toast.LENGTH_SHORT).show();

        } else if (abLoopState == AB_STATE_A_SET) {
            if (currentPos > abLoopPointA) {
                abLoopState = AB_STATE_LOOPING;
                abLoopPointB = currentPos;
                btnABLoop.setAlpha(1.0f);
                btnABLoop.setColorFilter(accentColor);
                abLoopStatus.setText("A-B: " + formatTime(abLoopPointA) + " - " + formatTime(abLoopPointB));
                abLoopStatus.setTextColor(accentColor);
                player.seekTo(abLoopPointA);
                Toast.makeText(this, "Looping A-B", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Point B must be after Point A", Toast.LENGTH_SHORT).show();
            }

        } else {
            abLoopState = AB_STATE_NONE;
            abLoopPointA = -1;
            abLoopPointB = -1;
            btnABLoop.setAlpha(0.4f);
            btnABLoop.setColorFilter(Color.WHITE);
            abLoopStatus.setText("");
            abLoopStatus.setTextColor(ContextCompat.getColor(this, R.color.glass_text_secondary));
            Toast.makeText(this, "A-B Loop cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTrackPosition() {
        if (player != null) {
            int current = player.getCurrentMediaItemIndex() + 1;
            int total = player.getMediaItemCount();
            if (total > 0) {
                trackPositionText.setText(String.format(Locale.getDefault(), "Track %d of %d", current, total));
            } else {
                trackPositionText.setText("");
            }
        }
    }

    private void showEqualizerDialog() {
        initLocalEqualizer();
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_equalizer, null);
        dialog.setContentView(dialogView);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }

        SwitchCompat eqSwitch = dialogView.findViewById(R.id.eq_switch);
        Spinner presetSpinner = dialogView.findViewById(R.id.preset_spinner);
        LinearLayout bandsList = dialogView.findViewById(R.id.bands_list);
        SeekBar bassBoostSeekBar = dialogView.findViewById(R.id.bass_boost_seekbar);
        TextView bassBoostValue = dialogView.findViewById(R.id.bass_boost_value);
        SeekBar spatializerSeekBar = dialogView.findViewById(R.id.spatializer_seekbar);
        TextView spatializerValue = dialogView.findViewById(R.id.spatializer_value);
        SwitchCompat eightDSwitch = dialogView.findViewById(R.id.eight_d_switch);

        // Master Switch
        eqSwitch.setChecked(sharedPreferences.getBoolean("EQ_Enabled", true));
        eqSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("EQ_Enabled", isChecked).apply();
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_EQ_ENABLED");
            intent.putExtra("enabled", isChecked);
            sendServiceCommand(intent);
        });

        // Bass Boost
        int savedBassBoost = sharedPreferences.getInt("BassBoost_Strength", 0);
        bassBoostSeekBar.setProgress(savedBassBoost);
        bassBoostValue.setText(String.format(Locale.US, "%d%%", savedBassBoost / 10));
        bassBoostSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                bassBoostValue.setText(String.format(Locale.US, "%d%%", progress / 10));
                if (fromUser) {
                    sharedPreferences.edit().putInt("BassBoost_Strength", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateEffectInService("ACTION_SET_BASS_BOOST", seekBar.getProgress() > 0,
                        (short) seekBar.getProgress());
                updateAudioEffectsVisualState();
            }
        });

        // Spatializer
        int savedSpatializer = sharedPreferences.getInt("Spatializer_Strength", 0);
        spatializerSeekBar.setProgress(savedSpatializer);
        spatializerValue.setText(String.format(Locale.US, "%d%%", savedSpatializer / 10));
        spatializerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                spatializerValue.setText(String.format(Locale.US, "%d%%", progress / 10));
                if (fromUser) {
                    sharedPreferences.edit().putInt("Spatializer_Strength", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateEffectInService("ACTION_SET_SPATIALIZER", seekBar.getProgress() > 0,
                        (short) seekBar.getProgress());
            }
        });

        // 8D Audio
        eightDSwitch.setChecked(sharedPreferences.getBoolean("RHYTHM_8D_ENABLED", false));
        eightDSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("RHYTHM_8D_ENABLED", isChecked).apply();
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_8D_AUDIO");
            intent.putExtra("enabled", isChecked);
            sendServiceCommand(intent);
            updateAudioEffectsVisualState();
        });

        // Preset Spinner
        isSpinnerInitializing = true;
        List<String> presets = new ArrayList<>();
        presets.add("Custom");
        if (localEqualizer != null) {
            try {
                short numPresets = localEqualizer.getNumberOfPresets();
                for (short i = 0; i < numPresets; i++) {
                    presets.add(localEqualizer.getPresetName(i));
                }
            } catch (Exception ignored) {
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        int savedPreset = sharedPreferences.getInt("EQ_Preset", 0);
        if (savedPreset >= 0 && savedPreset < presets.size()) {
            presetSpinner.setSelection(savedPreset, false);
        }

        // Native Band Sliders
        short bands = 5;
        int minLevel = -1500;
        int maxLevel = 1500;
        String[] fallbackFreqLabels = { "60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz" };

        if (localEqualizer != null) {
            try {
                short eqBands = localEqualizer.getNumberOfBands();
                short[] range = localEqualizer.getBandLevelRange();
                if (eqBands > 0) {
                    bands = eqBands;
                }
                if (range != null && range.length >= 2 && range[1] > range[0]) {
                    minLevel = range[0];
                    maxLevel = range[1];
                }
            } catch (Exception ignored) {
            }
        }

        final int finalMinLevel = minLevel;
        final int finalMaxLevel = maxLevel;

        for (short i = 0; i < bands; i++) {
            final short bandIndex = i;
            View bandView = LayoutInflater.from(this).inflate(R.layout.item_eq_band, bandsList, false);

            TextView freqText = bandView.findViewById(R.id.band_frequency);
            SeekBar slider = bandView.findViewById(R.id.band_seekbar);
            final TextView levelText = bandView.findViewById(R.id.band_level);

            if (localEqualizer != null) {
                try {
                    int freq = localEqualizer.getCenterFreq(bandIndex) / 1000;
                    freqText.setText(freq >= 1000 ? (freq / 1000) + " kHz" : freq + " Hz");
                } catch (Exception e) {
                    freqText.setText(bandIndex < fallbackFreqLabels.length ? fallbackFreqLabels[bandIndex]
                            : "Band " + (bandIndex + 1));
                }
            } else {
                freqText.setText(bandIndex < fallbackFreqLabels.length ? fallbackFreqLabels[bandIndex]
                        : "Band " + (bandIndex + 1));
            }

            slider.setMax(Math.max(1, finalMaxLevel - finalMinLevel));

            int savedLevel = sharedPreferences.getInt("EQ_Band_" + bandIndex, 0);
            slider.setProgress(Math.max(0, Math.min(finalMaxLevel - finalMinLevel, savedLevel - finalMinLevel)));
            levelText.setText((savedLevel / 100) + " dB");

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int newLevel = progress + finalMinLevel;
                    levelText.setText((newLevel / 100) + " dB");
                    if (fromUser) {
                        presetSpinner.setSelection(0);
                        sharedPreferences.edit()
                                .putInt("EQ_Preset", 0)
                                .putInt("EQ_Band_" + bandIndex, newLevel)
                                .apply();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int newLevel = seekBar.getProgress() + finalMinLevel;
                    Intent intent = new Intent(FullScreenPlayerActivity.this, PlaybackService.class);
                    intent.setAction("ACTION_SET_EQ_BAND");
                    intent.putExtra("band", bandIndex);
                    intent.putExtra("level", (short) newLevel);
                    sendServiceCommand(intent);
                }
            });

            bandsList.addView(bandView);
        }

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSpinnerInitializing) {
                    isSpinnerInitializing = false;
                    return;
                }
                sharedPreferences.edit().putInt("EQ_Preset", position).apply();
                if (position > 0 && localEqualizer != null) {
                    try {
                        localEqualizer.usePreset((short) (position - 1));
                        short numBands = localEqualizer.getNumberOfBands();
                        short[] eqRange = localEqualizer.getBandLevelRange();
                        int min = (eqRange != null && eqRange.length >= 2) ? eqRange[0] : finalMinLevel;
                        for (short b = 0; b < numBands; b++) {
                            short level = localEqualizer.getBandLevel(b);
                            sharedPreferences.edit().putInt("EQ_Band_" + b, level).apply();
                            View bv = bandsList.getChildAt(b);
                            if (bv != null) {
                                ((SeekBar) bv.findViewById(R.id.band_seekbar)).setProgress(Math.max(0, level - min));
                                ((TextView) bv.findViewById(R.id.band_level)).setText((level / 100) + " dB");
                            }
                            Intent intent = new Intent(FullScreenPlayerActivity.this, PlaybackService.class);
                            intent.setAction("ACTION_SET_EQ_BAND");
                            intent.putExtra("band", b);
                            intent.putExtra("level", level);
                            sendServiceCommand(intent);
                        }
                    } catch (Exception e) {
                        Log.e("FullScreenPlayerActivity", "Error applying preset", e);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        dialog.show();
    }

    private void updateEffectInService(String action, boolean enabled, short strength) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra("enabled", enabled);
        intent.putExtra("strength", strength);
        sendServiceCommand(intent);
    }

    // ==========================================
    // LIFECYCLE & CONTROLLER
    // ==========================================

    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
    }

    @Override
    protected void onResume() {
        super.onResume();
        favoriteIds = loadFavoriteIds();
        updateAudioEffectsVisualState();
        if (player != null)
            syncUIWithPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(updateProgressAction);
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
        }
        playerListener = null;
        player = null;
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (sleepTimer != null) {
            sleepTimer.cancel();
            sleepTimer = null;
        }
        if (localEqualizer != null) {
            try {
                localEqualizer.release();
            } catch (Exception ignored) {
            }
            localEqualizer = null;
        }
    }

    private void initializeController() {
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        final ListenableFuture<MediaController> future = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture = future;
        future.addListener(() -> {
            if (isFinishing() || isDestroyed() || future != controllerFuture) {
                return;
            }
            try {
                MediaController connectedPlayer = future.get();
                if (isFinishing() || isDestroyed() || future != controllerFuture) {
                    return;
                }
                player = connectedPlayer;
                onControllerConnected();
            } catch (ExecutionException | InterruptedException | IllegalStateException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onControllerConnected() {
        if (player == null)
            return;
        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                updateUIForNowPlaying(mediaItem);
                updateTrackPosition();
                // Clear A-B loop on song change
                if (abLoopState != AB_STATE_NONE) {
                    abLoopState = AB_STATE_NONE;
                    abLoopPointA = -1;
                    abLoopPointB = -1;
                    btnABLoop.setAlpha(0.4f);
                    btnABLoop.setColorFilter(Color.WHITE);
                    abLoopStatus.setText("");
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                syncUIWithPlayer();
                updatePlayPauseIcon();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                if (isPlaying) {
                    handler.removeCallbacks(updateProgressAction);
                    handler.post(updateProgressAction);
                } else {
                    handler.removeCallbacks(updateProgressAction);
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                updateRepeatIcon(repeatMode);
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                updateShuffleIcon(shuffleModeEnabled);
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                currentPlaybackSpeed = clampSpeed(playbackParameters.speed);
                keepOriginalPitch = Math.abs(playbackParameters.pitch - 1.0f) < 0.01f
                        || Math.abs(playbackParameters.speed - 1.0f) < 0.01f
                        ? sharedPreferences.getBoolean("Playback_Keep_Pitch", true)
                        : false;
                updateSpeedVisualState();
            }
        };
        player.addListener(playerListener);

        syncUIWithPlayer();
        updateShuffleIcon(player.getShuffleModeEnabled());
        updateRepeatIcon(player.getRepeatMode());
        updateTrackPosition();

        // Sync saved speed/pitch with active player
        PlaybackParameters currentParams = player.getPlaybackParameters();
        if (Math.abs(currentParams.speed - currentPlaybackSpeed) >= 0.01f
                || Math.abs(currentParams.pitch - (keepOriginalPitch ? 1.0f : currentPlaybackSpeed)) >= 0.01f) {
            float targetPitch = keepOriginalPitch ? 1.0f : currentPlaybackSpeed;
            player.setPlaybackParameters(new PlaybackParameters(currentPlaybackSpeed, targetPitch));
        }
        updateSpeedVisualState();
    }

    private void syncUIWithPlayer() {
        if (player == null || isFinishing() || isDestroyed())
            return;
        updateUIForNowPlaying(player.getCurrentMediaItem());
        long currentPos = Math.max(0L, player.getCurrentPosition());
        long duration = player.getDuration();

        if (duration > 0) {
            seekBar.setMax(clampToInt(duration));
        } else {
            seekBar.setMax(100);
        }

        seekBar.setProgress(clampToInt(currentPos));
        updateTimers(currentPos, duration);
        updatePlayPauseIcon();
        updateTrackPosition();

        if (player.isPlaying()) {
            handler.removeCallbacks(updateProgressAction);
            handler.post(updateProgressAction);
        }
    }

    private void updateUIForNowPlaying(MediaItem mediaItem) {
        if (mediaItem != null) {
            String title = FormatUtils.cleanTitle(
                    mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : null, null);
            String artist = FormatUtils.cleanArtist(
                    mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : null);

            fullSongTitle.setText(title);
            fullArtistName.setText(artist);
            fullSongTitle.setSelected(true);
            fullArtistName.setSelected(true);
            btnFavNow.setImageResource(
                    favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            Uri uri = null;
            if (mediaItem.requestMetadata != null)
                uri = mediaItem.requestMetadata.mediaUri;
            loadAlbumArtAndPalette(uri);
        } else {
            fullSongTitle.setText("Select a song");
            fullArtistName.setText("");
            fullSongTitle.setSelected(false);
            fullArtistName.setSelected(false);
            fullAlbumArt.setImageResource(R.drawable.cover_ep);
            backgroundBlur.setPalette(null);
            activeArtworkRequestKey = null;
        }
    }

    private void loadAlbumArtAndPalette(Uri uri) {
        if (uri == null) {
            fullAlbumArt.setImageResource(R.drawable.cover_ep);
            backgroundBlur.setPalette(null);
            activeArtworkRequestKey = null;
            return;
        }

        String requestKey = uri.toString();
        if (requestKey.equals(activeArtworkRequestKey) && fullAlbumArt.getDrawable() != null) {
            return;
        }

        activeArtworkRequestKey = requestKey;

        android.graphics.Bitmap cached = ArtworkLoader.getCachedBitmap(uri, 480);
        if (cached != null) {
            fullAlbumArt.setImageBitmap(cached);
        }

        ArtworkLoader.loadBitmapAndPalette(getApplicationContext().getContentResolver(), uri, 480,
                this::paletteToHuePalette, (bitmap, palette) -> {
                    if (isFinishing() || isDestroyed() || !requestKey.equals(activeArtworkRequestKey)) {
                        return;
                    }
                    if (bitmap != null && !bitmap.isRecycled()) {
                        fullAlbumArt.setImageBitmap(bitmap);
                        backgroundBlur.setPalette(palette);
                    } else {
                        fullAlbumArt.setImageResource(R.drawable.cover_ep);
                        backgroundBlur.setPalette(null);
                    }
                });
    }

    private int[] paletteToHuePalette(Palette palette) {
        if (palette == null)
            return null;

        int dominant = palette.getDominantColor(Color.TRANSPARENT);
        if (dominant == Color.TRANSPARENT) {
            dominant = palette.getVibrantColor(Color.TRANSPARENT);
        }
        if (dominant == Color.TRANSPARENT) {
            return null;
        }

        float[] hsv = new float[3];
        Color.colorToHSV(dominant, hsv);
        float saturation = Math.max(0.45f, hsv[1]);
        float value = Math.max(0.55f, hsv[2]);

        return new int[] {
                Color.HSVToColor(new float[] { normalizeHue(hsv[0]), saturation, value }),
                Color.HSVToColor(
                        new float[] { normalizeHue(hsv[0] + 22f), saturation * 0.9f, Math.min(1f, value + 0.1f) }),
                Color.HSVToColor(new float[] { normalizeHue(hsv[0] - 22f), Math.min(1f, saturation + 0.1f), value }),
                Color.HSVToColor(new float[] { normalizeHue(hsv[0] + 180f), Math.max(0.35f, saturation * 0.75f),
                        Math.max(0.45f, value * 0.9f) })
        };
    }

    private float normalizeHue(float hue) {
        float normalized = hue % 360f;
        if (normalized < 0f)
            normalized += 360f;
        return normalized;
    }

    private void configureTransitions() {
        TransitionSet sharedTransition = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeTransform())
                .addTransition(new ChangeImageTransform())
                .setDuration(320L)
                .setInterpolator(new DecelerateInterpolator());
        getWindow().setSharedElementsUseOverlay(false);
        getWindow().setSharedElementEnterTransition(sharedTransition);
        getWindow().setSharedElementReturnTransition(sharedTransition);
        getWindow().setEnterTransition(new Fade().setDuration(220L));
        getWindow().setReturnTransition(new Fade().setDuration(180L));
    }

    private void updatePlayPauseIcon() {
        if (player != null && player.isPlaying())
            btnPlayPause.setImageResource(R.drawable.ic_modern_pause);
        else
            btnPlayPause.setImageResource(R.drawable.ic_modern_play);
    }

    private void updateShuffleIcon(boolean enabled) {
        btnShuffle.setAlpha(enabled ? 1.0f : 0.4f);
    }

    private void updateRepeatIcon(int mode) {
        if (mode == Player.REPEAT_MODE_OFF) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            btnRepeat.setAlpha(0.4f);
        } else if (mode == Player.REPEAT_MODE_ALL) {
            btnRepeat.setImageResource(R.drawable.ic_repeat);
            btnRepeat.setAlpha(1.0f);
        } else if (mode == Player.REPEAT_MODE_ONE) {
            btnRepeat.setImageResource(R.drawable.ic_repeat_one);
            btnRepeat.setAlpha(1.0f);
        }
    }

    private void updateTimers(long currentPos, long duration) {
        if (duration < 0)
            duration = 0;
        currentTimeText.setText(formatTime(currentPos));
        remainingTimeText.setText("-" + formatTime(Math.max(0, duration - currentPos)));
    }

    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}