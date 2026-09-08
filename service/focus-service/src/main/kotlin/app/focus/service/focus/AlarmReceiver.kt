package app.focus.service.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import app.focus.domain.usecase.GrantBypassUseCase
import app.focus.domain.usecase.PlanSchedulesUseCase
import app.focus.domain.usecase.StartSessionUseCase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_SESSION_END_ALARM = "app.focus.service.focus.ACTION_SESSION_END_ALARM"
        const val ACTION_SESSION_START_ALARM = "app.focus.service.focus.ACTION_SESSION_START_ALARM"
        const val ACTION_ACCESS_WINDOW_WARNING = GrantBypassUseCase.ACTION_ACCESS_WINDOW_WARNING
        const val ACTION_ACCESS_WINDOW_EXPIRED = GrantBypassUseCase.ACTION_ACCESS_WINDOW_EXPIRED
        const val ACTION_EMERGENCY_EXIT_COMPLETE = app.focus.domain.usecase.RequestEmergencyExitUseCase.ACTION_EMERGENCY_EXIT_COMPLETE
        const val ACTION_SCHEDULE_START = PlanSchedulesUseCase.ACTION_SCHEDULE_START
        const val ACTION_SCHEDULE_END = PlanSchedulesUseCase.ACTION_SCHEDULE_END
        const val ACTION_SCHEDULE_WARNING = PlanSchedulesUseCase.ACTION_SCHEDULE_WARNING
        const val ACTION_POMODORO_BREAK_WARNING =
            app.focus.domain.usecase.AdvancePomodoroPhaseUseCase.ACTION_POMODORO_BREAK_WARNING
        const val ACTION_POMODORO_PHASE_END = StartSessionUseCase.ACTION_POMODORO_PHASE_END
        const val KEY_SESSION_ID = "sessionId"
        const val KEY_PACKAGE_NAME = GrantBypassUseCase.KEY_PACKAGE_NAME
        const val EXTRA_SOURCE = "source"
        const val EXTRA_PROFILE_ID = PlanSchedulesUseCase.KEY_PROFILE_ID
        const val EXTRA_DURATION_MINUTES = PlanSchedulesUseCase.KEY_DURATION_MINUTES
        const val EXTRA_SCHEDULE_LABEL = PlanSchedulesUseCase.KEY_SCHEDULE_LABEL
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SESSION_END_ALARM -> {
                val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: run {
                    Log.w(TAG, "Session end alarm without sessionId")
                    return
                }
                handleSessionEnd(context, sessionId)
            }
            ACTION_SCHEDULE_START, ACTION_SESSION_START_ALARM -> {
                val scheduleId = intent.getStringExtra(KEY_SESSION_ID) ?: run {
                    Log.w(TAG, "Schedule alarm without scheduleId")
                    return
                }
                handleScheduleStart(context, intent, scheduleId)
            }
            ACTION_SCHEDULE_END -> {
                val scheduleId = intent.getStringExtra(KEY_SESSION_ID) ?: return
                handleScheduleEnd(context, scheduleId)
            }
            ACTION_SCHEDULE_WARNING -> {
                val scheduleId = intent.getStringExtra(KEY_SESSION_ID) ?: return
                val label = intent.getStringExtra(EXTRA_SCHEDULE_LABEL) ?: scheduleId
                ScheduleNotificationFactory.showHardLockWarning(context, scheduleId, label)
            }
            ACTION_POMODORO_PHASE_END -> {
                val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: return
                handlePomodoroPhaseEnd(context, sessionId)
            }
            ACTION_POMODORO_BREAK_WARNING -> {
                PomodoroNotificationFactory.showBreakEndingSoon(context)
            }
            ACTION_ACCESS_WINDOW_WARNING -> handleAccessWindowWarning(context, intent)
            ACTION_ACCESS_WINDOW_EXPIRED -> handleAccessWindowExpired(context, intent)
            ACTION_EMERGENCY_EXIT_COMPLETE -> handleEmergencyExitComplete(context, intent)
            else -> Log.w(TAG, "Unknown alarm action: ${intent.action}")
        }
    }

    private fun handleSessionEnd(context: Context, sessionId: String) {
        Log.d(TAG, "Session end alarm for: $sessionId")
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_END_SESSION
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleScheduleStart(context: Context, intent: Intent, scheduleId: String) {
        val durationMinutes = intent.getStringExtra(EXTRA_DURATION_MINUTES)?.toIntOrNull() ?: 25
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ScheduleEntryPoint::class.java,
                )
                val started = entry.startScheduledSessionUseCase().execute(
                    scheduleId = scheduleId,
                    durationMinutesOverride = durationMinutes,
                )
                if (started) {
                    Log.d(TAG, "Scheduled session started: $scheduleId")
                }
                entry.planSchedulesUseCase().replanAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start scheduled session", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleScheduleEnd(context: Context, scheduleId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val deps = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    FocusServiceEntryPoint::class.java,
                ).dependencies()
                val session = deps.sessionRepository.observeActiveSession().first()
                if (session?.scheduleId == scheduleId) {
                    deps.stopSessionUseCase.execute(session.id, app.focus.domain.model.SessionStatus.Completed)
                }
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ScheduleEntryPoint::class.java,
                ).planSchedulesUseCase().replanAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end scheduled session", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handlePomodoroPhaseEnd(context: Context, sessionId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ScheduleEntryPoint::class.java,
                )
                entry.advancePomodoroPhaseUseCase().execute(sessionId)
                val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
                    action = FocusForegroundService.ACTION_POMODORO_PHASE_CHANGED
                    putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to advance pomodoro phase", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleAccessWindowWarning(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(KEY_PACKAGE_NAME) ?: return
        AccessWindowNotificationFactory.showWarning(context, packageName)
    }

    private fun handleAccessWindowExpired(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: return
        val packageName = intent.getStringExtra(KEY_PACKAGE_NAME) ?: return
        AccessWindowNotificationFactory.cancelAccessWindowNotification(context, packageName)
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_ACCESS_WINDOW_EXPIRED
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
            putExtra(FocusForegroundService.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleEmergencyExitComplete(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(KEY_SESSION_ID) ?: return
        Log.d(TAG, "Emergency exit alarm for: $sessionId")
        val serviceIntent = Intent(context, FocusForegroundService::class.java).apply {
            action = FocusForegroundService.ACTION_EMERGENCY_EXIT_COMPLETE
            putExtra(FocusForegroundService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
