# Rhythm Architecture Guide

Technical documentation of Rhythm's app structure, design patterns, and architectural decisions.

---

## 🏗️ Architecture Overview

Rhythm follows **Clean Architecture** principles with **MVVM (Model-View-ViewModel)** pattern, ensuring separation of concerns, testability, and maintainability.

### Architecture Layers

```
┌─────────────────────────────────────────────┐
│            Presentation Layer               │
│  ┌──────────────────────────────────────┐  │
│  │   Composables (UI Components)        │  │
│  │   - Screens, Components, Dialogs     │  │
│  └──────────────────────────────────────┘  │
│                    ↕                        │
│  ┌──────────────────────────────────────┐  │
│  │   ViewModels (State Management)      │  │
│  │   - Business Logic Coordination      │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────────┐
│              Domain Layer                   │
│  ┌──────────────────────────────────────┐  │
│  │   Repository Interfaces              │  │
│  │   - Abstractions for data access     │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │   Domain Models                      │  │
│  │   - Song, Album, Artist, Playlist    │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────────┐
│              Data Layer                     │
│  ┌──────────────────────────────────────┐  │
│  │   Repository Implementations         │  │
│  │   - Concrete data access logic       │  │
│  └──────────────────────────────────────┘  │
│  ┌──────────────────────────────────────┐  │
│  │   Data Sources                       │  │
│  │   - MediaStore, APIs, Local Storage │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 📦 Module Structure

### App Module (`/app`)

Primary application module containing all features and UI.

```
app/src/main/java/chromahub/rhythm/app/
├── activities/              # App entry points
│   └── MainActivity.kt      # Single activity architecture
│
├── features/                # Feature modules
│   └── local/              # Local music playback
│       ├── data/           # Data layer
│       │   ├── repository/ # Repository implementations
│       │   └── sources/    # MediaStore, file system
│       ├── domain/         # Business logic
│       │   └── models/     # Domain entities
│       └── presentation/   # UI layer
│           ├── screens/    # Screen composables
│           ├── components/ # Reusable UI components
│           └── viewmodel/  # ViewModels
│
├── shared/                 # Shared across features
│   ├── data/              # Shared data models
│   │   ├── model/         # Data classes
│   │   └── repository/    # Shared repositories
│   └── presentation/      # Shared UI components
│       ├── components/    # Common composables
│       └── theme/         # Material 3 theme
│
├── infrastructure/        # App infrastructure
│   ├── service/          # Background services
│   │   └── MediaPlaybackService.kt
│   ├── widget/           # Home screen widgets
│   │   ├── glance/      # Modern Glance widgets
│   │   └── legacy/      # RemoteViews widgets
│   └── worker/          # Background workers
│       ├── BackupWorker.kt
│       └── UpdateWorker.kt
│
└── util/                 # Utility classes
    ├── AudioDeviceManager.kt
    ├── AutoEQManager.kt
    └── Extensions.kt
```

---

## 🎨 Presentation Layer

### Jetpack Compose

All UI built with Compose using Material 3 design system.

#### Screen Composition

```kotlin
@Composable
fun PlayerScreen(
    viewModel: MusicViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    
    Scaffold(
        topBar = { PlayerTopBar(onNavigateBack) },
        content = { padding ->
            PlayerContent(
                song = currentSong,
                playbackState = playbackState,
                onPlayPause = { viewModel.togglePlayback() },
                modifier = Modifier.padding(padding)
            )
        }
    )
}
```

#### Component Hierarchy

```
Screen Composables
    └── Layout Composables (Scaffold, Column, Row)
        └── UI Components (Card, Button, Text)
            └── Custom Components (AlbumArt, ProgressBar)
```

### ViewModels

State management and business logic coordination.

```kotlin
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    
    // State flows for reactive UI
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _playbackState = MutableStateFlow(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    // Business logic methods
    fun playSong(song: Song) {
        viewModelScope.launch {
            _currentSong.value = song
            _playbackState.value = PlaybackState.Playing
            // Coordinate with service
        }
    }
}
```

### Navigation

Single-activity architecture with Compose Navigation.

```kotlin
@Composable
fun RhythmNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        composable("player") { PlayerScreen() }
        composable("library") { LibraryScreen() }
        // More routes...
    }
}
```

---

## 🗄️ Data Layer

### Repository Pattern

Abstracts data sources from ViewModels.

```kotlin
interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun updateSong(song: Song)
}

class MusicRepositoryImpl(
    private val mediaStore: MediaStoreDataSource
) : MusicRepository {
    override fun getAllSongs(): Flow<List<Song>> = flow {
        val songs = mediaStore.querySongs()
        emit(songs)
    }
}
```

### Data Sources

#### MediaStore

Primary source for music files on device.

```kotlin
class MediaStoreDataSource(private val context: Context) {
    fun querySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(mapCursorToSong(cursor))
            }
        }
        
        return songs
    }
}
```

#### Local Storage

Settings, playlists, and app data.

```kotlin
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", MODE_PRIVATE)
    
    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) = prefs.edit().putString("theme", value).apply()
}
```

---

## 🎵 Audio Playback Architecture

### Media3 ExoPlayer Integration

```
┌─────────────────────────────────────┐
│      MediaPlaybackService           │
│  (Foreground Service)               │
│                                     │
│  ┌──────────────────────────────┐  │
│  │      ExoPlayer               │  │
│  │  - Audio engine              │  │
│  │  - Playback control          │  │
│  │  - Queue management          │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaSession               │  │
│  │  - Media controls interface  │  │
│  │  - Bluetooth integration     │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   MediaNotification          │  │
│  │  - Persistent notification   │  │
│  │  - Playback controls         │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Service Implementation

