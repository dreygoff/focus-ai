package app.focus.domain.model

import java.util.UUID

/**
 * Domain model representing a focus session.
 */
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileNameSnapshot: String,
    val lockMode: LockMode,
    val targetPackagesSnapshot: List<String>,
    val goalText: String?,
    val startedAt: Long,
    val plannedEndAt: Long?,      // null = infinite (soft lock)
    val actualEndAt: Long?,
    val status: SessionStatus,
    val source: SessionSource,
    val pomodoroConfig: PomodoroConfig?,
    val bypassesUsed: Int,
    val blockAttempts: Int,
    val pausesUsed: Int,
    val scheduleId: String?
) {
    /** Remaining time in milliseconds, or null if infinite. */
    fun remainingTimeMillis(now: Long): Long? = when {
        plannedEndAt == null -> null
        else -> (plannedEndAt - now).coerceAtLeast(0L)
    }

    /** Whether the session has expired based on current time. */
    fun isExpired(now: Long): Boolean {
        return plannedEndAt != null && now >= plannedEndAt && status == SessionStatus.Running
    }
}
