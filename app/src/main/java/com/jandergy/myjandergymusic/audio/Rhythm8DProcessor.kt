package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

/**
 * Real-time Interactive 8D Spatial Audio Engine.
 *
 * Supports:
 * - MODE_AUTO: Smooth equal-power LFO circular orbit around the listener's head.
 * - MODE_MANUAL (Touch Radar Pad & Gyroscope Tilt): Real-time 2D spatial placement (x, y)
 *   with smooth slewing, ear crossfeed, distance attenuation, and rear-head warmth darkening.
 * Publishes live coordinates to [AudioTelemetry] for the 2D Radar UI.
 */
@OptIn(UnstableApi::class)
class Rhythm8DProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "Rhythm8DProcessor"
        const val MODE_AUTO = 0
        const val MODE_MANUAL = 1

        const val MAKEUP_GAIN = 1.075
        private const val BYPASS_FADE_MS = 25.0
        private const val MANUAL_SLEW_MS = 18.0
    }

    @Volatile
    private var effectEnabled: Boolean = false

    @Volatile
    var spatialMode: Int = MODE_AUTO

    /** Rotations per second (0.05 = slow, 0.12 = classic 8D ~8.3s, 0.45 = fast). */
    @Volatile
    var rotationSpeed: Float = 0.12f

    /** Pan depth 0..1. */
    @Volatile
    var intensity: Float = 0.90f

    /** Opposite-ear crossfeed so sound stays immersive in headphones. */
    @Volatile
    var crossfeed: Float = 0.22f

    /** Counter-clockwise rotation when true. */
    @Volatile
    var reverse: Boolean = false

    /** Target manual 2D coordinates in [-1..1] (x: left..right, y: back..front). */
    @Volatile
    private var targetX: Float = 0f

    @Volatile
    private var targetY: Float = 1f

    @Volatile
    private var phase: Double = 0.0

    val currentPhase: Double
        get() = phase

    @Volatile
    private var enabledTarget = 0.0
    private var enabledSm = 0.0

    // Smoothed manual position on audio thread
    private var smoothX = 0.0
    private var smoothY = 1.0

    // Subtle 1-pole rear head-shadow low-pass filter state
    private var rearLpL = 0.0
    private var rearLpR = 0.0

    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "8D Processor setEnabled: $enable")
        effectEnabled = enable
        enabledTarget = if (enable) 1.0 else 0.0
        AudioTelemetry.spatialActive = enable
        if (enable && enabledSm <= 1e-4) {
            phase = 0.0
        }
    }

    fun setManualPosition(x: Float, y: Float) {
        val len = hypot(x.toDouble(), y.toDouble())
        if (len > 1.0) {
            targetX = (x / len).toFloat()
            targetY = (y / len).toFloat()
        } else {
            targetX = x.coerceIn(-1f, 1f)
            targetY = y.coerceIn(-1f, 1f)
        }
    }

    override fun isEnabled(): Boolean = effectEnabled || enabledSm > 1e-4

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleRate <= 0 || sampleCount < 2) return

        val sr = sampleRate.toDouble()
        val inc = 2.0 * PI * rotationSpeed.coerceIn(0.03f, 0.8f) / sr
        val depth = intensity.toDouble().coerceIn(0.0, 1.0)
        val cf = crossfeed.toDouble().coerceIn(0.0, 0.5)
        val isManual = spatialMode == MODE_MANUAL

        val bypassSmoothAlpha = (1.0 - exp(-1.0 / (BYPASS_FADE_MS * 0.001 * sr))).coerceIn(0.0001, 1.0)
        val slewAlpha = (1.0 - exp(-1.0 / (MANUAL_SLEW_MS * 0.001 * sr))).coerceIn(0.0005, 1.0)
        val rearLpAlpha = (1.0 - exp(-2.0 * PI * 3200.0 / sr)).coerceIn(0.05, 0.95)

        val frameLimit = sampleCount and -2
        val tX = targetX.toDouble()
        val tY = targetY.toDouble()

        var lastX = 0.0
        var lastY = 1.0

        for (i in 0 until frameLimit step 2) {
            val lIn = samples[i].toDouble()
            val rIn = samples[i + 1].toDouble()

            enabledSm += bypassSmoothAlpha * (enabledTarget - enabledSm)
            if (!effectEnabled && enabledSm < 1e-5) {
                continue
            }

            val posX: Double
            val posY: Double
            if (isManual) {
                smoothX += slewAlpha * (tX - smoothX)
                smoothY += slewAlpha * (tY - smoothY)
                posX = smoothX
                posY = smoothY
            } else {
                posX = sin(phase)
                posY = cos(phase)
                phase += if (reverse) -inc else inc
                if (phase >= 2.0 * PI) phase -= 2.0 * PI
                if (phase < 0.0) phase += 2.0 * PI
            }
            lastX = posX
            lastY = posY

            // 1) Ear crossfeed blend
            val bl = lIn * (1.0 - cf) + rIn * cf
            val br = rIn * (1.0 - cf) + lIn * cf

            // 2) Equal-power stereo panning from X coordinate
            val pan = (depth * posX).coerceIn(-1.0, 1.0)
            val angle = (pan + 1.0) * (PI / 4.0)
            val gainL = cos(angle)
            val gainR = sin(angle)

            // 3) Subtle front/back distance & pinna head-shadow darkening when behind head (posY < 0)
            val dist = 0.88 + 0.12 * posY
            val rearAmt = ((-posY).coerceIn(0.0, 1.0)) * 0.45
            rearLpL += rearLpAlpha * (bl - rearLpL)
            rearLpR += rearLpAlpha * (br - rearLpR)
            val shapedL = bl * (1.0 - rearAmt) + rearLpL * rearAmt
            val shapedR = br * (1.0 - rearAmt) + rearLpR * rearAmt

            // 4) Spatial gain + makeup gain
            val processedL = shapedL * gainL * dist * MAKEUP_GAIN
            val processedR = shapedR * gainR * dist * MAKEUP_GAIN

            // 5) Click-free wet/dry crossfade
            val mixedL = lIn * (1.0 - enabledSm) + processedL * enabledSm
            val mixedR = rIn * (1.0 - enabledSm) + processedR * enabledSm

            samples[i] = clamp(mixedL)
            samples[i + 1] = clamp(mixedR)
        }

        // Publish live visual position for Radar Pad UI
        AudioTelemetry.spatialX = lastX.toFloat()
        AudioTelemetry.spatialY = lastY.toFloat()
        AudioTelemetry.spatialActive = effectEnabled
    }

    private fun clamp(v: Double): Short {
        val a = abs(v)
        val maxVal = 32767.0
        val threshold = 30000.0
        val finalVal = if (a <= threshold) {
            v
        } else {
            val excess = a - threshold
            val compressed = threshold + (maxVal - threshold) * (1.0 - exp(-excess / (maxVal - threshold)))
            sign(v) * compressed.coerceAtMost(maxVal)
        }
        return finalVal.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    override fun flush() {
        super.flush()
        rearLpL = 0.0
        rearLpR = 0.0
    }

    override fun reset() {
        super.reset()
        phase = 0.0
        rearLpL = 0.0
        rearLpR = 0.0
        AudioTelemetry.spatialActive = false
    }
}
