package app.focus.android.di

import app.focus.data.RealStatsRepository
import app.focus.datastore.UserSettingsRepository
import app.focus.database.dao.DailyStatsDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.SessionDao
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.EventLogRepository
import app.focus.domain.usecase.ExportStatsCsvUseCase
import app.focus.domain.usecase.GetStatsDashboardUseCase
import app.focus.domain.usecase.LogSessionEndEventUseCase
import app.focus.domain.usecase.ObserveEventLogUseCase
import app.focus.domain.usecase.PruneEventLogUseCase
import app.focus.domain.usecase.RecalculateDailyStatsUseCase
import app.focus.domain.usecase.StatsRepository
import app.focus.domain.usecase.UpdateDailyStatsOnSessionEndUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StatsModule {

    @Provides
    @Singleton
    fun provideStatsRepository(
        sessionDao: SessionDao,
        eventLogDao: EventLogDao,
        dailyStatsDao: DailyStatsDao,
    ): StatsRepository = RealStatsRepository(sessionDao, eventLogDao, dailyStatsDao)

    @Provides
    fun provideRecalculateDailyStatsUseCase(
        statsRepository: StatsRepository,
        clock: Clock,
    ): RecalculateDailyStatsUseCase = RecalculateDailyStatsUseCase(statsRepository, clock)

    @Provides
    fun provideUpdateDailyStatsOnSessionEndUseCase(
        statsRepository: StatsRepository,
    ): UpdateDailyStatsOnSessionEndUseCase = UpdateDailyStatsOnSessionEndUseCase(statsRepository)

    @Provides
    fun providePruneEventLogUseCase(
        eventLogRepository: EventLogRepository,
        clock: Clock,
        userSettingsRepository: UserSettingsRepository,
    ): PruneEventLogUseCase = PruneEventLogUseCase(
        eventLogRepository = eventLogRepository,
        clock = clock,
        retentionDaysProvider = { userSettingsRepository.getEventLogRetentionDays().first() },
    )

    @Provides
    fun provideGetCurrentStreakUseCase(
        statsRepository: StatsRepository,
        clock: Clock,
    ): app.focus.domain.usecase.GetCurrentStreakUseCase =
        app.focus.domain.usecase.GetCurrentStreakUseCase(statsRepository, clock)

    @Provides
    fun provideGetStatsDashboardUseCase(
        statsRepository: StatsRepository,
        clock: Clock,
    ): GetStatsDashboardUseCase = GetStatsDashboardUseCase(statsRepository, clock)

    @Provides
    fun provideExportStatsCsvUseCase(
        statsRepository: StatsRepository,
        clock: Clock,
    ): ExportStatsCsvUseCase = ExportStatsCsvUseCase(statsRepository, clock)

    @Provides
    fun provideObserveEventLogUseCase(
        statsRepository: StatsRepository,
    ): ObserveEventLogUseCase = ObserveEventLogUseCase(statsRepository)

    @Provides
    fun provideLogSessionEndEventUseCase(
        eventLogRepository: EventLogRepository,
        clock: Clock,
    ): LogSessionEndEventUseCase = LogSessionEndEventUseCase(eventLogRepository, clock)
}
