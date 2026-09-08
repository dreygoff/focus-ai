package app.focus.service.focus

import android.app.Service
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import app.focus.domain.model.SessionStatus
import app.focus.domain.usecase.StopSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class FocusSessionLifecycleHandler(
    private val scope: CoroutineScope,
    private val stopSessionUseCase: StopSessionUseCase,
    private val onRuntimeStopped: () -> Unit,
) {

    fun endSession(sessionId: String) {
        scope.launch {
            runCatching {
                stopSessionUseCase.execute(
                    sessionId = sessionId,
                    status = SessionStatus.Completed,
                    stopForegroundService = false,
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to complete session $sessionId", error)
            }
            onRuntimeStopped()
        }
    }

    fun cancelSession(sessionId: String) {
        scope.launch {
            runCatching {
                stopSessionUseCase.execute(
                    sessionId = sessionId,
                    status = SessionStatus.Cancelled,
                    stopForegroundService = false,
                )
            }.onFailure { error ->
                Log.e(TAG, "Failed to cancel session $sessionId", error)
            }
            onRuntimeStopped()
        }
    }

    companion object {
        private const val TAG = "FocusSessionLifecycle"
    }
}

internal fun FocusForegroundService.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            FocusForegroundService.CHANNEL_SESSION,
            getString(R.string.notification_channel_sessions),
            android.app.NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }
}
