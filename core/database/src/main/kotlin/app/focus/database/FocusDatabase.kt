package app.focus.database

import androidx.room.Database
import androidx.room.RoomDatabase
import app.focus.database.dao.AccessWindowDao
import app.focus.database.dao.AllowlistDao
import app.focus.database.dao.DailyStatsDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ProfileDao
import app.focus.database.dao.ScheduleDao
import app.focus.database.dao.SessionDao
import app.focus.database.entity.AccessWindowEntity
import app.focus.database.entity.AllowlistEntity
import app.focus.database.entity.DailyStatsEntity
import app.focus.database.entity.EventLogEntity
import app.focus.database.entity.ProfileAppEntity
import app.focus.database.entity.ProfileEntity
import app.focus.database.entity.ScheduleEntity
import app.focus.database.entity.SessionEntity

@Database(
    entities = [
        ProfileEntity::class,
        ProfileAppEntity::class,
        SessionEntity::class,
        ScheduleEntity::class,
        AccessWindowEntity::class,
        EventLogEntity::class,
        AllowlistEntity::class,
        DailyStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileAppDao(): ProfileAppDao
    abstract fun sessionDao(): SessionDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun accessWindowDao(): AccessWindowDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun allowlistDao(): AllowlistDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        const val DATABASE_NAME = "focus_db"

        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getDatabase(context: android.content.Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
