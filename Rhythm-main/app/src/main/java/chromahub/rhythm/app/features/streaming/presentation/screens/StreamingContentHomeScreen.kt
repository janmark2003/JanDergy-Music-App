package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveCard
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.core.domain.model.SourceType
import chromahub.rhythm.app.features.streaming.domain.model.StreamingAlbum
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingServiceId
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.features.streaming.presentation.components.settings.StreamingHomeSectionOrderBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository
import android.net.Uri
import chromahub.rhythm.app.shared.presentation.components.common.ButtonGroupStyle
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveElevatedCard
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveButtonGroup
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveFilledIconButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveGroupButton
import chromahub.rhythm.app.shared.presentation.components.common.ExpressiveShapeTarget
import chromahub.rhythm.app.shared.presentation.components.common.rememberExpressiveShapeFor
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.M3ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STREAMING_SECTION_GREETING = "GREETING"
private const val STREAMING_SECTION_RHYTHM_GUARD = "RHYTHM_GUARD"
private const val STREAMING_SECTION_RHYTHM_STATS = "RHYTHM_STATS"
private const val STREAMING_SECTION_DISCOVER = "DISCOVER"
private const val STREAMING_SECTION_RECENTLY_PLAYED = "RECENTLY_PLAYED"
private const val STREAMING_SECTION_ARTISTS = "ARTISTS"
private const val STREAMING_SECTION_NEW_RELEASES = "NEW_RELEASES"

private val defaultStreamingHomeSections = listOf(
    STREAMING_SECTION_GREETING,
    STREAMING_SECTION_DISCOVER,
    STREAMING_SECTION_RECENTLY_PLAYED,
    STREAMING_SECTION_ARTISTS,
    STREAMING_SECTION_RHYTHM_GUARD,
    STREAMING_SECTION_RHYTHM_STATS,
    STREAMING_SECTION_NEW_RELEASES
)

