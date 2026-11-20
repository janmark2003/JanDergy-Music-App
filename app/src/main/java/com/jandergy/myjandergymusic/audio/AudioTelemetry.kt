package com.jandergy.myjandergymusic.audio

import java.util.concurrent.atomic.AtomicInteger

/**
 * Lock-free real-time audio telemetry shared between DSP processors (audio thread)
 * and UI visualizers / haptics / radar widgets (main thread).
 */
object AudioTelemetry {

    @Volatile
    var bassEnergy: Float = 0f

    @Volatile
    var overallEnergy: Float = 0f

    @Volatile
    var beatPulse: Float = 0f

    @Volatile
    var lastBeatTimeMs: Long = 0L

    @Volatile
    var lastBeatStrength: Float = 0f

    private val beatCounter = AtomicInteger(0)

    @Volatile
    var spatialX: Float = 0f

    @Volatile
    var spatialY: Float = 1f

    @Volatile
    var spatialActive: Boolean = false

    fun getBeatCount(): Int = beatCounter.get()

    fun reportBeat(strength: Float, nowMs: Long) {
        lastBeatStrength = strength.coerceIn(0f, 1f)
        lastBeatTimeMs = nowMs
        beatPulse = maxOf(beatPulse, lastBeatStrength)
        beatCounter.incrementAndGet()
    }

    fun decayBeatPulse(factor: Float) {
        val next = beatPulse * factor
        beatPulse = if (next < 0.005f) 0f else next
    }

    fun resetAudioLevels() {
        bassEnergy = 0f
        overallEnergy = 0f
        beatPulse = 0f
    }
}
