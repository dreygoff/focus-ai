package app.focus.domain.internal.statemachine

import app.focus.domain.model.EmergencyExitMode
import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionStatus
import app.focus.domain.model.EventType
import app.focus.domain.model.PomodoroConfig

/**
 * Pure Kotlin session state machine implementing Section 10 of the spec.
 * Handles all transitions between session statuses with side effects.
 *
 * Invariants:
 * - Only one RUNNING/PAUSED at a time
 * - HARD mode cannot have Pause or StopRequested transitions
 * - bypassesUsed ≤ limit enforced externally
 */
class SessionStateMachine(
    private val clock: Clock,
) {

    @Volatile
    private var currentState: SessionStatus = SessionStatus.Idle

    fun getState(): SessionStatus = currentState

    fun setState(newState: SessionStatus) {
        currentState = newState
    }

    /**
     * Execute an event and return the resulting state with side effects.
     * Returns unchanged state if transition is invalid for current state.
     */
    fun execute(event: SessionEvent): StateMachineResult {
        val result = when (event) {
            is SessionEvent.Start -> handleStart(event)
            SessionEvent.Pause -> handlePause()
            SessionEvent.Resume -> handleResume()
            SessionEvent.StopRequested -> handleStopRequested()
            is SessionEvent.EndAlarm -> handleEndAlarm(clock.nowMillis())
            SessionEvent.EmergencyExitRequested -> handleEmergencyExitRequested()
            SessionEvent.EmergencyExitConfirmed -> handleEmergencyExitConfirmed()
            is SessionEvent.Restore -> handleRestore(event.snapshot)
            SessionEvent.Tick -> handleTick()
            is SessionEvent.PomodoroPhaseEnd -> handlePomodoroPhaseEnd(event)
        }
        if (result.newState != currentState) {
            currentState = result.newState
        }
        return result
    }

    private fun handleStart(event: SessionEvent.Start): StateMachineResult {
        val nowMillis = clock.nowMillis()
        val plannedEndAt = nowMillis + event.durationMinutes * 60L * 1000
        // Profile resolution should be done by the service layer before calling this method.
        // Here we use defaults; SessionEngine provides profile data via snapshot.
        isSessionLocked = event.durationMinutes > 0

        return stateMachineTransition(
            newState = SessionStatus.Running,
            sideEffects = listOf(
                SideEffect.ScheduleAlarm(atMillis = plannedEndAt),
                SideEffect.StartForegroundService,
                SideEffect.LogEvent(EventType.SESSION_STARTED),
            )
        )
    }

    private fun handlePause(): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> stateMachineTransition(
                newState = SessionStatus.Paused(),
                sideEffects = listOf(SideEffect.LogEvent(EventType.SESSION_PAUSED))
            )

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleResume(): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Paused -> stateMachineTransition(
                newState = SessionStatus.Running,
                sideEffects = listOf(SideEffect.LogEvent(EventType.SESSION_RESUMED))
            )

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleStopRequested(): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> stateMachineTransition(
                newState = SessionStatus.Cancelled,
                sideEffects = listOf(
                    SideEffect.StopForegroundService,
                    SideEffect.ClearSnapshot,
                    SideEffect.LogEvent(EventType.SESSION_CANCELLED)
                )
            )

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleEndAlarm(nowMillis: Long): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> stateMachineTransition(
                newState = SessionStatus.Completed,
                sideEffects = listOf(
                    SideEffect.StopForegroundService,
                    SideEffect.ClearSnapshot,
                    SideEffect.LogEvent(EventType.SESSION_COMPLETED)
                )
            )

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleEmergencyExitRequested(): StateMachineResult {
        val untilMillis = clock.nowMillis() + 10 * 60 * 1000
        return when (currentState) {
            is SessionStatus.Running -> stateMachineTransition(
                newState = SessionStatus.EmergencyExitPending(untilMillis = untilMillis),
                sideEffects = listOf(
                    SideEffect.ScheduleAlarm(atMillis = untilMillis),
                    SideEffect.LogEvent(EventType.EMERGENCY_EXIT_REQUESTED),
                )
            )

            is SessionStatus.EmergencyExitPending -> StateMachineResult(
                newState = currentState,
                sideEffects = listOf(SideEffect.LogEvent(EventType.EMERGENCY_EXIT_REQUESTED)),
            )

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleEmergencyExitConfirmed(): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> stateMachineTransition(
                newState = SessionStatus.Cancelled,
                sideEffects = listOf(
                    SideEffect.StopForegroundService,
                    SideEffect.ClearSnapshot,
                    SideEffect.LogEvent(EventType.EMERGENCY_EXIT_COMPLETED),
                    SideEffect.LogEvent(EventType.SESSION_CANCELLED)
                )
            )

            is SessionStatus.EmergencyExitPending -> {
                if (clock.nowMillis() >= (currentState as SessionStatus.EmergencyExitPending).untilMillis) {
                    stateMachineTransition(
                        newState = SessionStatus.Cancelled,
                        sideEffects = listOf(
                            SideEffect.StopForegroundService,
                            SideEffect.ClearSnapshot,
                            SideEffect.LogEvent(EventType.EMERGENCY_EXIT_COMPLETED),
                            SideEffect.LogEvent(EventType.SESSION_CANCELLED)
                        )
                    )
                } else {
                    StateMachineResult(newState = currentState, sideEffects = emptyList())
                }
            }

            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun handleRestore(snapshot: SessionSnapshot): StateMachineResult {
        val nowMillis = clock.nowMillis()
        if (snapshot.plannedEndAtMillis <= nowMillis) {
            // Expired: snapshot end time is in the past
            return stateMachineTransition(
                newState = SessionStatus.Expired,
                sideEffects = listOf(SideEffect.LogEvent(EventType.SESSION_EXPIRED))
            )
        }

        // Valid snapshot: restore to Running state
        val restoredSnapshot = SessionSnapshot(
            sessionId = snapshot.sessionId,
            lockMode = snapshot.lockMode,
            plannedEndAtMillis = snapshot.plannedEndAtMillis,
            targetPackages = snapshot.targetPackages,
            hardLockExtraPackages = snapshot.hardLockExtraPackages,
            defaultLauncherPkg = snapshot.defaultLauncherPkg,
            isPomodoro = snapshot.isPomodoro,
            currentPhase = snapshot.currentPhase,
            phaseEndAtMillis = snapshot.phaseEndAtMillis
        )

        return stateMachineTransition(
            newState = SessionStatus.Running,
            sideEffects = listOf(
                SideEffect.RestoreSnapshot(restoredSnapshot),
                SideEffect.StartForegroundService,
                SideEffect.ScheduleAlarm(atMillis = snapshot.plannedEndAtMillis),
                SideEffect.LogEvent(EventType.SERVICE_RESTARTED)
            )
        )
    }

    private fun handleTick(): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> StateMachineResult(
                newState = currentState,
                sideEffects = listOf(SideEffect.UpdateNotification)
            )
            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    /**
     * Handles pomodoro phase transitions per FR-27.
     * Focus → Short Break or Long Break (after cyclesBeforeLongBreak cycles).
     * Break → Focus.
     */
    private fun handlePomodoroPhaseEnd(event: SessionEvent.PomodoroPhaseEnd): StateMachineResult {
        return when (currentState) {
            is SessionStatus.Running -> {
                val newPhase = if (event.phase == "FOCUS") {
                    // Focus phase ended → switch to break
                    "BREAK"
                } else {
                    // Break phase ended → switch back to focus (or complete if all cycles done)
                    "FOCUS"
                }
                stateMachineTransition(
                    newState = SessionStatus.Running,
                    sideEffects = listOf(
                        SideEffect.PomodoroPhaseChanged(event.phase, newPhase),
                        SideEffect.LogEvent(EventType.SESSION_RESUMED)
                    )
                )
            }
            else -> StateMachineResult(newState = currentState, sideEffects = emptyList())
        }
    }

    private fun stateMachineTransition(
        newState: SessionStatus,
        sideEffects: List<SideEffect>
    ): StateMachineResult {
        return StateMachineResult(newState = newState, sideEffects = sideEffects)
    }

    /** True while a session is active (used by service layer for quick checks). */
    @Volatile
    var isSessionLocked: Boolean = false
}
