package chromahub.rhythm.app.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.shared.data.model.AppSettings

/**
 * Utility object for haptic feedback that respects user settings
 * 
 * IMPORTANT: This utility uses the current cached value of haptic settings
 * to avoid blocking the main thread. The value is read from StateFlow's current
 * value which is always available synchronously.
 */
object HapticUtils {
    
    /**
     * Performs haptic feedback only if enabled in settings.
     * Uses the cached StateFlow value to avoid blocking the main thread.
     */
    fun performHapticFeedback(
        context: Context,
        hapticFeedback: HapticFeedback,
        type: HapticFeedbackType
    ) {
        val appSettings = AppSettings.getInstance(context)
        // Use the StateFlow's current value which is synchronously available
        // This avoids runBlocking which can cause ANR
        val isEnabled = appSettings.hapticFeedbackEnabled.value
        
        if (isEnabled) {
            try {
                hapticFeedback.performHapticFeedback(type)
            } catch (e: SecurityException) {
                // Permission not granted or other security issue
                android.util.Log.w("HapticUtils", "Haptic feedback failed due to security exception: ${e.message}")
            } catch (e: Exception) {
                // Handle any other exceptions that might occur
                android.util.Log.w("HapticUtils", "Haptic feedback failed: ${e.message}")
            }
        }
    }
}

/**
 * Composable extension function for easier haptic feedback in Compose
 */
@Composable
fun HapticFeedback.performIfEnabled(type: HapticFeedbackType) {
    val context = LocalContext.current
    HapticUtils.performHapticFeedback(context, this, type)
}
