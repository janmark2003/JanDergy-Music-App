package com.jandergy.myjandergymusic.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * TikTok-grade real-time playback speed & pitch audio processor.
 *
 * Replaces ExoPlayer's speech-oriented SonicAudioProcessor with two studio-grade paths:
 * 1. Pitch-preserved Music WSOLA (Waveform Similarity Overlap-Add) with exact 50% COLA Hann
 *    windowing and 2-stage coarse-to-fine Normalized Cross-Correlation (stereo-locked so imaging,
 *    vocals, bass, and drum transients remain smooth and free of choppy stutter).
 * 2. Band-limited Varispeed Resampler (4th-order Butterworth anti-aliasing low-pass filter +
 *    4-point Catmull-Rom Cubic Hermite interpolation) for crystal-clear TikTok "Sped Up / Slowed"
 *    tape-style playback when pitch tracks speed.
 */
@OptIn(UnstableApi::class)
class TikTokSpeedAudioProcessor : AudioProcessor {

    companion object {
        private const val ACTIVE_THRESHOLD = 1e-4f
        private const val MIN_SPEED = 0.25f
        private const val MAX_SPEED = 4.0f
        private const val MIN_BYTES_FOR_DURATION_SCALING = 1024L
        private const val EPSILON = 1e-9f
    }

    @Volatile
    private var speed: Float = 1.0f

    @Volatile
    private var pitch: Float = 1.0f

    private var pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET

    private var wsolaEngine: WsolaEngine? = null
    private var resamplerEngine: CubicResamplerEngine? = null
    private var stageIntermediate = FloatArray(0)
    private var outputFifo: InterleavedFloatFifo? = null

