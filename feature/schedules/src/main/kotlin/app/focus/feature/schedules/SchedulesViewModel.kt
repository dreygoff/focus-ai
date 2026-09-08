package app.focus.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.Schedule
import app.focus.domain.usecase.PlanSchedulesUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val profileRepository: ProfileRepository,
    private val planSchedulesUseCase: PlanSchedulesUseCase,
) : ViewModel() {

    data class UiState(
        val schedules: List<Schedule> = emptyList(),
        val profiles: List<app.focus.domain.model.Profile> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scheduleRepository.observeSchedules().collect { schedules ->
                _uiState.value = _uiState.value.copy(schedules = schedules)
            }
        }
        viewModelScope.launch {
            profileRepository.observeProfiles().collect { profiles ->
                _uiState.value = _uiState.value.copy(profiles = profiles)
            }
        }
    }

    fun toggleEnabled(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.update(schedule.copy(enabled = !schedule.enabled))
            planSchedulesUseCase.replanAll()
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            planSchedulesUseCase.cancelAlarmsFor(scheduleId)
            scheduleRepository.delete(scheduleId)
            planSchedulesUseCase.replanAll()
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val existing = scheduleRepository.getScheduleById(schedule.id)
            if (existing == null) {
                scheduleRepository.insert(schedule)
            } else {
                scheduleRepository.update(schedule)
            }
            planSchedulesUseCase.replanAll()
        }
    }
}
