package app.focus.data

import androidx.room.Room
import app.focus.database.FocusDatabase
import app.focus.database.dao.ProfileDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProfileSeederTest {

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
    fun seedIfEmpty_insertsThreeTemplates() = runTest {
        ProfileSeeder(profileDao).seedIfEmpty()

        assertEquals(3, profileDao.count())
        val names = profileDao.getById(ProfileSeeder.ID_DEEP_WORK)?.name
        assertEquals("Глубокая работа", names)
        assertEquals("HARD", profileDao.getById(ProfileSeeder.ID_SLEEP)?.lockMode)
        assertEquals(120, profileDao.getById(ProfileSeeder.ID_DETOX)?.defaultDurationMinutes)
    }

    @Test
    fun seedIfEmpty_doesNotDuplicate() = runTest {
        ProfileSeeder(profileDao).seedIfEmpty()
        ProfileSeeder(profileDao).seedIfEmpty()
        assertEquals(3, profileDao.count())
    }
}
