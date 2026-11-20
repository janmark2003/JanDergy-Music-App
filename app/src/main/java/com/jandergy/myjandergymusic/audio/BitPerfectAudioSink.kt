package com.jandergy.myjandergymusic.audio

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor

/**
 * Factory for creating AudioSink instances configured with our TikTok-grade
 * time-stretch/varispeed engine and Rhythm audio DSP effects.
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
            .setEnableAudioTrackPlaybackParams(false)

        if (enableBitPerfect) {
            builder.setEnableFloatOutput(true)
            Log.d(TAG, "Bit-perfect mode: float output enabled, no audio processors")
        } else {
            builder.setEnableFloatOutput(false)

            val effectProcessors = listOfNotNull(
                bassBoostProcessor,
                spatializationProcessor,
                eightDProcessor
            )
            val chain = TikTokAudioProcessorChain(effectProcessors)
            Log.d(TAG, "Configuring TikTok-grade audio processor chain with ${chain.audioProcessors.size} processors")
            builder.setAudioProcessorChain(chain)
        }

        return builder.build()
    }

    /**
     * Custom AudioProcessorChain that replaces ExoPlayer's choppy SonicAudioProcessor
     * with [TikTokSpeedAudioProcessor], running BEFORE BassBoost/Spatializer/8D so
     * time-stretching operates on clean source PCM and downstream DSP filters never glitch.
     */
    private class TikTokAudioProcessorChain(
        effectProcessors: List<AudioProcessor>
    ) : AudioProcessorChain {

        private val silenceSkippingProcessor = SilenceSkippingAudioProcessor()
        private val speedProcessor = TikTokSpeedAudioProcessor()
        private val allProcessors: Array<AudioProcessor>

        init {
            val ordered = ArrayList<AudioProcessor>(effectProcessors.size + 2)
            ordered.add(silenceSkippingProcessor)
            ordered.add(speedProcessor)
            ordered.addAll(effectProcessors)
            allProcessors = ordered.toTypedArray()
        }

        override fun getAudioProcessors(): Array<AudioProcessor> = allProcessors

        override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
            val clampedSpeed = playbackParameters.speed.coerceIn(0.25f, 4.0f)
            val clampedPitch = playbackParameters.pitch.coerceIn(0.25f, 4.0f)
            speedProcessor.setSpeed(clampedSpeed)
            speedProcessor.setPitch(clampedPitch)
            return PlaybackParameters(clampedSpeed, clampedPitch)
        }

        override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean {
            silenceSkippingProcessor.setEnabled(skipSilenceEnabled)
            return skipSilenceEnabled
        }

        override fun getMediaDuration(playoutDuration: Long): Long {
            return if (speedProcessor.isActive()) {
                speedProcessor.getMediaDuration(playoutDuration)
            } else {
                playoutDuration
            }
        }

        override fun getSkippedOutputFrameCount(): Long {
            return silenceSkippingProcessor.skippedFrames
        }
    }
}
