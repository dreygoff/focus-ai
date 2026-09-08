package app.focus.android.di

import android.content.Context
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
    ): AlarmSchedulerService = AlarmSchedulerImpl(context)

    @Provides
    fun provideStartSessionUseCase(
        sessionRepo: SessionRepository,
        profileRepo: ProfileRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): StartSessionUseCase = StartSessionUseCase(
        sessionRepo = sessionRepo,
        profileRepo = profileRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    @Provides
    fun provideStopSessionUseCase(
        sessionRepo: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        clock: Clock,
    ): StopSessionUseCase = StopSessionUseCase(
        sessionRepo = sessionRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    @Provides
    fun provideObserveActiveSessionsUseCase(
        sessionRepo: SessionRepository,
    ): app.focus.domain.usecase.ObserveActiveSessionsUseCase =
        app.focus.domain.usecase.ObserveActiveSessionsUseCase(sessionRepo)

    @Provides
    fun provideCreateProfileUseCase(
        profileRepo: ProfileRepository,
        clock: Clock,
    ): app.focus.domain.usecase.CreateProfileUseCase =
        app.focus.domain.usecase.CreateProfileUseCase(profileRepo, clock)
}
