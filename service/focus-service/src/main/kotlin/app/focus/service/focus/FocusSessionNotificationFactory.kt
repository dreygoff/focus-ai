package app.focus.service.focus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

internal object FocusSessionNotificationFactory {

    private const val MS_PER_MINUTE = 60_000L

    data class Params(
        val context: Context,
        val profileName: String,
        val plannedEndAtMillis: Long,
        val activeSessionId: String?,
        val pomodoroPhase: String? = null,
        val phaseEndAtMillis: Long = 0L,
    )

    fun build(params: Params): NotificationCompat.Builder = with(params) {
        val stopPending = stopPendingIntent(context, activeSessionId)
        val openPending = FocusForegroundService.makeOpenPendingIntent(context)
        val remainingText = remainingText(context, profileName, plannedEndAtMillis, pomodoroPhase, phaseEndAtMillis)
        val chronometerEnd = chronometerEnd(plannedEndAtMillis, pomodoroPhase, phaseEndAtMillis)

        NotificationCompat.Builder(context, FocusForegroundService.CHANNEL_SESSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_session_title))
            .setContentText(remainingText)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(chronometerEnd > 0L)
            .apply {
                if (chronometerEnd > 0L) {
                    setWhen(chronometerEnd)
                    setShowWhen(true)
                }
            }
            .addAction(
                android.R.drawable.ic_menu_revert,
                context.getString(R.string.notification_stop),
                stopPending,
            )
    }

    private fun stopPendingIntent(context: Context, activeSessionId: String?): PendingIntent {
        val stopIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_USER_STOP
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, activeSessionId)
        }
        return PendingIntent.getService(
            context,
            REQUEST_STOP,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun remainingText(
        context: Context,
        profileName: String,
        plannedEndAtMillis: Long,
        pomodoroPhase: String?,
        phaseEndAtMillis: Long,
    ): String = when {
        isBreakPhase(pomodoroPhase) -> {
            context.getString(R.string.notification_pomodoro_break, profileName)
        }
        pomodoroPhase == "FOCUS" && phaseEndAtMillis > 0L -> {
            val remainingMin = remainingMinutes(phaseEndAtMillis)
            context.getString(R.string.notification_pomodoro_focus, "$remainingMin min")
        }
        plannedEndAtMillis > 0L -> {
            val remainingMin = remainingMinutes(plannedEndAtMillis)
            context.getString(R.string.notification_session_remaining, profileName, remainingMin)
        }
        else -> context.getString(R.string.notification_session_active, profileName)
    }

    private fun chronometerEnd(
        plannedEndAtMillis: Long,
        pomodoroPhase: String?,
        phaseEndAtMillis: Long,
    ): Long = when {
        pomodoroPhase == "FOCUS" && phaseEndAtMillis > 0L -> phaseEndAtMillis
        plannedEndAtMillis > 0L -> plannedEndAtMillis
        else -> 0L
    }

    private fun isBreakPhase(pomodoroPhase: String?): Boolean =
        pomodoroPhase == "SHORT_BREAK" || pomodoroPhase == "LONG_BREAK" || pomodoroPhase == "BREAK"

    private fun remainingMinutes(endAtMillis: Long): Long =
        ((endAtMillis - System.currentTimeMillis()) / MS_PER_MINUTE).coerceAtLeast(0)

    private const val REQUEST_STOP = 9001
}
