package chromahub.rhythm.app.features.streaming.presentation.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.features.local.presentation.navigation.Screen
import chromahub.rhythm.app.features.local.presentation.screens.AddToPlaylistScreen
import chromahub.rhythm.app.features.local.presentation.screens.ArtistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.EqualizerScreen
import chromahub.rhythm.app.features.local.presentation.screens.ListeningStatsScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlayerScreen
import chromahub.rhythm.app.features.local.presentation.screens.settings.RhythmGuardSettingsScreen
import chromahub.rhythm.app.features.local.presentation.screens.settings.QueuePlaybackSettingsScreen
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel as LocalMusicViewModel
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingAlbum
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingContentHomeScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingHomeScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingLibraryScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingSearchScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingServiceSetupScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.GoSettingsScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.toLibraryAlbum
import chromahub.rhythm.app.features.streaming.presentation.screens.toLibraryPlaylist
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.rememberModalBottomSheetState
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongPickerBottomSheet
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import chromahub.rhythm.app.ui.UiConstants
import chromahub.rhythm.app.ui.theme.MusicDimensions
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsScreenWrapper
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalDensity

private sealed class StreamingScreen(val route: String, val titleRes: Int? = null) {
    data object Integration : StreamingScreen("streaming_integration")
    data object Home : StreamingScreen("streaming_home", R.string.home)
    data object Library : StreamingScreen("streaming_library", R.string.library)
    data object Search : StreamingScreen("streaming_search", R.string.search)
    data object AddSongsToPlaylist : StreamingScreen("streaming_add_songs/{playlistId}") {
        fun createRoute(playlistId: String): String = "streaming_add_songs/${Uri.encode(playlistId)}"
    }
    data object Settings : StreamingScreen("streaming_settings", R.string.settings_title)
    data object RhythmStats : StreamingScreen("streaming_rhythm_stats")
    data object RhythmGuard : StreamingScreen("streaming_rhythm_guard")
    data object Player : StreamingScreen("streaming_player")
    data object ArtistDetail : StreamingScreen("streaming_artist/{artistId}?artistName={artistName}") {
        fun createRoute(artistId: String, artistName: String): String =
            "streaming_artist/${Uri.encode(artistId)}?artistName=${Uri.encode(artistName)}"
    }
    data object PlaylistDetail : StreamingScreen("streaming_playlist/{playlistId}") {
        fun createRoute(playlistId: String): String = "streaming_playlist/${Uri.encode(playlistId)}"
    }
    data object ServiceSetup : StreamingScreen("streaming_service_setup/{serviceId}") {
        fun createRoute(serviceId: String): String = "streaming_service_setup/${Uri.encode(serviceId)}"
    }
    data object GoSettings : StreamingScreen("streaming_go_settings")
}

