package app.focus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.EventType
import app.focus.domain.usecase.ObserveEventLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LatencyEntry(
    val timestamp: Long,
    val packageName: String?,
    val latencyMs: Long,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    observeEventLog: ObserveEventLogUseCase,
) : ViewModel() {

    private val sinceMillis = System.currentTimeMillis() - SEVEN_DAYS_MS

    val latencyEntries: StateFlow<List<LatencyEntry>> = observeEventLog
        .execute(sinceMillis, setOf(EventType.BLOCK_LATENCY_MS))
        .map { events ->
            events.mapNotNull { event ->
                val latency = event.payload?.toLongOrNull() ?: return@mapNotNull null
                LatencyEntry(
                    timestamp = event.timestamp,
                    packageName = event.packageName,
                    latencyMs = latency,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), emptyList())

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
        private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
