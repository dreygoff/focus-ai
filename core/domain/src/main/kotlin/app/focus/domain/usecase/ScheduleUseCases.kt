package app.focus.domain.usecase

import app.focus.domain.internal.schedule.ScheduleAlarmPlanner
import app.focus.domain.model.LockMode
import app.focus.domain.model.SessionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PlanSchedulesUseCase(
    private val scheduleRepo: ScheduleRepository,
    private val profileRepo: ProfileRepository,
    private val planner: ScheduleAlarmPlanner,
    private val alarmScheduler: AlarmSchedulerService,
    private val clock: Clock,
) {
    suspend fun replanAll() = withContext(Dispatchers.IO) {
        val schedules = scheduleRepo.observeSchedules().first()
        schedules.forEach { cancelAlarmsFor(it.id) }

        val now = clock.nowMillis()
        schedules.filter { it.enabled }.forEach { schedule ->
            val alarms = planner.calculateAlarms(schedule, now) ?: return@forEach
            val durationMinutes = ((alarms.endAt - alarms.startAt) / 60_000L).toInt().coerceAtLeast(1)

            alarmScheduler.scheduleExact(
                alarmMillis = alarms.startAt,
                operationCode = startCode(schedule.id),
                receiverClassName = ALARM_RECEIVER,
                sessionId = schedule.id,
                action = ACTION_SCHEDULE_START,
                extras = mapOf(
                    KEY_PROFILE_ID to schedule.profileId,
                    KEY_DURATION_MINUTES to durationMinutes.toString(),
                ),
            )

            alarmScheduler.scheduleExact(
                alarmMillis = alarms.endAt,
                operationCode = endCode(schedule.id),
                receiverClassName = ALARM_RECEIVER,
                sessionId = schedule.id,
                action = ACTION_SCHEDULE_END,
            )

            val profile = profileRepo.getProfile(schedule.profileId) ?: return@forEach
            if (profile.lockMode is LockMode.Hard && schedule.allowSkipDay) {
                val warningAt = alarms.startAt - WARNING_BEFORE_START_MS
                if (warningAt > now) {
                    alarmScheduler.scheduleExact(
                        alarmMillis = warningAt,
                        operationCode = warningCode(schedule.id),
                        receiverClassName = ALARM_RECEIVER,
                        sessionId = schedule.id,
                        action = ACTION_SCHEDULE_WARNING,
                        extras = mapOf(
                            KEY_PROFILE_ID to schedule.profileId,
                            KEY_SCHEDULE_LABEL to (schedule.label ?: schedule.id),
                        ),
                    )
                }
            }
        }
    }

    fun cancelAlarmsFor(scheduleId: String) {
        alarmScheduler.cancelAlarm(startCode(scheduleId), ALARM_RECEIVER)
        alarmScheduler.cancelAlarm(endCode(scheduleId), ALARM_RECEIVER)
        alarmScheduler.cancelAlarm(warningCode(scheduleId), ALARM_RECEIVER)
    }

    companion object {
        const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"
        const val ACTION_SCHEDULE_START = "app.focus.service.focus.ACTION_SCHEDULE_START"
        const val ACTION_SCHEDULE_END = "app.focus.service.focus.ACTION_SCHEDULE_END"
        const val ACTION_SCHEDULE_WARNING = "app.focus.service.focus.ACTION_SCHEDULE_WARNING"
        const val KEY_PROFILE_ID = "profileId"
        const val KEY_DURATION_MINUTES = "durationMinutes"
        const val KEY_SCHEDULE_LABEL = "scheduleLabel"
        private const val WARNING_BEFORE_START_MS = 5 * 60 * 1000L

        fun startCode(scheduleId: String): Int = "sched_start_$scheduleId".hashCode()
        fun endCode(scheduleId: String): Int = "sched_end_$scheduleId".hashCode()
        fun warningCode(scheduleId: String): Int = "sched_warn_$scheduleId".hashCode()
    }
}

class StartScheduledSessionUseCase(
    private val scheduleRepo: ScheduleRepository,
    private val sessionRepo: SessionRepository,
    private val skipRepo: ScheduleSkipRepository,
    private val planner: ScheduleAlarmPlanner,
    private val startSessionUseCase: StartSessionUseCase,
    private val clock: Clock,
) {
    suspend fun execute(
        scheduleId: String,
        durationMinutesOverride: Int? = null,
        targetPackagesOverride: List<String>? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (sessionRepo.observeActiveSession().first() != null) return@withContext false
            if (skipRepo.isSkippedToday(scheduleId)) return@withContext false

            val schedule = scheduleRepo.getScheduleById(scheduleId) ?: return@withContext false
            if (!schedule.enabled) return@withContext false

            val durationMinutes = durationMinutesOverride ?: run {
                val remaining = planner.remainingMillisInActiveWindow(schedule, clock.nowMillis())
                    ?: return@withContext false
                (remaining / 60_000L).toInt().coerceAtLeast(1)
            }

            startSessionUseCase.execute(
                StartSessionRequest(
                    profileId = schedule.profileId,
                    durationMinutes = durationMinutes,
                    goalText = null,
                    source = SessionSource.SCHEDULE,
                    scheduleId = scheduleId,
                    targetPackagesOverride = targetPackagesOverride,
                ),
            )
            true
        }
}

