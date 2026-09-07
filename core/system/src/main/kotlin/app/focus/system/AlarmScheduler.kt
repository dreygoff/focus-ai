package app.focus.system

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

interface AlarmScheduler {
    fun scheduleExact(alarmMillis: Long, operationCode: Int, receiverClass: Class<out BroadcastReceiver>)
    fun cancelAlarm(operationCode: Int, receiverClass: Class<out BroadcastReceiver>)
}

class DefaultAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    override fun scheduleExact(alarmMillis: Long, operationCode: Int, receiverClass: Class<out BroadcastReceiver>) {
        if (!canScheduleExactAlarms()) return

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            operationCode,
            Intent(context, receiverClass),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        /*
            Schedule an exact alarm that triggers at the specified time.
            Uses setExactAndAllowWhileIdle to ensure delivery even in doze mode.
         */
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            alarmMillis,
            pendingIntent
        )
    }

    override fun cancelAlarm(operationCode: Int, receiverClass: Class<out BroadcastReceiver>) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            operationCode,
            Intent(context, receiverClass),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Settings.AlarmClock.canScheduleExactAlarms(context.packageName)
        } else {
            true
        }
    }
}
