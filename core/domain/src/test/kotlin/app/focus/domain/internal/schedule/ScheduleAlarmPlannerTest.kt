package app.focus.domain.internal.schedule

import app.focus.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests for ScheduleAlarmPlanner (TR-10).
 */
class ScheduleAlarmPlannerTest {

    private lateinit var planner: ScheduleAlarmPlanner
    private val baseTimeMillis = 1_700_000_000_000L

    @Before
    fun setUp() {
        planner = ScheduleAlarmPlanner()
    }

    @Test
    fun `nextStartAt returns null for disabled schedule`() {
        val schedule = schedule(enabled = false)
        assertNull(planner.nextStartAt(schedule, baseTimeMillis))
    }

    @Test
    fun `nextStartAt returns future time for enabled schedule`() {
        val schedule = schedule(daysOfWeekMask = allDaysMask())
        val startAt = planner.nextStartAt(schedule, baseTimeMillis)
        assertNotNull(startAt)
        assertTrue(startAt!! > baseTimeMillis)
    }

    @Test
    fun `calculateAlarms returns start and end for enabled schedule`() {
        val schedule = schedule(daysOfWeekMask = allDaysMask())
        val alarms = planner.calculateAlarms(schedule, baseTimeMillis)
        assertNotNull(alarms)
        assertTrue(alarms!!.endAt > alarms.startAt)
    }

    @Test
    fun `endAt handles overnight schedule`() {
        val overnightDuration = planner.endAt(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60)
        assertEquals(8 * 60 * 60_000L, overnightDuration)
    }

    @Test
    fun `endAt handles same-day schedule`() {
        val duration = planner.endAt(startMinuteOfDay = 9 * 60, endMinuteOfDay = 17 * 60)
        assertEquals(8 * 60 * 60_000L, duration)
    }

    @Test
    fun `recalculateAll skips disabled schedules`() {
        val enabled = schedule(id = "enabled", enabled = true, daysOfWeekMask = allDaysMask())
        val disabled = schedule(id = "disabled", enabled = false, daysOfWeekMask = allDaysMask())
        val result = planner.recalculateAll(listOf(enabled, disabled), baseTimeMillis)
        assertEquals(1, result.size)
        assertEquals("enabled", result.first().scheduleId)
    }

    @Test
    fun `findActiveWindowEndAt returns end when inside window`() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val now = cal.timeInMillis
        val schedule = schedule(
            daysOfWeekMask = allDaysMask(),
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 17 * 60,
        )
        val endAt = planner.findActiveWindowEndAt(schedule, now)
        assertNotNull(endAt)
        assertTrue(endAt!! > now)
    }

    private fun schedule(
        id: String = "schedule-1",
        enabled: Boolean = true,
        daysOfWeekMask: Int = 0b0111111,
        startMinuteOfDay: Int = 9 * 60,
        endMinuteOfDay: Int = 17 * 60,
    ): Schedule = Schedule(
        id = id,
        profileId = "profile-1",
        enabled = enabled,
        daysOfWeekMask = daysOfWeekMask,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        allowSkipDay = false,
        label = "Work",
        createdAt = baseTimeMillis,
    )

    private fun allDaysMask(): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = baseTimeMillis
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val bitIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
        return 1 shl bitIndex
    }
}
