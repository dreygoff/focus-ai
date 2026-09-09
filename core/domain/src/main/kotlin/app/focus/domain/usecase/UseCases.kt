package app.focus.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

data class StartSessionResult(
    val session: app.focus.domain.model.Session,
    val alarmScheduled: Boolean,
)

data class StartSessionRequest(
    val profileId: String,
    val durationMinutes: Int,
    val goalText: String? = null,
    val source: app.focus.domain.model.SessionSource = app.focus.domain.model.SessionSource.MANUAL,
    val pomodoroConfig: app.focus.domain.model.PomodoroConfig? = null,
    val scheduleId: String? = null,
    val targetPackagesOverride: List<String>? = null,
    /** Absolute end time; overrides [durationMinutes] when set (FR-21 «до времени»). */
    val plannedEndAtMillis: Long? = null,
    /** Soft lock only — session runs until manually stopped (FR-21 «бесконечно»). */
    val infinite: Boolean = false,
)

class StartSessionUseCase(
    private val sessionRepo: SessionRepository,
    private val profileRepo: ProfileRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val alarmScheduler: AlarmSchedulerService,
    private val sessionRuntime: SessionRuntimeController,
    private val hardLockExtras: HardLockExtrasContributor,
    private val hardLockLifecycle: HardLockLifecycleController,
    private val clock: Clock
) {
    suspend fun execute(request: StartSessionRequest): StartSessionResult = withContext(Dispatchers.IO) {
        val profile = profileRepo.getProfile(request.profileId)
            ?: throw IllegalArgumentException("Profile not found: ${request.profileId}")

        val startedAt = clock.nowMillis()
        val plannedEndAt = resolvePlannedEndAt(startedAt, request, profile)
        val targetPackages = request.targetPackagesOverride ?: profile.targetPackageNames

        val session = buildSession(request, profile, startedAt, plannedEndAt, targetPackages)
        sessionRepo.insert(session)

        val isHardLock = session.lockMode is app.focus.domain.model.LockMode.Hard
        val hardExtras = if (isHardLock) hardLockExtras.resolveExtras() else HardLockExtras(emptyList(), null)
        val phaseEndAt = pomodoroPhaseEndAt(startedAt, request.pomodoroConfig, plannedEndAt)

        saveSnapshot(
            SnapshotWrite(
                session = session,
                targetPackages = targetPackages,
                isHardLock = isHardLock,
                hardExtras = hardExtras,
                pomodoroConfig = request.pomodoroConfig,
                phaseEndAt = phaseEndAt,
            ),
        )

        if (isHardLock) {
            hardLockLifecycle.onHardLockSessionStarted(profile.deviceAdminProtection)
        }

        schedulePomodoroPhaseAlarm(session.id, request.pomodoroConfig, phaseEndAt)
        val alarmScheduled = plannedEndAt?.let { endAt -> scheduleSessionEndAlarm(session.id, endAt) } ?: false

        sessionRuntime.onSessionStarted(
            sessionId = session.id,
            profileName = profile.name,
            plannedEndAtMillis = plannedEndAt ?: 0L,
        )

        StartSessionResult(session, alarmScheduled)
    }

    private fun buildSession(
        request: StartSessionRequest,
        profile: app.focus.domain.model.Profile,
        startedAt: Long,
        plannedEndAt: Long?,
        targetPackages: List<String>,
    ): app.focus.domain.model.Session = app.focus.domain.model.Session(
        id = UUID.randomUUID().toString(),
        profileId = request.profileId,
        profileNameSnapshot = profile.name,
        lockMode = profile.lockMode,
        targetPackagesSnapshot = targetPackages,
        goalText = request.goalText,
        startedAt = startedAt,
        plannedEndAt = plannedEndAt,
        actualEndAt = null,
        status = app.focus.domain.model.SessionStatus.Running,
        source = request.source,
        pomodoroConfig = request.pomodoroConfig,
        bypassesUsed = 0,
        blockAttempts = 0,
        pausesUsed = 0,
        scheduleId = request.scheduleId,
    )

    private fun resolvePlannedEndAt(
        startedAt: Long,
        request: StartSessionRequest,
        profile: app.focus.domain.model.Profile,
    ): Long? = when {
        request.infinite && profile.lockMode is app.focus.domain.model.LockMode.Soft -> null
        request.plannedEndAtMillis != null -> request.plannedEndAtMillis
        else -> startedAt + request.durationMinutes * 60L * 1000L
    }

    private fun pomodoroPhaseEndAt(
        startedAt: Long,
        pomodoroConfig: app.focus.domain.model.PomodoroConfig?,
        plannedEndAt: Long?,
    ): Long = if (pomodoroConfig != null) {
        startedAt + pomodoroConfig.focusMinutes * 60L * 1000L
    } else {
        plannedEndAt ?: 0L
    }

    private data class SnapshotWrite(
        val session: app.focus.domain.model.Session,
        val targetPackages: List<String>,
        val isHardLock: Boolean,
        val hardExtras: HardLockExtras,
        val pomodoroConfig: app.focus.domain.model.PomodoroConfig?,
        val phaseEndAt: Long,
    )

    private suspend fun saveSnapshot(write: SnapshotWrite) {
        snapshotStore.save(
            app.focus.domain.internal.statemachine.SessionSnapshot(
                sessionId = write.session.id,
                lockMode = if (write.isHardLock) "HARD" else "SOFT",
                plannedEndAtMillis = write.session.plannedEndAt ?: write.phaseEndAt,
                targetPackages = write.targetPackages,
                hardLockExtraPackages = write.hardExtras.extraPackages,
                defaultLauncherPkg = write.hardExtras.defaultLauncherPkg,
                isPomodoro = write.pomodoroConfig != null,
                currentPhase = "FOCUS",
                phaseEndAtMillis = write.phaseEndAt,
                pomodoroFocusCyclesDone = 0,
            ),
        )
    }

    private suspend fun schedulePomodoroPhaseAlarm(
        sessionId: String,
        pomodoroConfig: app.focus.domain.model.PomodoroConfig?,
        phaseEndAt: Long,
    ) {
        if (pomodoroConfig == null) return
        alarmScheduler.scheduleExact(
            alarmMillis = phaseEndAt,
            operationCode = pomodoroPhaseCode(sessionId),
            receiverClassName = ALARM_RECEIVER,
            sessionId = sessionId,
            action = ACTION_POMODORO_PHASE_END,
        )
    }

    private suspend fun scheduleSessionEndAlarm(sessionId: String, plannedEndAt: Long): Boolean = try {
        alarmScheduler.scheduleExact(
            alarmMillis = plannedEndAt,
            operationCode = sessionId.hashCode(),
            receiverClassName = ALARM_RECEIVER,
            sessionId = sessionId,
        )
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        const val ACTION_POMODORO_PHASE_END = "app.focus.service.focus.ACTION_POMODORO_PHASE_END"
        private const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"

        fun pomodoroPhaseCode(sessionId: String): Int = "pomodoro_$sessionId".hashCode()
    }
}

