package chromahub.rhythm.app.features.streaming.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.streaming.domain.model.StreamingArtist
import chromahub.rhythm.app.features.streaming.domain.model.StreamingPlaylist
import chromahub.rhythm.app.features.streaming.presentation.model.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsSearchBar

@Composable
fun StreamingSearchScreen(
    viewModel: StreamingMusicViewModel,
    onConfigureService: (String) -> Unit,
    onNavigateToArtist: (StreamingArtist) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }

    val selectedService by appSettings.streamingService.collectAsState()
    val sessions by viewModel.serviceSessions.collectAsState()
    val selectedOption = remember(selectedService) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == selectedService }
    }
    val selectedServiceName = selectedOption?.let { context.getString(it.nameRes) }
        ?: context.getString(R.string.streaming_not_selected)
    val isSelectedServiceConnected = sessions[selectedService]?.isConnected == true

    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedHomeContent by viewModel.hasLoadedHomeContent.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val featuredPlaylists by viewModel.featuredPlaylists.collectAsState()

    val hasProviderDiscovery =
        recommendations.isNotEmpty() ||
            newReleases.isNotEmpty() ||
            featuredPlaylists.isNotEmpty()

    LaunchedEffect(selectedService, isSelectedServiceConnected, hasProviderDiscovery, hasLoadedHomeContent) {
        if (isSelectedServiceConnected && !hasProviderDiscovery && !hasLoadedHomeContent) {
            viewModel.loadHomeContent()
        }
    }

    CollapsibleHeaderScreen(
        title = "$selectedServiceName Search",
        headerDisplayMode = 1
    ) { contentModifier ->
        LazyColumn(
            modifier = modifier
                .then(contentModifier)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsSearchBar(
                    query = query,
                    onQueryChange = { viewModel.search(it) },
                    modifier = Modifier.fillMaxWidth(),
                    hint = "Search"
                )
            }

            if (!isSelectedServiceConnected) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.streaming_search_unavailable_title),
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
                                onClick = { onConfigureService(selectedService) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(id = R.string.streaming_manage_service))
                            }
                        }
                    }
                }
            } else if (query.isBlank()) {
                if (hasProviderDiscovery) {
                    if (recommendations.isNotEmpty()) {
                        item {
                            Material3SettingsGroup(
                                title = stringResource(id = R.string.streaming_home_widget_recommended_title),
                                items = recommendations.take(8).map { song ->
                                    Material3SettingsItem(
                                        title = {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = if (song.album.isBlank()) song.artist else "${song.artist} - ${song.album}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        isHighlighted = true,
                                        onClick = { viewModel.playSong(song) }
                                    )
                                }
                            )
                        }
                    }

                    if (featuredPlaylists.isNotEmpty()) {
                        item {
                            Material3SettingsGroup(
                                title = stringResource(id = R.string.streaming_home_widget_playlists_title),
                                items = featuredPlaylists.take(6).map { playlist ->
                                    Material3SettingsItem(
                                        icon = Icons.Filled.Search,
                                        title = {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = "${playlist.songCount} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        isHighlighted = true,
                                        onClick = { viewModel.playPlaylist(playlist) }
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Material3SettingsGroup(
                        title = stringResource(id = R.string.streaming_search_suggestions_title),
                        items = serviceSearchSuggestions(selectedService).map { suggestion ->
                            Material3SettingsItem(
                                icon = Icons.Filled.Search,
                                title = {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                description = {
                                    Text(
                                        text = stringResource(id = R.string.streaming_search_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                isHighlighted = true,
                                onClick = { viewModel.search(suggestion) }
                            )
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(id = R.string.search_results),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLoading) {
                    item {
                        Text(
                            text = stringResource(id = R.string.streaming_status_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (results.isEmpty) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.streaming_search_results_empty, selectedServiceName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    if (results.songs.isNotEmpty()) {
                        item {
                            Material3SettingsGroup(
                                title = "Songs (${results.songs.size})",
                                items = results.songs.take(20).map { song ->
                                    Material3SettingsItem(
                                        title = {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = if (song.album.isBlank()) song.artist else "${song.artist} - ${song.album}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { viewModel.playSong(song) }
                                    )
                                }
                            )
                        }
                    }

                    if (results.artists.isNotEmpty()) {
                        item {
                            Material3SettingsGroup(
                                title = "Artists (${results.artists.size})",
                                items = results.artists.take(8).map { artist ->
                                    Material3SettingsItem(
                                        title = {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = "${artist.songCount} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { onNavigateToArtist(artist) }
                                    )
                                }
                            )
                        }
                    }

                    if (results.playlists.isNotEmpty()) {
                        item {
                            Material3SettingsGroup(
                                title = "Playlists (${results.playlists.size})",
                                items = results.playlists.take(8).map { playlist ->
                                    Material3SettingsItem(
                                        title = {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = "${playlist.songCount} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

fun serviceSearchSuggestions(serviceId: String): List<String> {
    return when (serviceId.uppercase()) {
        StreamingServiceOptions.SUBSONIC -> listOf(
            "Albums added this week",
            "Top played artists",
            "Recently played tracks"
        )
        StreamingServiceOptions.JELLYFIN -> listOf(
            "Favorite albums",
            "Library by genre",
            "Recently added songs"
        )
        else -> listOf(
            "Try searching for songs",
            "Try searching for artists",
            "Try searching for playlists"
        )
    }
}
