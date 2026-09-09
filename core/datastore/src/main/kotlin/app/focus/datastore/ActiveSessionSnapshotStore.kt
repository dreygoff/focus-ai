package app.focus.datastore

/**
 * Repository interface for managing active session snapshots in device-protected storage.
 * Used for Direct Boot compatibility (LOCKED_BOOT_COMPLETED).
 */
interface ActiveSessionSnapshotStore {
    /** Save the current active session snapshot. */
    suspend fun saveSnapshot(snapshot: SessionSnapshot)

    /** Load the saved session snapshot, or null if not found. */
    suspend fun loadSnapshot(): SessionSnapshot?

    /** Clear the saved snapshot (e.g., after successful restore). */
    suspend fun clearSnapshot()

    /** Check if a snapshot exists. */
    suspend fun hasSnapshot(): Boolean
}

/**
 * Serializable session snapshot for device-protected storage.
 */
data class SessionSnapshot(
    val sessionId: String,
    val lockMode: String, // "SOFT" | "HARD"
    val plannedEndAt: Long,
    val targetPackages: List<String>,
    val hardLockExtraPackages: List<String> = emptyList(),
    val defaultLauncher: String? = null,
    val isPomodoro: Boolean = false,
    val phaseEndAt: Long? = null,
    val currentPhase: String? = null, // "FOCUS" | "BREAK" | "LONG_BREAK"
) {
    companion object {
        /** Check if this snapshot represents an active session (not expired). */
        fun isActive(
            snapshot: SessionSnapshot?,
            now: Long = System.currentTimeMillis(),
        ): Boolean = snapshot != null && snapshot.plannedEndAt > now
    }
}
