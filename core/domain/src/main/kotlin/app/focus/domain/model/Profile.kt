package app.focus.domain.model

import java.util.UUID

enum class EmergencyExitMode {
    NONE, DELAY_10_MIN, RETYPE_TEXT
}

enum class SettingsShortcut {
    WIFI, BLUETOOTH, SOUND, NOTIFICATIONS, CELLULAR
}

/**
 * Domain model representing a focus profile.
 * A profile contains a set of target apps, lock mode (soft/hard), and configuration parameters.
 */
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String?,
    val colorArgb: Int,
    val lockMode: LockMode,
    val defaultDurationMinutes: Int,
    // Soft lock params
    val bypassDelaySeconds: Int,
    val bypassBreathingEnabled: Boolean,
    val bypassReasonRequired: Boolean,
    val bypassPhrase: String?,
    val bypassLimitPerSession: Int,
    val accessWindowMinutes: Int,
    val bypassAppliesToAllApps: Boolean,
    // Hard lock params
    val emergencyExitMode: EmergencyExitMode,
    val blockNewApps: Boolean,
    val deviceAdminProtection: Boolean,
    val allowedSettingsShortcuts: Set<SettingsShortcut>,
    val hideTargetNotifications: Boolean,
    val targetPackageNames: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
) {
    companion object {
        fun preview(name: String = "Focus"): Profile = Profile(
            name = name,
            emoji = "🎯",
            colorArgb = 0xFF2F6F6D.toInt(),
            lockMode = LockMode.Soft,
            defaultDurationMinutes = 25,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = false,
            bypassReasonRequired = true,
            bypassPhrase = "Я осознанно отвлекаюсь от своей цели",
            bypassLimitPerSession = 3,
            accessWindowMinutes = 5,
            bypassAppliesToAllApps = false,
            emergencyExitMode = EmergencyExitMode.NONE,
            blockNewApps = true,
            deviceAdminProtection = false,
            allowedSettingsShortcuts = emptySet(),
            hideTargetNotifications = false,
        )
    }
}
