package app.focus.domain.model

sealed interface SessionStatus {
    data object Idle : SessionStatus
    data object Scheduled : SessionStatus
    data object Running : SessionStatus
    data class Paused(val pausesRemaining: Int) : SessionStatus
    data object Completed : SessionStatus
    data object Cancelled : SessionStatus
    data object Expired : SessionStatus
    data class EmergencyExitPending(val untilMillis: Long) : SessionStatus
}
