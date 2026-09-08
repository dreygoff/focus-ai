package app.focus.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import app.focus.common.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

/**
 * Notification channel IDs used by Focus.
 */
object NotificationChannels {
    const val SESSION = "session"
    const val ALERTS = "alerts"
    const val SUMMARY = "summary"
}

/**
 * Creates all required notification channels for the application.
 * Must be called on app start (FocusForegroundService or Application class).
 */
class FocusNotificationChannelManager(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Session channel — low importance, ongoing during focus
        val sessionChannel = NotificationChannel(
            NotificationChannels.SESSION,
            "Focus Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for active focus sessions"
            setShowBadge(false)
            enableVibration(false)
        }

        // Alerts channel — for countdown warnings and access window expiry
        val alertsChannel = NotificationChannel(
            NotificationChannels.ALERTS,
            "Focus Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts about focus sessions (access window expiry, pomodoro breaks)"
        }

        // Summary channel — for session completion summaries
        val summaryChannel = NotificationChannel(
            NotificationChannels.SUMMARY,
            "Focus Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Session completion summaries"
        }

        manager.createNotificationChannel(sessionChannel)
        manager.createNotificationChannel(alertsChannel)
        manager.createNotificationChannel(summaryChannel)

        Timber.d("Created notification channels")
    }
}

/**
 * Builder for session notification.
 */
class SessionNotificationBuilder(
    private val context: Context,
    private val channelName: String = NotificationChannels.SESSION,
) {
    private val builder = NotificationCompat.Builder(context, channelName)
        .setSmallIcon(android.R.drawable.ic_menu_recent_history) // TODO: Replace with actual app icon
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)

    /** Set the session profile name. */
    fun setProfileName(name: String): SessionNotificationBuilder {
        builder.setContentTitle(name)
            .setContentText("Focus session active")
        return this
    }

    /** Set remaining time as chronometer. */
    fun setChronometer(whenMills: Long, counter: Boolean = true): SessionNotificationBuilder {
        builder.setWhen(whenMills)
            .setShowWhen(counter)
            .setUsesChronometer(counter)
        return this
    }

    /** Set the number of blocked attempts. */
    fun setBlockAttempts(attempts: Int): SessionNotificationBuilder {
        builder.setContentText("Blocked $attempts attempts")
        return this
    }

    /** Add "Complete" action button (soft lock only). */
    fun addCompleteAction(pendingIntent: android.app.PendingIntent?): SessionNotificationBuilder {
        pendingIntent?.let {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Complete",
                it
            )
        }
        return this
    }

    /** Add "Open Focus" action button. */
    fun addOpenAction(pendingIntent: android.app.PendingIntent?): SessionNotificationBuilder {
        pendingIntent?.let {
            builder.addAction(
                android.R.drawable.ic_menu_info_details,
                "Open",
                it
            )
        }
        return this
    }

    /** Build the notification. */
    fun build(): android.app.Notification = builder.build()
}
