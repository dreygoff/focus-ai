package app.focus.system

import android.Manifest.permission
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat.checkSelfPermission
import app.focus.domain.model.PermissionState
import app.focus.domain.model.PermissionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class PermissionChecker(private val context: Context) {

    companion object {
        private const val PACKAGE_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS"
        private const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        private const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS =
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    }

    private val _permissions = MutableStateFlow(emptySet<PermissionState>())
    val permissions: StateFlow<Set<PermissionState>> = _permissions

    init {
        refreshPermissions()
    }

    fun refreshPermissions(): Set<PermissionState> {
        val states = mutableSetOf<PermissionState>()

        states += PermissionState(
            name = PACKAGE_USAGE_STATS,
            granted = checkPackageUsageStatsInternal(),
            type = PermissionType.MANDATORY
        )

        states += PermissionState(
            name = SYSTEM_ALERT_WINDOW,
            granted = canDrawOverlays(),
            type = PermissionType.MANDATORY
        )

        val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        states += PermissionState(
            name = POST_NOTIFICATIONS,
            granted = postNotificationsGranted,
            type = PermissionType.RECOMMENDED
        )

        states += PermissionState(
            name = REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            granted = canIgnoreBatteryOptimizationsInternal(),
            type = PermissionType.RECOMMENDED
        )

        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(context, permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(context, permission.USE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        states += PermissionState(
            name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "android.permission.USE_EXACT_ALARM"
            } else {
                "android.permission.SCHEDULE_EXACT_ALARM"
            },
            granted = exactAlarmGranted,
            type = PermissionType.RECOMMENDED
        )

        states += PermissionState(
            name = permission.BIND_ACCESSIBILITY_SERVICE,
            granted = isAccessibilityEnabled(),
            type = PermissionType.MANDATORY
        )

        val adminComponent = android.content.ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val isAdminActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && devicePolicyManager != null) {
            devicePolicyManager.isAdminActive(adminComponent)
        } else false

        states += PermissionState(
            name = "DEVICE_ADMIN",
            granted = isAdminActive,
            type = PermissionType.OPTIONAL
        )

        _permissions.value = states
        return states
    }

    val mandatoryPermissions: Flow<Set<PermissionState>> by lazy {
        permissions.map { perms -> perms.filter { it.type == PermissionType.MANDATORY }.toSet() }
    }

    val recommendedPermissions: Flow<Set<PermissionState>> by lazy {
        permissions.map { perms -> perms.filter { it.type == PermissionType.RECOMMENDED }.toSet() }
    }

    fun checkPackageUsageStats(): Boolean = checkPackageUsageStatsInternal()

    fun canIgnoreBatteryOptimizations(): Boolean = canIgnoreBatteryOptimizationsInternal()

    private fun checkPackageUsageStatsInternal(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            if (appOps != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                ) == android.app.AppOpsManager.MODE_ALLOWED
            } else {
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager != null
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun canIgnoreBatteryOptimizationsInternal(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServices.isNotEmpty() ||
            checkSelfPermission(context, permission.BIND_ACCESSIBILITY_SERVICE) == PackageManager.PERMISSION_GRANTED
    }
}
