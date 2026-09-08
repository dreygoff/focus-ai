package app.focus.domain.usecase

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun insert(session: app.focus.domain.model.Session): Long
    suspend fun update(session: app.focus.domain.model.Session)
    fun observeActiveSession(): Flow<app.focus.domain.model.Session?>
    fun observeSessions(startDate: Long, endDate: Long): Flow<List<app.focus.domain.model.Session>>
    suspend fun getStatsDaily(startDate: Long, days: Int): List<app.focus.domain.model.DailyStats>
    suspend fun cancelOldEvents(beforeMillis: Long)
    suspend fun exportStatsCsv(startDate: Long, endDate: Long): String
}

interface ProfileRepository {
    suspend fun insert(profile: app.focus.domain.model.Profile): Long
    suspend fun update(profile: app.focus.domain.model.Profile)
    suspend fun delete(id: String)
    fun observeProfiles(): Flow<List<app.focus.domain.model.Profile>>
    suspend fun getProfile(id: String): app.focus.domain.model.Profile?
    suspend fun getDefaultProfileId(): String?
    suspend fun setDefaultProfileId(id: String)
    suspend fun updateTargetApps(profileId: String, packageNames: List<String>)
}

interface ScheduleRepository {
    suspend fun insert(schedule: app.focus.domain.model.Schedule): Long
    suspend fun update(schedule: app.focus.domain.model.Schedule)
    suspend fun delete(id: String)
    fun observeSchedules(): Flow<List<app.focus.domain.model.Schedule>>
    suspend fun getScheduleById(id: String): app.focus.domain.model.Schedule?
}

interface AllowlistRepository {
    suspend fun addPackage(packageName: String)
    suspend fun removePackage(packageName: String)
    fun observeAllowlist(): Flow<Set<String>>
    fun isAllowed(packageName: String): Boolean
}

interface AccessWindowRepository {
    suspend fun grant(
        sessionId: String,
        packageName: String,
        reason: String?,
        durationMinutes: Int,
    ): Long
    suspend fun revoke(packageName: String)
    fun observeActiveWindows(sessionId: String): Flow<Map<String, app.focus.domain.model.AccessWindow>>
    suspend fun removeExpired(beforeMillis: Long)
}

interface EventLogRepository {
    suspend fun log(event: app.focus.domain.model.EventLog): Long
    fun observeEventsForSession(sessionId: String): Flow<List<app.focus.domain.model.EventLog>>
    suspend fun deleteOlderThan(beforeMillis: Long)
}

interface PackageInfoRepository {
    fun observeInstalledApps(): Flow<List<app.focus.domain.model.AppInfo>>
    suspend fun clearCache()
}

interface AlarmSchedulerService {
    fun scheduleExact(
        alarmMillis: Long,
        operationCode: Int,
        receiverClassName: String,
        sessionId: String = "",
        action: String? = null,
        extras: Map<String, String> = emptyMap(),
    )
    fun cancelAlarm(operationCode: Int, receiverClassName: String)
}

/** Starts/stops the focus foreground service when session lifecycle changes. */
interface SessionRuntimeController {
    fun onSessionStarted(sessionId: String, profileName: String, plannedEndAtMillis: Long)
    fun syncStopService()
}

data class BlockRequest(
    val sessionId: String,
    val profileId: String,
    val profileName: String,
    val blockedPackage: String,
    val blockedAppName: String,
    val lockMode: app.focus.domain.model.LockMode,
    val remainingMillis: Long,
    val attemptNumber: Int,
    val bypassesUsed: Int = 0,
    val forceOverlay: Boolean = false,
    val tamperMessage: String? = null,
)

interface BlockLauncher {
    fun show(request: BlockRequest)
    fun dismiss()
    fun isShowing(): Boolean
}

interface ActiveSessionSnapshotStorage {
    suspend fun save(snapshot: app.focus.domain.internal.statemachine.SessionSnapshot)
    suspend fun load(): app.focus.domain.internal.statemachine.SessionSnapshot?
    suspend fun clear()
}

interface Clock {
    fun nowMillis(): Long
    fun nowInstant(): java.time.Instant
}

class RealClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun nowInstant(): java.time.Instant = java.time.Instant.now()
}
