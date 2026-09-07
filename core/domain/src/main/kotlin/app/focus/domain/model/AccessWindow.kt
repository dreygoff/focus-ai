package app.focus.domain.model

import java.util.UUID

/**
 * Domain model representing an access window granted during a bypass.
 */
data class AccessWindow(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val packageName: String,
    val grantedAt: Long,
    val expiresAt: Long,
    val reason: String?,
    val restrictedToActivity: String?
) {
    /** Whether this access window has expired at the given time. */
    fun isExpired(now: Long): Boolean = now >= expiresAt

    /** Remaining time in milliseconds. */
    fun remainingMillis(now: Long): Long = (expiresAt - now).coerceAtLeast(0L)
}
