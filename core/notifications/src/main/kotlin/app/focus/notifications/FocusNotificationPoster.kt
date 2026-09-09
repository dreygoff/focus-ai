package app.focus.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object FocusNotificationPoster {
    @SuppressLint("MissingPermission")
    fun notify(
        context: Context,
        id: Int,
        notification: Notification,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(
        context: Context,
        id: Int,
    ) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
