package app.focus.domain.internal.schedule

import app.focus.domain.model.Schedule
import java.util.Calendar
import java.util.TimeZone

/**
 * Per TR-10: ScheduleAlarmPlanner calculates the next start/end times for a schedule.
 * Handles midnight transitions, DST via ZonedDateTime-compatible Calendar arithmetic,
 * and day-of-week bitmasks (bit0=Mon ... bit6=Sun).
 */
class ScheduleAlarmPlanner {

    /**
     * Calculate the next upcoming occurrence of this schedule's start time
     * from the given baseTimeMillis.
     * Returns null if the schedule is disabled or invalid.
     */
    fun nextStartAt(schedule: Schedule, baseTimeMillis: Long): Long? {
        if (!schedule.enabled) return null

        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = baseTimeMillis
            // Round down to current second
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Iterate up to 7 days to find next match
        for (offset in 0L until 7 * 86400_000L step 86400_000L) {
            val candidate = cal.clone() as Calendar
            candidate.timeInMillis = baseTimeMillis + offset

            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            // Convert Calendar DAY_OF_WEEK (1=Sun, 2=Mon, ...) to bitmask index (0=Mon, ..., 6=Sun)
            val bitIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

            if (schedule.daysOfWeekMask and (1 shl bitIndex) != 0) {
                // Set to the scheduled start time
                candidate.set(Calendar.HOUR_OF_DAY, schedule.startMinuteOfDay / 60)
                candidate.set(Calendar.MINUTE, schedule.startMinuteOfDay % 60)
                candidate.set(Calendar.SECOND, 0)
                candidate.set(Calendar.MILLISECOND, 0)

                val candidateMillis = candidate.timeInMillis
                // If candidate is in the past (same day but time already passed), skip to next occurrence
                if (candidateMillis > baseTimeMillis) {
                    return candidateMillis
                }
            }
        }

        // Fallback: schedule next Monday
        val nextMonday = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = baseTimeMillis
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, schedule.startMinuteOfDay / 60)
            set(Calendar.MINUTE, schedule.startMinuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextMonday.timeInMillis
    }

    /**
     * Calculate the end time for this schedule.
     * Handles overnight schedules where end < start.
     */
    fun endAt(startMinuteOfDay: Int, endMinuteOfDay: Int): Long {
        val diff = if (endMinuteOfDay > startMinuteOfDay) {
            endMinuteOfDay - startMinuteOfDay
        } else {
            // Overnight: crosses midnight
            (24 * 60 - startMinuteOfDay) + endMinuteOfDay
        }
        return diff * 60_000L // minutes to milliseconds
    }

    /**
     * Calculate the next alarm pair (start at, end at) for this schedule from baseTimeMillis.
     */
    fun calculateAlarms(schedule: Schedule, baseTimeMillis: Long): AlarmPair? {
        val startAt = nextStartAt(schedule, baseTimeMillis) ?: return null
        val duration = endAt(schedule.startMinuteOfDay, schedule.endMinuteOfDay)
        return AlarmPair(startAt = startAt, endAt = startAt + duration)
    }

    /** Recalculate all enabled schedules from a given time. */
    fun recalculateAll(schedules: List<Schedule>, baseTimeMillis: Long): List<AlarmInfo> {
        return schedules.filter { it.enabled }.mapNotNull { schedule ->
            val alarms = calculateAlarms(schedule, baseTimeMillis) ?: return@mapNotNull null
            AlarmInfo(
                scheduleId = schedule.id,
                startAt = alarms.startAt,
                endAt = alarms.endAt,
                source = schedule.profileId,
                allowSkipDay = schedule.allowSkipDay
            )
        }
    }

    data class AlarmPair(val startAt: Long, val endAt: Long)
    data class AlarmInfo(
        val scheduleId: String,
        val startAt: Long,
        val endAt: Long,
        val source: String,
        val allowSkipDay: Boolean
    )
}
