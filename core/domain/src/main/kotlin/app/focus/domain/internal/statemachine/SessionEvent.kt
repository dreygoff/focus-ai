package app.focus.domain.internal.statemachine

sealed interface SessionEvent {
    data class Start(val profileId: String, val durationMinutes: Int) : SessionEvent // only from Idle
    data object Pause : SessionEvent // only from Running (soft)
    data object Resume : SessionEvent // only from Paused
    data object StopRequested : SessionEvent // only from Running (soft)
    data object EndAlarm : SessionEvent // only from Running
    data object EmergencyExitRequested : SessionEvent
    data object EmergencyExitConfirmed : SessionEvent
    data class Restore(val snapshot: SessionSnapshot) : SessionEvent
    data object Tick : SessionEvent
    data class PomodoroPhaseEnd(val phase: String) : SessionEvent // FR-27 pomodoro phase transition
}
