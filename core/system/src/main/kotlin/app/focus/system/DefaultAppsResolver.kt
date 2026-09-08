package app.focus.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager

class DefaultAppsResolver(private val context: Context) {

    fun launcherpkg(): String? {
        /*
            Resolve the current default launcher package name.
            Returns the package name of the app currently set as the home launcher.
         */
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolved = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName
    }

    fun dialerPackage(): String? {
        return try {
            val telecommManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecommManager.defaultDialerPackage
        } catch (_: Exception) {
            null
        }
    }

    fun imePackage(): String? {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
        } catch (_: Exception) {
            null
        }
    }

    fun currentLauncherIsDefault(launcherPkg: String?): Boolean {
        val currentLauncher = launcherpkg()
        return launcherPkg != null && currentLauncher != null && launcherPkg == currentLauncher
    }
}
