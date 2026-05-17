package chromahub.rhythm.app.shared.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import chromahub.rhythm.app.ui.theme.PlayerProgressColor
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    activeTrackColor: Color = PlayerProgressColor,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true,
    isPlaying: Boolean = true,
    trackHeight: Dp = 6.dp,
    thumbRadius: Dp = 8.dp,
    waveAmplitudeWhenPlaying: Dp = 3.dp,
    waveLength: Dp = 40.dp,
    waveAnimationDuration: Int = 4000,
    thumbLineHeightWhenInteracting: Dp = 24.dp,
    hideInactiveTrackPortion: Boolean = true,
    isWaveEligible: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed
    
    val hapticFeedback = LocalHapticFeedback.current
    val lastHapticStep = remember { mutableIntStateOf(-1) }

    // Thumb morphing animation
    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "ThumbInteractionAnim"
    )

    // Wave only shows when playing, not interacting, and eligible
    val shouldShowWave = isWaveEligible && isPlaying && !isInteracting

    val animatedWaveAmplitude by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (shouldShowWave) waveAmplitudeWhenPlaying else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "WaveAmplitudeAnim"
    )

    // Conditional phase animation - only when wave should show
    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value

    LaunchedEffect(shouldShowWave, waveAnimationDuration) {
        if (shouldShowWave && waveAnimationDuration > 0) {
            val fullRotation = (2 * PI).toFloat()
            while (shouldShowWave) {
                val start = (phaseShiftAnim.value % fullRotation).let { 
                    if (it < 0f) it + fullRotation else it 
                }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = waveAnimationDuration, easing = LinearEasing)
                )
            }
        }
    }

    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadius.toPx() }
    val waveAmplitudePx = with(LocalDensity.current) { animatedWaveAmplitude.toPx() }
    val waveLengthPx = with(LocalDensity.current) { waveLength.toPx() }
    val thumbLineHeightPx = with(LocalDensity.current) { thumbLineHeightWhenInteracting.toPx() }
    val thumbGapPx = with(LocalDensity.current) { 4.dp.toPx() }
    
    val waveFrequency = if (waveLengthPx > 0f) {
        ((2 * PI) / waveLengthPx).toFloat()
    } else {
        0f
    }

    val wavePath = remember { Path() }

    val sliderVisualHeight = remember(trackHeight, thumbRadius, thumbLineHeightWhenInteracting) {
        max(trackHeight * 2, max(thumbRadius * 2, thumbLineHeightWhenInteracting) + 8.dp)
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        Slider(
            value = value,
            onValueChange = { newValue ->
                val currentStep = (newValue * 100).toInt()
                if (currentStep != lastHapticStep.intValue) {
                    try {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (e: SecurityException) {
                        // Permission not granted or other security issue
                        android.util.Log.w("WaveSlider", "Haptic feedback failed due to security exception: ${e.message}")
                    } catch (e: Exception) {
                        // Handle any other exceptions that might occur
                        android.util.Log.w("WaveSlider", "Haptic feedback failed: ${e.message}")
                    }
                    lastHapticStep.intValue = currentStep
                }
                onValueChange(newValue)
            },
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight)
                .drawWithCache {
                    val canvasWidth = size.width
                    val localCenterY = size.height / 2f
                    val localTrackStart = thumbRadiusPx
                    val localTrackEnd = canvasWidth - thumbRadiusPx
                    val localTrackWidth = (localTrackEnd - localTrackStart).coerceAtLeast(0f)

                    val normalizedValue = value.coerceIn(0f, 1f)
                    
                    onDrawWithContent {
                        val currentProgressPxEnd = localTrackStart + localTrackWidth * normalizedValue

                        // Draw inactive track
                        if (hideInactiveTrackPortion) {
                            if (currentProgressPxEnd < localTrackEnd) {
                                drawLine(
                                    color = inactiveTrackColor,
                                    start = Offset(currentProgressPxEnd, localCenterY),
                                    end = Offset(localTrackEnd, localCenterY),
                                    strokeWidth = trackHeightPx,
                                    cap = StrokeCap.Round
                                )
                            }
                        } else {
                            drawLine(
                                color = inactiveTrackColor,
                                start = Offset(localTrackStart, localCenterY),
                                end = Offset(localTrackEnd, localCenterY),
                                strokeWidth = trackHeightPx,
                                cap = StrokeCap.Round
                            )
                        }

                        // Draw active track (wave or line)
                        if (normalizedValue > 0f) {
                            val activeTrackVisualEnd = currentProgressPxEnd - (thumbGapPx * thumbInteractionFraction)

                            if (waveAmplitudePx > 0.01f && waveFrequency > 0f) {
                                wavePath.reset()
                                val waveStartDrawX = localTrackStart
                                val waveEndDrawX = activeTrackVisualEnd.coerceAtLeast(waveStartDrawX)
                                if (waveEndDrawX > waveStartDrawX) {
                                    val periodPx = ((2 * PI) / waveFrequency).toFloat()
                                    val samplesPerCycle = 20f
                                    val waveStep = (periodPx / samplesPerCycle)
                                        .coerceAtLeast(1.2f)
                                        .coerceAtMost(trackHeightPx)

                                    fun yAt(x: Float): Float {
                                        val s = sin(waveFrequency * x + phaseShift)
                                        return (localCenterY + waveAmplitudePx * s)
                                            .coerceIn(
                                                localCenterY - waveAmplitudePx - trackHeightPx / 2f,
                                                localCenterY + waveAmplitudePx + trackHeightPx / 2f
                                            )
                                    }

                                    var prevX = waveStartDrawX
                                    var prevY = yAt(prevX)
                                    wavePath.moveTo(prevX, prevY)

                                    var x = prevX + waveStep
                                    while (x < waveEndDrawX) {
                                        val y = yAt(x)
                                        val midX = (prevX + x) * 0.5f
                                        val midY = (prevY + y) * 0.5f
                                        // Compose Path: quadraticBezierTo(controlX, controlY, endX, endY)
                                        wavePath.quadraticTo(prevX, prevY, midX, midY)
                                        prevX = x
                                        prevY = y
                                        x += waveStep
                                    }
                                    val endY = yAt(waveEndDrawX)
                                    wavePath.quadraticTo(prevX, prevY, waveEndDrawX, endY)

                                    drawPath(
                                        path = wavePath,
                                        color = activeTrackColor,
                                        style = Stroke(
                                            width = trackHeightPx,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round, // <- important for smooth joins
                                            miter = 1f
                                        )
                                    )
                                }
                            } else {
                                // Draw straight line when paused
                                if (activeTrackVisualEnd > localTrackStart) {
                                    drawLine(
                                        color = activeTrackColor,
                                        start = Offset(localTrackStart, localCenterY),
                                        end = Offset(activeTrackVisualEnd, localCenterY),
                                        strokeWidth = trackHeightPx,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Draw morphing thumb
                        val currentThumbCenterX = localTrackStart + localTrackWidth * normalizedValue
                        val thumbCurrentWidthPx = lerp(thumbRadiusPx * 2f, trackHeightPx * 1.2f, thumbInteractionFraction)
                        val thumbCurrentHeightPx = lerp(thumbRadiusPx * 2f, thumbLineHeightPx, thumbInteractionFraction)

                        drawRoundRect(
                            color = activeTrackColor,
                            topLeft = Offset(
                                currentThumbCenterX - thumbCurrentWidthPx / 2f,
                                localCenterY - thumbCurrentHeightPx / 2f
                            ),
                            size = Size(thumbCurrentWidthPx, thumbCurrentHeightPx),
                            cornerRadius = CornerRadius(thumbCurrentWidthPx / 2f)
                        )
                    }
                }
        )
    }
}


