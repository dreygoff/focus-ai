package app.focus.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.datastore.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    private val _showOnboarding = MutableStateFlow<Boolean?>(null)
    val showOnboarding: StateFlow<Boolean?> = _showOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            _showOnboarding.value = !userSettingsRepository.isOnboardingCompleted()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userSettingsRepository.completeOnboarding()
            _showOnboarding.value = false
        }
    }
}
