package app.focus.android.di

import app.focus.domain.usecase.PruneEventLogUseCase
import app.focus.domain.usecase.RecalculateDailyStatsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MaintenanceEntryPoint {
    fun recalculateDailyStatsUseCase(): RecalculateDailyStatsUseCase
    fun pruneEventLogUseCase(): PruneEventLogUseCase
}
