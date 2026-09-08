package app.focus.service.focus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

internal object FocusSessionNotificationFactory {

    private const val MS_PER_MINUTE = 60_000L

    fun build(
        context: android.content.Context,
        profileName: String,
        plannedEndAtMillis: Long,
        activeSessionId: String?,
    ): NotificationCompat.Builder {
        val stopIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_USER_STOP
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, activeSessionId)
        }
        val stopPending = PendingIntent.getService(
            context,
            REQUEST_STOP,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val openPending = FocusForegroundService.makeOpenPendingIntent(context)
        val remainingText = if (plannedEndAtMillis > 0L) {
            val remainingMin = ((plannedEndAtMillis - System.currentTimeMillis()) / MS_PER_MINUTE).coerceAtLeast(0)
            context.getString(R.string.notification_session_remaining, profileName, remainingMin)
        } else {
            context.getString(R.string.notification_session_active, profileName)
        }

        return NotificationCompat.Builder(context, FocusForegroundService.CHANNEL_SESSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_session_title))
            .setContentText(remainingText)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(plannedEndAtMillis > 0L)
            .apply {
                if (plannedEndAtMillis > 0L) {
                    setWhen(plannedEndAtMillis)
                    setShowWhen(true)
                }
            }
            .addAction(
                android.R.drawable.ic_menu_revert,
                context.getString(R.string.notification_stop),
                stopPending,
            )
    }

    private const val REQUEST_STOP = 9001
}
