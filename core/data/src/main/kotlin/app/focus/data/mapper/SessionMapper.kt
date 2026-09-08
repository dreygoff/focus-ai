package app.focus.data.mapper

import app.focus.database.entity.ProfileAppEntity as EntityProfileApp
import app.focus.domain.model.PomodoroConfig
import java.util.UUID

/* ============== SESSION MAPPER ============== */

fun app.focus.database.entity.SessionEntity.toDomain(): app.focus.domain.model.Session {
    return app.focus.domain.model.Session(
        id = id,
        profileId = profileId,
        profileNameSnapshot = profileNameSnapshot,
        lockMode = if (lockMode == "HARD") app.focus.domain.model.LockMode.Hard else app.focus.domain.model.LockMode.Soft,
        targetPackagesSnapshot = targetPackagesSnapshot.fromJsonList(),
        goalText = goalText,
        startedAt = startedAt,
        plannedEndAt = plannedEndAt,
        actualEndAt = actualEndAt,
        status = SessionStatusMapper.from(status),
        source = SessionSourceMapper.from(source),
        pomodoroConfig = parsePomodoroConfig(pomodoroConfig),
        bypassesUsed = bypassesUsed,
        blockAttempts = blockAttempts,
        pausesUsed = pausesUsed,
        scheduleId = scheduleId
    )
}

fun app.focus.domain.model.Session.toEntity(): app.focus.database.entity.SessionEntity {
    return app.focus.database.entity.SessionEntity(
        id = id,
        profileId = profileId,
        profileNameSnapshot = profileNameSnapshot,
        lockMode = if (lockMode is app.focus.domain.model.LockMode.Hard) "HARD" else "SOFT",
        targetPackagesSnapshot = targetPackagesSnapshot.toJsonList(),
        goalText = goalText,
        startedAt = startedAt,
        plannedEndAt = plannedEndAt,
        actualEndAt = actualEndAt,
        status = SessionStatusMapper.to(status),
        source = SessionSourceMapper.to(source),
        pomodoroConfig = pomodoroConfig?.toJsonPomodoro(),
        bypassesUsed = bypassesUsed,
        blockAttempts = blockAttempts,
        pausesUsed = pausesUsed,
        scheduleId = scheduleId
    )
}

/* ============== PROFILE MAPPER ============== */

fun app.focus.database.entity.ProfileEntity.toDomain(): app.focus.domain.model.Profile {
    return app.focus.domain.model.Profile(
        id = id,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        lockMode = if (lockMode == "HARD") app.focus.domain.model.LockMode.Hard else app.focus.domain.model.LockMode.Soft,
        defaultDurationMinutes = defaultDurationMinutes,
        bypassDelaySeconds = bypassDelaySeconds,
        bypassBreathingEnabled = bypassBreathingEnabled,
        bypassReasonRequired = bypassReasonRequired,
        bypassPhrase = bypassPhrase,
        bypassLimitPerSession = bypassLimitPerSession,
        accessWindowMinutes = accessWindowMinutes,
        bypassAppliesToAllApps = bypassAppliesToAllApps,
        emergencyExitMode = EmergencyExitMapper.from(emergencyExit),
        blockNewApps = blockNewApps,
        deviceAdminProtection = deviceAdminProtection,
        allowedSettingsShortcuts = (allowedShortcuts ?: "").fromJsonStringSet()
            .mapNotNull { runCatching { app.focus.domain.model.SettingsShortcut.valueOf(it) }.getOrNull() }
            .toSet(),
        hideTargetNotifications = hideTargetNotifications,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
        targetPackageNames = targetPackageNames.fromJsonList(),
    )
}

fun app.focus.domain.model.Profile.toEntity(isNew: Boolean = true): app.focus.database.entity.ProfileEntity {
    return app.focus.database.entity.ProfileEntity(
        id = id,
        name = name,
        emoji = emoji,
        colorArgb = colorArgb,
        lockMode = if (lockMode is app.focus.domain.model.LockMode.Hard) "HARD" else "SOFT",
        defaultDurationMinutes = defaultDurationMinutes,
        bypassDelaySeconds = bypassDelaySeconds,
        bypassBreathingEnabled = bypassBreathingEnabled,
        bypassReasonRequired = bypassReasonRequired,
        bypassPhrase = bypassPhrase,
        bypassLimitPerSession = bypassLimitPerSession,
        accessWindowMinutes = accessWindowMinutes,
        bypassAppliesToAllApps = bypassAppliesToAllApps,
        emergencyExit = EmergencyExitMapper.to(emergencyExitMode),
        blockNewApps = blockNewApps,
        deviceAdminProtection = deviceAdminProtection,
        allowedShortcuts = allowedSettingsShortcuts.map { it.name }.toSet().toJsonStringList(),
        hideTargetNotifications = hideTargetNotifications,
        targetPackageNames = targetPackageNames.toJsonList(),
        createdAt = if (isNew) System.currentTimeMillis() else createdAt,
        updatedAt = System.currentTimeMillis(),
        sortOrder = sortOrder,
    )
}