data class StopSessionResult(
    val oldStatus: app.focus.domain.model.SessionStatus,
    val newStatus: app.focus.domain.model.SessionStatus
)

class StopSessionUseCase(
    private val deps: StopSessionDependencies,
    private val updateDailyStats: UpdateDailyStatsOnSessionEndUseCase,
    private val logSessionEndEvent: LogSessionEndEventUseCase,
    private val clock: Clock,
) {
    suspend fun execute(
        sessionId: String,
        status: app.focus.domain.model.SessionStatus = app.focus.domain.model.SessionStatus.Cancelled,
        stopForegroundService: Boolean = true,
    ): StopSessionResult {
        return withContext(Dispatchers.IO) {
            val session = deps.sessionRepo.observeSessions(clock.nowMillis() - 86400000L, clock.nowMillis())
                .first()
                .firstOrNull { it.id == sessionId }
                ?: throw IllegalArgumentException("Session not found: $sessionId")

            val updated = session.copy(actualEndAt = clock.nowMillis(), status = status)
            deps.sessionRepo.update(updated)
            logSessionEndEvent.execute(sessionId, status)
            updateDailyStats.execute(updated)

            if (session.lockMode is app.focus.domain.model.LockMode.Hard) {
                val profile = deps.profileRepo.getProfile(session.profileId)
                deps.hardLockLifecycle.onHardLockSessionStopped(profile?.deviceAdminProtection == true)
            }

            deps.snapshotStore.clear()
            try { cancelAlarm(session) } catch(e: Exception) { /* ignore */ }
            if (stopForegroundService) {
                deps.sessionRuntime.syncStopService()
            }

            return@withContext StopSessionResult(oldStatus = session.status, newStatus = status)
        }
    }

    private suspend fun cancelAlarm(session: app.focus.domain.model.Session) {
        deps.alarmScheduler.cancelAlarm(session.id.hashCode(), "app.focus.service.focus.AlarmReceiver")
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
    private val sessionState: () -> SessionCheckState,
    private val inCallDialerPackage: () -> String? = { null },
) {
    data class SessionCheckState(
        val hasActiveSession: Boolean,
        val isPaused: Boolean = false,
        val inPomodoroBreak: Boolean = false,
        val isHardLock: Boolean,
        val sessionId: String?,
        val targetPackages: Set<String>,
        val hardLockExtraPackages: Set<String> = emptySet(),
    )

    suspend fun decide(packageName: String): app.focus.domain.model.BlockDecision {
        val state = sessionState()
        if (!state.hasActiveSession || state.isPaused || state.inPomodoroBreak) {
            return app.focus.domain.model.BlockDecision.Allow
        }
        // 2. Package in allowlist
        val fullAllowlist = systemAllowlist() + userAllowlistRepo.observeAllowlist().first()
        if (fullAllowlist.contains(packageName)) return app.focus.domain.model.BlockDecision.Allow

        // 3. Active phone call — allow default dialer (US-08 / TR-06)
        inCallDialerPackage()?.let { dialer ->
            if (packageName == dialer) return app.focus.domain.model.BlockDecision.Allow
        }

        // 4. Active access window for this package
        sessionState().sessionId?.let { sid ->
            val windows = accessWindowRepo.observeActiveWindows(sid).first()
            if (windows.containsKey(packageName)) return app.focus.domain.model.BlockDecision.Allow
        }

        // 5. Package is a target of active session OR hard lock extra
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

        // 6. Otherwise allow
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

    suspend fun grantBypass(sessionId: String, packageName: String, reason: String?, durationMinutes: Int): Long {
        return accessWindowRepo.grant(sessionId, packageName, reason, durationMinutes)
    }
}

class PauseSessionUseCase(
    private val sessionRepo: SessionRepository,
    private val eventLogRepo: EventLogRepository,
    private val blockState: ActiveSessionBlockState,
    private val clock: Clock,
) {
    suspend fun execute(sessionId: String): app.focus.domain.model.Session = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: error("No active session")
        check(session.id == sessionId) { "Session mismatch" }
        check(session.lockMode is app.focus.domain.model.LockMode.Soft) { "Pause only in soft lock" }
        check(session.status is app.focus.domain.model.SessionStatus.Running) { "Session not running" }
        check(session.pausesUsed < MAX_PAUSES) { "Max pauses reached" }

        val updated = session.copy(
            status = app.focus.domain.model.SessionStatus.Paused(),
            pausesUsed = session.pausesUsed + 1,
        )
        sessionRepo.update(updated)
        blockState.setPaused(true)
        eventLogRepo.log(
            app.focus.domain.model.EventLog(
                timestamp = clock.nowMillis(),
                sessionId = sessionId,
                type = app.focus.domain.model.EventType.SESSION_PAUSED,
                packageName = null,
                payload = null,
            ),
        )
        updated
    }

    companion object {
        const val MAX_PAUSES = 3
    }
}

class ResumeSessionUseCase(
    private val sessionRepo: SessionRepository,
    private val eventLogRepo: EventLogRepository,
    private val blockState: ActiveSessionBlockState,
    private val clock: Clock,
) {
    suspend fun execute(sessionId: String): app.focus.domain.model.Session = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: error("No active session")
        check(session.id == sessionId) { "Session mismatch" }
        check(session.status is app.focus.domain.model.SessionStatus.Paused) { "Session not paused" }

        val updated = session.copy(status = app.focus.domain.model.SessionStatus.Running)
        sessionRepo.update(updated)
        blockState.setPaused(false)
        eventLogRepo.log(
            app.focus.domain.model.EventLog(
                timestamp = clock.nowMillis(),
                sessionId = sessionId,
                type = app.focus.domain.model.EventType.SESSION_RESUMED,
                packageName = null,
                payload = null,
            ),
        )
        updated
    }
}

