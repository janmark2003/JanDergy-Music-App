package com.jandergy.myjandergymusic.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Ultra-lightweight pass-through sub-bass beat and energy analyzer.
 * Does NOT alter audio samples; only publishes lock-free energy & beat hits
 * to [AudioTelemetry] for beat-reactive album art, MovingBlurView glow, and haptic bass.
 */
@OptIn(UnstableApi::class)
class RhythmBeatAnalyzerProcessor : RhythmAudioProcessor() {

    companion object {
        private const val SUB_BASS_CUTOFF_HZ = 110.0
        private const val MIN_BEAT_INTERVAL_MS = 165L
    }

    @Volatile
    private var analyzerEnabled: Boolean = true

    private var lpState = 0f
    private var lpCoeff = 0.015f
    private var avgBassEnergy = 0.02f
    private var lastBeatMs = 0L

    fun setEnabled(enable: Boolean) {
        analyzerEnabled = enable
        if (!enable) {
            AudioTelemetry.resetAudioLevels()
        }
    }

    override fun isEnabled(): Boolean = analyzerEnabled

    override fun onAudioFormatConfigured(sampleRate: Int, channelCount: Int) {
        if (sampleRate > 0) {
            val rc = 1.0 / (2.0 * PI * SUB_BASS_CUTOFF_HZ)
            val dt = 1.0 / sampleRate
            lpCoeff = (dt / (rc + dt)).toFloat()
        }
        lpState = 0f
        avgBassEnergy = 0.02f
    }

    override fun processSamples(samples: ShortArray, sampleCount: Int) {
        if (!analyzerEnabled || sampleCount <= 0 || channelCount <= 0) return

        val ch = channelCount
        val frames = sampleCount / ch
        if (frames <= 0) return

        var bassSumSq = 0f
        var totalSumSq = 0f
        var bass = lpState
        val coeff = lpCoeff

        // Stride by 2 frames when high sample rate to stay under 0.1% CPU
        val stepFrames = if (sampleRate >= 44100) 2 else 1
        var counted = 0
        var f = 0
        while (f < frames) {
            val idx = f * ch
            val mono = if (ch >= 2) {
                (samples[idx] + samples[idx + 1]) * (0.5f / 32768.0f)
            } else {
                samples[idx] / 32768.0f
            }
            bass += coeff * stepFrames * (mono - bass)
            bassSumSq += bass * bass
            totalSumSq += mono * mono
            counted++
            f += stepFrames
        }
        lpState = bass

        if (counted <= 0) return
        val bassRms = sqrt(bassSumSq / counted).coerceIn(0f, 1f)
        val totalRms = sqrt(totalSumSq / counted).coerceIn(0f, 1f)

        // Smooth telemetry publication
        AudioTelemetry.bassEnergy = AudioTelemetry.bassEnergy * 0.55f + bassRms * 0.45f
        AudioTelemetry.overallEnergy = AudioTelemetry.overallEnergy * 0.65f + totalRms * 0.35f

        // Adaptive onset detection: fires when sub-bass rises sharply over rolling average
        val threshold = maxOf(0.055f, avgBassEnergy * 1.42f)
        val now = System.nanoTime() / 1_000_000L
        if (bassRms > threshold && (now - lastBeatMs) >= MIN_BEAT_INTERVAL_MS) {
            val strength = ((bassRms - avgBassEnergy) / maxOf(0.06f, avgBassEnergy * 1.5f))
                .coerceIn(0.35f, 1.0f)
            AudioTelemetry.reportBeat(strength, now)
            lastBeatMs = now
        } else {
            AudioTelemetry.decayBeatPulse(0.86f)
        }

        // Slow rolling baseline tracker
        val alpha = if (bassRms > avgBassEnergy) 0.08f else 0.04f
        avgBassEnergy = (avgBassEnergy * (1f - alpha) + bassRms * alpha).coerceIn(0.01f, 0.5f)
    }

    override fun flush() {
        super.flush()
        lpState = 0f
        AudioTelemetry.resetAudioLevels()
    }

    override fun reset() {
        super.reset()
        lpState = 0f
        avgBassEnergy = 0.02f
        AudioTelemetry.resetAudioLevels()
    }
}
