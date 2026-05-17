package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.util.HapticUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowSongInfo: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    showRemoveFromPlaylist: Boolean = true,
    haptics: HapticFeedback
) {
    val context = LocalContext.current
    var showContent by remember { mutableStateOf(false) }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Header with song info
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Song options",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Song title and artist
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                    // Row 1: Play next, Add to queue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = Icons.Rounded.SkipNext,
                                text = "Play next",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onPlayNext()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.Queue,
                                text = "Add to queue",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onAddToQueue()
                                }
                            )
                        }
                    }
                    
                    // Row 2: Add to playlist, Go to album
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.AddToPlaylist,
                                text = "Add to playlist",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onAddToPlaylist()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.Album,
                                text = "Go to album",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onGoToAlbum()
                                }
                            )
                        }
                    }
                    
                    // Row 3: Go to artist, Song info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = RhythmIcons.Artist,
                                text = "Go to artist",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onGoToArtist()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SongOptionGridItem(
                                icon = Icons.Rounded.Info,
                                text = "Song info",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                    onShowSongInfo()
                                }
                            )
                        }
                    }
                    
                    if (showRemoveFromPlaylist) {
                        // Row 4: Remove from playlist (full width, error color)
                        SongOptionGridItem(
                            icon = RhythmIcons.Remove,
                            text = "Remove from playlist",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                                onRemoveFromPlaylist()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SongOptionGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
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
                        .fillMaxWidth()
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
