package app.focus.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

class SettingsPackagesResolver(private val context: Context) {

    companion object {
        private const val SETTINGS_PACKAGE = "com.android.settings"
    }

    fun resolve(): Set<String> {
        val result = mutableSetOf<String>()

        // Settings.ACTION_SETTINGS - general settings
        resolveIntent(Settings.ACTION_SETTINGS)?.let { result += it }

        // Settings.ACTION_ACCESSIBILITY_SETTINGS - accessibility settings
        resolveIntent(Settings.ACTION_ACCESSIBILITY_SETTINGS)?.let { result += it }

        // Settings.ACTION_APPLICATION_DETAILS_SETTINGS - app details for specific package
        resolveApplicationDetailsSettings()?.let { result += it }

        // Settings.ACTION_MANAGE_OVERLAY_PERMISSION - system alert window settings
        resolveIntent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)?.let { result += it }

        // Settings.ACTION_USAGE_ACCESS_SETTINGS - usage access settings
        resolveIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS)?.let { result += it }

        return result
    }

    fun resolveAccessibilitySettings(): String? = resolveIntent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun resolveOverlayPermissionSettings(): String? = resolveIntent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

    fun resolveUsageAccessSettings(): String? = resolveIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun resolveApplicationDetails(packageName: String): String? {
        return try {
            val intent = Intent(Settings.APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
            val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                SETTINGS_PACKAGE
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveIntent(action: String): String? {
        return try {
            val intent = Intent(action)
            val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                SETTINGS_PACKAGE
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveApplicationDetailsSettings(): String? {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                SETTINGS_PACKAGE
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
