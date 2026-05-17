@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package chromahub.rhythm.app.features.streaming.presentation.screens

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.local.presentation.screens.SingleCardAlbumsContent
import chromahub.rhythm.app.features.local.presentation.screens.SingleCardArtistsContent
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistFabMenu
import chromahub.rhythm.app.features.local.presentation.screens.SingleCardPlaylistsContent
import chromahub.rhythm.app.features.local.presentation.screens.SingleCardSongsContent
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.features.streaming.domain.model.StreamingAlbum
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingSong
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Playlist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.common.TabAnimation
import chromahub.rhythm.app.util.ArtistSeparator
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.M3ImageUtils
import kotlinx.coroutines.launch
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.rounded.Info
import chromahub.rhythm.app.ui.LocalMiniPlayerPadding
import kotlin.random.Random

private enum class StreamingLibraryTab(@param:StringRes val titleRes: Int, val icon: ImageVector) {
    SONGS(R.string.library_tab_songs, Icons.Rounded.History),
    ALBUMS(R.string.library_tab_albums, Icons.Rounded.Album),
    ARTISTS(R.string.library_tab_artists, Icons.Rounded.Person),
    PLAYLISTS(R.string.library_tab_playlists, Icons.AutoMirrored.Rounded.QueueMusic)
}

private enum class StreamingSongSortOrder(
    @param:StringRes val labelRes: Int,
    val ascending: Boolean,
    val icon: ImageVector
) {
    TITLE_ASC(R.string.sort_title, true, Icons.AutoMirrored.Rounded.Sort),
    TITLE_DESC(R.string.sort_title, false, Icons.AutoMirrored.Rounded.Sort),
    ARTIST_ASC(R.string.sort_artist, true, Icons.Rounded.Person),
    ARTIST_DESC(R.string.sort_artist, false, Icons.Rounded.Person),
    ALBUM_ASC(R.string.metadata_album, true, Icons.Rounded.Album),
    ALBUM_DESC(R.string.metadata_album, false, Icons.Rounded.Album),
    DURATION_ASC(R.string.sort_duration_short_first, true, Icons.Rounded.History),
    DURATION_DESC(R.string.sort_duration_long_first, false, Icons.Rounded.History)
}

private enum class StreamingAlbumSortOrder(
    @param:StringRes val labelRes: Int,
    val ascending: Boolean,
    val icon: ImageVector
) {
    TITLE_ASC(R.string.sort_title, true, Icons.AutoMirrored.Rounded.Sort),
    TITLE_DESC(R.string.sort_title, false, Icons.AutoMirrored.Rounded.Sort),
    ARTIST_ASC(R.string.sort_artist, true, Icons.Rounded.Person),
    ARTIST_DESC(R.string.sort_artist, false, Icons.Rounded.Person),
    YEAR_ASC(R.string.metadata_year, true, Icons.Rounded.History),
    YEAR_DESC(R.string.metadata_year, false, Icons.Rounded.History),
    TRACK_COUNT_ASC(R.string.sort_song_count, true, Icons.AutoMirrored.Rounded.QueueMusic),
    TRACK_COUNT_DESC(R.string.sort_song_count, false, Icons.AutoMirrored.Rounded.QueueMusic)
}

private enum class StreamingArtistSortOrder(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val ascending: Boolean
) {
    NAME_ASC(R.string.sort_name, Icons.Rounded.Person, true),
    NAME_DESC(R.string.sort_name, Icons.Rounded.Person, false),
    SONG_COUNT_ASC(R.string.sort_song_count, Icons.AutoMirrored.Rounded.QueueMusic, true),
    SONG_COUNT_DESC(R.string.sort_song_count, Icons.AutoMirrored.Rounded.QueueMusic, false),
    ALBUM_COUNT_ASC(R.string.bottomsheet_albums, Icons.Rounded.Album, true),
    ALBUM_COUNT_DESC(R.string.bottomsheet_albums, Icons.Rounded.Album, false),
    POPULARITY_DESC(R.string.bottomsheet_sort_by, Icons.Rounded.TrendingUp, false),
    POPULARITY_ASC(R.string.bottomsheet_sort_by, Icons.Rounded.TrendingUp, true)
}

private enum class StreamingPlaylistSortOrder(
    @param:StringRes val labelRes: Int,
    val ascending: Boolean,
    val icon: ImageVector
) {
    NAME_ASC(R.string.sort_name, true, Icons.AutoMirrored.Rounded.QueueMusic),
    NAME_DESC(R.string.sort_name, false, Icons.AutoMirrored.Rounded.QueueMusic),
    TRACK_COUNT_ASC(R.string.sort_song_count, true, Icons.AutoMirrored.Rounded.QueueMusic),
    TRACK_COUNT_DESC(R.string.sort_song_count, false, Icons.AutoMirrored.Rounded.QueueMusic)
}

