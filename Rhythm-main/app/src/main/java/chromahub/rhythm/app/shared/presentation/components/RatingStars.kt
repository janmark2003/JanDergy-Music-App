package chromahub.rhythm.app.shared.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A composable to display and interact with song ratings (0-5 stars)
 * 
 * @param rating Current rating (0-5)
 * @param onRatingChanged Callback when rating is changed
 * @param enabled Whether the rating can be changed
 * @param modifier Modifier for the component
 * @param size Size of each star in dp
 * @param filledColor Color of filled stars
 * @param emptyColor Color of empty stars
 */
@Composable
fun RatingStars(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    filledColor: Color = Color(0xFFFFD700), // Gold
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val ratingLabels = listOf(
        "Not Rated",
        "Liked",
        "Good",
        "Great",
        "Loved",
        "Absolute Favorite"
    )
    
    Row(
        modifier = modifier.semantics {
            contentDescription = "${ratingLabels.getOrNull(rating) ?: "Unknown"} - $rating stars"
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val starRating = index + 1
            val isFilled = starRating <= rating
            
            val starColor by animateColorAsState(
                targetValue = if (isFilled) filledColor else emptyColor,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "star_color_$index"
            )
            
            val starSize by animateDpAsState(
                targetValue = if (isFilled) size * 1.1f else size,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "star_size_$index"
            )
            
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "${starRating} stars",
                tint = starColor,
                modifier = Modifier
                    .size(starSize)
                    .graphicsLayer {
                        scaleX = if (isFilled) 1.1f else 1f
                        scaleY = if (isFilled) 1.1f else 1f
                    }
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = ratingLabels.getOrNull(starRating),
                        onClick = {
                            // Toggle: if clicking same rating, unrate it
                            onRatingChanged(if (rating == starRating) 0 else starRating)
                        }
                    )
            )
        }
    }
}

/**
 * Compact version of RatingStars for display only
 */
@Composable
fun RatingStarsDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    filledColor: Color = Color(0xFFFFD700),
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            val isFilled = (index + 1) <= rating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (isFilled) filledColor else emptyColor,
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Get rating label based on rating value
 */
fun getRatingLabel(rating: Int): String {
    return when (rating) {
        0 -> "Not Rated"
        1 -> "Liked"
        2 -> "Good"
        3 -> "Great"
        4 -> "Loved"
        5 -> "Absolute Favorite"
        else -> "Unknown"
    }
}

/**
 * Get rating description for accessibility
 */
fun getRatingDescription(rating: Int): String {
    return when (rating) {
        0 -> "No rating"
        1 -> "Liked - Good for occasional listening"
        2 -> "Good - Solid track"
        3 -> "Great - Really enjoying this one"
        4 -> "Loved - One of my favorites"
        5 -> "Absolute Favorite - Top tier track"
        else -> "Unknown rating"
    }
}
