package app.focus.android.di

import android.content.Context
import androidx.room.Room
import app.focus.database.FocusDatabase
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
            "focus_db"
        )
            .addMigrations(FocusDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: FocusDatabase): app.focus.database.ProfileDao {
        return database.profileDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: FocusDatabase): app.focus.database.SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideScheduleDao(database: FocusDatabase): app.focus.database.ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    @Singleton
    fun provideAccessWindowDao(database: FocusDatabase): app.focus.database.AccessWindowDao {
        return database.accessWindowDao()
    }

    @Provides
    @Singleton
    fun provideEventLogDao(database: FocusDatabase): app.focus.database.EventLogDao {
        return database.eventLogDao()
    }

    @Provides
    @Singleton
    fun provideAllowlistDao(database: FocusDatabase): app.focus.database.AllowlistDao {
        return database.allowlistDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: FocusDatabase): app.focus.database.DailyStatsDao {
        return database.dailyStatsDao()
    }
}
