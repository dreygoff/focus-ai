package app.focus.service.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.focus.notifications.FocusNotificationPoster
import app.focus.notifications.NotificationChannels

object PackageAddedNotificationFactory {

    private const val CHANNEL_ID = NotificationChannels.ALERTS
    private const val NOTIFICATION_BASE_ID = 4000

    fun showPromptToAdd(context: Context, packageName: String, appLabel: String) {
        ensureChannel(context)
        val addIntent = Intent(context, AddPackageToProfileReceiver::class.java).apply {
            action = AddPackageToProfileReceiver.ACTION_ADD_PACKAGE
            putExtra(AddPackageToProfileReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        val addPending = PendingIntent.getBroadcast(
            context,
            packageName.hashCode(),
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentPending = openIntent?.let {
            PendingIntent.getActivity(
                context,
                packageName.hashCode() + 1,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.package_added_title))
            .setContentText(context.getString(R.string.package_added_body, appLabel))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, context.getString(R.string.package_added_add), addPending)
            .apply {
                contentPending?.let { setContentIntent(it) }
            }
            .setAutoCancel(true)
            .build()

        FocusNotificationPoster.notify(context, notificationId(packageName), notification)
    }

    fun cancel(context: Context, packageName: String) {
        FocusNotificationPoster.cancel(context, notificationId(packageName))
    }

    private fun notificationId(packageName: String): Int =
        NOTIFICATION_BASE_ID + packageName.hashCode() % NOTIF_HASH_MOD

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.package_added_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

private const val NOTIF_HASH_MOD = 1000
