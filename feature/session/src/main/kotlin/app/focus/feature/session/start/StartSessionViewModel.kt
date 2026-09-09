package app.focus.feature.session.start

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.LockMode
import app.focus.domain.usecase.LastUsedProfileProvider
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.StartSessionRequest
import app.focus.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val lastUsedProfileProvider: LastUsedProfileProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartSessionUiState())
    val uiState: StateFlow<StartSessionUiState> = _uiState.asStateFlow()

    private val _profiles = MutableStateFlow<List<app.focus.domain.model.Profile>>(emptyList())
    val profiles: StateFlow<List<app.focus.domain.model.Profile>> = _profiles.asStateFlow()

    private val _events = MutableSharedFlow<StartSessionNavEvent>()
    val events: SharedFlow<StartSessionNavEvent> = _events.asSharedFlow()

    private val _showHardLockConfirmation = MutableStateFlow(false)
    val showHardLockConfirmation: StateFlow<Boolean> = _showHardLockConfirmation.asStateFlow()

    init {
        val profileId = savedStateHandle.get<String>(ARG_PROFILE_ID)?.takeIf { it.isNotEmpty() }
        val durationMinutes = savedStateHandle.get<Int>(ARG_DURATION_MINUTES) ?: DEFAULT_DURATION_MINUTES
        _uiState.update {
            it.copy(
                selectedProfileId = profileId,
                durationMinutes = durationMinutes.coerceIn(MIN_DURATION, MAX_DURATION),
            )
        }
        viewModelScope.launch {
            profileRepository.observeProfiles().collect { profiles ->
                _profiles.value = profiles
                _uiState.update { state ->
                    val selected = state.selectedProfileId ?: profiles.firstOrNull()?.id
                    state.copy(selectedProfileId = selected)
                }
            }
        }
    }

    fun onEvent(event: StartSessionEvent) {
        _uiState.update { state ->
            when (event) {
                is StartSessionEvent.SelectProfile -> state.copy(selectedProfileId = event.id)
                is StartSessionEvent.SetDuration -> state.copy(
                    durationMinutes = event.minutes.coerceIn(MIN_DURATION, MAX_DURATION),
                    infinite = false,
                )
                is StartSessionEvent.SetGoal -> state.copy(goalText = event.text)
                is StartSessionEvent.TogglePomodoro -> state.copy(pomodoroEnabled = event.enabled)
                is StartSessionEvent.SetDurationMode -> state.copy(durationMode = event.mode)
                is StartSessionEvent.SetUntilTime -> state.copy(
                    untilHour = event.hour,
                    untilMinute = event.minute,
                )
                is StartSessionEvent.SetPlannedEndAt -> state.copy(plannedEndAtMillis = event.epochMillis)
                is StartSessionEvent.SetInfinite -> state.copy(
                    infinite = event.enabled,
                    durationMode = if (event.enabled) DurationMode.INFINITE else state.durationMode,
                )
            }
        }
    }

    fun confirmStart() {
        val state = _uiState.value
        val profileId = state.selectedProfileId ?: return
        viewModelScope.launch {
            val profile = profileRepository.getProfile(profileId) ?: return@launch
            if (profile.lockMode is LockMode.Hard) {
                _showHardLockConfirmation.value = true
            } else {
                executeStart(profileId, state)
            }
        }
    }

    fun dismissHardLockConfirmation() {
        _showHardLockConfirmation.value = false
    }

    fun confirmHardLockStart() {
        _showHardLockConfirmation.value = false
        val state = _uiState.value
        val profileId = state.selectedProfileId ?: return
        viewModelScope.launch {
            executeStart(profileId, state)
        }
    }

    private suspend fun executeStart(profileId: String, state: StartSessionUiState) {
        val goal = state.goalText.trim().takeIf { it.isNotEmpty() }
        val infinite = state.infinite || state.durationMode == DurationMode.INFINITE
        val plannedEndAt = if (state.durationMode == DurationMode.UNTIL_TIME) {
            state.plannedEndAtMillis
        } else {
            null
        }
        startSessionUseCase.execute(
            StartSessionRequest(
                profileId = profileId,
                durationMinutes = state.durationMinutes,
                goalText = goal,
                plannedEndAtMillis = plannedEndAt,
                infinite = infinite,
            ),
        )
        lastUsedProfileProvider.save(profileId, state.durationMinutes)
        _events.emit(StartSessionNavEvent.SessionStarted)
    }

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_DURATION_MINUTES = "durationMinutes"
        private const val DEFAULT_DURATION_MINUTES = 25
        private const val MIN_DURATION = 1
        private const val MAX_DURATION = 1440
    }
}

sealed interface StartSessionNavEvent {
    data object SessionStarted : StartSessionNavEvent
}
