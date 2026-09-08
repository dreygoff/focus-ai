package app.focus.domain.internal.pomodoro

import app.focus.domain.model.PomodoroConfig

/**
 * Per FR-27: PomodoroPlanner computes phase transitions for pomodoro mode.
 * FOCUS → SHORT_BREAK (after focusMinutes) → FOCUS → ... → LONG_BREAK (every cyclesBeforeLongBreak cycles)
 */
class PomodoroPlanner(val config: PomodoroConfig) {

    /** Phase names per spec. */
    enum class Phase { FOCUS, SHORT_BREAK, LONG_BREAK }

    private var _completedCycles = 0

    /** Current phase name. */
    fun currentPhase(): Phase = if (_completedCycles > 0 && _completedCycles % config.cyclesBeforeLongBreak == 0) {
        Phase.LONG_BREAK
    } else {
        when (config.currentPhaseName) {
            "SHORT_BREAK" -> Phase.SHORT_BREAK
            "LONG_BREAK" -> Phase.LONG_BREAK
            else -> Phase.FOCUS
        }
    }

    /** Remaining seconds in the current phase. */
    fun remainingSeconds(): Long = config.currentPhaseRemainingSeconds()

    /**
     * Tick one second of the current phase.
     * @return The new phase name if a transition occurred, or null if still in the same phase.
     */
    fun tick(): String? {
        return config.tick()
    }

    /** Reset all counters to initial state. */
    fun reset() {
        _completedCycles = 0
        config.reset()
    }

    /** Whether all pomodoro cycles are complete. */
    fun isComplete(): Boolean = config.isComplete()

    /** Number of remaining focus phases. */
    fun remainingFocusCycles(): Int = config.remainingFocusCycles()
}