class CheckMissedSchedulesUseCase(
    private val scheduleRepo: ScheduleRepository,
    private val sessionRepo: SessionRepository,
    private val skipRepo: ScheduleSkipRepository,
    private val planner: ScheduleAlarmPlanner,
    private val startScheduledSessionUseCase: StartScheduledSessionUseCase,
    private val profileRepo: ProfileRepository,
    private val clock: Clock,
) {
    suspend fun execute(): Boolean = withContext(Dispatchers.IO) {
        if (sessionRepo.observeActiveSession().first() != null) return@withContext false

        val now = clock.nowMillis()
        val activeSchedules = scheduleRepo.observeSchedules().first()
            .filter { it.enabled }
            .filter { planner.findActiveWindowEndAt(it, now) != null }
            .filter { !skipRepo.isSkippedToday(it.id) }

        if (activeSchedules.isEmpty()) return@withContext false

        val profiles = activeSchedules.mapNotNull { schedule ->
            profileRepo.getProfile(schedule.profileId)?.let { schedule to it }
        }
        if (profiles.isEmpty()) return@withContext false

        val strictest = profiles.maxBy { (_, profile) ->
            if (profile.lockMode is LockMode.Hard) 2 else 1
        }
        val mergedPackages = profiles.flatMap { (_, profile) -> profile.targetPackageNames }.distinct()
        val remaining = activeSchedules.minOf { schedule ->
            planner.remainingMillisInActiveWindow(schedule, now) ?: Long.MAX_VALUE
        }
        val durationMinutes = (remaining / 60_000L).toInt().coerceAtLeast(1)

        startScheduledSessionUseCase.execute(
            scheduleId = strictest.first.id,
            durationMinutesOverride = durationMinutes,
            targetPackagesOverride = mergedPackages,
        )
    }
}

class AdvancePomodoroPhaseUseCase(
    private val sessionRepo: SessionRepository,
    private val snapshotStore: ActiveSessionSnapshotStorage,
    private val alarmScheduler: AlarmSchedulerService,
    private val stopSessionUseCase: StopSessionUseCase,
    private val activeSessionBlockState: ActiveSessionBlockState,
    private val clock: Clock,
) {
    suspend fun execute(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val session = sessionRepo.observeActiveSession().first()
            ?: return@withContext false
        if (session.id != sessionId) return@withContext false
        val config = session.pomodoroConfig ?: return@withContext false
        val snapshot = snapshotStore.load() ?: return@withContext false

        val now = clock.nowMillis()
        val next = nextPhase(snapshot, config) ?: run {
            stopSessionUseCase.execute(sessionId, app.focus.domain.model.SessionStatus.Completed)
            return@withContext true
        }

        if (next == "COMPLETE") {
            stopSessionUseCase.execute(sessionId, app.focus.domain.model.SessionStatus.Completed)
            return@withContext true
        }

        val phaseDurationMs = phaseDurationMillis(next, config)
        val phaseEndAt = now + phaseDurationMs
        val cyclesDone = if (snapshot.currentPhase == "FOCUS") {
            snapshot.pomodoroFocusCyclesDone + 1
        } else {
            snapshot.pomodoroFocusCyclesDone
        }
        val updatedSnapshot = snapshot.copy(
            currentPhase = next,
            phaseEndAtMillis = phaseEndAt,
            pomodoroFocusCyclesDone = cyclesDone,
        )
        snapshotStore.save(updatedSnapshot)
        activeSessionBlockState.updateFromSnapshot(updatedSnapshot, isPaused = false)

        alarmScheduler.cancelAlarm(StartSessionUseCase.pomodoroPhaseCode(sessionId), ALARM_RECEIVER)
        alarmScheduler.cancelAlarm(pomodoroBreakWarningCode(sessionId), ALARM_RECEIVER)
        alarmScheduler.scheduleExact(
            alarmMillis = phaseEndAt,
            operationCode = StartSessionUseCase.pomodoroPhaseCode(sessionId),
            receiverClassName = ALARM_RECEIVER,
            sessionId = sessionId,
            action = StartSessionUseCase.ACTION_POMODORO_PHASE_END,
        )
        if (next == "SHORT_BREAK" || next == "LONG_BREAK") {
            val warningAt = phaseEndAt - BREAK_WARNING_BEFORE_MS
            if (warningAt > now) {
                alarmScheduler.scheduleExact(
                    alarmMillis = warningAt,
                    operationCode = pomodoroBreakWarningCode(sessionId),
                    receiverClassName = ALARM_RECEIVER,
                    sessionId = sessionId,
                    action = ACTION_POMODORO_BREAK_WARNING,
                )
            }
        }
        true
    }

    private fun nextPhase(
        snapshot: app.focus.domain.internal.statemachine.SessionSnapshot,
        config: app.focus.domain.model.PomodoroConfig,
    ): String? = when (snapshot.currentPhase) {
        "FOCUS" -> {
            val completed = snapshot.pomodoroFocusCyclesDone + 1
            if (completed >= config.totalCycles) "COMPLETE"
            else if (completed % config.cyclesBeforeLongBreak == 0) "LONG_BREAK"
            else "SHORT_BREAK"
        }
        "SHORT_BREAK", "LONG_BREAK", "BREAK" -> {
            if (snapshot.pomodoroFocusCyclesDone >= config.totalCycles) "COMPLETE" else "FOCUS"
        }
        else -> null
    }

    private fun phaseDurationMillis(phase: String, config: app.focus.domain.model.PomodoroConfig): Long = when (phase) {
        "FOCUS" -> config.focusMinutes * 60_000L
        "LONG_BREAK" -> config.longBreakMinutes * 60_000L
        else -> config.shortBreakMinutes * 60_000L
    }

    companion object {
        const val ALARM_RECEIVER = "app.focus.service.focus.AlarmReceiver"
        const val ACTION_POMODORO_BREAK_WARNING = "app.focus.service.focus.ACTION_POMODORO_BREAK_WARNING"
        private const val BREAK_WARNING_BEFORE_MS = 30_000L

        fun pomodoroBreakWarningCode(sessionId: String): Int = "pom_warn_$sessionId".hashCode()
    }
}
