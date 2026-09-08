package app.focus.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusAccessibilityService"
        var lastForegroundPkg: String? = null

        @Volatile
        var instance: FocusAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        if (isServiceOrIgnored(packageName, className)) return

        val rootPkg = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (_: Exception) {
            null
        }

        if (rootPkg != null && !packageName.startsWith(rootPkg)) return

        Log.d(TAG, "Foreground detected: $packageName ($className)")
        lastForegroundPkg = packageName
        ForegroundEventBus.publish(packageName, className)
    }

    private fun isServiceOrIgnored(pkg: String, className: String): Boolean {
        return pkg == packageName ||
            pkg.startsWith("com.android.systemui") ||
            pkg.contains("toast") ||
            className.contains("Dialog", ignoreCase = true) ||
            className.contains("Toast", ignoreCase = true) ||
            isImePackage(pkg) ||
            className.contains("Alert", ignoreCase = true)
    }

    private fun isImePackage(pkg: String): Boolean {
        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?: return false
        return defaultIme.contains(pkg)
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun isAccessibilityEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}
