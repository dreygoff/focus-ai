package app.focus.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.os.Build
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableSharedFlow
import app.focus.domain.model.BlockDecision

/**
 * FocusAccessibilityService per TR-01.
 * Thin service: only converts events to ForegroundAppEvent and publishes to event bus.
 * Provides goHome/goBack actions via performGlobalAction.
 */
class FocusAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusAccessibilityService"
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Per TR-01: filter for typeWindowStateChanged and typeWindowsChanged
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Filter out IME, dialogs, toasts from our own service
        if (isServiceOrIgnored(packageName, className)) return

        // Additional validation via rootInActiveWindow per TR-01
        val rootPkg = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (_: Exception) { null }

        val windows = try {
            windows
        } catch (_: EventLogException) { emptyList() }
        
        val isValidWindow = windows.any { it.type == AccessibilityEvent.TYPE_APPLICATION }
        if (rootPkg != null && !packageName.startsWith(rootPkg)) return

        // Create and publish event
        Log.d(TAG, "Foreground detected: $packageName ($className)")
        ForegroundEventBus.publish(packageName, className)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    private fun isServiceOrIgnored(pkg: String, className: String): Boolean {
        return pkg == packageName ||     // Our own service
            pkg.startsWith("com.android.systemui") ||
            pkg.contains("toast") ||
            className.contains("Dialog") ||
            className.contains("Toast") ||
            isIME(pkg) ||
            isOverlayWindow(className)
    }

    private fun isIME(pkg: String): Boolean {
        return try {
            val im = inputMethodManager
            im.enabledInputMethods?.any { 
                pkg.contains(it.packageName) 
            } ?: false
        } catch (_: Exception) { false }
    }

    private fun isOverlayWindow(className: String): Boolean {
        // Filter overlay windows (permissions, settings screens)
        return className.contains("Dialog") ||
            className.contains("Alert") ||
            className.contains("Panel")
    }

    /**
     * Go to home screen per TR-01.
     */
    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Go back per TR-01.
     */
    fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Check if accessibility settings are enabled.
     */
    fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            )
            enabled == 1 && hasCapability(android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES)
        } catch (_: Exception) { false }
    }

    /**
     * Open accessibility settings for this service.
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context?.startActivity(intent)
    }
}
