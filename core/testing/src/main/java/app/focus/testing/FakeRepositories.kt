package app.focus.core.testing

import app.focus.domain.internal.statemachine.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher

class TestClock(private val currentTimeMillis: () -> Long) : Clock {
    override fun nowMillis(): Long = currentTimeMillis.invoke()
    override fun nowInstant(): java.time.Instant = java.time.Instant.now()
}

class FakeSessionRepository : app.focus.domain.usecase.SessionRepository {
    private val sessions = mutableMapOf<String, app.focus.domain.model.Session>()
    private var currentFlow: kotlinx.coroutines.flow.MutableSharedFlow<app.focus.domain.model.Session?> = kotlinx.coroutines.flow.MutableSharedFlow()

    override suspend fun insert(session: app.focus.domain.model.Session): Long {
        sessions[session.id] = session
        return session.id.hashCode().toLong()
    }

    override suspend fun update(session: app.focus.domain.model.Session) {
        sessions[session.id] = session
    }

    override fun observeActiveSession(): kotlinx.coroutines.flow.Flow<app.focus.domain.model.Session?> {
        return currentFlow.asSharedFlow()
    }

    override fun observeSessions(startDate: Long, endDate: Long): kotlinx.coroutines.flow.Flow<List<app.focus.domain.model.Session>> {
        return kotlinx.coroutines.flow.flowOf(sessions.values.filter { it.startedAt in startDate..endDate }.toList())
    }

    override suspend fun getStatsDaily(startDate: Long, days: Int): List<app.focus.domain.model.DailyStats> = emptyList()
    override suspend fun cancelOldEvents(beforeMillis: Long) {}
    override suspend fun exportStatsCsv(startDate: Long, endDate: Long): String = "date,sessions,focus_minutes"
}

class FakeProfileRepository : app.focus.domain.usecase.ProfileRepository {
    private val profiles = mutableMapOf<String, app.focus.domain.model.Profile>()
    
    init {
        profiles["seed-work"] = app.focus.domain.model.Profile(
            id = "seed-work", name = "Work", emoji = "\uD83D\uDE80", colorArgb = 0xFF2F6F6D.toInt(),
            lockMode = app.focus.domain.model.LockMode.Soft, defaultDurationMinutes = 50,
            bypassDelaySeconds = 30, bypassBreathingEnabled = true, bypassReasonRequired = true,
            bypassPhrase = null, bypassLimitPerSession = 3, accessWindowMinutes = 5,
            bypassAppliesToAllApps = false, emergencyExitMode = app.focus.domain.model.EmergencyExitMode.NONE,
            blockNewApps = true, deviceAdminProtection = false, allowedSettingsShortcuts = emptySet(),
            hideTargetNotifications = false
        )
        profiles["seed-sleep"] = app.focus.domain.model.Profile(
            id = "seed-sleep", name = "Sleep", emoji = "\uD83D\uDE34", colorArgb = 0xFF2563EB.toInt(),
            lockMode = app.focus.domain.model.LockMode.Hard, defaultDurationMinutes = 480,
            bypassDelaySeconds = 0, bypassBreathingEnabled = false, bypassReasonRequired = false,
            bypassPhrase = null, bypassLimitPerSession = -1, accessWindowMinutes = 5,
            bypassAppliesToAllApps = false, emergencyExitMode = app.focus.domain.model.EmergencyExitMode.NONE,
            blockNewApps = true, deviceAdminProtection = false, allowedSettingsShortcuts = emptySet(),
            hideTargetNotifications = false
        )
        profiles["seed-detox"] = app.focus.domain.model.Profile(
            id = "seed-detox", name = "Detox", emoji = "\uD83D\uDD25", colorArgb = 0xFFDC2626.toInt(),
            lockMode = app.focus.domain.model.LockMode.Hard, defaultDurationMinutes = 120,
            bypassDelaySeconds = 0, bypassBreathingEnabled = false, bypassReasonRequired = false,
            bypassPhrase = null, bypassLimitPerSession = -1, accessWindowMinutes = 5,
            bypassAppliesToAllApps = false, emergencyExitMode = app.focus.domain.model.EmergencyExitMode.DELAY_10_MIN,
            blockNewApps = true, deviceAdminProtection = true, allowedSettingsShortcuts = emptySet(),
            hideTargetNotifications = false
        )
    }
    
