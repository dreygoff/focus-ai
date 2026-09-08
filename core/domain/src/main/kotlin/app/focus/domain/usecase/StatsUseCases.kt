package app.focus.domain.usecase

import app.focus.domain.internal.stats.EpochDays
import app.focus.domain.internal.stats.StatsAggregator
import app.focus.domain.model.DailyStats
import app.focus.domain.model.EventLog
import app.focus.domain.model.EventType
import app.focus.domain.model.Session
import app.focus.domain.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Suppress("MagicNumber")
enum class StatsPeriod(val dayCount: Int) {
    TODAY(1),
    WEEK(7),
    MONTH(30),
}

data class StatsDashboard(
    val dailyStats: List<DailyStats>,
    val sessionsCompleted: Int,
    val sessionsTotal: Int,
    val totalFocusMinutes: Int,
    val blockAttempts: Int,
    val bypasses: Int,
    val completionRatePercent: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val topBlockedApps: List<BlockedAppStat>,
)

class RecalculateDailyStatsUseCase(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {
    suspend fun execute(daysBack: Int = DEFAULT_DAYS_BACK) = withContext(Dispatchers.IO) {
        val today = EpochDays.fromMillis(clock.nowMillis())
        statsRepository.recalculateRange(today - daysBack, today)
    }

    companion object {
        const val DEFAULT_DAYS_BACK = 90
    }
}

class UpdateDailyStatsOnSessionEndUseCase(
    private val statsRepository: StatsRepository,
) {
    suspend fun execute(session: Session) = withContext(Dispatchers.IO) {
        val endMillis = session.actualEndAt ?: session.startedAt
        val epochDay = EpochDays.fromMillis(endMillis)
        statsRepository.recalculateDay(epochDay)
    }
}

class PruneEventLogUseCase(
    private val eventLogRepository: EventLogRepository,
    private val clock: Clock,
    private val retentionDaysProvider: suspend () -> Long,
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val retentionDays = retentionDaysProvider().takeIf { it > 0 } ?: DEFAULT_RETENTION_DAYS
        val cutoff = clock.nowMillis() - retentionDays * MS_PER_DAY
        eventLogRepository.deleteOlderThan(cutoff)
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 90L
        private const val MS_PER_DAY = 86_400_000L
    }
}

class GetStatsDashboardUseCase(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {
    suspend fun execute(period: StatsPeriod): StatsDashboard = withContext(Dispatchers.IO) {
        val today = EpochDays.fromMillis(clock.nowMillis())
        val startDay = today - (period.dayCount - 1L)
        val sinceMillis = EpochDays.dayStartMillis(startDay)

        val rawStats = statsRepository.getDailyStats(startDay, today)
        val dailyStats = StatsAggregator.fillRange(rawStats, startDay, today)
        val streaks = StatsAggregator.computeStreaks(dailyStats, today)
        val topApps = statsRepository.getTopBlockedApps(sinceMillis, TOP_APPS_LIMIT)

        val sessionsCompleted = dailyStats.sumOf { it.sessionsCompleted }
        val sessionsTotal = dailyStats.sumOf { it.sessionsTotal }

        StatsDashboard(
            dailyStats = dailyStats,
            sessionsCompleted = sessionsCompleted,
            sessionsTotal = sessionsTotal,
            totalFocusMinutes = dailyStats.sumOf { it.focusMinutes },
            blockAttempts = dailyStats.sumOf { it.blockAttempts },
            bypasses = dailyStats.sumOf { it.bypasses },
            completionRatePercent = StatsAggregator.completionRatePercent(sessionsCompleted, sessionsTotal),
            currentStreak = streaks.current,
            bestStreak = streaks.best,
            topBlockedApps = topApps,
        )
    }

    companion object {
        private const val TOP_APPS_LIMIT = 5
    }
}

class ExportStatsCsvUseCase(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {
    suspend fun execute(period: StatsPeriod): String = withContext(Dispatchers.IO) {
        val today = EpochDays.fromMillis(clock.nowMillis())
        val startDay = today - (period.dayCount - 1L)
        statsRepository.exportCsv(
            startMillis = EpochDays.dayStartMillis(startDay),
            endMillis = EpochDays.dayEndMillis(today),
        )
    }
}

class ObserveEventLogUseCase(
    private val statsRepository: StatsRepository,
) {
    fun execute(sinceMillis: Long, types: Set<EventType>? = null): Flow<List<EventLog>> =
        if (types.isNullOrEmpty()) {
            statsRepository.observeRecentEvents(sinceMillis)
        } else {
            statsRepository.observeRecentEventsFiltered(sinceMillis, types)
        }
}

class LogSessionEndEventUseCase(
    private val eventLogRepository: EventLogRepository,
    private val clock: Clock,
) {
    suspend fun execute(sessionId: String, status: SessionStatus) = withContext(Dispatchers.IO) {
        val type = when (status) {
            SessionStatus.Completed, SessionStatus.Expired -> EventType.SESSION_COMPLETED
            SessionStatus.Cancelled -> EventType.SESSION_CANCELLED
            else -> return@withContext
        }
        eventLogRepository.log(
            EventLog(
                timestamp = clock.nowMillis(),
                sessionId = sessionId,
                type = type,
                packageName = null,
                payload = null,
            ),
        )
    }
}