@Composable
fun StreamingLibraryScreen(
    viewModel: StreamingMusicViewModel,
    onConfigureService: (String) -> Unit,
    onNavigateToArtist: (StreamingArtist) -> Unit,
    onNavigateToPlaylist: (StreamingPlaylist) -> Unit,
    onAddSongToPlaylist: (StreamingSong) -> Unit = {},
    activeSongId: String? = null,
    isPlayerPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val scope = rememberCoroutineScope()

    val selectedService by appSettings.streamingService.collectAsState()
    val sessions by viewModel.serviceSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedLibrary by viewModel.hasLoadedLibrary.collectAsState()
    val hasLoadedHomeContent by viewModel.hasLoadedHomeContent.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentStreamingSong by viewModel.currentSong.collectAsState()
    val isPlayerPlaying by viewModel.isPlaying.collectAsState()

    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()
    val followedArtists by viewModel.followedArtists.collectAsState()
    val savedPlaylists by viewModel.savedPlaylists.collectAsState()
    val featuredPlaylists by viewModel.featuredPlaylists.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val groupByAlbumArtist by appSettings.groupByAlbumArtist.collectAsState()

    val resolvedServiceId = remember(selectedService, sessions) {
        when {
            sessions[selectedService]?.isConnected == true -> selectedService
            else -> sessions.entries.firstOrNull { it.value.isConnected }?.key ?: selectedService
        }
    }

    val selectedOption = remember(resolvedServiceId) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == resolvedServiceId }
    }
    val selectedServiceName = selectedOption?.let { context.getString(it.nameRes) }
        ?: context.getString(R.string.streaming_not_selected)
    val isSelectedServiceConnected = sessions[resolvedServiceId]?.isConnected == true
    val configureTargetServiceId = remember(resolvedServiceId) {
        if (resolvedServiceId.isNotBlank()) {
            resolvedServiceId
        } else {
            StreamingServiceOptions.defaults.firstOrNull()?.id.orEmpty()
        }
    }

    val allSongs by viewModel.allSongs.collectAsState()
    val librarySongs = remember(allSongs, likedSongs, downloadedSongs, recommendations) {
        if (allSongs.isNotEmpty()) {
            allSongs
        } else {
            (likedSongs + downloadedSongs + recommendations).distinctBy { it.id }
        }
    }
    val libraryAlbums = remember(savedAlbums, newReleases) {
        if (savedAlbums.isNotEmpty()) {
            savedAlbums
        } else {
            newReleases
        }
    }
    val libraryArtists = remember(followedArtists) {
        // Display only provider artists - no derivation or merging
        followedArtists
    }
    val libraryPlaylists = remember(savedPlaylists, featuredPlaylists) {
        (savedPlaylists + featuredPlaylists).distinctBy { it.id }
    }
    // Use provider albums directly - do NOT derive from songs to preserve year and prevent splitting
    val resolvedLibraryAlbums = remember(libraryAlbums) {
        libraryAlbums
    }

    // Get miniplayer padding for bottom content alignment
    val miniPlayerBottomPadding = LocalMiniPlayerPadding.current.calculateBottomPadding()
    val contentBottomPadding = miniPlayerBottomPadding + 24.dp // Add extra space for bottom nav

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = StreamingLibraryTab.entries
    val selectedTab = tabs[selectedTabIndex.coerceIn(0, tabs.lastIndex)]
    val tabRowState = rememberLazyListState()
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { tabs.size }
    )

    var songSortOrder by rememberSaveable { mutableStateOf(StreamingSongSortOrder.TITLE_ASC) }
    var albumSortOrder by rememberSaveable { mutableStateOf(StreamingAlbumSortOrder.TITLE_ASC) }
    var artistSortOrder by rememberSaveable { mutableStateOf(StreamingArtistSortOrder.NAME_ASC) }
    var playlistSortOrder by rememberSaveable { mutableStateOf(StreamingPlaylistSortOrder.NAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showPlaylistFabMenu by remember { mutableStateOf(false) }
    
    // Album bottom sheet state - shared across recompositions
    var showAlbumBottomSheet by remember { mutableStateOf(false) }
    var selectedAlbumForSheet by remember { mutableStateOf<StreamingAlbum?>(null) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<Song?>(null) }
    val albumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val sortedSongs = remember(librarySongs, songSortOrder) {
        when (songSortOrder) {
            StreamingSongSortOrder.TITLE_ASC -> librarySongs.sortedBy { it.title.lowercase() }
            StreamingSongSortOrder.TITLE_DESC -> librarySongs.sortedByDescending { it.title.lowercase() }
            StreamingSongSortOrder.ARTIST_ASC -> librarySongs.sortedBy { it.artist.lowercase() }
            StreamingSongSortOrder.ARTIST_DESC -> librarySongs.sortedByDescending { it.artist.lowercase() }
            StreamingSongSortOrder.ALBUM_ASC -> librarySongs.sortedBy { it.album.lowercase() }
            StreamingSongSortOrder.ALBUM_DESC -> librarySongs.sortedByDescending { it.album.lowercase() }
            StreamingSongSortOrder.DURATION_ASC -> librarySongs.sortedBy { it.duration }
            StreamingSongSortOrder.DURATION_DESC -> librarySongs.sortedByDescending { it.duration }
        }
    }
    val sortedAlbums = remember(resolvedLibraryAlbums, albumSortOrder) {
        when (albumSortOrder) {
            StreamingAlbumSortOrder.TITLE_ASC -> resolvedLibraryAlbums.sortedBy { it.title.lowercase() }
            StreamingAlbumSortOrder.TITLE_DESC -> resolvedLibraryAlbums.sortedByDescending { it.title.lowercase() }
            StreamingAlbumSortOrder.ARTIST_ASC -> resolvedLibraryAlbums.sortedBy { it.artist.lowercase() }
            StreamingAlbumSortOrder.ARTIST_DESC -> resolvedLibraryAlbums.sortedByDescending { it.artist.lowercase() }
            StreamingAlbumSortOrder.YEAR_ASC -> resolvedLibraryAlbums.sortedBy { it.year ?: 0 }
            StreamingAlbumSortOrder.YEAR_DESC -> resolvedLibraryAlbums.sortedByDescending { it.year ?: 0 }
            StreamingAlbumSortOrder.TRACK_COUNT_ASC -> resolvedLibraryAlbums.sortedBy { it.songCount }
            StreamingAlbumSortOrder.TRACK_COUNT_DESC -> resolvedLibraryAlbums.sortedByDescending { it.songCount }
        }
    }
    val sortedArtists = remember(libraryArtists, artistSortOrder) {
        when (artistSortOrder) {
            StreamingArtistSortOrder.NAME_ASC -> libraryArtists.sortedBy { it.name.lowercase() }
            StreamingArtistSortOrder.NAME_DESC -> libraryArtists.sortedByDescending { it.name.lowercase() }
            StreamingArtistSortOrder.SONG_COUNT_ASC -> libraryArtists.sortedBy { it.songCount }
            StreamingArtistSortOrder.SONG_COUNT_DESC -> libraryArtists.sortedByDescending { it.songCount }
            StreamingArtistSortOrder.ALBUM_COUNT_ASC -> libraryArtists.sortedBy { it.albumCount }
            StreamingArtistSortOrder.ALBUM_COUNT_DESC -> libraryArtists.sortedByDescending { it.albumCount }
            StreamingArtistSortOrder.POPULARITY_DESC -> libraryArtists.sortedByDescending { it.popularity ?: Int.MIN_VALUE }
            StreamingArtistSortOrder.POPULARITY_ASC -> libraryArtists.sortedBy { it.popularity ?: Int.MIN_VALUE }
        }
    }
    val sortedPlaylists = remember(libraryPlaylists, playlistSortOrder) {
        when (playlistSortOrder) {
            StreamingPlaylistSortOrder.NAME_ASC -> libraryPlaylists.sortedBy { it.name.lowercase() }
            StreamingPlaylistSortOrder.NAME_DESC -> libraryPlaylists.sortedByDescending { it.name.lowercase() }
            StreamingPlaylistSortOrder.TRACK_COUNT_ASC -> libraryPlaylists.sortedBy { it.songCount }
            StreamingPlaylistSortOrder.TRACK_COUNT_DESC -> libraryPlaylists.sortedByDescending { it.songCount }
        }
    }

    val sortedSongsById = remember(sortedSongs) { sortedSongs.associateBy { it.id } }
    val localSongs = remember(sortedSongs) {
        sortedSongs.map { it.toLibrarySong() }
    }
    val localSongsById = remember(localSongs) { localSongs.associateBy { it.id } }
    val localAlbums = remember(sortedAlbums) {
        sortedAlbums.map { it.toLibraryAlbum(localSongs) }
    }
    val localArtists = remember(sortedArtists, localSongs) {
        sortedArtists.map { it.toLibraryArtist(localSongs, emptyList()) }
    }
    val localPlaylists = remember(sortedPlaylists) {
        sortedPlaylists.map { it.toLibraryPlaylist() }
    }
    val localPlaylistsById = remember(localPlaylists, sortedPlaylists) {
        sortedPlaylists.associateBy { it.id }
    }
    val currentLocalSong = remember(activeSongId, localSongsById, currentStreamingSong) {
        activeSongId?.let(localSongsById::get) ?: currentStreamingSong?.toLibrarySong()
    }
    val streamingFavoriteSongIds = remember(sortedSongs, likedSongs) {
        (sortedSongs.filter { it.isFavorite }.map { it.id } + likedSongs.map { it.id }).toSet()
    }
    val openAlbumBottomSheet: (StreamingAlbum) -> Unit = { album ->
        if (album.tracks.isEmpty()) {
            selectedAlbumForSheet = album
            scope.launch {
                val tracks = viewModel.getAlbumSongs(album)
                if (tracks.isNotEmpty()) {
                    selectedAlbumForSheet = album.copy(tracks = tracks)
                }
            }
        } else {
            selectedAlbumForSheet = album
        }
        showAlbumBottomSheet = true
    }
    val openAlbumForSong: (Song) -> Unit = { localSong ->
        val resolvedStreamingSong = sortedSongsById[localSong.id]
        val albumArtist = localSong.albumArtist?.takeIf { it.isNotBlank() } ?: localSong.artist
        val resolvedAlbum = sortedAlbums.firstOrNull { album ->
            val albumMatchesById = resolvedStreamingSong?.albumId?.let { album.id == it } == true
            val albumMatchesByMetadata = album.title.equals(localSong.album, ignoreCase = true) &&
                album.artist.equals(albumArtist, ignoreCase = true)
            albumMatchesById || albumMatchesByMetadata
        }

        resolvedAlbum?.let(openAlbumBottomSheet)
    }
    val hasLibraryContent = remember(
        localSongs,
        localArtists,
        localPlaylists,
        recommendations,
        featuredPlaylists
    ) {
        localSongs.isNotEmpty() ||
            localAlbums.isNotEmpty() ||
            localArtists.isNotEmpty() ||
            localPlaylists.isNotEmpty() ||
            recommendations.isNotEmpty() ||
            featuredPlaylists.isNotEmpty()
    }
    val libraryErrorMessage = error?.takeIf { it.isNotBlank() }

    val mapLocalSongsToStreaming: (List<Song>) -> List<StreamingSong> = remember(sortedSongsById) {
        { localQueue ->
            localQueue
                .mapNotNull { sortedSongsById[it.id] }
                .distinctBy { it.id }
        }
    }

    LaunchedEffect(selectedService, resolvedServiceId) {
        if (resolvedServiceId.isNotBlank() && resolvedServiceId != selectedService) {
            appSettings.setStreamingService(resolvedServiceId)
        }
    }

    LaunchedEffect(resolvedServiceId, isSelectedServiceConnected) {
        if (isSelectedServiceConnected) {
            if (!hasLoadedLibrary) {
                viewModel.loadLibrary()
            }
            if (!hasLoadedHomeContent) {
                viewModel.loadHomeContent()
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedTabIndex != pagerState.currentPage) {
            selectedTabIndex = pagerState.currentPage
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
        tabRowState.animateScrollToItem(selectedTabIndex.coerceAtLeast(0))
    }

    val currentSortLabelRes = when (selectedTab) {
        StreamingLibraryTab.SONGS -> songSortOrder.labelRes
        StreamingLibraryTab.ALBUMS -> albumSortOrder.labelRes
        StreamingLibraryTab.ARTISTS -> artistSortOrder.labelRes
        StreamingLibraryTab.PLAYLISTS -> playlistSortOrder.labelRes
    }
    val isCurrentSortAscending = when (selectedTab) {
        StreamingLibraryTab.SONGS -> songSortOrder.ascending
        StreamingLibraryTab.ALBUMS -> albumSortOrder.ascending
        StreamingLibraryTab.ARTISTS -> artistSortOrder.ascending
        StreamingLibraryTab.PLAYLISTS -> playlistSortOrder.ascending
    }
    val currentSortIcon = when (selectedTab) {
        StreamingLibraryTab.SONGS -> songSortOrder.icon
        StreamingLibraryTab.ALBUMS -> albumSortOrder.icon
        StreamingLibraryTab.ARTISTS -> artistSortOrder.icon
        StreamingLibraryTab.PLAYLISTS -> playlistSortOrder.icon
    }
    val random = remember { Random(System.currentTimeMillis()) }
    val libraryTitle = remember(selectedServiceName, isSelectedServiceConnected) {
        if (isSelectedServiceConnected && selectedServiceName.isNotBlank()) {
            "$selectedServiceName ${context.getString(R.string.library_title)}"
        } else {
            context.getString(R.string.library_title)
        }
    }
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isLoading

    CollapsibleHeaderScreen(
        title = libraryTitle,
        headerDisplayMode = 1,
        actions = {
            if (isSelectedServiceConnected) {
                Box {
                    FilledTonalButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                            showSortMenu = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = currentSortIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = currentSortLabelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isCurrentSortAscending) {
                                Icons.Rounded.ArrowUpward
                            } else {
                                Icons.Rounded.ArrowDownward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        when (selectedTab) {
                            StreamingLibraryTab.SONGS -> {
                                StreamingSongSortOrder.entries.forEach { order ->
                                    StreamingSortMenuItem(
                                        label = stringResource(id = order.labelRes),
                                        icon = order.icon,
                                        ascending = order.ascending,
                                        selected = songSortOrder == order,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            songSortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }

                            StreamingLibraryTab.ALBUMS -> {
                                StreamingAlbumSortOrder.entries.forEach { order ->
                                    StreamingSortMenuItem(
                                        label = stringResource(id = order.labelRes),
                                        icon = order.icon,
                                        ascending = order.ascending,
                                        selected = albumSortOrder == order,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            albumSortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }

                            StreamingLibraryTab.ARTISTS -> {
                                StreamingArtistSortOrder.entries.forEach { order ->
                                    StreamingSortMenuItem(
                                        label = stringResource(id = order.labelRes),
                                        icon = order.icon,
                                        ascending = order.ascending,
                                        selected = artistSortOrder == order,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            artistSortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }

                            StreamingLibraryTab.PLAYLISTS -> {
                                StreamingPlaylistSortOrder.entries.forEach { order ->
                                    StreamingSortMenuItem(
                                        label = stringResource(id = order.labelRes),
                                        icon = order.icon,
                                        ascending = order.ascending,
                                        selected = playlistSortOrder == order,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            playlistSortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { contentModifier ->
        Column(
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                LazyRow(
                    state = tabRowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(tabs) { index, tab ->
                        val isSelected = selectedTabIndex == index
                        TabAnimation(
                            index = index,
                            selectedIndex = selectedTabIndex,
                            title = stringResource(id = tab.titleRes),
                            selectedColor = MaterialTheme.colorScheme.primary,
                            onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedColor = MaterialTheme.colorScheme.surfaceContainer,
                            onUnselectedColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                selectedTabIndex = index
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier.padding(all = 2.dp),
                            content = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(id = tab.titleRes),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadLibrary() },
                state = pullToRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 0.dp
                ) {
                when {
                    !isSelectedServiceConnected -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            StreamingLibraryDisconnectedCard(
                                selectedServiceName = selectedServiceName,
                                onConfigureService = { onConfigureService(configureTargetServiceId) },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }

                    (isLoading || !hasLoadedLibrary) && !hasLibraryContent -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            StreamingLibraryLoadingCard(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    !libraryErrorMessage.isNullOrBlank() && !hasLibraryContent -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            StreamingLibraryStateCard(
                                title = stringResource(id = R.string.streaming_home_selected_service_unavailable),
                                subtitle = libraryErrorMessage,
                                icon = Icons.Rounded.Info,
                                iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                                actionText = stringResource(id = R.string.streaming_manage_service),
                                onAction = { onConfigureService(configureTargetServiceId) },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    !hasLibraryContent -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            StreamingLibraryStateCard(
                                title = stringResource(id = R.string.streaming_library_empty),
                                subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint),
                                icon = Icons.Rounded.Album,
                                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                actionText = stringResource(id = R.string.streaming_manage_service),
                                onAction = { onConfigureService(configureTargetServiceId) },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    else -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 10.dp, bottom = contentBottomPadding)
                        ) { page ->
                        when (tabs[page]) {
                            StreamingLibraryTab.ALBUMS -> {
                                if (localAlbums.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No albums",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    SingleCardAlbumsContent(
                                        albums = localAlbums,
                                        onAlbumClick = { album ->
                                            val streamingAlbum = sortedAlbums.firstOrNull { it.id == album.id }
                                            streamingAlbum?.let {
                                                if (it.tracks.isNotEmpty()) {
                                                    viewModel.playQueue(it.tracks, startIndex = 0, shuffle = false)
                                                } else {
                                                    scope.launch {
                                                        val tracks = viewModel.getAlbumSongs(it)
                                                        if (tracks.isNotEmpty()) {
                                                            viewModel.playQueue(tracks, startIndex = 0, shuffle = false)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onAlbumBottomSheetClick = { album ->
                                            val streamingAlbum = sortedAlbums.firstOrNull { it.id == album.id }
                                            streamingAlbum?.let {
                                                if (it.tracks.isEmpty()) {
                                                    selectedAlbumForSheet = it
                                                    scope.launch {
                                                        val tracks = viewModel.getAlbumSongs(it)
                                                        if (tracks.isNotEmpty()) {
                                                            selectedAlbumForSheet = it.copy(tracks = tracks)
                                                        }
                                                    }
                                                } else {
                                                    selectedAlbumForSheet = it
                                                }
                                                showAlbumBottomSheet = true
                                            }
                                        },
                                        onSongClick = { song ->
                                            val index = localSongs.indexOfFirst { it.id == song.id }
                                            if (index >= 0) {
                                                viewModel.playQueue(
                                                    queue = sortedSongs,
                                                    startIndex = index,
                                                    shuffle = false
                                                )
                                            }
                                        },
                                        haptics = haptics,
                                        appSettings = appSettings,
                                        onPlayQueue = { localQueue ->
                                            val queue = mapLocalSongsToStreaming(localQueue)
                                            if (queue.isNotEmpty()) {
                                                viewModel.playQueue(queue = queue, startIndex = 0, shuffle = false)
                                            }
                                        },
                                        onShuffleQueue = { localQueue ->
                                            val queue = mapLocalSongsToStreaming(localQueue)
                                            if (queue.isNotEmpty()) {
                                                viewModel.playQueue(
                                                    queue = queue,
                                                    startIndex = if (queue.size > 1) random.nextInt(queue.size) else 0,
                                                    shuffle = true
                                                )
                                            }
                                        },
                                        onRefreshClick = { viewModel.loadLibrary() }
                                    )
                                }
                            }

                            StreamingLibraryTab.SONGS -> {
                                SingleCardSongsContent(
                                    songs = localSongs,
                                    albums = localAlbums,
                                    artists = localArtists,
                                    onSongClick = { localSong ->
                                        val index = sortedSongs.indexOfFirst { it.id == localSong.id }
                                        if (index >= 0) {
                                            viewModel.playQueue(
                                                queue = sortedSongs,
                                                startIndex = index,
                                                shuffle = false
                                            )
                                        }
                                    },
                                    onAddToPlaylist = {},
                                    onAddToQueue = {},
                                    onPlayNext = {},
                                    onShowSongInfo = { song ->
                                        selectedSongForInfo = song
                                        showSongInfoSheet = true
                                    },
                                    onAddToBlacklist = {},
                                    favoriteSongs = streamingFavoriteSongIds,
                                    onPlayQueue = { localQueue ->
                                        val queue = mapLocalSongsToStreaming(localQueue)
                                        if (queue.isNotEmpty()) {
                                            viewModel.playQueue(
                                                queue = queue,
                                                startIndex = 0,
                                                shuffle = false
                                            )
                                        }
                                    },
                                    onPlayQueueFromIndex = { localQueue, index ->
                                        val queue = mapLocalSongsToStreaming(localQueue)
                                        if (queue.isNotEmpty()) {
                                            viewModel.playQueue(
                                                queue = queue,
                                                startIndex = index.coerceIn(0, queue.lastIndex),
                                                shuffle = false
                                            )
                                        }
                                    },
                                    onShuffleQueue = { localQueue ->
                                        val queue = mapLocalSongsToStreaming(localQueue)
                                        if (queue.isNotEmpty()) {
                                            viewModel.playQueue(
                                                queue = queue,
                                                startIndex = if (queue.size > 1) random.nextInt(queue.size) else 0,
                                                shuffle = true
                                            )
                                        }
                                    },
                                    onGoToArtist = { localArtist ->
                                        val resolvedArtist = sortedArtists.firstOrNull { it.id == localArtist.id }
                                            ?: sortedArtists.firstOrNull {
                                                it.name.equals(localArtist.name, ignoreCase = true)
                                            }
                                        resolvedArtist?.let(onNavigateToArtist)
                                    },
                                    onGoToAlbum = { album ->
                                        val streamingAlbum = sortedAlbums.firstOrNull { it.id == album.id }
                                        streamingAlbum?.let(openAlbumBottomSheet)
                                    },
                                    currentSong = currentLocalSong,
                                    isPlaying = isPlayerPlaying,
                                    haptics = haptics,
                                    enableRatingSystem = false,
                                    songMenuContent = { localSong, dismissMenu ->
                                        val songIndex = sortedSongs.indexOfFirst { it.id == localSong.id }
                                        val resolvedArtist = sortedArtists.firstOrNull {
                                            it.name.equals(localSong.artist, ignoreCase = true)
                                        }

                                        // Play
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
                                                        stringResource(id = R.string.action_play),
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
                                                            imageVector = Icons.Rounded.PlayArrow,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(6.dp)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    if (songIndex >= 0) {
                                                        viewModel.playQueue(
                                                            queue = sortedSongs,
                                                            startIndex = songIndex,
                                                            shuffle = false
                                                        )
                                                    }
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
                                                    dismissMenu()
                                                    sortedSongsById[localSong.id]?.let { streamingSong ->
                                                        viewModel.playQueue(queue = listOf(streamingSong), startIndex = 0, shuffle = false)
                                                    }
                                                }
                                            )
                                        }

                                        // Like / Unlike
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            val streamingSong = sortedSongsById[localSong.id]
                                            val isLiked = streamingSong != null && likedSongs.any { it.id == streamingSong.id }
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        if (isLiked) "Unlike" else "Like",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                leadingIcon = {
                                                    Surface(
                                                        color = if (isLiked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                                            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isLiked) Icons.Filled.ThumbDown else Icons.Rounded.ThumbUp,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(6.dp)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    streamingSong?.let { s ->
                                                        if (isLiked) viewModel.unlikeSong(s) else viewModel.likeSong(s)
                                                    }
                                                }
                                            )
                                        }

                                        // Add to Playlist
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Add to playlist", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                                leadingIcon = {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons.AddToPlaylist,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.fillMaxSize().padding(6.dp)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    sortedSongsById[localSong.id]?.let { s ->
                                                        onAddSongToPlaylist(s)
                                                    }
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
                                                            imageVector = Icons.Rounded.Info,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(6.dp)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    dismissMenu()
                                                    selectedSongForInfo = localSong
                                                    showSongInfoSheet = true
                                                }
                                            )
                                        }

                                        val resolvedAlbum = sortedSongsById[localSong.id]?.let { streamingSong ->
                                            val albumArtist = localSong.albumArtist?.takeIf { it.isNotBlank() } ?: localSong.artist
                                            sortedAlbums.firstOrNull { album ->
                                                val albumMatchesById = streamingSong.albumId?.let { album.id == it } == true
                                                val albumMatchesByMetadata = album.title.equals(localSong.album, ignoreCase = true) &&
                                                    album.artist.equals(albumArtist, ignoreCase = true)
                                                albumMatchesById || albumMatchesByMetadata
                                            }
                                        }

                                        resolvedAlbum?.let { album ->
                                            // Go to album
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
                                                            "Go to album",
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
                                                                imageVector = Icons.Rounded.Album,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .padding(6.dp)
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        dismissMenu()
                                                        openAlbumBottomSheet(album)
                                                    }
                                                )
                                            }
                                        }

                                        // Go to artist
                                        resolvedArtist?.let { artist ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Go to artist", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                                    leadingIcon = {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                                            shape = CircleShape,
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.fillMaxSize().padding(6.dp)
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        dismissMenu()
                                                        onNavigateToArtist(artist)
                                                    }
                                                )
                                            }
                                        }

                                    },
                                    onRefreshClick = { viewModel.loadLibrary() }
                                )
                            }

                            StreamingLibraryTab.ARTISTS -> {
                                SingleCardArtistsContent(
                                    artists = localArtists,
                                    onArtistClick = { localArtist ->
                                        val resolvedArtist = sortedArtists.firstOrNull { it.id == localArtist.id }
                                            ?: sortedArtists.firstOrNull {
                                                it.name.equals(localArtist.name, ignoreCase = true)
                                            }
                                        resolvedArtist?.let(onNavigateToArtist)
                                    },
                                    haptics = haptics,
                                    onPlayQueue = { localQueue ->
                                        val queue = mapLocalSongsToStreaming(localQueue)
                                        if (queue.isNotEmpty()) {
                                            viewModel.playQueue(queue = queue, startIndex = 0, shuffle = false)
                                        }
                                    },
                                    onShuffleQueue = { localQueue ->
                                        val queue = mapLocalSongsToStreaming(localQueue)
                                        if (queue.isNotEmpty()) {
                                            viewModel.playQueue(
                                                queue = queue,
                                                startIndex = if (queue.size > 1) random.nextInt(queue.size) else 0,
                                                shuffle = true
                                            )
                                        }
                                    },
                                    onRefreshClick = { viewModel.loadLibrary() }
                                )
                            }

                            StreamingLibraryTab.PLAYLISTS -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    SingleCardPlaylistsContent(
                                        playlists = localPlaylists,
                                        onPlaylistClick = { localPlaylist ->
                                            localPlaylistsById[localPlaylist.id]?.let(onNavigateToPlaylist)
                                        },
                                        haptics = haptics,
                                        onCreatePlaylist = {
                                            showCreatePlaylistDialog = true
                                        },
                                        appSettings = appSettings,
                                        onRefreshClick = { viewModel.loadLibrary() }
                                    )

                                    PlaylistFabMenu(
                                        visible = true,
                                        expanded = showPlaylistFabMenu,
                                        onExpandedChange = { showPlaylistFabMenu = it },
                                        onCreatePlaylist = {
                                            showCreatePlaylistDialog = true
                                        },
                                        onImportPlaylist = null,
                                        onExportPlaylists = null,
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        bottomPadding = 0.dp,
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

        // Album BottomSheet - rendered outside tabs so it's available from any tab
        if (showAlbumBottomSheet && selectedAlbumForSheet != null) {
            val albumForSheet = selectedAlbumForSheet!!
            val streamingTracks = albumForSheet.tracks
            val libraryAlbum = albumForSheet.toLibraryAlbum(localSongs)

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
                        val startIndex = random.nextInt(streamingTracks.size)
                        viewModel.playQueue(queue = streamingTracks, startIndex = startIndex, shuffle = true)
                    }
                },
                onAddToQueue = { },
                onAddSongToPlaylist = { },
                onPlayerClick = { },
                sheetState = albumSheetState,
                haptics = haptics,
                onToggleFavorite = { localSong ->
                    sortedSongsById[localSong.id]?.let { streamingSong ->
                        if (streamingFavoriteSongIds.contains(streamingSong.id)) {
                            viewModel.unlikeSong(streamingSong)
                        } else {
                            viewModel.likeSong(streamingSong)
                        }
                    }
                },
                favoriteSongs = streamingFavoriteSongIds,
                onShowSongInfo = { song ->
                    selectedSongForInfo = song
                    showSongInfoSheet = true
                },
                showPlayNextAction = false,
                showAddToQueueAction = false,
                showAddToPlaylistAction = false,
                showAddToBlacklistAction = false,
                currentSong = currentLocalSong,
                isPlaying = isPlayerPlaying
            )
        }

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

    // Create Playlist Dialog for streaming
    if (showCreatePlaylistDialog) {
        chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
private fun StreamingSongsTabPage(
    songs: List<StreamingSong>,
    isLoading: Boolean,
    onPlaySongAtIndex: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            StreamingLibrarySectionHeader(
                title = stringResource(id = R.string.library_your_music),
                subtitle = stringResource(
                    id = R.string.streaming_home_widget_playlist_track_count,
                    songs.size
                ),
                onPlayAll = if (songs.isNotEmpty()) onPlayAll else null,
                onShufflePlay = if (songs.size > 1) onShuffle else null
            )
        }

        if (isLoading && songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StreamingLibraryLoadingCard()
                }
            }
        } else if (songs.isEmpty()) {
            item {
                StreamingLibraryEmptyCard(
                    icon = Icons.Rounded.History,
                    title = stringResource(id = R.string.library_no_songs),
                    subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                )
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                StreamingLibrarySongRow(
                    song = song,
                    onClick = { onPlaySongAtIndex(index) }
                )
            }
        }
    }
}

@Composable
private fun StreamingAlbumsTabPage(
    albums: List<StreamingAlbum>,
    isLoading: Boolean,
    onOpenAlbum: (StreamingAlbum) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            StreamingLibrarySectionHeader(
                title = stringResource(id = R.string.library_your_albums),
                subtitle = stringResource(
                    id = R.string.library_albums_count,
                    albums.size
                )
            )
        }

        if (isLoading && albums.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StreamingLibraryLoadingCard()
                }
            }
        } else if (albums.isEmpty()) {
            item {
                StreamingLibraryEmptyCard(
                    icon = Icons.Rounded.Album,
                    title = stringResource(id = R.string.library_no_albums),
                    subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                )
            }
        } else {
            items(albums, key = { it.id }) { album ->
                StreamingLibraryAlbumRow(
                    album = album,
                    onClick = { onOpenAlbum(album) }
                )
            }
        }
    }
}

@Composable
private fun StreamingArtistsTabPage(
    artists: List<StreamingArtist>,
    isLoading: Boolean,
    onOpenArtist: (StreamingArtist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            StreamingLibrarySectionHeader(
                title = stringResource(id = R.string.library_your_artists),
                subtitle = "${artists.size} artists"
            )
        }

        if (isLoading && artists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StreamingLibraryLoadingCard()
                }
            }
        } else if (artists.isEmpty()) {
            item {
                StreamingLibraryEmptyCard(
                    icon = Icons.Rounded.Person,
                    title = stringResource(id = R.string.library_no_artists),
                    subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                )
            }
        } else {
            items(artists, key = { it.id }) { artist ->
                StreamingLibraryArtistRow(
                    artist = artist,
                    onClick = { onOpenArtist(artist) }
                )
            }
        }
    }
}

@Composable
private fun StreamingPlaylistsTabPage(
    playlists: List<StreamingPlaylist>,
    isLoading: Boolean,
    onOpenPlaylist: (StreamingPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            StreamingLibrarySectionHeader(
                title = stringResource(id = R.string.library_your_playlists),
                subtitle = stringResource(
                    id = R.string.library_playlists_count,
                    playlists.size
                )
            )
        }

        if (isLoading && playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StreamingLibraryLoadingCard()
                }
            }
        } else if (playlists.isEmpty()) {
            item {
                StreamingLibraryEmptyCard(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = stringResource(id = R.string.library_no_playlists),
                    subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint)
                )
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                StreamingLibraryPlaylistRow(
                    playlist = playlist,
                    onClick = { onOpenPlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
private fun StreamingLibrarySectionHeader(
    title: String,
    subtitle: String,
    onPlayAll: (() -> Unit)? = null,
    onShufflePlay: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onPlayAll != null || onShufflePlay != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onPlayAll?.let { playAction ->
                    FilledTonalButton(onClick = playAction) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.action_play))
                    }
                }

                onShufflePlay?.let { shuffleAction ->
                    FilledTonalButton(onClick = shuffleAction) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.action_shuffle))
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingSortMenuItem(
    label: String,
    icon: ImageVector,
    ascending: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    showSortDirection: Boolean = true
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            trailingIcon = {
                when {
                    selected -> Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    showSortDirection -> Icon(
                        imageVector = if (ascending) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = onClick,
            colors = MenuDefaults.itemColors(
                textColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        )
    }
}

@Composable
private fun StreamingLibraryDisconnectedCard(
    selectedServiceName: String,
    onConfigureService: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.streaming_home_selected_service_unavailable),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    id = R.string.streaming_home_connect_selected_service,
                    selectedServiceName
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onConfigureService,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.streaming_manage_service))
            }
        }
    }
}

