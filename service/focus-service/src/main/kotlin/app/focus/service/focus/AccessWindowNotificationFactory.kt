package app.focus.service.focus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.focus.notifications.FocusNotificationPoster
import app.focus.notifications.NotificationChannels

internal object AccessWindowNotificationFactory {

    private const val ACCESS_WINDOW_NOTIF_BASE = 2000

    fun showActive(context: Context, packageName: String, appLabel: String, expiresAtMillis: Long) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_access_window_title))
            .setContentText(context.getString(R.string.notification_access_window_active, appLabel))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(expiresAtMillis)
            .setShowWhen(true)
            .build()
        FocusNotificationPoster.notify(context, notificationId(packageName), notification)
    }

    fun showWarning(context: Context, packageName: String) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(
            context,
            notificationId(packageName) + 1,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_access_window_title))
            .setContentText(context.getString(R.string.notification_access_window_warning))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        FocusNotificationPoster.notify(context, notificationId(packageName) + 1, notification)
    }

    fun cancelAccessWindowNotification(context: Context, packageName: String) {
        FocusNotificationPoster.cancel(context, notificationId(packageName))
        FocusNotificationPoster.cancel(context, notificationId(packageName) + 1)
    }

    private fun notificationId(packageName: String): Int =
        ACCESS_WINDOW_NOTIF_BASE + packageName.hashCode() % NOTIF_HASH_MOD
}

private const val NOTIF_HASH_MOD = 1000
