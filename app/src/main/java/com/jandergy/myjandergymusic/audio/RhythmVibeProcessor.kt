package com.jandergy.myjandergymusic.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * One-Tap "Vibe Modes" real-time DSP processor:
 * - MODE_OFF: zero-CPU bypass
 * - MODE_SLOWED_REVERB: lush stereo Schroeder/Freeverb hall reverb
 * - MODE_OUTSIDE_CLUB: muffled 360 Hz low-pass + booming sub-bass + dark room echo
 * - MODE_LOFI_VINYL: warm band-pass + subtle tape wow/flutter + vinyl dust crackle
 * - MODE_NIGHTCORE: bright presence air shelf + tight glossy stereo ambience
 * - MODE_CONCERT_HALL: wide stadium/concert hall reflections
 */
@OptIn(UnstableApi::class)
class RhythmVibeProcessor : RhythmAudioProcessor() {

    companion object {
        const val MODE_OFF = 0
        const val MODE_SLOWED_REVERB = 1
        const val MODE_OUTSIDE_CLUB = 2
        const val MODE_LOFI_VINYL = 3
        const val MODE_NIGHTCORE = 4
        const val MODE_CONCERT_HALL = 5

        private val BASE_COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356)
        private val BASE_ALLPASS_TUNING = intArrayOf(556, 441)
        private const val STEREO_SPREAD = 23
    }

    @Volatile
    private var vibeMode: Int = MODE_OFF

    @Volatile
    private var activeWetTarget: Float = 0f

    private var wetSmooth: Float = 0f

    // Stereo Schroeder reverb banks (4 combs + 2 all-pass per channel)
    private var combsL: Array<CombFilter> = emptyArray()
    private var combsR: Array<CombFilter> = emptyArray()
    private var allpassL: Array<AllPassFilter> = emptyArray()
    private var allpassR: Array<AllPassFilter> = emptyArray()

    // Tone-shaping biquads per stereo channel
    private val clubLp1L = Biquad(); private val clubLp1R = Biquad()
    private val clubLp2L = Biquad(); private val clubLp2R = Biquad()
    private val clubSubL = Biquad(); private val clubSubR = Biquad()

    private val lofiHpL = Biquad(); private val lofiHpR = Biquad()
    private val lofiLpL = Biquad(); private val lofiLpR = Biquad()

    private val nightAirL = Biquad(); private val nightAirR = Biquad()

    // Subtle tape wow/flutter fractional delay for Lo-Fi Vinyl
    private var tapeDelayL = FloatArray(1024)
    private var tapeDelayR = FloatArray(1024)
    private var tapeWriteIdx = 0
    private var wowPhase = 0.0
    private var flutterPhase = 0.0
    private var rngState: Int = 0x517CC1B7

    fun setMode(mode: Int) {
        val clamped = mode.coerceIn(MODE_OFF, MODE_CONCERT_HALL)
        vibeMode = clamped
        activeWetTarget = if (clamped == MODE_OFF) 0f else 1f
    }

    fun getMode(): Int = vibeMode

    override fun isEnabled(): Boolean = vibeMode != MODE_OFF || wetSmooth > 0.002f

    override fun onAudioFormatConfigured(sampleRate: Int, channelCount: Int) {
        val sr = max(8000, sampleRate)
        val scale = sr / 44100.0

        combsL = Array(BASE_COMB_TUNING.size) { i ->
            CombFilter(max(64, (BASE_COMB_TUNING[i] * scale).toInt()))
        }
        combsR = Array(BASE_COMB_TUNING.size) { i ->
            CombFilter(max(64, ((BASE_COMB_TUNING[i] + STEREO_SPREAD) * scale).toInt()))
        }
        allpassL = Array(BASE_ALLPASS_TUNING.size) { i ->
            AllPassFilter(max(32, (BASE_ALLPASS_TUNING[i] * scale).toInt()))
        }
        allpassR = Array(BASE_ALLPASS_TUNING.size) { i ->
            AllPassFilter(max(32, ((BASE_ALLPASS_TUNING[i] + STEREO_SPREAD) * scale).toInt()))
        }

        // Outside the Club: 4th-order 360Hz muffled low-pass + booming +6dB 95Hz sub shelf
        clubLp1L.setLowPass(sr.toDouble(), 360.0, 0.707)
        clubLp1R.setLowPass(sr.toDouble(), 360.0, 0.707)
        clubLp2L.setLowPass(sr.toDouble(), 360.0, 0.707)
        clubLp2R.setLowPass(sr.toDouble(), 360.0, 0.707)
        clubSubL.setLowShelf(sr.toDouble(), 95.0, 6.0)
        clubSubR.setLowShelf(sr.toDouble(), 95.0, 6.0)

        // Lo-Fi Vinyl: warm 160 Hz - 4200 Hz band-pass
        lofiHpL.setHighPass(sr.toDouble(), 160.0, 0.707)
        lofiHpR.setHighPass(sr.toDouble(), 160.0, 0.707)
        lofiLpL.setLowPass(sr.toDouble(), 4200.0, 0.85)
        lofiLpR.setLowPass(sr.toDouble(), 4200.0, 0.85)

        // Nightcore: crisp +3.5dB high-shelf presence air
        nightAirL.setHighShelf(sr.toDouble(), 5200.0, 3.5)
        nightAirR.setHighShelf(sr.toDouble(), 5200.0, 3.5)

        val delaySize = max(512, (sr * 0.025).toInt())
        if (tapeDelayL.size != delaySize) {
            tapeDelayL = FloatArray(delaySize)
            tapeDelayR = FloatArray(delaySize)
        } else {
            tapeDelayL.fill(0f)
            tapeDelayR.fill(0f)
        }
        tapeWriteIdx = 0
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleCount < 2 || combsL.isEmpty()) return

        val mode = vibeMode
        val targetWet = activeWetTarget
        val smoothStep = 1.0f / max(1f, sampleRate * 0.025f)

        // Configure reverb parameters per vibe mode
        val feedback: Float
        val damp: Float
        val reverbMix: Float
        when (mode) {
            MODE_SLOWED_REVERB -> {
                feedback = 0.86f; damp = 0.34f; reverbMix = 0.36f
            }
            MODE_OUTSIDE_CLUB -> {
                feedback = 0.72f; damp = 0.72f; reverbMix = 0.28f
            }
            MODE_LOFI_VINYL -> {
                feedback = 0.58f; damp = 0.65f; reverbMix = 0.14f
            }
            MODE_NIGHTCORE -> {
                feedback = 0.64f; damp = 0.25f; reverbMix = 0.15f
            }
            MODE_CONCERT_HALL -> {
                feedback = 0.89f; damp = 0.26f; reverbMix = 0.40f
            }
            else -> {
                feedback = 0.75f; damp = 0.4f; reverbMix = 0.25f
            }
        }

        val sr = sampleRate.toDouble().coerceAtLeast(8000.0)
        val wowInc = 2.0 * PI * 0.45 / sr
        val flutterInc = 2.0 * PI * 5.2 / sr
        val delayLen = tapeDelayL.size
        val baseDelaySamples = (sr * 0.006).toFloat().coerceIn(8f, (delayLen - 16).toFloat())
        val wowDepth = (sr * 0.0014).toFloat()
        val flutterDepth = (sr * 0.00025).toFloat()

        val limit = sampleCount and -2
        var i = 0
        while (i < limit) {
            wetSmooth += (targetWet - wetSmooth) * smoothStep
            if (mode == MODE_OFF && wetSmooth < 0.001f) {
                wetSmooth = 0f
                i += 2
                continue
            }

            val dryL = samples[i] / 32768.0f
            val dryR = samples[i + 1] / 32768.0f

            var fxL = dryL
            var fxR = dryR

            when (mode) {
                MODE_SLOWED_REVERB, MODE_CONCERT_HALL -> {
                    val revIn = (dryL + dryR) * 0.015f
                    val revL = processReverbL(revIn, feedback, damp)
                    val revR = processReverbR(revIn, feedback, damp)
                    val dryGain = 1.0f - reverbMix * 0.35f
                    fxL = dryL * dryGain + revL * (reverbMix * 1.45f)
                    fxR = dryR * dryGain + revR * (reverbMix * 1.45f)
                }

                MODE_OUTSIDE_CLUB -> {
                    val muffledL = clubLp2L.process(clubLp1L.process(clubSubL.process(dryL)))
                    val muffledR = clubLp2R.process(clubLp1R.process(clubSubR.process(dryR)))
                    val revIn = (muffledL + muffledR) * 0.016f
                    val revL = processReverbL(revIn, feedback, damp)
                    val revR = processReverbR(revIn, feedback, damp)
                    fxL = (muffledL * 0.92f + revL * (reverbMix * 1.6f)) * 1.25f
                    fxR = (muffledR * 0.92f + revR * (reverbMix * 1.6f)) * 1.25f
                }

                MODE_LOFI_VINYL -> {
                    // Subtle tape wow & flutter via fractional delay line
                    tapeDelayL[tapeWriteIdx] = dryL
                    tapeDelayR[tapeWriteIdx] = dryR
                    val modDelay = baseDelaySamples +
                        sin(wowPhase).toFloat() * wowDepth +
                        sin(flutterPhase).toFloat() * flutterDepth
                    val readPos = tapeWriteIdx - modDelay
                    var idx0 = floor(readPos).toInt()
                    val frac = readPos - idx0
                    if (idx0 < 0) idx0 += delayLen
                    var idx1 = idx0 + 1
                    if (idx1 >= delayLen) idx1 -= delayLen

                    val warbleL = tapeDelayL[idx0] * (1f - frac) + tapeDelayL[idx1] * frac
                    val warbleR = tapeDelayR[idx0] * (1f - frac) + tapeDelayR[idx1] * frac
                    tapeWriteIdx++
                    if (tapeWriteIdx >= delayLen) tapeWriteIdx = 0

                    wowPhase += wowInc
                    if (wowPhase >= 2.0 * PI) wowPhase -= 2.0 * PI
                    flutterPhase += flutterInc
                    if (flutterPhase >= 2.0 * PI) flutterPhase -= 2.0 * PI

                    // Warm vintage band-pass + subtle tape saturation
                    val bandL = softSat(lofiLpL.process(lofiHpL.process(warbleL)) * 1.15f)
                    val bandR = softSat(lofiLpR.process(lofiHpR.process(warbleR)) * 1.15f)

                    // Subtle vinyl noise floor + occasional dust crackle
                    val crackle = nextVinylCrackle()
                    val revIn = (bandL + bandR) * 0.014f
                    val revL = processReverbL(revIn, feedback, damp)
                    val revR = processReverbR(revIn, feedback, damp)
                    fxL = bandL * 0.92f + revL * reverbMix + crackle
                    fxR = bandR * 0.92f + revR * reverbMix + crackle * 0.85f
                }

                MODE_NIGHTCORE -> {
                    val brightL = nightAirL.process(dryL)
                    val brightR = nightAirR.process(dryR)
                    val revIn = (brightL + brightR) * 0.015f
                    val revL = processReverbL(revIn, feedback, damp)
                    val revR = processReverbR(revIn, feedback, damp)
                    fxL = brightL * 0.95f + revL * (reverbMix * 1.25f)
                    fxR = brightR * 0.95f + revR * (reverbMix * 1.25f)
                }
            }

            val mix = wetSmooth.coerceIn(0f, 1f)
            val outL = dryL * (1f - mix) + fxL * mix
            val outR = dryR * (1f - mix) + fxR * mix

            samples[i] = floatToPcm16(outL)
            samples[i + 1] = floatToPcm16(outR)
            i += 2
        }
    }

    private fun processReverbL(input: Float, feedback: Float, damp: Float): Float {
        var acc = 0f
        for (c in combsL) acc += c.process(input, feedback, damp)
        var out = acc
        for (ap in allpassL) out = ap.process(out)
        return out
    }

    private fun processReverbR(input: Float, feedback: Float, damp: Float): Float {
        var acc = 0f
        for (c in combsR) acc += c.process(input, feedback, damp)
        var out = acc
        for (ap in allpassR) out = ap.process(out)
        return out
    }

    private fun nextVinylCrackle(): Float {
        var x = rngState
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        rngState = x
        val norm = (x and 0x7FFFFFFF) / 2147483647.0f
        val hiss = (norm - 0.5f) * 0.0045f
        val pop = if (norm > 0.99945f) (norm - 0.99945f) * 160.0f else 0f
        return hiss + pop
    }

    private fun softSat(x: Float): Float {
        val a = abs(x)
        return if (a <= 0.7f) x else sign(x) * (0.7f + 0.3f * ((a - 0.7f) / (0.3f + (a - 0.7f))))
    }

    private fun floatToPcm16(v: Float): Short {
        if (!v.isFinite()) return 0
        val a = abs(v)
        val limited = if (a <= 0.92f) {
            v
        } else {
            val over = a - 0.92f
            sign(v) * (0.92f + 0.08f * (over / (0.08f + over)))
        }
        return (limited * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
    }

    override fun flush() {
        super.flush()
        for (c in combsL) c.clear()
        for (c in combsR) c.clear()
        for (ap in allpassL) ap.clear()
        for (ap in allpassR) ap.clear()
        clubLp1L.clear(); clubLp1R.clear()
        clubLp2L.clear(); clubLp2R.clear()
        clubSubL.clear(); clubSubR.clear()
        lofiHpL.clear(); lofiHpR.clear()
        lofiLpL.clear(); lofiLpR.clear()
        nightAirL.clear(); nightAirR.clear()
        tapeDelayL.fill(0f)
        tapeDelayR.fill(0f)
        tapeWriteIdx = 0
    }

    private class CombFilter(size: Int) {
        private val buf = FloatArray(size)
        private var idx = 0
        private var filterStore = 0f

        fun process(input: Float, feedback: Float, damp: Float): Float {
            val out = buf[idx]
            filterStore = out * (1f - damp) + filterStore * damp
            buf[idx] = input + filterStore * feedback
            idx++
            if (idx >= buf.size) idx = 0
            return out
        }

        fun clear() {
            buf.fill(0f)
            idx = 0
            filterStore = 0f
        }
    }

    private class AllPassFilter(size: Int) {
        private val buf = FloatArray(size)
        private var idx = 0

        fun process(input: Float): Float {
            val bufOut = buf[idx]
            val out = -input + bufOut
            buf[idx] = input + bufOut * 0.5f
            idx++
            if (idx >= buf.size) idx = 0
            return out
        }

        fun clear() {
            buf.fill(0f)
            idx = 0
        }
    }

    private class Biquad {
        private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
        private var a1 = 0f; private var a2 = 0f
        private var z1 = 0f; private var z2 = 0f

        fun setLowPass(sr: Double, freq: Double, q: Double) {
            val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            b0 = (((1.0 - cosW0) * 0.5) / a0).toFloat()
            b1 = ((1.0 - cosW0) / a0).toFloat()
            b2 = (((1.0 - cosW0) * 0.5) / a0).toFloat()
            a1 = ((-2.0 * cosW0) / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun setHighPass(sr: Double, freq: Double, q: Double) {
            val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            b0 = (((1.0 + cosW0) * 0.5) / a0).toFloat()
            b1 = (-(1.0 + cosW0) / a0).toFloat()
            b2 = (((1.0 + cosW0) * 0.5) / a0).toFloat()
            a1 = ((-2.0 * cosW0) / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun setLowShelf(sr: Double, freq: Double, gainDb: Double) {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0 * kotlin.math.sqrt((a + 1.0 / a) * (1.0 / 0.9 - 1.0) + 2.0)
            val twoSqrtAAlpha = 2.0 * kotlin.math.sqrt(a) * alpha
            val a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
            b0 = ((a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)) / a0).toFloat()
            b1 = ((2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)) / a0).toFloat()
            b2 = ((a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)) / a0).toFloat()
            a1 = ((-2.0 * ((a - 1.0) + (a + 1.0) * cosW0)) / a0).toFloat()
            a2 = (((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0).toFloat()
        }

        fun setHighShelf(sr: Double, freq: Double, gainDb: Double) {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freq.coerceIn(20.0, sr * 0.45) / sr
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / 2.0 * kotlin.math.sqrt((a + 1.0 / a) * (1.0 / 0.9 - 1.0) + 2.0)
            val twoSqrtAAlpha = 2.0 * kotlin.math.sqrt(a) * alpha
            val a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
            b0 = ((a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)) / a0).toFloat()
            b1 = ((-2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)) / a0).toFloat()
            b2 = ((a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)) / a0).toFloat()
            a1 = ((2.0 * ((a - 1.0) - (a + 1.0) * cosW0)) / a0).toFloat()
            a2 = (((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0).toFloat()
        }

        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }

        fun clear() {
            z1 = 0f; z2 = 0f
        }
    }
}
