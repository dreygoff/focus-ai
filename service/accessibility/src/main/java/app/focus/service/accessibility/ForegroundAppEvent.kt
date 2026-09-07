package app.focus.service.accessibility

import android.os.SystemClock

/** Represents a foreground app event from any detector source. */
data class ForegroundAppEvent(
    val packageName: String,
    val className: String?,
    val timestamp: Long = SystemClock.uptimeMillis()
) {
    val isFromAccessibilitySource: Boolean = className != null
}
