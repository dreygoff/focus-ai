package app.focus.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.ExportStatsCsvUseCase
import app.focus.domain.usecase.GetStatsDashboardUseCase
import app.focus.domain.usecase.ObserveEventLogUseCase
import app.focus.domain.usecase.StatsDashboard
import app.focus.domain.usecase.StatsPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val getStatsDashboard: GetStatsDashboardUseCase,
    private val exportStatsCsv: ExportStatsCsvUseCase,
    private val observeEventLog: ObserveEventLogUseCase,
    private val clock: Clock,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val period: StatsPeriod = StatsPeriod.WEEK,
        val dashboard: StatsDashboard? = null,
        val events: List<EventLog> = emptyList(),
        val eventFilter: EventFilter = EventFilter.ALL,
        val exportCsv: String? = null,
        val errorMessage: String? = null,
    )

    enum class EventFilter(val types: Set<EventType>?) {
        ALL(null),
        SESSIONS(SESSION_TYPES),
        BLOCKS(BLOCK_TYPES),
        BYPASSES(BYPASS_TYPES),
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var eventsJob: Job? = null

    init {
        refresh()
    }

    fun selectPeriod(period: StatsPeriod) {
        if (_uiState.value.period == period) return
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    fun selectEventFilter(filter: EventFilter) {
        _uiState.update { it.copy(eventFilter = filter) }
        observeEvents(_uiState.value.period, filter)
    }

    fun refresh() {
        val period = _uiState.value.period
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getStatsDashboard.execute(period) }
                .onSuccess { dashboard ->
                    _uiState.update { it.copy(isLoading = false, dashboard = dashboard) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load stats")
                    }
                }
        }
        observeEvents(period, _uiState.value.eventFilter)
    }

    fun requestExport() {
        viewModelScope.launch {
            runCatching { exportStatsCsv.execute(_uiState.value.period) }
                .onSuccess { csv -> _uiState.update { it.copy(exportCsv = csv) } }
        }
    }

    fun clearExportRequest() {
        _uiState.update { it.copy(exportCsv = null) }
    }

    private fun observeEvents(period: StatsPeriod, filter: EventFilter) {
        eventsJob?.cancel()
        val sinceMillis = clock.nowMillis() - period.dayCount * MS_PER_DAY
        eventsJob = viewModelScope.launch {
            observeEventLog.execute(sinceMillis, filter.types).collect { events ->
                _uiState.update { it.copy(events = events) }
            }
        }
    }

    companion object {
        private const val MS_PER_DAY = 86_400_000L
        private val SESSION_TYPES = setOf(
            EventType.SESSION_STARTED,
            EventType.SESSION_COMPLETED,
            EventType.SESSION_CANCELLED,
            EventType.SESSION_EXPIRED,
            EventType.SESSION_PAUSED,
            EventType.SESSION_RESUMED,
        )
        private val BLOCK_TYPES = setOf(EventType.BLOCK_SHOWN, EventType.TAMPER_ATTEMPT)
        private val BYPASS_TYPES = setOf(
            EventType.BYPASS_GRANTED,
            EventType.BYPASS_DENIED,
            EventType.BYPASS_STARTED,
        )
    }
}
