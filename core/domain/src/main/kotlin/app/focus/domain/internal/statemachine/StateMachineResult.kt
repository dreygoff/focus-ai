package app.focus.domain.internal.statemachine

import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionSource
import app.focus.domain.model.SessionStatus
import app.focus.domain.model.EventType

data class StateMachineResult(
    val newState: SessionStatus,
    val sideEffects: List<SideEffect> = emptyList()
)