```kotlin
class MediaPlaybackService : Service() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    
    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        createMediaSession()
        startForegroundService()
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
```

---

## 📱 Widget Architecture

### Glance Framework (Modern)

```kotlin
class RhythmMusicWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val data = getWidgetData()
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }
}

@Composable
fun WidgetContent(data: WidgetData) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        // Widget UI
    }
}
```

### Widget Updates

```kotlin
class WidgetUpdateWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        GlanceAppWidgetManager(context)
            .getGlanceIds(RhythmMusicWidget::class.java)
            .forEach { glanceId ->
                RhythmMusicWidget().update(context, glanceId)
            }
        return Result.success()
    }
}
```

---

## 🔄 State Management

### StateFlow Pattern

Reactive state updates using Kotlin Flow.

```kotlin
class PlayerViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlayerState.Idle)
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    fun updateState(newState: PlayerState) {
        _state.value = newState
    }
}

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()
    
    when (state) {
        is PlayerState.Playing -> ShowPlayingUI()
        is PlayerState.Paused -> ShowPausedUI()
        is PlayerState.Idle -> ShowIdleUI()
    }
}
```

---

## 🌐 Network Layer

### API Integration

```kotlin
interface LyricsApi {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String
    ): LyricsResponse
}

class LyricsRepository(private val api: LyricsApi) {
    suspend fun fetchLyrics(song: Song): Result<Lyrics> {
        return try {
            val response = api.getLyrics(song.title, song.artist)
            Result.success(response.toLyrics())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🧪 Testing Architecture

### Unit Tests

```kotlin
class MusicViewModelTest {
    @Test
    fun `playSong updates state correctly`() = runTest {
        val viewModel = MusicViewModel()
        val testSong = Song(/* test data */)
        
        viewModel.playSong(testSong)
        
        assertEquals(testSong, viewModel.currentSong.value)
        assertEquals(PlaybackState.Playing, viewModel.playbackState.value)
    }
}
```

### UI Tests

```kotlin
@Test
fun playerScreen_showsCorrectSongInfo() {
    composeTestRule.setContent {
        PlayerScreen(song = testSong)
    }
    
    composeTestRule
        .onNodeWithText(testSong.title)
        .assertIsDisplayed()
}
```

---

## 🔐 Security & Privacy

### Data Privacy

- No analytics or tracking code
- All data stored locally
- No server communication except optional features
- Encrypted backups (optional)

### Permissions

```kotlin
object PermissionManager {
    fun requestStoragePermission(activity: Activity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Request READ_MEDIA_AUDIO
            }
            else -> {
                // Request READ_EXTERNAL_STORAGE
            }
        }
    }
}
```

---

## ⚡ Performance Optimization

### Lazy Loading

```kotlin
@Composable
fun SongList(songs: List<Song>) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            SongItem(song)
        }
    }
}
```

### Image Caching

```kotlin
// Coil for efficient image loading
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(song.albumArtUri)
        .crossfade(true)
        .build(),
    contentDescription = "Album Art"
)
```

### Background Processing

```kotlin
class MediaScanWorker : CoroutineWorker() {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Heavy processing off main thread
        scanMediaLibrary()
        Result.success()
    }
}
```

---

## 📊 Dependency Injection

Currently using manual DI. Future migration to Hilt planned.

```kotlin
class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MusicRepository = MusicRepositoryImpl(
        MediaStoreDataSource(application)
    )
}
```

---

## 🔄 Build System

### Gradle Kotlin DSL

```kotlin
// build.gradle.kts
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 40310853
        versionName = "4.0.310.853"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Version Catalog

```toml
[versions]
kotlin = "1.9.22"
compose = "1.6.0"
exoplayer = "1.9.0"

[libraries]
compose-ui = { module = "androidx.compose.ui:ui", version.ref = "compose" }
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "exoplayer" }
```

---

## 🎯 Design Patterns

### Repository Pattern
- Abstraction over data sources
- Testable business logic
- Single source of truth

### Observer Pattern
- StateFlow for reactive updates
- LiveData alternative
- Lifecycle-aware

### Factory Pattern
- ViewModel creation
- Widget instantiation

### Singleton Pattern
- AppSettings
- Repository instances

---

## 📚 Further Reading

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)

---

**Questions?** Check [Contributing Guide](https://github.com/cromaguy/Rhythm/wiki/Contributing) or ask in [Telegram](https://t.me/RhythmSupport)!
