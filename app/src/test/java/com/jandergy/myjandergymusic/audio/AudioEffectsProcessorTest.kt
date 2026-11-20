package com.jandergy.myjandergymusic.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class AudioEffectsProcessorTest {

    private fun runStereoProcessor(
        processor: AudioProcessor,
        stereoInput: ShortArray,
        sampleRate: Int = 48000,
        chunkFrames: Int = 512
    ): ShortArray {
        processor.configure(AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        val output = ShortArray(stereoInput.size)
        var outOffset = 0
        val totalFrames = stereoInput.size / 2
        var frameOffset = 0
        val inBuf = ByteBuffer.allocateDirect(chunkFrames * 4).order(ByteOrder.nativeOrder())

        while (frameOffset < totalFrames) {
            val count = minOf(chunkFrames, totalFrames - frameOffset)
            inBuf.clear()
            val sb = inBuf.asShortBuffer()
            for (i in 0 until count * 2) {
                sb.put(i, stereoInput[frameOffset * 2 + i])
            }
            inBuf.position(0)
            inBuf.limit(count * 4)
            processor.queueInput(inBuf)

            val outBuf = processor.getOutput()
            val outSb = outBuf.order(ByteOrder.nativeOrder()).asShortBuffer()
            while (outSb.hasRemaining() && outOffset < output.size) {
                output[outOffset++] = outSb.get()
            }
            frameOffset += count
        }
        return output
    }

    private fun rms(samples: ShortArray, startFrame: Int, endFrame: Int): Double {
        var sum = 0.0
        var count = 0
        for (f in startFrame until endFrame) {
            val l = samples[f * 2] / 32768.0
            val r = samples[f * 2 + 1] / 32768.0
            sum += (l * l + r * r) * 0.5
            count++
        }
        return if (count > 0) sqrt(sum / count) else 0.0
    }

    @Test
    fun vibeProcessor_allModes_produceCleanBoundedOutput() {
        val sr = 48000
        val frames = sr / 2
        val input = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 330.0 * i / sr) * 0.45 * 32767.0).toInt().toShort()
            input[i * 2] = s
            input[i * 2 + 1] = s
        }

        val modes = intArrayOf(
            RhythmVibeProcessor.MODE_SLOWED_REVERB,
            RhythmVibeProcessor.MODE_OUTSIDE_CLUB,
            RhythmVibeProcessor.MODE_LOFI_VINYL,
            RhythmVibeProcessor.MODE_NIGHTCORE,
            RhythmVibeProcessor.MODE_CONCERT_HALL
        )

        for (mode in modes) {
            val processor = RhythmVibeProcessor()
            processor.setMode(mode)
            assertTrue(processor.isEnabled())
            val out = runStereoProcessor(processor, input, sr)
            val energy = rms(out, frames / 4, frames)
            assertTrue("Mode $mode produced silent output", energy > 0.05)
            assertTrue("Mode $mode saturated abnormally: $energy", energy < 0.99)
        }
    }

    @Test
    fun vocalProcessor_instrumentalRemovesCenterVocalsButPreservesSubBass() {
        val sr = 48000
        val frames = sr / 2

        // 1) Centered 1000 Hz vocal-band tone should be heavily attenuated in Instrumental mode
        val centeredVocal = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 1000.0 * i / sr) * 0.6 * 32767.0).toInt().toShort()
            centeredVocal[i * 2] = s
            centeredVocal[i * 2 + 1] = s
        }

        val vocalProcessor = RhythmVocalProcessor()
        vocalProcessor.setMode(RhythmVocalProcessor.MODE_INSTRUMENTAL)
        val instVocalOut = runStereoProcessor(vocalProcessor, centeredVocal, sr)
        val inRms = rms(centeredVocal, frames / 4, frames)
        val outVocalRms = rms(instVocalOut, frames / 4, frames)
        assertTrue(
            "Centered 1kHz vocal was not removed enough: in=$inRms out=$outVocalRms",
            outVocalRms < inRms * 0.20
        )

        // 2) Centered 65 Hz sub-bass tone MUST stay strong (bass preservation!)
        val subBass = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 65.0 * i / sr) * 0.6 * 32767.0).toInt().toShort()
            subBass[i * 2] = s
            subBass[i * 2 + 1] = s
        }
        val vocalProcessor2 = RhythmVocalProcessor()
        vocalProcessor2.setMode(RhythmVocalProcessor.MODE_INSTRUMENTAL)
        val instBassOut = runStereoProcessor(vocalProcessor2, subBass, sr)
        val bassInRms = rms(subBass, frames / 4, frames)
        val bassOutRms = rms(instBassOut, frames / 4, frames)
        assertTrue(
            "65Hz sub-bass was lost in Instrumental mode: in=$bassInRms out=$bassOutRms",
            bassOutRms > bassInRms * 0.70
        )
    }

    @Test
    fun tapeStopProcessor_spinsDownToSilenceAndFiresCallback() {
        val sr = 48000
        val frames = sr // 1 second
        val input = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 440.0 * i / sr) * 0.5 * 32767.0).toInt().toShort()
            input[i * 2] = s
            input[i * 2 + 1] = s
        }

        val processor = RhythmTapeStopProcessor()
        processor.setFeatureEnabled(true)
        processor.configure(AudioProcessor.AudioFormat(sr, 2, C.ENCODING_PCM_16BIT))
        processor.flush()

        val callbackFired = AtomicBoolean(false)
        processor.triggerStop { callbackFired.set(true) }

        val out = ShortArray(input.size)
        var outOff = 0
        val chunk = 512
        val inBuf = ByteBuffer.allocateDirect(chunk * 4).order(ByteOrder.nativeOrder())
        var offset = 0
        while (offset < frames) {
            val n = minOf(chunk, frames - offset)
            inBuf.clear()
            val sb = inBuf.asShortBuffer()
            for (i in 0 until n * 2) sb.put(i, input[offset * 2 + i])
            inBuf.position(0)
            inBuf.limit(n * 4)
            processor.queueInput(inBuf)
            val ob = processor.getOutput().order(ByteOrder.nativeOrder()).asShortBuffer()
            while (ob.hasRemaining() && outOff < out.size) out[outOff++] = ob.get()
            offset += n
        }

        assertTrue("Tape-stop completion callback did not fire", callbackFired.get())
        // Tail after 0.5s should be silent
        val tailRms = rms(out, sr / 2, sr)
        assertEquals(0.0, tailRms, 1e-4)
    }

    @Test
    fun eightDProcessor_manualPositionPansLeftAndRightCleanly() {
        val sr = 48000
        val frames = sr / 2
        val input = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 440.0 * i / sr) * 0.5 * 32767.0).toInt().toShort()
            input[i * 2] = s
            input[i * 2 + 1] = s
        }

        val processor = Rhythm8DProcessor()
        processor.spatialMode = Rhythm8DProcessor.MODE_MANUAL
        processor.setManualPosition(1.0f, 0.0f) // Full Right
        processor.setEnabled(true)

        val out = runStereoProcessor(processor, input, sr)
        var leftSq = 0.0
        var rightSq = 0.0
        for (f in frames / 2 until frames) {
            val l = out[f * 2] / 32768.0
            val r = out[f * 2 + 1] / 32768.0
            leftSq += l * l
            rightSq += r * r
        }
        assertTrue("Manual right pan should make right louder than left", rightSq > leftSq * 3.0)
        assertTrue("AudioTelemetry spatialX should track right pan", AudioTelemetry.spatialX > 0.8f)
    }

    @Test
    fun beatAnalyzer_passesAudioUnmodifiedAndMeasuresBassEnergy() {
        val sr = 48000
        val frames = sr / 4
        val input = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val s = (sin(2.0 * PI * 60.0 * i / sr) * 0.7 * 32767.0).toInt().toShort()
            input[i * 2] = s
            input[i * 2 + 1] = s
        }

        val processor = RhythmBeatAnalyzerProcessor()
        processor.setEnabled(true)
        val out = runStereoProcessor(processor, input, sr)

        // Must be bit-exact pass-through
        for (i in input.indices) {
            assertEquals(input[i], out[i])
        }
        assertTrue("Bass energy should be detected on 60Hz tone", AudioTelemetry.bassEnergy > 0.1f)
        processor.setEnabled(false)
        assertFalse(processor.isEnabled())
    }
}
