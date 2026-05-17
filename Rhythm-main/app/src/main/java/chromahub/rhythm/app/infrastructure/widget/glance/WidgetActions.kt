package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.infrastructure.service.MediaPlaybackService
import kotlinx.coroutines.delay

private fun hasActiveSongSnapshot(context: Context): Boolean {
    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    val title = prefs.getString("song_title", "").orEmpty().trim()
    val artist = prefs.getString("artist_name", "").orEmpty().trim()
    val isIdleDefault = title.equals("Rhythm", ignoreCase = true) && artist.isBlank()
    return title.isNotBlank() && !isIdleDefault
}

private fun openRhythm(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    context.startActivity(intent)
}

private fun dispatchServiceAction(context: Context, action: String) {
    val intent = Intent(context, MediaPlaybackService::class.java).apply {
        this.action = action
    }
    try {
        ContextCompat.startForegroundService(context, intent)
    } catch (e: Exception) {
        Log.w("WidgetAction", "Cannot start foreground service for action: $action", e)
        openRhythm(context)
    }
}

/**
 * Play/Pause action callback for widget
 */
class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (!hasActiveSongSnapshot(context)) {
            openRhythm(context)
            return
        }

        dispatchServiceAction(context, MediaPlaybackService.ACTION_PLAY_PAUSE)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
    }
}

/**
 * Skip to next track action callback for widget
 */
class SkipNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (!hasActiveSongSnapshot(context)) {
            openRhythm(context)
            return
        }

        dispatchServiceAction(context, MediaPlaybackService.ACTION_SKIP_NEXT)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
    }
}

/**
 * Skip to previous track action callback for widget
 */
class SkipPreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (!hasActiveSongSnapshot(context)) {
            openRhythm(context)
            return
        }

        dispatchServiceAction(context, MediaPlaybackService.ACTION_SKIP_PREVIOUS)
        
        // Trigger immediate widget update after short delay for state change
        delay(100)
        try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
    }
}

/**
 * Toggle favorite action callback for widget
 */
class ToggleFavoriteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (!hasActiveSongSnapshot(context)) {
            openRhythm(context)
            return
        }

        dispatchServiceAction(context, MediaPlaybackService.ACTION_TOGGLE_FAVORITE)
        
        // Trigger immediate widget update after short delay for state change
        delay(200)
        try { RhythmMusicWidget().updateAll(context) } catch (_: Exception) {}
    }
}
