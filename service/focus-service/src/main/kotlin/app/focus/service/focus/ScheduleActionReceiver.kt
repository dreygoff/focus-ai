package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.focus.domain.usecase.PlanSchedulesUseCase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScheduleActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScheduleActionReceiver"
        const val ACTION_SKIP_TODAY = "app.focus.service.focus.ACTION_SKIP_TODAY"
        const val EXTRA_SCHEDULE_ID = "scheduleId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SKIP_TODAY) return
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ScheduleEntryPoint::class.java,
                )
                entryPoint.scheduleSkipRepository().skipToday(scheduleId)
                entryPoint.planSchedulesUseCase().cancelAlarmsFor(scheduleId)
                ScheduleNotificationFactory.cancelWarning(context, scheduleId)
                Log.d(TAG, "Skipped schedule for today: $scheduleId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to skip schedule", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
