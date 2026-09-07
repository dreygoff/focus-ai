package app.focus.domain.model

/**
 * Denormalized daily statistics for fast chart rendering.
 */
data class DailyStats(
    val dateEpochDay: Long,
    val focusMinutes: Int,
    val sessionsCompleted: Int,
    val sessionsTotal: Int,
    val blockAttempts: Int,
    val bypasses: Int,
)
