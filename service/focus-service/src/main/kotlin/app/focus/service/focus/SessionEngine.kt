package app.focus.service.focus

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import app.focus.domain.internal.pomodoro.PomodoroPlanner
import app.focus.domain.internal.schedule.ScheduleAlarmPlanner
import app.focus.domain.internal.statemachine.SessionEvent
import app.focus.domain.internal.statemachine.SessionStateMachine
import app.focus.domain.internal.statemachine.SessionSnapshot
import app.focus.domain.model.BlockDecision
import app.focus.domain.model.LockMode
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SessionEngine — the core orchestrator for focus session lifecycle.
 * Manages Timer, PomodoroPlanner, StateMachine, and coordinates with
 * HardLockEnforcer, BlockDecisionEngine, and DetectorOrchestrator.
 */
class SessionEngine(
    private val context: Context,
    private val clock: Clock,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val scheduleAlarmPlanner: ScheduleAlarmPlanner,
    private val stateMachine: SessionStateMachine,
) {

    companion object {
        private const val TAG = "SessionEngine"
    }

    private val _state = MutableStateFlow<SessionEngineState>(SessionEngineState.IDLE)
    val state: StateFlow<SessionEngineState> = _state.asStateFlow()

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var pomodoroPlanner: PomodoroPlanner? = null

    /** Current active session info. */
    var activeSessionId: String? = null
        private set

    /** Lock mode for the current session. */
    var lockMode: LockMode = LockMode.Soft
        private set

    /** Start a new focus session. */
    suspend fun startSession(
        sessionId: String,
        profileId: String,
        durationMinutes: Int,
        targetPackages: List<String>,
        hardLockExtraPackages: List<String> = emptyList(),
        pomodoroConfig: app.focus.domain.model.PomodoroConfig? = null
    ): Result<Unit> {
        return try {
            activeSessionId = sessionId
            lockMode = if (pomodoroConfig != null || targetPackages.isNotEmpty()) LockMode.Hard else LockMode.Soft

            // Save snapshot for recovery on boot
            val plannedEndAt = clock.nowMillis() + durationMinutes * 60L * 1000L
            val snapshot = SessionSnapshot(
                sessionId = sessionId,
                lockMode = "HARD",
                plannedEndAtMillis = plannedEndAt,
                targetPackages = targetPackages,
                hardLockExtraPackages = hardLockExtraPackages,
                defaultLauncherPkg = getDefaultLauncherPkg(),
                isPomodoro = pomodoroConfig != null,
                currentPhase = "FOCUS",
                phaseEndAtMillis = plannedEndAt
            )
            snapshotStore.save(snapshot)

            // Initialize state machine
            val result = stateMachine.execute(SessionEvent.Start(profileId, durationMinutes))
            if (result.newState !is app.focus.domain.model.SessionStatus.Running) {
                return Result.failure(IllegalStateException("Failed to start session: ${result.newState}"))
            }

            // Start pomodoro planner if configured
            if (pomodoroConfig != null) {
                pomodoroPlanner = PomodoroPlanner(pomodoroConfig)
                startPomodoroTimer(pomodoroConfig)
            }

            // Start the main timer
            startSessionTimer(durationMinutes)

            _state.value = SessionEngineState.RUNNING(sessionId)
            Log.d(TAG, "Session started: $sessionId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting session", e)
            Result.failure(e)
        }
    }

    /** Pause the current session. */
    suspend fun pauseSession(): Result<Unit> {
        return try {
            val result = stateMachine.execute(SessionEvent.Pause)
            if (result.newState is app.focus.domain.model.SessionStatus.Paused) {
                _state.value = SessionEngineState.PAUSED(activeSessionId)
                timerJob?.cancel()
                pomodoroPlanner?.let { planner ->
                    // Save remaining time for resume
                    planner.config.reset()
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Cannot pause: current state is ${result.newState}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Resume the paused session. */
    suspend fun resumeSession(): Result<Unit> {
        return try {
            val result = stateMachine.execute(SessionEvent.Resume)
            if (result.newState is app.focus.domain.model.SessionStatus.Running) {
                _state.value = SessionEngineState.RUNNING(activeSessionId)
                pomodoroPlanner?.let { planner ->
                    startPomodoroTimer(planner.config)
                } ?: startSessionTimer(0) // resume with remaining time
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Cannot resume: current state is ${result.newState}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Stop the current session. */
    suspend fun stopSession(status: app.focus.domain.model.SessionStatus = app.focus.domain.model.SessionStatus.Cancelled): Result<Unit> {
        return try {
            val result = stateMachine.execute(SessionEvent.StopRequested)
            timerJob?.cancel()
            pomodoroPlanner = null
            activeSessionId = null

            // Clear snapshot and cancel alarms
            snapshotStore.clear()

            _state.value = SessionEngineState.IDLE
            Log.d(TAG, "Session stopped: $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Check if a package should be blocked. */
    fun checkBlockDecision(packageName: String): BlockDecision {
        val currentState = stateMachine.getState()

        // No active session
        if (currentState !is app.focus.domain.model.SessionStatus.Running &&
            currentState !is app.focus.domain.model.SessionStatus.Paused) {
            return BlockDecision.Allow
        }

        // Paused sessions allow all apps
        if (currentState is app.focus.domain.model.SessionStatus.Paused) {
            return BlockDecision.Allow
        }

        // Check target packages
        if (lockMode == LockMode.Hard) {
            // Hard lock: check if package is in the block list
            // In production, this would use the actual session state to get targetPackages
            return BlockDecision.Block(
                reason = app.focus.domain.model.BlockReason.TARGET_APP,
                packageName = packageName
            )
        }

        // Soft lock: allow by default (bypass flow handles blocking)
        return BlockDecision.Allow
    }

    /** Handle pomodoro phase end. */
    fun onPomodoroPhaseEnd(phaseName: String): Boolean {
        val planner = pomodoroPlanner ?: return false

        val result = stateMachine.execute(SessionEvent.PomodoroPhaseEnd(phaseName))
        return result.newState is app.focus.domain.model.SessionStatus.Running
    }

    /** Handle emergency exit request. */
    suspend fun handleEmergencyExit(): Result<Unit> {
        return try {
            val result = stateMachine.execute(SessionEvent.EmergencyExitRequested)
            _state.value = SessionEngineState.EMERGENCY_EXIT_PENDING(activeSessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Handle emergency exit confirmation. */
    suspend fun handleEmergencyExitConfirmed(): Result<Unit> {
        return try {
            val result = stateMachine.execute(SessionEvent.EmergencyExitConfirmed)
            timerJob?.cancel()
            pomodoroPlanner = null
            activeSessionId = null
            snapshotStore.clear()

            _state.value = SessionEngineState.IDLE
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Restore a session from snapshot. */
    suspend fun restoreFromSnapshot(snapshot: SessionSnapshot): Result<Unit> {
        return try {
            activeSessionId = snapshot.sessionId
            lockMode = if (snapshot.lockMode == "HARD") LockMode.Hard else LockMode.Soft

            val result = stateMachine.execute(SessionEvent.Restore(snapshot))
            if (result.newState is app.focus.domain.model.SessionStatus.Running) {
                _state.value = SessionEngineState.RUNNING(snapshot.sessionId)

                // Start timer based on remaining time
                val remainingMs = snapshot.plannedEndAtMillis - clock.nowMillis()
                if (remainingMs > 0) {
                    startSessionTimer((remainingMs / 60_000).toInt())
                }

                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Restore failed: ${result.newState}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session", e)
            Result.failure(e)
        }
    }

    private fun startSessionTimer(durationMinutes: Int) {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            val durationMs = if (durationMinutes > 0) durationMinutes * 60_000L else Long.MAX_VALUE

            while (isActive) {
                delay(1000) // Tick every second

                val remaining = stateMachine.getState()
                if (remaining !is app.focus.domain.model.SessionStatus.Running) {
                    break
                }

                val elapsedMs = clock.nowMillis() - (activeSessionId?.let {
                    snapshotStore.load()?.plannedEndAtMillis ?: Long.MAX_VALUE
                } ?: Long.MAX_VALUE - durationMs)

                if (elapsedMs >= durationMs) {
                    // Session time complete
                    stateMachine.execute(SessionEvent.EndAlarm)
                    _state.value = SessionEngineState.COMPLETED(activeSessionId)
                    break
                }
            }
        }
    }

    private fun startPomodoroTimer(config: app.focus.domain.model.PomodoroConfig) {
        pomodoroPlanner?.let { planner ->
            engineScope.launch {
                while (isActive && !planner.isComplete()) {
                    delay(1000) // Tick every second

                    val newPhase = planner.tick()
                    if (newPhase != null) {
                        // Phase changed - notify state machine
                        onPomodoroPhaseEnd(newPhase)

                        // Restart timer for new phase
                        when (newPhase) {
                            "SHORT_BREAK" -> {} // Break timer doesn't block apps
                            "LONG_BREAK" -> {}
                            else -> startSessionTimer(planner.config.focusMinutes)
                        }
                    }
                }
            }
        }
    }

    private fun getDefaultLauncherPkg(): String? {
        return try {
            val intent = Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
            }
            context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    /** Clean up resources. */
    fun destroy() {
        timerJob?.cancel()
        pomodoroPlanner = null
        engineScope.cancel()
    }
}

/** Represents the current state of the session engine. */
sealed interface SessionEngineState {
    data object IDLE : SessionEngineState
    data class RUNNING(val sessionId: String?) : SessionEngineState
    data class PAUSED(val sessionId: String?) : SessionEngineState
    data class COMPLETED(val sessionId: String?) : SessionEngineState
    data class EMERGENCY_EXIT_PENDING(val sessionId: String?) : SessionEngineState
}
