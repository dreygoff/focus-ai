package app.focus.service.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class FocusForegroundService : Service() {

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    override fun onCreate() {
        super.onCreate()
        initializeChannels()
    }

    private fun initializeChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_SESSION,
                "Focus Sessions",
                android.app.NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> stopSelf()
            ACTION_END_SESSION -> stopSelf()
            ACTION_RESTORE -> handleStart(intent)
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val notification = buildSessionNotification(intent.getStringExtra(EXTRA_SESSION_ID)).build()
        startForeground(NOTIF_ID, notification)
    }

    private fun buildSessionNotification(sessionId: String?): NotificationCompat.Builder {
        val id = sessionId?.takeIf { it.isNotBlank() } ?: "unknown"
        return NotificationCompat.Builder(this, CHANNEL_SESSION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Focus")
            .setContentText("Session $id active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .addAction(android.R.drawable.ic_menu_revert, "Stop", makeStopPendingIntent(this))
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val ACTION_START = "app.focus.service.START"
        const val ACTION_STOP = "app.focus.service.STOP"
        const val ACTION_END_SESSION = "app.focus.service.END_SESSION"
        const val ACTION_RESTORE = "app.focus.service.RESTORE"
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_SESSION_ID = "sessionId"
        const val NOTIF_ID = 1001
        const val CHANNEL_SESSION = "session"

        fun makeStopPendingIntent(ctx: Context): PendingIntent {
            val intent = Intent(ctx, FocusForegroundService::class.java).apply { action = ACTION_STOP }
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getService(ctx, 9001, intent, flags)
        }

        fun makeOpenPendingIntent(ctx: Context): PendingIntent {
            val launchIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?: Intent().apply { setPackage(ctx.packageName) }
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getActivity(ctx, 9002, launchIntent, flags)
        }
    }
}
