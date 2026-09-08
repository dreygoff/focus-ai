package app.focus.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Resolves packages automatically added to hard lock block list (FR-36, TR-08).
 */
class HardLockExtrasResolver(private val context: Context) {

    private val settingsPackagesResolver = SettingsPackagesResolver(context)

    fun resolve(): HardLockExtrasResult {
        val defaultLauncher = resolveDefaultLauncher()
        val extras = linkedSetOf<String>()

        extras.addAll(settingsPackagesResolver.resolve())
        extras.addAll(INSTALLER_PACKAGES)
        extras.addAll(resolveNonDefaultLaunchers(defaultLauncher))

        return HardLockExtrasResult(
            extraPackages = extras.toList(),
            defaultLauncherPkg = defaultLauncher,
        )
    }

    private fun resolveDefaultLauncher(): String? {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }.getOrNull()
    }

    private fun resolveNonDefaultLaunchers(defaultLauncher: String?): Set<String> {
        return runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .map { it.activityInfo.packageName }
                .filter { pkg ->
                    pkg != defaultLauncher &&
                        pkg != context.packageName &&
                        !pkg.startsWith("com.android.systemui")
                }
                .toSet()
        }.getOrNull().orEmpty()
    }

    data class HardLockExtrasResult(
        val extraPackages: List<String>,
        val defaultLauncherPkg: String?,
    )

    companion object {
        private val INSTALLER_PACKAGES = setOf(
            "com.android.vending",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )

        val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.google.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
        )

        fun isSettingsPackage(packageName: String): Boolean =
            packageName in SETTINGS_PACKAGES || packageName.contains("settings", ignoreCase = true)
    }
}
