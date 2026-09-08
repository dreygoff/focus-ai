package app.focus.domain.model

/**
 * Represents the permission state for a specific permission.
 */
data class PermissionState(
    val name: String,
    val granted: Boolean,
    val type: PermissionType = PermissionType.MANDATORY,
    val isOptional: Boolean = false,
    val descriptionResId: Int? = null,
    val settingsAction: String? = null
)
