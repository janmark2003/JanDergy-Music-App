package com.jandergy.myjandergymusic.audio

import android.content.Context
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.util.ArrayList

/**
 * Custom RenderersFactory that injects our Studio AudioSink with TikTok Speed,
 * Tape-Stop, Vocal/Karaoke, Vibe/Reverb, BassBoost, Spatializer, 8D, and Beat Analyzer.
 */
@OptIn(UnstableApi::class)
class BitPerfectRenderersFactory(
    context: Context,
    private val enableBitPerfect: Boolean = false,
    private val tapeStopProcessor: RhythmTapeStopProcessor? = null,
    private val vocalProcessor: RhythmVocalProcessor? = null,
    private val vibeProcessor: RhythmVibeProcessor? = null,
    private val bassBoostProcessor: RhythmBassBoostProcessor? = null,
    private val spatializationProcessor: RhythmSpatializationProcessor? = null,
    private val eightDProcessor: Rhythm8DProcessor? = null,
    private val beatAnalyzerProcessor: RhythmBeatAnalyzerProcessor? = null
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
            tapeStopProcessor,
            vocalProcessor,
            vibeProcessor,
            bassBoostProcessor,
            spatializationProcessor,
            eightDProcessor,
            beatAnalyzerProcessor
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
