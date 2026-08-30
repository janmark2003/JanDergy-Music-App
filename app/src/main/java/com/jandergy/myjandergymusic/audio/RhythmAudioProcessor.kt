package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Rhythm Audio Processor - High-performance base class for real-time audio effects.
 * 
 * Optimized with lock-free, zero-allocation buffers and direct zero-copy bypass
 * to ensure smooth playback at any playback speed without audio thread contention.
 */
@OptIn(UnstableApi::class)
@Suppress("OVERRIDE_DEPRECATION")
abstract class RhythmAudioProcessor : AudioProcessor {
    
    companion object {
        private const val TAG = "RhythmAudioProcessor"
    }
    
    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    // Per-instance dedicated buffers: no locks, no synchronized blocks, zero thread contention
    private var internalBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var shortBuffer: ShortArray = ShortArray(0)
    private var inputEnded = false

    // Audio format parameters
    protected var sampleRate: Int = 44100
    protected var channelCount: Int = 2
    protected var encoding: Int = C.ENCODING_PCM_16BIT
    
    /**
     * Process audio samples in-place using custom DSP algorithm
     * @param samples Array of audio samples (16-bit PCM)
     * @param sampleCount Number of valid samples in the array
     */
    abstract fun processSamples(samples: ShortArray, sampleCount: Int)
    
    /**
     * Check if the processor is enabled
     */
    abstract fun isEnabled(): Boolean
    
    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        Log.d(TAG, "configure() - sampleRate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}, encoding=${inputAudioFormat.encoding}")
        
        this.inputAudioFormat = inputAudioFormat
        this.sampleRate = inputAudioFormat.sampleRate
        this.channelCount = inputAudioFormat.channelCount
        this.encoding = inputAudioFormat.encoding
        this.outputAudioFormat = inputAudioFormat
        
        return outputAudioFormat
    }
    
    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET &&
            encoding == C.ENCODING_PCM_16BIT
    }

    private fun ensureCapacity(byteCount: Int) {
        if (internalBuffer.capacity() < byteCount) {
            internalBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        } else {
            internalBuffer.clear()
        }
    }
    
    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }

        // Direct zero-copy bypass if processor is inactive or disabled:
        // Avoids ShortArray conversions, memory allocations, and DSP math completely
        if (!isActive() || !isEnabled()) {
            ensureCapacity(remaining)
            internalBuffer.put(inputBuffer)
            internalBuffer.flip()
            outputBuffer = internalBuffer
            return
        }

        // Active processing:
        ensureCapacity(remaining)
        internalBuffer.put(inputBuffer)
        internalBuffer.flip()

        val sampleCount = remaining / 2
        if (shortBuffer.size < sampleCount) {
            shortBuffer = ShortArray(sampleCount)
        }

        internalBuffer.asShortBuffer().get(shortBuffer, 0, sampleCount)
        processSamples(shortBuffer, sampleCount)

        internalBuffer.position(0)
        internalBuffer.asShortBuffer().put(shortBuffer, 0, sampleCount)
        internalBuffer.position(0)
        internalBuffer.limit(remaining)
        outputBuffer = internalBuffer
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }
    
    override fun queueEndOfStream() {
        Log.d(TAG, "queueEndOfStream()")
        inputEnded = true
    }
    
    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }
    
    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }
    
    override fun reset() {
        flush()
        internalBuffer = AudioProcessor.EMPTY_BUFFER
        shortBuffer = ShortArray(0)
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    }
}
