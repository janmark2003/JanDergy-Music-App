package com.jandergy.myjandergymusic;

import android.Manifest;
import android.widget.ImageView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.OvershootInterpolator;
import com.google.android.material.card.MaterialCardView;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODES = 100;
    private static final String PREFS_NAME = "MusicPrefs";
    private static final String FAVORITES_KEY = "Favorites";

    private MediaController player;
    private ListenableFuture<MediaController> controllerFuture;
    private final List<AudioAdapter.AudioItem> allAudioItems = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Set<String> favoriteIds = new HashSet<>();
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger libraryLoadVersion = new AtomicInteger();
    private final List<MediaItem> playbackQueue = new ArrayList<>();
    private int playbackQueueVersion = 0;
    private int appliedPlayerQueueVersion = -1;

    private ViewPager2 viewPager;
    private MusicSectionsAdapter sectionsAdapter;

    private MarqueeTextView nowPlayingTitle, nowPlayingArtist;
    private TextView currentTimeText, remainingTimeText;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat, btnFavNow, btnSettings;
    private ImageButton btnPrev, btnNext;
    private SearchView searchView;
    private TabLayout tabLayout;

    private View splashScreen;
    private ImageView splashLogo, logoInHeader;
    private TextView splashCopyright;
    private MaterialCardView headerPanel, libraryPanel, playerControls;
    private android.widget.FrameLayout musicNotesContainer;

    private boolean libraryLoaded = false;
    private boolean splashFinished = false;

    private final AudioAdapter.OnItemClickListener itemClickListener = new AudioAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(AudioAdapter.AudioItem item) {
            playAudio(item);
        }

        @Override
        public void onFavoriteClick(AudioAdapter.AudioItem item) {
            toggleFavorite(item);
        }

        @Override
        public void onAlbumArtClick(AudioAdapter.AudioItem item, View albumArtView) {
            openPlayerActivity(item, albumArtView);
        }
    };

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

    private ContentObserver musicObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        configureTransitions();
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        favoriteIds = new HashSet<>(sharedPreferences.getStringSet(FAVORITES_KEY, new HashSet<>()));

        initUI();
        registerMusicObserver();
        checkPermissions();
    }

    private void registerMusicObserver() {
        musicObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                loadAudioFiles();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, musicObserver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeController();
        setUIVisibility(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUIVisibility(true);
        if (player != null) {
            syncUIWithPlayer();
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
        getContentResolver().unregisterContentObserver(musicObserver);
        libraryExecutor.shutdownNow();
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

    private void initUI() {
        splashScreen = findViewById(R.id.splash_screen);
        splashLogo = findViewById(R.id.splash_logo);
        splashCopyright = findViewById(R.id.splash_copyright);
        musicNotesContainer = findViewById(R.id.music_notes_container);
        logoInHeader = findViewById(R.id.logo);
        headerPanel = findViewById(R.id.header_panel);
        libraryPanel = findViewById(R.id.library_panel);
        playerControls = findViewById(R.id.player_controls);

        logoInHeader.setAlpha(1f);
        headerPanel.setAlpha(0f);
        libraryPanel.setAlpha(0f);
        playerControls.setAlpha(0f);

        String appVersion = "1.0";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        splashCopyright.setText(String.format("[%s, %s] Janmark The Blue Dragon\nAll Rights Reserved", appVersion, Build.MODEL));

        viewPager = findViewById(R.id.view_pager);
        sectionsAdapter = new MusicSectionsAdapter(this);

        for (int i = 0; i < 4; i++) {
            sectionsAdapter.getFragment(i).setOnItemClickListener(itemClickListener);
        }

        viewPager.setAdapter(sectionsAdapter);

        tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setIcon(R.drawable.ic_all_music); break;
                case 1: tab.setIcon(R.drawable.ic_artists); break;
                case 2: tab.setIcon(R.drawable.ic_recent); break;
                case 3: tab.setIcon(R.drawable.ic_heart_filled); break;
            }
        }).attach();

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
        btnSettings = findViewById(R.id.btn_settings);
        searchView = findViewById(R.id.search_view);

        View miniPlayerBox = findViewById(R.id.player_controls);
        miniPlayerBox.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullScreenPlayerActivity.class);
            Pair<View, String> logoPair = Pair.create(findViewById(R.id.logo), "logo_transition");
            Pair<View, String> playerPair = Pair.create(miniPlayerBox, "player_box_transition");

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, logoPair, playerPair);
            startActivity(intent, options.toBundle());
        });

        btnSettings.setOnClickListener(v -> launchSettings());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                for (int i = 0; i < 4; i++) {
                    sectionsAdapter.getFragment(i).filter(newText);
                }
                return true;
            }
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
                if (mode == Player.REPEAT_MODE_OFF) player.setRepeatMode(Player.REPEAT_MODE_ONE);
                else if (mode == Player.REPEAT_MODE_ONE) player.setRepeatMode(Player.REPEAT_MODE_ALL);
                else player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        });

        btnFavNow.setOnClickListener(v -> {
            MediaItem item = (player != null) ? player.getCurrentMediaItem() : null;
            if (item != null) {
                AudioAdapter.AudioItem audio = findItemById(Long.parseLong(item.mediaId));
                if (audio != null) toggleFavorite(audio);
            }
        });

        OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MusicListFragment artistFragment = sectionsAdapter.getFragment(1);
                if (viewPager.getCurrentItem() == 1 && artistFragment.handleBack()) {
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

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

    private void launchSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        Pair<View, String> logoPair = Pair.create(findViewById(R.id.logo), "logo_transition");
        Pair<View, String> playerPair = Pair.create(findViewById(R.id.player_controls), "player_box_transition");
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, logoPair, playerPair);
        startActivity(intent, options.toBundle());
    }

    private void openPlayerActivity(AudioAdapter.AudioItem item, View albumArtView) {
        playAudio(item);
        setUIVisibility(false);

        Intent intent = new Intent(this, FullScreenPlayerActivity.class);
        Pair<View, String> logoPair = Pair.create(findViewById(R.id.logo), "logo_transition");
        Pair<View, String> playerPair = Pair.create(findViewById(R.id.player_controls), "player_box_transition");
        Pair<View, String> artPair = Pair.create(albumArtView, "album_art_transition");

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, logoPair, playerPair, artPair);
        startActivity(intent, options.toBundle());
    }

    private void setUIVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        searchView.setVisibility(visibility);
        tabLayout.setVisibility(visibility);
        viewPager.setVisibility(visibility);

        if (visible) {
            searchView.setAlpha(1.0f);
            tabLayout.setAlpha(1.0f);
            viewPager.setAlpha(1.0f);
        }
    }

    private void updateShuffleIcon(boolean enabled) {
        btnShuffle.setAlpha(enabled ? 1.0f : 0.4f);
    }

    private void updateRepeatIcon(int mode) {
        btnRepeat.setAlpha(mode == Player.REPEAT_MODE_OFF ? 0.4f : 1.0f);
    }

    private AudioAdapter.AudioItem findItemById(long id) {
        synchronized (allAudioItems) {
            for (AudioAdapter.AudioItem item : allAudioItems) {
                if (item.id == id) return item;
            }
        }
        return null;
    }

    private void checkPermissions() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
        } else {
            loadAudioFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudioFiles();
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadAudioFiles() {
        final int requestVersion = libraryLoadVersion.incrementAndGet();
        libraryExecutor.execute(() -> {
            Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA
            };

            List<AudioAdapter.AudioItem> newList = new ArrayList<>();
            try (Cursor cursor = getContentResolver().query(
                    collection,
                    projection,
                    MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                    null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC")) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                    int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME);
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String title = cursor.getString(titleColumn);
                        String artist = cursor.getString(artistColumn);
                        long duration = cursor.getLong(durationColumn);
                        long date = cursor.getLong(dateColumn);
                        String bucketName = cursor.getString(bucketColumn);
                        String data = cursor.getString(dataColumn);

                        String folderName = bucketName != null && !bucketName.trim().isEmpty() ? bucketName : "Unknown";
                        if ("Unknown".equals(folderName) && data != null) {
                            File file = new File(data);
                            if (file.getParentFile() != null) {
                                folderName = file.getParentFile().getName();
                            }
                        }

                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        AudioAdapter.AudioItem item = new AudioAdapter.AudioItem(
                                id,
                                contentUri,
                                title != null && !title.trim().isEmpty() ? title : "Unknown title",
                                artist != null && !artist.trim().isEmpty() ? artist : "Unknown artist",
                                folderName,
                                duration,
                                date);
                        item.isFavorite = favoriteIds.contains(String.valueOf(id));
                        newList.add(item);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            SectionData sectionData = buildSectionData(newList);
            List<MediaItem> nextPlaybackQueue = buildPlaybackQueue(newList);
            runOnUiThread(() -> {
                if (requestVersion != libraryLoadVersion.get() || isFinishing() || isDestroyed()) {
                    return;
                }
                synchronized (allAudioItems) {
                    allAudioItems.clear();
                    allAudioItems.addAll(newList);
                    playbackQueue.clear();
                    playbackQueue.addAll(nextPlaybackQueue);
                    playbackQueueVersion++;
                }
                applySectionData(sectionData);
            });
        });
    }

    private void applySectionData(SectionData sectionData) {
        sectionsAdapter.getFragment(0).updateList(sectionData.allItems);
        sectionsAdapter.getFragment(1).updateList(sectionData.artistItems);
        sectionsAdapter.getFragment(2).updateList(sectionData.recentItems);
        sectionsAdapter.getFragment(3).updateList(sectionData.favoriteItems);

        if (!libraryLoaded) {
            libraryLoaded = true;
            checkStartEntranceAnimation();
        }
    }

    private void checkStartEntranceAnimation() {
        if (libraryLoaded && !splashFinished) {
            startEntranceAnimation();
        }
    }

    private void startEntranceAnimation() {
        splashFinished = true;

        splashCopyright.animate().alpha(0f).setDuration(400).start();
        splashLogo.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(600)
                .setInterpolator(new AnticipateInterpolator())
                .start();

        musicNotesContainer.setVisibility(View.VISIBLE);
        for (int i = 0; i < 10; i++) {
            showAnimatedNote(i);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            splashScreen.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .withEndAction(() -> splashScreen.setVisibility(View.GONE))
                    .start();
        }, 1200);

        animateBubbly(headerPanel, 1500);
        animateBubbly(libraryPanel, 1750);
        animateBubbly(playerControls, 2000);
    }

    private void showAnimatedNote(int index) {
        ImageView note = new ImageView(this);
        note.setImageResource(R.drawable.ic_music_note);
        int size = (int) (30 + Math.random() * 35);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                (int) (size * getResources().getDisplayMetrics().density),
                (int) (size * getResources().getDisplayMetrics().density));
        note.setLayoutParams(params);

        musicNotesContainer.addView(note);

        float centerX = musicNotesContainer.getWidth() / 2f - (size * getResources().getDisplayMetrics().density) / 2f;
        float centerY = musicNotesContainer.getHeight() / 2f - (size * getResources().getDisplayMetrics().density) / 2f;

        note.setX(centerX);
        note.setY(centerY);
        note.setAlpha(0f);
        note.setScaleX(0f);
        note.setScaleY(0f);

        float angle = (float) (Math.random() * 2 * Math.PI);
        float dist = (float) (250 + Math.random() * 450);
        float endX = centerX + (float) Math.cos(angle) * dist;
        float endY = centerY + (float) Math.sin(angle) * dist;

        note.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .translationX(endX)
                .translationY(endY)
                .rotation(angle * 100)
                .setDuration(1600)
                .setStartDelay(index * 80L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    note.animate()
                            .alpha(0f)
                            .scaleX(0.4f)
                            .scaleY(0.4f)
                            .setDuration(600)
                            .start();
                })
                .start();
    }

    private void animateBubbly(View v, long delay) {
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        v.setAlpha(0f);
        v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setStartDelay(delay)
                .setDuration(1000)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    private void toggleFavorite(AudioAdapter.AudioItem item) {
        item.isFavorite = !item.isFavorite;
        if (item.isFavorite) {
            favoriteIds.add(String.valueOf(item.id));
        } else {
            favoriteIds.remove(String.valueOf(item.id));
        }

        sharedPreferences.edit().putStringSet(FAVORITES_KEY, favoriteIds).apply();

        MediaItem current = (player != null) ? player.getCurrentMediaItem() : null;
        if (current != null && current.mediaId.equals(String.valueOf(item.id))) {
            btnFavNow.setImageResource(item.isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        }

        refreshSections();
    }

    private void playAudio(AudioAdapter.AudioItem item) {
        if (player == null) return;

        MediaItem currentItem = player.getCurrentMediaItem();
        if (currentItem != null && currentItem.mediaId.equals(String.valueOf(item.id))) {
            if (!player.isPlaying()) {
                player.play();
            }
            return;
        }

        int startIndex = -1;
        List<MediaItem> queueSnapshot;
        int queueVersionSnapshot;
        synchronized (allAudioItems) {
            for (int i = 0; i < allAudioItems.size(); i++) {
                if (allAudioItems.get(i).id == item.id) {
                    startIndex = i;
                    break;
                }
            }
            queueSnapshot = new ArrayList<>(playbackQueue);
            queueVersionSnapshot = playbackQueueVersion;
        }
        if (startIndex < 0 || queueSnapshot.isEmpty()) return;

        boolean shouldReplaceQueue = appliedPlayerQueueVersion != queueVersionSnapshot
                || player.getMediaItemCount() != queueSnapshot.size();
        if (shouldReplaceQueue) {
            player.setMediaItems(queueSnapshot, startIndex, 0L);
            player.prepare();
            appliedPlayerQueueVersion = queueVersionSnapshot;
        } else {
            player.seekToDefaultPosition(startIndex);
            if (player.getPlaybackState() == Player.STATE_IDLE) {
                player.prepare();
            }
        }
        player.play();
    }

    private void refreshSections() {
        List<AudioAdapter.AudioItem> snapshot;
        synchronized (allAudioItems) {
            snapshot = new ArrayList<>(allAudioItems);
        }

        libraryExecutor.execute(() -> {
            SectionData sectionData = buildSectionData(snapshot);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    applySectionData(sectionData);
                }
            });
        });
    }

    private SectionData buildSectionData(List<AudioAdapter.AudioItem> sourceItems) {
        List<AudioAdapter.AudioItem> allItems = new ArrayList<>(sourceItems);
        List<AudioAdapter.AudioItem> artistItems = new ArrayList<>(sourceItems);
        Collections.sort(artistItems, (a, b) -> a.folderName.compareToIgnoreCase(b.folderName));

        List<AudioAdapter.AudioItem> recentItems = new ArrayList<>(sourceItems);
        Collections.sort(recentItems, (a, b) -> Long.compare(b.dateAdded, a.dateAdded));

        List<AudioAdapter.AudioItem> favoriteItems = new ArrayList<>();
        for (AudioAdapter.AudioItem item : sourceItems) {
            if (item.isFavorite) {
                favoriteItems.add(item);
            }
        }
        return new SectionData(allItems, artistItems, recentItems, favoriteItems);
    }

    private List<MediaItem> buildPlaybackQueue(List<AudioAdapter.AudioItem> audioItems) {
        List<MediaItem> items = new ArrayList<>(audioItems.size());
        for (AudioAdapter.AudioItem audio : audioItems) {
            items.add(new MediaItem.Builder()
                    .setUri(audio.uri)
                    .setMediaId(String.valueOf(audio.id))
                    .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(audio.title)
                            .setArtist(audio.artist)
                            .build())
                    .setRequestMetadata(new androidx.media3.common.MediaItem.RequestMetadata.Builder()
                            .setMediaUri(audio.uri)
                            .build())
                    .build());
        }
        return items;
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

    private static class SectionData {
        final List<AudioAdapter.AudioItem> allItems;
        final List<AudioAdapter.AudioItem> artistItems;
        final List<AudioAdapter.AudioItem> recentItems;
        final List<AudioAdapter.AudioItem> favoriteItems;

        SectionData(List<AudioAdapter.AudioItem> allItems,
                    List<AudioAdapter.AudioItem> artistItems,
                    List<AudioAdapter.AudioItem> recentItems,
                    List<AudioAdapter.AudioItem> favoriteItems) {
            this.allItems = allItems;
            this.artistItems = artistItems;
            this.recentItems = recentItems;
            this.favoriteItems = favoriteItems;
        }
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
}