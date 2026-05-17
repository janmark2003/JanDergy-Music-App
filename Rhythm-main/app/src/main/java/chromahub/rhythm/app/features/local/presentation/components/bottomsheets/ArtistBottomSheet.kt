package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.features.local.presentation.components.player.PlayingEqIcon
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.features.local.presentation.components.player.formatDuration
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistBottomSheet(
    artist: Artist,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddSongToPlaylist: (Song) -> Unit,
    onPlayerClick: () -> Unit,
    sheetState: SheetState,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPlayNext: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit = {},
    favoriteSongs: Set<String> = emptySet(),
    onShowSongInfo: (Song) -> Unit = {},
    onAddToBlacklist: (Song) -> Unit = {},
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    songs: List<Song>? = null,
    albums: List<Album>? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MusicViewModel = viewModel()
    val appSettings = remember { AppSettings.getInstance(context) }
    val groupByAlbumArtist by appSettings.groupByAlbumArtist.collectAsState()
    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()
    val useHoursFormat by appSettings.useHoursInTimeFormat.collectAsState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Detect tablet mode
    val isTablet = configuration.screenWidthDp >= 600
    
    // Get songs and albums from viewModel
    val allSongs by viewModel.songs.collectAsState()
    val allAlbums by viewModel.albums.collectAsState()
    val displaySongs = songs ?: allSongs
    val displayAlbums = albums ?: allAlbums
    
    // Helper function to split artist names
    val splitArtistNames: (String) -> List<String> = remember {
        { artistName ->
            // Character-level delimiters from settings
            val artistSeparatorEnabled = appSettings.artistSeparatorEnabled.value
            val charDelimiters = if (artistSeparatorEnabled) {
                appSettings.artistSeparatorDelimiters.value.toList().map { it.toString() }
            } else emptyList()

            if (charDelimiters.isEmpty()) {
                listOf(artistName.trim()).filter { it.isNotBlank() }
            } else {
                val selectedDelimiterChars = charDelimiters.mapNotNull { it.firstOrNull() }.toSet()
                val wordSeparators = mutableListOf<String>().apply {
                    if (selectedDelimiterChars.contains('&')) add(" & ")
                    add(" and ")
                    if (selectedDelimiterChars.contains(',')) add(", ")
                    add(" feat. ")
                    add(" feat ")
                    add(" ft. ")
                    add(" ft ")
                    add(" featuring ")
                    add(" x ")
                    add(" X ")
                    add(" vs ")
                    add(" vs. ")
                    add(" with ")
                }

                var names = listOf(artistName)
                for (delimiter in charDelimiters) {
                    names = names.flatMap { it.split(delimiter) }
                }
                for (separator in wordSeparators) {
                    names = names.flatMap { it.split(separator, ignoreCase = true) }
                }
                names.map { it.trim() }.filter { it.isNotBlank() }
            }
        }
    }
    
    // Filter songs and albums for this artist based on grouping preference
    val artistSongs = remember(displaySongs, artist, groupByAlbumArtist, artistSeparatorEnabled, artistSeparatorDelimiters) {
        displaySongs.filter { song ->
            if (groupByAlbumArtist) {
                // Match split album artist names, falling back to split track artists.
                val explicitAlbumArtist = song.albumArtist?.trim().orEmpty()
                val songArtistNames = if (explicitAlbumArtist.isNotBlank() && !explicitAlbumArtist.equals("<unknown>", ignoreCase = true)) {
                    splitArtistNames(explicitAlbumArtist)
                } else {
                    splitArtistNames(song.artist)
                }
                songArtistNames.any { it.equals(artist.name, ignoreCase = true) }
            } else {
                // When not grouping, check if artist appears in track artist field (split collaborations)
                splitArtistNames(song.artist).any { it.equals(artist.name, ignoreCase = true) }
            }
        }
    }
    
    val artistAlbums = remember(displayAlbums, displaySongs, artist, groupByAlbumArtist, artistSeparatorEnabled, artistSeparatorDelimiters) {
        if (groupByAlbumArtist) {
            // When grouping by album artist, check if any song in the album has matching album artist
            displayAlbums.filter { album ->
                displaySongs.any { song ->
                    val explicitAlbumArtist = song.albumArtist?.trim().orEmpty()
                    val songArtistNames = if (explicitAlbumArtist.isNotBlank() && !explicitAlbumArtist.equals("<unknown>", ignoreCase = true)) {
                        splitArtistNames(explicitAlbumArtist)
                    } else {
                        splitArtistNames(song.artist)
                    }

                    song.album == album.title &&
                    song.albumId == album.id &&
                    songArtistNames.any { it.equals(artist.name, ignoreCase = true) }
                }
            }
        } else {
            // When not grouping, check if artist appears in any song's track artist field for this album
            displayAlbums.filter { album ->
                displaySongs.any { song ->
                    song.album == album.title &&
                    song.albumId == album.id &&
                    splitArtistNames(song.artist).any { it.equals(artist.name, ignoreCase = true) }
                }
            }
        }
    }
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
    }
    
    val headerAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
        label = "headerAlpha"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(700),
        label = "contentAlpha"
    )
    
    val contentTranslation by animateFloatAsState(
        targetValue = if (showContent) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentTranslation"
    )

    if (isTablet) {
        // Tablet layout: Dialog with side-by-side layout
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .navigationBarsPadding()
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left side: Artist info and artwork
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 24.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Artist artwork
                                Surface(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .graphicsLayer {
                                            alpha = headerAlpha
                                            scaleX = if (showContent) 1f else 0.9f
                                            scaleY = if (showContent) 1f else 0.9f
                                        },
                                    shape = CircleShape,
                                    shadowElevation = 16.dp,
                                    tonalElevation = 8.dp
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .apply(
                                                    ImageUtils.buildImageRequest(
                                                        artist.artworkUri,
                                                        artist.name,
                                                        context.cacheDir,
                                                        M3PlaceholderType.ARTIST
                                                    )
                                                )
                                                .build(),
                                            contentDescription = "Artist image for ${artist.name}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Artist info
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metadata
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${artistSongs.size} songs",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "${artistAlbums.size} albums",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Action buttons - Grouped button design
                                var shufflePressed by remember { mutableStateOf(false) }
                                var playAllPressed by remember { mutableStateOf(false) }
                                
                                val shuffleScale by animateFloatAsState(
                                    targetValue = if (shufflePressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "shuffleScale"
                                )
                                
                                val playAllScale by animateFloatAsState(
                                    targetValue = if (playAllPressed) 0.96f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "playAllScale"
                                )
                                
                                LaunchedEffect(shufflePressed) {
                                    if (shufflePressed) {
                                        delay(150)
                                        shufflePressed = false
                                    }
                                }
                                
                                LaunchedEffect(playAllPressed) {
                                    if (playAllPressed) {
                                        delay(150)
                                        playAllPressed = false
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    // Play All Button - Equal sizing with text
                                    Button(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticFeedbackType.LongPress
                                            )
                                            playAllPressed = true
                                            if (artistSongs.isNotEmpty()) {
                                                onPlayAll(artistSongs)
                                            }
                                            onDismiss()
                                            onPlayerClick()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .graphicsLayer {
                                                scaleX = playAllScale
                                                scaleY = playAllScale
                                            },
                                        shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Play,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Play All",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Shuffle Button - Equal sizing with text
                                    FilledTonalButton(
                                        onClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticFeedbackType.LongPress
                                            )
                                            shufflePressed = true
                                            if (artistSongs.isNotEmpty()) {
                                                onShufflePlay(artistSongs)
                                            }
                                            onDismiss()
                                            onPlayerClick()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .graphicsLayer {
                                                scaleX = shuffleScale
                                                scaleY = shuffleScale
                                            },
                                        shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = RhythmIcons.Shuffle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Shuffle",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Right side: Albums and Songs list
                        Surface(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight(),
                            color = Color.Transparent
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Header with close button
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Content",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Close button on tablet
                                        IconButton(
                                            onClick = onDismiss,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Content list
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                        .graphicsLayer { translationY = contentTranslation },
                                    shape = RoundedCornerShape(
                                        topStart = 28.dp,
                                        topEnd = 28.dp
                                    ),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    tonalElevation = 1.dp
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            top = 8.dp,
                                            bottom = 24.dp
                                        ),
                                        userScrollEnabled = true
                                    ) {
                                        // Albums Section
                                        if (artistAlbums.isNotEmpty()) {
                                            item {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text(
                                                        text = context.getString(R.string.bottomsheet_albums),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    )
                                                    
                                                    LazyRow(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        items(
                                                            items = artistAlbums,
                                                            key = { "artistalbum_${it.id}" }
                                                        ) { album ->
                                                            ArtistAlbumCard(
                                                                album = album,
                                                                onClick = {
                                                                    HapticUtils.performHapticFeedback(
                                                                        context,
                                                                        haptics,
                                                                        HapticFeedbackType.TextHandleMove
                                                                    )
                                                                    // Show album bottom sheet
                                                                    onAlbumClick(album)
                                                                },
                                                                onPlay = {
                                                                    HapticUtils.performHapticFeedback(
                                                                        context,
                                                                        haptics,
                                                                        HapticFeedbackType.LongPress
                                                                    )
                                                                    viewModel.playAlbum(album)
                                                                },
                                                                haptics = haptics
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Songs Section
                                        if (artistSongs.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = context.getString(R.string.bottomsheet_songs),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }

                                            items(
                                                items = artistSongs,
                                                key = { "artistsong_${it.id}_${it.uri}" }
                                            ) { song ->
                                                EnhancedArtistSongItem(
                                                    song = song,
                                                    onClick = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onSongClick(song)
                                                        onDismiss()
                                                        onPlayerClick()
                                                    },
                                                    onAddToQueue = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onAddToQueue(song)
                                                    },
                                                    onAddToPlaylist = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onAddSongToPlaylist(song)
                                                    },
                                                    onPlayNext = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onPlayNext(song)
                                                    },
                                                    onToggleFavorite = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onToggleFavorite(song)
                                                    },
                                                    isFavorite = favoriteSongs.contains(song.id),
                                                    onShowSongInfo = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onShowSongInfo(song)
                                                    },
                                                    onAddToBlacklist = {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptics,
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        onAddToBlacklist(song)
                                                    },
                                                    currentSong = currentSong,
                                                    isPlaying = isPlaying,
                                                    useHoursFormat = useHoursFormat,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                                    haptics = haptics
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Phone layout: Bottom sheet
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
            // Artist Header (Sticky)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp) // Fixed height for the header
                    .graphicsLayer { alpha = headerAlpha }
            ) {
                // Artist Image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(
                            artist.artworkUri,
                            artist.name,
                            context.cacheDir,
                            M3PlaceholderType.ARTIST
                        ))
                        .build(),
                    contentDescription = "Artist image for ${artist.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Enhanced gradient overlay with multiple layers
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                // Horizontal gradient for more depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                // Enhanced close button with better design
                FilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(WindowInsets.statusBars.asPaddingValues()) // Adjust for status bar
                        .padding(20.dp)
                        .size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = RhythmIcons.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Artist info at bottom with enhanced layout
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enhanced statistics with better visual clarity
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${artistSongs.size} songs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${artistAlbums.size} albums",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced Action buttons row - Grouped button design
                    var shufflePressed by remember { mutableStateOf(false) }
                    var playAllPressed by remember { mutableStateOf(false) }
                    
                    val shuffleScale by animateFloatAsState(
                        targetValue = if (shufflePressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "shuffleScale"
                    )
                    
                    val playAllScale by animateFloatAsState(
                        targetValue = if (playAllPressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "playAllScale"
                    )
                    
                    LaunchedEffect(shufflePressed) {
                        if (shufflePressed) {
                            delay(150)
                            shufflePressed = false
                        }
                    }
                    
                    LaunchedEffect(playAllPressed) {
                        if (playAllPressed) {
                            delay(150)
                            playAllPressed = false
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Play All Button - Equal sizing with text
                        Button(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                playAllPressed = true
                                if (artistSongs.isNotEmpty()) {
                                    onPlayAll(artistSongs)
                                    scope.launch {
                                        sheetState.hide()
                                    }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            onDismiss()
                                            onPlayerClick()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = playAllScale
                                    scaleY = playAllScale
                                },
                            shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Play All",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Shuffle Button - Equal sizing with text
                        FilledTonalButton(
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                shufflePressed = true
                                if (artistSongs.isNotEmpty()) {
                                    onShufflePlay(artistSongs)
                                    scope.launch {
                                        sheetState.hide()
                                    }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            onDismiss()
                                            onPlayerClick()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = shuffleScale
                                    scaleY = shuffleScale
                                },
                            shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = RhythmIcons.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Shuffle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take remaining space
            ) {
            // Albums Section with animation
            if (artistAlbums.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = contentAlpha
                                    translationY = contentTranslation
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = context.getString(R.string.bottomsheet_albums),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${artistAlbums.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = artistAlbums,
                                    key = { "artistalbum_${it.id}" },
                                    contentType = { "album" }
                                ) { album ->
                                    ArtistAlbumCard(
                                        album = album,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            // Show album bottom sheet
                                            onAlbumClick(album)
                                        },
                                        onPlay = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            viewModel.playAlbum(album)
                                        },
                                        haptics = haptics
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Songs Section with animation
            if (artistSongs.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = contentAlpha
                                    translationY = contentTranslation
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = context.getString(R.string.bottomsheet_songs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "${artistSongs.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column {
                                artistSongs.forEach { song ->
                                EnhancedArtistSongItem(
                                        song = song,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onSongClick(song)
                                            scope.launch {
                                                sheetState.hide()
                                            }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismiss()
                                                    onPlayerClick()
                                                }
                                            }
                                        },
                                        onAddToQueue = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onAddToQueue(song)
                                        },
                                        onAddToPlaylist = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onAddSongToPlaylist(song)
                                        },
                                        onPlayNext = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onPlayNext(song)
                                        },
                                        onToggleFavorite = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onToggleFavorite(song)
                                        },
                                        isFavorite = favoriteSongs.contains(song.id),
                                        onShowSongInfo = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onShowSongInfo(song)
                                        },
                                        onAddToBlacklist = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            onAddToBlacklist(song)
                                        },
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        useHoursFormat = useHoursFormat,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = contentAlpha
                                                translationY = contentTranslation
                                            }
                                            .padding(horizontal = 16.dp, vertical = 4.dp), // Added padding to song item
                                        haptics = haptics
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacing
                item {
                    Spacer(
                        modifier = Modifier
                            .height(32.dp)
                            .graphicsLayer { alpha = contentAlpha }
                    )
                }
            }
        }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val context = LocalContext.current
    
    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .width(160.dp)
            .padding(4.dp), // Add some padding around the card
        shape = RoundedCornerShape(16.dp), // Larger rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Add subtle shadow
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Use a slightly higher surface color
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Clip image corners
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(
                            album.artworkUri,
                            album.title,
                            context.cacheDir,
                            M3PlaceholderType.ALBUM
                        ))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp) // Slightly more padding
                ) {
                    FilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onPlay()
                        },
                        modifier = Modifier.size(40.dp), // Slightly larger button
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Play,
                            contentDescription = "Play album",
                            modifier = Modifier.size(22.dp) // Slightly larger icon
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp) // Adjusted vertical padding
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall, // Use titleSmall for better hierarchy
                    fontWeight = FontWeight.Bold, // Make title bolder
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${album.numberOfSongs} songs",
                    style = MaterialTheme.typography.bodySmall, // Use bodySmall for song count
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedArtistSongItem(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPlayNext: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    isFavorite: Boolean = false,
    onShowSongInfo: () -> Unit = {},
    onAddToBlacklist: () -> Unit = {},
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    useHoursFormat: Boolean = false
) {
    val context = LocalContext.current
    var showDropdown by remember { mutableStateOf(false) }
    
    val isCurrentSong = currentSong?.id == song.id
    
    val titleColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300), label = "titleColor"
    )
    
    val artistColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300), label = "artistColor"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isCurrentSong) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(300), label = "containerColor"
    )
    
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = titleColor,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            },
            supportingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display album name instead of artist for better clarity in artist context
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = artistColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (song.duration > 0) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = artistColor
                        )
                        
                        val durationText = formatDuration(song.duration, useHoursFormat)
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = artistColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            leadingContent = {
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp),
                        tonalElevation = 2.dp
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .apply(ImageUtils.buildImageRequest(
                                    song.artworkUri,
                                    song.title,
                                    context.cacheDir,
                                    M3PlaceholderType.TRACK
                                ))
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    if (isCurrentSong && isPlaying) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .offset(x = 4.dp, y = 4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayingEqIcon(
                                    modifier = Modifier.size(width = 12.dp, height = 10.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    isPlaying = isPlaying,
                                    bars = 3
                                )
                            }
                        }
                    }
                }
            },
        trailingContent = {
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    showDropdown = true
                },
                modifier = Modifier
                    .size(width = 40.dp, height = 36.dp),
                shape = RoundedCornerShape(18.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = RhythmIcons.More,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    showDropdown = false
                },
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(5.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                // Play next
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Play next",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.SkipNext,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showDropdown = false
                            onPlayNext()
                        }
                    )
                }

                // Add to queue
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Add to queue",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.Queue,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showDropdown = false
                            onAddToQueue()
                        }
                    )
                }

                // Toggle favorite
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFavorite) "Remove from favorites" else "Add to favorites",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) androidx.compose.material.icons.Icons.Filled.Favorite else androidx.compose.material.icons.Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showDropdown = false
                            onToggleFavorite()
                        }
                    )
                }

                // Add to playlist
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Add to playlist",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = RhythmIcons.AddToPlaylist,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showDropdown = false
                            onAddToPlaylist()
                        }
                    )
                }

                // Song info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Song info",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        },
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showDropdown = false
                            onShowSongInfo()
                        }
                    )
                }

                // Add to blacklist
//                Surface(
//                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
//                    shape = RoundedCornerShape(16.dp),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 8.dp, vertical = 2.dp)
//                ) {
//                    DropdownMenuItem(
//                        text = {
//                            Text(
//                                "Hide song",
//                                style = MaterialTheme.typography.bodyMedium,
//                                fontWeight = FontWeight.Medium,
//                                color = MaterialTheme.colorScheme.onErrorContainer
//                            )
//                        },
//                        leadingIcon = {
//                            Surface(
//                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
//                                shape = CircleShape,
//                                modifier = Modifier.size(32.dp)
//                            ) {
//                                Icon(
//                                    imageVector = androidx.compose.material.icons.Icons.Rounded.Block,
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.error,
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .padding(6.dp)
//                                )
//                            }
//                        },
//                        onClick = {
//                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
//                            showDropdown = false
//                            onAddToBlacklist()
//                        }
//                    )
//                }
            }
        },
        modifier = modifier.clickable(onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
            onClick()
        }),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
    }
}