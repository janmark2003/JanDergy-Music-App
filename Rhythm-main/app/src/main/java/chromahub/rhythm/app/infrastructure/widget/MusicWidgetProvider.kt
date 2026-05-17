package chromahub.rhythm.app.infrastructure.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.R
import chromahub.rhythm.app.infrastructure.service.MediaPlaybackService
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_PLAY_PAUSE = "chromahub.rhythm.app.infrastructure.widget.PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "chromahub.rhythm.app.infrastructure.widget.SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "chromahub.rhythm.app.infrastructure.widget.SKIP_PREVIOUS"
        
        fun updateWidgets(context: Context) {
            val intent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                sendMediaAction(context, MediaPlaybackService.ACTION_PLAY_PAUSE)
            }
            ACTION_SKIP_NEXT -> {
                sendMediaAction(context, MediaPlaybackService.ACTION_SKIP_NEXT)
            }
            ACTION_SKIP_PREVIOUS -> {
                sendMediaAction(context, MediaPlaybackService.ACTION_SKIP_PREVIOUS)
            }
        }
    }

    private fun sendMediaAction(context: Context, action: String) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            // Fallback: launch the main activity which will bind to the service
            try {
                val fallback = Intent(context, MainActivity::class.java).apply {
                    this.action = action
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) { }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Get widget size
        val widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        
        // Determine widget layout based on size - comprehensive grid support
        // Android home screen grid: ~70dp per cell, but we use more granular detection
        val aspectRatio = minHeight.toFloat() / minWidth.toFloat()
        val layoutId = when {
            // 5x5+ (350+ width x 350+ height): Premium largest size - enable for larger screens
            minWidth >= 350 && minHeight >= 350 -> R.layout.widget_music_5x5
            
            // 5x4 or 4x5 (350+ width x 280+ height): Tall wide layout
            minWidth >= 350 && minHeight >= 280 -> R.layout.widget_music_extra_large
            
            // 4x4+ (280+ width x 280+ height): Extra large square
            minWidth >= 280 && minHeight >= 280 -> R.layout.widget_music_extra_large
            
            // 5x3 or 4x3 (300+ width x 210+ height): Large horizontal
            minWidth >= 300 && minHeight >= 210 -> R.layout.widget_music_large
            
            // 3x4 or 4x3 (210+ width x 300+ height): Large vertical
            minWidth >= 210 && minHeight >= 300 -> R.layout.widget_music_large
            
            // 3x3 (210+ width x 210+ height): Large square
            minWidth >= 210 && minHeight >= 210 -> R.layout.widget_music_large
            
            // 5x2 or 4x2 (320+ width x 140+ height): Wide horizontal
            minWidth >= 320 && minHeight < 210 -> R.layout.widget_music_wide
            
            // 3x2 (180-320dp width x 100-210dp height): Medium horizontal
            minWidth >= 180 && minHeight >= 100 && minHeight < 210 && aspectRatio < 1.2f -> R.layout.widget_music_medium
            
            // 2x3 (100-180dp width x 210+ height): Medium vertical - use vertical layout
            minWidth >= 100 && minWidth < 180 && minHeight >= 210 && aspectRatio > 1.5f -> R.layout.widget_music_vertical
            
            // 2x2 (100-180dp width x 100-210dp height): Small square
            minWidth >= 100 && minWidth < 180 && minHeight >= 100 -> R.layout.widget_music_small
            
            // 3x1 or 2x1 (110-320dp width x 40-100dp height): Extra small horizontal strip
            minHeight < 100 -> R.layout.widget_music_extra_small
            
            // Default fallback: Medium for any unmatched sizes
            else -> R.layout.widget_music_medium
        }
        
        val views = RemoteViews(context.packageName, layoutId)
        
        // Get current playback state from SharedPreferences
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val songTitle = prefs.getString("song_title", "Rhythm") ?: "Rhythm"
        val artistName = prefs.getString("artist_name", "") ?: ""
        val albumName = prefs.getString("album_name", "") ?: ""
        val artworkUriString = prefs.getString("artwork_uri", null)
        val isPlaying = prefs.getBoolean("is_playing", false)
        
        // Get widget settings from SharedPreferences directly (avoid blocking StateFlow)
        val settingsPrefs = context.getSharedPreferences("rhythm_preferences", Context.MODE_PRIVATE)
        val showArtist = settingsPrefs.getBoolean("widget_show_artist", true)
        val showAlbum = settingsPrefs.getBoolean("widget_show_album", true)
        val showAlbumArt = settingsPrefs.getBoolean("widget_show_album_art", true)
        
        // Update text views
        views.setTextViewText(R.id.widget_song_title, songTitle)
        
        // Update artist name visibility
        if (showArtist) {
            views.setTextViewText(R.id.widget_artist_name, artistName)
            views.setViewVisibility(R.id.widget_artist_name, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_artist_name, android.view.View.GONE)
        }
        
        // Update album name if widget layout supports it
        try {
            if (layoutId == R.layout.widget_music_large || 
                layoutId == R.layout.widget_music_medium ||
                layoutId == R.layout.widget_music_wide ||
                layoutId == R.layout.widget_music_extra_large ||
                layoutId == R.layout.widget_music_5x5) {
                if (showAlbum) {
                    views.setTextViewText(R.id.widget_album_name, albumName)
                    views.setViewVisibility(R.id.widget_album_name, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_album_name, android.view.View.GONE)
                }
            }
        } catch (e: Exception) {
            // Some layouts might not have album_name view
        }
        
        // Note: Transparency/alpha settings are not applied to RemoteViews widgets
        // as they don't support dynamic alpha changes on FrameLayout backgrounds
        
        // Update artwork
        if (showAlbumArt && artworkUriString != null) {
            // First update widget with default state
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_music_note)
            views.setViewVisibility(R.id.widget_album_art, android.view.View.VISIBLE)
            
            // Then load actual artwork asynchronously
            scope.launch {
                try {
                    val artworkUri = Uri.parse(artworkUriString)
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .size(300)
                        .target { drawable ->
                            val bitmap = drawable.toBitmap()
                            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                        .build()
                    imageLoader.enqueue(request)
                } catch (e: Exception) {
                    // Keep default icon on error
                }
            }
        } else if (showAlbumArt) {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_music_note)
            views.setViewVisibility(R.id.widget_album_art, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_album_art, android.view.View.GONE)
        }
        
        // Update play/pause button icon
        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
        
        // Set up click intents
        setupClickIntents(context, views, appWidgetId)
        
        // Update the widget immediately
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupClickIntents(context: Context, views: RemoteViews, appWidgetId: Int) {
        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent)
        
        // Play/Pause intent (all widgets have this)
        val playPauseIntent = Intent(context, MusicWidgetProvider::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
        
        // Skip Next intent (all widgets except extra_small have this)
        try {
            val skipNextIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_SKIP_NEXT
            }
            val skipNextPendingIntent = PendingIntent.getBroadcast(
                context,
                2,
                skipNextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next, skipNextPendingIntent)
        } catch (e: Exception) {
            // Some layouts might not have next button
        }
        
        // Skip Previous intent (medium, wide, large, and extra_large widgets have this)
        try {
            val skipPreviousIntent = Intent(context, MusicWidgetProvider::class.java).apply {
                action = ACTION_SKIP_PREVIOUS
            }
            val skipPreviousPendingIntent = PendingIntent.getBroadcast(
                context,
                3,
                skipPreviousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_previous, skipPreviousPendingIntent)
        } catch (e: Exception) {
            // Some layouts might not have previous button
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scope.cancel()
    }
}
