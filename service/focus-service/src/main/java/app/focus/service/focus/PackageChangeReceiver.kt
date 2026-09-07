package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * PackageChangeReceiver - debug-only receiver for PACKAGE_ADDED.
 * Per FR-15: show notification offering to add new apps to current profile.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageChangeReceiver"
        const val ACTION_PACKAGE_ADDED_DEBUG = "app.focus.service.debug.PACKAGE_ADDED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_PACKAGE_ADDED && action != ACTION_PACKAGE_ADDED_DEBUG) return
        
        val packageName = intent.data?.schemeSpecificPart ?: return
        Log.d(TAG, "Package added (debug): $packageName")
        
        // Per FR-15: show notification offering to add app to current profile
        // In release this is handled by a foreground service watching PackageManager
    }
}
