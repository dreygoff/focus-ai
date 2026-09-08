package app.focus.data

import app.focus.database.dao.AccessWindowDao
import app.focus.database.dao.AllowlistDao
import app.focus.database.dao.DailyStatsDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ScheduleDao
import app.focus.database.dao.SessionDao
import app.focus.data.mapper.toDomain
import app.focus.data.mapper.toEntity
import app.focus.domain.model.AccessWindow
import app.focus.domain.model.DailyStats
import app.focus.domain.model.EventLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Qualifier

@Qualifier
annotation class SystemAllowlistQualifier

class RealSessionRepository(
    private val sessionDao: SessionDao,
    private val profileAppDao: ProfileAppDao,
    private val dailyStatsDao: DailyStatsDao,
) : app.focus.domain.usecase.SessionRepository {

    override suspend fun insert(session: app.focus.domain.model.Session): Long = sessionDao.insert(session.toEntity())
    override suspend fun update(session: app.focus.domain.model.Session) = sessionDao.update(session.toEntity())
    override fun observeActiveSession(): Flow<app.focus.domain.model.Session?> = sessionDao.observeActiveSession().map { it?.toDomain() }
    override fun observeSessions(startDate: Long, endDate: Long): Flow<List<app.focus.domain.model.Session>> =
        sessionDao.observeSessionsInRange(startDate, endDate).map { sessions -> sessions.map { it.toDomain() } }

    override suspend fun getStatsDaily(startDate: Long, days: Int): List<DailyStats> {
        val startEpochDay = startDate / MILLIS_PER_DAY
        val endEpochDay = startEpochDay + days
        return dailyStatsDao.getStatsInRange(startEpochDay, endEpochDay).map { it.toDomain() }
    }

    override suspend fun cancelOldEvents(beforeMillis: Long) {}

    override suspend fun exportStatsCsv(startDate: Long, endDate: Long): String {
        val sessions = sessionDao.observeSessionsInRange(startDate, endDate).first()
        val header = "id,profileId,profileName,startAt,endAt,durationMin,status,blockAttempts,bypasses\n"
        val rows = sessions.joinToString("\n") { session ->
            val durationMin = session.plannedEndAt?.let { end ->
                ((end - session.startedAt) / MILLIS_PER_MINUTE).coerceAtLeast(0)
            } ?: 0
            listOf(
                session.id,
                session.profileId,
                session.profileNameSnapshot,
                session.startedAt,
                session.actualEndAt ?: session.plannedEndAt ?: "",
                durationMin,
                session.status,
                session.blockAttempts,
                session.bypassesUsed,
            ).joinToString(",")
        }
        return header + rows
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

class RealScheduleRepository(
    private val scheduleDao: ScheduleDao
) : app.focus.domain.usecase.ScheduleRepository {
    override suspend fun insert(schedule: app.focus.domain.model.Schedule): Long = scheduleDao.insert(schedule.toEntity())
    override suspend fun update(schedule: app.focus.domain.model.Schedule) = scheduleDao.update(schedule.toEntity())
    override suspend fun delete(id: String) { scheduleDao.getById(id)?.let { scheduleDao.delete(it) } }
    override fun observeSchedules(): Flow<List<app.focus.domain.model.Schedule>> =
        scheduleDao.observeSchedules().map { schedules -> schedules.mapNotNull { it.toDomain() } }
    override suspend fun getScheduleById(id: String): app.focus.domain.model.Schedule? = scheduleDao.getById(id)?.toDomain()
}

class RealAllowlistRepository(
    private val allowlistDao: AllowlistDao,
    @SystemAllowlistQualifier private val systemPkgSet: Set<String>
) : app.focus.domain.usecase.AllowlistRepository {
    override suspend fun addPackage(packageName: String) { if (!systemPkgSet.contains(packageName)) allowlistDao.add(packageName) }
    override suspend fun removePackage(packageName: String) = allowlistDao.remove(packageName)
    override fun observeAllowlist(): Flow<Set<String>> = allowlistDao.observeAllowlist().map { systemPkgSet + it.toSet() }
    override fun isAllowed(packageName: String): Boolean = systemPkgSet.contains(packageName) || allowlistDao.isAllowed(packageName) != 0
}

class RealAccessWindowRepository(
    private val accessWindowDao: AccessWindowDao
) : app.focus.domain.usecase.AccessWindowRepository {
    override suspend fun grant(
        sessionId: String,
        packageName: String,
        reason: String?,
        durationMinutes: Int,
    ): Long {
        val now = System.currentTimeMillis()
        val expiresAt = now + durationMinutes * 60_000L
        val entity = app.focus.database.entity.AccessWindowEntity(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = sessionId,
            packageName = packageName,
            grantedAt = now,
            expiresAt = expiresAt,
            reason = reason,
            restrictedToActivity = null,
        )
        accessWindowDao.insert(entity)
        return expiresAt
    }
    override suspend fun revoke(packageName: String) = accessWindowDao.removeWindow(packageName)
    override fun observeActiveWindows(sessionId: String): Flow<Map<String, AccessWindow>> {
        val now = System.currentTimeMillis()
        return accessWindowDao.observeActiveWindows(sessionId).map { windows ->
            windows.mapNotNull { entity -> entity.toDomain().takeIf { window -> window.expiresAt > now } }
                .associateBy { it.packageName }
        }
    }
    override suspend fun removeExpired(beforeMillis: Long) = accessWindowDao.deleteExpired(beforeMillis)
}

class RealEventLogRepository(
    private val eventLogDao: EventLogDao,
    private val clock: app.focus.domain.usecase.Clock
) : app.focus.domain.usecase.EventLogRepository {
    override suspend fun log(event: app.focus.domain.model.EventLog): Long = eventLogDao.log(event.toEntity())
    override fun observeEventsForSession(sessionId: String): Flow<List<app.focus.domain.model.EventLog>> =
        eventLogDao.observeEventsForSession(sessionId).map { events -> events.mapNotNull { it.toDomain() } }
    override suspend fun deleteOlderThan(beforeMillis: Long) = eventLogDao.deleteOlderThan(beforeMillis)
}
