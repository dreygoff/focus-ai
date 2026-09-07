package app.focus.domain.model

/**
 * Domain model representing an allowlist entry.
 */
data class AllowlistEntry(
    val packageName: String,
    val addedAt: Long,
)
