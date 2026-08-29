package com.jandergy.myjandergymusic.audio

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

/**
 * Rhythm Spatialization Processor - Real-time 3D audio enhancement
 * 
 * Uses advanced mid-side (M/S) processing for stereo widening.
 */
@OptIn(UnstableApi::class)
class RhythmSpatializationProcessor : RhythmAudioProcessor() {
    
    companion object {
        private const val TAG = "RhythmSpatialization"
    }
    
    private var strength: Short = 0
    private var enabled: Boolean = false
    
    fun setEnabled(enable: Boolean) {
        Log.d(TAG, "Spatialization enabled: $enable")
        this.enabled = enable
    }
    
    fun setStrength(strength: Short) {
        this.strength = strength.coerceIn(0, 1000).toShort()
        Log.d(TAG, "Spatialization strength set to: ${this.strength}")
    }
    
    fun getStrength(): Short = strength
    
    override fun isEnabled(): Boolean = enabled
    
    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (!enabled || strength == 0.toShort() || channelCount != 2) {
            return
        }
        
        val width = when {
            strength == 0.toShort() -> 1.0f
            strength <= 500 -> 1.0f + (strength / 500.0f) * 0.8f
            else -> 1.8f + ((strength - 500) / 500.0f) * 1.2f
        }
        
        // Center preservation factor: 1.0 (no change) to 0.7 (reduce mid slightly to enhance space)
        val midGain = 1.0f - (strength / 1000.0f) * 0.15f
        
        for (i in 0 until sampleCount - 1 step 2) {
            val left = samples[i] / 32768.0f
            val right = samples[i + 1] / 32768.0f
            
            // M/S Decomposition
            val mid = (left + right) * 0.5f
            val side = (left - right) * 0.5f
            
            // Process Side for widening
            val wideSide = side * width
            
            // Process Mid for focus
            val focusedMid = mid * midGain
            
            // Reconstruct
            val newLeft = focusedMid + wideSide
            val newRight = focusedMid - wideSide
            
            samples[i] = (softClip(newLeft) * 32767.0f).toInt().toShort()
            samples[i + 1] = (softClip(newRight) * 32767.0f).toInt().toShort()
        }
    }
    
    private fun softClip(x: Float): Float {
        if (x == 0f) return 0f
        val absValue = kotlin.math.abs(x)
        val threshold = 0.8f
        if (absValue <= threshold) return x
        
        val over = absValue - threshold
        val maxOver = 1.0f - threshold
        val limited = threshold + over / (1.0f + over / maxOver)
        return kotlin.math.sign(x) * limited
    }
}
