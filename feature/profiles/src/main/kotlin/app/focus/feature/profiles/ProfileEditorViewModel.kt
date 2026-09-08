package app.focus.feature.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    observeActiveSessionsUseCase: ObserveActiveSessionsUseCase,
) : ViewModel() {

    private val profileId: String? = savedStateHandle.get<String>("profileId")

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _editingLocked = MutableStateFlow(false)
    val editingLocked: StateFlow<Boolean> = _editingLocked.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveSessionsUseCase.execute().collect { session ->
                _editingLocked.value = session?.lockMode is LockMode.Hard &&
                    session.status is SessionStatus.Running
            }
        }
        profileId?.let { id ->
            viewModelScope.launch {
                _profile.value = profileRepository.getProfile(id)
            }
        }
    }

    fun save(profile: Profile) {
        if (_editingLocked.value) return
        viewModelScope.launch {
            if (profileId == null) {
                profileRepository.insert(profile)
            } else {
                profileRepository.update(profile.copy(id = profileId))
            }
        }
    }
}