@Composable
private fun StreamingLibraryLoadingCard(
    modifier: Modifier = Modifier
) {
    StreamingLibraryStateCard(
        title = stringResource(id = R.string.streaming_library_syncing),
        subtitle = stringResource(id = R.string.streaming_home_widget_empty_hint),
        icon = Icons.Rounded.History,
        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        centeredContent = true,
        modifier = modifier
    )
}

@Composable
private fun StreamingLibraryStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    showProgressIndicator: Boolean = false,
    centeredContent: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = if (centeredContent) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (showProgressIndicator) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iconTint
                        )
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
                fontWeight = FontWeight.SemiBold
                ,
                textAlign = if (centeredContent) TextAlign.Center else TextAlign.Start
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (centeredContent) TextAlign.Center else TextAlign.Start
            )

            if (actionText != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = if (centeredContent) Modifier else Modifier.fillMaxWidth()
                ) {
                    Text(text = actionText)
                }
            }
        }
    }
}

@Composable
private fun StreamingLibrarySongRow(
    song: StreamingSong,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.TrackImage(
                imageUrl = song.artworkUri,
                trackName = song.title,
                modifier = Modifier.size(66.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatCompactDuration(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreamingLibraryAlbumRow(
    album: StreamingAlbum,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.AlbumArt(
                imageUrl = album.artworkUri,
                albumName = album.title,
                modifier = Modifier.size(66.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = album.songCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreamingLibraryArtistRow(
    artist: StreamingArtist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.ArtistImage(
                imageUrl = artist.artworkUri,
                artistName = artist.name,
                modifier = Modifier.size(66.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.songCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StreamingLibraryPlaylistRow(
    playlist: StreamingPlaylist,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            M3ImageUtils.PlaylistImage(
                imageUrl = playlist.artworkUri,
                playlistName = playlist.name,
                modifier = Modifier.size(66.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description.orEmpty().ifBlank {
                        stringResource(id = R.string.streaming_home_widget_playlist_track_count, playlist.songCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StreamingLibraryEmptyCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun deriveAlbumsFromSongs(songs: List<StreamingSong>): List<StreamingAlbum> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    return songs
        .filter { it.album.isNotBlank() }
        .groupBy { song -> song.albumId ?: "${song.sourceType.name}:${song.artist.lowercase()}:${song.album.lowercase()}" }
        .values
        .sortedByDescending { albumSongs -> albumSongs.size }
        .take(40)
        .map { albumSongs ->
            val firstSong = albumSongs.first()
            StreamingAlbum(
                id = firstSong.albumId ?: "ui-derived:${firstSong.sourceType.name}:album:${firstSong.artist.lowercase()}:${firstSong.album.lowercase()}",
                title = firstSong.album,
                artist = firstSong.albumArtist?.takeIf { it.isNotBlank() } ?: firstSong.artist,
                artworkUri = albumSongs.firstNotNullOfOrNull { it.artworkUri },
                songCount = albumSongs.size,
                year = firstSong.releaseDate?.take(4)?.toIntOrNull(),
                sourceType = firstSong.sourceType,
                tracks = albumSongs
            )
        }
}

fun StreamingSong.toLibrarySong(): Song {
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
        albumId = "${sourceType.name}:${artist.lowercase()}:${album.lowercase()}",
        duration = duration,
        uri = playbackUri,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    )
}

fun StreamingPlaylist.toLibraryPlaylist(): Playlist {
    val loadedTracks = getTracks()
    val displaySongs = if (loadedTracks.isNotEmpty()) {
        loadedTracks.map { it.toLibrarySong() }
    } else if (songCount > 0) {
        // Generate placeholder songs so the count displays correctly in the UI
        (1..songCount).map { i ->
            Song(
                id = "${id}_placeholder_$i",
                title = "Track $i",
                artist = "",
                album = name,
                duration = 0L,
                uri = Uri.parse("streaming://playlist/$id/track/$i")
            )
        }
    } else {
        emptyList()
    }
    return Playlist(
        id = id,
        name = name,
        songs = displaySongs,
        dateCreated = externalId?.hashCode()?.toLong() ?: id.hashCode().toLong(),
        dateModified = snapshotId?.hashCode()?.toLong() ?: songCount.toLong(),
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    )
}

fun StreamingAlbum.toLibraryAlbum(librarySongs: List<chromahub.rhythm.app.shared.data.model.Song>): Album {
    val streamingTracks = tracks
    val matchingSongs = if (streamingTracks.isNotEmpty()) {
        streamingTracks.map { it.toLibrarySong() }
    } else if (librarySongs.isNotEmpty()) {
        librarySongs.filter {
            it.album.equals(title, ignoreCase = true) &&
                it.artist.equals(artist, ignoreCase = true)
        }
    } else {
        emptyList()
    }

    val displayedSongCount = when {
        songCount > 0 -> songCount
        streamingTracks.isNotEmpty() -> streamingTracks.size
        else -> matchingSongs.size
    }

    return Album(
        id = id,
        title = title,
        artist = artist,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        year = year ?: 0,
        songs = matchingSongs,
        numberOfSongs = displayedSongCount
    )
}

private fun StreamingArtist.toLibraryArtist(
    librarySongs: List<Song>,
    libraryAlbums: List<Album>
): Artist {
    val matchingSongs = if (librarySongs.isNotEmpty()) {
        librarySongs.filter { it.artist.equals(name, ignoreCase = true) }
    } else {
        getTopTracks().map { it.toLibrarySong() }
    }

    val matchingAlbums = if (libraryAlbums.isNotEmpty()) {
        libraryAlbums.filter { it.artist.equals(name, ignoreCase = true) }
    } else {
        getAlbumsList().map { it.toLibraryAlbum(matchingSongs) }
    }

    return Artist(
        id = id,
        name = name,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albums = matchingAlbums,
        songs = matchingSongs,
        numberOfAlbums = if (albumCount > 0) albumCount else matchingAlbums.size,
        numberOfTracks = if (songCount > 0) songCount else matchingSongs.size
    )
}

private fun deriveArtistsFromSongs(
    songs: List<StreamingSong>,
    separatorEnabled: Boolean,
    separatorDelimiters: String
): List<StreamingArtist> {
    if (songs.isEmpty()) {
        return emptyList()
    }

    return songs
        .filter { it.artist.isNotBlank() }
        .flatMap { song ->
            val artistNames = ArtistSeparator.splitArtists(
                artistString = song.artist,
                delimiters = separatorDelimiters,
                enabled = separatorEnabled
            )

            if (artistNames.isEmpty()) {
                listOf(song to song.artist.trim())
            } else {
                artistNames.mapNotNull { artistName ->
                    artistName.trim().takeIf { it.isNotBlank() }?.let { trimmedName ->
                        song to trimmedName
                    }
                }
            }
        }
        .groupBy { (song, artistName) -> "${song.sourceType.name}:${artistName.lowercase()}" }
        .values
        .sortedByDescending { artistSongs -> artistSongs.size }
        .take(40)
        .map { artistSongs ->
            val firstSong = artistSongs.first().first
            val artistName = artistSongs.first().second
            val artistTracks = artistSongs.map { it.first }
            StreamingArtist(
                id = "ui-derived:${firstSong.sourceType.name}:artist:${artistName.lowercase()}",
                name = artistName,
                artworkUri = artistTracks.firstNotNullOfOrNull { it.artworkUri },
                songCount = artistTracks.size,
                albumCount = artistTracks.map { it.album }.distinct().size,
                sourceType = firstSong.sourceType,
                topTracks = artistTracks.take(20)
            )
        }
}

private fun formatCompactDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
}

