package app.focus.android.di

import android.content.Context
import androidx.room.Room
import app.focus.database.FocusDatabase
import app.focus.database.dao.AccessWindowDao
import app.focus.database.dao.AllowlistDao
import app.focus.database.dao.DailyStatsDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ProfileDao
import app.focus.database.dao.ScheduleDao
import app.focus.database.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database instance.
 * Creates the singleton FocusDatabase pointing to device-protected storage.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFocusDatabase(
        @ApplicationContext context: Context,
    ): FocusDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FocusDatabase::class.java,
            "focus_db",
        ).build()
    }

    @Provides
    fun provideProfileDao(database: FocusDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideProfileAppDao(database: FocusDatabase): ProfileAppDao = database.profileAppDao()

    @Provides
    fun provideSessionDao(database: FocusDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideScheduleDao(database: FocusDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideAccessWindowDao(database: FocusDatabase): AccessWindowDao = database.accessWindowDao()

    @Provides
    fun provideEventLogDao(database: FocusDatabase): EventLogDao = database.eventLogDao()

    @Provides
    fun provideAllowlistDao(database: FocusDatabase): AllowlistDao = database.allowlistDao()

    @Provides
    fun provideDailyStatsDao(database: FocusDatabase): DailyStatsDao = database.dailyStatsDao()
}
