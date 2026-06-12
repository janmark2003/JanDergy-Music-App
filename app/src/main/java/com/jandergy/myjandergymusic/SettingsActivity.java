package com.jandergy.myjandergymusic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private View settingsContent, settingsActions;
    private ImageView characterImg;

    private Equalizer localEqualizer;
    private SharedPreferences sharedPreferences;
    private Set<String> favoriteIds = new HashSet<>();
    private boolean isSpinnerInitializing = true;

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

        settingsActions = findViewById(R.id.settings_actions);

        findViewById(R.id.btn_equalizer).setOnClickListener(v -> showEqualizerDialog());

        findViewById(R.id.btn_community).setOnClickListener(v ->
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.contact_link).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:janmarkthebluedragon@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "JanDergy Music - Concern");
            startActivity(Intent.createChooser(emailIntent, "Send email"));
        });

        nowPlayingTitle = findViewById(R.id.now_playing_title);
        nowPlayingArtist = findViewById(R.id.now_playing_artist);
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

        androidx.cardview.widget.CardView playerControls = findViewById(R.id.player_controls);
        playerControls.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, FullScreenPlayerActivity.class);
            startActivity(intent);
        });

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

    private void initLocalEqualizer() {
        if (localEqualizer != null) return;
        try {
            localEqualizer = new Equalizer(0, 0);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error initializing local equalizer", e);
        }
    }

    private void updateEffectInService(String action, boolean enabled, short strength) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra("enabled", enabled);
        intent.putExtra("strength", strength);
        startService(intent);
    }

    private void showEqualizerDialog() {
        if (localEqualizer == null) {
            Toast.makeText(this, "Equalizer not available", Toast.LENGTH_SHORT).show();
            return;
        }

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

        // Master Switch
        eqSwitch.setChecked(sharedPreferences.getBoolean("EQ_Enabled", true));
        eqSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("EQ_Enabled", isChecked).apply();
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_EQ_ENABLED");
            intent.putExtra("enabled", isChecked);
            startService(intent);
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
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateEffectInService("ACTION_SET_BASS_BOOST", true, (short) seekBar.getProgress());
            }
        });

        // Spatial Audio
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
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateEffectInService("ACTION_SET_SPATIALIZER", true, (short) seekBar.getProgress());
            }
        });

        // Preset Spinner
        isSpinnerInitializing = true;
        short numPresets = localEqualizer.getNumberOfPresets();
        List<String> presets = new ArrayList<>();
        presets.add("Custom");
        for (short i = 0; i < numPresets; i++) {
            presets.add(localEqualizer.getPresetName(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        int savedPreset = sharedPreferences.getInt("EQ_Preset", 0);
        if (savedPreset < presets.size()) {
            presetSpinner.setSelection(savedPreset, false);
        }

        // Native Band Sliders
        short bands = localEqualizer.getNumberOfBands();
        short[] range = localEqualizer.getBandLevelRange();
        int minLevel = range[0];
        int maxLevel = range[1];

        for (short i = 0; i < bands; i++) {
            final short bandIndex = i;
            View bandView = LayoutInflater.from(this).inflate(R.layout.item_eq_band, bandsList, false);

            TextView freqText = bandView.findViewById(R.id.band_frequency);
            SeekBar slider = bandView.findViewById(R.id.band_seekbar);
            final TextView levelText = bandView.findViewById(R.id.band_level);

            int freq = localEqualizer.getCenterFreq(bandIndex) / 1000;
            freqText.setText(freq >= 1000 ? (freq / 1000) + " kHz" : freq + " Hz");

            slider.setMax(maxLevel - minLevel);

            int savedLevel = sharedPreferences.getInt("EQ_Band_" + bandIndex, 0);
            slider.setProgress(savedLevel - minLevel);
            levelText.setText((savedLevel / 100) + " dB");

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int newLevel = progress + minLevel;
                    levelText.setText((newLevel / 100) + " dB");
                    if (fromUser) {
                        presetSpinner.setSelection(0);
                        sharedPreferences.edit().putInt("EQ_Preset", 0).apply();
                        sharedPreferences.edit().putInt("EQ_Band_" + bandIndex, newLevel).apply();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int newLevel = seekBar.getProgress() + minLevel;
                    Intent intent = new Intent(SettingsActivity.this, PlaybackService.class);
                    intent.setAction("ACTION_SET_EQ_BAND");
                    intent.putExtra("band", bandIndex);
                    intent.putExtra("level", (short) newLevel);
                    startService(intent);
                }
            });

            bandsList.addView(bandView);
        }

        // Spinner listener (after bands are inflated so preset apply works)
        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSpinnerInitializing) {
                    isSpinnerInitializing = false;
                    return;
                }
                sharedPreferences.edit().putInt("EQ_Preset", position).apply();
                if (position > 0) {
                    try {
                        localEqualizer.usePreset((short) (position - 1));
                        short numBands = localEqualizer.getNumberOfBands();
                        short[] eqRange = localEqualizer.getBandLevelRange();
                        int min = eqRange[0];
                        for (short b = 0; b < numBands; b++) {
                            short level = localEqualizer.getBandLevel(b);
                            sharedPreferences.edit().putInt("EQ_Band_" + b, level).apply();
                            View bv = bandsList.getChildAt(b);
                            if (bv != null) {
                                ((SeekBar) bv.findViewById(R.id.band_seekbar)).setProgress(level - min);
                                ((TextView) bv.findViewById(R.id.band_level)).setText((level / 100) + " dB");
                            }
                            Intent intent = new Intent(SettingsActivity.this, PlaybackService.class);
                            intent.setAction("ACTION_SET_EQ_BAND");
                            intent.putExtra("band", b);
                            intent.putExtra("level", level);
                            startService(intent);
                        }
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "Error applying preset", e);
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        dialog.show();
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
        settingsActions.animate().alpha(1f).setDuration(1200).setStartDelay(300).start();
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
        handler.removeCallbacks(updateProgressAction);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localEqualizer != null) {
            localEqualizer.release();
            localEqualizer = null;
        }
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
        initLocalEqualizer();

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

            if (!nowPlayingTitle.getText().toString().equals(title)) {
                nowPlayingTitle.setText(title);
            }
            if (!nowPlayingArtist.getText().toString().equals(artist)) {
                nowPlayingArtist.setText(artist);
            }

            btnFavNow.setImageResource(favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        } else {
            if (!nowPlayingTitle.getText().toString().equals("Select a song")) {
                nowPlayingTitle.setText("Select a song");
                nowPlayingArtist.setText("");
            }
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