class GrantBypassUseCase(
    private val accessWindowRepo: AccessWindowRepository,
    private val sessionRepo: SessionRepository,
    private val eventLogRepo: EventLogRepository,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock,
) {
    data class Result(val expiresAt: Long, val grantedPackages: List<String>)

    suspend fun execute(
        sessionId: String,
        packageName: String,
        reason: String?,
        profile: app.focus.domain.model.Profile,
        targetPackages: List<String>,
    ): Result = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: error("No active session")

        val packages = if (profile.bypassAppliesToAllApps) {
            targetPackages.ifEmpty { listOf(packageName) }
        } else {
            listOf(packageName)
        }

        var expiresAt = 0L
        packages.forEach { pkg ->
            expiresAt = accessWindowRepo.grant(
                sessionId = sessionId,
                packageName = pkg,
                reason = reason,
                durationMinutes = profile.accessWindowMinutes,
            )
        }

        sessionRepo.update(session.copy(bypassesUsed = session.bypassesUsed + 1))

        eventLogRepo.log(
            app.focus.domain.model.EventLog(
                timestamp = clock.nowMillis(),
                sessionId = sessionId,
                type = app.focus.domain.model.EventType.BYPASS_GRANTED,
                packageName = packageName,
                payload = reason,
            ),
        )

        val warningAt = expiresAt - WARNING_BEFORE_EXPIRY_MS
        if (warningAt > clock.nowMillis()) {
            alarmScheduler.scheduleExact(
                alarmMillis = warningAt,
                operationCode = accessWindowWarningCode(sessionId, packageName),
                receiverClassName = ALARM_RECEIVER,
                sessionId = sessionId,
                action = ACTION_ACCESS_WINDOW_WARNING,
                extras = mapOf(KEY_PACKAGE_NAME to packageName),
            )
        }

        alarmScheduler.scheduleExact(
            alarmMillis = expiresAt,
            operationCode = accessWindowExpiryCode(sessionId, packageName),
            receiverClassName = ALARM_RECEIVER,
            sessionId = sessionId,
            action = ACTION_ACCESS_WINDOW_EXPIRED,
            extras = mapOf(KEY_PACKAGE_NAME to packageName),
        )

        Result(expiresAt, packages)
    }

    companion object {
        const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"
        const val ACTION_ACCESS_WINDOW_WARNING = "app.focus.service.focus.ACTION_ACCESS_WINDOW_WARNING"
        const val ACTION_ACCESS_WINDOW_EXPIRED = "app.focus.service.focus.ACTION_ACCESS_WINDOW_EXPIRED"
        const val KEY_PACKAGE_NAME = "packageName"
        private const val WARNING_BEFORE_EXPIRY_MS = 30_000L

        fun accessWindowWarningCode(sessionId: String, packageName: String): Int =
            "warn_${sessionId}_$packageName".hashCode()

        fun accessWindowExpiryCode(sessionId: String, packageName: String): Int =
            "exp_${sessionId}_$packageName".hashCode()
    }
}

