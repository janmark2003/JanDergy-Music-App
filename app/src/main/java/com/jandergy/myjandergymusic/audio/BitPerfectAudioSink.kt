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
 * time-stretch/varispeed engine and full Rhythm Studio DSP pipeline.
 */
@OptIn(UnstableApi::class)
object BitPerfectAudioSink {

    private const val TAG = "BitPerfectAudioSink"

    fun create(
        context: Context,
        enableBitPerfect: Boolean,
        tapeStopProcessor: RhythmTapeStopProcessor? = null,
        vocalProcessor: RhythmVocalProcessor? = null,
        vibeProcessor: RhythmVibeProcessor? = null,
        bassBoostProcessor: RhythmBassBoostProcessor? = null,
        spatializationProcessor: RhythmSpatializationProcessor? = null,
        eightDProcessor: Rhythm8DProcessor? = null,
        beatAnalyzerProcessor: RhythmBeatAnalyzerProcessor? = null
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
                tapeStopProcessor,
                vocalProcessor,
                vibeProcessor,
                bassBoostProcessor,
                spatializationProcessor,
                eightDProcessor,
                beatAnalyzerProcessor
            )
            val chain = TikTokAudioProcessorChain(effectProcessors)
            Log.d(TAG, "Configuring Studio DSP audio chain with ${chain.audioProcessors.size} processors")
            builder.setAudioProcessorChain(chain)
        }

        return builder.build()
    }

    /**
     * Custom AudioProcessorChain replacing ExoPlayer's SonicAudioProcessor with
     * [TikTokSpeedAudioProcessor], followed by TapeStop, Vocal, Vibe, Bass, Spatial, 8D,
     * and BeatAnalyzer so every stage operates on clean, continuous audio.
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
