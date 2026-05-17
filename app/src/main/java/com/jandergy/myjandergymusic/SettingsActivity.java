package com.jandergy.myjandergymusic;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SettingsActivity extends AppCompatActivity {

    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;

    private MarqueeTextView nowPlayingTitle, nowPlayingArtist;
    private TextView currentTimeText, remainingTimeText;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat, btnFavNow;
    private ImageButton btnPrev, btnNext;

    private View settingsContent, eqContainer;
    private ImageView characterImg;
    private LinearLayout bandsList;
    
    private Spinner presetSpinner;
    private SwitchCompat eqSwitch;
    private SeekBar bassBoostSeekBar, spatializerSeekBar;
    private TextView bassBoostValue, spatializerValue;

    private Equalizer localEqualizer; // Only for metadata query
    private short numberOfBands;
    private final int[] uiFrequencies = {31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
    private final float[] uiBandLevels = new float[10];

    private SharedPreferences sharedPreferences;
    private Set<String> favoriteIds = new HashSet<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long currentPos = player.getCurrentPosition();
                long duration = player.getDuration();
                seekBar.setProgress((int) currentPos);
                updateTimers(currentPos, duration);
                handler.postDelayed(this, 200);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet("Favorites", new HashSet<>()));

        initUI();
        populateSettings();
        startFadeInAnimations();
    }

    private void initUI() {
        settingsContent = findViewById(R.id.settings_content);
        characterImg = findViewById(R.id.character_img);
        
        eqContainer = findViewById(R.id.eq_container);
        bandsList = findViewById(R.id.bands_list);
        presetSpinner = findViewById(R.id.preset_spinner);
        eqSwitch = findViewById(R.id.eq_switch);
        
        bassBoostSeekBar = findViewById(R.id.bass_boost_seekbar);
        bassBoostValue = findViewById(R.id.bass_boost_value);
        spatializerSeekBar = findViewById(R.id.spatializer_seekbar);
        spatializerValue = findViewById(R.id.spatializer_value);

        eqSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("EQ_Enabled", isChecked).apply();
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_EQ_ENABLED");
            intent.putExtra("enabled", isChecked);
            startService(intent);
        });

        bassBoostSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                bassBoostValue.setText(String.format(Locale.US, "%d%%", progress / 10));
                if (fromUser) {
                    sharedPreferences.edit().putInt("BassBoost_Strength", progress).apply();
                    updateEffectInService("ACTION_SET_BASS_BOOST", true, (short) progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        spatializerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                spatializerValue.setText(String.format(Locale.US, "%d%%", progress / 10));
                if (fromUser) {
                    sharedPreferences.edit().putInt("Spatializer_Strength", progress).apply();
                    updateEffectInService("ACTION_SET_SPATIALIZER", true, (short) progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        nowPlayingTitle = findViewById(R.id.now_playing_title);
        nowPlayingArtist = findViewById(R.id.now_playing_artist);
        
        // Ensure marquee can work
        nowPlayingTitle.setSelected(true);
        nowPlayingArtist.setSelected(true);

        currentTimeText = findViewById(R.id.current_time);
        remainingTimeText = findViewById(R.id.remaining_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnFavNow = findViewById(R.id.btn_fav_now);

        btnPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) player.pause();
            else player.play();
        });

        btnNext.setOnClickListener(v -> { if (player != null) player.seekToNext(); });
        btnPrev.setOnClickListener(v -> { if (player != null) player.seekToPrevious(); });

        btnShuffle.setOnClickListener(v -> {
            if (player != null) player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
        });

        btnRepeat.setOnClickListener(v -> {
            if (player != null) {
                int mode = player.getRepeatMode();
                switch (mode) {
                    case Player.REPEAT_MODE_OFF:
                        player.setRepeatMode(Player.REPEAT_MODE_ONE);
                        break;
                    case Player.REPEAT_MODE_ONE:
                        player.setRepeatMode(Player.REPEAT_MODE_ALL);
                        break;
                    case Player.REPEAT_MODE_ALL:
                        player.setRepeatMode(Player.REPEAT_MODE_OFF);
                        break;
                }
            }
        });

        btnFavNow.setOnClickListener(v -> {
            MediaItem item = (player != null) ? player.getCurrentMediaItem() : null;
            if (item != null) {
                String mediaId = item.mediaId;
                if (favoriteIds.contains(mediaId)) {
                    favoriteIds.remove(mediaId);
                } else {
                    favoriteIds.add(mediaId);
                }
                sharedPreferences.edit().putStringSet("Favorites", favoriteIds).apply();
                btnFavNow.setImageResource(favoriteIds.contains(mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    updateTimers(progress, player.getDuration());
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @UnstableApi
    private void setupEqualizer(int sessionId) {
        if (localEqualizer != null) return;

        try {
            localEqualizer = new Equalizer(0, sessionId);
            numberOfBands = localEqualizer.getNumberOfBands();

            boolean isEnabled = sharedPreferences.getBoolean("EQ_Enabled", true);
            eqSwitch.setChecked(isEnabled);

            int savedBassBoost = sharedPreferences.getInt("BassBoost_Strength", 0);
            bassBoostSeekBar.setProgress(savedBassBoost);
            bassBoostValue.setText(String.format(Locale.US, "%d%%", savedBassBoost / 10));
            updateEffectInService("ACTION_SET_BASS_BOOST", true, (short) savedBassBoost);

            int savedSpatializer = sharedPreferences.getInt("Spatializer_Strength", 0);
            spatializerSeekBar.setProgress(savedSpatializer);
            spatializerValue.setText(String.format(Locale.US, "%d%%", savedSpatializer / 10));
            updateEffectInService("ACTION_SET_SPATIALIZER", true, (short) savedSpatializer);

            setupPresetSpinner();
            setupBandSliders();

        } catch (Exception e) {
            Log.e("SettingsActivity", "Error setting up equalizer", e);
            eqContainer.setVisibility(View.GONE);
        }
    }

    private void updateEffectInService(String action, boolean enabled, short strength) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra("enabled", enabled);
        intent.putExtra("strength", strength);
        startService(intent);
    }

    private void setupPresetSpinner() {
        if (localEqualizer == null) return;
        short numPresets = localEqualizer.getNumberOfPresets();
        String[] presets = new String[numPresets];
        for (short i = 0; i < numPresets; i++) {
            presets[i] = localEqualizer.getPresetName(i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        int savedPreset = sharedPreferences.getInt("EQ_Preset", 0);
        if (savedPreset < numPresets) {
            presetSpinner.setSelection(savedPreset);
        }

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sharedPreferences.edit().putInt("EQ_Preset", position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupBandSliders() {
        if (localEqualizer == null) return;
        
        bandsList.removeAllViews();
        for (int i = 0; i < uiFrequencies.length; i++) {
            final int bandIndex = i;
            View bandView = LayoutInflater.from(this).inflate(R.layout.item_eq_band, bandsList, false);

            TextView freqText = bandView.findViewById(R.id.band_frequency);
            SeekBar slider = bandView.findViewById(R.id.band_seekbar);
            final TextView levelText = bandView.findViewById(R.id.band_level);

            String freqStr = (uiFrequencies[i] < 1000) ? uiFrequencies[i] + " Hz" : (uiFrequencies[i] / 1000) + " kHz";
            freqText.setText(freqStr);
            slider.setMax(3000); // -15.0 to +15.0 dB, 0.01 step resolution
            
            float savedLevel = sharedPreferences.getFloat("UI_EQ_Band_" + bandIndex, 0f);
            uiBandLevels[bandIndex] = savedLevel;
            slider.setProgress((int) ((savedLevel + 15) * 100));
            levelText.setText(String.format(Locale.US, "%.1f dB", savedLevel));

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float level = (progress / 100f) - 15f;
                    uiBandLevels[bandIndex] = level;
                    levelText.setText(String.format(Locale.US, "%.1f dB", level));
                    if (fromUser) {
                        applyInterpolatedEQ();
                        sharedPreferences.edit().putFloat("UI_EQ_Band_" + bandIndex, level).apply();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandsList.addView(bandView);
        }
        applyInterpolatedEQ();
    }

    private void applyInterpolatedEQ() {
        if (localEqualizer == null) return;
        short[] hardwareLevels = interpolateBands(uiBandLevels, numberOfBands);
        
        for (short i = 0; i < numberOfBands; i++) {
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_EQ_BAND");
            intent.putExtra("band", i);
            intent.putExtra("level", hardwareLevels[i]);
            startService(intent);
        }
    }

    private short[] interpolateBands(float[] uiLevels, int hardwareBands) {
        short[] result = new short[hardwareBands];
        if (hardwareBands == 5) {
            // Simplified mapping for standard 5-band hardware
            result[0] = (short) ((uiLevels[0] * 0.3f + uiLevels[1] * 0.4f + uiLevels[2] * 0.3f) * 100);
            result[1] = (short) ((uiLevels[3] * 0.5f + uiLevels[4] * 0.5f) * 100);
            result[2] = (short) ((uiLevels[5] * 0.5f + uiLevels[6] * 0.5f) * 100);
            result[3] = (short) ((uiLevels[7] * 0.5f + uiLevels[8] * 0.5f) * 100);
            result[4] = (short) (uiLevels[9] * 100);
        } else {
            // Linear interpolation fallback
            float ratio = (uiLevels.length - 1) / (float) (hardwareBands - 1);
            for (int i = 0; i < hardwareBands; i++) {
                float srcPos = i * ratio;
                int low = (int) srcPos;
                int high = Math.min(low + 1, uiLevels.length - 1);
                float weight = srcPos - low;
                result[i] = (short) ((uiLevels[low] * (1 - weight) + uiLevels[high] * weight) * 100);
            }
        }
        return result;
    }

    private void populateSettings() {
        TextView appVerText = findViewById(R.id.app_version);
        TextView deviceModelText = findViewById(R.id.device_model);
        TextView androidVerText = findViewById(R.id.android_version);

        String appVersion = "1.0";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}

        appVerText.setText(String.format(Locale.US, "App Version: %s", appVersion));
        deviceModelText.setText(String.format(Locale.US, "Device Model: %s", Build.MODEL));
        androidVerText.setText(String.format(Locale.US, "Android Version: %s", Build.VERSION.RELEASE));
    }

    private void startFadeInAnimations() {
        eqContainer.animate().alpha(1f).setDuration(1200).setStartDelay(300).start();
        settingsContent.animate().alpha(1f).setDuration(1500).setStartDelay(500).start();
        characterImg.animate().alpha(1f).setDuration(1500).setStartDelay(800).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
    }

    @Override
    protected void onResume() {
        super.onResume();
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet("Favorites", new HashSet<>()));
        if (player != null) {
            syncUIWithPlayer();
            nowPlayingTitle.setSelected(true);
            nowPlayingArtist.setSelected(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        if (localEqualizer != null) {
            localEqualizer.release();
            localEqualizer = null;
        }
        handler.removeCallbacks(updateProgressAction);
    }

    @UnstableApi
    private void initializeController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                player = controllerFuture.get();
                onControllerConnected();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @UnstableApi
    private void onControllerConnected() {
        int sessionId = player.getSessionExtras().getInt("audio_session_id", 0);
        setupEqualizer(sessionId);

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                updateUIForNowPlaying(mediaItem);
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
        });

        syncUIWithPlayer();
        updateShuffleIcon(player.getShuffleModeEnabled());
        updateRepeatIcon(player.getRepeatMode());
        
        nowPlayingTitle.setSelected(true);
        nowPlayingArtist.setSelected(true);
    }

    private void syncUIWithPlayer() {
        if (player == null) return;
        updateUIForNowPlaying(player.getCurrentMediaItem());
        
        long currentPos = player.getCurrentPosition();
        long duration = player.getDuration();
        if (duration > 0) {
            seekBar.setMax((int) duration);
        }
        seekBar.setProgress((int) currentPos);
        updateTimers(currentPos, duration);

        updatePlayPauseIcon();

        if (player.isPlaying()) {
            handler.removeCallbacks(updateProgressAction);
            handler.post(updateProgressAction);
        }
    }

    private void updateUIForNowPlaying(MediaItem mediaItem) {
        if (mediaItem != null) {
            String title = mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : "Unknown Title";
            String artist = mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : "Unknown Artist";

            nowPlayingTitle.setText(title);
            nowPlayingArtist.setText(artist);
            
            btnFavNow.setImageResource(favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        } else {
            nowPlayingTitle.setText("Select a song");
            nowPlayingArtist.setText("");
            btnFavNow.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    private void updatePlayPauseIcon() {
        if (player != null && player.isPlaying()) btnPlayPause.setImageResource(R.drawable.ic_modern_pause);
        else btnPlayPause.setImageResource(R.drawable.ic_modern_play);
    }

    private void updateShuffleIcon(boolean enabled) {
        btnShuffle.setAlpha(enabled ? 1.0f : 0.4f);
    }

    private void updateRepeatIcon(int mode) {
        btnRepeat.setAlpha(mode == Player.REPEAT_MODE_OFF ? 0.4f : 1.0f);
    }

    private void updateTimers(long currentPos, long durationMs) {
        if (durationMs < 0) durationMs = 0;
        currentTimeText.setText(formatTime(currentPos));
        remainingTimeText.setText("-" + formatTime(Math.max(0, durationMs - currentPos)));
    }

    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        if (hours > 0) {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
        super.onBackPressed();
    }
}
