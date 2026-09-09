package app.focus.android.di

import app.focus.domain.usecase.AccessWindowRepository
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.GrantBypassUseCase
import app.focus.domain.usecase.GrantSettingsShortcutUseCase
import app.focus.domain.usecase.PauseSessionUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ResumeSessionUseCase
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.SettingsShortcutGateway
import app.focus.domain.usecase.StopSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BypassUseCaseModule {

    @Provides
    fun provideGrantBypassUseCase(
        accessWindowRepository: AccessWindowRepository,
        sessionRepository: SessionRepository,
        eventLogRepository: EventLogRepository,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): GrantBypassUseCase = GrantBypassUseCase(
        accessWindowRepo = accessWindowRepository,
        sessionRepo = sessionRepository,
        eventLogRepo = eventLogRepository,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    @Provides
    fun provideGrantSettingsShortcutUseCase(
        accessWindowRepository: AccessWindowRepository,
        sessionRepository: SessionRepository,
        profileRepository: ProfileRepository,
        shortcutGateway: SettingsShortcutGateway,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): GrantSettingsShortcutUseCase = GrantSettingsShortcutUseCase(
        accessWindowRepo = accessWindowRepository,
        sessionRepo = sessionRepository,
        profileRepo = profileRepository,
        shortcutGateway = shortcutGateway,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    @Provides
    fun providePauseSessionUseCase(
        sessionRepository: SessionRepository,
        eventLogRepository: EventLogRepository,
        activeSessionBlockState: ActiveSessionBlockState,
        clock: Clock,
    ): PauseSessionUseCase = PauseSessionUseCase(
        sessionRepo = sessionRepository,
        eventLogRepo = eventLogRepository,
        blockState = activeSessionBlockState,
        clock = clock,
    )

    @Provides
    fun provideResumeSessionUseCase(
        sessionRepository: SessionRepository,
        eventLogRepository: EventLogRepository,
        activeSessionBlockState: ActiveSessionBlockState,
        clock: Clock,
    ): ResumeSessionUseCase = ResumeSessionUseCase(
        sessionRepo = sessionRepository,
        eventLogRepo = eventLogRepository,
        blockState = activeSessionBlockState,
        clock = clock,
    )

    @Provides
    fun provideRequestEmergencyExitUseCase(
        sessionRepository: SessionRepository,
        profileRepository: ProfileRepository,
        eventLogRepository: EventLogRepository,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): app.focus.domain.usecase.RequestEmergencyExitUseCase =
        app.focus.domain.usecase.RequestEmergencyExitUseCase(
            sessionRepo = sessionRepository,
            profileRepo = profileRepository,
            eventLogRepo = eventLogRepository,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )

    @Provides
    fun provideCancelEmergencyExitUseCase(
        sessionRepository: SessionRepository,
        alarmScheduler: AlarmSchedulerService,
    ): app.focus.domain.usecase.CancelEmergencyExitUseCase =
        app.focus.domain.usecase.CancelEmergencyExitUseCase(
            sessionRepo = sessionRepository,
            alarmScheduler = alarmScheduler,
        )

    @Provides
    fun provideCompleteEmergencyExitUseCase(
        sessionRepository: SessionRepository,
        stopSessionUseCase: StopSessionUseCase,
        eventLogRepository: EventLogRepository,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): app.focus.domain.usecase.CompleteEmergencyExitUseCase =
        app.focus.domain.usecase.CompleteEmergencyExitUseCase(
            sessionRepo = sessionRepository,
            stopSessionUseCase = stopSessionUseCase,
            eventLogRepo = eventLogRepository,
            alarmScheduler = alarmScheduler,
            clock = clock,
        )
}