class RequestEmergencyExitUseCase(
    private val sessionRepo: SessionRepository,
    private val profileRepo: ProfileRepository,
    private val eventLogRepo: EventLogRepository,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock,
) {
    sealed interface Result {
        data class DelayStarted(val untilMillis: Long) : Result
        data class RetypeRequired(val targetText: String) : Result
        data object NotAvailable : Result
    }

    suspend fun execute(sessionId: String): Result = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: return@withContext Result.NotAvailable
        check(session.id == sessionId) { "Session mismatch" }
        check(session.lockMode is app.focus.domain.model.LockMode.Hard) { "Emergency exit only in hard lock" }

        val profile = profileRepo.getProfile(session.profileId) ?: return@withContext Result.NotAvailable
        when (profile.emergencyExitMode) {
            app.focus.domain.model.EmergencyExitMode.NONE -> Result.NotAvailable
            app.focus.domain.model.EmergencyExitMode.DELAY_10_MIN -> {
                val untilMillis = clock.nowMillis() + DELAY_MS
                sessionRepo.update(
                    session.copy(status = app.focus.domain.model.SessionStatus.EmergencyExitPending(untilMillis)),
                )
                alarmScheduler.scheduleExact(
                    alarmMillis = untilMillis,
                    operationCode = emergencyExitCode(sessionId),
                    receiverClassName = ALARM_RECEIVER,
                    sessionId = sessionId,
                    action = ACTION_EMERGENCY_EXIT_COMPLETE,
                )
                eventLogRepo.log(
                    app.focus.domain.model.EventLog(
                        timestamp = clock.nowMillis(),
                        sessionId = sessionId,
                        type = app.focus.domain.model.EventType.EMERGENCY_EXIT_REQUESTED,
                        packageName = null,
                        payload = null,
                    ),
                )
                Result.DelayStarted(untilMillis)
            }
            app.focus.domain.model.EmergencyExitMode.RETYPE_TEXT -> {
                Result.RetypeRequired(app.focus.domain.model.EmergencyExitTextGenerator.generate())
            }
        }
    }

    companion object {
        const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"
        const val ACTION_EMERGENCY_EXIT_COMPLETE = "app.focus.service.focus.ACTION_EMERGENCY_EXIT_COMPLETE"
        private const val DELAY_MS = 10 * 60 * 1000L

        fun emergencyExitCode(sessionId: String): Int = "emergency_$sessionId".hashCode()
    }
}

