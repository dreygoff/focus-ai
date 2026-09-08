package app.focus.data

import app.focus.data.mapper.toDomain
import app.focus.data.mapper.toEntity
import app.focus.database.dao.DailyStatsDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.SessionDao
import app.focus.domain.internal.stats.EpochDays
import app.focus.domain.internal.stats.StatsAggregator
import app.focus.domain.model.DailyStats
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.usecase.BlockedAppStat
import app.focus.domain.usecase.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RealStatsRepository(
    private val sessionDao: SessionDao,
    private val eventLogDao: EventLogDao,
    private val dailyStatsDao: DailyStatsDao,
) : StatsRepository {

    override suspend fun recalculateDay(epochDay: Long) {
        val dayStart = EpochDays.dayStartMillis(epochDay)
        val dayEnd = EpochDays.dayEndMillis(epochDay)
        val sessions = sessionDao.getSessionsStartedInRange(dayStart, dayEnd).map { it.toDomain() }
        val stats = StatsAggregator.aggregate(
            epochDay = epochDay,
            input = StatsAggregator.DayInput(
                sessions = sessions,
                blockAttempts = eventLogDao.countBlockAttemptsInRange(dayStart, dayEnd),
                bypasses = eventLogDao.countBypassesInRange(dayStart, dayEnd),
            ),
        )
        dailyStatsDao.upsert(stats.toEntity())
    }

    override suspend fun recalculateRange(startEpochDay: Long, endEpochDay: Long) {
        for (day in startEpochDay..endEpochDay) {
            recalculateDay(day)
        }
    }

    override suspend fun getDailyStats(startEpochDay: Long, endEpochDay: Long): List<DailyStats> =
        dailyStatsDao.getStatsInRange(startEpochDay, endEpochDay).map { it.toDomain() }

    override suspend fun getTopBlockedApps(sinceMillis: Long, limit: Int): List<BlockedAppStat> =
        eventLogDao.topBlockedApps(sinceMillis, limit).map { row ->
            BlockedAppStat(packageName = row.packageName, attempts = row.count)
        }

    override fun observeRecentEvents(sinceMillis: Long, limit: Int): Flow<List<EventLog>> =
        eventLogDao.observeRecent(sinceMillis, limit).map { events -> events.mapNotNull { it.toDomain() } }

    override fun observeRecentEventsFiltered(
        sinceMillis: Long,
        types: Set<EventType>?,
        limit: Int,
    ): Flow<List<EventLog>> = observeRecentEvents(sinceMillis, limit).map { events ->
        if (types.isNullOrEmpty()) events else events.filter { it.type in types }
    }

    override suspend fun exportCsv(startMillis: Long, endMillis: Long): String {
        val sessions = sessionDao.observeSessionsInRange(startMillis, endMillis).first()
        val header = "date,focus_minutes,sessions_completed,sessions_total,block_attempts,bypasses\n"
        val startDay = EpochDays.fromMillis(startMillis)
        val endDay = EpochDays.fromMillis(endMillis - 1)
        val stats = getDailyStats(startDay, endDay)
        val rows = stats.joinToString("\n") { day ->
            listOf(
                day.dateEpochDay,
                day.focusMinutes,
                day.sessionsCompleted,
                day.sessionsTotal,
                day.blockAttempts,
                day.bypasses,
            ).joinToString(",")
        }
        val sessionHeader = "\n\nsession_id,profile,started_at,ended_at,status,block_attempts,bypasses\n"
        val sessionRows = sessions.joinToString("\n") { session ->
            listOf(
                session.id,
                session.profileNameSnapshot,
                session.startedAt,
                session.actualEndAt ?: "",
                session.status,
                session.blockAttempts,
                session.bypassesUsed,
            ).joinToString(",")
        }
        return header + rows + sessionHeader + sessionRows
    }

    override suspend fun clearStatistics() {
        dailyStatsDao.deleteAll()
        eventLogDao.deleteAll()
        sessionDao.deleteCompleted()
    }

    private fun DailyStats.toEntity() = app.focus.database.entity.DailyStatsEntity(
        dateEpochDay = dateEpochDay,
        focusMinutes = focusMinutes,
        sessionsCompleted = sessionsCompleted,
        sessionsTotal = sessionsTotal,
        blockAttempts = blockAttempts,
        bypasses = bypasses,
    )
}
