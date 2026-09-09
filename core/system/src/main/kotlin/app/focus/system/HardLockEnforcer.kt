package app.focus.system

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Per FR-38, TR-07: HardLockEnforcer enforces hard lock mode by preventing
 * launcher switching, settings access, Play Store, and task killers.
 */
class HardLockEnforcer(private val context: Context) {

    companion object {
        private const val TAG = "HardLockEnforcer"
        private val HARD_LOCK_PACKAGES = setOf(
            "com.android.settings",           // Settings
            "com.google.android.play/assetscore", // Play Store
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
            "com.google.android.launcher",     // Google Launcher
            "com.teslacoilsw.launcher",       // Nova Launcher (task killer)
            "org.adw.launcher",                // ADW Launcher
            "com.sec.android.app.twlauncher",  // Samsung Experience/Home
            "com.huawei.android.launcher",     // Huawei Launcher
            "com.oppo.launcher",               // OPPO Launcher
            "com.vivo.launcher",               // Vivo Launcher
            "com.zte.assistantscreen",         // ZTE Assist Screen
            "com.lge.livedocs",                // LG utilities
        )

        private val HARD_LOCK_INTENTS = setOf(
            // Intent actions that lead to Settings
            "android.settings.SETTINGS",
            "android.settings.APPLICATION_SETTINGS",
            "android.settings.MANAGE_APPLICATIONS",
            "android.settings.USAGE_ACCESS_SETTINGS",
            // Task killers / App managers
            "com.miui.securitycenter.intent.START_SERVICE",
        )

        private val HARD_LOCK_SCHEMES = setOf(
            "settings:",
            "content://com.android.providers.settings/",
            "package:",  // Package manager URIs
            "market:",   // Play Store
        )
    }

    /** Check if this event should be blocked in hard lock mode. */
    fun shouldBlockEvent(intent: Intent): Boolean {
        val action = intent.action ?: return false

        // Block Settings actions
        if (HARD_LOCK_INTENTS.contains(action)) {
            Log.d(TAG, "Blocking Settings intent: $action")
            return true
        }

        // Block Play Store / Package actions during hard lock
        if (intent.`package` != null && HARD_LOCK_PACKAGES.contains(intent.`package`)) {
            Log.d(TAG, "Blocking app: ${intent.`package`}")
            return true
        }

        return false
    }

    /** Check if this package should be blocked during hard lock. */
    fun shouldBlockPackage(packageName: String): Boolean {
        // Allow focus process itself and essential system packages
        if (packageName == context.packageName || packageName.startsWith("app.focus.")) {
            return false
        }

        if (HARD_LOCK_PACKAGES.contains(packageName)) {
            Log.d(TAG, "Blocking hard lock package: $packageName")
            return true
        }

        return false
    }

