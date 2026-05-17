package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlin.random.Random

/**
 * Material 3 placeholder types
 */
enum class M3PlaceholderType {
    ALBUM,
    ARTIST,
    TRACK,
    PLAYLIST,
    GENERAL
}

/**
 * A Material 3 placeholder for media content with expressive shape support
 */
@Composable
fun M3Placeholder(
    type: M3PlaceholderType,
    name: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = null
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val expressiveShapesEnabled by appSettings.expressiveShapesEnabled.collectAsState()
    
    // Get the default shapes for each type (when expressive shapes are disabled)
    val defaultShape = when (type) {
        M3PlaceholderType.ALBUM -> RoundedCornerShape(8.dp)
        M3PlaceholderType.ARTIST -> CircleShape
        M3PlaceholderType.TRACK -> RoundedCornerShape(4.dp)
        M3PlaceholderType.PLAYLIST -> RoundedCornerShape(8.dp)
        M3PlaceholderType.GENERAL -> RoundedCornerShape(8.dp)
    }
    
    // Use provided shape if available, otherwise get expressive shape from settings
    val finalShape = shape ?: if (expressiveShapesEnabled) {
        when (type) {
            M3PlaceholderType.ALBUM -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART, defaultShape)
            M3PlaceholderType.ARTIST -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ARTIST_ART, defaultShape)
            M3PlaceholderType.TRACK -> rememberExpressiveShapeFor(ExpressiveShapeTarget.SONG_ART, defaultShape)
            M3PlaceholderType.PLAYLIST -> rememberExpressiveShapeFor(ExpressiveShapeTarget.PLAYLIST_ART, defaultShape)
            M3PlaceholderType.GENERAL -> rememberExpressiveShapeFor(ExpressiveShapeTarget.ALBUM_ART, defaultShape)
        }
    } else {
        defaultShape
    }
    
    val icon = when (type) {
        M3PlaceholderType.ALBUM -> Icons.Filled.Album
        M3PlaceholderType.ARTIST -> Icons.Filled.Person
        M3PlaceholderType.TRACK -> Icons.Filled.MusicNote
        M3PlaceholderType.PLAYLIST -> Icons.AutoMirrored.Filled.QueueMusic
        M3PlaceholderType.GENERAL -> Icons.Filled.AudioFile
    }
    
    val containerColor = getColorForName(name, MaterialTheme.colorScheme.surfaceVariant)

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = finalShape,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generate a consistent color based on the name
 */
@Composable
private fun getColorForName(name: String?, fallbackColor: Color): Color {
    if (name.isNullOrBlank()) {
        return fallbackColor
    }
    
    val seed = name.hashCode()
    val random = Random(seed)
    
    // Generate a color in the same palette as the theme
    val baseColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )
    
    return baseColors[random.nextInt(baseColors.size)]
}

/**
 * Album art placeholder
 */
@Composable
fun AlbumPlaceholder(name: String? = null, modifier: Modifier = Modifier, shape: Shape? = null) {
    M3Placeholder(
        type = M3PlaceholderType.ALBUM,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Artist image placeholder
 */
@Composable
fun ArtistPlaceholder(name: String? = null, modifier: Modifier = Modifier, shape: Shape? = null) {
    M3Placeholder(
        type = M3PlaceholderType.ARTIST,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Track placeholder
 */
@Composable
fun TrackPlaceholder(name: String? = null, modifier: Modifier = Modifier, shape: Shape? = null) {
    M3Placeholder(
        type = M3PlaceholderType.TRACK,
        name = name,
        modifier = modifier,
        shape = shape
    )
}

/**
 * Playlist placeholder
 */
@Composable
fun PlaylistPlaceholder(name: String? = null, modifier: Modifier = Modifier, shape: Shape? = null) {
    M3Placeholder(
        type = M3PlaceholderType.PLAYLIST,
        name = name,
        modifier = modifier,
        shape = shape
    )
}
