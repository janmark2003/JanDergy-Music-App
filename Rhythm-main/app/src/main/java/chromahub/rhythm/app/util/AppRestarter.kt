package chromahub.rhythm.app.util

import android.content.Context
import android.content.Intent
import chromahub.rhythm.app.activities.MainActivity

object AppRestarter {
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}
