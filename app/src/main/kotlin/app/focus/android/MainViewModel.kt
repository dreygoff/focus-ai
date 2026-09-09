package app.focus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.datastore.UserSettingsRepository
import app.focus.domain.model.Theme
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.QuickStartProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val quickStartProfileUseCase: QuickStartProfileUseCase,
    private val observeActiveSessionsUseCase: ObserveActiveSessionsUseCase,
) : ViewModel() {

    private val _showOnboarding = MutableStateFlow<Boolean?>(null)
    val showOnboarding: StateFlow<Boolean?> = _showOnboarding.asStateFlow()

    private var trackedActiveSessionId: String? = null

    private val _pendingSessionSummaryId = MutableStateFlow<String?>(null)
    val pendingSessionSummaryId: StateFlow<String?> = _pendingSessionSummaryId.asStateFlow()

    val theme: StateFlow<Theme> = userSettingsRepository.getTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), Theme.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = userSettingsRepository.getDynamicColorEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), true)

    init {
        viewModelScope.launch {
            _showOnboarding.value = !userSettingsRepository.isOnboardingCompleted()
        }
        viewModelScope.launch {
            observeActiveSessionsUseCase.execute().collect { session ->
                if (session != null) {
                    trackedActiveSessionId = session.id
                } else {
                    val endedSessionId = trackedActiveSessionId
                    trackedActiveSessionId = null
                    if (endedSessionId != null) {
                        _pendingSessionSummaryId.value = endedSessionId
                    }
                }
            }
        }
    }

    fun consumeSessionSummaryNavigation() {
        _pendingSessionSummaryId.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userSettingsRepository.completeOnboarding()
            _showOnboarding.value = false
        }
    }

    fun handleShortcutStart(profileId: String) {
        viewModelScope.launch {
            quickStartProfileUseCase.execute(profileId)
        }
    }

    companion object {
        private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
