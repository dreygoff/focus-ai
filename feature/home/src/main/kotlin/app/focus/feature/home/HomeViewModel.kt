package app.focus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeActiveSessionsUseCase: ObserveActiveSessionsUseCase,
    private val profileRepo: ProfileRepository,
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val activeSession: app.focus.domain.model.Session? = null,
        val profiles: List<app.focus.domain.model.Profile> = emptyList(),
        val todayFocusMinutes: Int = 0,
        val streakDays: Int = 0,
        val lastUsedProfileId: String? = null,
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> get() = _uiStateFlow

    sealed interface Event {
        data object NavigateToProfiles : Event
        data class StartSession(val profileId: String, val durationMinutes: Int) : Event
    }

    init {
        viewModelScope.launch {
            observeActiveSessionsUseCase.execute().collect { session ->
                _uiStateFlow.value = _uiStateFlow.value.copy(
                    activeSession = session,
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            profileRepo.observeProfiles().collect { profiles ->
                _uiStateFlow.value = _uiStateFlow.value.copy(profiles = profiles)
            }
        }
    }

    fun startSession(profileId: String, durationMinutes: Int) {
        viewModelScope.launch {
            startSessionUseCase.execute(
                profileId = profileId,
                durationMinutes = durationMinutes,
                goalText = null,
            )
        }
    }

    fun cancelSession() {
        val session = _uiStateFlow.value.activeSession ?: return
        viewModelScope.launch {
            stopSessionUseCase.execute(session.id, SessionStatus.Cancelled)
        }
    }
}
