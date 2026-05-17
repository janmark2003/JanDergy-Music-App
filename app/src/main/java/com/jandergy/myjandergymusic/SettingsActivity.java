package com.jandergy.myjandergymusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SettingsActivity extends AppCompatActivity {

    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;

    private TextView nowPlayingTitle, nowPlayingArtist, currentTimeText, remainingTimeText;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat, btnFavNow;
    private ImageButton btnPrev, btnNext;

    private View settingsContent;
    private ImageView characterImg;

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
                handler.postDelayed(this, 1000);
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

        nowPlayingTitle = findViewById(R.id.now_playing_title);
        nowPlayingArtist = findViewById(R.id.now_playing_artist);

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
                if (mode == Player.REPEAT_MODE_OFF) player.setRepeatMode(Player.REPEAT_MODE_ONE);
                else if (mode == Player.REPEAT_MODE_ONE) player.setRepeatMode(Player.REPEAT_MODE_ALL);
                else player.setRepeatMode(Player.REPEAT_MODE_OFF);
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
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void populateSettings() {
        TextView appVerText = findViewById(R.id.app_version);
        TextView deviceModelText = findViewById(R.id.device_model);
        TextView androidVerText = findViewById(R.id.android_version);

        String appVersion = "1.0";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        appVerText.setText("App Version: " + appVersion);
        deviceModelText.setText("Device Model: " + Build.MODEL);
        androidVerText.setText("Android Version: " + Build.VERSION.RELEASE);
    }

    private void startFadeInAnimations() {
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
        if (player != null) syncUIWithPlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        handler.removeCallbacks(updateProgressAction);
    }

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

    private void onControllerConnected() {
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
                if (isPlaying) handler.post(updateProgressAction);
                else handler.removeCallbacks(updateProgressAction);
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
    }

    private void syncUIWithPlayer() {
        if (player == null) return;
        updateUIForNowPlaying(player.getCurrentMediaItem());
        long currentPos = player.getCurrentPosition();
        long duration = player.getDuration();
        seekBar.setMax((int) duration);
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
            nowPlayingTitle.setScrollX(0);
            nowPlayingArtist.setScrollX(0);

            nowPlayingTitle.post(() -> syncMarquees(nowPlayingTitle, title, nowPlayingArtist, artist));
            btnFavNow.setImageResource(favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        } else {
            nowPlayingTitle.setText("Select a song");
            nowPlayingArtist.setText("");
            nowPlayingTitle.setScrollX(0);
            nowPlayingArtist.setScrollX(0);
            btnFavNow.setImageResource(R.drawable.ic_heart_outline);
        }
    }

    private void syncMarquees(TextView tv1, String text1, TextView tv2, String text2) {
        if (tv1.getTag() instanceof ValueAnimator) ((ValueAnimator) tv1.getTag()).cancel();
        if (tv2.getTag() instanceof ValueAnimator) ((ValueAnimator) tv2.getTag()).cancel();

        float w1 = tv1.getPaint().measureText(text1) - (tv1.getWidth() - tv1.getPaddingLeft() - tv1.getPaddingRight());
        float w2 = tv2.getPaint().measureText(text2) - (tv2.getWidth() - tv2.getPaddingLeft() - tv2.getPaddingRight());

        int maxScroll1 = Math.max(0, (int) w1);
        int maxScroll2 = Math.max(0, (int) w2);

        if (maxScroll1 == 0 && maxScroll2 == 0) return;

        int longestScroll = Math.max(maxScroll1, maxScroll2);
        long totalDuration = Math.max(3000, longestScroll * 15L);

        ValueAnimator masterAnimator = ValueAnimator.ofFloat(0f, 1f);
        masterAnimator.setInterpolator(new LinearInterpolator());
        masterAnimator.setDuration(totalDuration);

        masterAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            if (maxScroll1 > 0) tv1.setScrollX((int) (fraction * maxScroll1));
            if (maxScroll2 > 0) tv2.setScrollX((int) (fraction * maxScroll2));
        });

        masterAnimator.addListener(new AnimatorListenerAdapter() {
            private final Handler marqueeHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onAnimationEnd(Animator animation) {
                if (tv1.getTag() == masterAnimator) {
                    marqueeHandler.postDelayed(() -> {
                        if (tv1.getTag() == masterAnimator) {
                            tv1.setScrollX(0);
                            tv2.setScrollX(0);
                            marqueeHandler.postDelayed(() -> {
                                if (tv1.getTag() == masterAnimator) {
                                    masterAnimator.start();
                                }
                            }, 2000);
                        }
                    }, 5000);
                }
            }
        });

        tv1.setTag(masterAnimator);
        tv2.setTag(masterAnimator);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (tv1.getTag() == masterAnimator) {
                masterAnimator.start();
            }
        }, 2000);
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

    private void updateTimers(long currentPos, long duration) {
        if (duration < 0) duration = 0;
        currentTimeText.setText(formatTime(currentPos));
        remainingTimeText.setText("-" + formatTime(Math.max(0, duration - currentPos)));
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