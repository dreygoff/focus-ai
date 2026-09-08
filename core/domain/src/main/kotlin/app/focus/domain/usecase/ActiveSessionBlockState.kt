package app.focus.domain.usecase

import app.focus.domain.internal.statemachine.SessionSnapshot

/**
 * Holds the active session context used by [DecideBlockUseCase] from the foreground service.
 */
class ActiveSessionBlockState {
    @Volatile
    var sessionState: DecideBlockUseCase.SessionCheckState =
        DecideBlockUseCase.SessionCheckState(
            hasActiveSession = false,
            isPaused = false,
            isHardLock = false,
            sessionId = null,
            targetPackages = emptySet(),
        )

    fun updateFromSnapshot(snapshot: SessionSnapshot, isPaused: Boolean = false) {
        sessionState = DecideBlockUseCase.SessionCheckState(
            hasActiveSession = true,
            isPaused = isPaused,
            isHardLock = snapshot.lockMode == "HARD",
            sessionId = snapshot.sessionId,
            targetPackages = snapshot.targetPackages.toSet(),
            hardLockExtraPackages = snapshot.hardLockExtraPackages.toSet(),
        )
    }

    fun setPaused(paused: Boolean) {
        sessionState = sessionState.copy(isPaused = paused)
    }

    fun clear() {
        sessionState = DecideBlockUseCase.SessionCheckState(
            hasActiveSession = false,
            isPaused = false,
            isHardLock = false,
            sessionId = null,
            targetPackages = emptySet(),
        )
    }
}
