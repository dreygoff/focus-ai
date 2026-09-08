package app.focus.feature.permissions

import app.focus.domain.model.PermissionType
import app.focus.system.PermissionChecker

data class PermissionCheckItem(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val isGranted: Boolean,
    val permissionType: PermissionType,
) {
    val isOptional: Boolean
        get() = permissionType != PermissionType.MANDATORY

    companion object {
        fun from(id: String, type: PermissionType, granted: Boolean): PermissionCheckItem {
            val (title, description) = titleAndDescription(id)
            return PermissionCheckItem(
                id = id,
                titleRes = title,
                descriptionRes = description,
                isGranted = granted,
                permissionType = type,
            )
        }

        private fun titleAndDescription(id: String): Pair<Int, Int> = when (id) {
            PermissionChecker.ID_USAGE_STATS ->
                R.string.permission_usage_stats_title to R.string.permission_usage_stats_desc
            PermissionChecker.ID_OVERLAY ->
                R.string.permission_overlay_title to R.string.permission_overlay_desc
            PermissionChecker.ID_ACCESSIBILITY ->
                R.string.permission_accessibility_title to R.string.permission_accessibility_desc
            PermissionChecker.ID_NOTIFICATIONS ->
                R.string.permission_notifications_title to R.string.permission_notifications_desc
            PermissionChecker.ID_BATTERY ->
                R.string.permission_battery_title to R.string.permission_battery_desc
            PermissionChecker.ID_EXACT_ALARM ->
                R.string.permission_exact_alarm_title to R.string.permission_exact_alarm_desc
            PermissionChecker.ID_DEVICE_ADMIN ->
                R.string.permission_device_admin_title to R.string.permission_device_admin_desc
            else -> R.string.permissions_title to R.string.permissions_missing
        }
    }
}
