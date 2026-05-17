package chromahub.rhythm.app.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import chromahub.rhythm.app.shared.data.model.Song

/**
 * ScrobblerManager handles broadcasting scrobble intents to Last.fm, Pano Scrobbler,
 * and other music scrobbling apps that support the Simple Last.fm Scrobbler (SLS) API.
 * 
 * This implements the standard Android scrobble broadcast protocol used by:
 * - Last.fm for Android
 * - Pano Scrobbler
 * - Simple Last.fm Scrobbler
 * - And other compatible apps
 * 
 * API Documentation: https://github.com/tgwizard/sls/wiki/Developer's-API
 */
class ScrobblerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ScrobblerManager"
        
        // Standard SLS API Actions
        private const val ACTION_METADATA_CHANGED = "com.android.music.metachanged"
        private const val ACTION_PLAYSTATE_CHANGED = "com.android.music.playstatechanged"
        
        // Intent extras
        private const val EXTRA_ARTIST = "artist"
        private const val EXTRA_ALBUM = "album"
        private const val EXTRA_TRACK = "track"
        private const val EXTRA_DURATION = "duration"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_PLAYING = "playing"
        
        // Optional extras
        private const val EXTRA_ALBUM_ARTIST = "albumArtist"
        private const val EXTRA_TRACK_NUMBER = "trackNumber"
        private const val EXTRA_SOURCE = "source"
        
        private const val SOURCE_APP_NAME = "Rhythm Music"
    }
    
    /**
     * Send metadata update broadcast when track changes
     */
    fun sendMetadataUpdate(song: Song, position: Long = 0L) {
        try {
            val intent = Intent(ACTION_METADATA_CHANGED).apply {
                putExtra(EXTRA_ARTIST, song.artist)
                putExtra(EXTRA_ALBUM, song.album)
                putExtra(EXTRA_TRACK, song.title)
                putExtra(EXTRA_DURATION, song.duration.toInt()) // Duration in milliseconds
                putExtra(EXTRA_POSITION, position.toInt()) // Current position in milliseconds
                putExtra(EXTRA_SOURCE, SOURCE_APP_NAME)
                
                // Optional metadata
                song.albumArtist?.let { putExtra(EXTRA_ALBUM_ARTIST, it) }
                if (song.trackNumber > 0) {
                    putExtra(EXTRA_TRACK_NUMBER, song.trackNumber)
                }
            }
            
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent metadata broadcast: ${song.artist} - ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending metadata broadcast", e)
        }
    }
    
    /**
     * Send playback state change broadcast when play/pause state changes
     */
    fun sendPlaybackStateUpdate(isPlaying: Boolean, position: Long = 0L) {
        try {
            val intent = Intent(ACTION_PLAYSTATE_CHANGED).apply {
                putExtra(EXTRA_PLAYING, isPlaying)
                putExtra(EXTRA_POSITION, position.toInt())
                putExtra(EXTRA_SOURCE, SOURCE_APP_NAME)
            }
            
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent playback state broadcast: playing=$isPlaying, position=$position")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending playback state broadcast", e)
        }
    }
    
    /**
     * Send complete scrobble when track starts playing
     * This sends both metadata and playback state in one call
     */
    fun scrobbleNowPlaying(song: Song, position: Long = 0L) {
        sendMetadataUpdate(song, position)
        sendPlaybackStateUpdate(isPlaying = true, position)
    }
    
    /**
     * Send pause broadcast
     */
    fun scrobblePaused(position: Long = 0L) {
        sendPlaybackStateUpdate(isPlaying = false, position)
    }
    
    /**
     * Send resume broadcast
     */
    fun scrobbleResumed(position: Long = 0L) {
        sendPlaybackStateUpdate(isPlaying = true, position)
    }
}
