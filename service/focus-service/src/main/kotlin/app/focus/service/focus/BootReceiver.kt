package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import app.focus.datastore.ProtoActiveSessionSnapshotStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> handleRestore(context, intent.action ?: "unknown")
        }
    }

    private fun handleRestore(context: Context, fromAction: String) {
        Log.d(TAG, "Boot event: $fromAction")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val snapshotStore = ProtoActiveSessionSnapshotStorage.create(context)
                val snapshot = snapshotStore.load() ?: run {
                    Log.d(TAG, "No active session snapshot found")
                    return@launch
                }

                if (snapshot.plannedEndAtMillis <= System.currentTimeMillis()) {
                    Log.d(TAG, "Session expired at restore: ${snapshot.sessionId}")
                    expireExpiredSession(snapshotStore, snapshot.sessionId)
                    return@launch
                }

                val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_RESTORE
                    putExtra(FocusForegroundService.EXTRA_SESSION_ID, snapshot.sessionId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d(TAG, "Service restored with sessionId=${snapshot.sessionId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring session on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun expireExpiredSession(
        snapshotStore: app.focus.domain.usecase.ActiveSessionSnapshotStorage,
        sessionId: String,
    ) {
        Log.d(TAG, "Marking session $sessionId as EXPIRED")
        snapshotStore.clear()
    }
}
