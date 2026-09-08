package app.focus.system

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

data class PackageInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val usageMinutesLast7Days: Long = 0L
)

interface PackageRepository {
    fun observeInstalledApps(): Flow<List<PackageInfo>>
    suspend fun clearCache()
}

class DefaultPackageRepository(
    private val context: Context,
) : PackageRepository {

    companion object {
        private const val CACHE_KEY = "installed_packages"
        private const val USAGE_DAYS_BACK = 7
    }

    private val _appsFlow = MutableStateFlow<List<PackageInfo>>(emptyList())

    override fun observeInstalledApps(): Flow<List<PackageInfo>> = _appsFlow

    init {
        refresh(includeUsage = true)
    }

    override suspend fun clearCache() {
        synchronized(this) {
            _cachedPackages = emptyList()
            updateFlow(emptyList())
        }
    }

    private var _cachedPackages: List<PackageInfo> = emptyList()

    private fun refresh(includeUsage: Boolean = false) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        /*
            Uses PackageManager.queryIntentActivities with Intent.ACTION_MAIN and CATEGORY_LAUNCHER.
            Returns a Flow<List<PackageInfo>> where PackageInfo includes packageName, appName, isSystemApp.
         */
        val resolvedActivities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val apps: List<PackageInfo> = resolvedActivities.map { ri ->
            val pkg = ri.activityInfo.packageName
            val name = ri.loadLabel(context.packageManager).toString()
            val isSys = hasFlag(pkg, ApplicationInfo.FLAG_SYSTEM)
            PackageInfo(
                packageName = pkg,
                appName = name,
                isSystemApp = isSys,
                usageMinutesLast7Days = if (includeUsage) queryUsageMinutes(pkg) else 0L,
            )
        }.distinctBy { it.packageName }

        synchronized(this) {
            _cachedPackages = apps.sortedBy { it.appName.lowercase() }
            _appsFlow.value = _cachedPackages
        }
    }

    private fun updateFlow(list: List<PackageInfo>) {
        _appsFlow.value = list
    }

    private fun hasFlag(pkg: String, flag: Int): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
            (appInfo.flags and flag) != 0
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryUsageMinutes(pkgName: String): Long {
        @Suppress("UNCHECKED_CAST")
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (USAGE_DAYS_BACK * 24L * 60 * 60 * 1000)

        /*
            Queries UsageStatsManager for usage stats over the past 7 days.
            Returns total foreground time in minutes for the given package.
         */
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        return (stats.orEmpty().find { it.packageName == pkgName }?.totalTimeInForeground?.div(60_000L)) ?: 0L
    }
}
