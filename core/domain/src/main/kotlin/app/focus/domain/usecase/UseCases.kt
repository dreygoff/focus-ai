package app.focus.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

data class StartSessionResult(
    val session: app.focus.domain.model.Session,
    val alarmScheduled: Boolean
)

class StartSessionUseCase(
    private val sessionRepo: SessionRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock
) {
    suspend fun execute(
        profileId: String,
        durationMinutes: Int,
        goalText: String?,
        source: app.focus.domain.model.SessionSource = app.focus.domain.model.SessionSource.MANUAL,
        pomodoroConfig: app.focus.domain.model.PomodoroConfig? = null
    ): StartSessionResult = withContext(Dispatchers.IO) {
        val startedAt = clock.nowMillis()
        val plannedEndAt = startedAt + durationMinutes * 60L * 1000L

        val session = app.focus.domain.model.Session(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            profileNameSnapshot = "", // would be resolved from ProfileRepository
            lockMode = app.focus.domain.model.LockMode.Soft, // default
            targetPackagesSnapshot = emptyList(),
            goalText = goalText,
            startedAt = startedAt,
            plannedEndAt = plannedEndAt,
            actualEndAt = null,
            status = app.focus.domain.model.SessionStatus.Running,
            source = source,
            pomodoroConfig = pomodoroConfig,
            bypassesUsed = 0,
            blockAttempts = 0,
            pausesUsed = 0,
            scheduleId = null
        )

        sessionRepo.insert(session)

        // Save snapshot to device-protected storage for recovery on boot (TR-05)
        snapshotStore.save(
            app.focus.domain.internal.statemachine.SessionSnapshot(
                sessionId = session.id,
                lockMode = if (session.lockMode is app.focus.domain.model.LockMode.Hard) "HARD" else "SOFT",
                plannedEndAtMillis = plannedEndAt,
                targetPackages = session.targetPackagesSnapshot,
                hardLockExtraPackages = emptyList(),
                defaultLauncherPkg = null,
                isPomodoro = pomodoroConfig != null,
                currentPhase = if (pomodoroConfig != null) "FOCUS" else null ?: "FOCUS",
                phaseEndAtMillis = plannedEndAt
            )
        )

        // Schedule alarm for session end (TR-05)
        val alarmScheduled = try {
            alarmScheduler.scheduleExact(plannedEndAt, session.id.hashCode(), "app.focus.service.receiver.AlarmReceiver")
            true
        } catch (e: Exception) {
            false
        }

        StartSessionResult(session, alarmScheduled)
    }
}

data class StopSessionResult(
    val oldStatus: app.focus.domain.model.SessionStatus,
    val newStatus: app.focus.domain.model.SessionStatus
)

class StopSessionUseCase(
    private val sessionRepo: SessionRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock
) {
    suspend fun execute(sessionId: String, status: app.focus.domain.model.SessionStatus = app.focus.domain.model.SessionStatus.Cancelled): StopSessionResult {
        return withContext(Dispatchers.IO) {
            val session = sessionRepo.observeSessions(clock.nowMillis() - 86400000L, clock.nowMillis())
                .first()
                .firstOrNull { it.id == sessionId }
                ?: throw IllegalArgumentException("Session not found: $sessionId")

            val updated = session.copy(actualEndAt = clock.nowMillis(), status = status)
            sessionRepo.update(updated)

            // Clear snapshot and cancel alarm (TR-05)
            snapshotStore.clear()
            try { cancelAlarm(session) } catch(e: Exception) { /* ignore */ }

            return@withContext StopSessionResult(oldStatus = session.status, newStatus = status)
        }
    }

    private suspend fun cancelAlarm(session: app.focus.domain.model.Session) {
        alarmScheduler.cancelAlarm(session.id.hashCode(), "app.focus.service.receiver.AlarmReceiver")
    }
}

class ObserveActiveSessionsUseCase(
    private val sessionRepo: SessionRepository
) {
    fun execute(): Flow<app.focus.domain.model.Session?> = sessionRepo.observeActiveSession()
}

class GetStatsUseCase(
    private val sessionRepo: SessionRepository,
    private val clock: Clock
) {
    data class StatsSummary(
        val sessionsCompleted: Int,
        val totalFocusMinutes: Int,
        val currentStreak: Int,
        val bestStreak: Int
    )

    suspend fun execute(days: Int): Pair<List<app.focus.domain.model.DailyStats>, StatsSummary> {
        return withContext(Dispatchers.IO) {
            val startDate = clock.nowMillis() - days * 86400000L
            val endDate = clock.nowMillis()

            val dailyStats = sessionRepo.getStatsDaily(startDate, days)
            
            val summary = StatsSummary(
                sessionsCompleted = dailyStats.sumOf { it.sessionsCompleted },
                totalFocusMinutes = dailyStats.sumOf { it.focusMinutes },
                currentStreak = 0,
                bestStreak = 0
            )

            return@withContext dailyStats to summary
        }
    }
}

