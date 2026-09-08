package app.focus.domain.internal.stats

import app.focus.domain.model.DailyStats
import app.focus.domain.model.Session
import app.focus.domain.model.SessionStatus

object StatsAggregator {

    data class DayInput(
        val sessions: List<Session>,
        val blockAttempts: Int,
        val bypasses: Int,
    )

    data class Streaks(
        val current: Int,
        val best: Int,
    )

    fun aggregate(epochDay: Long, input: DayInput): DailyStats {
        val completed = input.sessions.count { isCompleted(it.status) }
        return DailyStats(
            dateEpochDay = epochDay,
            focusMinutes = input.sessions.sumOf(::sessionFocusMinutes),
            sessionsCompleted = completed,
            sessionsTotal = input.sessions.size,
            blockAttempts = input.blockAttempts,
            bypasses = input.bypasses,
        )
    }

    fun fillRange(stats: List<DailyStats>, startEpochDay: Long, endEpochDay: Long): List<DailyStats> {
        if (endEpochDay < startEpochDay) return emptyList()
        val byDay = stats.associateBy { it.dateEpochDay }
        return (startEpochDay..endEpochDay).map { day ->
            byDay[day] ?: DailyStats(
                dateEpochDay = day,
                focusMinutes = 0,
                sessionsCompleted = 0,
                sessionsTotal = 0,
                blockAttempts = 0,
                bypasses = 0,
            )
        }
    }

    fun computeStreaks(dailyStats: List<DailyStats>, todayEpochDay: Long): Streaks {
        val completedDays = dailyStats
            .filter { it.sessionsCompleted > 0 }
            .map { it.dateEpochDay }
            .toSet()

        val current = streakEndingAt(completedDays, todayEpochDay)
        val best = bestStreak(completedDays)
        return Streaks(current = current, best = best)
    }

    fun completionRatePercent(sessionsCompleted: Int, sessionsTotal: Int): Int {
        if (sessionsTotal == 0) return 0
        return ((sessionsCompleted.toDouble() / sessionsTotal.toDouble()) * PERCENT_SCALE).toInt()
    }

    private fun isCompleted(status: SessionStatus): Boolean =
        status is SessionStatus.Completed || status is SessionStatus.Expired

    private fun sessionFocusMinutes(session: Session): Int {
        if (!isCompleted(session.status)) return 0
        val end = session.actualEndAt ?: session.plannedEndAt
        if (end == null) return 0
        return ((end - session.startedAt) / MS_PER_MINUTE).toInt().coerceAtLeast(0)
    }

    private fun streakEndingAt(completedDays: Set<Long>, endDay: Long): Int {
        var streak = 0
        var day = endDay
        while (completedDays.contains(day)) {
            streak++
            day--
        }
        return streak
    }

    private fun bestStreak(completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        val sorted = completedDays.sorted()
        var best = 1
        var current = 1
        for (index in 1 until sorted.size) {
            if (sorted[index] == sorted[index - 1] + 1) {
                current++
            } else {
                best = maxOf(best, current)
                current = 1
            }
        }
        return maxOf(best, current)
    }

    private const val MS_PER_MINUTE = 60_000L
    private const val PERCENT_SCALE = 100
}
