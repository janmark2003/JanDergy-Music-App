package chromahub.rhythm.app.infrastructure.widget

import android.content.Context
import android.net.Uri
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater

object WidgetUpdater {
    
    private const val PREFS_FILE = "widget_prefs"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_SONG_TITLE = "song_title"
    private const val KEY_ARTIST_NAME = "artist_name"
    
    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
        isFavorite: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        if (song != null) {
            editor.putString(KEY_SONG_TITLE, song.title)
            editor.putString(KEY_ARTIST_NAME, song.artist)
            editor.putString("album_name", song.album)
            editor.putString("artwork_uri", song.artworkUri?.toString())
        } else {
            editor.putString(KEY_SONG_TITLE, "Rhythm")
            editor.putString(KEY_ARTIST_NAME, "")
            editor.putString("album_name", "")
            editor.remove("artwork_uri")
        }
        
        editor.putBoolean(KEY_IS_PLAYING, isPlaying)
        editor.putBoolean("has_previous", hasPrevious)
        editor.putBoolean("has_next", hasNext)
        editor.putBoolean("is_favorite", isFavorite)
        editor.apply() // Use apply for async write to prevent ANR
        
        // Update legacy RemoteViews widget
        MusicWidgetProvider.updateWidgets(context)
        
        // Update modern Glance widget
        GlanceWidgetUpdater.updateWidget(context, song, isPlaying, hasPrevious, hasNext, isFavorite)

        // Avoid forcing TileService listen cycles on every song change.
        // The tile reads the latest snapshot from preferences in onStartListening.
    }
    
    fun clearWidget(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // Update legacy RemoteViews widget
        MusicWidgetProvider.updateWidgets(context)
        
        // Update modern Glance widget
        GlanceWidgetUpdater.updateWidgetEmpty(context)

        // Avoid forcing TileService listen cycles during clear operations.
    }
}