@Composable
fun StreamingContentHomeScreen(
    viewModel: StreamingMusicViewModel,
    recentlyPlayedSongs: List<Song>,
    playbackStatsSummary: PlaybackStatsRepository.PlaybackStatsSummary?,
    listeningTimeMs: Long,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToRhythmGuard: () -> Unit,
    onNavigateToRhythmStats: () -> Unit,
    onNavigateToArtist: (StreamingArtist) -> Unit,
    onConfigureService: (String) -> Unit,
    onOpenAlbumSheet: (StreamingAlbum) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }

    val selectedService by appSettings.streamingService.collectAsState()
    val sessions by viewModel.serviceSessions.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val followedArtists by viewModel.followedArtists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedHomeContent by viewModel.hasLoadedHomeContent.collectAsState()
    val hasLoadedLibrary by viewModel.hasLoadedLibrary.collectAsState()
    val error by viewModel.error.collectAsState()
    val sectionOrder by appSettings.streamingHomeSectionOrder.collectAsState()
    val showGreetingSection by appSettings.streamingHomeShowGreeting.collectAsState()
    val showRhythmGuardSection by appSettings.streamingHomeShowRhythmGuard.collectAsState()
    val showRhythmStatsSection by appSettings.streamingHomeShowRhythmStats.collectAsState()
    val showDiscoverSection by appSettings.streamingHomeShowRecommended.collectAsState()
    val discoverAutoScroll by appSettings.homeDiscoverAutoScroll.collectAsState()
    val discoverAutoScrollInterval by appSettings.homeDiscoverAutoScrollInterval.collectAsState()
    val showRecentSection by appSettings.streamingHomeShowRecentlyPlayed.collectAsState()
    val showArtistsSection by appSettings.streamingHomeShowArtists.collectAsState()
    val showNewReleasesSection by appSettings.streamingHomeShowNewReleases.collectAsState()
    val persistedSongsPlayed by appSettings.songsPlayed.collectAsState()
    val persistedUniqueArtists by appSettings.uniqueArtists.collectAsState()
    val dailyListeningStats by appSettings.dailyListeningStats.collectAsState()
    val rhythmGuardMode by appSettings.rhythmGuardMode.collectAsState()
    val rhythmGuardAge by appSettings.rhythmGuardAge.collectAsState()
    val rhythmGuardAlertThresholdMinutes by appSettings.rhythmGuardAlertThresholdMinutes.collectAsState()
    val rhythmGuardTimeoutUntilMs by appSettings.rhythmGuardTimeoutUntilMs.collectAsState()
    val loadingSectionHeight = LocalConfiguration.current.screenHeightDp.dp

    var showSectionOrderBottomSheet by remember { mutableStateOf(false) }

    var showAlbumBottomSheet by remember { mutableStateOf(false) }
    var selectedAlbumForSheet by remember { mutableStateOf<StreamingAlbum?>(null) }
    val albumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading

    // Use internal state for album sheet instead of the parameter callback
    val handleAlbumClick: (StreamingAlbum) -> Unit = { album ->
        if (album.tracks.isEmpty()) {
            selectedAlbumForSheet = album
            scope.launch {
                val tracks = viewModel.getAlbumSongs(album)
                if (tracks.isNotEmpty()) {
                    // Update state with loaded tracks
                    selectedAlbumForSheet = album.copy(tracks = tracks)
                }
            }
        } else {
            selectedAlbumForSheet = album
        }
        showAlbumBottomSheet = true
    }

    val selectedOption = remember(selectedService) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == selectedService }
    }
    val selectedServiceName = selectedOption?.let { context.getString(it.nameRes) }
        ?: context.getString(R.string.streaming_not_selected)
    val selectedServiceId = selectedOption?.id ?: selectedService
    val fallbackServiceId = StreamingServiceOptions.defaults.firstOrNull()?.id.orEmpty()
    val serviceIdForConfig = selectedServiceId.ifBlank { fallbackServiceId }

    val isSelectedServiceConnected = sessions[selectedServiceId]?.isConnected == true
    val homeErrorMessage = error?.takeIf { it.isNotBlank() }

    val hasAnyWidgetContent =
        recommendations.isNotEmpty() ||
            newReleases.isNotEmpty() ||
            followedArtists.isNotEmpty()

    val resolvedSectionOrder = remember(sectionOrder) {
        (
            sectionOrder
                .map(::normalizeStreamingSectionId)
                .filter { it in defaultStreamingHomeSections } + defaultStreamingHomeSections
            ).distinct()
    }
    val sectionVisibility = remember(
        showGreetingSection,
        showDiscoverSection,
        showRhythmGuardSection,
        showRhythmStatsSection,
        showRecentSection,
        showArtistsSection,
        showNewReleasesSection
    ) {
        mapOf(
            STREAMING_SECTION_GREETING to showGreetingSection,
            STREAMING_SECTION_DISCOVER to showDiscoverSection,
            STREAMING_SECTION_RHYTHM_GUARD to showRhythmGuardSection,
            STREAMING_SECTION_RHYTHM_STATS to showRhythmStatsSection,
            STREAMING_SECTION_RECENTLY_PLAYED to showRecentSection,
            STREAMING_SECTION_ARTISTS to showArtistsSection,
            STREAMING_SECTION_NEW_RELEASES to showNewReleasesSection
        )
    }
    val orderedSections = remember(resolvedSectionOrder) {
        val fixedTopSections = listOf(STREAMING_SECTION_GREETING, STREAMING_SECTION_DISCOVER)
        fixedTopSections + resolvedSectionOrder.filterNot { it in fixedTopSections }
    }
    val visibleSections = remember(orderedSections, sectionVisibility) {
        orderedSections.filter { sectionVisibility[it] == true }
    }

    val recommendationWidgetSongs = recommendations.take(12)
    val streamingArtists = remember(followedArtists) {
        followedArtists
            .sortedBy { it.name.lowercase() }
            .take(12)
    }
    val streamingRecentSongs = remember(recentlyPlayedSongs, selectedServiceId) {
        recentlyPlayedSongs
            .mapNotNull { song -> song.toStreamingSongOrNull(selectedServiceId) }
            .distinctBy { it.id }
            .take(20)
    }
    val recentSongsForSelectedService = remember(streamingRecentSongs, selectedServiceId) {
        val serviceSongs = streamingRecentSongs.filter { it.id.startsWith("$selectedServiceId::") }
        if (serviceSongs.isNotEmpty()) serviceSongs else streamingRecentSongs
    }

    val totalListeningDurationMs = playbackStatsSummary?.totalDurationMs?.takeIf { it > 0L } ?: listeningTimeMs
    val songsPlayedForStats = playbackStatsSummary?.totalPlayCount?.takeIf { it > 0 } ?: persistedSongsPlayed
    val uniqueArtistsForStats = playbackStatsSummary?.uniqueArtists?.takeIf { it > 0 } ?: persistedUniqueArtists
    val rhythmGuardPolicy = remember(rhythmGuardAge) { appSettings.getRhythmGuardPolicy(rhythmGuardAge) }
    val rhythmGuardRecommendedMinutes = when (rhythmGuardMode) {
        AppSettings.RHYTHM_GUARD_MODE_MANUAL -> rhythmGuardAlertThresholdMinutes
            .takeIf { it > 0 }
            ?: rhythmGuardPolicy.recommendedDailyMinutes
        else -> rhythmGuardPolicy.recommendedDailyMinutes
    }
    val todayListeningMinutes = remember(dailyListeningStats, songsPlayedForStats, totalListeningDurationMs) {
        appSettings.estimateRhythmGuardTodayListeningMinutes(
            dailyListeningStats = dailyListeningStats,
            songsPlayed = songsPlayedForStats,
            listeningTimeMs = totalListeningDurationMs
        )
    }
    val rhythmGuardTimeoutRemainingMs = (rhythmGuardTimeoutUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val isRhythmGuardTimeoutActive = rhythmGuardTimeoutRemainingMs > 0L
    val greetingMessage = rememberGreetingMessage(context)
    val greetingQuote = rememberGreetingQuote(context)

    LaunchedEffect(selectedServiceId, isSelectedServiceConnected, hasLoadedHomeContent) {
        if (isSelectedServiceConnected && !hasLoadedHomeContent) {
            viewModel.loadHomeContent()
        }
    }

    LaunchedEffect(isSelectedServiceConnected, showArtistsSection, hasLoadedLibrary) {
        if (isSelectedServiceConnected && showArtistsSection && !hasLoadedLibrary) {
            viewModel.loadLibrary()
        }
    }

    if (showSectionOrderBottomSheet) {
        StreamingHomeSectionOrderBottomSheet(
            appSettings = appSettings,
            onDismiss = { showSectionOrderBottomSheet = false }
        )
    }

    CollapsibleHeaderScreen(
        title = stringResource(id = R.string.streaming_integration_title),
        headerDisplayMode = 1,
        actions = {
            if (isSelectedServiceConnected) {
                ExpressiveFilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(
                            context,
                            haptics,
                            HapticFeedbackType.LongPress
                        )
                        showSectionOrderBottomSheet = true
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Reorder,
                        contentDescription = stringResource(id = R.string.cd_reorder_home_sections)
                    )
                }
            }

            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onNavigateToSettings()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = stringResource(id = R.string.home_settings_cd)
                )
            }
        }
    ) { contentModifier ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadHomeContent() },
            state = pullToRefreshState,
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

            when {
                !isSelectedServiceConnected -> {
                    item {
                        StreamingHomeStateCard(
                            title = stringResource(id = R.string.streaming_home_selected_service_unavailable),
                            subtitle = stringResource(
                                id = R.string.streaming_home_connect_selected_service,
                                selectedServiceName
                            ),
                            icon = Icons.Rounded.CloudOff,
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            actionText = stringResource(id = R.string.streaming_manage_service),
                            onAction = {
                                HapticUtils.performHapticFeedback(
                                    context,
                                    haptics,
                                    HapticFeedbackType.LongPress
                                )
                                if (serviceIdForConfig.isNotBlank()) {
                                    onConfigureService(serviceIdForConfig)
                                } else {
                                    onNavigateToSettings()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                    }
                }

                isSelectedServiceConnected && (isLoading || !hasLoadedHomeContent) -> {
                    item {
                        Box(
                            modifier = Modifier
                                .height(loadingSectionHeight)
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StreamingHomeStateCard(
                                title = stringResource(id = R.string.streaming_library_syncing),
                                subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint),
                                icon = Icons.Filled.History,
                                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                showProgressIndicator = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        }
                    }
                }

                !homeErrorMessage.isNullOrBlank() && !hasAnyWidgetContent -> {
                    item {
                        StreamingHomeStateCard(
                            title = stringResource(id = R.string.streaming_home_selected_service_unavailable),
                            subtitle = homeErrorMessage,
                            icon = Icons.Rounded.Info,
                            iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            actionText = stringResource(id = R.string.streaming_manage_service),
                            onAction = {
                                HapticUtils.performHapticFeedback(
                                    context,
                                    haptics,
                                    HapticFeedbackType.LongPress
                                )
                                if (serviceIdForConfig.isNotBlank()) {
                                    onConfigureService(serviceIdForConfig)
                                } else {
                                    onNavigateToSettings()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        )
                    }
                }

                isSelectedServiceConnected && !hasAnyWidgetContent -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StreamingHomeStateCard(
                                title = stringResource(id = R.string.streaming_home_no_content_title),
                                subtitle = stringResource(id = R.string.streaming_home_no_content_hint),
                                icon = Icons.Filled.Home,
                                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                actionText = stringResource(id = R.string.streaming_manage_service),
                                onAction = {
                                    HapticUtils.performHapticFeedback(
                                        context,
                                        haptics,
                                        HapticFeedbackType.LongPress
                                    )
                                    if (serviceIdForConfig.isNotBlank()) {
                                        onConfigureService(serviceIdForConfig)
                                    } else {
                                        onNavigateToSettings()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        }
                    }
                }

                else -> {
                    visibleSections.forEach { sectionId ->
                        when (sectionId) {
                        STREAMING_SECTION_GREETING -> {
                            item(key = "streaming_section_greeting") {
                                StreamingGreetingWidgetCard(
                                    greeting = greetingMessage,
                                    quote = greetingQuote,
                                    selectedServiceName = selectedServiceName,
                                    onNavigateToSearch = onNavigateToSearch
                                )
                            }
                        }

                        STREAMING_SECTION_DISCOVER -> {
                            item(key = "streaming_section_discover") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    if (showGreetingSection) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.home_section_discover),
                                        subtitle = stringResource(
                                            id = R.string.streaming_home_widget_recommended_subtitle,
                                            selectedServiceName
                                        ),
                                        onPlayAll = recommendationWidgetSongs.takeIf { it.isNotEmpty() }
                                            ?.let { songs ->
                                                {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.playQueue(songs, startIndex = 0, shuffle = false)
                                                }
                                            },
                                        onShufflePlay = recommendationWidgetSongs.takeIf { it.size > 1 }
                                            ?.let { songs ->
                                                {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.playQueue(songs, startIndex = 0, shuffle = true)
                                                }
                                            }
                                    )
                                }

                                    if (recommendationWidgetSongs.isEmpty()) {
                                    StreamingSectionEmptyCard(
                                        icon = Icons.Rounded.Headphones,
                                        title = stringResource(id = R.string.streaming_home_widget_recommended_empty),
                                        subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                                    )
                                } else {
                                    StreamingRecommendationsCarousel(
                                        songs = recommendationWidgetSongs,
                                        autoScrollEnabled = discoverAutoScroll,
                                        autoScrollIntervalSeconds = discoverAutoScrollInterval,
                                        onPlaySong = { _, index ->
                                            viewModel.playQueue(
                                                queue = recommendationWidgetSongs,
                                                startIndex = index,
                                                shuffle = false
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        }

                        STREAMING_SECTION_RHYTHM_GUARD -> {
                            item(key = "streaming_section_rhythm_guard") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.settings_rhythm_guard),
                                        subtitle = stringResource(id = R.string.settings_rhythm_guard_list_desc)
                                    )

                                    StreamingRhythmGuardCard(
                                        rhythmGuardMode = rhythmGuardMode,
                                        rhythmGuardRecommendedMinutes = rhythmGuardRecommendedMinutes,
                                        todayListeningMinutes = todayListeningMinutes,
                                        isGuardTimeoutActive = isRhythmGuardTimeoutActive,
                                        guardTimeoutRemainingMs = rhythmGuardTimeoutRemainingMs,
                                        onCardClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticFeedbackType.LongPress
                                            )
                                            onNavigateToRhythmGuard()
                                        }
                                    )
                                }
                            }
                        }

                        STREAMING_SECTION_ARTISTS -> {
                            item(key = "streaming_section_artists") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.home_top_artists),
                                        subtitle = stringResource(id = R.string.home_top_artists_subtitle)
                                    )

                                    if (streamingArtists.isEmpty()) {
                                        StreamingSectionEmptyCard(
                                            icon = Icons.Rounded.Person,
                                            title = stringResource(id = R.string.home_no_artists),
                                            subtitle = stringResource(id = R.string.home_no_artists_desc)
                                        )
                                    } else {
                                        StreamingArtistsSection(
                                            artists = streamingArtists,
                                            onArtistClick = onNavigateToArtist
                                        )
                                    }
                                }
                            }
                        }

                        STREAMING_SECTION_RHYTHM_STATS -> {
                            item(key = "streaming_section_rhythm_stats") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.settings_rhythm_stats),
                                        subtitle = stringResource(id = R.string.settings_rhythm_stats_desc)
                                    )

                                    StreamingRhythmStatsCard(
                                        listeningDurationMs = totalListeningDurationMs,
                                        songsPlayed = songsPlayedForStats,
                                        uniqueArtists = uniqueArtistsForStats,
                                        onCardClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptics,
                                                HapticFeedbackType.LongPress
                                            )
                                            onNavigateToRhythmStats()
                                        }
                                    )
                                }
                            }
                        }

                        STREAMING_SECTION_RECENTLY_PLAYED -> {
                            item(key = "streaming_section_recent") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.home_section_recently_played),
                                        subtitle = stringResource(id = R.string.home_recently_played_subtitle),
                                        onPlayAll = recentSongsForSelectedService.takeIf { it.isNotEmpty() }
                                            ?.let { songs ->
                                                {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.playQueue(
                                                        queue = songs,
                                                        startIndex = 0,
                                                        shuffle = false
                                                    )
                                                }
                                            },
                                        onShufflePlay = recentSongsForSelectedService.takeIf { it.size > 1 }
                                            ?.let { songs ->
                                                {
                                                    HapticUtils.performHapticFeedback(
                                                        context,
                                                        haptics,
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.playQueue(
                                                        queue = songs,
                                                        startIndex = 0,
                                                        shuffle = true
                                                    )
                                                }
                                            }
                                    )

                                    if (recentSongsForSelectedService.isEmpty()) {
                                    StreamingSectionEmptyCard(
                                        icon = Icons.Default.History,
                                        title = stringResource(id = R.string.home_no_recent_activity),
                                        subtitle = stringResource(id = R.string.home_no_recent_activity_desc)
                                    )
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        itemsIndexed(
                                            items = recentSongsForSelectedService,
                                            key = { _, song -> song.id }
                                        ) { index, song ->
                                            StreamingSongWidgetCard(
                                                song = song,
                                                onPlaySong = {
                                                    viewModel.playQueue(
                                                        queue = recentSongsForSelectedService,
                                                        startIndex = index,
                                                        shuffle = false
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }

                        STREAMING_SECTION_NEW_RELEASES -> {
                            item(key = "streaming_section_new_releases") {
                                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    StreamingWidgetSectionTitle(
                                        title = stringResource(id = R.string.streaming_home_widget_new_releases_title),
                                        subtitle = stringResource(
                                            id = R.string.streaming_home_widget_new_releases_subtitle,
                                            selectedServiceName
                                        )
                                    )

                                    if (newReleases.isEmpty()) {
                                    StreamingSectionEmptyCard(
                                        icon = Icons.Rounded.NewReleases,
                                        title = stringResource(id = R.string.streaming_home_widget_new_releases_empty),
                                        subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                                    )
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = newReleases.take(12),
                                            key = { it.id }
                                        ) { album ->
                                            StreamingAlbumWidgetCard(
                                                album = album,
                                                onPlayAlbum = { viewModel.playAlbum(album) },
                                                onAlbumClick = { handleAlbumClick(album) }
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

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
            }
        }

        // Album BottomSheet
        if (showAlbumBottomSheet && selectedAlbumForSheet != null) {
            val albumForSheet = selectedAlbumForSheet!!
            val streamingTracks = albumForSheet.tracks
            val libraryAlbum = albumForSheet.toLibraryAlbum(recentlyPlayedSongs)
            
                AlbumBottomSheet(
                    album = libraryAlbum,
                    onDismiss = {
                        showAlbumBottomSheet = false
                        selectedAlbumForSheet = null
                    },
                    onSongClick = { song ->
                        val streamingSong = streamingTracks.firstOrNull { it.id == song.id }
                        streamingSong?.let { ss ->
                            viewModel.playQueue(queue = listOf(ss), startIndex = 0, shuffle = false)
                        }
                    },
                    onPlayAll = { songs ->
                        if (streamingTracks.isNotEmpty()) {
                            viewModel.playQueue(queue = streamingTracks, startIndex = 0, shuffle = false)
                        }
                    },
                    onShufflePlay = { songs ->
                        if (streamingTracks.isNotEmpty()) {
                            val startIndex = (0 until streamingTracks.size).random()
                            viewModel.playQueue(queue = streamingTracks, startIndex = startIndex, shuffle = true)
                        }
                    },
                    onAddToQueue = { },
                    onAddSongToPlaylist = { },
                    onPlayerClick = { },
                    sheetState = albumSheetState,
                    haptics = haptics,
                    showPlayNextAction = false,
                    showAddToQueueAction = false,
                    showToggleFavoriteAction = false,
                    showAddToPlaylistAction = false,
                    showSongInfoAction = false,
                    showAddToBlacklistAction = false,
                    currentSong = null,
                    isPlaying = false
                )
            
        }
    }
}

@Composable
private fun StreamingHomeStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    showProgressIndicator: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = noShadowCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (showProgressIndicator) {
                        androidx.compose.material3.ContainedLoadingIndicator()
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = actionText)
                }
            }
        }
    }
}

