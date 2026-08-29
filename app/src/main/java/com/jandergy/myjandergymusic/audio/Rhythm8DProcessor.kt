package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.*

/**
 * Realistic & Ultra-Smooth 8D Spatializer Engine (v4 - Binaural HRTF & Acoustic Orbit)
 *
 * Key Realistic Enhancements:
 *  - Woodworth Human Head ITD Model (~0.63 ms maximum interaural delay)
 *  - Organic Figure-8 / Lissajous Trajectory (smoother 360° rotation with elevation shifts)
 *  - Dual-Stage Binaural Pinna & Head Shadowing (true "behind head" acoustic absorption)
 *  - Early Reflection Tap Network + Diffuse Room Reverb for true 3D spatial space
 *  - Vocal & Sub-Bass Center Anchor (prevents vocals/kick from sounding hollow or dizzying)
 *  - Hermite 3rd-order fractional delay interpolation for zero pitch-click motion
 */
@OptIn(UnstableApi::class)
class Rhythm8DProcessor : RhythmAudioProcessor() {

    companion object {
        private const val TAG = "Rhythm8DProcessor"

        // Motion Parameters
        private const val PRIMARY_LFO_FREQ = 0.048          // Azimuth rotation speed (cycles per ~20.8s)
        private const val SECONDARY_LFO_FREQ = 0.096        // Elevation & depth oscillation
        private const val MAX_DELAY_MS = 0.63               // Realistic human head ITD (~0.63ms)

        // Crossovers & Acoustic Filters
        private const val SUB_BASS_CROSSOVER_HZ = 120.0     // Sub-bass stays omnidirectional
        private const val HEAD_SHADOW_FREQ_HZ = 2200.0      // Head absorption cutoff
        private const val PINNA_NOTCH_FREQ_HZ = 6500.0      // Pinna reflection absorption
        private const val DENORMAL_GUARD = 1e-20f

        // Room & Spatial Network
        private const val ROOM_DAMP_COEFF = 0.42f
        private const val ROOM_FEEDBACK = 0.38f

        // Smoothing Times
        private const val PARAM_SMOOTH_MS = 40.0            // Smooth parameter transition
        private const val BYPASS_FADE_MS = 25.0             // Click-free toggle fade
    }

    // Controls & State
    private var enabled: Boolean = false
    private var time: Double = 0.0

    var wetDryMix: Float = 1.0f
    var intensity: Float = 1.0f
    var stereoPreserve: Float = 0.35f
    var vocalAnchorStrength: Float = 0.25f                    // Keeps lead vocal centered & punchy

    // Smoothed runtime values
    private var wetDrySm = 1.0f
    private var intensitySm = 1.0f
    private var stereoPreserveSm = 0.35f
    private var vocalAnchorSm = 0.25f

    private var enabledTarget = 0f
    private var enabledSm = 0f

    // Delay Lines (ITD)
    private var delayBufferL = FloatArray(1024)
    private var delayBufferR = FloatArray(1024)
    private var delayIndex = 0
    private var maxDelaySamples = 0f

    // Head Shadow Filters (2-pole LP per ear)
    private var headShadowLP1L = 0f
    private var headShadowLP2L = 0f
    private var headShadowLP1R = 0f
    private var headShadowLP2R = 0f

    // Pinna Filters (1-pole LP for HF damping when behind)
    private var pinnaL = 0f
    private var pinnaR = 0f

    // Sub-Bass Crossover Filters (2-pole LP per channel)
    private var bassLP1L = 0f
    private var bassLP2L = 0f
    private var bassLP1R = 0f
    private var bassLP2R = 0f

    // Room Reverb Buffers (Diffused acoustic early reflections)
    private var roomBufferL = FloatArray(2053)
    private var roomBufferR = FloatArray(2609)
    private var roomBufferL2 = FloatArray(1427)
    private var roomIdxL = 0
    private var roomIdxR = 0
    private var roomIdxL2 = 0

    private var dampStateL = 0f
    private var dampStateR = 0f
    private var dampStateL2 = 0f

