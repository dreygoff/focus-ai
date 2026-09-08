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
        hardLockExtras: app.focus.domain.usecase.HardLockExtrasContributor,
        hardLockLifecycle: app.focus.domain.usecase.HardLockLifecycleController,
        clock: Clock,
    ): StartSessionUseCase = StartSessionUseCase(
        sessionRepo = sessionRepo,
        profileRepo = profileRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        sessionRuntime = sessionRuntime,
        hardLockExtras = hardLockExtras,
        hardLockLifecycle = hardLockLifecycle,
        clock = clock,
    )

    @Provides
    fun provideStopSessionDependencies(
        sessionRepo: SessionRepository,
        profileRepo: ProfileRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        alarmScheduler: AlarmSchedulerService,
        sessionRuntime: app.focus.domain.usecase.SessionRuntimeController,
        hardLockLifecycle: app.focus.domain.usecase.HardLockLifecycleController,
    ): app.focus.domain.usecase.StopSessionDependencies = app.focus.domain.usecase.StopSessionDependencies(
        sessionRepo = sessionRepo,
        profileRepo = profileRepo,
        snapshotStore = snapshotStore,
        alarmScheduler = alarmScheduler,
        sessionRuntime = sessionRuntime,
        hardLockLifecycle = hardLockLifecycle,
    )

    @Provides
    fun provideStopSessionUseCase(
        deps: app.focus.domain.usecase.StopSessionDependencies,
        updateDailyStats: app.focus.domain.usecase.UpdateDailyStatsOnSessionEndUseCase,
        logSessionEndEvent: app.focus.domain.usecase.LogSessionEndEventUseCase,
        clock: Clock,
    ): StopSessionUseCase = StopSessionUseCase(
        deps = deps,
        updateDailyStats = updateDailyStats,
        logSessionEndEvent = logSessionEndEvent,
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
