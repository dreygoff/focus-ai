package app.focus.android.di

import android.content.Context
import app.focus.data.SystemAllowlistQualifier
import app.focus.domain.usecase.AccessWindowRepository
import app.focus.domain.usecase.ActiveSessionBlockState
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.AlarmSchedulerService
import app.focus.domain.usecase.AllowlistRepository
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.DecideBlockUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import app.focus.feature.blocker.DefaultBlockerLauncher
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
    @Singleton
    fun provideActiveSessionBlockState(): ActiveSessionBlockState = ActiveSessionBlockState()

    @Provides
    @Singleton
    fun provideBlockLauncher(
        launcher: DefaultBlockerLauncher,
    ): app.focus.domain.usecase.BlockLauncher = launcher

    @Provides
    fun provideDecideBlockUseCase(
        @SystemAllowlistQualifier systemAllowlist: Set<String>,
        allowlistRepository: AllowlistRepository,
        accessWindowRepository: AccessWindowRepository,
        activeSessionBlockState: ActiveSessionBlockState,
    ): DecideBlockUseCase = DecideBlockUseCase(
        systemAllowlist = { systemAllowlist },
        userAllowlistRepo = allowlistRepository,
        accessWindowRepo = accessWindowRepository,
        sessionState = { activeSessionBlockState.sessionState },
    )

    @Provides
    @Singleton
    fun provideSessionRuntimeController(
        controller: AndroidSessionRuntimeController,
    ): app.focus.domain.usecase.SessionRuntimeController = controller

    @Provides
    fun provideStartSessionUseCase(
        sessionRepo: SessionRepository,
        profileRepo: ProfileRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        sessionRuntime: app.focus.domain.usecase.SessionRuntimeController,
        clock: Clock,
    ): StartSessionUseCase = StartSessionUseCase(
        sessionRepo = sessionRepo,
        profileRepo = profileRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        sessionRuntime = sessionRuntime,
        clock = clock,
    )

    @Provides
    fun provideStopSessionUseCase(
        sessionRepo: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        sessionRuntime: app.focus.domain.usecase.SessionRuntimeController,
        clock: Clock,
    ): StopSessionUseCase = StopSessionUseCase(
        sessionRepo = sessionRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        sessionRuntime = sessionRuntime,
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
