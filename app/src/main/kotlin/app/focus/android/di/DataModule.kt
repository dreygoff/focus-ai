package app.focus.android.di

import app.focus.data.RealAccessWindowRepository
import app.focus.data.RealAllowlistRepository
import app.focus.data.RealEventLogRepository
import app.focus.data.RealProfileRepository
import app.focus.data.RealScheduleRepository
import app.focus.data.RealSessionRepository
import app.focus.database.dao.AccessWindowDao
import app.focus.database.dao.AllowlistDao
import app.focus.database.dao.EventLogDao
import app.focus.database.dao.ProfileAppDao
import app.focus.database.dao.ScheduleDao
import app.focus.database.dao.SessionDao
import app.focus.data.SystemAllowlistQualifier
import app.focus.domain.usecase.Clock
import app.focus.domain.usecase.RealClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun provideSessionRepository(
        sessionDao: SessionDao,
        profileAppDao: ProfileAppDao
    ): app.focus.domain.usecase.SessionRepository = RealSessionRepository(sessionDao, profileAppDao)

    @Provides
    fun provideProfileRepository(
        dao: app.focus.database.dao.ProfileDao,
        profileApp: ProfileAppDao
    ): app.focus.domain.usecase.ProfileRepository = RealProfileRepository(dao, profileApp)

    @Provides
    fun provideScheduleRepository(
        scheduleDao: ScheduleDao
    ): app.focus.domain.usecase.ScheduleRepository = RealScheduleRepository(scheduleDao)

    @Provides
    @Singleton
    fun provideClock(): Clock = RealClock()

    @Provides
    @SystemAllowlistQualifier
    fun provideSystemAllowlist(): Set<String> = setOf(
        "app.focus.android",
        "com.android.systemui",
        "com.android.server.telecom",
        "com.android.phone",
        "com.google.android.apps.messaging"
    )

    @Provides
    fun provideAllowlistRepository(
        dao: AllowlistDao,
        @SystemAllowlistQualifier systemPkgSet: Set<String>
    ): app.focus.domain.usecase.AllowlistRepository = RealAllowlistRepository(dao, systemPkgSet)

    @Provides
    fun provideAccessWindowRepository(
        dao: AccessWindowDao
    ): app.focus.domain.usecase.AccessWindowRepository = RealAccessWindowRepository(dao)

    @Provides
    fun provideEventLogRepository(
        dao: EventLogDao,
        clock: Clock
    ): app.focus.domain.usecase.EventLogRepository = RealEventLogRepository(dao, clock)
}