/* ============== SCHEDULE MAPPER ============== */

fun app.focus.database.entity.ScheduleEntity.toDomain(): app.focus.domain.model.Schedule {
    return app.focus.domain.model.Schedule(
        id = id,
        profileId = profileId,
        enabled = enabled,
        daysOfWeekMask = daysOfWeekMask,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        allowSkipDay = allowSkipDay,
        label = label,
        createdAt = createdAt
    )
}

fun app.focus.domain.model.Schedule.toEntity(): app.focus.database.entity.ScheduleEntity {
    return app.focus.database.entity.ScheduleEntity(
        id = id,
        profileId = profileId,
        enabled = enabled,
        daysOfWeekMask = daysOfWeekMask,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        allowSkipDay = allowSkipDay,
        label = label,
        createdAt = createdAt
    )
}

/* ============== EVENT LOG MAPPER ============== */

fun app.focus.database.entity.EventLogEntity.toDomain(): app.focus.domain.model.EventLog {
    return app.focus.domain.model.EventLog(
        id = id,
        timestamp = timestamp,
        sessionId = sessionId,
        type = EventTypeMapper.from(type),
        packageName = packageName,
        payload = payload
    )
}

fun app.focus.domain.model.EventLog.toEntity(): app.focus.database.entity.EventLogEntity {
    return app.focus.database.entity.EventLogEntity(
        id = 0,
        timestamp = timestamp,
        sessionId = sessionId,
        type = EventTypeMapper.to(type),
        packageName = packageName,
        payload = payload
    )
}

/* ============== ACCESS WINDOW MAPPER ============== */

fun app.focus.database.entity.AccessWindowEntity.toDomain(): app.focus.domain.model.AccessWindow {
    return app.focus.domain.model.AccessWindow(
        id = id,
        sessionId = sessionId,
        packageName = packageName,
        grantedAt = grantedAt,
        expiresAt = expiresAt,
        reason = reason,
        restrictedToActivity = restrictedToActivity
    )
}

/* ============== POMODORO MAPPER ============== */

private fun parsePomodoroConfig(json: String?): PomodoroConfig? {
    if (json.isNullOrEmpty()) return null
    try {
        val s = json.trim()
        if (s == "{}") return null
        val focusMinutes = extractInt(s, "focusMinutes", 25)
        val shortBreakMinutes = extractInt(s, "shortBreakMinutes", 5)
        val longBreakMinutes = extractInt(s, "longBreakMinutes", 15)
        val cyclesBeforeLongBreak = extractInt(s, "cyclesBeforeLongBreak", 4)
        val totalCycles = extractInt(s, "totalCycles", 4)
        return PomodoroConfig(focusMinutes, shortBreakMinutes, longBreakMinutes, cyclesBeforeLongBreak, totalCycles)
    } catch (_: Exception) {
        return null
    }
}

private fun PomodoroConfig.toJsonPomodoro(): String {
    return """{"focusMinutes":$focusMinutes,"shortBreakMinutes":$shortBreakMinutes,"longBreakMinutes":$longBreakMinutes,"cyclesBeforeLongBreak":$cyclesBeforeLongBreak,"totalCycles":$totalCycles}"""
}

private fun extractInt(json: String, key: String, default: Int): Int {
    val pattern = "\"$key\":([-]?\\d+)"
    val match = pattern.toRegex().find(json)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: default
}

/* ============== UTILITY OBJECTS ============== */

object SessionStatusMapper {
    fun from(value: String): app.focus.domain.model.SessionStatus = when (value) {
        "RUNNING" -> app.focus.domain.model.SessionStatus.Running
        "PAUSED" -> app.focus.domain.model.SessionStatus.Paused()
        "COMPLETED" -> app.focus.domain.model.SessionStatus.Completed
        "CANCELLED" -> app.focus.domain.model.SessionStatus.Cancelled
        "EXPIRED" -> app.focus.domain.model.SessionStatus.Expired
        "EMERGENCY_EXIT_PENDING" -> app.focus.domain.model.SessionStatus.EmergencyExitPending(0L)
        else -> app.focus.domain.model.SessionStatus.Scheduled
    }

    fun to(status: app.focus.domain.model.SessionStatus): String = when (status) {
        is app.focus.domain.model.SessionStatus.Running -> "RUNNING"
        is app.focus.domain.model.SessionStatus.Paused -> "PAUSED"
        is app.focus.domain.model.SessionStatus.Completed -> "COMPLETED"
        is app.focus.domain.model.SessionStatus.Cancelled -> "CANCELLED"
        is app.focus.domain.model.SessionStatus.Expired -> "EXPIRED"
        is app.focus.domain.model.SessionStatus.EmergencyExitPending -> "EMERGENCY_EXIT_PENDING"
        else -> "SCHEDULED"
    }
}

