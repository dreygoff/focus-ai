package app.focus.datastore

/**
 * User settings stored in Proto DataStore.
 */
data class UserSettings(
    val theme: Theme = Theme.SYSTEM,
    val dynamicColor: Boolean = true,
    val languageTag: String = "ru",
    val onboardingCompleted: Boolean = false,
    val blockVibration: Boolean = false,
    val blockSound: Boolean = false,
    val quotesEnabled: Boolean = true,
    val customGoalDefault: String = "",
    val lastUsedProfileId: String? = null,
    val lastUsedDurationMinutes: Int = 25,
    val accessibilityDisclosureAccepted: Boolean = false,
) {
    enum class Theme { SYSTEM, LIGHT, DARK }

    companion object {
        val DEFAULT = UserSettings()
    }
}

/**
 * Snapshot of an active focus session, stored in device-protected storage for Direct Boot support.
 */
data class ActiveSessionSnapshot(
    val sessionId: String,
    val lockMode: String, // SOFT or HARD
    val plannedEndAtMillis: Long,
    val targetPackages: List<String> = emptyList(),
    val hardLockExtraPackages: List<String> = emptyList(),
    val defaultLauncherPkg: String? = null,
    val isPomodoro: Boolean = false,
    val currentPhase: String? = null, // "FOCUS" or "BREAK"
    val phaseEndAtMillis: Long? = null,
) {
    companion object {
        fun empty(): ActiveSessionSnapshot = ActiveSessionSnapshot(
            sessionId = "",
            lockMode = "",
            plannedEndAtMillis = 0L,
        )

        /** Check if the snapshot represents a valid (non-expired) session. */
        fun isValid(snapshot: ActiveSessionSnapshot?): Boolean {
            return snapshot != null && snapshot.sessionId.isNotEmpty() && snapshot.plannedEndAtMillis > System.currentTimeMillis()
        }
    }
}
