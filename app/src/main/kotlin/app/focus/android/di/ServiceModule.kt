package app.focus.android.di

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import app.focus.domain.internal.pomodoro.PomodoroPlanner
import app.focus.domain.internal.schedule.ScheduleAlarmPlanner
import app.focus.domain.model.PomodoroConfig
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.BlockLauncher
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.DecideBlockUseCase
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StopSessionUseCase
import app.focus.service.focus.FocusServiceDependencies
import app.focus.service.focus.AlarmReceiver
import app.focus.system.DefaultUsageStatsPollingDetector
import app.focus.system.DetectorOrchestrator
import app.focus.system.HardLockEnforcer
import app.focus.system.PermissionChecker
import app.focus.system.timer.SessionTimerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun providePermissionChecker(
        @ApplicationContext context: Context
    ): PermissionChecker = PermissionChecker(context)

    @Provides
    @Singleton
    fun provideHardLockEnforcer(
        @ApplicationContext context: Context
    ): HardLockEnforcer = HardLockEnforcer(context)

    @Provides
    @Singleton
    fun provideScheduleAlarmPlanner(): ScheduleAlarmPlanner = ScheduleAlarmPlanner()

    @Provides
    @Singleton
    fun provideDetectorOrchestrator(
        @ApplicationContext context: Context,
        permissionChecker: PermissionChecker
    ): DetectorOrchestrator = DetectorOrchestrator(context, permissionChecker.permissions)

    @Provides
    @Singleton
    fun provideUsageStatsPollingDetector(
        @ApplicationContext context: Context
    ): app.focus.system.UsageStatsPollingDetector = DefaultUsageStatsPollingDetector(context)

    @Provides
    @Singleton
    fun providePackageRepository(
        @ApplicationContext context: Context,
    ): app.focus.system.PackageRepository = app.focus.system.DefaultPackageRepository(context)

    @Provides
    @Singleton
    fun provideSessionTimerManager(
        @ApplicationContext context: Context,
        snapshotStore: ActiveSessionSnapshotStorage
    ): SessionTimerManager {
        return SessionTimerManager(context, null, null)
    }

    @Provides
    @Singleton
    fun provideFocusServiceDependencies(
        stopSessionUseCase: StopSessionUseCase,
        completeEmergencyExitUseCase: app.focus.domain.usecase.CompleteEmergencyExitUseCase,
        sessionRepository: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
        detectorOrchestrator: DetectorOrchestrator,
        usageStatsPollingDetector: app.focus.system.UsageStatsPollingDetector,
        decideBlockUseCase: DecideBlockUseCase,
        blockLauncher: BlockLauncher,
        eventLogRepository: EventLogRepository,
        activeSessionBlockState: ActiveSessionBlockState,
    ): FocusServiceDependencies = FocusServiceDependencies(
        stopSessionUseCase = stopSessionUseCase,
        completeEmergencyExitUseCase = completeEmergencyExitUseCase,
        sessionRepository = sessionRepository,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        clock = clock,
        detectorOrchestrator = detectorOrchestrator,
        usageStatsPollingDetector = usageStatsPollingDetector,
        decideBlockUseCase = decideBlockUseCase,
        blockLauncher = blockLauncher,
        eventLogRepository = eventLogRepository,
        activeSessionBlockState = activeSessionBlockState,
    )
}

@Module
@InstallIn(ViewModelComponent::class)
object PomodoroModule {

    @Provides
    fun providePomodoroPlanner(config: PomodoroConfig): PomodoroPlanner = PomodoroPlanner(config)
}

/**
 * AlarmScheduler implementation using AlarmManager.
 * Handles exact alarms for session start/end and scheduled sessions.
 */
class AlarmSchedulerImpl(
    private val context: android.content.Context
) : AlarmSchedulerService {

    companion object {
        const val TAG = "AlarmScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
        ?: throw IllegalStateException("AlarmManager not available")

    override fun scheduleExact(
        alarmMillis: Long,
        operationCode: Int,
        receiverClassName: String,
        sessionId: String,
        action: String?,
        extras: Map<String, String>,
    ) {
        try {
            val intent = android.content.Intent(context, Class.forName(receiverClassName)).apply {
                this.action = action ?: AlarmReceiver.ACTION_SESSION_END_ALARM
                putExtra(AlarmReceiver.KEY_SESSION_ID, sessionId)
                extras.forEach { (key, value) -> putExtra(key, value) }
            }

            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, operationCode, intent, flags
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    alarmMillis,
                    pendingIntent
                )
            } else {
                // Pre-M: Use setExact
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    alarmMillis,
                    pendingIntent
                )
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    override fun cancelAlarm(operationCode: Int, receiverClassName: String) {
        try {
            val intent = android.content.Intent(context, Class.forName(receiverClassName))
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context, operationCode, intent, flags
            )

            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cancel alarm", e)
        }
    }
}
