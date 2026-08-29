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
import androidx.annotation.OptIn;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Arrays;
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

    private View settingsContent, settingsActions, flyerCard;
    private ViewPager2 flyerViewPager;
    private final Handler slideHandler = new Handler(Looper.getMainLooper());
    private Runnable slideRunnable;
    private static final long AUTO_SLIDE_INTERVAL_MS = 3500;
    private android.widget.FrameLayout musicNotesContainer;
    private long lastBannerClickTime = 0;
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
                
                if (duration > 0 && seekBar.getMax() != (int) duration) {
                    seekBar.setMax((int) duration);
                }
                
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

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet("Favorites", new HashSet<>()));

        initUI();
        applyInitialState();
        populateSettings();
        startFadeInAnimations();
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

            nowPlayingTitle.setText(FormatUtils.cleanTitle(title, null));
            nowPlayingArtist.setText(FormatUtils.cleanArtist(artist));

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
                btnFavNow.setImageResource(favoriteIds.contains(mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            }
        }
    }

    private void initUI() {
        settingsContent = findViewById(R.id.settings_content);
        characterImg = findViewById(R.id.character_img);
        settingsActions = findViewById(R.id.settings_actions);
        flyerCard = findViewById(R.id.flyer_card);
        flyerViewPager = findViewById(R.id.flyer_view_pager);
        musicNotesContainer = findViewById(R.id.music_notes_container);

        if (flyerViewPager != null) {
            List<Integer> flyerImages = Arrays.asList(
                    R.drawable.flyer_1,
                    R.drawable.flyer_2,
                    R.drawable.flyer_3,
                    R.drawable.flyer_4,
                    R.drawable.flyer_5,
                    R.drawable.flyer_6
            );
            FlyerPagerAdapter adapter = new FlyerPagerAdapter(flyerImages, v -> triggerBannerAnimation());
            flyerViewPager.setAdapter(adapter);

            slideRunnable = new Runnable() {
                @Override
                public void run() {
                    if (flyerViewPager != null && flyerViewPager.getAdapter() != null) {
                        int current = flyerViewPager.getCurrentItem();
                        int count = flyerViewPager.getAdapter().getItemCount();
                        if (count > 0) {
                            flyerViewPager.setCurrentItem((current + 1) % count, true);
                        }
                        slideHandler.postDelayed(this, AUTO_SLIDE_INTERVAL_MS);
                    }
                }
            };

            flyerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrollStateChanged(int state) {
                    super.onPageScrollStateChanged(state);
                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        slideHandler.removeCallbacks(slideRunnable);
                    } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        slideHandler.removeCallbacks(slideRunnable);
                        slideHandler.postDelayed(slideRunnable, AUTO_SLIDE_INTERVAL_MS);
                    }
                }
            });
        }

        findViewById(R.id.btn_equalizer).setOnClickListener(v -> showEqualizerDialog());

        findViewById(R.id.btn_community).setOnClickListener(v -> showCommunityDialog());

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
            Log.w("SettingsActivity", "Session 0 equalizer restricted on this device, using dynamic fallback", e);
            localEqualizer = null;
        }
    }

    private void updateEffectInService(String action, boolean enabled, short strength) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        intent.putExtra("enabled", enabled);
        intent.putExtra("strength", strength);
        startService(intent);
    }

    private void showCommunityDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_community, null);
        dialog.setContentView(dialogView);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }

        dialogView.findViewById(R.id.card_bluesky).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://bsky.app/profile/janmarkbluederg.bsky.social"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.card_telegram).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/JanmarkTheBlueDragon"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showEqualizerDialog() {
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

        // 8D Audio
        eightDSwitch.setChecked(sharedPreferences.getBoolean("RHYTHM_8D_ENABLED", false));
        eightDSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("RHYTHM_8D_ENABLED", isChecked).apply();
            Intent intent = new Intent(this, PlaybackService.class);
            intent.setAction("ACTION_SET_8D_AUDIO");
            intent.putExtra("enabled", isChecked);
            startService(intent);
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
            } catch (Exception ignored) {}
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        int savedPreset = sharedPreferences.getInt("EQ_Preset", 0);
        if (savedPreset < presets.size()) {
            presetSpinner.setSelection(savedPreset, false);
        }

        // Native Band Sliders
        short bands = 5;
        int minLevel = -1500;
        int maxLevel = 1500;
        String[] fallbackFreqLabels = {"60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz"};

        if (localEqualizer != null) {
            try {
                bands = localEqualizer.getNumberOfBands();
                short[] range = localEqualizer.getBandLevelRange();
                minLevel = range[0];
                maxLevel = range[1];
            } catch (Exception ignored) {}
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
                    freqText.setText(bandIndex < fallbackFreqLabels.length ? fallbackFreqLabels[bandIndex] : "Band " + (bandIndex + 1));
                }
            } else {
                freqText.setText(bandIndex < fallbackFreqLabels.length ? fallbackFreqLabels[bandIndex] : "Band " + (bandIndex + 1));
            }

            slider.setMax(finalMaxLevel - finalMinLevel);

            int savedLevel = sharedPreferences.getInt("EQ_Band_" + bandIndex, 0);
            slider.setProgress(savedLevel - finalMinLevel);
            levelText.setText((savedLevel / 100) + " dB");

            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int newLevel = progress + finalMinLevel;
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
                    int newLevel = seekBar.getProgress() + finalMinLevel;
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
                if (position > 0 && localEqualizer != null) {
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
        flyerCard.animate().alpha(1f).setDuration(1000).setStartDelay(200).start();
        settingsActions.animate().alpha(1f).setDuration(1200).setStartDelay(300).start();
        settingsContent.animate().alpha(1f).setDuration(1500).setStartDelay(500).start();
        characterImg.animate().alpha(1f).setDuration(1500).setStartDelay(800).start();
        View playerControls = findViewById(R.id.player_controls);
        if (playerControls != null) {
            playerControls.animate().alpha(1f).setDuration(1500).setStartDelay(500).start();
        }
    }

    private void triggerBannerAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBannerClickTime < 5000) {
            return;
        }
        lastBannerClickTime = currentTime;

        animateBubblyClick(flyerCard);

        for (int i = 0; i < 20; i++) {
            showAnimatedNoteFromBanner();
        }
    }

    private void animateBubblyClick(View v) {
        v.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(400)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f))
                            .start();
                })
                .start();
    }

    private void showAnimatedNoteFromBanner() {
        ImageView note = new ImageView(this);
        note.setImageResource(R.drawable.ic_music_note);

        int[] colors = {0xFFFF1493, 0xFF00BFFF, 0xFFFFD700, 0xFF32CD32, 0xFFFF4500, 0xFF9370DB};
        int color = colors[(int) (Math.random() * colors.length)];
        note.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

        int size = (int) (24 + Math.random() * 32);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                (int) (size * getResources().getDisplayMetrics().density),
                (int) (size * getResources().getDisplayMetrics().density));
        note.setLayoutParams(params);

        musicNotesContainer.addView(note);

        // Get center of the banner
        int[] location = new int[2];
        flyerCard.getLocationInWindow(location);
        float startX = location[0] + flyerCard.getWidth() / 2f - params.width / 2f;
        float startY = location[1] + flyerCard.getHeight() / 2f - params.height / 2f;

        // Note: locationInWindow might need offset if container is not full screen or has padding
        // Since music_notes_container is match_parent, we can just use raw coords if it's the root child.
        // Better: use relative to container
        int[] containerLoc = new int[2];
        musicNotesContainer.getLocationInWindow(containerLoc);
        startX -= containerLoc[0];
        startY -= containerLoc[1];

        note.setX(startX);
        note.setY(startY);
        note.setAlpha(0f);
        note.setScaleX(0f);
        note.setScaleY(0f);

        float angle = (float) (Math.random() * 2 * Math.PI);
        float dist = (float) (150 + Math.random() * 400);
        float endX = startX + (float) Math.cos(angle) * dist;
        float endY = startY + (float) Math.sin(angle) * dist;

        note.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .translationX(endX)
                .translationY(endY)
                .rotation((float) (Math.random() * 720 - 360))
                .setDuration(800 + (long)(Math.random() * 600))
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    // Pop animation
                    note.animate()
                            .scaleX(1.8f)
                            .scaleY(1.8f)
                            .alpha(0f)
                            .setDuration(250)
                            .setInterpolator(new android.view.animation.AccelerateInterpolator())
                            .withEndAction(() -> musicNotesContainer.removeView(note))
                            .start();
                })
                .start();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
    }

    @Override
    public void onResume() {
        super.onResume();
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet("Favorites", new HashSet<>()));
        if (player != null) {
            syncUIWithPlayer();
            nowPlayingTitle.setSelected(true);
            nowPlayingArtist.setSelected(true);
        }
        if (slideRunnable != null) {
            slideHandler.removeCallbacks(slideRunnable);
            slideHandler.postDelayed(slideRunnable, AUTO_SLIDE_INTERVAL_MS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (slideRunnable != null) {
            slideHandler.removeCallbacks(slideRunnable);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        handler.removeCallbacks(updateProgressAction);
        if (slideRunnable != null) {
            slideHandler.removeCallbacks(slideRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slideRunnable != null) {
            slideHandler.removeCallbacks(slideRunnable);
        }
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
        } else {
            seekBar.setMax(100);
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
            String title = FormatUtils.cleanTitle(mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : null, null);
            String artist = FormatUtils.cleanArtist(mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : null);

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

}