@Composable
fun StreamingNavigation(
    localMusicViewModel: LocalMusicViewModel = viewModel(),
    streamingMusicViewModel: StreamingMusicViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onSwitchToLocalMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: StreamingScreen.Integration.route

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }

    val currentSong by localMusicViewModel.currentSong.collectAsState()
    val isPlaying by localMusicViewModel.isPlaying.collectAsState()
    val progress by localMusicViewModel.progress.collectAsState()
    val queueState by localMusicViewModel.currentQueue.collectAsState()
    val currentDevice by localMusicViewModel.currentDevice.collectAsState()
    val locations by localMusicViewModel.locations.collectAsState()
    val volume by localMusicViewModel.volume.collectAsState()
    val isMuted by localMusicViewModel.isMuted.collectAsState()
    val isShuffleEnabled by localMusicViewModel.isShuffleEnabled.collectAsState()
    val repeatMode by localMusicViewModel.repeatMode.collectAsState()
    val isFavorite by localMusicViewModel.isFavorite.collectAsState()
    val showLyrics by localMusicViewModel.showLyrics.collectAsState()
    val showOnlineOnlyLyrics by localMusicViewModel.showOnlineOnlyLyrics.collectAsState()
    val lyrics by localMusicViewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by localMusicViewModel.isLoadingLyrics.collectAsState()
    val playlists by localMusicViewModel.playlists.collectAsState()
    val songs by localMusicViewModel.songs.collectAsState()
    val albums by localMusicViewModel.albums.collectAsState()
    val artists by localMusicViewModel.artists.collectAsState()
    val isMediaLoading by localMusicViewModel.isBuffering.collectAsState()
    val isSeeking by localMusicViewModel.isSeeking.collectAsState()
    val recentlyPlayed by localMusicViewModel.recentlyPlayed.collectAsState()
    val listeningTime by localMusicViewModel.listeningTime.collectAsState()
    val playbackStatsSummary by localMusicViewModel.playbackStatsSummary.collectAsState()

    val sessions by streamingMusicViewModel.serviceSessions.collectAsState()
    val hasConnectedService = sessions.values.any { it.isConnected }

    val isServiceSetupRoute = currentRoute.startsWith("streaming_service_setup")
    val isPlayerRoute = currentRoute == StreamingScreen.Player.route
    val isEqualizerRoute = currentRoute == Screen.Equalizer.route
    val isQueuePlaybackRoute = currentRoute == Screen.TunerQueuePlayback.route
    val isSettingsRoute = currentRoute == Screen.Settings.route
    
    // MiniPlayer should show on all screens except Player, Equalizer, and QueuePlayback
    val showMiniPlayer = currentSong != null && !isPlayerRoute && !isEqualizerRoute && !isQueuePlaybackRoute
    
    // Bottom nav only shows on main navigation screens (Home, Library, Search)
    val showBottomNav = hasConnectedService && !isPlayerRoute && !isEqualizerRoute && !isQueuePlaybackRoute && !isSettingsRoute && (
        currentRoute == StreamingScreen.Home.route ||
            currentRoute == StreamingScreen.Library.route ||
            currentRoute == StreamingScreen.Search.route
        )
    
    val requiresConnectedService =
        currentRoute == StreamingScreen.Home.route ||
            currentRoute == StreamingScreen.Library.route ||
            currentRoute == StreamingScreen.Search.route
    
    // Calculate miniplayer padding for bottom content alignment
    val miniPlayerPaddingValues = remember(showMiniPlayer, currentRoute) {
        var totalPadding = 0.dp
        
        // Add MiniPlayer height if visible
        if (showMiniPlayer) {
            totalPadding += UiConstants.MiniPlayerHeight + 16.dp // Card height + spacing
        }
        
        // Return padding values
        PaddingValues(bottom = totalPadding)
    }
    
    var hasPendingStartupRoute by remember { mutableStateOf(false) }

    // Streaming add-to-playlist and favorites state
    var showStreamingAddToPlaylist by remember { mutableStateOf(false) }
    var selectedStreamingSongForPlaylist by remember { mutableStateOf<StreamingSong?>(null) }
    val streamingPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingDownloadedSongs by streamingMusicViewModel.downloadedSongs.collectAsState()
    val streamingRecommendations by streamingMusicViewModel.recommendations.collectAsState()
    val streamingSearchResults by streamingMusicViewModel.searchResults.collectAsState()
    val streamingLikedSongIds = remember(streamingLikedSongs) { streamingLikedSongs.map { it.id }.toSet() }
    val streamingCurrentSong by streamingMusicViewModel.currentSong.collectAsState()

    val streamingAddSongsCandidates = remember(
        streamingLikedSongs,
        streamingDownloadedSongs,
        streamingRecommendations,
        streamingSearchResults
    ) {
        (streamingLikedSongs + streamingDownloadedSongs + streamingRecommendations + streamingSearchResults.songs)
            .distinctBy { it.id }
    }
    val streamingAddSongsCandidatesById = remember(streamingAddSongsCandidates) {
        streamingAddSongsCandidates.associateBy { it.id }
    }

    val streamingSavedAlbums by streamingMusicViewModel.savedAlbums.collectAsState()
    val streamingNewReleases by streamingMusicViewModel.newReleases.collectAsState()
    val streamingAlbumCatalog = remember(streamingSavedAlbums, streamingNewReleases) {
        (streamingSavedAlbums + streamingNewReleases).distinctBy { it.id }
    }

    val onStreamingAddSongToPlaylist: (StreamingSong) -> Unit = { song ->
        selectedStreamingSongForPlaylist = song
        showStreamingAddToPlaylist = true
    }
    val onStreamingToggleFavorite: (Song) -> Unit = { localSong ->
        // Find the streaming song by ID and toggle
        val streamingSong = streamingLikedSongs.firstOrNull { it.id == localSong.id }
        if (streamingSong != null) {
            streamingMusicViewModel.unlikeSong(streamingSong)
        } else {
            // We need to find the streaming song from all known songs
            // Use the library songs from the ViewModel
            streamingMusicViewModel.likeSongById(localSong.id)
        }
    }

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(navController) {
        val pendingRoute = appSettings.consumeInitialStreamingRoute()
        if (!pendingRoute.isNullOrBlank()) {
            // Validate that the pending route exists in the NavHost
            val isValidRoute = listOf(
                StreamingScreen.Integration.route,
                StreamingScreen.Home.route,
                StreamingScreen.Library.route,
                StreamingScreen.Search.route,
                StreamingScreen.AddSongsToPlaylist.route,
                StreamingScreen.GoSettings.route,
                StreamingScreen.ServiceSetup.route,
                StreamingScreen.RhythmStats.route,
                StreamingScreen.RhythmGuard.route
            ).any { validRoute ->
                pendingRoute == validRoute || pendingRoute.startsWith(validRoute.substringBefore("{"))
            }

            if (isValidRoute) {
                hasPendingStartupRoute = true
                navController.navigate(pendingRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(currentRoute, hasPendingStartupRoute) {
        if (hasPendingStartupRoute && currentRoute != StreamingScreen.Integration.route) {
            hasPendingStartupRoute = false
        }
    }

    LaunchedEffect(streamingMusicViewModel, localMusicViewModel, navController) {
        streamingMusicViewModel.setPlaybackHandler { streamingQueue, startIndex ->
            val mappedQueue = streamingQueue.mapNotNull { it.toLocalSong() }
            if (mappedQueue.isEmpty()) {
                // Error: Unable to map streaming songs to local songs
                // This can happen if song metadata is invalid or missing
                streamingMusicViewModel.reportError(
                    "Playback failed: Unable to convert streaming songs for playback. " +
                    "Try reconnecting to your service."
                )
                return@setPlaybackHandler
            }

            if (streamingQueue.size != mappedQueue.size) {
                // Warning: Some songs couldn't be converted
                streamingMusicViewModel.reportWarning(
                    "Some songs in queue couldn't be loaded for playback."
                )
            }

            val safeIndex = startIndex.coerceIn(0, mappedQueue.lastIndex)
            val targetSong = mappedQueue[safeIndex]

            localMusicViewModel.playSongFromSearch(targetSong, mappedQueue)
            onNavigateToPlayer()
            navController.navigate(StreamingScreen.Player.route) {
                launchSingleTop = true
            }
        }

        streamingMusicViewModel.setSeekHandlers(
            progressHandler = { progress -> localMusicViewModel.seekTo(progress) },
            positionHandler = { positionMs -> localMusicViewModel.seekTo(positionMs) }
        )
    }

    LaunchedEffect(hasConnectedService, currentRoute) {
        when {
            !hasConnectedService && requiresConnectedService && !isServiceSetupRoute -> {
                navController.navigate(StreamingScreen.Integration.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
            // Detail screens also require connected service - redirect if disconnected
            !hasConnectedService && (currentRoute.startsWith("streaming_artist") || currentRoute.startsWith("streaming_playlist")) && !isServiceSetupRoute -> {
                navController.navigate(StreamingScreen.Integration.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
            // Only auto-navigate to Home if user is explicitly on Integration screen (not manually staying there)
            hasConnectedService && currentRoute == StreamingScreen.Integration.route && !hasPendingStartupRoute && !isServiceSetupRoute -> {
                navController.navigate(StreamingScreen.Home.route) {
                    popUpTo(StreamingScreen.Integration.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    CompositionLocalProvider(LocalMiniPlayerPadding provides miniPlayerPaddingValues) {
        Scaffold(
            modifier = modifier,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.25f to MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                                0.62f to MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                                1f to MaterialTheme.colorScheme.surface.copy(alpha = 1f)
                            )
                        )
                    )
            ) {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Only render MiniPlayer if currentSong is not null
                        currentSong?.let { song ->
                            MiniPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                progress = progress,
                                onPlayPause = { localMusicViewModel.togglePlayPause() },
                                onPlayerClick = {
                                    onNavigateToPlayer()
                                    navController.navigate(StreamingScreen.Player.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onSkipNext = { localMusicViewModel.skipToNext() },
                                onSkipPrevious = { localMusicViewModel.skipToPrevious() },
                                onDismiss = { 
                                    localMusicViewModel.clearCurrentSong()
                                    streamingMusicViewModel.clearCurrentSong()
                                },
                                isMediaLoading = isMediaLoading,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showBottomNav,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    StreamingBottomBar(
                        currentRoute = currentRoute,
                        navController = navController,
                        context = context,
                        haptic = haptic,
                        onSearchClick = { navigateToTopLevel(StreamingScreen.Search.route) }
                    )
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = StreamingScreen.Integration.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalMiniPlayerPadding.current)
        ) {
            composable(
                route = StreamingScreen.Integration.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(250))
                }
            ) {
                StreamingHomeScreen(
                    viewModel = streamingMusicViewModel,
                    onNavigateToSettings = onNavigateToSettings,
                    onConfigureService = { serviceId ->
                        navController.navigate(StreamingScreen.ServiceSetup.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    },
                    onSwitchToLocalMode = onSwitchToLocalMode
                )
            }

            composable(
                route = StreamingScreen.Home.route,
                enterTransition = {
                    when {
                        initialState.destination.route == StreamingScreen.Library.route -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(300))
                        }
                    }
                },
                exitTransition = {
                    when {
                        targetState.destination.route == StreamingScreen.Library.route -> {
                            fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeOut(animationSpec = tween(300))
                        }
                    }
                },
                popEnterTransition = {
                    when {
                        initialState.destination.route == StreamingScreen.Library.route -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(200))
                        }
                    }
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) {
                StreamingContentHomeScreen(
                    viewModel = streamingMusicViewModel,
                    recentlyPlayedSongs = recentlyPlayed,
                    playbackStatsSummary = playbackStatsSummary,
                    listeningTimeMs = listeningTime,
                    onNavigateToSettings = {
                        onNavigateToSettings()
                    },
                    onNavigateToSearch = {
                        navigateToTopLevel(StreamingScreen.Search.route)
                    },
                    onNavigateToRhythmGuard = {
                        navController.navigate(StreamingScreen.RhythmGuard.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRhythmStats = {
                        navController.navigate(StreamingScreen.RhythmStats.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToArtist = { artist ->
                        navController.navigate(
                            StreamingScreen.ArtistDetail.createRoute(artist.id, artist.name)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onConfigureService = { serviceId ->
                        navController.navigate(StreamingScreen.ServiceSetup.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = StreamingScreen.Library.route,
                enterTransition = {
                    when {
                        initialState.destination.route == StreamingScreen.Home.route -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(300))
                        }
                    }
                },
                exitTransition = {
                    when {
                        targetState.destination.route == StreamingScreen.Home.route -> {
                            fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeOut(animationSpec = tween(300))
                        }
                    }
                },
                popEnterTransition = {
                    when {
                        initialState.destination.route == StreamingScreen.Home.route -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(200))
                        }
                    }
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) {
                StreamingLibraryScreen(
                    viewModel = streamingMusicViewModel,
                    onConfigureService = { serviceId ->
                        navController.navigate(StreamingScreen.ServiceSetup.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToArtist = { artist ->
                        navController.navigate(
                            StreamingScreen.ArtistDetail.createRoute(artist.id, artist.name)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPlaylist = { playlist ->
                        navController.navigate(
                            StreamingScreen.PlaylistDetail.createRoute(playlist.id)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onAddSongToPlaylist = onStreamingAddSongToPlaylist,
                    activeSongId = currentSong?.id,
                    isPlayerPlaying = isPlaying
                )
            }

            composable(
                route = StreamingScreen.Search.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                StreamingSearchScreen(
                    viewModel = streamingMusicViewModel,
                    onConfigureService = { serviceId ->
                        navController.navigate(StreamingScreen.ServiceSetup.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToArtist = { artist ->
                        navController.navigate(
                            StreamingScreen.ArtistDetail.createRoute(artist.id, artist.name)
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = StreamingScreen.Settings.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                // Show the shared settings screen wrapper in streaming mode
                SettingsScreenWrapper(
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(StreamingScreen.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    appSettings = appSettings,
                    navController = navController,
                    musicViewModel = localMusicViewModel
                )
            }

            // Also add the local Settings route for proper navigation from streaming mode
            composable(
                route = Screen.Settings.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                // Show the shared settings screen wrapper
                SettingsScreenWrapper(
                    onBack = {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            navController.navigate(StreamingScreen.Home.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    appSettings = appSettings,
                    navController = navController,
                    musicViewModel = localMusicViewModel
                )
            }

            composable(
                route = StreamingScreen.GoSettings.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                GoSettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onConfigureCurrentProvider = { serviceId ->
                        navController.navigate(StreamingScreen.ServiceSetup.createRoute(serviceId)) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = streamingMusicViewModel
                )
            }

            composable(
                route = StreamingScreen.RhythmStats.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) {
                ListeningStatsScreen(
                    navController = navController,
                    viewModel = localMusicViewModel
                )
            }

            composable(
                route = StreamingScreen.RhythmGuard.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) {
                RhythmGuardSettingsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = StreamingScreen.ArtistDetail.route,
                arguments = listOf(
                    navArgument("artistId") { type = NavType.StringType },
                    navArgument("artistName") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                ),
                enterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments
                    ?.getString("artistId")
                    ?.let(Uri::decode)
                    .orEmpty()
                val routeArtistName = backStackEntry.arguments
                    ?.getString("artistName")
                    ?.let(Uri::decode)
                    .orEmpty()

                val followedArtists by streamingMusicViewModel.followedArtists.collectAsState()
                val searchResults by streamingMusicViewModel.searchResults.collectAsState()
                val currentService by streamingMusicViewModel.currentService.collectAsState()
                val haptics = LocalHapticFeedback.current

                val resolvedArtistName = remember(artistId, routeArtistName) {
                    routeArtistName.ifBlank { inferArtistNameFromId(artistId) }
                }

                val selectedArtist = remember(
                    artistId,
                    resolvedArtistName,
                    followedArtists,
                    searchResults,
                    currentService
                ) {
                    (followedArtists + searchResults.artists)
                        .distinctBy { it.id }
                        .firstOrNull { it.id == artistId }
                        ?: StreamingArtist(
                            id = artistId,
                            name = resolvedArtistName.ifBlank {
                                context.getString(R.string.artists_title)
                            },
                            artworkUri = null,
                            songCount = 0,
                            albumCount = 0,
                            sourceType = currentService
                        )
                }

                var artistSongs by remember(artistId) {
                    mutableStateOf(selectedArtist.getTopTracks())
                }
                var isArtistLoading by remember(artistId) { mutableStateOf(true) }

                LaunchedEffect(artistId, selectedArtist.name) {
                    if (artistId.isBlank()) {
                        artistSongs = emptyList()
                        isArtistLoading = false
                        return@LaunchedEffect
                    }

                    isArtistLoading = true
                    artistSongs = streamingMusicViewModel.getArtistTopSongs(
                        artistId = artistId,
                        artistNameHint = selectedArtist.name,
                        limit = 80
                    )
                    isArtistLoading = false
                }

                val localArtistSongs = remember(artistSongs) {
                    artistSongs.map { it.toDisplaySong() }
                }
                val localArtist = remember(
                    selectedArtist,
                    resolvedArtistName,
                    localArtistSongs
                ) {
                    selectedArtist.toDisplayArtist(
                        fallbackName = resolvedArtistName,
                        songs = localArtistSongs,
                        albums = emptyList()
                    )
                }
                val artistSongsById = remember(artistSongs) { artistSongs.associateBy { it.id } }
                var showSongInfoSheet by remember { mutableStateOf(false) }
                var selectedSongForInfo by remember { mutableStateOf<Song?>(null) }

                ArtistDetailScreen(
                    artistName = localArtist.name,
                    onBack = { navController.popBackStack() },
                    onSongClick = { localSong ->
                        val queue = if (artistSongs.isNotEmpty()) artistSongs else {
                            artistSongsById[localSong.id]?.let { listOf(it) }.orEmpty()
                        }
                        if (queue.isNotEmpty()) {
                            val index = queue.indexOfFirst { it.id == localSong.id }.coerceAtLeast(0)
                            streamingMusicViewModel.playQueue(
                                queue = queue,
                                startIndex = index,
                                shuffle = false
                            )
                        }
                    },
                    onAlbumClick = {},
                    onPlayAll = { songs ->
                        if (artistSongs.isNotEmpty()) {
                            streamingMusicViewModel.playQueue(
                                queue = artistSongs,
                                startIndex = 0,
                                shuffle = false
                            )
                        } else {
                            val fallbackQueue = songs.mapNotNull { artistSongsById[it.id] }
                            if (fallbackQueue.isNotEmpty()) {
                                streamingMusicViewModel.playQueue(
                                    queue = fallbackQueue,
                                    startIndex = 0,
                                    shuffle = false
                                )
                            }
                        }
                    },
                    onShufflePlay = {
                        if (artistSongs.isNotEmpty()) {
                            streamingMusicViewModel.playQueue(
                                queue = artistSongs,
                                startIndex = 0,
                                shuffle = true
                            )
                        }
                    },
                    onAddToQueue = { localSong ->
                        artistSongsById[localSong.id]?.let { streamingSong ->
                            streamingMusicViewModel.playQueue(
                                queue = listOf(streamingSong),
                                startIndex = 0,
                                shuffle = false
                            )
                        }
                    },
                    onAddSongToPlaylist = { localSong ->
                        artistSongsById[localSong.id]?.let { onStreamingAddSongToPlaylist(it) }
                    },
                    onPlayerClick = {
                        navController.navigate(StreamingScreen.Player.route) {
                            launchSingleTop = true
                        }
                    },
                    onPlayNext = {},
                    onToggleFavorite = onStreamingToggleFavorite,
                    favoriteSongs = streamingLikedSongIds,
                    onShowSongInfo = { song ->
                        selectedSongForInfo = song
                        showSongInfoSheet = true
                    },
                    showPlayNextAction = false,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    artistOverride = localArtist,
                    songsOverride = localArtistSongs,
                    albumsOverride = emptyList(),
                    isContentLoadingOverride = isArtistLoading
                )

                if (showSongInfoSheet && selectedSongForInfo != null) {
                    SongInfoBottomSheet(
                        song = selectedSongForInfo,
                        onDismiss = {
                            showSongInfoSheet = false
                            selectedSongForInfo = null
                        },
                        appSettings = appSettings,
                        isStreamingMode = true
                    )
                }
            }

            composable(
                route = StreamingScreen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                enterTransition = {
                    fadeIn(animationSpec = tween(250))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200))
                }
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments
                    ?.getString("playlistId")
                    ?.let(Uri::decode)
                    .orEmpty()

                val savedPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
                val featuredPlaylists by streamingMusicViewModel.featuredPlaylists.collectAsState()
                val searchResults by streamingMusicViewModel.searchResults.collectAsState()

                val selectedPlaylist = remember(
                    playlistId,
                    savedPlaylists,
                    featuredPlaylists,
                    searchResults
                ) {
                    (savedPlaylists + featuredPlaylists + searchResults.playlists)
                        .distinctBy { it.id }
                        .firstOrNull { it.id == playlistId }
                }

                val playlistTracks = remember(selectedPlaylist) {
                    selectedPlaylist?.getTracks().orEmpty()
                }
                val playlistTracksById = remember(playlistTracks) {
                    playlistTracks.associateBy { it.id }
                }
                val localPlaylistSongs = remember(playlistTracks) {
                    playlistTracks.map { it.toDisplaySong() }
                }
                val localPlaylist = remember(selectedPlaylist, localPlaylistSongs, playlistId) {
                    selectedPlaylist?.toDisplayPlaylist(localPlaylistSongs)
                        ?: Playlist(
                            id = playlistId,
                            name = context.getString(R.string.library_tab_playlists),
                            songs = localPlaylistSongs
                        )
                }
                val scope = rememberCoroutineScope()
                var showAlbumBottomSheet by remember { mutableStateOf(false) }
                var selectedAlbumForSheet by remember { mutableStateOf<Album?>(null) }
                val albumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

                PlaylistDetailScreen(
                    playlist = localPlaylist,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPause = { localMusicViewModel.togglePlayPause() },
                    onPlayerClick = {
                        navController.navigate(StreamingScreen.Player.route) {
                            launchSingleTop = true
                        }
                    },
                    onPlayAll = {
                        if (playlistTracks.isNotEmpty()) {
                            streamingMusicViewModel.playQueue(
                                queue = playlistTracks,
                                startIndex = 0,
                                shuffle = false
                            )
                        }
                    },
                    onShufflePlay = {
                        if (playlistTracks.isNotEmpty()) {
                            streamingMusicViewModel.playQueue(
                                queue = playlistTracks,
                                startIndex = 0,
                                shuffle = true
                            )
                        }
                    },
                    onSongClick = { localSong ->
                        val index = playlistTracks.indexOfFirst { it.id == localSong.id }
                        if (index >= 0) {
                            streamingMusicViewModel.playQueue(
                                queue = playlistTracks,
                                startIndex = index,
                                shuffle = false
                            )
                        }
                    },
                    onPlaySongFromPlaylist = { localSong, localQueue ->
                        val queue = localQueue
                            .mapNotNull { playlistTracksById[it.id] }
                            .ifEmpty { playlistTracks }
                        if (queue.isNotEmpty()) {
                            val index = queue.indexOfFirst { it.id == localSong.id }.coerceAtLeast(0)
                            streamingMusicViewModel.playQueue(
                                queue = queue,
                                startIndex = index,
                                shuffle = false
                            )
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onRemoveSong = { localSong, _ ->
                        playlistTracksById[localSong.id]?.let { streamingSong ->
                            streamingMusicViewModel.removeSongFromPlaylist(playlistId, streamingSong.id)
                        }
                    },
                    onRenamePlaylist = { newName ->
                        selectedPlaylist?.let { playlist ->
                            streamingMusicViewModel.renamePlaylist(playlist, newName)
                        }
                    },
                    onDeletePlaylist = {
                        selectedPlaylist?.let { playlist ->
                            streamingMusicViewModel.deletePlaylist(playlist) { success ->
                                if (success) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                    onAddSongsToPlaylist = {
                        selectedPlaylist?.id?.let { playlistId ->
                            navController.navigate(StreamingScreen.AddSongsToPlaylist.createRoute(playlistId)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onSkipNext = { streamingMusicViewModel.skipToNext() },
                    onSearchClick = { navigateToTopLevel(StreamingScreen.Search.route) },
                    isStreamingPlaylist = false,
                    onPlayNext = {},
                    onAddToQueue = { localSong ->
                        playlistTracksById[localSong.id]?.let { streamingSong ->
                            streamingMusicViewModel.playQueue(
                                queue = listOf(streamingSong),
                                startIndex = 0,
                                shuffle = false
                            )
                        }
                    },
                    onToggleFavorite = { localSong ->
                        playlistTracksById[localSong.id]?.let { streamingSong ->
                            val isLiked = streamingLikedSongs.any { it.id == streamingSong.id }
                            if (isLiked) streamingMusicViewModel.unlikeSong(streamingSong)
                            else streamingMusicViewModel.likeSong(streamingSong)
                        }
                    },
                    onAddToPlaylist = { localSong ->
                        playlistTracksById[localSong.id]?.let { onStreamingAddSongToPlaylist(it) }
                    },
                    onGoToAlbum = { song ->
                        val baseAlbumId = song.albumId.takeIf { it.isNotBlank() }
                        val albumArtist = song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist
                        val playlistDisplaySongs = localPlaylistSongs
                        val matchingAlbum = streamingAlbumCatalog.firstOrNull { album ->
                            val albumMatchesById = baseAlbumId?.let { album.id == it } == true
                            val albumMatchesByMetadata = album.title.equals(song.album, ignoreCase = true) &&
                                album.artist.equals(albumArtist, ignoreCase = true)
                            albumMatchesById || albumMatchesByMetadata
                        }

                        val fallbackAlbum = Album(
                            id = baseAlbumId ?: "streaming-playlist:${song.id}:${song.album.lowercase()}",
                            title = song.album,
                            artist = albumArtist,
                            artworkUri = song.artworkUri,
                            year = 0,
                            songs = playlistDisplaySongs.filter {
                                if (baseAlbumId != null) {
                                    it.albumId == baseAlbumId
                                } else {
                                    it.album.equals(song.album, ignoreCase = true) &&
                                        (it.albumArtist?.takeIf { it.isNotBlank() } ?: it.artist).equals(albumArtist, ignoreCase = true)
                                }
                            }.ifEmpty { listOf(song) },
                            numberOfSongs = playlistDisplaySongs.count {
                                if (baseAlbumId != null) {
                                    it.albumId == baseAlbumId
                                } else {
                                    it.album.equals(song.album, ignoreCase = true) &&
                                        (it.albumArtist?.takeIf { it.isNotBlank() } ?: it.artist).equals(albumArtist, ignoreCase = true)
                                }
                            }.coerceAtLeast(1)
                        )

                        selectedAlbumForSheet = matchingAlbum?.toLibraryAlbum(playlistDisplaySongs) ?: fallbackAlbum
                        showAlbumBottomSheet = true
                    },
                    onGoToArtist = {},
                    onShare = {}
                )

                if (showAlbumBottomSheet && selectedAlbumForSheet != null) {
                    val albumForSheet = selectedAlbumForSheet!!
                    val playlistTracksForAlbum = playlistTracks

                    AlbumBottomSheet(
                        album = albumForSheet,
                        onDismiss = {
                            showAlbumBottomSheet = false
                            selectedAlbumForSheet = null
                        },
                        onSongClick = { song ->
                            val streamingSong = playlistTracksForAlbum.firstOrNull { it.id == song.id }
                            streamingSong?.let { ss ->
                                streamingMusicViewModel.playQueue(queue = listOf(ss), startIndex = 0, shuffle = false)
                            }
                        },
                        onPlayAll = { _ ->
                            if (playlistTracksForAlbum.isNotEmpty()) {
                                streamingMusicViewModel.playQueue(queue = playlistTracksForAlbum, startIndex = 0, shuffle = false)
                            }
                        },
                        onShufflePlay = { _ ->
                            if (playlistTracksForAlbum.isNotEmpty()) {
                                streamingMusicViewModel.playQueue(
                                    queue = playlistTracksForAlbum,
                                    startIndex = (0 until playlistTracksForAlbum.size).random(),
                                    shuffle = true
                                )
                            }
                        },
                        onAddToQueue = { },
                        onAddSongToPlaylist = { },
                        onPlayerClick = { },
                        sheetState = albumSheetState,
                        haptics = haptic,
                        onToggleFavorite = { localSong ->
                            playlistTracksForAlbum.firstOrNull { it.id == localSong.id }?.let { streamingSong ->
                                val isLiked = streamingLikedSongs.any { it.id == streamingSong.id }
                                if (isLiked) streamingMusicViewModel.unlikeSong(streamingSong)
                                else streamingMusicViewModel.likeSong(streamingSong)
                            }
                        },
                        favoriteSongs = streamingLikedSongs.map { it.id }.toSet(),
                        showPlayNextAction = false,
                        showAddToQueueAction = false,
                        showAddToPlaylistAction = false,
                        showSongInfoAction = false,
                        showAddToBlacklistAction = false,
                        currentSong = currentSong,
                        isPlaying = isPlaying
                    )
                }
            }

            composable(
                route = StreamingScreen.AddSongsToPlaylist.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId").orEmpty()
                val selectedPlaylistForEdit = remember(playlistId, streamingPlaylists) {
                    streamingPlaylists.firstOrNull { it.id == playlistId }
                }

                if (selectedPlaylistForEdit != null) {
                    val targetPlaylist = remember(selectedPlaylistForEdit) {
                        selectedPlaylistForEdit.toDisplayPlaylist(
                            selectedPlaylistForEdit.getTracks().map { it.toDisplaySong() }
                        )
                    }
                    val availableSongs = remember(targetPlaylist, streamingAddSongsCandidates) {
                        val existingSongIds = targetPlaylist.songs.map { it.id }.toSet()
                        streamingAddSongsCandidates
                            .filterNot { it.id in existingSongIds }
                            .map { it.toDisplaySong() }
                    }
                    var addSongsSearchQuery by remember { mutableStateOf("") }

                    AddToPlaylistScreen(
                        targetPlaylist = targetPlaylist,
                        availableSongs = availableSongs,
                        searchQuery = addSongsSearchQuery,
                        onSearchQueryChange = { addSongsSearchQuery = it },
                        onBackClick = { navController.popBackStack() },
                        onAddSongsToPlaylist = { songs ->
                            val songsToAdd = songs.mapNotNull { song ->
                                streamingAddSongsCandidatesById[song.id]
                            }
                            if (songsToAdd.isNotEmpty()) {
                                streamingMusicViewModel.addSongsToPlaylist(
                                    playlistId = playlistId,
                                    songs = songsToAdd
                                )
                            }
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(
                route = StreamingScreen.Player.route,
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = EaseInOutQuart
                        )
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                exitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 250)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    )
                },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 250)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 200)
                    )
                },
                popEnterTransition = {
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(durationMillis = 350)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            ) {
                val playerSongs = remember(currentSong, queueState.songs) {
                    val queueSongs = queueState.songs
                    if (queueSongs.isNotEmpty()) {
                        queueSongs
                    } else {
                        currentSong?.let { listOf(it) }.orEmpty()
                    }
                }
                var fetchedCurrentAlbumSongs by remember(currentSong?.id, streamingCurrentSong?.albumId) {
                    mutableStateOf<List<Song>>(emptyList())
                }

                LaunchedEffect(streamingCurrentSong?.id, streamingCurrentSong?.albumId) {
                    val activeStreamingSong = streamingCurrentSong
                    val albumId = activeStreamingSong?.albumId?.takeIf { it.isNotBlank() }
                    if (activeStreamingSong == null || albumId == null) {
                        fetchedCurrentAlbumSongs = emptyList()
                        return@LaunchedEffect
                    }

                    val albumTracks = streamingMusicViewModel.getAlbumSongs(
                        StreamingAlbum(
                            id = albumId,
                            title = activeStreamingSong.album,
                            artist = activeStreamingSong.albumArtist?.takeIf { it.isNotBlank() }
                                ?: activeStreamingSong.artist,
                            artworkUri = activeStreamingSong.artworkUri,
                            songCount = 0,
                            year = activeStreamingSong.releaseDate?.take(4)?.toIntOrNull(),
                            sourceType = activeStreamingSong.sourceType
                        )
                    )

                    fetchedCurrentAlbumSongs = albumTracks.mapNotNull { it.toLocalSong() }
                }

                val allKnownPlayerSongs = remember(currentSong, playerSongs, streamingAddSongsCandidates) {
                    (
                        streamingAddSongsCandidates.mapNotNull { it.toLocalSong() } +
                            playerSongs +
                            currentSong?.let { listOf(it) }.orEmpty()
                        )
                        .distinctBy { it.id }
                }
                val allKnownPlayerSongsWithAlbumFetch = remember(allKnownPlayerSongs, fetchedCurrentAlbumSongs) {
                    (allKnownPlayerSongs + fetchedCurrentAlbumSongs).distinctBy { it.id }
                }
                val playerAlbums = remember(currentSong, playerSongs, allKnownPlayerSongsWithAlbumFetch) {
                    buildStreamingPlayerAlbums(
                        currentSong = currentSong,
                        queueSongs = playerSongs,
                        catalogSongs = allKnownPlayerSongsWithAlbumFetch
                    )
                }
                val playerArtists = remember(currentSong, playerSongs, playerAlbums) {
                    buildStreamingPlayerArtists(currentSong, playerSongs, playerAlbums)
                }
                
                var showAddToPlaylistSheet by remember { mutableStateOf(false) }
                var showCreatePlaylistDialog by remember { mutableStateOf(false) }
                val savedPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
                val mappedPlaylists = remember(savedPlaylists) {
                    savedPlaylists.map { it.toLibraryPlaylist() }
                }

                PlayerScreen(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    location = currentDevice,
                    queuePosition = (queueState.currentIndex + 1).coerceAtLeast(1),
                    queueTotal = queueState.songs.size.coerceAtLeast(1),
                    onPlayPause = { localMusicViewModel.togglePlayPause() },
                    isStreamingMode = true,
                    onSkipNext = { localMusicViewModel.skipToNext() },
                    onSkipPrevious = { localMusicViewModel.skipToPrevious() },
                    onSeek = { position -> streamingMusicViewModel.seekTo(position) },
                    onLyricsSeek = { lyricPositionMs ->
                        streamingMusicViewModel.seekTo(lyricPositionMs.coerceAtLeast(0L))
                    },
                    onBack = { navController.popBackStack() },
                    onLocationClick = { localMusicViewModel.showOutputSwitcherDialog() },
                    onQueueClick = {},
                    locations = locations,
                    onLocationSelect = { location -> localMusicViewModel.setCurrentDevice(location) },
                    volume = volume,
                    isMuted = isMuted,
                    onVolumeChange = { newVolume -> localMusicViewModel.setVolume(newVolume) },
                    onToggleMute = { localMusicViewModel.toggleMute() },
                    onMaxVolume = { localMusicViewModel.maxVolume() },
                    onRefreshDevices = { localMusicViewModel.startDeviceMonitoringOnDemand() },
                    onStopDeviceMonitoring = { localMusicViewModel.stopDeviceMonitoringOnDemand() },
                    onToggleShuffle = { localMusicViewModel.toggleShuffle() },
                    onToggleRepeat = { localMusicViewModel.toggleRepeatMode() },
                    onToggleFavorite = { 
                        val activeStreamingSong = streamingCurrentSong ?: currentSong?.let { localSong ->
                            streamingAddSongsCandidatesById[localSong.id]
                        }
                        if (activeStreamingSong != null) {
                            val isLiked = streamingLikedSongIds.contains(activeStreamingSong.id)
                            if (isLiked) {
                                streamingMusicViewModel.unlikeSong(activeStreamingSong)
                            } else {
                                streamingMusicViewModel.likeSong(activeStreamingSong)
                            }
                        }
                    },
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = streamingCurrentSong?.let { streamingLikedSongIds.contains(it.id) } == true,
                    showLyrics = showLyrics,
                    onlineOnlyLyrics = showOnlineOnlyLyrics,
                    lyrics = lyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    onRetryLyrics = { localMusicViewModel.retryFetchLyrics() },
                    playlists = mappedPlaylists,
                    showAddToPlaylistSheet = showAddToPlaylistSheet,
                    onAddToPlaylist = { showAddToPlaylistSheet = true },
                    onAddToPlaylistSheetDismiss = { showAddToPlaylistSheet = false },
                    onAddSongToPlaylist = { song, playlistId ->
                        val streamingSong = streamingMusicViewModel.currentSong.value
                        if (streamingSong != null && streamingSong.id == song.id) {
                            streamingMusicViewModel.addSongToPlaylist(playlistId, streamingSong)
                        }
                        showAddToPlaylistSheet = false
                    },
                    onCreatePlaylist = { name ->
                        streamingMusicViewModel.createPlaylist(name)
                        showAddToPlaylistSheet = false
                    },
                    onShowCreatePlaylistDialog = {
                        showCreatePlaylistDialog = true
                    },
                    queue = queueState.songs,
                    onSongClick = { song -> localMusicViewModel.playSong(song) },
                    onSongClickAtIndex = { index -> localMusicViewModel.playSongAtIndex(index) },
                    onRemoveFromQueueAtIndex = { index -> localMusicViewModel.removeFromQueueAtIndex(index) },
                    onMoveQueueItem = { fromIndex, toIndex ->
                        localMusicViewModel.moveQueueItem(fromIndex, toIndex)
                    },
                    onAddSongsToQueue = { navigateToTopLevel(StreamingScreen.Search.route) },
                    onNavigateToLibrary = { navigateToTopLevel(StreamingScreen.Library.route) },
                    onClearQueue = { localMusicViewModel.clearQueue() },
                    isMediaLoading = isMediaLoading,
                    isSeeking = isSeeking,
                    songs = allKnownPlayerSongsWithAlbumFetch,
                    albums = playerAlbums,
                    artists = playerArtists,
                    onPlayAlbumSongs = { albumSongs -> localMusicViewModel.playSongs(albumSongs) },
                    onShuffleAlbumSongs = { albumSongs -> localMusicViewModel.playShuffled(albumSongs) },
                    onPlayArtistSongs = { artistSongs -> localMusicViewModel.playSongs(artistSongs) },
                    onShuffleArtistSongs = { artistSongs -> localMusicViewModel.playShuffled(artistSongs) },
                    appSettings = appSettings,
                    musicViewModel = localMusicViewModel,
                    navController = navController
                )
                
                if (showCreatePlaylistDialog) {
                    chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog(
                        onDismiss = { showCreatePlaylistDialog = false },
                        onConfirm = { name ->
                            streamingMusicViewModel.createPlaylist(name)
                            showCreatePlaylistDialog = false
                        }
                    )
                }
            }

            composable(
                route = Screen.TunerQueuePlayback.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                QueuePlaybackSettingsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(
                route = Screen.Equalizer.route,
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                EqualizerScreen(
                    navController = navController,
                    viewModel = localMusicViewModel
                )
            }

            composable(
                route = StreamingScreen.ServiceSetup.route,
                arguments = listOf(navArgument("serviceId") { type = NavType.StringType }),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) { backStackEntry ->
                val serviceId = backStackEntry.arguments?.getString("serviceId").orEmpty()
                if (serviceId.isNotBlank()) {
                    StreamingServiceSetupScreen(
                        serviceId = serviceId,
                        viewModel = streamingMusicViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        // Streaming Add to Playlist Bottom Sheet
        if (showStreamingAddToPlaylist && selectedStreamingSongForPlaylist != null) {
            val displaySong = remember(selectedStreamingSongForPlaylist) {
                selectedStreamingSongForPlaylist!!.toDisplaySong()
            }
            val displayPlaylists = remember(streamingPlaylists) {
                streamingPlaylists.map { it.toLibraryPlaylist() }
            }
            var showCreatePlaylistDialog by remember { mutableStateOf(false) }

            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onConfirm = { name ->
                        streamingMusicViewModel.createPlaylist(name)
                        showCreatePlaylistDialog = false
                    }
                )
            }

            AddToPlaylistBottomSheet(
                song = displaySong,
                playlists = displayPlaylists,
                onDismissRequest = {
                    showStreamingAddToPlaylist = false
                    selectedStreamingSongForPlaylist = null
                },
                onAddToPlaylist = { playlist ->
                    selectedStreamingSongForPlaylist?.let { song ->
                        streamingMusicViewModel.addSongToPlaylist(playlist.id, song)
                    }
                    showStreamingAddToPlaylist = false
                    selectedStreamingSongForPlaylist = null
                },
                onCreateNewPlaylist = {
                    showCreatePlaylistDialog = true
                }
            )
        }

    }
    }
}

private fun StreamingSong.toLocalSong(): Song? {
    val playbackUri = when {
        !streamingUrl.isNullOrBlank() -> Uri.parse(streamingUrl)
        !previewUrl.isNullOrBlank() -> Uri.parse(previewUrl)
        else -> Uri.parse("streaming://track/$id")
    }

    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId.orEmpty(),
        duration = duration,
        uri = playbackUri,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albumArtist = albumArtist
    )
}

private fun StreamingSong.toDisplaySong(): Song {
    val playbackUri = when {
        !streamingUrl.isNullOrBlank() -> Uri.parse(streamingUrl)
        !previewUrl.isNullOrBlank() -> Uri.parse(previewUrl)
        else -> Uri.parse("streaming://track/$id")
    }

    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        uri = playbackUri,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    )
}

private fun StreamingArtist.toDisplayArtist(
    fallbackName: String,
    songs: List<Song>,
    albums: List<Album>
): Artist {
    return Artist(
        id = id,
        name = name.ifBlank { fallbackName.ifBlank { "Artist" } },
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albums = albums,
        songs = songs,
        numberOfAlbums = if (albumCount > 0) albumCount else albums.size,
        numberOfTracks = if (songCount > 0) songCount else songs.size
    )
}

private fun StreamingPlaylist.toDisplayPlaylist(displaySongs: List<Song>): Playlist {
    return Playlist(
        id = id,
        name = name,
        songs = displaySongs,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    )
}

private fun inferArtistNameFromId(artistId: String): String {
    if (artistId.isBlank()) {
        return ""
    }

    val decoded = Uri.decode(artistId)
    val normalized = decoded
        .substringAfterLast("artist:", decoded)
        .substringAfterLast("::")
        .substringAfterLast(":")
        .substringAfterLast("/")
        .replace('_', ' ')
        .replace('-', ' ')
        .replace('+', ' ')
        .trim()

    return normalized.ifBlank { decoded }
}

private fun buildStreamingPlayerAlbums(
    currentSong: Song?,
    queueSongs: List<Song>,
    catalogSongs: List<Song> = emptyList()
): List<Album> {
    val baseSong = currentSong ?: queueSongs.firstOrNull() ?: return emptyList()
    val baseAlbumId = baseSong.albumId.takeIf { it.isNotBlank() }
    val baseAlbumArtist = baseSong.albumArtist?.takeIf { it.isNotBlank() } ?: baseSong.artist
    val candidateSongs = (queueSongs + catalogSongs + listOf(baseSong)).distinctBy { it.id }

    val matchingSongs = candidateSongs.filter { song ->
        val songAlbumId = song.albumId.takeIf { it.isNotBlank() }
        when {
            baseAlbumId != null -> songAlbumId == baseAlbumId
            else -> {
                val songAlbumArtist = song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist
                song.album.equals(baseSong.album, ignoreCase = true) &&
                    songAlbumArtist.equals(baseAlbumArtist, ignoreCase = true)
            }
        }
    }
    val albumSongs = if (matchingSongs.isNotEmpty()) matchingSongs else listOf(baseSong)

    return listOf(
        Album(
            id = baseAlbumId
                ?: "streaming-player:album:${baseAlbumArtist.lowercase()}:${baseSong.album.lowercase()}",
            title = baseSong.album,
            artist = baseAlbumArtist,
            artworkUri = baseSong.artworkUri,
            songs = albumSongs,
            numberOfSongs = albumSongs.size
        )
    )
}

private fun buildStreamingPlayerArtists(
    currentSong: Song?,
    queueSongs: List<Song>,
    playerAlbums: List<Album>
): List<Artist> {
    val baseSong = currentSong ?: queueSongs.firstOrNull() ?: return emptyList()
    val artistName = baseSong.albumArtist?.takeIf { it.isNotBlank() } ?: baseSong.artist
    val matchingSongs = queueSongs.filter {
        it.artist.equals(baseSong.artist, ignoreCase = true) ||
            (!baseSong.albumArtist.isNullOrBlank() && it.albumArtist.equals(baseSong.albumArtist, ignoreCase = true))
    }
    val artistSongs = if (matchingSongs.isNotEmpty()) matchingSongs else listOf(baseSong)

    return listOf(
        Artist(
            id = "streaming-player:artist:${artistName.lowercase()}",
            name = artistName,
            artworkUri = baseSong.artworkUri,
            albums = playerAlbums,
            songs = artistSongs,
            numberOfAlbums = playerAlbums.size,
            numberOfTracks = artistSongs.size
        )
    )
}

@Composable
private fun StreamingBottomBar(
    currentRoute: String,
    navController: NavHostController,
    context: android.content.Context,
    haptic: HapticFeedback,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ExpressiveShapes.Full,
                tonalElevation = 3.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .height(MusicDimensions.bottomNavigationHeight)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        Triple(
                            StreamingScreen.Home.route,
                            context.getString(R.string.home),
                            Pair(RhythmIcons.HomeFilled, RhythmIcons.Home)
                        ),
                        Triple(
                            StreamingScreen.Library.route,
                            context.getString(R.string.library),
                            Pair(RhythmIcons.Navigation.Library, RhythmIcons.Navigation.LibraryOutlined)
                        )
                    )

                    items.forEach { (route, title, icons) ->
                        val isSelected = currentRoute == route
                        val (selectedIcon, unselectedIcon) = icons

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "streaming_bottom_scale_$title"
                        )

                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.7f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "streaming_bottom_alpha_$title"
                        )

                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 120.dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "streaming_bottom_pill_$title"
                        )

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(300),
                            label = "streaming_bottom_icon_$title"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        alpha = animatedAlpha
                                    }
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, ExpressiveShapes.Full)
                                                .height(48.dp)
                                                .widthIn(min = pillWidth)
                                                .padding(horizontal = 18.dp)
                                        } else {
                                            Modifier.padding(horizontal = 16.dp)
                                        }
                                    )
                            ) {
                                androidx.compose.animation.Crossfade(
                                    targetState = isSelected,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessVeryLow
                                    ),
                                    label = "streaming_icon_crossfade_$title"
                                ) { selected ->
                                    Icon(
                                        imageVector = if (selected) selectedIcon else unselectedIcon,
                                        contentDescription = title,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) + expandHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                    exit = fadeOut(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + shrinkHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                ) {
                                    Row {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = iconColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val searchInteractionSource = remember { MutableInteractionSource() }
            val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
            val searchScale by animateFloatAsState(
                targetValue = if (isSearchPressed) 0.88f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "streaming_search_button_scale"
            )

            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                    onSearchClick()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                interactionSource = searchInteractionSource,
                modifier = Modifier
                    .size(MusicDimensions.bottomNavigationHeight)
                    .graphicsLayer {
                        scaleX = searchScale
                        scaleY = searchScale
                    }
            ) {
                Icon(
                    imageVector = RhythmIcons.Search,
                    contentDescription = context.getString(R.string.search),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}
