package app.focus.system

import android.content.Context
import app.focus.domain.model.PermissionState
import app.focus.domain.model.PermissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FocusEvent(
    val packageName: String,
    val appName: String,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    enum class EventType { APP_FOREGROUND, APP_BACKGROUND }
}

sealed class DetectorSource {
    data object ACCESSIBILITY : DetectorSource()
    data object USAGE_STATS : DetectorSource()
    data object UNDETERMINED : DetectorSource()
}

interface AccessibilityDetector {
    fun observe(): Flow<FocusEvent>
    fun onDestroy()
}

interface UsageStatsPollingDetector {
    fun start()
    fun stop(): Boolean
    val events: Flow<FocusEvent>
    fun onDestroy()
}

/**
 * From TR-01: Orchestrates detector source switching between
 * AccessibilityDetector and UsageStatsPollingDetector based on
 * the current permission state.
 */
class DetectorOrchestrator(
    private val context: Context,
    permissionsFlow: StateFlow<Set<PermissionState>>
) {

    private var accessibilityDetector: AccessibilityDetector? = null
    private var usageStatsPollingDetector: UsageStatsPollingDetector? = null

    private val _currentSource = MutableStateFlow<DetectorSource>(DetectorSource.UNDETERMINED)
    private val _events = MutableSharedFlow<FocusEvent>(extraBufferCapacity = 64, overflowBehavior = kotlinx.coroutines.flow.OverflowBuffer)

    val currentSource: StateFlow<DetectorSource> = _currentSource
    val events: Flow<FocusEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        permissionsFlow.distinctUntilChanged().map { perms ->
            detectSource(perms)
        }.stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = DetectorSource.UNDETERMINED
        )

        // Observe permission changes and reactively switch detectors
        scope.launch {
            permissionsFlow.distinctUntilChanged().collect { perms ->
                val newSource = detectSource(perms)
                if (newSource != _currentSource.value) {
                    switchTo(newSource, force = true)
                }
            }
        }

        // Determine initial detector source
        val defaultSource = detectSource(permissionsFlow.value)
        switchTo(defaultSource, force = true)
    }

    fun registerAccessibilityDetector(detector: AccessibilityDetector) {
        if (_currentSource.value == DetectorSource.ACCESSIBILITY) {
            this.accessibilityDetector = detector
            detector.observe()
        } else {
            this.accessibilityDetector = detector
        }
    }

    fun registerUsageStatsPollingDetector(detector: UsageStatsPollingDetector) {
        if (_currentSource.value == DetectorSource.USAGE_STATS) {
            this.usageStatsPollingDetector = detector
            detector.start()
        } else {
            this.usageStatsPollingDetector = detector
        }
    }

    fun switchTo(source: DetectorSource, force: Boolean = false) {
        val currentState = _currentSource.value
        if (currentState == source && !force) return

        stopCurrent()

        when (source) {
            DetectorSource.ACCESSIBILITY -> {
                accessibilityDetector?.observe()
            }
            DetectorSource.USAGE_STATS -> {
                usageStatsPollingDetector?.start()
            }
            else -> Unit
        }

        _currentSource.value = source
    }

    private fun stopCurrent() {
        when (_currentSource.value) {
            DetectorSource.ACCESSIBILITY -> {
                accessibilityDetector?.onDestroy()
            }
            DetectorSource.USAGE_STATS -> {
                usageStatsPollingDetector?.stop()
                usageStatsPollingDetector?.onDestroy()
            }
            else -> Unit
        }
    }

    fun onDestroy() {
        stopCurrent()
        if (!scope.isCancelled) scope.cancel()
    }

    private fun detectSource(perms: Set<PermissionState>): DetectorSource {
        val hasAccessibility = perms.any { p ->
            p.granted && p.type == PermissionType.MANDATORY
        }
        return if (hasAccessibility && accessibilityDetector != null) {
            DetectorSource.ACCESSIBILITY
        } else if (usageStatsPollingDetector != null) {
            DetectorSource.USAGE_STATS
        } else {
            DetectorSource.UNDETERMINED
        }
    }
}
