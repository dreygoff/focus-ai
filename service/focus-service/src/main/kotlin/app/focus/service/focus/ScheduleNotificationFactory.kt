package app.focus.service.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object ScheduleNotificationFactory {

    private const val CHANNEL_ID = "schedule_warnings"
    private const val NOTIFICATION_BASE_ID = 3000

    fun showHardLockWarning(context: Context, scheduleId: String, label: String) {
        ensureChannel(context)
        val skipIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
            action = ScheduleActionReceiver.ACTION_SKIP_TODAY
            putExtra(ScheduleActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val skipPending = PendingIntent.getBroadcast(
            context,
            scheduleId.hashCode(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.schedule_warning_title))
            .setContentText(context.getString(R.string.schedule_warning_body, label))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, context.getString(R.string.schedule_skip_today), skipPending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_ID + scheduleId.hashCode(), notification)
    }

    fun cancelWarning(context: Context, scheduleId: String) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_BASE_ID + scheduleId.hashCode())
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.schedule_warning_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
