package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.util.M3ImageUtils

/**
 * Bottom sheet for batch operations on multiple selected songs.
 * Matches SearchSongOptionsBottomSheet design with grid layout.
 *
 * @param selectedSongs List of selected songs in selection order
 * @param favoriteSongIds Set of song IDs that are currently favorited
 * @param onDismiss Callback when sheet is dismissed
 * @param onPlayAll Play all selected songs
 * @param onAddToQueue Add all to end of queue
 * @param onPlayNext Add all to play next
 * @param onAddToPlaylist Open playlist picker for batch add
 * @param onToggleLikeAll Toggle like status - if all are liked, unlike all; otherwise like all
 * @param onGoToAlbum Navigate to album (first selected song's album)
 * @param onGoToArtist Navigate to artist (first selected song's artist)
 * @param onAddToBlacklist Add all selected songs to blacklist
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectionBottomSheet(
    selectedSongs: List<Song>,
    favoriteSongIds: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onPlayAll: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLikeAll: (shouldLike: Boolean) -> Unit,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onAddToBlacklist: (() -> Unit)? = null,
    onBatchEditTags: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    
    var showContent by remember { mutableStateOf(false) }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentAlpha"
    )
    
    // Compute if all selected songs are liked
    val allAreLiked by remember(selectedSongs, favoriteSongIds) {
        derivedStateOf {
            selectedSongs.isNotEmpty() && selectedSongs.all { favoriteSongIds.contains(it.id) }
        }
    }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with selection info
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                MultiSelectionHeader(selectedSongs = selectedSongs)
            }
            
            // Actions section with grid layout
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Play all, Play next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.Play,
                                text = "Play all",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onPlayAll()
                                    onDismiss()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = Icons.Rounded.SkipNext,
                                text = "Play next",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onPlayNext()
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    // Row 2: Add to queue, Add to playlist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.Queue,
                                text = "Add to queue",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onAddToQueue()
                                    onDismiss()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.AddToPlaylist,
                                text = "Add to playlist",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onAddToPlaylist()
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    // Row 3: Toggle favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SongOptionGridItem(
                                icon = if (allAreLiked) Icons.Rounded.HeartBroken else Icons.Filled.Favorite,
                                text = if (allAreLiked) "Remove from favorites" else "Add to favorites",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onToggleLikeAll(!allAreLiked)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    // Row 4: Go to album, Go to artist (if provided)
                    if (onGoToAlbum != null || onGoToArtist != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onGoToAlbum != null) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = RhythmIcons.Album,
                                        text = "Go to album",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onGoToAlbum()
                                            onDismiss()
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            
                            if (onGoToArtist != null) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = RhythmIcons.Artist,
                                        text = "Go to artist",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onGoToArtist()
                                            onDismiss()
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    
                    // Row 5: Blacklist and Edit tags - combined row for smarter layout
                    if (onAddToBlacklist != null || onBatchEditTags != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Calculate weights based on how many items are present
                            val hasBlacklist = onAddToBlacklist != null
                            val hasEditTags = onBatchEditTags != null
                            val itemCount = listOf(hasBlacklist, hasEditTags).count { it }
                            
                            if (hasBlacklist) {
                                Box(modifier = if (itemCount == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = Icons.Rounded.Block,
                                        text = "Add to blacklist",
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        iconColor = MaterialTheme.colorScheme.error,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onAddToBlacklist()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                            
                            if (hasEditTags) {
                                Box(modifier = if (itemCount == 1) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                                    SongOptionGridItem(
                                        icon = Icons.Rounded.Edit,
                                        text = "Edit tags",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                            onBatchEditTags()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Header showing selection count and stacked album arts
 */
@Composable
private fun MultiSelectionHeader(
    selectedSongs: List<Song>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Multi-selection",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(18.dp))
        
        // Selection info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stacked album arts
                val stackedImageSize = 56.dp
                val stackedOverlap = 28.dp
                val stackedCount = selectedSongs.take(4).size
                val stackedWidth = if (stackedCount > 0) {
                    (stackedImageSize - stackedOverlap) * (stackedCount - 1) + stackedImageSize
                } else stackedImageSize
                
                StackedAlbumArts(
                    songs = selectedSongs.take(4),
                    modifier = Modifier
                        .height(56.dp)
                        .width(stackedWidth)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Song count
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${selectedSongs.size} songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    
                    if (selectedSongs.isNotEmpty()) {
                        Text(
                            text = "${selectedSongs.first().artist} • ${selectedSongs.first().album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid item matching SearchScreen style
 */
@Composable
private fun SongOptionGridItem(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with colored background
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = containerColor.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    containerColor.copy(alpha = 0.15f),
                                    containerColor.copy(alpha = 0.05f)
                                ),
                                radius = 22f
                            )
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Displays stacked album art images with overlap effect
 */
@Composable
private fun StackedAlbumArts(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    val imageSize = 56.dp
    val overlap = 28.dp
    val borderWidth = 3.dp
    val borderColor = MaterialTheme.colorScheme.surface
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        songs.forEachIndexed { index, song ->
            val offsetX = index * (imageSize.value - overlap.value)
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.dp.roundToPx(), 0) }
                    .zIndex((songs.size - index).toFloat())
                    .size(imageSize)
                    .background(borderColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(borderWidth)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    M3ImageUtils.AlbumArt(
                        imageUrl = song.artworkUri,
                        albumName = song.album,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }
        }
    }
}
