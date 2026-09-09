package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AddPackageToProfileReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ADD_PACKAGE) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    PackageChangeEntryPoint::class.java,
                )
                entry.handlePackageAddedUseCase().addPackageToActiveProfile(packageName)
                PackageAddedNotificationFactory.cancel(context, packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADD_PACKAGE = "app.focus.service.action.ADD_PACKAGE_TO_PROFILE"
        const val EXTRA_PACKAGE_NAME = "packageName"
    }
}
