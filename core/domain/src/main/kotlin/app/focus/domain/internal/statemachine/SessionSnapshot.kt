package app.focus.domain.internal.statemachine

data class SessionSnapshot(
    val sessionId: String,
    val lockMode: String,
    val plannedEndAtMillis: Long,
    val targetPackages: List<String>,
    val hardLockExtraPackages: List<String> = emptyList(),
    val defaultLauncherPkg: String?,
    val isPomodoro: Boolean,
    val currentPhase: String,
    val phaseEndAtMillis: Long,
    val pomodoroFocusCyclesDone: Int = 0,
)
