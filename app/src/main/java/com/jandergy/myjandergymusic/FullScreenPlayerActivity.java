package com.jandergy.myjandergymusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.palette.graphics.Palette;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FullScreenPlayerActivity extends AppCompatActivity {

    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;

    private TextView currentTimeText, remainingTimeText;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat, btnFavNow;
    private ImageButton btnPrev, btnNext;

    private ImageView fullAlbumArt;
    private TextView fullSongTitle, fullArtistName;
    private MovingBlurView backgroundBlur;
    private String activeArtworkRequestKey;

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
        configureTransitions();
        supportPostponeEnterTransition();
        setContentView(R.layout.activity_full_screen_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.player_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("MusicPrefs", MODE_PRIVATE);
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet("Favorites", new HashSet<>()));

        initUI();
        findViewById(R.id.player_root).post(this::supportStartPostponedEnterTransition);
        applyBubblyEntrance();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                supportFinishAfterTransition();
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

        fullAlbumArt.setOnClickListener(this::bubblyClick);

        btnPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) player.pause();
            else player.play();
            bubblyClick(v);
        });

        btnNext.setOnClickListener(v -> {
            if (player != null) player.seekToNext();
            bubblyClick(v);
        });
        btnPrev.setOnClickListener(v -> {
            if (player != null) player.seekToPrevious();
            bubblyClick(v);
        });

        btnShuffle.setOnClickListener(v -> {
            if (player != null) player.setShuffleModeEnabled(!player.getShuffleModeEnabled());
            bubblyClick(v);
        });

        btnRepeat.setOnClickListener(v -> {
            if (player != null) {
                int mode = player.getRepeatMode();
                if (mode == Player.REPEAT_MODE_OFF) player.setRepeatMode(Player.REPEAT_MODE_ONE);
                else if (mode == Player.REPEAT_MODE_ONE) player.setRepeatMode(Player.REPEAT_MODE_ALL);
                else player.setRepeatMode(Player.REPEAT_MODE_OFF);
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

    private void applyBubblyEntrance() {
        View[] elements = {fullAlbumArt, fullSongTitle, fullArtistName};
        for (int i = 0; i < elements.length; i++) {
            elements[i].setScaleX(0.7f);
            elements[i].setScaleY(0.7f);
            elements[i].setAlpha(0f);
            elements[i].animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(950)
                    .setStartDelay(220 + (i * 120L))
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    private void bubblyClick(View v) {
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        ).start();
    }

    private void toggleFavorite(String mediaId) {
        if (favoriteIds.contains(mediaId)) {
            favoriteIds.remove(mediaId);
        } else {
            favoriteIds.add(mediaId);
        }
        sharedPreferences.edit().putStringSet("Favorites", favoriteIds).apply();
        btnFavNow.setImageResource(favoriteIds.contains(mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            String title = mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : "Unknown";
            String artist = mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : "Unknown";

            fullSongTitle.setText(title);
            fullArtistName.setText(artist);
            fullSongTitle.setSelected(true);
            fullArtistName.setSelected(true);
            btnFavNow.setImageResource(favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            Uri uri = null;
            if (mediaItem.requestMetadata != null) uri = mediaItem.requestMetadata.mediaUri;
            loadAlbumArtAndPalette(uri);
        } else {
            fullSongTitle.setText("Select a song");
            fullArtistName.setText("");
            fullSongTitle.setSelected(false);
            fullArtistName.setSelected(false);
            fullAlbumArt.setImageResource(R.drawable.blank_icon_album);
            backgroundBlur.setPalette(null);
            activeArtworkRequestKey = null;
        }
    }

    private void loadAlbumArtAndPalette(Uri uri) {
        if (uri == null) {
            fullAlbumArt.setImageResource(R.drawable.blank_icon_album);
            backgroundBlur.setPalette(null);
            activeArtworkRequestKey = null;
            return;
        }

        activeArtworkRequestKey = uri.toString();
        String requestKey = activeArtworkRequestKey;
        ArtworkLoader.loadBitmapAndPalette(getContentResolver(), uri, 480, this::paletteToHuePalette, (bitmap, palette) -> {
            if (!requestKey.equals(activeArtworkRequestKey)) {
                return;
            }
            if (bitmap != null) {
                fullAlbumArt.setImageBitmap(bitmap);
                backgroundBlur.setPalette(palette);
            } else {
                fullAlbumArt.setImageResource(R.drawable.blank_icon_album);
                backgroundBlur.setPalette(null);
            }
        });
    }

    private int[] paletteToHuePalette(Palette palette) {
        if (palette == null) return null;

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

        return new int[]{
                Color.HSVToColor(new float[]{normalizeHue(hsv[0]), saturation, value}),
                Color.HSVToColor(new float[]{normalizeHue(hsv[0] + 22f), saturation * 0.9f, Math.min(1f, value + 0.1f)}),
                Color.HSVToColor(new float[]{normalizeHue(hsv[0] - 22f), Math.min(1f, saturation + 0.1f), value}),
                Color.HSVToColor(new float[]{normalizeHue(hsv[0] + 180f), Math.max(0.35f, saturation * 0.75f), Math.max(0.45f, value * 0.9f)})
        };
    }

    private float normalizeHue(float hue) {
        float normalized = hue % 360f;
        if (normalized < 0f) normalized += 360f;
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
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}