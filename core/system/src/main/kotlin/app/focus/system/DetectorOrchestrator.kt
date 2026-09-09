package app.focus.system

import android.content.Context
import app.focus.domain.model.PermissionState
import app.focus.domain.model.PermissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class FocusEvent(
    val packageName: String,
    val appName: String,
    val activityClassName: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
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
    private val _events = MutableSharedFlow<FocusEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val currentSource: StateFlow<DetectorSource> = _currentSource
    val events: Flow<FocusEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var collectJob: Job? = null

    init {
        scope.launch {
            permissionsFlow.collect { perms ->
                val newSource = detectSource(perms)
                if (newSource != _currentSource.value) {
                    switchTo(newSource, force = true)
                }
            }
        }

        switchTo(detectSource(permissionsFlow.value), force = true)
    }

    fun registerAccessibilityDetector(detector: AccessibilityDetector) {
        accessibilityDetector = detector
        if (_currentSource.value == DetectorSource.ACCESSIBILITY) {
            switchTo(DetectorSource.ACCESSIBILITY, force = true)
        }
    }

    fun registerUsageStatsPollingDetector(detector: UsageStatsPollingDetector) {
        usageStatsPollingDetector = detector
        if (_currentSource.value == DetectorSource.USAGE_STATS) {
            switchTo(DetectorSource.USAGE_STATS, force = true)
        }
    }

    fun switchTo(source: DetectorSource, force: Boolean = false) {
        if (_currentSource.value == source && !force) return
        stopCurrent()
        _currentSource.value = source
        when (source) {
            DetectorSource.ACCESSIBILITY -> usageStatsPollingDetector?.stop()
            DetectorSource.USAGE_STATS -> usageStatsPollingDetector?.start()
            else -> Unit
        }
        startCollecting()
    }

    fun stopActiveDetectors() {
        collectJob?.cancel()
        usageStatsPollingDetector?.stop()
    }

    private fun startCollecting() {
        collectJob?.cancel()
        val sourceFlow = when (_currentSource.value) {
            DetectorSource.ACCESSIBILITY -> accessibilityDetector?.observe()
            DetectorSource.USAGE_STATS -> usageStatsPollingDetector?.events
            DetectorSource.UNDETERMINED -> null
        } ?: return

        collectJob = scope.launch {
            sourceFlow.collect { event ->
                _events.emit(event)
            }
        }
    }

    private fun stopCurrent() {
        collectJob?.cancel()
        when (_currentSource.value) {
            DetectorSource.USAGE_STATS -> usageStatsPollingDetector?.stop()
            else -> Unit
        }
    }

    fun onDestroy() {
        stopCurrent()
        accessibilityDetector?.onDestroy()
        usageStatsPollingDetector?.onDestroy()
        scope.cancel()
    }

    private fun detectSource(perms: Set<PermissionState>): DetectorSource {
        val hasAccessibility = perms.any { permission ->
            permission.name == PermissionChecker.ID_ACCESSIBILITY && permission.granted
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
