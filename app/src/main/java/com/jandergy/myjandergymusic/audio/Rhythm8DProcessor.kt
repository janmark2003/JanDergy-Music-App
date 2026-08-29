package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.*

/**
 * Ultra-Immersive 8D Spatializer Engine (v3 - production-focused lightweight)
 *
 * Improvements over v2:
 *  - Smoothed runtime parameters (wetDryMix / intensity / stereoPreserve)
 *  - Click-free enable/disable via output fade envelope
 *  - 3rd-order Hermite fractional delay interpolation (cleaner moving ITD)
 *  - Output DC blocker
 *  - Safer final limiting (soft limiter curve)
 *  - Guard rails on send/feedback/intensity
 *
 * Notes:
 *  - Still lightweight (no full HRTF convolution yet)
 *  - Designed for headphone playback
 */
@OptIn(UnstableApi::class)
class Rhythm8DProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "Rhythm8DProcessor"

        // Motion
        private const val EFFECT_FREQUENCY = 0.065          // Hz
        private const val MAX_DELAY_MS = 0.75               // interaural delay max

        // Tone / filtering
        private const val BASS_CROSSOVER_HZ = 140.0
        private const val SHELF_FREQ_HZ = 1500.0
        private const val DENORMAL_GUARD = 1e-20f

        // Reverb network
        private const val ROOM_DAMP_COEFF = 0.35f
        private const val ROOM_FEEDBACK = 0.40f

        // Runtime smoothing
        private const val PARAM_SMOOTH_MS = 30.0            // smoothing for user params
        private const val BYPASS_FADE_MS = 18.0             // click-free enable/disable
    }

    // ---------------------------------------------------------------------------------------------
    // Public controls (targets)
    // ---------------------------------------------------------------------------------------------
    private var enabled: Boolean = false
    private var time: Double = 0.0

    /** 0f = dry, 1f = fully processed */
    var wetDryMix: Float = 1.0f
    /** scales movement depth, delay depth, room send */
    var intensity: Float = 1.0f
    /** keep original side information 0..1 */
    var stereoPreserve: Float = 0.30f

    // Smoothed params (actual runtime values)
    private var wetDrySm = 1.0f
    private var intensitySm = 1.0f
    private var stereoPreserveSm = 0.30f

    // Bypass envelope smoothing (0 = bypass, 1 = active)
    private var enabledTarget = 0f
    private var enabledSm = 0f

    // ---------------------------------------------------------------------------------------------
    // Delay / ITD
    // ---------------------------------------------------------------------------------------------
    private var delayBufferL = FloatArray(512)
    private var delayBufferR = FloatArray(512)
    private var delayIndex = 0
    private var maxDelaySamples = 0f

    // Head-shadow shelf state
    private var shelfStateL = 0f
    private var shelfStateR = 0f

    // Bass crossover state (2x one-pole LP per channel)
    private var bassLPF1L = 0f
    private var bassLPF2L = 0f
    private var bassLPF1R = 0f
    private var bassLPF2R = 0f

    // Room network: 3 crossed comb-ish lines
    private var roomBufferL = FloatArray(1987)
    private var roomBufferR = FloatArray(2531)
    private var roomBufferL2 = FloatArray(1381)
    private var roomIdxL = 0
    private var roomIdxR = 0
    private var roomIdxL2 = 0

    // Damping states for room lines
    private var dampStateL = 0f
    private var dampStateR = 0f
    private var dampStateL2 = 0f

    // Output DC blocker states
    private var dcPrevInL = 0f
    private var dcPrevOutL = 0f
    private var dcPrevInR = 0f
    private var dcPrevOutR = 0f

    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "Immersive 8D Engine status: $enable")
        enabled = enable
        enabledTarget = if (enable) 1f else 0f

        // Keep time running only when active target is on, but don't hard-reset audio path instantly.
        // Hard state reset only when turning ON initially from full bypass:
        if (enable && enabledSm <= 1e-4f) {
            time = 0.0
        }
    }

    override fun isEnabled(): Boolean = enabled || enabledSm > 1e-4f

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleRate <= 0) return
        if (sampleCount < 2) return

        val sr = sampleRate.toDouble()
        val dt = 1.0 / sr
        val frameLimit = if (sampleCount % 2 == 0) sampleCount else sampleCount - 1

        // Ensure delay buffer is enough for current sample rate + margin
        val neededDelay = ((MAX_DELAY_MS / 1000.0) * sr).toInt() + 4
        if (delayBufferL.size < neededDelay) {
            val newSize = max(neededDelay.nextPowerOfTwo(), 512)
            delayBufferL = FloatArray(newSize)
            delayBufferR = FloatArray(newSize)
            delayIndex = 0
        }

        maxDelaySamples = ((MAX_DELAY_MS / 1000.0) * sr)
            .toFloat()
            .coerceAtMost((delayBufferL.size - 4).toFloat())

        val bassAlpha = (2.0 * PI * BASS_CROSSOVER_HZ / sr).coerceAtMost(1.0).toFloat()
        val shelfAlpha = (2.0 * PI * SHELF_FREQ_HZ / sr).coerceAtMost(1.0).toFloat()
        val lfoPeriod = 1.0 / EFFECT_FREQUENCY

        val paramSmoothA = onePoleCoeffMs(PARAM_SMOOTH_MS, sr)
        val bypassSmoothA = onePoleCoeffMs(BYPASS_FADE_MS, sr)

        for (i in 0 until frameLimit step 2) {
            val leftIn = samples[i] / 32768.0f
            val rightIn = samples[i + 1] / 32768.0f

            // Smooth controls every sample
            wetDrySm += paramSmoothA * (wetDryMix.coerceIn(0f, 1f) - wetDrySm)
            intensitySm += paramSmoothA * (intensity.coerceIn(0f, 1.25f) - intensitySm)
            stereoPreserveSm += paramSmoothA * (stereoPreserve.coerceIn(0f, 1f) - stereoPreserveSm)
            enabledSm += bypassSmoothA * (enabledTarget - enabledSm)

            // If effectively bypassed and target off, just passthrough quickly
            if (!enabled && enabledSm < 1e-5f) {
                // still apply tiny DC blocker passthrough for consistency
                val outL = dcBlock(leftIn, true)
                val outR = dcBlock(rightIn, false)
                samples[i] = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                samples[i + 1] = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                continue
            }

            time += dt

            // 1) Bass anchor split
            bassLPF1L += bassAlpha * (leftIn - bassLPF1L) + DENORMAL_GUARD
            bassLPF2L += bassAlpha * (bassLPF1L - bassLPF2L)
            val bassL = bassLPF2L
            val midHighL = leftIn - bassL

            bassLPF1R += bassAlpha * (rightIn - bassLPF1R) + DENORMAL_GUARD
            bassLPF2R += bassAlpha * (bassLPF1R - bassLPF2R)
            val bassR = bassLPF2R
            val midHighR = rightIn - bassR

            // 2) Mid/Side
            val midHighMid = (midHighL + midHighR) * 0.5f
            val midHighSide = (midHighL - midHighR) * 0.5f

            // 3) Orbit
            val angle = 2.0 * PI * EFFECT_FREQUENCY * time
            val x = (sin(angle) * intensitySm).coerceIn(-1.0, 1.0)
            val y = (cos(angle) * (0.9 + 0.1 * intensitySm)).coerceIn(-1.0, 1.0)

            // 4) Constant-power pan
            val panAngle = (x + 1.0) * (PI / 4.0)
            var spatialGainL = cos(panAngle).toFloat()
            var spatialGainR = sin(panAngle).toFloat()

            if (y < 0) {
                val rearDamp = 1.0f - (abs(y).toFloat() * 0.18f)
                spatialGainL *= rearDamp
                spatialGainR *= rearDamp
            }

            // 5) Fractional ITD delay with Hermite interpolation
            delayBufferL[delayIndex] = midHighMid
            delayBufferR[delayIndex] = midHighMid

            val delayL = if (x > 0) (x * maxDelaySamples).toFloat() else 0f
            val delayR = if (x < 0) (-x * maxDelaySamples).toFloat() else 0f

            val delayedSourceL = readHermite(delayBufferL, delayIndex, delayL)
            val delayedSourceR = readHermite(delayBufferR, delayIndex, delayR)

            delayIndex = (delayIndex + 1) % delayBufferL.size

            // 6) Frequency-selective head shadow
            val sideMuffleL = if (x > 0) (x * 0.65f).toFloat() else 0f
            val sideMuffleR = if (x < 0) (-x * 0.65f).toFloat() else 0f
            val rearMuffle = if (y < 0) (abs(y) * 0.35f).toFloat() else 0f

            val shelfAmountL = (sideMuffleL + rearMuffle).coerceIn(0f, 0.92f)
            val shelfAmountR = (sideMuffleR + rearMuffle).coerceIn(0f, 0.92f)

            shelfStateL += shelfAlpha * (delayedSourceL - shelfStateL) + DENORMAL_GUARD
            val highsL = delayedSourceL - shelfStateL
            val shadowedL = shelfStateL + highsL * (1.0f - shelfAmountL)

            shelfStateR += shelfAlpha * (delayedSourceR - shelfStateR) + DENORMAL_GUARD
            val highsR = delayedSourceR - shelfStateR
            val shadowedR = shelfStateR + highsR * (1.0f - shelfAmountR)

            var spatializedL = shadowedL * spatialGainL
            var spatializedR = shadowedR * spatialGainR

            // Preserve original width
            spatializedL += midHighSide * stereoPreserveSm
            spatializedR -= midHighSide * stereoPreserveSm

            // 7) Room
            val wetRoomL = roomBufferL[roomIdxL]
            val wetRoomR = roomBufferR[roomIdxR]
            val wetRoomL2 = roomBufferL2[roomIdxL2]

            dampStateL += ROOM_DAMP_COEFF * (wetRoomR - dampStateL)
            dampStateR += ROOM_DAMP_COEFF * (wetRoomL - dampStateR)
            dampStateL2 += ROOM_DAMP_COEFF * (wetRoomL2 - dampStateL2)

            roomBufferL[roomIdxL] = spatializedL + dampStateL * ROOM_FEEDBACK
            roomBufferR[roomIdxR] = spatializedR + dampStateR * ROOM_FEEDBACK
            roomBufferL2[roomIdxL2] =
                (spatializedL + spatializedR) * 0.5f + dampStateL2 * (ROOM_FEEDBACK * 0.6f)

            roomIdxL = (roomIdxL + 1) % roomBufferL.size
            roomIdxR = (roomIdxR + 1) % roomBufferR.size
            roomIdxL2 = (roomIdxL2 + 1) % roomBufferL2.size

            val roomSend = (0.35f * intensitySm).coerceIn(0f, 0.55f)
            val spatializedWithRoomL =
                spatializedL * (1f - roomSend) + (wetRoomL + wetRoomL2 * 0.5f) * roomSend
            val spatializedWithRoomR =
                spatializedR * (1f - roomSend) + (wetRoomR + wetRoomL2 * 0.5f) * roomSend

            // 8) Recombine bass with tiny glue pan
            val bassPanAngle = (x * 0.12 + 1.0) * (PI / 4.0)
            val processedL = spatializedWithRoomL + (bassL * cos(bassPanAngle).toFloat())
            val processedR = spatializedWithRoomR + (bassR * sin(bassPanAngle).toFloat())

            // 9) Wet/dry + bypass envelope
            val wetL = leftIn * (1f - wetDrySm) + processedL * wetDrySm
            val wetR = rightIn * (1f - wetDrySm) + processedR * wetDrySm

            // enable envelope crossfade (click-free)
            val mixedL = leftIn * (1f - enabledSm) + wetL * enabledSm
            val mixedR = rightIn * (1f - enabledSm) + wetR * enabledSm

            // 10) headroom, limit, DC block, PCM16
            val limitedL = softLimit(mixedL * 0.90f)
            val limitedR = softLimit(mixedR * 0.90f)

            val outL = dcBlock(limitedL, true)
            val outR = dcBlock(limitedR, false)

            samples[i] = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            samples[i + 1] = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        if (time >= lfoPeriod) time %= lfoPeriod

        // If fully disabled now, clear heavy buffers to avoid stale tails next enable.
        if (!enabled && enabledSm < 1e-4f) {
            clearSpatialStateButKeepDc()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** 1-pole smoothing coefficient from time in ms. */
    private fun onePoleCoeffMs(timeMs: Double, sampleRate: Double): Float {
        val t = max(0.1, timeMs) * 0.001
        return (1.0 - exp(-1.0 / (t * sampleRate))).toFloat()
    }

    /** 3rd-order Hermite interpolated read from circular delay buffer. */
    private fun readHermite(buffer: FloatArray, writeIndex: Int, delaySamples: Float): Float {
        val size = buffer.size
        val rp = writeIndex - delaySamples + size
        val i1 = floor(rp).toInt().mod(size)
        val frac = (rp - floor(rp)).toFloat()

        val i0 = (i1 - 1).mod(size)
        val i2 = (i1 + 1).mod(size)
        val i3 = (i1 + 2).mod(size)

        val y0 = buffer[i0]
        val y1 = buffer[i1]
        val y2 = buffer[i2]
        val y3 = buffer[i3]

        val c0 = y1
        val c1 = 0.5f * (y2 - y0)
        val c2 = y0 - 2.5f * y1 + 2f * y2 - 0.5f * y3
        val c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2)

        return ((c3 * frac + c2) * frac + c1) * frac + c0
    }

    /** Soft limiter (musical saturation near full-scale). */
    private fun softLimit(x: Float): Float {
        val a = abs(x)
        if (a <= 0.80f) return x
        // Smoothly bend into ceiling near 1.0 without hard clipping
        val y = 0.80f + (1f - exp(-(a - 0.80f) * 6.0f)) * 0.20f
        return sign(x) * y.coerceAtMost(0.9995f)
    }

    /** Simple DC blocker: y[n] = x[n] - x[n-1] + R*y[n-1] */
    private fun dcBlock(x: Float, left: Boolean): Float {
        val r = 0.995f
        return if (left) {
            val y = x - dcPrevInL + r * dcPrevOutL
            dcPrevInL = x
            dcPrevOutL = y
            y
        } else {
            val y = x - dcPrevInR + r * dcPrevOutR
            dcPrevInR = x
            dcPrevOutR = y
            y
        }
    }

    private fun clearSpatialStateButKeepDc() {
        time = 0.0
        delayBufferL.fill(0f)
        delayBufferR.fill(0f)
        roomBufferL.fill(0f)
        roomBufferR.fill(0f)
        roomBufferL2.fill(0f)

        delayIndex = 0
        roomIdxL = 0
        roomIdxR = 0
        roomIdxL2 = 0

        shelfStateL = 0f
        shelfStateR = 0f

        bassLPF1L = 0f
        bassLPF2L = 0f
        bassLPF1R = 0f
        bassLPF2R = 0f

        dampStateL = 0f
        dampStateR = 0f
        dampStateL2 = 0f
    }

    private fun Int.nextPowerOfTwo(): Int {
        var v = this
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return max(v, 1)
    }
}