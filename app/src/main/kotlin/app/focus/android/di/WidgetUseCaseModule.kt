package app.focus.android.di

import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.GetWidgetSessionStateUseCase
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.QuickStartLastProfileUseCase
import app.focus.domain.usecase.QuickStartProfileUseCase
import app.focus.domain.usecase.QuickStopSessionUseCase
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.LastUsedProfileProvider
import app.focus.domain.usecase.MandatoryPermissionsGateway
import app.focus.domain.usecase.StartSessionUseCase
import app.focus.domain.usecase.StopSessionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WidgetUseCaseModule {

    @Provides
    fun provideGetWidgetSessionStateUseCase(
        sessionRepository: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        clock: Clock,
    ): GetWidgetSessionStateUseCase = GetWidgetSessionStateUseCase(sessionRepository, snapshotStore, clock)

    @Provides
    fun provideQuickStartLastProfileUseCase(
        sessionRepository: SessionRepository,
        profileRepository: ProfileRepository,
        startSessionUseCase: StartSessionUseCase,
        lastUsedProfileProvider: LastUsedProfileProvider,
        permissionsGateway: MandatoryPermissionsGateway,
    ): QuickStartLastProfileUseCase = QuickStartLastProfileUseCase(
        sessionRepository,
        profileRepository,
        startSessionUseCase,
        lastUsedProfileProvider,
        permissionsGateway,
    )

    @Provides
    fun provideQuickStartProfileUseCase(
        sessionRepository: SessionRepository,
        profileRepository: ProfileRepository,
        startSessionUseCase: StartSessionUseCase,
        lastUsedProfileProvider: LastUsedProfileProvider,
        permissionsGateway: MandatoryPermissionsGateway,
    ): QuickStartProfileUseCase = QuickStartProfileUseCase(
        sessionRepository,
        profileRepository,
        startSessionUseCase,
        lastUsedProfileProvider,
        permissionsGateway,
    )

    @Provides
    fun provideQuickStopSessionUseCase(
        sessionRepository: SessionRepository,
        stopSessionUseCase: StopSessionUseCase,
    ): QuickStopSessionUseCase = QuickStopSessionUseCase(sessionRepository, stopSessionUseCase)
}
