package app.focus.domain.model

/**
 * Represents the permission state for a specific permission.
 */
data class PermissionState(
    val name: String,                     // Android permission string
    val granted: Boolean,
    val isOptional: Boolean = false,
    val descriptionResId: Int? = null,    // resource ID for description text
    val settingsAction: String? = null,   // deep link action or setting intent
)
