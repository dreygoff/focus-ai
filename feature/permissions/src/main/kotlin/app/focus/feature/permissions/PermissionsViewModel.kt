package app.focus.feature.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.datastore.UserSettingsRepository
import app.focus.domain.model.PermissionType
import app.focus.system.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionsUiState>(PermissionsUiState.Loading)
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    private val _showAccessibilityDisclosure = MutableStateFlow(false)
    val showAccessibilityDisclosure: StateFlow<Boolean> = _showAccessibilityDisclosure.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            try {
                val permissions = permissionChecker.refreshPermissions()
                val items = permissions.map { state ->
                    PermissionCheckItem.from(state.name, state.type, state.granted)
                }
                val mandatoryGranted = permissionChecker.areMandatoryGranted()
                val showRestrictedHint = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    !permissionChecker.isAccessibilityGranted()
                _uiState.value = PermissionsUiState.Ready(
                    permissions = items,
                    mandatoryGranted = mandatoryGranted,
                    showRestrictedSettingsHint = showRestrictedHint,
                )
            } catch (e: Exception) {
                _uiState.value = PermissionsUiState.Error(e.message ?: "Failed to check permissions")
            }
        }
    }

    fun onPermissionClick(permissionId: String) {
        if (permissionId == PermissionChecker.ID_ACCESSIBILITY) {
            viewModelScope.launch {
                if (!userSettingsRepository.isAccessibilityDisclosureAccepted()) {
                    _showAccessibilityDisclosure.value = true
                    return@launch
                }
                permissionChecker.openPermissionSettings(permissionId)
            }
        } else {
            permissionChecker.openPermissionSettings(permissionId)
        }
    }

    fun acceptAccessibilityDisclosure() {
        viewModelScope.launch {
            userSettingsRepository.acceptAccessibilityDisclosure()
            _showAccessibilityDisclosure.value = false
            permissionChecker.openPermissionSettings(PermissionChecker.ID_ACCESSIBILITY)
        }
    }

    fun dismissAccessibilityDisclosure() {
        _showAccessibilityDisclosure.value = false
    }
}

sealed interface PermissionsUiState {
    data object Loading : PermissionsUiState
    data class Ready(
        val permissions: List<PermissionCheckItem>,
        val mandatoryGranted: Boolean,
        val showRestrictedSettingsHint: Boolean = false,
    ) : PermissionsUiState

    data class Error(val message: String) : PermissionsUiState
}
