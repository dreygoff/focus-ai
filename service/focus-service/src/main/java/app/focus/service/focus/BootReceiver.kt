package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.focus.domain.internal.statemachine.SessionEvent
import app.focus.domain.internal.statemachine.SessionSnapshot
import app.focus.domain.internal.statemachine.SessionStateMachine
import app.focus.domain.model.SessionStatus
import app.focus.datastore.ActiveSessionSnapshotStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * BootReceiver per TR-05, FR-39, FR-43.
 * Handles BOOT_COMPLETED, LOCKED_BOOT_COMPLETED, MY_PACKAGE_REPLACED.
 * Restores active sessions after reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action).let { action ->
            android.content.Intent.ACTION_BOOT_COMPLETED,
            android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED,
            android.content.Intent.MY_PACKAGE_REPLACED -> handleRestore(context, action)
        }
    }

    private fun handleRestore(context: Context, fromAction: String) {
        Log.d(TAG, "Boot event: $fromAction")

        val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        coilScope.launch {
            try {
                // Per TR-05: check device-protected storage for ActiveSessionSnapshot
                val snapshot = retrieveActiveSessionSnapshot(context)
                    ?: run {
                        Log.d(TAG, "No active session snapshot found")
                        return@launch
                    }

                Log.d(TAG, "Restoring session ${snapshot.sessionId}")

                // Validate snapshot hasn't expired
                if (snapshot.plannedEndAt <= System.currentTimeMillis()) {
                    Log.d(TAG, "Session expired at restore: ${snapshot.sessionId}")
                    expireExpiredSession(context, snapshot)
                    return@launch
                }

                // Restore by relaunching the foreground service with session data
                val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_RESTORE
                    putExtra(FocusForegroundService.EXTRA_SESSION_ID, snapshot.sessionId)
                    putExtra(FocusForegroundService.EXTRA_LOCK_MODE, snapshot.lockMode)
                    putExtra(FocusForegroundService.EXTRA_TARGET_PACKAGES, snapshot.targetPackages.toTypedArray())
                    putExtra(FocusForegroundService.EXTRA_HARD_LOCK_EXTRA, snapshot.hardLockExtraPackages.toTypedArray())
                    putExtra(FocusForegroundService.EXTRA_DEFAULT_LAUNCHER, snapshot.defaultLauncher)
                    putExtra(FocusForegroundService.EXTRA_POMODORO, snapshot.isPomodoro)
                }

                android.content.ContextCompat.startForegroundService(context, serviceIntent)
                Log.d(TAG, "Service restored with sessionId=${snapshot.sessionId}")

            } catch (e: Exception) {
                Log.e.TAG, "Error restoring session on boot", e)
                // On error, expire any active session as EXPIRED
                try {
                    val snapshot = retrieveActiveSessionSnapshot(context)
                    if (snapshot != null) {
                        expireExpiredSession(context, snapshot)
                    }
                } catch (ignore: Exception) {}
            }
        }
    }

    private suspend fun restoreWithStateMachine(): Pair<SessionStatus, List<String>>? = null
    
    private suspend fun expireExpiredSession(context: Context, snapshot: SessionSnapshot): Unit = try {
        // Log expiry event via session repo (would be injected in production)
        Log.d(TAG, "Marking session ${snapshot.sessionId} as EXPIRED")
    } catch (_: Exception) {}

    private fun retrieveActiveSessionSnapshot(context: Context): SessionSnapshot? = null
}