class CreateProfileUseCase(
    private val profileRepo: ProfileRepository,
    private val clock: Clock
) {
    suspend fun execute(
        name: String,
        emoji: String?,
        colorArgb: Int,
        lockMode: app.focus.domain.model.LockMode = app.focus.domain.model.LockMode.Soft,
        defaultDurationMinutes: Int = 25
    ): app.focus.domain.model.Profile {
        val profile = app.focus.domain.model.Profile(
            name = name,
            emoji = emoji,
            colorArgb = colorArgb,
            lockMode = lockMode,
            defaultDurationMinutes = defaultDurationMinutes,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = true,
            bypassReasonRequired = true,
            bypassPhrase = null,
            bypassLimitPerSession = 3,
            accessWindowMinutes = 5,
            bypassAppliesToAllApps = false,
            emergencyExitMode = app.focus.domain.model.EmergencyExitMode.NONE,
            blockNewApps = true,
            deviceAdminProtection = lockMode is app.focus.domain.model.LockMode.Hard,
            allowedSettingsShortcuts = emptySet(),
            hideTargetNotifications = false,
            createdAt = clock.nowMillis(),
            updatedAt = clock.nowMillis()
        )
        profileRepo.insert(profile)
        return profile
    }
}

class DecideBlockUseCase(
    private val systemAllowlist: () -> Set<String>,
    private val userAllowlistRepo: AllowlistRepository,
    private val accessWindowRepo: AccessWindowRepository,
    private val sessionState: () -> SessionCheckState
) {
    data class SessionCheckState(
        val hasActiveSession: Boolean,
        val isHardLock: Boolean,
        val sessionId: String?,
        val targetPackages: Set<String>,
        val hardLockExtraPackages: Set<String> = emptySet()
    )

    suspend fun decide(packageName: String): app.focus.domain.model.BlockDecision {
        // TR-06 rules by priority
        // 1. No active session or in pomodoro break
        if (!sessionState().hasActiveSession) return app.focus.domain.model.BlockDecision.Allow
        
        // 2. Package in allowlist
        val fullAllowlist = systemAllowlist() + userAllowlistRepo.observeAllowlist().first()
        if (fullAllowlist.contains(packageName)) return app.focus.domain.model.BlockDecision.Allow

        // 3. Active access window for this package
        sessionState().sessionId?.let { sid ->
            val windows = accessWindowRepo.observeActiveWindows(sid).first()
            if (windows.containsKey(packageName)) return app.focus.domain.model.BlockDecision.Allow
        }

        // 4. Package is a target of active session OR hard lock extra
        val state = sessionState()
        if (state.targetPackages.contains(packageName) || 
            (state.isHardLock && state.hardLockExtraPackages.contains(packageName))) {
            return app.focus.domain.model.BlockDecision.Block(
                reason = if (state.isHardLock && !state.targetPackages.contains(packageName)) {
                    app.focus.domain.model.BlockReason.HARD_LOCK_EXTRA
                } else {
                    app.focus.domain.model.BlockReason.TARGET_APP
                },
                packageName = packageName
            )
        }

        // 5. Otherwise allow
        return app.focus.domain.model.BlockDecision.Allow
    }
}

class BypassFlowExecutor(
    private val accessWindowRepo: AccessWindowRepository,
    private val clock: Clock
) {
    data class BypassResult(
        val granted: Boolean,
        val sessionRemainingSeconds: Int = 0,
        val bypassesUsed: Int = 0,
        var limitReached: Boolean = false
    )

    suspend fun executeStep(step: app.focus.domain.model.BypassState): BypassResult {
        return when (step) {
            is app.focus.domain.model.BypassState.Delay -> {
                // Show delay countdown UI - remaining seconds
                BypassResult(granted = step.remainingSeconds <= 0, sessionRemainingSeconds = step.remainingSeconds)
            }
            is app.focus.domain.model.BypassState.Reason -> {
                // Validate reason text min length 10 chars
                BypassResult(granted = step.text.length >= 10)
            }
            is app.focus.domain.model.BypassState.Phrase -> {
                // Check typedText matches targetPhrase exactly, no autocompose or paste
                BypassResult(granted = step.typedText == step.targetPhrase)
            }
            app.focus.domain.model.BypassState.Idle -> BypassResult(granted = false)
            app.focus.domain.model.BypassState.LimitReached -> BypassResult(granted = false, limitReached = true)
            else -> BypassResult(granted = false)
        }
    }

    suspend fun grantBypass(sessionId: String, packageName: String, reason: String?): Long {
        return accessWindowRepo.grant(sessionId, packageName, reason)
    }
}

class SessionStateMachineExecutor(
    private val clock: Clock
) {
    fun execute(event: app.focus.domain.internal.statemachine.SessionEvent): app.focus.domain.internal.statemachine.StateMachineResult {
        val stateMachineClock = object : app.focus.domain.internal.statemachine.Clock {
            override fun nowMillis(): Long = clock.nowMillis()
        }
        return app.focus.domain.internal.statemachine.SessionStateMachine(stateMachineClock).execute(event)
    }
}

class GrantBypassUseCase(
    private val accessWindowRepo: AccessWindowRepository,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock
) {
    suspend fun execute(sessionId: String, packageName: String, profile: app.focus.domain.model.Profile): Long {
        return withContext(Dispatchers.IO) {
            val expiresAt = System.currentTimeMillis() + profile.accessWindowMinutes * 60L * 1000L
            
            accessWindowRepo.grant(sessionId, packageName, "bypass_granted")

            // Schedule notification warning for access window expiry (30s before)
            alarmScheduler.scheduleExact(
                expiresAt - 30_000L,
                "ACCESS_WINDOW_EXPIRED_${packageName}".hashCode(),
                "app.focus.service.receiver.AlarmReceiver"
            )

            expiresAt
        }
    }
}