class CancelEmergencyExitUseCase(
    private val sessionRepo: SessionRepository,
    private val alarmScheduler: AlarmSchedulerService,
) {
    suspend fun execute(sessionId: String) = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: error("No active session")
        check(session.id == sessionId) { "Session mismatch" }
        check(session.status is app.focus.domain.model.SessionStatus.EmergencyExitPending) {
            "No pending emergency exit"
        }

        sessionRepo.update(session.copy(status = app.focus.domain.model.SessionStatus.Running))
        alarmScheduler.cancelAlarm(
            operationCode = RequestEmergencyExitUseCase.emergencyExitCode(sessionId),
            receiverClassName = RequestEmergencyExitUseCase.ALARM_RECEIVER,
        )
    }
}

class CompleteEmergencyExitUseCase(
    private val sessionRepo: SessionRepository,
    private val stopSessionUseCase: StopSessionUseCase,
    private val eventLogRepo: EventLogRepository,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock,
) {
    suspend fun execute(sessionId: String) = withContext(Dispatchers.IO) {
        runCatching {
            alarmScheduler.cancelAlarm(
                operationCode = RequestEmergencyExitUseCase.emergencyExitCode(sessionId),
                receiverClassName = RequestEmergencyExitUseCase.ALARM_RECEIVER,
            )
        }

        stopSessionUseCase.execute(sessionId, app.focus.domain.model.SessionStatus.Cancelled)
        eventLogRepo.log(
            app.focus.domain.model.EventLog(
                timestamp = clock.nowMillis(),
                sessionId = sessionId,
                type = app.focus.domain.model.EventType.EMERGENCY_EXIT_COMPLETED,
                packageName = null,
                payload = null,
            ),
        )
    }
}

class SessionStateMachineExecutor(
    private val clock: Clock,
) {
    fun execute(event: app.focus.domain.internal.statemachine.SessionEvent): app.focus.domain.internal.statemachine.StateMachineResult {
        val stateMachineClock = object : app.focus.domain.internal.statemachine.Clock {
            override fun nowMillis(): Long = clock.nowMillis()
        }
        return app.focus.domain.internal.statemachine.SessionStateMachine(stateMachineClock).execute(event)
    }
}
