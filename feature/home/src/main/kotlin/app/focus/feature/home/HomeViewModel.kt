package app.focus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepo: app.focus.domain.usecase.SessionRepository,
    private val profileRepo: app.focus.domain.usecase.ProfileRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val activeSession: app.focus.domain.model.Session? = null,
        val profiles: List<app.focus.domain.model.Profile> = emptyList(),
        val todayFocusMinutes: Int = 0,
        val streakDays: Int = 0,
        val lastUsedProfileId: String? = null
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> get() = _uiStateFlow

    sealed interface Event {
        data object NavigateToProfiles : Event
        data class StartSession(val profileId: String, val durationMinutes: Int) : Event
    }

    init {
        viewModelScope.launch {
            sessionRepo.observeActiveSession().collect { session ->
                _uiStateFlow.value = _uiStateFlow.value.copy(
                    activeSession = session,
                    isLoading = false
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
            val session = app.focus.domain.model.Session(
                id = java.util.UUID.randomUUID().toString(),
                profileId = profileId,
                profileNameSnapshot = "",
                lockMode = app.focus.domain.model.LockMode.Soft,
                targetPackagesSnapshot = emptyList(),
                goalText = null,
                startedAt = System.currentTimeMillis(),
                plannedEndAt = System.currentTimeMillis() + durationMinutes * 60_000L,
                actualEndAt = null,
                status = app.focus.domain.model.SessionStatus.Running,
                source = app.focus.domain.model.SessionSource.MANUAL,
                pomodoroConfig = null,
                bypassesUsed = 0,
                blockAttempts = 0,
                pausesUsed = 0,
                scheduleId = null
            )
            sessionRepo.insert(session)
        }
    }

    fun cancelSession() {
        _uiStateFlow.value.activeSession?.let { session ->
            viewModelScope.launch {
                val updated = session.copy(
                    status = app.focus.domain.model.SessionStatus.Cancelled,
                    actualEndAt = System.currentTimeMillis()
                )
                sessionRepo.update(updated)
            }
        }
    }
}
