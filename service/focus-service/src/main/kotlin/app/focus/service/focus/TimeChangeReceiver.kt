package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> reschedule(context)
        }
    }

    private fun reschedule(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ScheduleEntryPoint::class.java,
                )
                entry.planSchedulesUseCase().replanAll()
                entry.checkMissedSchedulesUseCase().execute()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to replan schedules after time change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
