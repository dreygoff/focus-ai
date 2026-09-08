package app.focus.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.CreateProfileUseCase
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val createProfileUseCase: CreateProfileUseCase,
    observeActiveSessionsUseCase: ObserveActiveSessionsUseCase,
) : ViewModel() {

    data class UiState(
        val profiles: List<Profile> = emptyList(),
        val isLoading: Boolean = true,
        val editingLocked: Boolean = false,
        val editingLockedReason: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                profileRepository.observeProfiles(),
                observeActiveSessionsUseCase.execute(),
            ) { profiles, activeSession ->
                val hardLockActive = activeSession?.lockMode is LockMode.Hard &&
                    activeSession.status is SessionStatus.Running
                UiState(
                    profiles = profiles,
                    isLoading = false,
                    editingLocked = hardLockActive,
                    editingLockedReason = if (hardLockActive) {
                        "Profile editing is disabled during an active hard lock session."
                    } else {
                        null
                    },
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteProfile(id: String) {
        if (_uiState.value.editingLocked) return
        viewModelScope.launch {
            profileRepository.delete(id)
        }
    }

    fun createProfile(name: String, lockMode: LockMode) {
        if (_uiState.value.editingLocked) return
        viewModelScope.launch {
            createProfileUseCase.execute(
                name = name,
                emoji = "🎯",
                colorArgb = 0xFF2F6F6D.toInt(),
                lockMode = lockMode,
            )
        }
    }
}