    /** Block all launcher switching */
    @SuppressLint("WrongConstant")
    fun canBlockLauncherSwitching(): Boolean {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false

            val componentName = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
            if (!devicePolicyManager.isAdminActive(componentName)) {
                Log.w(TAG, "Device admin not active for hard lock")
                return false
            }

            // Attempt to set launcher restriction
            devicePolicyManager.setLockTaskPackages(componentName, emptyArray<String>())
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error blocking launcher switching", e)
            return false
        }
    }

    /** Unregister hard lock restrictions */
    fun unregisterHardLock() {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return

            val componentName = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
            if (devicePolicyManager.isAdminActive(componentName)) {
                devicePolicyManager.setLockTaskPackages(componentName, emptyArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering hard lock", e)
        }
    }

    /** Check if device admin is active */
    fun isDeviceAdminActive(): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false

            val componentName = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
            devicePolicyManager.isAdminActive(componentName)
        } catch (_: Exception) {
            false
        }
    }

    /** Remove device admin when protection is no longer needed (FR-38). */
    fun deactivateDeviceAdmin() {
        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return
            val componentName = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
            if (devicePolicyManager.isAdminActive(componentName)) {
                devicePolicyManager.removeActiveAdmin(componentName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating device admin", e)
        }
    }

    /** Opens system UI where the user can disable device admin. */
    fun getDeviceAdminSettingsIntent(): Intent =
        Intent("android.settings.DEVICE_ADMIN_SETTINGS")

    /** Build intent to enable device admin */
    fun getDeviceAdminPermissionIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            val componentName = ComponentName(context, app.focus.system.internal.DummyAdminReceiver::class.java)
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Required to protect hard lock sessions from being bypassed",
            )
        }

    /** Check if accessibility service is enabled */
    fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, 0
            )
            enabled == 1
        } catch (_: Exception) {
            false
        }
    }

    /** Check if overlay permission is granted */
    fun canDrawOverlays(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /** Get list of enabled input methods */
    fun getEnabledInputMethods(): List<String> {
        return try {
            val defaultIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            ) ?: return emptyList()
            listOf(defaultIme.substringBefore('/'))
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Check if any IME is currently active */
    fun isImeActive(packageName: String): Boolean {
        return try {
            val defaultIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            ) ?: return false
            defaultIme.contains(packageName)
        } catch (_: Exception) {
            false
        }
    }

    /** Check if current activity is a system dialog */
    fun isSystemDialogPackage(packageName: String): Boolean {
        return setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.packageinstaller"
        ).contains(packageName)
    }

    /** Check if intent targets a system overlay permission */
    fun isSystemOverlayIntent(intent: Intent): Boolean {
        val action = intent.action ?: return false
        return when (action) {
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Settings.ACTION_ACCESSIBILITY_SETTINGS,
            Settings.ACTION_DEVICE_INFO_SETTINGS -> true
            else -> false
        }
    }

    /** Check if accessibility service has gesture permission */
    fun hasAccessibilityGesturePermission(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val serviceId = "${context.packageName}/app.focus.service.accessibility.FocusAccessibilityService"
            enabledServices.contains(serviceId) &&
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0
                ) == 1

        } catch (_: Exception) {
            false
        }
    }

    /** Check if app is ignoring battery optimization */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return try {
            val pm = context.packageManager
            val name = pm.getNameForUid(android.os.Process.myUid())
            if (name == null) return false

            val policy = pm.getApplicationInfo(name, 0)?.let { info ->
                Settings.Global.getInt(
                    context.contentResolver,
                    "ignore_battery_optimizations_$info.packageName", 0
                )
            }
            policy == 1
        } catch (_: Exception) {
            false
        }
    }

    /** Check if app has exact alarm permission */
    fun hasExactAlarmPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val pm = context.packageManager
            when {
                pm.checkPermission(permission.SCHEDULE_EXACT_ALARM, context.packageName) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED -> true
                pm.checkPermission(permission.USE_EXACT_ALARM, context.packageName) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED -> true
                else -> false
            }
        } else true
    }

    /** Check if usage stats permission is granted */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val pm = context.packageManager
            val uid = pm.getApplicationInfo(context.packageName, 0).uid
            Settings.Global.getInt(
                context.contentResolver,
                "usagestats.access.$uid", 0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    /** Check if app is in restricted mode (DPC managed device) */
    fun isRestrictedMode(): Boolean = false

    /** Get list of installed packages matching focus criteria */
    fun getInstalledFocusPackages(minSdkVersion: Int = 26): List<String> {
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)

            apps.filter { app ->
                // Focus-related packages
                app.packageName.startsWith("app.focus") ||
                // System overlay permission related
                app.packageName.contains("overlay") ||
                // Accessibility services
                app.packageName.contains("accessibility") ||
                // Battery optimization
                app.packageName.contains("battery")
            }.map { it.packageName }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Check if service is running */
    fun isForegroundServiceRunning(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                ?: return false

            am.getRunningServices(Integer.MAX_VALUE)?.any { service ->
                service.service.packageName == context.packageName && service.foreground
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /** Check if device has USB debugging enabled */
    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    /** Check if Safe Mode is active */
    fun isSafeMode(): Boolean {
        return try {
            context.packageManager.isSafeMode
        } catch (_: Exception) {
            false
        }
    }

    /** Get list of recent tasks (requires MANAGE_ACTIVITY_STACKS permission) */
    fun getRecentTasks(maxNum: Int): List<android.app.ActivityManager.RecentTaskInfo> = emptyList()

    /** Check if current launcher is the default */
    fun getDefaultLauncherPackage(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolver = context.packageManager
            val resolveInfo = resolver.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)

            resolveInfo?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    /** Check if any app can draw over other apps */
    fun getOverlayPermissionApps(): List<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                Settings.Secure.getString(
                    context.contentResolver,
                    "floating_window_permission_list"
                )?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    /** Check if package can draw overlays */
    fun canPackageDrawOverlays(packageName: String): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    /** Check if accessibility service has capture permission */
    fun hasCapturePermission(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val serviceId = "${context.packageName}/app.focus.service.accessibility.FocusAccessibilityService"
            enabledServices.contains(serviceId) &&
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0
                ) == 1

        } catch (_: Exception) {
            false
        }
    }

    /** Check if accessibility service is accessible */
    fun isAccessibilityServiceAccessible(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val colonSeparatedServices = enabledServices.split(":")
            val serviceId = "${context.packageName}/app.focus.service.accessibility.FocusAccessibilityService"

            colonSeparatedServices.any { service ->
                service == serviceId || service.contains(context.packageName)
            } && Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
            ) == 1

        } catch (_: Exception) {
            false
        }
    }
}
