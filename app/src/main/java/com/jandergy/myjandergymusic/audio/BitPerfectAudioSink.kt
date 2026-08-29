package com.jandergy.myjandergymusic.audio

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Factory for creating AudioSink instances configured with Rhythm audio effects.
 */
@OptIn(UnstableApi::class)
object BitPerfectAudioSink {
    
    private const val TAG = "BitPerfectAudioSink"
    
    fun create(
        context: Context, 
        enableBitPerfect: Boolean,
        bassBoostProcessor: RhythmBassBoostProcessor? = null,
        spatializationProcessor: RhythmSpatializationProcessor? = null,
        eightDProcessor: Rhythm8DProcessor? = null
    ): AudioSink {
        Log.d(TAG, "Creating AudioSink (bit-perfect: $enableBitPerfect)")
        
        val builder = DefaultAudioSink.Builder(context)
        
        if (enableBitPerfect) {
            builder.setEnableFloatOutput(true)
            Log.d(TAG, "Bit-perfect mode: float output enabled, no audio processors")
        } else {
            builder.setEnableFloatOutput(false)
            
            val processors = listOfNotNull(bassBoostProcessor, spatializationProcessor, eightDProcessor)
            
            if (processors.isNotEmpty()) {
                Log.d(TAG, "Configuring audio processor chain with ${processors.size} processors")
                builder.setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        *processors.toTypedArray()
                    )
                )
            }
        }
        
        return builder.build()
    }
}