    // DC Blocker
    private var dcPrevInL = 0f
    private var dcPrevOutL = 0f
    private var dcPrevInR = 0f
    private var dcPrevOutR = 0f

    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "Immersive Realistic 8D Engine setEnabled: $enable")
        enabled = enable
        enabledTarget = if (enable) 1f else 0f
        if (enable && enabledSm <= 1e-4f) {
            time = 0.0
        }
    }

    override fun isEnabled(): Boolean = enabled || enabledSm > 1e-4f

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleRate <= 0 || sampleCount < 2) return

        val sr = sampleRate.toDouble()
        val dt = 1.0 / sr
        val frameLimit = if (sampleCount % 2 == 0) sampleCount else sampleCount - 1

        val neededDelay = ((MAX_DELAY_MS / 1000.0) * sr).toInt() + 8
        if (delayBufferL.size < neededDelay) {
            val newSize = max(neededDelay.nextPowerOfTwo(), 1024)
            delayBufferL = FloatArray(newSize)
            delayBufferR = FloatArray(newSize)
            delayIndex = 0
        }

        maxDelaySamples = ((MAX_DELAY_MS / 1000.0) * sr)
            .toFloat()
            .coerceAtMost((delayBufferL.size - 8).toFloat())

        val bassAlpha = (2.0 * PI * SUB_BASS_CROSSOVER_HZ / sr).coerceAtMost(1.0).toFloat()
        val headShadowAlpha = (2.0 * PI * HEAD_SHADOW_FREQ_HZ / sr).coerceAtMost(1.0).toFloat()
        val pinnaAlpha = (2.0 * PI * PINNA_NOTCH_FREQ_HZ / sr).coerceAtMost(1.0).toFloat()

        val paramSmoothA = onePoleCoeffMs(PARAM_SMOOTH_MS, sr)
        val bypassSmoothA = onePoleCoeffMs(BYPASS_FADE_MS, sr)
        val lfoPeriod = 1.0 / PRIMARY_LFO_FREQ

        for (i in 0 until frameLimit step 2) {
            val leftIn = samples[i] / 32768.0f
            val rightIn = samples[i + 1] / 32768.0f

            // Smooth controls
            wetDrySm += paramSmoothA * (wetDryMix.coerceIn(0f, 1f) - wetDrySm)
            intensitySm += paramSmoothA * (intensity.coerceIn(0f, 1.25f) - intensitySm)
            stereoPreserveSm += paramSmoothA * (stereoPreserve.coerceIn(0f, 1f) - stereoPreserveSm)
            vocalAnchorSm += paramSmoothA * (vocalAnchorStrength.coerceIn(0f, 1f) - vocalAnchorSm)
            enabledSm += bypassSmoothA * (enabledTarget - enabledSm)

            if (!enabled && enabledSm < 1e-5f) {
                val outL = dcBlock(leftIn, true)
                val outR = dcBlock(rightIn, false)
                samples[i] = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                samples[i + 1] = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                continue
            }

            time += dt

            // 1) Crossover Sub-Bass Filter (keeps sub-bass solid & punchy)
            bassLP1L += bassAlpha * (leftIn - bassLP1L) + DENORMAL_GUARD
            bassLP2L += bassAlpha * (bassLP1L - bassLP2L)
            val subBassL = bassLP2L
            val midHighL = leftIn - subBassL

            bassLP1R += bassAlpha * (rightIn - bassLP1R) + DENORMAL_GUARD
            bassLP2R += bassAlpha * (bassLP1R - bassLP2R)
            val subBassR = bassLP2R
            val midHighR = rightIn - subBassR

            // 2) Mid/Side Decomposition
            val midSignal = (midHighL + midHighR) * 0.5f
            val sideSignal = (midHighL - midHighR) * 0.5f

            // 3) Smooth Lissajous Orbit Trajectory
            val angleAzimuth = 2.0 * PI * PRIMARY_LFO_FREQ * time
            val angleElevation = 2.0 * PI * SECONDARY_LFO_FREQ * time

            // x = left (-1) to right (+1), y = front (+1) to back (-1)
            val x = (sin(angleAzimuth) * intensitySm).coerceIn(-1.0, 1.0)
            val y = (cos(angleAzimuth) * (0.85 + 0.15 * sin(angleElevation))).coerceIn(-1.0, 1.0)

            // 4) Binaural Interaural Time Difference (ITD - Woodworth Model)
            // ITD varies non-linearly with angle theta: ITD ~ (r/c) * (theta + sin(theta))
            val theta = x * (PI / 2.0)
            val itdFraction = (theta + sin(theta)) / (PI / 2.0 + 1.0)

            delayBufferL[delayIndex] = midSignal
            delayBufferR[delayIndex] = midSignal

            val delayL = if (itdFraction > 0) (itdFraction * maxDelaySamples).toFloat() else 0f
            val delayR = if (itdFraction < 0) (-itdFraction * maxDelaySamples).toFloat() else 0f

            val delayedL = readHermite(delayBufferL, delayIndex, delayL)
            val delayedR = readHermite(delayBufferR, delayIndex, delayR)

            delayIndex = (delayIndex + 1) % delayBufferL.size

            // 5) Binaural Pinna & Head Shadowing (ILD)
            // Opposite ear absorption:
            val shadowAmountL = if (x > 0) (x * 0.55).toFloat() else 0f
            val shadowAmountR = if (x < 0) (-x * 0.55).toFloat() else 0f

            // Rear acoustic damping when sound is behind listener:
            val rearDamping = if (y < 0) (abs(y) * 0.40).toFloat() else 0f

            val cutoffL = (headShadowAlpha * (1.0f - (shadowAmountL + rearDamping).coerceIn(0f, 0.85f))).coerceAtLeast(0.01f)
            val cutoffR = (headShadowAlpha * (1.0f - (shadowAmountR + rearDamping).coerceIn(0f, 0.85f))).coerceAtLeast(0.01f)

            headShadowLP1L += cutoffL * (delayedL - headShadowLP1L) + DENORMAL_GUARD
            headShadowLP2L += cutoffL * (headShadowLP1L - headShadowLP2L)
            val shadowedL = headShadowLP2L

            headShadowLP1R += cutoffR * (delayedR - headShadowLP1R) + DENORMAL_GUARD
            headShadowLP2R += cutoffR * (headShadowLP1R - headShadowLP2R)
            val shadowedR = headShadowLP2R

            // High frequency Pinna notch absorption
            pinnaL += pinnaAlpha * (shadowedL - pinnaL)
            pinnaR += pinnaAlpha * (shadowedR - pinnaR)
            val processedMidL = shadowedL * 0.7f + pinnaL * 0.3f
            val processedMidR = shadowedR * 0.7f + pinnaR * 0.3f

            // 6) Panning gains with constant power curve
            val panAngle = (x + 1.0) * (PI / 4.0)
            var panGainL = cos(panAngle).toFloat()
            var panGainR = sin(panAngle).toFloat()

            // Distance attenuation when sound is behind
            if (y < 0) {
                val distFactor = 1.0f - (abs(y).toFloat() * 0.15f)
                panGainL *= distFactor
                panGainR *= distFactor
            }

            var spatializedL = processedMidL * panGainL
            var spatializedR = processedMidR * panGainR

            // Preserve original stereo width & Vocal Center Anchor
            spatializedL += sideSignal * stereoPreserveSm
            spatializedR -= sideSignal * stereoPreserveSm
            
            // Add subtle vocal center anchor so main lead vocal stays intelligible
            val centerAnchor = midSignal * vocalAnchorSm * 0.30f
            spatializedL += centerAnchor
            spatializedR += centerAnchor

            // 7) 3D Room Acoustic Reflections
            val wetRoomL = roomBufferL[roomIdxL]
            val wetRoomR = roomBufferR[roomIdxR]
            val wetRoomL2 = roomBufferL2[roomIdxL2]

            dampStateL += ROOM_DAMP_COEFF * (wetRoomR - dampStateL)
            dampStateR += ROOM_DAMP_COEFF * (wetRoomL - dampStateR)
            dampStateL2 += ROOM_DAMP_COEFF * (wetRoomL2 - dampStateL2)

            roomBufferL[roomIdxL] = spatializedL + dampStateL * ROOM_FEEDBACK
            roomBufferR[roomIdxR] = spatializedR + dampStateR * ROOM_FEEDBACK
            roomBufferL2[roomIdxL2] = (spatializedL + spatializedR) * 0.5f + dampStateL2 * (ROOM_FEEDBACK * 0.5f)

            roomIdxL = (roomIdxL + 1) % roomBufferL.size
            roomIdxR = (roomIdxR + 1) % roomBufferR.size
            roomIdxL2 = (roomIdxL2 + 1) % roomBufferL2.size

            val roomSend = (0.28f * intensitySm).coerceIn(0f, 0.45f)
            val spatialized3DL = spatializedL * (1f - roomSend) + (wetRoomL + wetRoomL2 * 0.4f) * roomSend
            val spatialized3DR = spatializedR * (1f - roomSend) + (wetRoomR + wetRoomL2 * 0.4f) * roomSend

            // 8) Recombine Sub-Bass with subtle directional glue
            val bassPanAngle = (x * 0.10 + 1.0) * (PI / 4.0)
            val finalL = spatialized3DL + (subBassL * cos(bassPanAngle).toFloat())
            val finalR = spatialized3DR + (subBassR * sin(bassPanAngle).toFloat())

            // 9) Wet/Dry & Enable Crossfade
            val wetL = leftIn * (1f - wetDrySm) + finalL * wetDrySm
            val wetR = rightIn * (1f - wetDrySm) + finalR * wetDrySm

            val mixedL = leftIn * (1f - enabledSm) + wetL * enabledSm
            val mixedR = rightIn * (1f - enabledSm) + wetR * enabledSm

            // 10) Musical Soft Limiting & DC Blocker
            val limitedL = softLimit(mixedL * 0.92f)
            val limitedR = softLimit(mixedR * 0.92f)

            val outL = dcBlock(limitedL, true)
            val outR = dcBlock(limitedR, false)

            samples[i] = (outL * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
            samples[i + 1] = (outR * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        if (time >= lfoPeriod) time %= lfoPeriod

        if (!enabled && enabledSm < 1e-4f) {
            clearSpatialStateButKeepDc()
        }
    }

    private fun onePoleCoeffMs(timeMs: Double, sampleRate: Double): Float {
        val t = max(0.1, timeMs) * 0.001
        return (1.0 - exp(-1.0 / (t * sampleRate))).toFloat()
    }

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

    private fun softLimit(x: Float): Float {
        val a = abs(x)
        if (a <= 0.82f) return x
        val y = 0.82f + (1f - exp(-(a - 0.82f) * 5.5f)) * 0.18f
        return sign(x) * y.coerceAtMost(0.9995f)
    }

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

        headShadowLP1L = 0f
        headShadowLP2L = 0f
        headShadowLP1R = 0f
        headShadowLP2R = 0f

        pinnaL = 0f
        pinnaR = 0f

        bassLP1L = 0f
        bassLP2L = 0f
        bassLP1R = 0f
        bassLP2R = 0f

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