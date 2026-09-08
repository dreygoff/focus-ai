package app.focus.android.di

import app.focus.domain.usecase.AccessWindowRepository
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.GrantBypassUseCase
import app.focus.domain.usecase.PauseSessionUseCase
import app.focus.domain.usecase.ResumeSessionUseCase
import app.focus.domain.usecase.SessionRepository
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
}
