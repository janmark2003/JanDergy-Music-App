package com.jandergy.myjandergymusic.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign

/**
 * Vinyl "Tape-Stop" Pause & Spin-Up Play DSP processor.
 *
 * When [triggerStop] is invoked, smoothly decelerates playback pitch down to a halt
 * over ~340 ms using cubic interpolation over a ring buffer, then invokes the completion
 * callback so PlaybackService pauses cleanly.
 * When [triggerSpinUp] is invoked on resume, ramps speed smoothly up from ~0.42x to 1.0x
 * over ~200 ms for a tactile turntable startup feel.
 */
@OptIn(UnstableApi::class)
class RhythmTapeStopProcessor : RhythmAudioProcessor() {

    companion object {
        private const val STATE_IDLE = 0
        private const val STATE_STOPPING = 1
        private const val STATE_STOPPED = 2
        private const val STATE_SPINNING_UP = 3

        private const val STOP_DURATION_SEC = 0.34
        private const val SPIN_UP_DURATION_SEC = 0.22
    }

    @Volatile
    private var state: Int = STATE_IDLE

    @Volatile
    private var stopCallback: Runnable? = null

    private var ringCapacity = 32768
    private var ringL = FloatArray(ringCapacity)
    private var ringR = FloatArray(ringCapacity)
    private var writePos = 0L
    private var readPos = 0.0
    private var progressFrames = 0
    private var durationFrames = 16384

    fun triggerStop(onStopped: Runnable?) {
        stopCallback = onStopped
        durationFrames = max(2048, (sampleRate * STOP_DURATION_SEC).toInt())
        progressFrames = 0
        readPos = max(0.0, (writePos - 4).toDouble())
        state = STATE_STOPPING
    }

    fun triggerSpinUp() {
        stopCallback = null
        durationFrames = max(1024, (sampleRate * SPIN_UP_DURATION_SEC).toInt())
        progressFrames = 0
        readPos = max(0.0, (writePos - 4).toDouble())
        state = STATE_SPINNING_UP
    }

    fun cancelEffect() {
        stopCallback = null
        state = STATE_IDLE
    }

    fun isStopping(): Boolean = state == STATE_STOPPING || state == STATE_STOPPED

    // Keep ring primed when feature is active, or bypass when idle if not needed
    @Volatile
    private var featureEnabled: Boolean = false

    fun setFeatureEnabled(enabled: Boolean) {
        featureEnabled = enabled
        if (!enabled) {
            cancelEffect()
        }
    }

    override fun isEnabled(): Boolean = featureEnabled || state != STATE_IDLE

