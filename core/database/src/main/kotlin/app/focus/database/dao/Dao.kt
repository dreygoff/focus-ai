package app.focus.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.focus.database.entity.AccessWindowEntity
import app.focus.database.entity.DailyStatsEntity
import app.focus.database.entity.EventLogEntity
import app.focus.database.entity.ProfileAppEntity
import app.focus.database.entity.ProfileEntity
import app.focus.database.entity.ScheduleEntity
import app.focus.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC, createdAt ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Query("UPDATE profiles SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}

@Dao
interface ProfileAppDao {
    @Query("SELECT * FROM profile_apps WHERE profileId = :profileId ORDER BY addedAt DESC")
    fun observeAppsForProfile(profileId: String): Flow<List<ProfileAppEntity>>

    @Query("SELECT packageName FROM profile_apps WHERE profileId = :profileId")
    suspend fun getPackageNames(profileId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: ProfileAppEntity): Long

    @Query("DELETE FROM profile_apps WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun removeProfileApp(profileId: String, packageName: String)

    @Query("DELETE FROM profile_apps WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE status IN ('RUNNING', 'PAUSED') LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE profileId = :profileId ORDER BY startedAt DESC")
    fun observeSessionsForProfile(profileId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status IN ('RUNNING', 'PAUSED') LIMIT 100")
    suspend fun getActiveSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("UPDATE sessions SET status = :status, actualEndAt = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM sessions WHERE startedAt >= :startDate AND startedAt <= :endDate ORDER BY startedAt DESC")
    fun observeSessionsInRange(startDate: Long, endDate: Long): Flow<List<SessionEntity>>

    @Transaction
    suspend fun completeSession(id: String, status: String) {
        updateStatus(id, status)
    }
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startMinuteOfDay ASC")
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)
}

@Dao
interface AccessWindowDao {
    @Query("SELECT * FROM access_windows WHERE sessionId = :sessionId AND expiresAt > CURRENT_TIMESTAMP")
    fun observeActiveWindows(sessionId: String): Flow<List<AccessWindowEntity>>

    @Query("SELECT * FROM access_windows WHERE packageName = :packageName")
    suspend fun getWindowForPackage(packageName: String): AccessWindowEntity?

    @Query("SELECT * FROM access_windows WHERE expiresAt <= :beforeMillis")
    suspend fun getExpiredWindows(beforeMillis: Long): List<AccessWindowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: AccessWindowEntity): Long

    @Query("DELETE FROM access_windows WHERE packageName = :packageName")
    suspend fun removeWindow(packageName: String)

    @Query("DELETE FROM access_windows WHERE expiresAt <= :beforeMillis")
    suspend fun deleteExpired(beforeMillis: Long)
}

@Dao
interface EventLogDao {
    @Insert
    suspend fun log(event: EventLogEntity): Long

    @Query("SELECT * FROM event_log WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 500")
    fun observeEventsForSession(sessionId: String): Flow<List<EventLogEntity>>

    @Query("DELETE FROM event_log WHERE type = 'BYPASS_DENIED' AND packageName IS NULL AND timestamp < :beforeMillis")
    suspend fun deleteDeniedOlderThan(beforeMillis: Long)

    @Query(
        "DELETE FROM event_log WHERE sessionId IS NOT NULL " +
            "AND sessionId NOT IN (SELECT id FROM sessions) AND timestamp < :beforeMillis",
    )
    suspend fun cleanupOrphaned(beforeMillis: Long): Int

    @Query("SELECT COUNT(*) FROM event_log WHERE type IN ('BLOCK_SHOWN','BYPASS_GRANTED') AND sessionId = :sessionId AND timestamp >= :since")
    suspend fun countBlockAttempts(sessionId: String, since: Long): Int

    @Query("DELETE FROM event_log WHERE timestamp < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}

@Dao
interface AllowlistDao {
    @Query("SELECT packageName FROM allowlist")
    fun observeAllowlist(): Flow<List<String>>

    @Query("INSERT OR IGNORE INTO allowlist (packageName) VALUES (:packageName)")
    suspend fun add(packageName: String): Long

    @Query("DELETE FROM allowlist WHERE packageName = :packageName")
    suspend fun remove(packageName: String)

    @Query("SELECT COUNT(*) FROM allowlist WHERE packageName = :packageName")
    fun isAllowed(packageName: String): Int
}

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE dateEpochDay >= :startDate AND dateEpochDay <= :endDate ORDER BY dateEpochDay ASC")
    fun getStatsInRange(startDate: Long, endDate: Long): List<DailyStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStatsEntity): Long

    @Query("DELETE FROM daily_stats WHERE dateEpochDay < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: Long): Int
}
