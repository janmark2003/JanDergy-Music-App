package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.*

/**
 * Real-time 8D Audio Effect Engine.
 *
 * Rotates audio around the listener's head by sweeping stereo panning
 * using a smooth LFO (equal-power pan law) combined with subtle distance modulation
 * and binaural crossfeed, creating the classic 8D circular motion in headphones.
 */
@OptIn(UnstableApi::class)
class Rhythm8DProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "Rhythm8DProcessor"

        /**
         * Output makeup gain. The peak of gainL * dist at full depth is ~0.9291,
         * so 1.075 keeps healthy perceived loudness while preventing digital clipping.
         */
        const val MAKEUP_GAIN = 1.075

        /** Smooth fade duration when toggling 8D on/off to prevent clicks/pops (ms). */
        private const val BYPASS_FADE_MS = 25.0
    }

    /** Master on/off flag. */
    @Volatile
    private var effectEnabled: Boolean = false

    /** Rotations per second (0.05 = very slow, 0.12 = classic 8D ~8.3s per circle, 0.5 = fast). */
    @Volatile
    var rotationSpeed: Float = 0.12f

    /** Pan depth 0..1 (how far the sound swings left/right). */
    @Volatile
    var intensity: Float = 0.90f

    /** Crossfeed: how much of the opposite channel blends in so sound never completely cuts out in one ear. */
    @Volatile
    var crossfeed: Float = 0.22f

    /** Reverses the rotation direction (counter-clockwise if true). */
    @Volatile
    var reverse: Boolean = false

    @Volatile
    private var phase: Double = 0.0

    /** Current LFO phase (radians) for visualizers. */
    val currentPhase: Double
        get() = phase

    // Smooth bypass state tracking
    private var enabledTarget = 0.0
    private var enabledSm = 0.0

    /**
     * Enable or disable the 8D audio processor.
     * Compatible with PlaybackService.
     */
    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "8D Processor setEnabled: $enable")
        effectEnabled = enable
        enabledTarget = if (enable) 1.0 else 0.0
        if (enable && enabledSm <= 1e-4) {
            phase = 0.0
        }
    }

    override fun isEnabled(): Boolean = effectEnabled || enabledSm > 1e-4

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleRate <= 0 || sampleCount < 2) return

        val sr = sampleRate.toDouble()
        val inc = 2.0 * PI * rotationSpeed / sr
        val depth = intensity.toDouble().coerceIn(0.0, 1.0)
        val cf = crossfeed.toDouble().coerceIn(0.0, 0.5)

        val bypassSmoothAlpha = (1.0 - exp(-1.0 / (BYPASS_FADE_MS * 0.001 * sr))).coerceIn(0.0001, 1.0)
        val frameLimit = if (sampleCount % 2 == 0) sampleCount else sampleCount - 1

        for (i in 0 until frameLimit step 2) {
            val lIn = samples[i].toDouble()
            val rIn = samples[i + 1].toDouble()

            // Smoothly interpolate bypass state to eliminate clicks when toggled
            enabledSm += bypassSmoothAlpha * (enabledTarget - enabledSm)

            if (!effectEnabled && enabledSm < 1e-5) {
                // Completely bypassed: samples are already untouched in the buffer
                continue
            }

            // 1) Ear crossfeed blend so sound doesn't abruptly drop to silence in opposite ear
            val bl = lIn * (1.0 - cf) + rIn * cf
            val br = rIn * (1.0 - cf) + lIn * cf

            // 2) Equal-power panning driven by slow sine LFO
            val pan = depth * sin(phase)               // -1.0 .. +1.0
            val angle = (pan + 1.0) * (PI / 4.0)       // 0.0 .. PI/2
            val gainL = cos(angle)
            val gainR = sin(angle)

            // 3) Subtle distance modulation for circular (front/back) depth
            val dist = 0.88 + 0.12 * cos(phase)

            // 4) Apply spatial gains & makeup gain
            val processedL = bl * gainL * dist * MAKEUP_GAIN
            val processedR = br * gainR * dist * MAKEUP_GAIN

            // 5) Wet/dry crossfade for click-free toggling
            val mixedL = lIn * (1.0 - enabledSm) + processedL * enabledSm
            val mixedR = rIn * (1.0 - enabledSm) + processedR * enabledSm

            // 6) Safe soft limiting & write back to short array
            samples[i] = clamp(mixedL)
            samples[i + 1] = clamp(mixedR)

            // 7) Advance LFO phase
            phase += if (reverse) -inc else inc
            if (phase >= 2.0 * PI) phase -= 2.0 * PI
            if (phase < 0.0) phase += 2.0 * PI
        }
    }

    /**
     * Soft limiting clamp near full scale to prevent harsh digital clipping.
     */
    private fun clamp(v: Double): Short {
        val a = abs(v)
        val maxVal = 32767.0
        val threshold = 30000.0 // ~ -0.7 dBFS
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
        phase = 0.0
    }

    override fun reset() {
        super.reset()
        phase = 0.0
    }
}