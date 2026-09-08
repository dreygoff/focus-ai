package app.focus.domain.usecase

import app.focus.domain.model.DailyStats
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import kotlinx.coroutines.flow.Flow

data class BlockedAppStat(
    val packageName: String,
    val attempts: Int,
)

interface StatsRepository {
    suspend fun recalculateDay(epochDay: Long)
    suspend fun recalculateRange(startEpochDay: Long, endEpochDay: Long)
    suspend fun getDailyStats(startEpochDay: Long, endEpochDay: Long): List<DailyStats>
    suspend fun getTopBlockedApps(sinceMillis: Long, limit: Int): List<BlockedAppStat>
    fun observeRecentEvents(sinceMillis: Long, limit: Int = 500): Flow<List<EventLog>>
    fun observeRecentEventsFiltered(
        sinceMillis: Long,
        types: Set<EventType>?,
        limit: Int = 500,
    ): Flow<List<EventLog>>
    suspend fun exportCsv(startMillis: Long, endMillis: Long): String
    suspend fun clearStatistics()
}
