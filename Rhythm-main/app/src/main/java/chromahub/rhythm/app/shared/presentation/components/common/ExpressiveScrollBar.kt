@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Expressive scrollbar component with smooth animations and interactive features.
 * Adapts to both LazyColumn and LazyVerticalGrid.
 *
 * @param modifier Modifier for the scrollbar container
 * @param listState LazyListState for LazyColumn (mutually exclusive with gridState)
 * @param gridState LazyGridState for LazyVerticalGrid (mutually exclusive with listState)
 * @param minHeight Minimum height of the scrollbar thumb
 * @param thickness Default thickness when not interacting
 * @param indicatorExpandedWidth Width when interacting
 * @param paddingEnd Padding from the right edge
 * @param trackGap Gap between thumb and track
 */
@Composable
fun ExpressiveScrollBar(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    minHeight: Dp = 48.dp,
    thickness: Dp = 8.dp,
    indicatorExpandedWidth: Dp = 24.dp,
    paddingEnd: Dp = 4.dp,
    trackGap: Dp = 8.dp
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(-1f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.secondaryContainer
    val innerIcon = Icons.Rounded.UnfoldMore

    val isInteracting = isPressed || isDragging
    
    val animatedWidth by animateDpAsState(
        targetValue = if (isInteracting) indicatorExpandedWidth else thickness,
        animationSpec = tween(durationMillis = 200),
        label = "WidthAnimation"
    )
    
    val iconAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "IconAlpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(indicatorExpandedWidth + paddingEnd)
    ) {
        val density = LocalDensity.current
        val constraintsMaxWidth = maxWidth
        val constraintsMaxHeight = maxHeight

        val canScrollForward by remember { derivedStateOf { listState?.canScrollForward ?: gridState?.canScrollForward ?: false } }
        val canScrollBackward by remember { derivedStateOf { listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false } }
        
        if (!canScrollForward && !canScrollBackward) return@BoxWithConstraints

        fun getScrollStats(): Triple<Float, Int, Float> {
            val totalItemsCount: Int
            val firstVisibleItemIndex: Int
            val visibleCount: Int

            if (listState != null) {
                val layoutInfo = listState.layoutInfo
                totalItemsCount = layoutInfo.totalItemsCount
                firstVisibleItemIndex = listState.firstVisibleItemIndex
                visibleCount = layoutInfo.visibleItemsInfo.size
            } else if (gridState != null) {
                val layoutInfo = gridState.layoutInfo
                totalItemsCount = layoutInfo.totalItemsCount
                firstVisibleItemIndex = gridState.firstVisibleItemIndex
                visibleCount = layoutInfo.visibleItemsInfo.size
            } else {
                return Triple(0f, 0, 1f)
            }

            if (totalItemsCount == 0) return Triple(0f, 0, 1f)

            val progress = firstVisibleItemIndex.toFloat() / totalItemsCount.coerceAtLeast(1)
            val thumbSizeFraction = (visibleCount.toFloat() / totalItemsCount.coerceAtLeast(1)).coerceIn(0.1f, 1f)
            return Triple(progress, totalItemsCount, thumbSizeFraction)
        }

        val (scrollProgress, totalItems, thumbSizeFraction) = getScrollStats()

        suspend fun scrollToProgress(progress: Float) {
            val targetIndex = (progress * totalItems).toInt().coerceIn(0, totalItems - 1)
            listState?.animateScrollToItem(targetIndex)
            gridState?.animateScrollToItem(targetIndex)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalItems) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { offset ->
                            val containerHeight = size.height.toFloat()
                            val tapProgress = (offset.y / containerHeight).coerceIn(0f, 1f)
                            coroutineScope.launch { scrollToProgress(tapProgress) }
                        }
                    )
                }
                .pointerInput(totalItems) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            dragProgress = -1f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = -1f
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val containerHeight = size.height.toFloat()
                            val newProgress = (change.position.y / containerHeight).coerceIn(0f, 1f)
                            dragProgress = newProgress
                            coroutineScope.launch { scrollToProgress(newProgress) }
                        }
                    )
                }
        ) {
            val currentProgress = if (isDragging && dragProgress >= 0f) dragProgress else scrollProgress
            val containerHeightPx = with(density) { constraintsMaxHeight.toPx() }
            val minHeightPx = with(density) { minHeight.toPx() }
            val thumbHeightPx = (containerHeightPx * thumbSizeFraction).coerceAtLeast(minHeightPx)
            val maxThumbTop = containerHeightPx - thumbHeightPx
            val thumbTop = (currentProgress * maxThumbTop).coerceIn(0f, maxThumbTop)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = paddingEnd)
            ) {
                val width = animatedWidth.toPx()
                val thumbLeft = size.width - width

                // Draw thumb
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(width, thumbHeightPx),
                    cornerRadius = CornerRadius(width / 2f)
                )
            }

            // Icon indicator
            if (isInteracting) {
                val iconSize = 18.dp
                val iconOffsetY = with(density) { thumbTop.toDp() + (thumbHeightPx.toDp() - iconSize) / 2 }
                
                Icon(
                    imageVector = innerIcon,
                    contentDescription = "Scroll Indicator",
                    modifier = Modifier
                        .offset(
                            x = paddingEnd + (animatedWidth - iconSize) / 2,
                            y = iconOffsetY
                        )
                        .size(iconSize)
                        .graphicsLayer { alpha = iconAlpha },
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
