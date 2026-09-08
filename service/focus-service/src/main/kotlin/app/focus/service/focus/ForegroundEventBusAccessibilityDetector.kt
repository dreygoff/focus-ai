package app.focus.service.focus

import app.focus.service.accessibility.ForegroundEventBus
import app.focus.system.AccessibilityDetector
import app.focus.system.FocusEvent
import kotlinx.coroutines.flow.map

internal class ForegroundEventBusAccessibilityDetector : AccessibilityDetector {
    override fun observe() = ForegroundEventBus.events.map { event ->
        FocusEvent(
            packageName = event.packageName,
            appName = event.className.orEmpty(),
            timestampMillis = System.currentTimeMillis(),
        )
    }

    override fun onDestroy() = Unit
}
