package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI

/**
 * Rhythm Bass Boost Processor - Real-time bass enhancement
 * 
 * Uses a high-quality IIR low-pass filter with variable gain to enhance low frequencies.
 */
@OptIn(UnstableApi::class)
class RhythmBassBoostProcessor : RhythmAudioProcessor() {
    
    companion object {
        private const val TAG = "RhythmBassBoost"
        private const val BASS_CUTOFF_FREQ = 150.0 // Hz
    }
    
    private var strength: Short = 0
    private var enabled: Boolean = false
    
    private var prevSample = FloatArray(2) // Support stereo
    private var filterCoeff = 0f
    
    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "Bass boost enabled: $enable")
        this.enabled = enable
        if (!enable) {
            prevSample.fill(0f)
        }
    }

    fun setStrength(strength: Short) {
        this.strength = strength.coerceIn(0, 1000).toShort()
        updateFilterCoeff()
        Log.d(TAG, "Bass boost strength set to: ${this.strength}")
    }
    
    fun getStrength(): Short = strength
    
    override fun isEnabled(): Boolean = enabled
    
    private fun updateFilterCoeff() {
        if (sampleRate <= 0) return
        val rc = 1.0 / (2.0 * PI * BASS_CUTOFF_FREQ)
        val dt = 1.0 / sampleRate
        filterCoeff = (dt / (rc + dt)).toFloat()
    }

    private fun softLimitSample(sample: Float): Float {
        if (sample == 0f) return 0f
        val absValue = kotlin.math.abs(sample)
        
        val threshold = 0.8f
        if (absValue <= threshold) {
            return sample
        }
        
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        
        return kotlin.math.sign(sample) * limited
    }
    
    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (!enabled || strength == 0.toShort()) {
            return
        }
        
        if (filterCoeff == 0f) {
            updateFilterCoeff()
        }
        
        // Strength maps to 0..1000
        // We want a gain of 1.0 (0dB) to 4.0 (+12dB)
        val targetGain = 1.0f + (strength / 1000.0f) * 3.0f
        
        // Auto-gain compensation: reduce overall volume as bass increases
        // to prevent clipping and keep perceived loudness stable
        val outputGain = 1.0f / (1.0f + (strength / 1000.0f) * 0.5f)
        
        val isStereo = channelCount == 2
        
        for (i in 0 until sampleCount) {
            val channelIdx = if (isStereo) i % 2 else 0
            val input = samples[i] / 32768.0f
            
            // High-quality IIR Low-pass (Shelf filter component)
            val lowPass = prevSample[channelIdx] + filterCoeff * (input - prevSample[channelIdx])
            prevSample[channelIdx] = lowPass
            
            // Boost only the low frequencies
            val bassBoosted = input + (lowPass * (targetGain - 1.0f))
            
            // Apply compensation and soft limit
            val finalOutput = bassBoosted * outputGain
            
            samples[i] = (softLimitSample(finalOutput) * 32767.0f).toInt().toShort()
        }
    }
}