    private var directBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    private var inputBytes: Long = 0L
    private var outputBytes: Long = 0L
    private var inputEnded: Boolean = false

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    fun setPitch(newPitch: Float) {
        pitch = newPitch.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    fun getSpeed(): Float = speed

    fun getPitch(): Float = pitch

    fun getMediaDuration(playoutDuration: Long): Long {
        if (outputBytes >= MIN_BYTES_FOR_DURATION_SCALING) {
            val processedInputBytes = (inputBytes - getPendingInputBytes()).coerceAtLeast(0L)
            if (processedInputBytes > 0L) {
                return Util.scaleLargeTimestamp(playoutDuration, processedInputBytes, outputBytes)
            }
        }
        return (speed.toDouble() * playoutDuration.toDouble()).toLong()
    }

    private fun getPendingInputBytes(): Long {
        val fmt = inputAudioFormat
        if (fmt == AudioProcessor.AudioFormat.NOT_SET || fmt.bytesPerFrame <= 0) {
            return 0L
        }
        var pendingFrames = 0.0
        val wsola = wsolaEngine
        if (wsola != null) {
            pendingFrames += wsola.pendingInputFrames()
        }
        val resampler = resamplerEngine
        if (resampler != null) {
            val stretchFactor = if (wsola != null) wsola.stretchRatio else 1.0
            pendingFrames += resampler.pendingInputFrames() * stretchFactor
        }
        val outFifo = outputFifo
        if (outFifo != null) {
            pendingFrames += outFifo.frameCount.toDouble() * speed.toDouble()
        }
        return (pendingFrames * fmt.bytesPerFrame).toLong().coerceAtLeast(0L)
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.sampleRate <= 0 ||
            inputAudioFormat.channelCount <= 0
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        pendingInputAudioFormat = inputAudioFormat
        pendingOutputAudioFormat = AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
        return pendingOutputAudioFormat
    }

    override fun isActive(): Boolean {
        return pendingOutputAudioFormat.sampleRate != Format.NO_VALUE &&
            (abs(speed - 1.0f) >= ACTIVE_THRESHOLD || abs(pitch - 1.0f) >= ACTIVE_THRESHOLD)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        val fmt = inputAudioFormat
        if (fmt == AudioProcessor.AudioFormat.NOT_SET || fmt.bytesPerFrame <= 0) {
            return
        }

        val remainingBytes = inputBuffer.remaining()
        val frameCount = remainingBytes / fmt.bytesPerFrame
        if (frameCount <= 0) {
            return
        }
        val consumedBytes = frameCount * fmt.bytesPerFrame
        inputBytes += consumedBytes.toLong()

        val channels = fmt.channelCount
        val neededFloats = frameCount * channels
        if (stageIntermediate.size < neededFloats) {
            stageIntermediate = FloatArray(neededFloats)
        }
        val shortView = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        for (i in 0 until neededFloats) {
            stageIntermediate[i] = shortView.get(i) / 32768.0f
        }
        inputBuffer.position(inputBuffer.position() + consumedBytes)

        processFloatFrames(stageIntermediate, frameCount)
        drainOutputFifoToBuffer()
    }

    override fun queueEndOfStream() {
        val outFifo = outputFifo
        if (outFifo != null) {
            val wsola = wsolaEngine
            val resampler = resamplerEngine
            when {
                wsola != null && resampler != null -> {
                    wsola.flushEndOfStream { frames, count ->
                        resampler.process(frames, count) { rFrames, rCount ->
                            outFifo.appendInterleaved(rFrames, 0, rCount)
                        }
                    }
                    resampler.flushEndOfStream { rFrames, rCount ->
                        outFifo.appendInterleaved(rFrames, 0, rCount)
                    }
                }
                wsola != null -> {
                    wsola.flushEndOfStream { frames, count ->
                        outFifo.appendInterleaved(frames, 0, count)
                    }
                }
                resampler != null -> {
                    resampler.flushEndOfStream { rFrames, rCount ->
                        outFifo.appendInterleaved(rFrames, 0, rCount)
                    }
                }
            }
            drainOutputFifoToBuffer()
        }
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        if (!outputBuffer.hasRemaining()) {
            drainOutputFifoToBuffer()
        }
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        val pendingOut = outputFifo?.frameCount ?: 0
        return inputEnded && !outputBuffer.hasRemaining() && pendingOut == 0
    }

    override fun flush() {
        if (isActive()) {
            inputAudioFormat = pendingInputAudioFormat
            outputAudioFormat = pendingOutputAudioFormat
            rebuildEngines(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        } else {
            wsolaEngine = null
            resamplerEngine = null
            outputFifo = null
        }
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputBytes = 0L
        outputBytes = 0L
        inputEnded = false
    }

    override fun reset() {
        speed = 1.0f
        pitch = 1.0f
        pendingInputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        pendingOutputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        wsolaEngine = null
        resamplerEngine = null
        outputFifo = null
        stageIntermediate = FloatArray(0)
        directBuffer = AudioProcessor.EMPTY_BUFFER
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputBytes = 0L
        outputBytes = 0L
        inputEnded = false
    }

    private fun rebuildEngines(sampleRate: Int, channelCount: Int) {
        val currentSpeed = speed.toDouble()
        val currentPitch = pitch.toDouble()
        val stretchRatio = currentSpeed / currentPitch

        val needsWsola = abs(stretchRatio - 1.0) >= ACTIVE_THRESHOLD
        val needsResampler = abs(currentPitch - 1.0) >= ACTIVE_THRESHOLD

        wsolaEngine = if (needsWsola) WsolaEngine(sampleRate, channelCount, stretchRatio) else null
        resamplerEngine = if (needsResampler) CubicResamplerEngine(sampleRate, channelCount, currentPitch) else null
        outputFifo = InterleavedFloatFifo(channelCount, max(4096, sampleRate / 4))
    }

    private fun processFloatFrames(frames: FloatArray, frameCount: Int) {
        val outFifo = outputFifo ?: return
        val wsola = wsolaEngine
        val resampler = resamplerEngine

        when {
            wsola != null && resampler != null -> {
                wsola.process(frames, frameCount) { stretched, stretchedCount ->
                    resampler.process(stretched, stretchedCount) { resampled, resampledCount ->
                        outFifo.appendInterleaved(resampled, 0, resampledCount)
                    }
                }
            }
            wsola != null -> {
                wsola.process(frames, frameCount) { stretched, stretchedCount ->
                    outFifo.appendInterleaved(stretched, 0, stretchedCount)
                }
            }
            resampler != null -> {
                resampler.process(frames, frameCount) { resampled, resampledCount ->
                    outFifo.appendInterleaved(resampled, 0, resampledCount)
                }
            }
            else -> {
                outFifo.appendInterleaved(frames, 0, frameCount)
            }
        }
    }

    private fun drainOutputFifoToBuffer() {
        if (outputBuffer.hasRemaining()) {
            return
        }
        val outFifo = outputFifo ?: return
        val availableFrames = outFifo.frameCount
        if (availableFrames <= 0) {
            return
        }
        val fmt = outputAudioFormat
        if (fmt == AudioProcessor.AudioFormat.NOT_SET || fmt.bytesPerFrame <= 0) {
            return
        }

        val byteCount = availableFrames * fmt.bytesPerFrame
        if (directBuffer.capacity() < byteCount) {
            directBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())
        } else {
            directBuffer.clear()
        }

        val sampleCount = availableFrames * fmt.channelCount
        val data = outFifo.data
        val startOffset = outFifo.headFrame * fmt.channelCount
        val shortOut = directBuffer.asShortBuffer()

        for (i in 0 until sampleCount) {
            shortOut.put(i, floatToPcm16(data[startOffset + i]))
        }

        outFifo.consumeFrames(availableFrames)
        directBuffer.position(0)
        directBuffer.limit(byteCount)
        outputBytes += byteCount.toLong()
        outputBuffer = directBuffer
    }

    private fun floatToPcm16(sample: Float): Short {
        if (!sample.isFinite()) return 0
        val absVal = abs(sample)
        val limited = if (absVal <= 0.95f) {
            sample
        } else {
            // Smooth soft-knee saturation above -0.45 dBFS to eliminate digital clipping clicks
            val over = absVal - 0.95f
            val compressed = 0.95f + 0.05f * (over / (0.05f + over))
            if (sample >= 0f) compressed else -compressed
        }
        return (limited * 32767.0f).roundToInt().coerceIn(-32768, 32767).toShort()
    }

    // =========================================================================
    // INTERLEAVED FLOAT FIFO (Zero-GC steady state)
    // =========================================================================

    private class InterleavedFloatFifo(
        val channels: Int,
        initialCapacityFrames: Int
    ) {
        var data = FloatArray(max(256, initialCapacityFrames) * channels)
            private set
        var headFrame: Int = 0
            private set
        var frameCount: Int = 0
            private set

        fun ensureAppendCapacity(additionalFrames: Int) {
            val capacityFrames = data.size / channels
            if (headFrame + frameCount + additionalFrames <= capacityFrames) {
                return
            }
            if (frameCount + additionalFrames <= capacityFrames && headFrame > 0) {
                System.arraycopy(data, headFrame * channels, data, 0, frameCount * channels)
                headFrame = 0
                return
            }
            var newCapacityFrames = max(capacityFrames * 2, frameCount + additionalFrames + 1024)
            if (newCapacityFrames < frameCount + additionalFrames) {
                newCapacityFrames = frameCount + additionalFrames
            }
            val next = FloatArray(newCapacityFrames * channels)
            if (frameCount > 0) {
                System.arraycopy(data, headFrame * channels, next, 0, frameCount * channels)
            }
            data = next
            headFrame = 0
        }

        fun appendInterleaved(src: FloatArray, srcFrameOffset: Int, framesToAppend: Int) {
            if (framesToAppend <= 0) return
            ensureAppendCapacity(framesToAppend)
            System.arraycopy(
                src,
                srcFrameOffset * channels,
                data,
                (headFrame + frameCount) * channels,
                framesToAppend * channels
            )
            frameCount += framesToAppend
        }

        fun appendZeros(zeroFrames: Int) {
            if (zeroFrames <= 0) return
            ensureAppendCapacity(zeroFrames)
            val start = (headFrame + frameCount) * channels
            val end = start + zeroFrames * channels
            data.fill(0f, start, end)
            frameCount += zeroFrames
        }

        fun consumeFrames(count: Int) {
            val clamped = count.coerceIn(0, frameCount)
            headFrame += clamped
            frameCount -= clamped
            if (frameCount == 0) {
                headFrame = 0
            }
        }
    }

    // =========================================================================
    // ENGINE 1: MUSIC-GRADE STEREO-LOCKED WSOLA (50% COLA HANN + 2-STAGE NCC)
    // =========================================================================

    private class WsolaEngine(
        sampleRate: Int,
        private val channels: Int,
        val stretchRatio: Double
    ) {
        // Music-optimized window lengths: tight enough to prevent drum flamming/echoes,
        // wide enough to preserve deep bass cycles & silky vocal harmonics.
        private val overlapFrames: Int = run {
            val halfWindowMs = if (stretchRatio >= 1.0) 21.0 else 24.0
            ((sampleRate * halfWindowMs) / 1000.0).roundToInt().coerceAtLeast(128)
        }
        private val windowFrames: Int = overlapFrames * 2
        private val seekToleranceFrames: Int = run {
            val tolMs = if (stretchRatio >= 1.0) 14.0 else 15.0
            ((sampleRate * tolMs) / 1000.0).roundToInt().coerceAtLeast(64)
        }

        // Exact 50%-overlap Constant Overlap-Add (COLA) Hann window:
        // fadeIn[k] + fadeOut[k] == 1.0f for every k in 0 until overlapFrames
        private val fadeIn = FloatArray(overlapFrames)
        private val fadeOut = FloatArray(overlapFrames)

        private val inputFifo = InterleavedFloatFifo(channels, windowFrames * 4 + seekToleranceFrames * 2)
        private val prevTail = FloatArray(overlapFrames * channels)
        private val prevTailMono = FloatArray(overlapFrames)
        private var prevTailEnergy: Float = 0f
        private var hasPrevTail: Boolean = false

        private var idealPos: Double = 0.0
        private var lastBestPos: Int = -overlapFrames

        private val outputHop = FloatArray(overlapFrames * channels)
        private var monoSearchBuf = FloatArray(seekToleranceFrames * 2 + overlapFrames + 16)

        init {
            val invOverlap = PI / overlapFrames.toDouble()
            for (k in 0 until overlapFrames) {
                val fi = (0.5 * (1.0 - cos(invOverlap * k))).toFloat()
                fadeIn[k] = fi
                fadeOut[k] = 1.0f - fi
            }
        }

        fun pendingInputFrames(): Double {
            return (inputFifo.frameCount.toDouble() - idealPos).coerceAtLeast(0.0)
        }

        fun process(src: FloatArray, frameCount: Int, emit: (FloatArray, Int) -> Unit) {
            if (frameCount <= 0) return
            inputFifo.appendInterleaved(src, 0, frameCount)
            synthesizeAvailable(emit)
        }

        fun flushEndOfStream(emit: (FloatArray, Int) -> Unit) {
            val remainingBeforePad = inputFifo.frameCount
            if (remainingBeforePad <= 0 && !hasPrevTail) {
                return
            }

            if (!hasPrevTail) {
                // Track shorter than a single WSOLA window: output directly
                val directCount = remainingBeforePad
                if (directCount > 0) {
                    val temp = FloatArray(directCount * channels)
                    System.arraycopy(
                        inputFifo.data,
                        inputFifo.headFrame * channels,
                        temp,
                        0,
                        directCount * channels
                    )
                    emit(temp, directCount)
                    inputFifo.consumeFrames(directCount)
                }
                return
            }

            // Pad silence so final synthesis hops finish cleanly without truncation
            val padFrames = windowFrames + seekToleranceFrames + 64
            inputFifo.appendZeros(padFrames)

            val targetHops = max(1, ((remainingBeforePad - idealPos) / (overlapFrames * stretchRatio)).roundToInt())
            var emittedHops = 0
            while (emittedHops < targetHops && canSynthesizeOneHop()) {
                synthesizeOneHop(emit)
                emittedHops++
            }
            inputFifo.consumeFrames(inputFifo.frameCount)
            hasPrevTail = false
        }

        private fun synthesizeAvailable(emit: (FloatArray, Int) -> Unit) {
            if (!hasPrevTail) {
                if (inputFifo.frameCount < windowFrames) {
                    return
                }
                // Output the initial half-window cleanly with zero startup fade artifact
                val baseIdx = inputFifo.headFrame * channels
                System.arraycopy(inputFifo.data, baseIdx, outputHop, 0, overlapFrames * channels)
                emit(outputHop, overlapFrames)

                capturePrevTail(overlapFrames)
                hasPrevTail = true
                lastBestPos = 0
                idealPos = overlapFrames * stretchRatio
                compactFifoIfPossible()
            }

            while (canSynthesizeOneHop()) {
                synthesizeOneHop(emit)
            }
        }

        private fun canSynthesizeOneHop(): Boolean {
            val targetPos = idealPos.roundToInt()
            val searchMax = targetPos + seekToleranceFrames
            val requiredFrames = searchMax + windowFrames
            return inputFifo.frameCount >= requiredFrames
        }

        private fun synthesizeOneHop(emit: (FloatArray, Int) -> Unit) {
            val targetPos = idealPos.roundToInt()
            val searchMin = max(0, targetPos - seekToleranceFrames)
            val searchMax = targetPos + seekToleranceFrames

            val bestPos = findBestOverlapPosition(searchMin, searchMax, targetPos)

            // Exact COLA Hann overlap-add across all channels simultaneously (100% stereo-locked)
            val srcData = inputFifo.data
            val srcBaseFrame = inputFifo.headFrame + bestPos
            var outIdx = 0
            var tailIdx = 0
            var srcIdx = srcBaseFrame * channels

            if (channels == 2) {
                for (k in 0 until overlapFrames) {
                    val fi = fadeIn[k]
                    val fo = fadeOut[k]
                    outputHop[outIdx] = prevTail[tailIdx] * fo + srcData[srcIdx] * fi
                    outputHop[outIdx + 1] = prevTail[tailIdx + 1] * fo + srcData[srcIdx + 1] * fi
                    outIdx += 2
                    tailIdx += 2
                    srcIdx += 2
                }
            } else {
                for (k in 0 until overlapFrames) {
                    val fi = fadeIn[k]
                    val fo = fadeOut[k]
                    for (ch in 0 until channels) {
                        outputHop[outIdx++] = prevTail[tailIdx++] * fo + srcData[srcIdx++] * fi
                    }
                }
            }

            emit(outputHop, overlapFrames)

            // Capture unweighted natural continuation of the newly selected grain
            capturePrevTail(bestPos + overlapFrames)
            lastBestPos = bestPos
            idealPos += overlapFrames * stretchRatio
            compactFifoIfPossible()
        }

        private fun capturePrevTail(frameOffsetFromHead: Int) {
            val srcData = inputFifo.data
            val startFloat = (inputFifo.headFrame + frameOffsetFromHead) * channels
            System.arraycopy(srcData, startFloat, prevTail, 0, overlapFrames * channels)

            var energy = 0f
            val invCh = 1.0f / channels
            if (channels == 2) {
                var idx = 0
                for (k in 0 until overlapFrames) {
                    val m = (prevTail[idx] + prevTail[idx + 1]) * 0.5f
                    prevTailMono[k] = m
                    energy += m * m
                    idx += 2
                }
            } else {
                var idx = 0
                for (k in 0 until overlapFrames) {
                    var sum = 0f
                    for (ch in 0 until channels) {
                        sum += prevTail[idx++]
                    }
                    val m = sum * invCh
                    prevTailMono[k] = m
                    energy += m * m
                }
            }
            prevTailEnergy = energy
        }

        private fun findBestOverlapPosition(searchMin: Int, searchMax: Int, targetPos: Int): Int {
            if (searchMin >= searchMax || prevTailEnergy < 1e-7f) {
                return targetPos.coerceIn(searchMin, max(searchMin, searchMax))
            }

            val monoNeeded = (searchMax - searchMin) + overlapFrames
            if (monoSearchBuf.size < monoNeeded) {
                monoSearchBuf = FloatArray(monoNeeded + 256)
            }

            // Downmix candidate search window once to mono for fast phase alignment
            val srcData = inputFifo.data
            var srcIdx = (inputFifo.headFrame + searchMin) * channels
            if (channels == 2) {
                for (i in 0 until monoNeeded) {
                    monoSearchBuf[i] = (srcData[srcIdx] + srcData[srcIdx + 1]) * 0.5f
                    srcIdx += 2
                }
            } else {
                val invCh = 1.0f / channels
                for (i in 0 until monoNeeded) {
                    var sum = 0f
                    for (ch in 0 until channels) {
                        sum += srcData[srcIdx++]
                    }
                    monoSearchBuf[i] = sum * invCh
                }
            }

            val invTol = 1.0f / max(1, seekToleranceFrames).toFloat()
            val avoidRepeatRadius = if (stretchRatio < 0.95) overlapFrames / 4 else 0

            // Stage 1: Coarse search (step 8 on offset, step 4 on correlation) tracking top 2 peaks
            var bestCoarse1 = targetPos
            var bestScore1 = Float.NEGATIVE_INFINITY
            var bestCoarse2 = targetPos
            var bestScore2 = Float.NEGATIVE_INFINITY

            var pos = searchMin
            while (pos <= searchMax) {
                val score = scoreCandidate(pos, searchMin, targetPos, invTol, avoidRepeatRadius, 4)
                if (score > bestScore1) {
                    bestScore2 = bestScore1
                    bestCoarse2 = bestCoarse1
                    bestScore1 = score
                    bestCoarse1 = pos
                } else if (score > bestScore2) {
                    bestScore2 = score
                    bestCoarse2 = pos
                }
                pos += 8
            }

            // Stage 2: Fine 1-sample-precision refinement around top 2 coarse peaks
            var bestPos = bestCoarse1
            var bestScore = Float.NEGATIVE_INFINITY

            val peaks = intArrayOf(bestCoarse1, bestCoarse2)
            for (peak in peaks) {
                val fineStart = max(searchMin, peak - 7)
                val fineEnd = min(searchMax, peak + 7)
                for (cand in fineStart..fineEnd) {
                    val score = scoreCandidate(cand, searchMin, targetPos, invTol, avoidRepeatRadius, 2)
                    if (score > bestScore) {
                        bestScore = score
                        bestPos = cand
                    }
                }
            }

            return bestPos
        }

        private fun scoreCandidate(
            pos: Int,
            searchMin: Int,
            targetPos: Int,
            invTol: Float,
            avoidRepeatRadius: Int,
            innerStride: Int
        ): Float {
            val base = pos - searchMin
            var dot = 0f
            var candEnergy = 0f
            var k = 0
            while (k < overlapFrames) {
                val r = prevTailMono[k]
                val c = monoSearchBuf[base + k]
                dot += r * c
                candEnergy += c * c
                k += innerStride
            }

            // Normalized Cross-Correlation (invariant to volume swings, maximizes phase lock)
            var score = dot / sqrt(candEnergy + EPSILON)

            // Gentle proximity preference keeps timing centered on targetPos during periodic passages
            val distNorm = abs(pos - targetPos) * invTol
            score += 0.015f * (1.0f - distNorm)

            // On slowdown (< 1.0x), discourage locking onto the exact same grain twice in a row
            if (avoidRepeatRadius > 0 && abs(pos - lastBestPos) < avoidRepeatRadius) {
                score -= 0.12f * sqrt(prevTailEnergy / max(1, overlapFrames / innerStride))
            }
            return score
        }

        private fun compactFifoIfPossible() {
            val nextTarget = idealPos.roundToInt()
            val earliestNeeded = max(0, nextTarget - seekToleranceFrames)
            if (earliestNeeded > 0) {
                val drop = min(earliestNeeded, inputFifo.frameCount)
                inputFifo.consumeFrames(drop)
                idealPos -= drop.toDouble()
                lastBestPos -= drop
            }
        }
    }

    // =========================================================================
    // ENGINE 2: ANTI-ALIASED 4-POINT CUBIC HERMITE VARISPEED RESAMPLER
    // =========================================================================

    private class CubicResamplerEngine(
        sampleRate: Int,
        private val channels: Int,
        private val rate: Double
    ) {
        private val fifo = InterleavedFloatFifo(channels, 4096)
        private var phase: Double = 1.0
        private var outChunk = FloatArray(4096 * channels)

        // 4th-order Butterworth low-pass anti-aliasing filter (2 cascaded biquads per channel)
        // Only engaged when speeding up (rate > 1.02) to block Nyquist foldback aliasing.
        private val useAntiAlias: Boolean = rate > 1.02
        private val biquad1: Array<BiquadLowPass>
        private val biquad2: Array<BiquadLowPass>

        init {
            // Prime 1 zero frame so cubic lookbehind (idx - 1) is always valid from frame 0
            fifo.appendZeros(1)

            val cutoffHz = (0.44 * sampleRate / rate).coerceIn(1000.0, sampleRate * 0.48)
            biquad1 = Array(channels) { BiquadLowPass(sampleRate.toDouble(), cutoffHz, 0.5411961) }
            biquad2 = Array(channels) { BiquadLowPass(sampleRate.toDouble(), cutoffHz, 1.3065630) }
        }

        fun pendingInputFrames(): Double {
            return (fifo.frameCount.toDouble() - phase).coerceAtLeast(0.0)
        }

        fun process(src: FloatArray, frameCount: Int, emit: (FloatArray, Int) -> Unit) {
            if (frameCount <= 0) return

            if (useAntiAlias) {
                val filtered = FloatArray(frameCount * channels)
                var idx = 0
                for (f in 0 until frameCount) {
                    for (ch in 0 until channels) {
                        val s = biquad2[ch].process(biquad1[ch].process(src[idx]))
                        filtered[idx] = s
                        idx++
                    }
                }
                fifo.appendInterleaved(filtered, 0, frameCount)
            } else {
                fifo.appendInterleaved(src, 0, frameCount)
            }

            resampleAvailable(emit)
        }

        fun flushEndOfStream(emit: (FloatArray, Int) -> Unit) {
            // Pad 2 frames so cubic lookahead (idx + 2) drains the final valid samples cleanly
            fifo.appendZeros(2)
            resampleAvailable(emit)
            fifo.consumeFrames(fifo.frameCount)
            phase = 1.0
        }

        private fun resampleAvailable(emit: (FloatArray, Int) -> Unit) {
            val available = fifo.frameCount
            if (available < 4) return

            val maxIdx = available - 3
            val estOutFrames = (((maxIdx - phase) / rate) + 4).toInt().coerceAtLeast(64)
            if (outChunk.size < estOutFrames * channels) {
                outChunk = FloatArray(estOutFrames * channels)
            }

            val data = fifo.data
            val head = fifo.headFrame
            var producedFrames = 0
            var outIdx = 0

            while (true) {
                val idx = floor(phase).toInt()
                if (idx > maxIdx) {
                    break
                }
                if (idx < 1) {
                    phase += rate
                    continue
                }
                val t = (phase - idx).toFloat()
                val base0 = (head + idx - 1) * channels
                val base1 = base0 + channels
                val base2 = base1 + channels
                val base3 = base2 + channels

                if (outIdx + channels > outChunk.size) {
                    outChunk = outChunk.copyOf(outChunk.size * 2)
                }

                for (ch in 0 until channels) {
                    val y0 = data[base0 + ch]
                    val y1 = data[base1 + ch]
                    val y2 = data[base2 + ch]
                    val y3 = data[base3 + ch]

                    // 4-point Catmull-Rom Cubic Hermite interpolation
                    val c0 = y1
                    val c1 = 0.5f * (y2 - y0)
                    val c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3
                    val c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2)
                    outChunk[outIdx++] = ((c3 * t + c2) * t + c1) * t + c0
                }
                producedFrames++
                phase += rate
            }

            if (producedFrames > 0) {
                emit(outChunk, producedFrames)
            }

            val drop = (floor(phase).toInt() - 1).coerceIn(0, fifo.frameCount)
            if (drop > 0) {
                fifo.consumeFrames(drop)
                phase -= drop.toDouble()
            }
        }
    }

    /**
     * Direct Form II Transposed 2nd-order Butterworth low-pass biquad filter.
     */
    private class BiquadLowPass(sampleRate: Double, cutoffHz: Double, q: Double) {
        private val b0: Float
        private val b1: Float
        private val b2: Float
        private val a1: Float
        private val a2: Float
        private var z1: Float = 0f
        private var z2: Float = 0f

        init {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            b0 = (((1.0 - cosW0) * 0.5) / a0).toFloat()
            b1 = ((1.0 - cosW0) / a0).toFloat()
            b2 = (((1.0 - cosW0) * 0.5) / a0).toFloat()
            a1 = ((-2.0 * cosW0) / a0).toFloat()
            a2 = ((1.0 - alpha) / a0).toFloat()
        }

        fun process(input: Float): Float {
            val out = b0 * input + z1
            z1 = b1 * input - a1 * out + z2
            z2 = b2 * input - a2 * out
            return out
        }
    }
}