object SessionSourceMapper {
    fun from(value: String): app.focus.domain.model.SessionSource = when (value) {
        "MANUAL" -> app.focus.domain.model.SessionSource.MANUAL
        "SCHEDULE" -> app.focus.domain.model.SessionSource.SCHEDULE
        "WIDGET" -> app.focus.domain.model.SessionSource.WIDGET
        "TILE" -> app.focus.domain.model.SessionSource.TILE
        "POMODORO" -> app.focus.domain.model.SessionSource.POMODORO
        "SHORTCUT" -> app.focus.domain.model.SessionSource.SHORTCUT
        else -> app.focus.domain.model.SessionSource.MANUAL
    }

    fun to(source: app.focus.domain.model.SessionSource): String = when (source) {
        app.focus.domain.model.SessionSource.MANUAL -> "MANUAL"
        app.focus.domain.model.SessionSource.SCHEDULE -> "SCHEDULE"
        app.focus.domain.model.SessionSource.WIDGET -> "WIDGET"
        app.focus.domain.model.SessionSource.TILE -> "TILE"
        app.focus.domain.model.SessionSource.POMODORO -> "POMODORO"
        app.focus.domain.model.SessionSource.SHORTCUT -> "SHORTCUT"
    }
}

object EmergencyExitMapper {
    fun from(value: String): app.focus.domain.model.EmergencyExitMode = when (value) {
        "NONE" -> app.focus.domain.model.EmergencyExitMode.NONE
        "DELAY_10_MIN" -> app.focus.domain.model.EmergencyExitMode.DELAY_10_MIN
        "RETYPE_TEXT" -> app.focus.domain.model.EmergencyExitMode.RETYPE_TEXT
        else -> app.focus.domain.model.EmergencyExitMode.NONE
    }

    fun to(mode: app.focus.domain.model.EmergencyExitMode): String = when (mode) {
        app.focus.domain.model.EmergencyExitMode.NONE -> "NONE"
        app.focus.domain.model.EmergencyExitMode.DELAY_10_MIN -> "DELAY_10_MIN"
        app.focus.domain.model.EmergencyExitMode.RETYPE_TEXT -> "RETYPE_TEXT"
    }
}

object EventTypeMapper {
    fun to(type: app.focus.domain.model.EventType): String = when (type) {
        app.focus.domain.model.EventType.SESSION_STARTED -> "SESSION_STARTED"
        app.focus.domain.model.EventType.SESSION_PAUSED -> "SESSION_PAUSED"
        app.focus.domain.model.EventType.SESSION_RESUMED -> "SESSION_RESUMED"
        app.focus.domain.model.EventType.SESSION_COMPLETED -> "SESSION_COMPLETED"
        app.focus.domain.model.EventType.SESSION_CANCELLED -> "SESSION_CANCELLED"
        app.focus.domain.model.EventType.SESSION_EXPIRED -> "SESSION_EXPIRED"
        app.focus.domain.model.EventType.BLOCK_SHOWN -> "BLOCK_SHOWN"
        app.focus.domain.model.EventType.BYPASS_GRANTED -> "BYPASS_GRANTED"
        else -> type.name
    }

    fun from(value: String): app.focus.domain.model.EventType = try {
        app.focus.domain.model.EventType.valueOf(value)
    } catch (_: IllegalArgumentException) {
        app.focus.domain.model.EventType.SESSION_STARTED
    }
}

fun app.focus.database.entity.DailyStatsEntity.toDomain(): app.focus.domain.model.DailyStats =
    app.focus.domain.model.DailyStats(
        dateEpochDay = dateEpochDay,
        focusMinutes = focusMinutes,
        sessionsCompleted = sessionsCompleted,
        sessionsTotal = sessionsTotal,
        blockAttempts = blockAttempts,
        bypasses = bypasses,
    )

/* ============== STRING EXTENSIONS ============== */

fun List<String>.toJsonList(): String {
    return joinToString("|")
}

/** Simple JSON-like list parsing (format: a|b|c or ["a","b","c"]) */
fun String.fromJsonList(): List<String> {
    return if (isBlank()) emptyList() else split("|").map { it.trim('"', '[', ']', ',') }.filter { it.isNotBlank() }
}

fun Set<String>.toJsonStringList(): String {
    return joinToString("|")
}

fun String.fromJsonStringSet(): Set<String> {
    return if (isBlank()) emptySet() else split("|").mapNotNull { s -> s.takeIf { it.isNotBlank() } }.toSet()
}
