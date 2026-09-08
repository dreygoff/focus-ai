package app.focus.feature.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.system.HardLockEnforcer
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
    private val hardLockEnforcer: HardLockEnforcer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionsUiState>(PermissionsUiState.Loading)
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

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
                val mandatoryGranted = permissions.all {
                    it.granted || it.type != app.focus.domain.model.PermissionType.MANDATORY
                }
                _uiState.value = PermissionsUiState.Ready(
                    permissions = items,
                    mandatoryGranted = mandatoryGranted,
                )
            } catch (e: Exception) {
                _uiState.value = PermissionsUiState.Error(e.message ?: "Failed to check permissions")
            }
        }
    }

    fun openPermissionSettings(permissionType: String) {
        // Settings intents are launched from the UI layer.
    }
}

sealed interface PermissionsUiState {
    data object Loading : PermissionsUiState
    data class Ready(
        val permissions: List<PermissionCheckItem>,
        val mandatoryGranted: Boolean,
    ) : PermissionsUiState

    data class Error(val message: String) : PermissionsUiState
}
