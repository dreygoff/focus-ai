package app.focus.domain.model

/** Represents the status of a focus session. */
sealed class SessionStatus {
    data object Idle : SessionStatus()
    data object Starting : SessionStatus()
    data object Running : SessionStatus()
    data class Paused(val pauseReason: String = "") : SessionStatus()
    data object Completed : SessionStatus()
    data object Cancelled : SessionStatus()
    data object Expired : SessionStatus()
    data object Scheduled : SessionStatus()
    data class EmergencyExitPending(val untilMillis: Long) : SessionStatus()
}