    override suspend fun insert(profile: app.focus.domain.model.Profile): String { profiles[profile.id] = profile; return profile.id }
    override suspend fun update(profile: app.focus.domain.model.Profile) { profiles[profile.id] = profile }
    override suspend fun delete(id: String) { profiles.remove(id) }
    override fun observeProfiles(): kotlinx.coroutines.flow.Flow<List<app.focus.domain.model.Profile>> = kotlinx.coroutines.flow.flowOf(profiles.values.toList())
    override suspend fun get(id: String): app.focus.domain.model.Profile? = profiles[id]
    override suspend fun getDefaultProfileId(): String? = "seed-work"
    override suspend fun setDefaultProfileId(id: String) {}
}

class FakeScheduleRepository : app.focus.domain.usecase.ScheduleRepository {
    private val schedules = mutableListOf<app.focus.domain.model.Schedule>()
    override suspend fun insert(schedule: app.focus.domain.model.Schedule) { schedules.add(schedule) }
    override suspend fun update(schedule: app.focus.domain.model.Schedule) {}
    override suspend fun delete(id: String) { schedules.removeAll { it.id == id } }
    override fun observeSchedules(): kotlinx.coroutines.flow.Flow<List<app.focus.domain.model.Schedule>> = kotlinx.coroutines.flow.flowOf(schedules)
    override suspend fun get(id: String): app.focus.domain.model.Schedule? = schedules.find { it.id == id }
}

class FakeAllowlistRepository : app.focus.domain.usecase.AllowlistRepository {
    private val allowlist = mutableSetOf("com.android.systemui", "app.focus.android")
    init { }
    override suspend fun add(packageName: String) { allowlist.add(packageName) }
    override suspend fun remove(packageName: String) { allowlist.remove(packageName) }
    override fun observeAllowlist(): kotlinx.coroutines.flow.Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(allowlist)
    override fun isAllowed(packageName: String): Boolean = allowlist.contains(packageName)
}

class FakeAccessWindowRepository : app.focus.domain.usecase.AccessWindowRepository {
    private val windows = mutableMapOf<String, app.focus.domain.model.AccessWindow>()
    override suspend fun grant(sessionId: String, packageName: String, reason: String?): Long {
        val window = app.focus.domain.model.AccessWindow(
            sessionId = sessionId, packageName = packageName, grantedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L, reason = reason, restrictedToActivity = null
        )
        windows[packageName] = window
        return window.expiresAt
    }
    override suspend fun revoke(packageName: String) { windows.remove(packageName) }
    override fun observeActiveWindows(sessionId: String): kotlinx.coroutines.flow.Flow<Map<String, app.focus.domain.model.AccessWindow>> = 
        kotlinx.coroutines.flow.flowOf(windows.filter { it.value.sessionId == sessionId }.mapKeys { it.key })
    override suspend fun removeExpired(beforeMillis: Long) { windows.keys.removeAll { windows[it]?.expiresAt?.let { e -> e < beforeMillis } ?: false } }
}

class FakeEventLogRepository : app.focus.domain.usecase.EventLogRepository {
    private val events = mutableListOf<app.focus.domain.model.EventLog>()
    override suspend fun log(event: app.focus.domain.model.EventLog) { events.add(event) }
    override fun observeEventsForSession(sessionId: String): kotlinx.coroutines.flow.Flow<List<app.focus.domain.model.EventLog>> = 
        kotlinx.coroutines.flow.flowOf(events.filter { it.sessionId == sessionId })
}
