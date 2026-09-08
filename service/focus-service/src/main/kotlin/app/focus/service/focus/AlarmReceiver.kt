package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import app.focus.domain.usecase.GrantBypassUseCase

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_SESSION_END_ALARM = "app.focus.service.focus.ACTION_SESSION_END_ALARM"
        const val ACTION_SESSION_START_ALARM = "app.focus.service.focus.ACTION_SESSION_START_ALARM"
        const val ACTION_ACCESS_WINDOW_WARNING = GrantBypassUseCase.ACTION_ACCESS_WINDOW_WARNING
        const val ACTION_ACCESS_WINDOW_EXPIRED = GrantBypassUseCase.ACTION_ACCESS_WINDOW_EXPIRED
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_PACKAGE_NAME = GrantBypassUseCase.KEY_PACKAGE_NAME
        const val EXTRA_SOURCE = "source"
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SESSION_END_ALARM -> {
                val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: run {
                    Log.w(TAG, "Session end alarm without sessionId")
                    return
                }
                handleSessionEnd(context, sessionId)
            }
            ACTION_SESSION_START_ALARM -> {
                val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: run {
                    Log.w(TAG, "Schedule alarm without sessionId")
                    return
                }
                handleSessionStart(context, intent, sessionId)
            }
            ACTION_ACCESS_WINDOW_WARNING -> handleAccessWindowWarning(context, intent)
            ACTION_ACCESS_WINDOW_EXPIRED -> handleAccessWindowExpired(context, intent)
            else -> Log.w(TAG, "Unknown alarm action: ${intent.action}")
        }
    }

    private fun handleSessionEnd(context: Context, sessionId: String) {
        Log.d(TAG, "Session end alarm for: $sessionId")
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_END_SESSION
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleSessionStart(context: Context, intent: Intent, scheduleId: String) {
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: run {
            Log.w(TAG, "Schedule alarm without profileId")
            return
        }
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START
            putExtra(FocusForegroundService.EXTRA_PROFILE_ID, profileId)
            putExtra(FocusForegroundService.EXTRA_DURATION, durationMinutes * 60L * 1000L)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
        Log.d(TAG, "Scheduled session started from schedule $scheduleId")
    }

    private fun handleAccessWindowWarning(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(KEY_PACKAGE_NAME) ?: return
        AccessWindowNotificationFactory.showWarning(context, packageName)
    }

    private fun handleAccessWindowExpired(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: return
        val packageName = intent.getStringExtra(KEY_PACKAGE_NAME) ?: return
        AccessWindowNotificationFactory.cancelAccessWindowNotification(context, packageName)
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_ACCESS_WINDOW_EXPIRED
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
            putExtra(FocusForegroundService.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
