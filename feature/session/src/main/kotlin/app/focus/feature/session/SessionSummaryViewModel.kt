package app.focus.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.usecase.GetCurrentStreakUseCase
import app.focus.domain.usecase.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getCurrentStreakUseCase: GetCurrentStreakUseCase,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val session: app.focus.domain.model.Session? = null,
        val streakDays: Int = 0,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(sessionId: String) {
        viewModelScope.launch {
            val session = sessionRepository.getSession(sessionId)
            val streak = runCatching { getCurrentStreakUseCase.execute() }.getOrDefault(0)
            _uiState.value = UiState(
                isLoading = false,
                session = session,
                streakDays = streak,
            )
        }
    }
}
