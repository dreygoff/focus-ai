package app.focus.domain.model

import java.util.UUID

/**
 * Domain model representing a schedule for automatic session start/end.
 */
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val enabled: Boolean,
    val daysOfWeekMask: Int,              // bit0=Mon … bit6=Sun
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,              // end < start = overnight
    val allowSkipDay: Boolean,
    val label: String?,
    val createdAt: Long = System.currentTimeMillis()
)
