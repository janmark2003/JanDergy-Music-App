package com.jandergy.myjandergymusic.audio

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.util.ArrayList

/**
 * Custom RenderersFactory that injects our custom AudioSink with Rhythm processors.
 */
@OptIn(UnstableApi::class)
class BitPerfectRenderersFactory(
    context: Context,
    private val enableBitPerfect: Boolean = false,
    private val bassBoostProcessor: RhythmBassBoostProcessor? = null,
    private val spatializationProcessor: RhythmSpatializationProcessor? = null,
) : DefaultRenderersFactory(context) {
    
    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        val customAudioSink = BitPerfectAudioSink.create(
            context, 
            enableBitPerfect,
            bassBoostProcessor,
            spatializationProcessor
        )
        
        super.buildAudioRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            customAudioSink,
            eventHandler,
            eventListener,
            out
        )
    }
}
