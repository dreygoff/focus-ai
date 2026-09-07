package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.focus.domain.model.SessionSource

/**
 * AlarmReceiver per TR-05, FR-43.
 * Receives exact alarm events for session start/end.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_SESSION_END_ALARM = "app.focus.service.receiver.ACTION_SESSION_END_ALARM"
        const val ACTION_SESSION_START_ALARM = "app.focus.service.receiver.ACTION_SESSION_START_ALARM"
        const val KEY_SESSION_ID = "sessionId"
        const val EXTRA_SOURCE = "source"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: run {
            Log.w(TAG, "Alarm received without sessionId")
            return
        }
        val action = intent.action

        when (action) {
            ACTION_SESSION_END_ALARM -> handleSessionEnd(context, sessionId)
            ACTION_SESSION_START_ALARM -> handleSessionStart(context, intent, sessionId)
            else -> Log.w(TAG, "Unknown alarm action: $action")
        }
    }

    private fun handleSessionEnd(context: Context, sessionId: String) {
        Log.d(TAG, "Session end alarm for: $sessionId")
        // Per FR-25: session completed via alarm - the FocusForegroundService
        // should have its own internal timer checking. When this alarm fires,
        // update state to COMPLETED and stop foreground service.
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_END_SESSION
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
        }
        android.content.ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleSessionStart(context: Context, intent: Intent, scheduleId: String) {
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: run {
            Log.w(TAG, "Schedule alarm without profileId")
            return
        }
        val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
        
        // Build a dummy session start to trigger service
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_START
            putExtra(FocusForegroundService.EXTRA_PROFILE_ID, profileId)
            putExtra(FocusForegroundService.EXTRA_DURATION, (durationMinutes * 60L * 1000L).toLong())
        }
        android.content.ContextCompat.startForegroundService(context, serviceIntent)
        Log.d(TAG, "Scheduled session started from schedule $scheduleId")
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"
    }
}
