package app.focus.android.di

import app.focus.domain.internal.schedule.ScheduleAlarmPlanner
import app.focus.domain.usecase.AdvancePomodoroPhaseUseCase
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.CheckMissedSchedulesUseCase
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.PlanSchedulesUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ScheduleRepository
import app.focus.domain.usecase.ScheduleSkipRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StartScheduledSessionUseCase
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ScheduleModule {

    @Provides
    fun providePlanSchedulesUseCase(
        scheduleRepository: ScheduleRepository,
        profileRepository: ProfileRepository,
        planner: ScheduleAlarmPlanner,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): PlanSchedulesUseCase = PlanSchedulesUseCase(
        scheduleRepo = scheduleRepository,
        profileRepo = profileRepository,
        planner = planner,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    @Provides
    fun provideStartScheduledSessionUseCase(
        scheduleRepository: ScheduleRepository,
        sessionRepository: SessionRepository,
        skipRepository: ScheduleSkipRepository,
        planner: ScheduleAlarmPlanner,
        startSessionUseCase: StartSessionUseCase,
        clock: Clock,
    ): StartScheduledSessionUseCase = StartScheduledSessionUseCase(
        scheduleRepo = scheduleRepository,
        sessionRepo = sessionRepository,
        skipRepo = skipRepository,
        planner = planner,
        startSessionUseCase = startSessionUseCase,
        clock = clock,
    )

    @Provides
    fun provideCheckMissedSchedulesUseCase(
        scheduleRepository: ScheduleRepository,
        sessionRepository: SessionRepository,
        skipRepository: ScheduleSkipRepository,
        planner: ScheduleAlarmPlanner,
        startScheduledSessionUseCase: StartScheduledSessionUseCase,
        profileRepository: ProfileRepository,
        clock: Clock,
    ): CheckMissedSchedulesUseCase = CheckMissedSchedulesUseCase(
        scheduleRepo = scheduleRepository,
        sessionRepo = sessionRepository,
        skipRepo = skipRepository,
        planner = planner,
        startScheduledSessionUseCase = startScheduledSessionUseCase,
        profileRepo = profileRepository,
        clock = clock,
    )

    @Provides
    fun provideAdvancePomodoroPhaseUseCase(
        sessionRepository: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        stopSessionUseCase: StopSessionUseCase,
        activeSessionBlockState: ActiveSessionBlockState,
        clock: Clock,
    ): AdvancePomodoroPhaseUseCase = AdvancePomodoroPhaseUseCase(
        sessionRepo = sessionRepository,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        stopSessionUseCase = stopSessionUseCase,
        activeSessionBlockState = activeSessionBlockState,
        clock = clock,
    )
}
