package app.focus.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

internal object PermissionSettingsIntents {

    fun forPermission(context: Context, permissionId: String): Intent? = when (permissionId) {
        PermissionChecker.ID_USAGE_STATS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        PermissionChecker.ID_OVERLAY -> overlayIntent(context)
        PermissionChecker.ID_ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        PermissionChecker.ID_NOTIFICATIONS -> notificationIntent(context)
        PermissionChecker.ID_BATTERY -> batteryIntent(context)
        PermissionChecker.ID_EXACT_ALARM -> exactAlarmIntent(context)
        PermissionChecker.ID_DEVICE_ADMIN -> deviceAdminIntent(context)
        else -> null
    }

    private fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    private fun notificationIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    private fun batteryIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    private fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }

    private fun deviceAdminIntent(context: Context): Intent =
        Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java),
            )
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.permission_device_admin_explanation),
            )
        }
}
