package com.jandergy.myjandergymusic.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

/**
 * Real-time Karaoke / Instrumental (Vocal Remover with Sub-Bass Preservation)
 * and Vocal Focus (Acapella highlight) processor.
 */
@OptIn(UnstableApi::class)
class RhythmVocalProcessor : RhythmAudioProcessor() {

    companion object {
        const val MODE_OFF = 0
        const val MODE_INSTRUMENTAL = 1
        const val MODE_VOCAL_FOCUS = 2
    }

    @Volatile
    private var vocalMode: Int = MODE_OFF

    // 150 Hz low-pass crossover preserves punchy mono kick/sub-bass in Karaoke mode
    private var b0Lp = 0f; private var b1Lp = 0f; private var b2Lp = 0f
    private var a1Lp = 0f; private var a2Lp = 0f
    private var z1LpL = 0f; private var z2LpL = 0f
    private var z1LpR = 0f; private var z2LpR = 0f

    // Vocal Focus band-pass (180 Hz HP + 6500 Hz LP) on centered mid signal
    private var b0Hp = 1f; private var b1Hp = 0f; private var b2Hp = 0f
    private var a1Hp = 0f; private var a2Hp = 0f
    private var z1HpM = 0f; private var z2HpM = 0f

    private var b0Vlp = 1f; private var b1Vlp = 0f; private var b2Vlp = 0f
    private var a1Vlp = 0f; private var a2Vlp = 0f
    private var z1VlpM = 0f; private var z2VlpM = 0f

    fun setMode(mode: Int) {
        vocalMode = mode.coerceIn(MODE_OFF, MODE_VOCAL_FOCUS)
    }

    fun getMode(): Int = vocalMode

    override fun isEnabled(): Boolean = vocalMode != MODE_OFF

    override fun onAudioFormatConfigured(sampleRate: Int, channelCount: Int) {
        val sr = max(8000, sampleRate).toDouble()
        computeLowPass(sr, 150.0, 0.707) { b0, b1, b2, a1, a2 ->
            b0Lp = b0; b1Lp = b1; b2Lp = b2; a1Lp = a1; a2Lp = a2
        }
        computeHighPass(sr, 180.0, 0.707) { b0, b1, b2, a1, a2 ->
            b0Hp = b0; b1Hp = b1; b2Hp = b2; a1Hp = a1; a2Hp = a2
        }
        computeLowPass(sr, 6500.0, 0.707) { b0, b1, b2, a1, a2 ->
            b0Vlp = b0; b1Vlp = b1; b2Vlp = b2; a1Vlp = a1; a2Vlp = a2
        }
        clearState()
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        val mode = vocalMode
        if (mode == MODE_OFF || channelCount != 2 || sampleCount < 2) return

        val limit = sampleCount and -2
        var i = 0
        if (mode == MODE_INSTRUMENTAL) {
            while (i < limit) {
                val l = samples[i] / 32768.0f
                val r = samples[i + 1] / 32768.0f

                // Preserve low-end punch (< 150 Hz) untouched so kick & bass never disappear
                val bassL = b0Lp * l + z1LpL
                z1LpL = b1Lp * l - a1Lp * bassL + z2LpL
                z2LpL = b2Lp * l - a2Lp * bassL

                val bassR = b0Lp * r + z1LpR
                z1LpR = b1Lp * r - a1Lp * bassR + z2LpR
                z2LpR = b2Lp * r - a2Lp * bassR

                // Cancel center-panned vocals above 150 Hz via Mid/Side decomposition
                val highL = l - bassL
                val highR = r - bassR
                val highSide = (highL - highR) * 0.5f
                val highMidLeak = (highL + highR) * 0.5f * 0.06f

                val outL = bassL + highSide * 1.18f + highMidLeak
                val outR = bassR - highSide * 1.18f + highMidLeak

                samples[i] = floatToPcm16(outL)
                samples[i + 1] = floatToPcm16(outR)
                i += 2
            }
        } else if (mode == MODE_VOCAL_FOCUS) {
            while (i < limit) {
                val l = samples[i] / 32768.0f
                val r = samples[i + 1] / 32768.0f

                val mid = (l + r) * 0.5f
                val side = (l - r) * 0.5f

                // Isolate vocal frequency band on center channel
                val hp = b0Hp * mid + z1HpM
                z1HpM = b1Hp * mid - a1Hp * hp + z2HpM
                z2HpM = b2Hp * mid - a2Hp * hp

                val vocalMid = b0Vlp * hp + z1VlpM
                z1VlpM = b1Vlp * hp - a1Vlp * vocalMid + z2VlpM
                z2VlpM = b2Vlp * hp - a2Vlp * vocalMid

                val outL = vocalMid * 1.25f + side * 0.14f
                val outR = vocalMid * 1.25f - side * 0.14f

                samples[i] = floatToPcm16(outL)
                samples[i + 1] = floatToPcm16(outR)
                i += 2
            }
        }
    }

    private fun computeLowPass(
        sr: Double,
        freq: Double,
        q: Double,
        apply: (Float, Float, Float, Float, Float) -> Unit
    ) {
        val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        apply(
            (((1.0 - cosW0) * 0.5) / a0).toFloat(),
            ((1.0 - cosW0) / a0).toFloat(),
            (((1.0 - cosW0) * 0.5) / a0).toFloat(),
            ((-2.0 * cosW0) / a0).toFloat(),
            ((1.0 - alpha) / a0).toFloat()
        )
    }

    private fun computeHighPass(
        sr: Double,
        freq: Double,
        q: Double,
        apply: (Float, Float, Float, Float, Float) -> Unit
    ) {
        val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        apply(
            (((1.0 + cosW0) * 0.5) / a0).toFloat(),
            (-(1.0 + cosW0) / a0).toFloat(),
            (((1.0 + cosW0) * 0.5) / a0).toFloat(),
            ((-2.0 * cosW0) / a0).toFloat(),
            ((1.0 - alpha) / a0).toFloat()
        )
    }

    private fun floatToPcm16(v: Float): Short {
        if (!v.isFinite()) return 0
        val a = abs(v)
        val limited = if (a <= 0.94f) {
            v
        } else {
            val over = a - 0.94f
            sign(v) * (0.94f + 0.06f * (over / (0.06f + over)))
        }
        return (limited * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
    }

    private fun clearState() {
        z1LpL = 0f; z2LpL = 0f
        z1LpR = 0f; z2LpR = 0f
        z1HpM = 0f; z2HpM = 0f
        z1VlpM = 0f; z2VlpM = 0f
    }

    override fun flush() {
        super.flush()
        clearState()
    }

    override fun reset() {
        super.reset()
        clearState()
    }
}
