package app.focus.domain.internal.statemachine

sealed interface SideEffect {
    data class ScheduleAlarm(val atMillis: Long) : SideEffect
    data object CancelAlarm : SideEffect
    data object StartForegroundService : SideEffect
    data object StopForegroundService : SideEffect
    data class LogEvent(val type: app.focus.domain.model.EventType) : SideEffect
    data class SaveSnapshot(val snapshot: SessionSnapshot) : SideEffect
    data class RestoreSnapshot(val snapshot: SessionSnapshot) : SideEffect
    data object ClearSnapshot : SideEffect
    data object UpdateNotification : SideEffect
    data class PomodoroPhaseChanged(val oldPhase: String, val newPhase: String) : SideEffect
}
