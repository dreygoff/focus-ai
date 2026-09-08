package app.focus.system

import android.content.Context
import android.content.Intent
import app.focus.domain.model.PermissionState
import app.focus.domain.model.PermissionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Checks and tracks Focus runtime permissions (FR-02).
 */
class PermissionChecker(private val context: Context) {

    companion object {
        const val ID_USAGE_STATS = "usage_stats"
        const val ID_OVERLAY = "overlay"
        const val ID_ACCESSIBILITY = "accessibility"
        const val ID_NOTIFICATIONS = "notifications"
        const val ID_BATTERY = "battery"
        const val ID_EXACT_ALARM = "exact_alarm"
        const val ID_DEVICE_ADMIN = "device_admin"
    }

    private val _permissions = MutableStateFlow(emptySet<PermissionState>())
    val permissions: StateFlow<Set<PermissionState>> = _permissions

    init {
        refreshPermissions()
    }

    fun refreshPermissions(): Set<PermissionState> {
        val states = linkedSetOf(
            permissionState(ID_USAGE_STATS, PermissionGrantChecks.usageStatsGranted(context), PermissionType.MANDATORY),
            permissionState(ID_OVERLAY, PermissionGrantChecks.overlayGranted(context), PermissionType.MANDATORY),
            permissionState(ID_ACCESSIBILITY, PermissionGrantChecks.accessibilityGranted(context), PermissionType.RECOMMENDED),
            permissionState(ID_NOTIFICATIONS, PermissionGrantChecks.notificationsGranted(context), PermissionType.RECOMMENDED),
            permissionState(ID_BATTERY, PermissionGrantChecks.batteryOptimizationIgnored(context), PermissionType.RECOMMENDED),
            permissionState(ID_EXACT_ALARM, PermissionGrantChecks.exactAlarmsGranted(context), PermissionType.RECOMMENDED),
            permissionState(ID_DEVICE_ADMIN, PermissionGrantChecks.deviceAdminActive(context), PermissionType.OPTIONAL),
        )
        _permissions.value = states
        return states
    }

    private fun permissionState(id: String, granted: Boolean, type: PermissionType) =
        PermissionState(name = id, granted = granted, type = type)

    fun areMandatoryGranted(): Boolean =
        _permissions.value.all { it.granted || it.type != PermissionType.MANDATORY }

    fun isAccessibilityGranted(): Boolean =
        _permissions.value.any { it.name == ID_ACCESSIBILITY && it.granted }

    val mandatoryPermissions: Flow<Set<PermissionState>> by lazy {
        permissions.map { perms -> perms.filter { it.type == PermissionType.MANDATORY }.toSet() }
    }

    fun settingsIntent(permissionId: String): Intent? =
        PermissionSettingsIntents.forPermission(context, permissionId)

    fun openPermissionSettings(permissionId: String) {
        settingsIntent(permissionId)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
