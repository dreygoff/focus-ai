package app.focus.feature.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import app.focus.system.HardLockEnforcer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProtectionInfoViewModel @Inject constructor(
    private val hardLockEnforcer: HardLockEnforcer,
) : ViewModel() {

    private val _deviceAdminActive = MutableStateFlow(hardLockEnforcer.isDeviceAdminActive())
    val deviceAdminActive: StateFlow<Boolean> = _deviceAdminActive.asStateFlow()

    fun refreshDeviceAdminStatus() {
        _deviceAdminActive.value = hardLockEnforcer.isDeviceAdminActive()
    }

    fun deactivateDeviceAdmin() {
        hardLockEnforcer.deactivateDeviceAdmin()
        refreshDeviceAdminStatus()
    }

    fun deviceAdminSettingsIntent(): Intent = hardLockEnforcer.getDeviceAdminSettingsIntent()
}
