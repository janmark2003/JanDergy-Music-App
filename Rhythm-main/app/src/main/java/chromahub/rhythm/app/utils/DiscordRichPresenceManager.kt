package chromahub.rhythm.app.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import chromahub.rhythm.app.shared.data.model.Song

/**
 * DiscordRichPresenceManager handles broadcasting Rich Presence updates to Discord
 * via the standard Android broadcast intents used by Discord integrations.
 * 
 * This implements the Discord Rich Presence protocol for Android music players,
 * allowing Rhythm to display "Now Playing" information in users' Discord status.
 * 
 * Compatible with:
 * - Discord for Android (with music status enabled)
 * - PreMiD (cross-platform presence manager)
 * - Discord Rich Presence apps that follow the standard protocol
 * 
 * Protocol: Broadcasts playback state using standardized intent actions that
 * Discord's Android app listens for to update user activity status.
 */
class DiscordRichPresenceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DiscordRichPresence"
        
        // Discord Rich Presence broadcast actions
        // These are the standard intents Discord listens for on Android
        private const val ACTION_DISCORD_PRESENCE_UPDATE = "com.discordapp.presence.UPDATE"
        private const val ACTION_DISCORD_METADATA = "com.discordapp.media.METADATA"
        
        // Intent extras for Discord Rich Presence
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_ACTIVITY_TYPE = "type"
        private const val EXTRA_STATE = "state"
        private const val EXTRA_DETAILS = "details"
        private const val EXTRA_LARGE_IMAGE = "large_image"
        private const val EXTRA_LARGE_TEXT = "large_text"
        private const val EXTRA_SMALL_IMAGE = "small_image"
        private const val EXTRA_SMALL_TEXT = "small_text"
        private const val EXTRA_TIMESTAMP_START = "timestamp_start"
        private const val EXTRA_TIMESTAMP_END = "timestamp_end"
        
        // Activity type for listening to music
        private const val ACTIVITY_TYPE_LISTENING = 2
        
        private const val APP_NAME = "Rhythm Music"
    }
    
    private var currentStartTime: Long = 0
    private var isPresenceActive = false
    
    /**
     * Update Discord Rich Presence with now playing information
     */
    fun updateNowPlaying(song: Song, isPlaying: Boolean, position: Long = 0L) {
        if (!isPlaying) {
            clearPresence()
            return
        }
        
        try {
            val intent = Intent(ACTION_DISCORD_PRESENCE_UPDATE).apply {
                putExtra(EXTRA_PACKAGE, context.packageName)
                putExtra(EXTRA_ACTIVITY_TYPE, ACTIVITY_TYPE_LISTENING)
                
                // Details: Song title
                putExtra(EXTRA_DETAILS, song.title)
                
                // State: Artist - Album
                val stateText = if (song.album.isNotEmpty()) {
                    "${song.artist} - ${song.album}"
                } else {
                    song.artist
                }
                putExtra(EXTRA_STATE, stateText)
                
                // Large image: Album artwork (if available)
                song.artworkUri?.toString()?.let { artworkUrl ->
                    putExtra(EXTRA_LARGE_IMAGE, artworkUrl)
                    putExtra(EXTRA_LARGE_TEXT, song.album)
                }
                
                // Small image: App icon/playing indicator
                putExtra(EXTRA_SMALL_IMAGE, "playing")
                putExtra(EXTRA_SMALL_TEXT, APP_NAME)
                
                // Timestamps for progress bar
                if (currentStartTime == 0L || position < 1000) {
                    currentStartTime = System.currentTimeMillis() - position
                }
                putExtra(EXTRA_TIMESTAMP_START, currentStartTime)
                
                // End timestamp (start + remaining duration)
                if (song.duration > 0) {
                    val remainingMs = song.duration - position
                    val endTime = System.currentTimeMillis() + remainingMs
                    putExtra(EXTRA_TIMESTAMP_END, endTime)
                }
            }
            
            context.sendBroadcast(intent)
            isPresenceActive = true
            Log.d(TAG, "Discord presence updated: ${song.artist} - ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Discord presence", e)
        }
    }
    
    /**
     * Clear Discord Rich Presence (when paused or stopped)
     */
    fun clearPresence() {
        if (!isPresenceActive) return
        
        try {
            val intent = Intent(ACTION_DISCORD_PRESENCE_UPDATE).apply {
                putExtra(EXTRA_PACKAGE, context.packageName)
                // Send empty presence to clear
                putExtra(EXTRA_ACTIVITY_TYPE, -1) // Clear activity
            }
            
            context.sendBroadcast(intent)
            isPresenceActive = false
            currentStartTime = 0
            Log.d(TAG, "Discord presence cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing Discord presence", e)
        }
    }
    
    /**
     * Update presence when playback is paused
     */
    fun updatePaused(song: Song) {
        try {
            val intent = Intent(ACTION_DISCORD_PRESENCE_UPDATE).apply {
                putExtra(EXTRA_PACKAGE, context.packageName)
                putExtra(EXTRA_ACTIVITY_TYPE, ACTIVITY_TYPE_LISTENING)
                
                putExtra(EXTRA_DETAILS, song.title)
                
                val stateText = if (song.album.isNotEmpty()) {
                    "${song.artist} - ${song.album} (Paused)"
                } else {
                    "${song.artist} (Paused)"
                }
                putExtra(EXTRA_STATE, stateText)
                
                song.artworkUri?.toString()?.let { artworkUrl ->
                    putExtra(EXTRA_LARGE_IMAGE, artworkUrl)
                    putExtra(EXTRA_LARGE_TEXT, song.album)
                }
                
                putExtra(EXTRA_SMALL_IMAGE, "paused")
                putExtra(EXTRA_SMALL_TEXT, "$APP_NAME - Paused")
                
                // Don't include timestamps when paused
            }
            
            context.sendBroadcast(intent)
            Log.d(TAG, "Discord presence updated (paused): ${song.artist} - ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating paused Discord presence", e)
        }
    }
    
    /**
     * Reset the start time when seeking or changing tracks
     */
    fun resetStartTime() {
        currentStartTime = 0
    }
}
