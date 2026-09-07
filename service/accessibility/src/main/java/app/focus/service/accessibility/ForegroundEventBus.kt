package app.focus.service.accessibility

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn

/**
 * ForegroundEventBus per TR-01 - singleton shared flow for accessibility events.
 * Buffers up to 64 events with DROP_OLDEST overflow behavior.
 */
object ForegroundEventBus {
    private const val TAG = "ForegroundEventBus"
    
    private val _events = MutableSharedFlow<ForegroundAppEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.flow.BufferOverflow.DROP_OLDEST
    )
    
    val events = _events.asSharedFlow()
    
    /** Publish a foreground app event from AccessibilityService */
    suspend fun publish(packageName: String, className: String) {
        try {
            // Validate the event before publishing
            if (packageName.isBlank()) return
            
            _events.emit(
                ForegroundAppEvent(
                    packageName = packageName,
                    className = className,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            // Also update the global state in FocusAccessibilityService
            FocusAccessibilityService.lastForegroundPkg = packageName
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish foreground event", e)
        }
    }
    
    /** Get the last known foreground package from Accessibility events */
    var lastKnownPackage: String? = null
    
    companion object {
        // Allow setting last known from service
        var FocusAccessibilityService.Companion.lastForegroundPkg: String?
            get() = FocusAccessibilityService.lastKownPkg
            set(value) { 
                FocusAccessibilityService.lastKownPkg = value
                if (value != null) publishSync(value, "")
            }
    }
    
    private fun publishSync(pkg: String, cls: String) {
        // Sync helper for onAccessibilityEvent context
        ForegroundEventBus.lastKnownPackage = pkg
    }
}
