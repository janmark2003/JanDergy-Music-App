package com.jandergy.myjandergymusic;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private RecyclerView queueRecyclerView;
    private QueueAdapter queueAdapter;
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
                handler.postDelayed(this, 1000);
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
        queueRecyclerView = findViewById(R.id.queue_recycler_view);

        queueAdapter = new QueueAdapter(index -> {
            if (player != null) {
                player.seekToDefaultPosition(index);
                player.play();
            }
        });
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        queueRecyclerView.setHasFixedSize(true);
        queueRecyclerView.setAdapter(queueAdapter);

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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void applyBubblyEntrance() {
        View[] elements = {fullAlbumArt, fullSongTitle, fullArtistName, queueRecyclerView};
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
                updateUIForNowPlaying(mediaItem, true);
                queueAdapter.setNowPlayingMediaId(mediaItem != null ? mediaItem.mediaId : null);
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
            public void onTimelineChanged(Timeline timeline, int reason) {
                rebuildQueueFromPlayer();
            }
        });

        syncUIWithPlayer();
        updateShuffleIcon(player.getShuffleModeEnabled());
        updateRepeatIcon(player.getRepeatMode());
        rebuildQueueFromPlayer();
    }

    private void rebuildQueueFromPlayer() {
        if (player == null) return;
        List<QueueItem> queueItems = new ArrayList<>();
        for (int i = 0; i < player.getMediaItemCount(); i++) {
            MediaItem mediaItem = player.getMediaItemAt(i);
            String title = (mediaItem.mediaMetadata.title != null)
                    ? mediaItem.mediaMetadata.title.toString()
                    : "Unknown";
            String artist = (mediaItem.mediaMetadata.artist != null)
                    ? mediaItem.mediaMetadata.artist.toString()
                    : "Unknown";
            queueItems.add(new QueueItem(i, mediaItem.mediaId, title, artist));
        }
        queueAdapter.updateItems(queueItems);
        MediaItem current = player.getCurrentMediaItem();
        queueAdapter.setNowPlayingMediaId(current != null ? current.mediaId : null);
    }

    private void syncUIWithPlayer() {
        if (player == null) return;
        updateUIForNowPlaying(player.getCurrentMediaItem(), false);
        long currentPos = player.getCurrentPosition();
        long duration = player.getDuration();
        seekBar.setMax((int) duration);
        seekBar.setProgress((int) currentPos);
        updateTimers(currentPos, duration);

        if (player.isPlaying()) {
            handler.removeCallbacks(updateProgressAction);
            handler.post(updateProgressAction);
        }
    }

    private void updateUIForNowPlaying(MediaItem mediaItem, boolean animate) {
        if (mediaItem != null) {
            String title = mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : "Unknown";
            String artist = mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : "Unknown";

            updateTitleAndArtist(title, artist, animate);

            btnFavNow.setImageResource(favoriteIds.contains(mediaItem.mediaId) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            Uri uri = null;
            if (mediaItem.requestMetadata != null) uri = mediaItem.requestMetadata.mediaUri;
            loadAlbumArtAndPalette(uri, animate);
        } else {
            updateTitleAndArtist("Select a song", "", animate);
            loadAlbumArtAndPalette(null, animate);
            activeArtworkRequestKey = null;
        }
    }

    private void updateTitleAndArtist(String title, String artist, boolean animate) {
        if (!animate) {
            fullSongTitle.animate().cancel();
            fullArtistName.animate().cancel();
            fullSongTitle.setText(title);
            fullArtistName.setText(artist);
            fullSongTitle.setAlpha(1f);
            fullArtistName.setAlpha(1f);
            fullSongTitle.setTranslationY(0f);
            fullArtistName.setTranslationY(0f);
            return;
        }
        animateTextChange(fullSongTitle, title);
        animateTextChange(fullArtistName, artist);
    }

    private void animateTextChange(TextView view, String text) {
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .translationY(-12f)
                .setDuration(120)
                .withEndAction(() -> {
                    view.setText(text);
                    view.setTranslationY(12f);
                    view.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(200)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void loadAlbumArtAndPalette(Uri uri, boolean animate) {
        if (uri == null) {
            applyAlbumArt(null, null, animate);
            activeArtworkRequestKey = null;
            return;
        }

        activeArtworkRequestKey = uri.toString();
        String requestKey = activeArtworkRequestKey;
        ArtworkLoader.loadBitmapAndPalette(getContentResolver(), uri, 480, this::paletteToHuePalette, (bitmap, palette) -> {
            if (!requestKey.equals(activeArtworkRequestKey)) {
                return;
            }
            applyAlbumArt(bitmap, palette, animate);
        });
    }

    private void applyAlbumArt(Bitmap bitmap, Palette palette, boolean animate) {
        Runnable applyImage = () -> {
            if (bitmap != null) {
                fullAlbumArt.setImageBitmap(bitmap);
                backgroundBlur.setPalette(palette);
            } else {
                fullAlbumArt.setImageResource(R.drawable.blank_icon_album);
                backgroundBlur.setPalette(null);
            }
        };

        if (!animate) {
            fullAlbumArt.animate().cancel();
            fullAlbumArt.setAlpha(1f);
            fullAlbumArt.setScaleX(1f);
            fullAlbumArt.setScaleY(1f);
            applyImage.run();
            return;
        }

        fullAlbumArt.animate().cancel();
        fullAlbumArt.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(140)
                .withEndAction(() -> {
                    applyImage.run();
                    fullAlbumArt.setScaleX(0.92f);
                    fullAlbumArt.setScaleY(0.92f);
                    fullAlbumArt.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(220)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
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

    private static class QueueItem {
        final int index;
        final String mediaId;
        final String title;
        final String artist;

        QueueItem(int index, String mediaId, String title, String artist) {
            this.index = index;
            this.mediaId = mediaId;
            this.title = title;
            this.artist = artist;
        }
    }

    private interface OnQueueItemClickListener {
        void onQueueItemClick(int index);
    }

    private static class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {
        private final List<QueueItem> allItems = new ArrayList<>();
        private final List<QueueItem> filteredItems = new ArrayList<>();
        private final OnQueueItemClickListener listener;
        private String nowPlayingMediaId;

        QueueAdapter(OnQueueItemClickListener listener) {
            this.listener = listener;
        }

        void updateItems(List<QueueItem> items) {
            allItems.clear();
            allItems.addAll(items);
            filteredItems.clear();
            filteredItems.addAll(items);
            notifyDataSetChanged();
        }

        void filter(String query) {
            filteredItems.clear();
            if (query == null || query.trim().isEmpty()) {
                filteredItems.addAll(allItems);
            } else {
                String lowered = query.toLowerCase(Locale.getDefault());
                for (QueueItem item : allItems) {
                    if (item.title.toLowerCase(Locale.getDefault()).contains(lowered)
                            || item.artist.toLowerCase(Locale.getDefault()).contains(lowered)) {
                        filteredItems.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        void setNowPlayingMediaId(String mediaId) {
            nowPlayingMediaId = mediaId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_queue, parent, false);
            return new QueueViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
            QueueItem item = filteredItems.get(position);
            holder.titleView.setText(item.title);
            holder.artistView.setText(item.artist);
            boolean isNowPlaying = item.mediaId != null && item.mediaId.equals(nowPlayingMediaId);
            holder.itemView.setAlpha(isNowPlaying ? 1f : 0.82f);
            holder.itemView.setOnClickListener(v -> listener.onQueueItemClick(item.index));
        }

        @Override
        public int getItemCount() {
            return filteredItems.size();
        }

        static class QueueViewHolder extends RecyclerView.ViewHolder {
            final TextView titleView;
            final TextView artistView;

            QueueViewHolder(@NonNull View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.queue_song_title);
                artistView = itemView.findViewById(R.id.queue_artist_name);
            }
        }
    }
}
