package app.focus.domain.internal.stats

import app.focus.domain.model.DailyStats
import app.focus.domain.model.Session
import app.focus.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsAggregatorTest {

    @Test
    fun aggregate_countsCompletedSessionsAndFocusMinutes() {
        val sessions = listOf(
            session(status = SessionStatus.Completed, focusMinutes = 25),
            session(status = SessionStatus.Cancelled, focusMinutes = 10),
            session(status = SessionStatus.Expired, focusMinutes = 15),
        )

        val stats = StatsAggregator.aggregate(
            epochDay = 20_000L,
            input = StatsAggregator.DayInput(
                sessions = sessions,
                blockAttempts = 7,
                bypasses = 2,
            ),
        )

        assertEquals(20_000L, stats.dateEpochDay)
        assertEquals(40, stats.focusMinutes)
        assertEquals(2, stats.sessionsCompleted)
        assertEquals(3, stats.sessionsTotal)
        assertEquals(7, stats.blockAttempts)
        assertEquals(2, stats.bypasses)
    }

    @Test
    fun fillRange_insertsZeroDays() {
        val stats = listOf(
            DailyStats(
                dateEpochDay = 2,
                focusMinutes = 10,
                sessionsCompleted = 1,
                sessionsTotal = 1,
                blockAttempts = 0,
                bypasses = 0,
            ),
        )

        val filled = StatsAggregator.fillRange(stats, startEpochDay = 1, endEpochDay = 3)

        assertEquals(3, filled.size)
        assertEquals(0, filled.first { it.dateEpochDay == 1L }.focusMinutes)
        assertEquals(10, filled.first { it.dateEpochDay == 2L }.focusMinutes)
    }

    @Test
    fun computeStreaks_currentAndBest() {
        val stats = listOf(
            DailyStats(4, 25, 1, 1, 0, 0),
            DailyStats(5, 25, 1, 1, 0, 0),
            DailyStats(10, 25, 1, 1, 0, 0),
            DailyStats(11, 25, 1, 1, 0, 0),
        )

        val streaks = StatsAggregator.computeStreaks(stats, todayEpochDay = 5L)

        assertEquals(2, streaks.current)
        assertEquals(2, streaks.best)
    }

    @Test
    fun completionRatePercent() {
        assertEquals(75, StatsAggregator.completionRatePercent(3, 4))
        assertEquals(0, StatsAggregator.completionRatePercent(0, 0))
    }

    private fun session(status: SessionStatus, focusMinutes: Int): Session {
        val startedAt = 1_000L
        val endAt = startedAt + focusMinutes * 60_000L
        return Session(
            profileId = "p1",
            profileNameSnapshot = "Work",
            lockMode = app.focus.domain.model.LockMode.Soft,
            targetPackagesSnapshot = emptyList(),
            goalText = null,
            startedAt = startedAt,
            plannedEndAt = endAt,
            actualEndAt = endAt,
            status = status,
            source = app.focus.domain.model.SessionSource.MANUAL,
            pomodoroConfig = null,
            bypassesUsed = 0,
            blockAttempts = 0,
            pausesUsed = 0,
            scheduleId = null,
        )
    }
}
