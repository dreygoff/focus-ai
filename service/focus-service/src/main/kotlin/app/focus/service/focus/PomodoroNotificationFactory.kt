package app.focus.service.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.focus.notifications.FocusNotificationPoster

object PomodoroNotificationFactory {

    private const val CHANNEL_ID = "pomodoro"
    private const val NOTIFICATION_ID = 2001

    fun showBreakActive(context: android.content.Context, phaseEndAtMillis: Long) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.pomodoro_break_title))
            .setContentText(context.getString(R.string.pomodoro_break_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setUsesChronometer(phaseEndAtMillis > 0L)
            .apply {
                if (phaseEndAtMillis > 0L) {
                    setWhen(phaseEndAtMillis)
                    setShowWhen(true)
                }
            }
            .build()
        FocusNotificationPoster.notify(context, NOTIFICATION_ID, notification)
    }

    fun showBreakEndingSoon(context: android.content.Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.pomodoro_break_warning_title))
            .setContentText(context.getString(R.string.pomodoro_break_warning_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        FocusNotificationPoster.notify(context, NOTIFICATION_ID + 1, notification)
    }

    fun cancelBreakNotifications(context: android.content.Context) {
        FocusNotificationPoster.cancel(context, NOTIFICATION_ID)
        FocusNotificationPoster.cancel(context, NOTIFICATION_ID + 1)
    }

    private fun ensureChannel(context: android.content.Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.pomodoro_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
