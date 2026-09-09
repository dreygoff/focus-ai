package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.focus.domain.usecase.HandlePackageAddedUseCase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Invalidates installed-app cache and handles [FR-15] when a package is added during a session.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!isPackageChangeAction(action)) return

        val packageName = intent.data?.schemeSpecificPart
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    PackageChangeEntryPoint::class.java,
                )
                entry.packageRepository().clearCache()

                if (isPackageAddedAction(action) && packageName != null) {
                    handlePackageAdded(context, entry.handlePackageAddedUseCase(), packageName)
                } else if (packageName != null) {
                    PackageAddedNotificationFactory.cancel(context, packageName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePackageAdded(
        context: Context,
        useCase: HandlePackageAddedUseCase,
        packageName: String,
    ) {
        when (val result = useCase.execute(packageName)) {
            is HandlePackageAddedUseCase.Result.AutoBlocked -> {
                Log.d(TAG, "Auto-blocked new package: ${result.packageName}")
            }
            is HandlePackageAddedUseCase.Result.PromptToAdd -> {
                val label = resolveAppLabel(context, result.packageName)
                PackageAddedNotificationFactory.showPromptToAdd(context, result.packageName, label)
            }
            HandlePackageAddedUseCase.Result.AlreadyInProfile -> Unit
            HandlePackageAddedUseCase.Result.NoActiveSession -> Unit
        }
    }

    private fun resolveAppLabel(context: Context, packageName: String): String =
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)

    companion object {
        private const val TAG = "PackageChangeReceiver"
        const val ACTION_PACKAGE_ADDED_DEBUG = "app.focus.service.debug.PACKAGE_ADDED"

        private fun isPackageChangeAction(action: String): Boolean =
            action == Intent.ACTION_PACKAGE_ADDED ||
                action == Intent.ACTION_PACKAGE_REMOVED ||
                action == Intent.ACTION_PACKAGE_REPLACED ||
                action == Intent.ACTION_PACKAGE_CHANGED ||
                action == ACTION_PACKAGE_ADDED_DEBUG

        private fun isPackageAddedAction(action: String): Boolean =
            action == Intent.ACTION_PACKAGE_ADDED || action == ACTION_PACKAGE_ADDED_DEBUG
    }
}