    override fun onAudioFormatConfigured(sampleRate: Int, channelCount: Int) {
        val needed = max(16384, sampleRate)
        // Next power of 2 for bitmask wrapping
        var pow2 = 16384
        while (pow2 < needed) pow2 = pow2 shl 1
        if (pow2 != ringCapacity) {
            ringCapacity = pow2
            ringL = FloatArray(ringCapacity)
            ringR = FloatArray(ringCapacity)
        } else {
            ringL.fill(0f)
            ringR.fill(0f)
        }
        writePos = 0L
        readPos = 0.0
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (channelCount != 2 || sampleCount < 2) return
        val mask = ringCapacity - 1
        val limit = sampleCount and -2
        val currentState = state

        if (currentState == STATE_IDLE) {
            // Silently record into ring buffer so a sudden Tape-Stop has valid history
            var i = 0
            while (i < limit) {
                val wIdx = (writePos and mask.toLong()).toInt()
                ringL[wIdx] = samples[i] / 32768.0f
                ringR[wIdx] = samples[i + 1] / 32768.0f
                writePos++
                i += 2
            }
            return
        }

        if (currentState == STATE_STOPPED) {
            for (i in 0 until limit) {
                samples[i] = 0
            }
            return
        }

        if (currentState == STATE_STOPPING) {
            var i = 0
            while (i < limit) {
                val wIdx = (writePos and mask.toLong()).toInt()
                ringL[wIdx] = samples[i] / 32768.0f
                ringR[wIdx] = samples[i + 1] / 32768.0f
                writePos++

                val t = (progressFrames.toDouble() / durationFrames.toDouble()).coerceIn(0.0, 1.0)
                // Quadratic vinyl brake curve: 1.0 -> 0.03
                val rate = (1.0 - t) * (1.0 - t) * 0.97 + 0.03
                // Smooth fade-out over the final 25% of the spin-down
                val gain = if (t < 0.75) 1.0f else ((1.0 - t) / 0.25).toFloat().coerceIn(0f, 1f)

                val outL = readCubic(ringL, readPos, mask) * gain
                val outR = readCubic(ringR, readPos, mask) * gain
                samples[i] = floatToPcm16(outL)
                samples[i + 1] = floatToPcm16(outR)

                readPos += rate
                if (readPos > writePos - 3) {
                    readPos = (writePos - 3).toDouble()
                }
                progressFrames++

                if (progressFrames >= durationFrames) {
                    state = STATE_STOPPED
                    val cb = stopCallback
                    stopCallback = null
                    cb?.run()
                    var z = i + 2
                    while (z < limit) {
                        samples[z] = 0
                        z++
                    }
                    return
                }
                i += 2
            }
            return
        }

        if (currentState == STATE_SPINNING_UP) {
            var i = 0
            while (i < limit) {
                val wIdx = (writePos and mask.toLong()).toInt()
                ringL[wIdx] = samples[i] / 32768.0f
                ringR[wIdx] = samples[i + 1] / 32768.0f
                writePos++

                val t = (progressFrames.toDouble() / durationFrames.toDouble()).coerceIn(0.0, 1.0)
                // Smooth ease-out vinyl motor spin-up: 0.45 -> 1.0
                val eased = 1.0 - (1.0 - t) * (1.0 - t)
                val rate = 0.45 + 0.55 * eased
                val fadeIn = (t * 8.0).toFloat().coerceIn(0f, 1f)

                val outL = readCubic(ringL, readPos, mask) * fadeIn
                val outR = readCubic(ringR, readPos, mask) * fadeIn
                samples[i] = floatToPcm16(outL)
                samples[i + 1] = floatToPcm16(outR)

                readPos += rate
                if (readPos > writePos - 3) {
                    readPos = (writePos - 3).toDouble()
                }
                progressFrames++

                if (progressFrames >= durationFrames) {
                    state = STATE_IDLE
                    return
                }
                i += 2
            }
        }
    }

    private fun readCubic(ring: FloatArray, pos: Double, mask: Int): Float {
        val base = floor(pos).toLong()
        val frac = (pos - base).toFloat()
        val y0 = ring[((base - 1) and mask.toLong()).toInt()]
        val y1 = ring[(base and mask.toLong()).toInt()]
        val y2 = ring[((base + 1) and mask.toLong()).toInt()]
        val y3 = ring[((base + 2) and mask.toLong()).toInt()]

        val c0 = y1
        val c1 = 0.5f * (y2 - y0)
        val c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3
        val c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2)
        return ((c3 * frac + c2) * frac + c1) * frac + c0
    }

    private fun floatToPcm16(v: Float): Short {
        if (!v.isFinite()) return 0
        val a = abs(v)
        val limited = if (a <= 0.95f) v else sign(v) * (0.95f + 0.05f * ((a - 0.95f) / (0.05f + (a - 0.95f))))
        return (limited * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
    }

    override fun flush() {
        super.flush()
        ringL.fill(0f)
        ringR.fill(0f)
        writePos = 0L
        readPos = 0.0
        if (state == STATE_STOPPED || state == STATE_STOPPING) {
            state = STATE_IDLE
            stopCallback = null
        }
    }

    override fun reset() {
        super.reset()
        cancelEffect()
        ringL.fill(0f)
        ringR.fill(0f)
        writePos = 0L
        readPos = 0.0
    }
}
