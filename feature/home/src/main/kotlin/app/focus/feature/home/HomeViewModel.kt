package app.focus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.LockMode
import app.focus.domain.model.PomodoroConfig
import app.focus.domain.model.SessionSource
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.GetCurrentStreakUseCase
import app.focus.domain.usecase.LastUsedProfileProvider
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.PauseSessionUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ResumeSessionUseCase
import app.focus.domain.usecase.StartSessionRequest
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import app.focus.domain.di.DebugPomodoroAccelerated
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
    private val pauseSessionUseCase: PauseSessionUseCase,
    private val resumeSessionUseCase: ResumeSessionUseCase,
    snapshotStore: ActiveSessionSnapshotStorage,
    private val lastUsedProfileProvider: LastUsedProfileProvider,
    private val getCurrentStreakUseCase: GetCurrentStreakUseCase,
    @DebugPomodoroAccelerated private val debugPomodoroAccelerated: Boolean,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val activeSession: app.focus.domain.model.Session? = null,
        val pomodoroPhase: String? = null,
        val phaseEndAtMillis: Long? = null,
        val profiles: List<app.focus.domain.model.Profile> = emptyList(),
        val todayFocusMinutes: Int = 0,
        val streakDays: Int = 0,
        val lastUsedProfileId: String? = null,
        val showStopConfirmation: Boolean = false,
        val showHardLockConfirmation: Boolean = false,
        val pendingStart: PendingStart? = null,
    )

    data class PendingStart(
        val profileId: String,
        val durationMinutes: Int,
        val pomodoro: Boolean = false,
    )

    private val _uiStateFlow = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> get() = _uiStateFlow

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
        viewModelScope.launch {
            snapshotStore.observe().collect { snapshot ->
                _uiStateFlow.value = _uiStateFlow.value.copy(
                    pomodoroPhase = snapshot?.takeIf { it.isPomodoro }?.currentPhase,
                    phaseEndAtMillis = snapshot?.phaseEndAtMillis?.takeIf { it > 0L },
                )
            }
        }
        viewModelScope.launch {
            val streak = runCatching { getCurrentStreakUseCase.execute() }.getOrDefault(0)
            _uiStateFlow.value = _uiStateFlow.value.copy(streakDays = streak)
        }
    }

    private fun pomodoroConfigFor(profile: app.focus.domain.model.Profile): PomodoroConfig =
        PomodoroConfig.forProfile(
            focusMinutes = profile.defaultDurationMinutes,
            debugAccelerated = debugPomodoroAccelerated,
        )

    fun startSession(profileId: String, durationMinutes: Int) {
        viewModelScope.launch {
            val profile = profileRepo.getProfile(profileId) ?: return@launch
            if (profile.lockMode is LockMode.Hard) {
                _uiStateFlow.value = _uiStateFlow.value.copy(
                    showHardLockConfirmation = true,
                    pendingStart = PendingStart(profileId, durationMinutes, pomodoro = false),
                )
            } else {
                startSessionUseCase.execute(
                    StartSessionRequest(
                        profileId = profileId,
                        durationMinutes = durationMinutes,
                        goalText = null,
                    ),
                )
                lastUsedProfileProvider.save(profileId, durationMinutes)
            }
        }
    }

    fun startPomodoroSession(profileId: String) {
        viewModelScope.launch {
            val profile = profileRepo.getProfile(profileId) ?: return@launch
            val config = pomodoroConfigFor(profile)
            val totalMinutes = config.focusMinutes * config.totalCycles +
                config.shortBreakMinutes * (config.totalCycles - 1)
            if (profile.lockMode is LockMode.Hard) {
                _uiStateFlow.value = _uiStateFlow.value.copy(
                    showHardLockConfirmation = true,
                    pendingStart = PendingStart(profileId, totalMinutes, pomodoro = true),
                )
            } else {
                startSessionUseCase.execute(
                    StartSessionRequest(
                        profileId = profileId,
                        durationMinutes = totalMinutes,
                        goalText = null,
                        source = SessionSource.POMODORO,
                        pomodoroConfig = config,
                    ),
                )
                lastUsedProfileProvider.save(profileId, totalMinutes)
            }
        }
    }

    fun dismissHardLockConfirmation() {
        _uiStateFlow.value = _uiStateFlow.value.copy(
            showHardLockConfirmation = false,
            pendingStart = null,
        )
    }

    fun confirmHardLockStart() {
        val pending = _uiStateFlow.value.pendingStart ?: return
        _uiStateFlow.value = _uiStateFlow.value.copy(
            showHardLockConfirmation = false,
            pendingStart = null,
        )
        viewModelScope.launch {
            if (pending.pomodoro) {
                val profile = profileRepo.getProfile(pending.profileId) ?: return@launch
                val config = pomodoroConfigFor(profile)
                startSessionUseCase.execute(
                    StartSessionRequest(
                        profileId = pending.profileId,
                        durationMinutes = pending.durationMinutes,
                        goalText = null,
                        source = SessionSource.POMODORO,
                        pomodoroConfig = config,
                    ),
                )
                lastUsedProfileProvider.save(pending.profileId, pending.durationMinutes)
            } else {
                startSessionUseCase.execute(
                    StartSessionRequest(
                        profileId = pending.profileId,
                        durationMinutes = pending.durationMinutes,
                        goalText = null,
                    ),
                )
                lastUsedProfileProvider.save(pending.profileId, pending.durationMinutes)
            }
        }
    }

    fun requestStopSession() {
        _uiStateFlow.value = _uiStateFlow.value.copy(showStopConfirmation = true)
    }

    fun dismissStopConfirmation() {
        _uiStateFlow.value = _uiStateFlow.value.copy(showStopConfirmation = false)
    }

    fun confirmStopSession() {
        val session = _uiStateFlow.value.activeSession ?: return
        _uiStateFlow.value = _uiStateFlow.value.copy(showStopConfirmation = false)
        viewModelScope.launch {
            stopSessionUseCase.execute(session.id, SessionStatus.Cancelled)
            val streak = runCatching { getCurrentStreakUseCase.execute() }.getOrDefault(_uiStateFlow.value.streakDays)
            _uiStateFlow.value = _uiStateFlow.value.copy(streakDays = streak)
        }
    }

    fun pauseOrResumeSession() {
        val session = _uiStateFlow.value.activeSession ?: return
        viewModelScope.launch {
            when (session.status) {
                is SessionStatus.Running -> pauseSessionUseCase.execute(session.id)
                is SessionStatus.Paused -> resumeSessionUseCase.execute(session.id)
                else -> Unit
            }
        }
    }
}
