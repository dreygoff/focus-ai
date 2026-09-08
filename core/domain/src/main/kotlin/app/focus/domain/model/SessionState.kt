package app.focus.domain.model

/** Represents the state of a focus session for persistence. */
data class SessionState(
    val sessionId: String? = null,
    val profileId: String? = null,
    val status: SessionStatus = SessionStatus.Idle,
    val targetPackages: List<String> = emptyList(),
    val lockMode: LockMode = LockMode.Soft,
    val startedAt: Long? = null,
    val plannedEndAt: Long? = null,
    val actualEndAt: Long? = null,
    val goalText: String? = null,
    val isPomodoro: Boolean = false,
    val pomodoroConfig: PomodoroConfig? = null
)
