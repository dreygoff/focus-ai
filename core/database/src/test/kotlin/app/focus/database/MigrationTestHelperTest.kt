package app.focus.database

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Skeleton for Room schema migration tests starting at version 1.
 * When adding MIGRATION_1_2, use androidx.room.testing.MigrationTestHelper in androidTest.
 */
class MigrationTestHelperTest {

    @Test
    fun currentSchemaVersionIsOne() {
        assertEquals(1, SCHEMA_VERSION)
    }

    companion object {
        /** Keep in sync with [FocusDatabase] version. */
        const val SCHEMA_VERSION = 1
    }
}
