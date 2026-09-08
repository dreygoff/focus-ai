package app.focus.database.dao

import androidx.room.Room
import app.focus.database.FocusDatabase
import app.focus.database.entity.ProfileAppEntity
import app.focus.database.entity.ProfileEntity
import app.focus.database.entity.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProfileDaoTest {

    private lateinit var database: FocusDatabase
    private lateinit var profileDao: ProfileDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FocusDatabase::class.java,
        ).allowMainThreadQueries().build()
        profileDao = database.profileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveProfiles() = runTest {
        val profile = ProfileEntity(
            id = "p1",
            name = "Work",
            emoji = "🎯",
            colorArgb = 0xFF2F6F6D.toInt(),
            lockMode = "SOFT",
            defaultDurationMinutes = 25,
            bypassDelaySeconds = 30,
            bypassBreathingEnabled = true,
            bypassReasonRequired = true,
            bypassPhrase = null,
            bypassLimitPerSession = 3,
            accessWindowMinutes = 5,
            bypassAppliesToAllApps = false,
            emergencyExit = "NONE",
            blockNewApps = true,
            deviceAdminProtection = false,
            allowedShortcuts = null,
            hideTargetNotifications = false,
            targetPackageNames = "[]",
            createdAt = 1L,
            updatedAt = 1L,
            sortOrder = 0,
        )
        profileDao.insert(profile)

        val profiles = profileDao.observeProfiles().first()
        assertEquals(1, profiles.size)
        assertEquals("Work", profiles.first().name)
    }

    @Test
    fun getByIdReturnsNullWhenMissing() = runTest {
        assertNull(profileDao.getById("missing"))
    }

    @Test
    fun countReturnsCorrectValue() = runTest {
        assertEquals(0, profileDao.count())
        profileDao.insert(sampleProfile("p1"))
        assertEquals(1, profileDao.count())
    }

    private fun sampleProfile(id: String) = ProfileEntity(
        id = id,
        name = "Test",
        emoji = null,
        colorArgb = 0,
        lockMode = "SOFT",
        defaultDurationMinutes = 25,
        bypassDelaySeconds = 30,
        bypassBreathingEnabled = false,
        bypassReasonRequired = true,
        bypassPhrase = null,
        bypassLimitPerSession = 3,
        accessWindowMinutes = 5,
        bypassAppliesToAllApps = false,
        emergencyExit = "NONE",
        blockNewApps = false,
        deviceAdminProtection = false,
        allowedShortcuts = null,
        hideTargetNotifications = false,
        targetPackageNames = "[]",
        createdAt = 1L,
        updatedAt = 1L,
    )
}

@RunWith(RobolectricTestRunner::class)
class SessionDaoTest {

    private lateinit var database: FocusDatabase
    private lateinit var sessionDao: SessionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FocusDatabase::class.java,
        ).allowMainThreadQueries().build()
        sessionDao = database.sessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeActiveSessionReturnsRunningSession() = runTest {
        val session = SessionEntity(
            id = "s1",
            profileId = "p1",
            profileNameSnapshot = "Work",
            lockMode = "SOFT",
            targetPackagesSnapshot = "[]",
            goalText = null,
            startedAt = 1000L,
            plannedEndAt = 2000L,
            actualEndAt = null,
            status = "RUNNING",
            source = "MANUAL",
            pomodoroConfig = null,
            bypassesUsed = 0,
            blockAttempts = 0,
            pausesUsed = 0,
            scheduleId = null,
        )
        sessionDao.insert(session)

        val active = sessionDao.observeActiveSession().first()
        assertNotNull(active)
        assertEquals("s1", active?.id)
    }

    @Test
    fun observeActiveSessionIgnoresCompleted() = runTest {
        sessionDao.insert(
            SessionEntity(
                id = "s2",
                profileId = "p1",
                profileNameSnapshot = "Work",
                lockMode = "SOFT",
                targetPackagesSnapshot = "[]",
                goalText = null,
                startedAt = 1000L,
                plannedEndAt = 2000L,
                actualEndAt = 2000L,
                status = "COMPLETED",
                source = "MANUAL",
                pomodoroConfig = null,
                bypassesUsed = 0,
                blockAttempts = 0,
                pausesUsed = 0,
                scheduleId = null,
            ),
        )

        assertNull(sessionDao.observeActiveSession().first())
    }
}

@RunWith(RobolectricTestRunner::class)
class ProfileAppDaoTest {

    private lateinit var database: FocusDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var profileAppDao: ProfileAppDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            FocusDatabase::class.java,
        ).allowMainThreadQueries().build()
        profileDao = database.profileDao()
        profileAppDao = database.profileAppDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetPackageNames() = runTest {
        profileDao.insert(
            ProfileEntity(
                id = "p1",
                name = "Work",
                emoji = null,
                colorArgb = 0,
                lockMode = "SOFT",
                defaultDurationMinutes = 25,
                bypassDelaySeconds = 30,
                bypassBreathingEnabled = false,
                bypassReasonRequired = true,
                bypassPhrase = null,
                bypassLimitPerSession = 3,
                accessWindowMinutes = 5,
                bypassAppliesToAllApps = false,
                emergencyExit = "NONE",
                blockNewApps = false,
                deviceAdminProtection = false,
                allowedShortcuts = null,
                hideTargetNotifications = false,
                targetPackageNames = "[]",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        profileAppDao.insert(ProfileAppEntity(profileId = "p1", packageName = "com.example.app", addedAt = 1L))

        val packages = profileAppDao.getPackageNames("p1")
        assertEquals(listOf("com.example.app"), packages)
    }
}
