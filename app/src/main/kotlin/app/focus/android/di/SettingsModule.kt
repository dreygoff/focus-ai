package app.focus.android.di

import app.focus.data.ProfileSeeder
import app.focus.database.dao.ProfileDao
import app.focus.domain.usecase.ClearStatisticsUseCase
import app.focus.domain.usecase.DeleteAllUserDataUseCase
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import app.focus.domain.usecase.AllowlistRepository
import app.focus.domain.usecase.ProfileRepository
import app.focus.domain.usecase.ScheduleRepository
import app.focus.domain.usecase.SessionRepository
import app.focus.domain.usecase.StatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    fun provideClearStatisticsUseCase(
        statsRepository: StatsRepository,
    ): ClearStatisticsUseCase = ClearStatisticsUseCase(statsRepository)

    @Provides
    fun provideDeleteAllUserDataUseCase(
        statsRepository: StatsRepository,
        profileRepository: ProfileRepository,
        scheduleRepository: ScheduleRepository,
        allowlistRepository: AllowlistRepository,
        sessionRepository: SessionRepository,
        snapshotStore: ActiveSessionSnapshotStorage,
        profileDao: ProfileDao,
    ): DeleteAllUserDataUseCase = DeleteAllUserDataUseCase(
        DeleteAllUserDataUseCase.Dependencies(
            statsRepository = statsRepository,
            profileRepository = profileRepository,
            scheduleRepository = scheduleRepository,
            allowlistRepository = allowlistRepository,
            sessionRepository = sessionRepository,
            snapshotStore = snapshotStore,
            reseedProfiles = { ProfileSeeder(profileDao).seedIfEmpty() },
        ),
    )
}
