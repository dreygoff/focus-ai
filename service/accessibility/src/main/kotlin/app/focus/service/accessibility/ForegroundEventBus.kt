package app.focus.service.accessibility

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ForegroundEventBus {
    private const val TAG = "ForegroundEventBus"

    private val _events = MutableSharedFlow<ForegroundAppEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events = _events.asSharedFlow()

    var lastKnownPackage: String? = null

    fun publish(packageName: String, className: String) {
        if (packageName.isBlank()) return
        val event = ForegroundAppEvent(
            packageName = packageName,
            className = className,
        )
        if (!_events.tryEmit(event)) {
            Log.w(TAG, "Foreground event buffer full, dropping $packageName")
        }
        lastKnownPackage = packageName
    }
}
