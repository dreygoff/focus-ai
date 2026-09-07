package app.focus.domain.model

/**
 * Represents a target application associated with a profile.
 */
data class ProfileApp(
    val profileId: String,
    val packageName: String,
    val addedAt: Long,
)
