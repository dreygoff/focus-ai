package app.focus.domain.model

/**
 * Pomodoro timer configuration with built-in phase transition logic.
 * Implements the pomodoro workflow: FOCUS → BREAK (SHORT/LONG) → FOCUS cycle.
 */
data class PomodoroConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val cyclesBeforeLongBreak: Int = 4,
    val totalCycles: Int = 4
) {
    // Internal state
    private var currentPhase: PomodoroPhase = PomodoroPhase.FOCUS
    private var remainingSeconds: Long = focusMinutes * 60L
    private var completedCycles: Int = 0
    private var _isComplete: Boolean = false

    /** Current phase name as a human-readable string. */
    val currentPhaseName: String
        get() = when (currentPhase) {
            PomodoroPhase.FOCUS -> "FOCUS"
            PomodoroPhase.SHORT_BREAK -> "SHORT_BREAK"
            PomodoroPhase.LONG_BREAK -> "LONG_BREAK"
        }

    /** Remaining time in the current phase, in seconds. */
    fun currentPhaseRemainingSeconds(): Long {
        return remainingSeconds.coerceAtLeast(0L)
    }

    /** Check if the pomodoro cycle is complete. */
    fun isComplete(): Boolean = _isComplete

    /** Progress through one second of the current phase. Returns the new phase name or null if complete. */
    fun tick(): String? {
        if (_isComplete) return null

        remainingSeconds--

        when {
            currentPhase == PomodoroPhase.FOCUS -> {
                if (remainingSeconds <= 0) {
                    completedCycles++
                    currentPhase = if (completedCycles % cyclesBeforeLongBreak == 0 && completedCycles < totalCycles) {
                        PomodoroPhase.LONG_BREAK
                    } else {
                        PomodoroPhase.SHORT_BREAK
                    }
                    remainingSeconds = when (currentPhase) {
                        PomodoroPhase.LONG_BREAK -> longBreakMinutes * 60L
                        else -> shortBreakMinutes * 60L
                    }
                    _isComplete = completedCycles >= totalCycles
                    return if (_isComplete) null else currentPhaseName
                }
            }

            currentPhase == PomodoroPhase.SHORT_BREAK || currentPhase == PomodoroPhase.LONG_BREAK -> {
                if (remainingSeconds <= 0) {
                    currentPhase = PomodoroPhase.FOCUS
                    remainingSeconds = focusMinutes * 60L
                    return "FOCUS"
                }
            }
        }

        return null
    }

    /** Number of remaining focus phases to complete. */
    fun remainingFocusCycles(): Int {
        return (totalCycles - completedCycles).coerceAtLeast(0)
    }

    /** Reset the timer to the initial FOCUS phase. */
    fun reset() {
        currentPhase = PomodoroPhase.FOCUS
        remainingSeconds = focusMinutes * 60L
        completedCycles = 0
        _isComplete = false
    }

    enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

    companion object {
        private const val DEBUG_FOCUS_MINUTES = 1
        private const val DEBUG_SHORT_BREAK_MINUTES = 1
        private const val DEBUG_LONG_BREAK_MINUTES = 2

        private const val DEFAULT_MAX_FOCUS_MINUTES = 25

        fun forProfile(focusMinutes: Int, debugAccelerated: Boolean = false): PomodoroConfig {
            if (debugAccelerated) {
                return PomodoroConfig(
                    focusMinutes = DEBUG_FOCUS_MINUTES,
                    shortBreakMinutes = DEBUG_SHORT_BREAK_MINUTES,
                    longBreakMinutes = DEBUG_LONG_BREAK_MINUTES,
                )
            }
            return PomodoroConfig(focusMinutes = focusMinutes.coerceAtMost(DEFAULT_MAX_FOCUS_MINUTES))
        }
    }
}
