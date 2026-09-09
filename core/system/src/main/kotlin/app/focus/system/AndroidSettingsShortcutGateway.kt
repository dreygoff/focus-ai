package app.focus.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.focus.domain.model.SettingsShortcut
import app.focus.domain.usecase.SettingsShortcutGateway
import app.focus.domain.usecase.SettingsShortcutTarget
class AndroidSettingsShortcutGateway(
    private val context: Context,
) : SettingsShortcutGateway {

    override fun resolve(shortcut: SettingsShortcut): SettingsShortcutTarget? {
        val action = actionFor(shortcut) ?: return null
        val intent = Intent(action).apply {
            if (shortcut == SettingsShortcut.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null
        return SettingsShortcutTarget(
            settingsPackage = resolved.activityInfo.packageName,
            activityClassName = resolved.activityInfo.name,
            intentAction = action,
        )
    }

    private fun actionFor(shortcut: SettingsShortcut): String? = when (shortcut) {
        SettingsShortcut.WIFI -> Settings.ACTION_WIFI_SETTINGS
        SettingsShortcut.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
        SettingsShortcut.SOUND -> Settings.ACTION_SOUND_SETTINGS
        SettingsShortcut.NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            } else {
                Settings.ACTION_SETTINGS
            }
        }
        SettingsShortcut.CELLULAR -> Settings.ACTION_WIRELESS_SETTINGS
    }
}