@Composable
private fun StreamingDisconnectedStateCard(
    selectedServiceName: String,
    onConfigureService: () -> Unit,
    modifier: Modifier = Modifier
) {
    StreamingHomeStateCard(
        title = stringResource(id = R.string.streaming_home_selected_service_unavailable),
        subtitle = stringResource(
            id = R.string.streaming_home_connect_selected_service,
            selectedServiceName
        ),
        icon = Icons.Rounded.CloudOff,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        iconTint = MaterialTheme.colorScheme.onErrorContainer,
        actionText = stringResource(id = R.string.streaming_manage_service),
        onAction = onConfigureService,
        modifier = modifier
    )
}

@Composable
private fun StreamingWidgetSectionTitle(
    title: String,
    subtitle: String,
    onPlayAll: (() -> Unit)? = null,
    onShufflePlay: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (onPlayAll != null || onShufflePlay != null) {
            ExpressiveButtonGroup(style = ButtonGroupStyle.Tonal) {
                onPlayAll?.let { playAction ->
                    ExpressiveGroupButton(
                        onClick = playAction,
                        isStart = true,
                        isEnd = onShufflePlay == null
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.action_play),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                onShufflePlay?.let { shuffleAction ->
                    ExpressiveGroupButton(
                        onClick = shuffleAction,
                        isStart = onPlayAll == null,
                        isEnd = true
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        if (onPlayAll == null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.action_shuffle),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingGreetingWidgetCard(
    greeting: String,
    quote: String,
    selectedServiceName: String,
    onNavigateToSearch: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Mirror local HomeScreen's ModernWelcomeSection design and text logic
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactWidth = screenWidthDp < 400
    val isTablet = screenWidthDp >= 600

    val greetingFontSize = when {
        isCompactWidth -> 28.sp
        isTablet -> 36.sp
        else -> 32.sp
    }
    val messageFontSize = when {
        isCompactWidth -> 12.sp
        isTablet -> 16.sp
        else -> 14.sp
    }
    val quoteFontSize = when {
        isCompactWidth -> 11.sp
        isTablet -> 14.sp
        else -> 12.sp
    }

    val timeBasedQuote = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 0..4 -> listOf(
                context.getString(R.string.home_quote_late_night_1),
                context.getString(R.string.home_quote_late_night_2),
                context.getString(R.string.home_quote_late_night_3),
                context.getString(R.string.home_quote_late_night_4)
            )
            hour in 5..11 -> listOf(
                context.getString(R.string.home_quote_morning_1),
                context.getString(R.string.home_quote_morning_2),
                context.getString(R.string.home_quote_morning_3),
                context.getString(R.string.home_quote_morning_4)
            )
            hour in 12..16 -> listOf(
                context.getString(R.string.home_quote_afternoon_1),
                context.getString(R.string.home_quote_afternoon_2),
                context.getString(R.string.home_quote_afternoon_3),
                context.getString(R.string.home_quote_afternoon_4)
            )
            hour in 17..20 -> listOf(
                context.getString(R.string.home_quote_evening_1),
                context.getString(R.string.home_quote_evening_2),
                context.getString(R.string.home_quote_evening_3),
                context.getString(R.string.home_quote_evening_4)
            )
            else -> listOf(
                context.getString(R.string.home_quote_night_1),
                context.getString(R.string.home_quote_night_2),
                context.getString(R.string.home_quote_night_3),
                context.getString(R.string.home_quote_night_4)
            )
        }.random()
    }

    ExpressiveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                onNavigateToSearch()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = ExpressiveShapes.ExtraLarge
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Text(
                        text = "✨",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.alpha(0.12f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 0.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "emoji_pulse")
                    val emojiScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "emoji_scale"
                    )

                    Text(
                        text = "☀️",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .graphicsLayer { scaleX = emojiScale; scaleY = emojiScale }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = timeBasedQuote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }

                    ExpressiveFilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                            onNavigateToSearch()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = context.getString(R.string.cd_search),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingRhythmGuardCard(
    rhythmGuardMode: String,
    rhythmGuardRecommendedMinutes: Int,
    todayListeningMinutes: Int,
    isGuardTimeoutActive: Boolean,
    guardTimeoutRemainingMs: Long,
    onCardClick: () -> Unit
) {
    val guardModeLabel = rhythmGuardModeDisplayName(rhythmGuardMode)
    val guardSnapshotLabel = if (todayListeningMinutes > rhythmGuardRecommendedMinutes) {
        stringResource(id = R.string.settings_rhythm_guard_snapshot_widget_above_limit)
    } else {
        stringResource(id = R.string.settings_rhythm_guard_snapshot_widget_within_limit)
    }
    val guardStateSummary = if (isGuardTimeoutActive) {
        stringResource(
            id = R.string.streaming_home_guard_break_active,
            formatCompactDuration(guardTimeoutRemainingMs)
        )
    } else {
        guardSnapshotLabel
    }

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = noShadowCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (isGuardTimeoutActive) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = guardModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isGuardTimeoutActive) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = stringResource(
                    id = R.string.streaming_home_guard_exposure,
                    todayListeningMinutes,
                    rhythmGuardRecommendedMinutes
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = guardStateSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StreamingRhythmStatsCard(
    listeningDurationMs: Long,
    songsPlayed: Int,
    uniqueArtists: Int,
    onCardClick: () -> Unit
) {
    val listeningTimeLabel = formatListeningDurationShort(listeningDurationMs)

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = noShadowCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreamingStatPill(
                    title = stringResource(id = R.string.home_stat_listening_time),
                    value = listeningTimeLabel,
                    modifier = Modifier.weight(1f)
                )
                StreamingStatPill(
                    title = stringResource(id = R.string.home_stat_songs_played),
                    value = songsPlayed.toString(),
                    modifier = Modifier.weight(1f)
                )
                StreamingStatPill(
                    title = stringResource(id = R.string.home_stat_artists),
                    value = uniqueArtists.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StreamingStatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamingRecommendationsCarousel(
    songs: List<StreamingSong>,
    autoScrollEnabled: Boolean,
    autoScrollIntervalSeconds: Int,
    onPlaySong: (StreamingSong, Int) -> Unit
) {
    val carouselState = rememberCarouselState(
        initialItem = 0,
        itemCount = { songs.size }
    )

    LaunchedEffect(songs.size, autoScrollEnabled, autoScrollIntervalSeconds) {
        if (autoScrollEnabled && songs.size > 1) {
            while (true) {
                delay(autoScrollIntervalSeconds.coerceIn(2, 20) * 1000L)
                val currentItem = carouselState.currentItem
                val nextItem = (currentItem + 1) % songs.size
                carouselState.animateScrollToItem(nextItem)
            }
        }
    }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        modifier = Modifier
            .fillMaxWidth()
            .height(268.dp)
            .padding(vertical = 6.dp),
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 0.dp),
        flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
    ) { itemIndex ->
        val song = songs[itemIndex]
        StreamingRecommendationHeroCard(
            song = song,
            onPlaySong = { onPlaySong(song, itemIndex) },
            isPeeked = itemIndex != carouselState.currentItem,
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun androidx.compose.material3.carousel.CarouselItemScope.StreamingRecommendationHeroCard(
    song: StreamingSong,
    onPlaySong: () -> Unit,
    isPeeked: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onPlaySong()
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = noShadowCardElevation()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            M3ImageUtils.TrackImage(
                imageUrl = song.artworkUri,
                trackName = song.title,
                modifier = Modifier.fillMaxSize(),
                applyExpressiveShape = false
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            if (!isPeeked) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingLoadingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = noShadowCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.material3.ContainedLoadingIndicator()

            Text(
                text = stringResource(id = R.string.streaming_status_loading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.streaming_library_syncing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StreamingSectionEmptyCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = noShadowCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StreamingSongWidgetCard(
    song: StreamingSong,
    onPlaySong: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    ExpressiveCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onPlaySong()
        },
        modifier = Modifier
            .width(180.dp)
            .height(80.dp)
            .shadow(
                elevation = 4.dp,
                shape = ExpressiveShapes.Large,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ExpressiveShapes.Large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.TrackImage(
                imageUrl = song.artworkUri,
                trackName = song.title,
                modifier = Modifier.size(52.dp),
                applyExpressiveShape = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(id = R.string.cd_play),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StreamingAlbumWidgetCard(
    album: StreamingAlbum,
    onPlayAlbum: () -> Unit,
    onAlbumClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    ExpressiveElevatedCard(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onAlbumClick()
        },
        modifier = Modifier
            .width(160.dp)
            .height(252.dp)
            .shadow(
                elevation = 12.dp,
                shape = ExpressiveShapes.SquircleLarge,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ),
        shape = ExpressiveShapes.SquircleLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Box {
                M3ImageUtils.AlbumArt(
                    imageUrl = album.artworkUri,
                    albumName = album.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    ExpressiveFilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            onPlayAlbum()
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(id = R.string.cd_play_album),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                album.year?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingArtistWidgetCard(
    artist: StreamingArtist,
    onOpenArtist: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val cardSize = 120.dp

    Column(
        modifier = Modifier
            .width(cardSize)
            .clickable {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                onOpenArtist()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(cardSize)) {
            M3ImageUtils.ArtistImage(
                imageUrl = artist.artworkUri,
                artistName = artist.name,
                modifier = Modifier.fillMaxSize()
            )

            ExpressiveFilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onOpenArtist()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(id = R.string.cd_play_artist, artist.name),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StreamingArtistsSection(
    artists: List<StreamingArtist>,
    onArtistClick: (StreamingArtist) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        val gridColumns = if (configuration.screenWidthDp >= 840) 6 else 4
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.height(320.dp)
        ) {
            items(
                items = artists,
                key = { it.id },
                contentType = { "artist" }
            ) { artist ->
                StreamingArtistWidgetCard(
                    artist = artist,
                    onOpenArtist = { onArtistClick(artist) }
                )
            }
        }
    } else {
        val artistsListState = rememberLazyListState()
        LazyRow(
            state = artistsListState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = artists,
                key = { it.id },
                contentType = { "artist" }
            ) { artist ->
                StreamingArtistWidgetCard(
                    artist = artist,
                    onOpenArtist = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun StreamingPlaylistWidgetCard(
    playlist: StreamingPlaylist,
    onPlayPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onPlayPlaylist()
        },
        modifier = Modifier.width(232.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        ),
        elevation = noShadowCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.PlaylistImage(
                imageUrl = playlist.artworkUri,
                playlistName = playlist.name,
                modifier = Modifier.size(68.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = playlist.description.orEmpty().ifBlank {
                        stringResource(
                            id = R.string.streaming_home_widget_playlist_track_count,
                            playlist.songCount
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun streamingGroupedBottomSheetItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    if (totalCount <= 1) return RoundedCornerShape(24.dp)

    return when (index) {
        0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        totalCount - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(8.dp)
    }
}

private fun streamingSectionIcon(sectionId: String): ImageVector {
    return when (sectionId) {
        STREAMING_SECTION_GREETING -> Icons.Default.WavingHand
        STREAMING_SECTION_DISCOVER -> Icons.Default.Recommend
        STREAMING_SECTION_RHYTHM_GUARD -> Icons.Default.Security
        STREAMING_SECTION_RHYTHM_STATS -> Icons.Default.AutoGraph
        STREAMING_SECTION_RECENTLY_PLAYED -> Icons.Default.History
        STREAMING_SECTION_NEW_RELEASES -> Icons.Rounded.NewReleases
        else -> RhythmIcons.Music.Song
    }
}

private fun streamingSectionSubtitleRes(sectionId: String): Int? {
    return when (sectionId) {
        STREAMING_SECTION_DISCOVER -> R.string.home_explore_music
        STREAMING_SECTION_RHYTHM_GUARD -> R.string.settings_rhythm_guard_list_desc
        STREAMING_SECTION_RHYTHM_STATS -> R.string.settings_rhythm_stats_desc
        STREAMING_SECTION_RECENTLY_PLAYED -> R.string.home_recently_played_subtitle
        STREAMING_SECTION_NEW_RELEASES -> R.string.streaming_home_widget_new_releases_empty
        else -> null
    }
}

private fun streamingSectionTitleRes(sectionId: String): Int {
    return when (sectionId) {
        STREAMING_SECTION_GREETING -> R.string.home_section_greeting
        STREAMING_SECTION_DISCOVER -> R.string.home_section_discover
        STREAMING_SECTION_RHYTHM_GUARD -> R.string.settings_rhythm_guard
        STREAMING_SECTION_RHYTHM_STATS -> R.string.settings_rhythm_stats
        STREAMING_SECTION_RECENTLY_PLAYED -> R.string.home_section_recently_played
        STREAMING_SECTION_NEW_RELEASES -> R.string.home_section_new_releases
        else -> R.string.home
    }
}

private fun normalizeStreamingSectionId(sectionId: String): String {
    return when (sectionId.trim()) {
        "STATS" -> STREAMING_SECTION_RHYTHM_STATS
        "RECOMMENDED" -> STREAMING_SECTION_DISCOVER
        "PLAYLISTS" -> STREAMING_SECTION_DISCOVER
        else -> sectionId.trim()
    }
}

private fun rhythmGuardModeDisplayName(mode: String): String {
    return when (mode) {
        AppSettings.RHYTHM_GUARD_MODE_AUTO -> "Adaptive Guard"
        AppSettings.RHYTHM_GUARD_MODE_MANUAL -> "Manual Guard"
        else -> "Guard Off"
    }
}

@Composable
private fun noShadowCardElevation() = CardDefaults.cardElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp
)

@Composable
private fun rememberGreetingMessage(context: android.content.Context): String {
    val hourOfDay = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingRes = when (hourOfDay) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..16 -> R.string.home_greeting_afternoon
        in 17..21 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    }
    return context.getString(greetingRes)
}

@Composable
private fun rememberGreetingQuote(context: android.content.Context): String {
    val hourOfDay = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    return remember(hourOfDay) {
        when (hourOfDay) {
            in 0..4 -> listOf(
                context.getString(R.string.home_quote_late_night_1),
                context.getString(R.string.home_quote_late_night_2),
                context.getString(R.string.home_quote_late_night_3),
                context.getString(R.string.home_quote_late_night_4)
            )

            in 5..11 -> listOf(
                context.getString(R.string.home_quote_morning_1),
                context.getString(R.string.home_quote_morning_2),
                context.getString(R.string.home_quote_morning_3),
                context.getString(R.string.home_quote_morning_4)
            )

            in 12..16 -> listOf(
                context.getString(R.string.home_quote_afternoon_1),
                context.getString(R.string.home_quote_afternoon_2),
                context.getString(R.string.home_quote_afternoon_3),
                context.getString(R.string.home_quote_afternoon_4)
            )

            in 17..20 -> listOf(
                context.getString(R.string.home_quote_evening_1),
                context.getString(R.string.home_quote_evening_2),
                context.getString(R.string.home_quote_evening_3),
                context.getString(R.string.home_quote_evening_4)
            )

            else -> listOf(
                context.getString(R.string.home_quote_night_1),
                context.getString(R.string.home_quote_night_2),
                context.getString(R.string.home_quote_night_3),
                context.getString(R.string.home_quote_night_4)
            )
        }.random()
    }
}

private fun formatListeningDurationShort(durationMs: Long): String {
    if (durationMs <= 0L) return "0m"

    val totalMinutes = durationMs / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours <= 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

private fun formatCompactDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun Song.toStreamingSongOrNull(defaultServiceId: String): StreamingSong? {
    val uriValue = uri.toString()
    val hasPrefixedServiceId = id.contains("::")
    val isNetworkStream =
        uriValue.startsWith("https://", ignoreCase = true) ||
            uriValue.startsWith("http://", ignoreCase = true)

    if (!hasPrefixedServiceId && !isNetworkStream) {
        return null
    }

    val embeddedServiceId = id.substringBefore("::", missingDelimiterValue = "")
    val resolvedServiceId = when {
        embeddedServiceId in StreamingServiceId.all -> embeddedServiceId
        defaultServiceId in StreamingServiceId.all -> defaultServiceId
        else -> StreamingServiceId.SUBSONIC
    }
    val normalizedId = if (hasPrefixedServiceId) id else "$resolvedServiceId::$id"

    return StreamingSong(
        id = normalizedId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        artworkUri = artworkUri?.toString(),
        sourceType = resolvedServiceId.toSourceType(),
        streamingUrl = uriValue.takeIf { isNetworkStream },
        previewUrl = null,
        albumId = albumId?.takeIf { it.isNotBlank() },
        albumArtist = albumArtist?.takeIf { it.isNotBlank() },
        externalId = normalizedId.substringAfter("::", missingDelimiterValue = id)
    )
}

private fun String.toSourceType(): SourceType {
    return when (this) {
        StreamingServiceId.SUBSONIC -> SourceType.SUBSONIC
        StreamingServiceId.JELLYFIN -> SourceType.JELLYFIN
        else -> SourceType.UNKNOWN
    }
}
