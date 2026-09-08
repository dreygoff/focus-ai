package app.focus.system

import android.Manifest.permission
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat.checkSelfPermission

internal object PermissionGrantChecks {

    private const val ACCESSIBILITY_SERVICE_CLASS =
        "app.focus.service.accessibility.FocusAccessibilityService"

    fun usageStatsGranted(context: Context): Boolean {
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

    fun overlayGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    fun accessibilityGranted(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val expected = ComponentName(context.packageName, ACCESSIBILITY_SERVICE_CLASS).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun notificationsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(context, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun batteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    fun exactAlarmsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.canScheduleExactAlarms() == true ||
                checkSelfPermission(context, permission.USE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun deviceAdminActive(context: Context): Boolean {
        val adminComponent = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && devicePolicyManager != null) {
            devicePolicyManager.isAdminActive(adminComponent)
        } else {
            false
        }
    }
}
