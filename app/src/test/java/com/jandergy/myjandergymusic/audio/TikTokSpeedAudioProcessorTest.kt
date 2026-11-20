package com.jandergy.myjandergymusic.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class TikTokSpeedAudioProcessorTest {

    private fun generateStereoSinePcm(sampleRate: Int, durationSec: Double, freqHz: Double): ShortArray {
        val frames = (sampleRate * durationSec).toInt()
        val out = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val sample = (sin(2.0 * PI * freqHz * i / sampleRate) * 0.6 * 32767.0).toInt().toShort()
            out[i * 2] = sample
            out[i * 2 + 1] = sample
        }
        return out
    }

    private fun runProcessor(
        input: ShortArray,
        sampleRate: Int,
        speed: Float,
        pitch: Float,
        chunkFrames: Int = 1024
    ): Pair<ShortArray, TikTokSpeedAudioProcessor> {
        val processor = TikTokSpeedAudioProcessor()
        processor.setSpeed(speed)
        processor.setPitch(pitch)
        val format = AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        assertTrue(processor.isActive())
        processor.flush()

        val collected = ArrayList<Short>(input.size)
        val totalFrames = input.size / 2
        var offsetFrame = 0
        val inBuf = ByteBuffer.allocateDirect(chunkFrames * 4).order(ByteOrder.nativeOrder())

        while (offsetFrame < totalFrames) {
            val framesThisChunk = minOf(chunkFrames, totalFrames - offsetFrame)
            inBuf.clear()
            val shortView = inBuf.asShortBuffer()
            for (i in 0 until framesThisChunk * 2) {
                shortView.put(i, input[offsetFrame * 2 + i])
            }
            inBuf.position(0)
            inBuf.limit(framesThisChunk * 4)

            processor.queueInput(inBuf)
            drainAll(processor, collected)
            offsetFrame += framesThisChunk
        }

        processor.queueEndOfStream()
        drainAll(processor, collected)
        assertTrue(processor.isEnded())

        val result = ShortArray(collected.size)
        for (i in collected.indices) {
            result[i] = collected[i]
        }
        return result to processor
    }

    private fun drainAll(processor: TikTokSpeedAudioProcessor, dst: MutableList<Short>) {
        while (true) {
            val out = processor.getOutput()
            if (!out.hasRemaining()) break
            val sb = out.order(ByteOrder.nativeOrder()).asShortBuffer()
            while (sb.hasRemaining()) {
                dst.add(sb.get())
            }
            out.position(out.limit())
        }
    }

    private fun estimateDominantFrequencyHz(stereoPcm: ShortArray, sampleRate: Int): Double {
        // Measure average zero-crossing frequency in the steady middle half of the left channel
        val totalFrames = stereoPcm.size / 2
        val start = totalFrames / 4
        val end = (totalFrames * 3) / 4
        var crossings = 0
        for (i in start + 1 until end) {
            val prev = stereoPcm[(i - 1) * 2].toInt()
            val curr = stereoPcm[i * 2].toInt()
            if (prev <= 0 && curr > 0) {
                crossings++
            }
        }
        val seconds = (end - start).toDouble() / sampleRate.toDouble()
        return crossings / seconds
    }

    private fun maxSampleJump(stereoPcm: ShortArray): Int {
        val totalFrames = stereoPcm.size / 2
        val start = totalFrames / 8
        val end = (totalFrames * 7) / 8
        var maxJump = 0
        for (i in start + 1 until end) {
            val jumpL = abs(stereoPcm[i * 2].toInt() - stereoPcm[(i - 1) * 2].toInt())
            val jumpR = abs(stereoPcm[i * 2 + 1].toInt() - stereoPcm[(i - 1) * 2 + 1].toInt())
            if (jumpL > maxJump) maxJump = jumpL
            if (jumpR > maxJump) maxJump = jumpR
        }
        return maxJump
    }

    @Test
    fun keepPitch_speedUpAndSlowDown_preservesPitchAndDurationCleanly() {
        val sampleRate = 48000
        val input = generateStereoSinePcm(sampleRate, durationSec = 1.5, freqHz = 440.0)
        val inputFrames = input.size / 2

        for (speed in floatArrayOf(0.5f, 0.75f, 1.25f, 1.5f, 2.0f)) {
            val (output, processor) = runProcessor(input, sampleRate, speed = speed, pitch = 1.0f)
            val outputFrames = output.size / 2
            val expectedFrames = inputFrames / speed

            // Output duration matches 1 / speed accurately
            val ratioError = abs(outputFrames - expectedFrames) / expectedFrames
            assertTrue("Duration error too large at ${speed}x: $ratioError", ratioError < 0.08)

            // Pitch stays locked at ~440 Hz
            val detectedFreq = estimateDominantFrequencyHz(output, sampleRate)
            assertEquals("Pitch drifted at ${speed}x", 440.0, detectedFreq, 20.0)

            // No hard clicks or discontinuities (max step of a 440Hz 0.6-amp sine at 48kHz is ~1130; allow comfortable headroom)
            val jump = maxSampleJump(output)
            assertTrue("Click/choppiness detected at ${speed}x: maxJump=$jump", jump < 4000)

            // Stereo lock: left and right remain identical for identical stereo input
            for (i in 0 until outputFrames) {
                assertEquals(output[i * 2], output[i * 2 + 1])
            }

            // Media duration scaling tracks playback speed accurately
            val mediaUs = processor.getMediaDuration(1_000_000L)
            assertEquals((1_000_000.0 * speed).toLong().toDouble(), mediaUs.toDouble(), 120_000.0)
        }
    }

    @Test
    fun varispeedMode_tiktokSpedUpAndSlowed_shiftsPitchAndIsArtifactFree() {
        val sampleRate = 48000
        val input = generateStereoSinePcm(sampleRate, durationSec = 1.5, freqHz = 440.0)
        val inputFrames = input.size / 2

        for (speed in floatArrayOf(0.5f, 0.75f, 1.25f, 1.5f, 2.0f)) {
            val (output, _) = runProcessor(input, sampleRate, speed = speed, pitch = speed)
            val outputFrames = output.size / 2
            val expectedFrames = inputFrames / speed

            val ratioError = abs(outputFrames - expectedFrames) / expectedFrames
            assertTrue("Varispeed duration error at ${speed}x: $ratioError", ratioError < 0.03)

            // Pitch tracks speed accurately (e.g. 440 * 1.5 = 660Hz, 440 * 0.5 = 220Hz)
            val detectedFreq = estimateDominantFrequencyHz(output, sampleRate)
            assertEquals("Varispeed pitch mismatch at ${speed}x", 440.0 * speed, detectedFreq, 15.0)

            // Ultra-smooth waveform with zero clicks
            val jump = maxSampleJump(output)
            assertTrue("Varispeed jump too large at ${speed}x: maxJump=$jump", jump < 3500)
        }
    }

    @Test
    fun unitSpeedAndPitch_isInactiveBypass() {
        val processor = TikTokSpeedAudioProcessor()
        processor.setSpeed(1.0f)
        processor.setPitch(1.0f)
        processor.configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
        assertFalse(processor.isActive())
    }
}
