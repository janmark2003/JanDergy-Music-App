package chromahub.rhythm.app.infrastructure.service.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DataSpec
import android.net.Uri
import chromahub.rhythm.app.features.streaming.di.StreamingMusicModule
import kotlinx.coroutines.runBlocking
import chromahub.rhythm.app.infrastructure.audio.BitPerfectRenderersFactory
import chromahub.rhythm.app.infrastructure.audio.BitPerfectAudioSink
import chromahub.rhythm.app.infrastructure.audio.RhythmBassBoostProcessor
import chromahub.rhythm.app.infrastructure.audio.RhythmSpatializationProcessor
import chromahub.rhythm.app.shared.data.model.TransitionSettings
import chromahub.rhythm.app.util.envelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages two ExoPlayer instances (A and B) to enable seamless crossfade transitions.
 *
 * Player A is the designated "master" player, which is exposed to the MediaSession.
 * Player B is the auxiliary player used to pre-buffer and fade in the next track.
 * After a transition, the players swap roles — Player A adopts the state of Player B,
 * ensuring continuity for the MediaSession.
 */
@OptIn(UnstableApi::class)
class RhythmPlayerEngine(
    private val context: Context,
    private val bitPerfectMode: Boolean = false,
    private val bassBoostProcessor: RhythmBassBoostProcessor? = null,
    private val spatializationProcessor: RhythmSpatializationProcessor? = null
) {
    companion object {
        private const val TAG = "RhythmPlayerEngine"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var transitionJob: Job? = null
    @Volatile
    private var transitionRunning = false

    private lateinit var playerA: ExoPlayer
    private lateinit var playerB: ExoPlayer

    private val onPlayerSwappedListeners = mutableListOf<(Player) -> Unit>()

    // Active Audio Session ID Flow — used for equalizer re-attachment
    private val _activeAudioSessionId = MutableStateFlow(0)
    val activeAudioSessionId: StateFlow<Int> = _activeAudioSessionId.asStateFlow()

    // Audio Focus Management — managed manually so both players share a single focus request
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isFocusLossPause = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus LOSS. Pausing both players.")
                isFocusLossPause = false
                playerA.playWhenReady = false
                playerB.playWhenReady = false
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AudioFocus LOSS_TRANSIENT. Pausing.")
                isFocusLossPause = true
                playerA.playWhenReady = false
                playerB.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus GAIN. Resuming if paused by loss.")
                if (isFocusLossPause) {
                    isFocusLossPause = false
                    playerA.playWhenReady = true
                    if (transitionRunning) playerB.playWhenReady = true
                }
            }
        }
    }

    // Listener attached to the active master player (playerA) for audio focus management
    private val masterPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                requestAudioFocus()
            } else {
                if (!isFocusLossPause) {
                    abandonAudioFocus()
                }
            }
        }
        
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            _activeAudioSessionId.value = audioSessionId
            Log.d(TAG, "Audio session ID changed: $audioSessionId")
        }
        
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            if (bitPerfectMode) {
                // Log the current audio format for bit-perfect playback
                val audioTrack = tracks.groups
                    .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
                if (audioTrack != null && audioTrack.length > 0) {
                    val format = audioTrack.getTrackFormat(0)
                    BitPerfectAudioSink.logPlaybackFormat(format)
                }
            }
        }
    }

    fun addPlayerSwapListener(listener: (Player) -> Unit) {
        onPlayerSwappedListeners.add(listener)
    }

    fun removePlayerSwapListener(listener: (Player) -> Unit) {
        onPlayerSwappedListeners.remove(listener)
    }

    /** The master player instance that should be connected to the MediaSession. */
    val masterPlayer: Player
        get() = playerA

    fun isTransitionRunning(): Boolean = transitionRunning || transitionJob?.isActive == true

    fun getAudioSessionId(): Int = if (::playerA.isInitialized) playerA.audioSessionId else 0

    private var isReleased = false

    fun initialize() {
        if (!isReleased && ::playerA.isInitialized && playerA.applicationLooper.thread.isAlive) return

        if (::playerA.isInitialized) {
            try { playerA.release() } catch (_: Exception) {}
        }
        if (::playerB.isInitialized) {
            try { playerB.release() } catch (_: Exception) {}
        }

        playerA = buildPlayer(handleAudioFocus = false)
        playerB = buildPlayer(handleAudioFocus = false)

        playerA.addListener(masterPlayerListener)

        _activeAudioSessionId.value = playerA.audioSessionId

        isReleased = false
        Log.d(TAG, "RhythmPlayerEngine initialized. SessionA=${playerA.audioSessionId}, BitPerfect=$bitPerfectMode")
    }

    private fun requestAudioFocus() {
        if (audioFocusRequest != null) return

        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        val result = audioManager.requestAudioFocus(request)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocusRequest = request
        } else {
            Log.w(TAG, "AudioFocus Request Failed: $result")
            playerA.playWhenReady = false
        }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
        }
    }

    private fun buildPlayer(handleAudioFocus: Boolean): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 30_000, 1_500, 2_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = if (bitPerfectMode || bassBoostProcessor != null || spatializationProcessor != null) {
            // Use BitPerfectRenderersFactory if bit-perfect mode is enabled OR if Rhythm processors are available
            Log.d(TAG, "Using BitPerfectRenderersFactory (bit-perfect: $bitPerfectMode, processors: ${bassBoostProcessor != null || spatializationProcessor != null})")
            BitPerfectRenderersFactory(
                context, 
                enableBitPerfect = bitPerfectMode,
                bassBoostProcessor = bassBoostProcessor,
                spatializationProcessor = spatializationProcessor
            )
        } else {
            Log.d(TAG, "Using DefaultRenderersFactory")
            DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory()),
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    if (dataSpec.uri.scheme == "streaming") {
                        val trackId = dataSpec.uri.lastPathSegment
                        if (!trackId.isNullOrBlank()) {
                            val repository = StreamingMusicModule.provideStreamingMusicRepository(context)
                            // Run blocking is safe here as ExoPlayer calls resolveDataSpec on a background thread
                            val freshUrl = runBlocking { repository.getStreamingUrl(trackId) }
                            if (!freshUrl.isNullOrBlank()) {
                                return dataSpec.withUri(Uri.parse(freshUrl))
                            }
                        }
                    }
                    return dataSpec
                }
            }
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(resolvingDataSourceFactory)

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setAudioAttributes(audioAttributes, handleAudioFocus)
                setHandleAudioBecomingNoisy(true)
                setWakeMode(C.WAKE_MODE_LOCAL)
                setSkipSilenceEnabled(false)
                playWhenReady = false
            }
    }

    fun setPauseAtEndOfMediaItems(shouldPause: Boolean) {
        playerA.pauseAtEndOfMediaItems = shouldPause
    }

    /**
     * Enables or disables gapless playback.
     * When enabled, uses ExoPlayer's native gapless mechanism.
     */
    fun setGaplessPlayback(enabled: Boolean) {
        if (::playerA.isInitialized) {
            playerA.pauseAtEndOfMediaItems = !enabled
        }
        if (::playerB.isInitialized) {
            playerB.pauseAtEndOfMediaItems = !enabled
        }
        Log.d(TAG, "Gapless playback ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Pre-buffers the next track on Player B.
     * Sets volume to 0 and pauses, ready for the crossfade transition.
     */
    fun prepareNext(mediaItem: MediaItem, startPositionMs: Long = 0L) {
        try {
            Log.d(TAG, "prepareNext called for ${mediaItem.mediaId}")
            playerB.stop()
            playerB.clearMediaItems()
            playerB.playWhenReady = false
            playerB.setMediaItem(mediaItem)
            playerB.prepare()
            playerB.volume = 0f
            if (startPositionMs > 0) {
                playerB.seekTo(startPositionMs)
            } else {
                playerB.seekTo(0)
            }
            playerB.pause()
            Log.d(TAG, "Player B prepared, paused, volume=0")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare next player", e)
        }
    }

    /**
     * Cancels any pending transition and resets Player B.
     */
    fun cancelNext() {
        transitionJob?.cancel()
        transitionRunning = false
        if (::playerB.isInitialized && playerB.mediaItemCount > 0) {
            Log.d(TAG, "Cancelling next player")
            playerB.stop()
            playerB.clearMediaItems()
        }
        if (::playerA.isInitialized) {
            playerA.volume = 1f
            setPauseAtEndOfMediaItems(false)
        }
    }

    /**
     * Performs the crossfade transition using the given settings.
     */
    @Synchronized
    fun performTransition(settings: TransitionSettings) {
        if (isTransitionRunning()) {
            Log.w(TAG, "Ignoring duplicate transition request; a transition is already active.")
            return
        }

        transitionRunning = true
        transitionJob = scope.launch {
            try {
                performOverlapTransition(settings)
            } catch (_: CancellationException) {
                Log.d(TAG, "Transition cancelled before completion.")
            } catch (e: Exception) {
                Log.e(TAG, "Error performing transition", e)
                playerA.volume = 1f
                setPauseAtEndOfMediaItems(false)
                playerB.stop()
            } finally {
                transitionRunning = false
            }
        }
    }

    /**
     * Core crossfade logic:
     * 1. Waits for Player B to be ready
     * 2. Starts Player B at volume 0
     * 3. Swaps players EARLY so UI immediately shows the new song
     * 4. Transfers queue history/future and playback settings
     * 5. Runs a fade loop with shaped curves
     * 6. Releases old player and recreates it fresh
     */
    private suspend fun performOverlapTransition(settings: TransitionSettings) {
        Log.d(TAG, "Starting crossfade. Duration: ${settings.durationMs}ms")

        if (playerB.mediaItemCount == 0) {
            Log.w(TAG, "Skipping overlap — next player not prepared (count=0)")
            playerA.volume = 1f
            setPauseAtEndOfMediaItems(false)
            return
        }

        // Ensure Player B is ready
        if (playerB.playbackState == Player.STATE_IDLE) {
            Log.d(TAG, "Player B idle. Preparing now.")
            playerB.prepare()
        }

        var readinessChecks = 0
        while (playerB.playbackState == Player.STATE_BUFFERING && readinessChecks < 120) {
            delay(25)
            readinessChecks++
        }

        if (playerB.playbackState != Player.STATE_READY) {
            Log.w(TAG, "Player B not ready for overlap. State=${playerB.playbackState}")
            playerA.volume = 1f
            setPauseAtEndOfMediaItems(false)
            return
        }

        // Start Player B playing at volume 0
        playerB.volume = 0f
        playerA.volume = 1f
        if (!playerA.isPlaying && playerA.playbackState == Player.STATE_READY) {
            playerA.play()
        }

        playerB.playWhenReady = true
        playerB.play()

        Log.d(TAG, "Player B started. Playing=${playerB.isPlaying}, state=${playerB.playbackState}")

        // Wait for Player B to actually start rendering audio
        var playChecks = 0
        while (!playerB.isPlaying && playChecks < 80) {
            delay(25)
            playChecks++
        }

        if (!playerB.isPlaying) {
            Log.e(TAG, "Player B failed to start in time. Aborting crossfade.")
            playerA.volume = 1f
            setPauseAtEndOfMediaItems(false)
            return
        }

        delay(75) // Small stabilization delay

        // --- SWAP PLAYERS EARLY (Before Fade) ---
        // This makes the UI immediately show the new song
        val outgoingPlayer = playerA
        val incomingPlayer = playerB

        val isSelfTransition = outgoingPlayer.currentMediaItem?.mediaId == incomingPlayer.currentMediaItem?.mediaId
        val outgoingMediaItemCount = outgoingPlayer.mediaItemCount
        val currentOutgoingIndex = outgoingPlayer.currentMediaItemIndex
            .takeIf { it in 0 until outgoingMediaItemCount }
            ?: 0

        // Resolve where the incoming media item belongs in the outgoing queue.
        // This keeps queue order stable across wrap-around transitions (e.g., last -> first with repeat-all)
        // and prevents duplicating the first song as a permanent loop target.
        val outgoingTimeline = outgoingPlayer.currentTimeline
        val timelineNextIndex = if (!outgoingTimeline.isEmpty && currentOutgoingIndex != C.INDEX_UNSET) {
            outgoingTimeline.getNextWindowIndex(
                currentOutgoingIndex,
                outgoingPlayer.repeatMode,
                outgoingPlayer.shuffleModeEnabled
            )
        } else {
            C.INDEX_UNSET
        }
        val incomingMediaId = incomingPlayer.currentMediaItem?.mediaId
        val incomingQueueIndex = when {
            isSelfTransition -> currentOutgoingIndex
            timelineNextIndex in 0 until outgoingMediaItemCount -> timelineNextIndex
            incomingMediaId != null -> (0 until outgoingMediaItemCount)
                .firstOrNull { index -> outgoingPlayer.getMediaItemAt(index).mediaId == incomingMediaId }
                ?: currentOutgoingIndex
            else -> currentOutgoingIndex
        }

        val historyToTransfer = mutableListOf<MediaItem>()
        for (i in 0 until incomingQueueIndex) {
            historyToTransfer.add(outgoingPlayer.getMediaItemAt(i))
        }

        val futureToTransfer = mutableListOf<MediaItem>()
        for (i in (incomingQueueIndex + 1) until outgoingMediaItemCount) {
            futureToTransfer.add(outgoingPlayer.getMediaItemAt(i))
        }

        Log.d(
            TAG,
            "Queue transfer indices: current=$currentOutgoingIndex, incoming=$incomingQueueIndex, nextFromTimeline=$timelineNextIndex, total=$outgoingMediaItemCount"
        )

        // Transfer playback settings
        val repeatModeToTransfer = outgoingPlayer.repeatMode
        val shuffleModeToTransfer = outgoingPlayer.shuffleModeEnabled
        val playbackParamsToTransfer = outgoingPlayer.playbackParameters
        incomingPlayer.repeatMode = repeatModeToTransfer
        incomingPlayer.shuffleModeEnabled = shuffleModeToTransfer
        incomingPlayer.playbackParameters = playbackParamsToTransfer
        Log.d(TAG, "Transferred playback settings: repeat=$repeatModeToTransfer, shuffle=$shuffleModeToTransfer, speed=${playbackParamsToTransfer.speed}, pitch=${playbackParamsToTransfer.pitch}")

        // Swap the player references
        outgoingPlayer.removeListener(masterPlayerListener)

        playerA = incomingPlayer
        playerB = outgoingPlayer

        // Keep the outgoing player from auto-advancing while it is fading out.
        // Otherwise it can jump to the next item and briefly replay an intro.
        playerB.pauseAtEndOfMediaItems = true
        playerA.pauseAtEndOfMediaItems = false

        playerA.addListener(masterPlayerListener)
        if (playerA.playWhenReady) {
            requestAudioFocus()
        }

        // Add history and future items to the new master player
        if (historyToTransfer.isNotEmpty()) {
            playerA.addMediaItems(0, historyToTransfer)
            Log.d(TAG, "Transferred ${historyToTransfer.size} history items.")
        }

        if (futureToTransfer.isNotEmpty()) {
            playerA.addMediaItems(futureToTransfer)
            Log.d(TAG, "Transferred ${futureToTransfer.size} future items.")
        }

        // Notify listeners about the player swap
        onPlayerSwappedListeners.forEach { it(playerA) }

        _activeAudioSessionId.value = playerA.audioSessionId

        Log.d(TAG, "Players swapped EARLY. UI should now show next song.")

        // *** FADE LOOP — shaped volume curves ***
        val duration = settings.durationMs.toLong().coerceAtLeast(500L)
        val stepMs = 16L
        var elapsed = 0L

        while (elapsed <= duration) {
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val volIn = envelope(progress, settings.curveIn)
            val volOut = 1f - envelope(progress, settings.curveOut)

            playerA.volume = volIn
            playerB.volume = volOut.coerceIn(0f, 1f)

            if (playerA.playbackState == Player.STATE_ENDED || playerB.playbackState == Player.STATE_ENDED) {
                Log.w(TAG, "A player ended during crossfade (A=${playerA.playbackState}, B=${playerB.playbackState})")
                break
            }

            delay(stepMs)
            elapsed += stepMs
        }

        Log.d(TAG, "Crossfade loop finished.")
        playerB.volume = 0f
        playerA.volume = 1f

        // Clean up outgoing player
        playerB.pause()
        playerB.stop()
        playerB.clearMediaItems()

        // Try to reset Player B for reuse instead of always recreating
        try {
            playerB.seekTo(0)
            playerB.setPlaybackSpeed(1.0f)
            playerB.setPlaybackParameters(playerB.playbackParameters)
            Log.d(TAG, "Player B reset for reuse.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reset Player B, recreating", e)
            // Fallback: Release and recreate Player B fresh to avoid OEM stale session bugs
            playerB.release()
            playerB = buildPlayer(handleAudioFocus = false)
            Log.d(TAG, "Old player released and recreated fresh.")
        }

        setPauseAtEndOfMediaItems(false)
    }

    /**
     * Releases both players and cleans up resources.
     */
    fun release() {
        transitionJob?.cancel()
        abandonAudioFocus()
        if (::playerA.isInitialized) {
            playerA.removeListener(masterPlayerListener)
            playerA.release()
        }
        if (::playerB.isInitialized) playerB.release()
        isReleased = true
        Log.d(TAG, "RhythmPlayerEngine released.")
    }
}
