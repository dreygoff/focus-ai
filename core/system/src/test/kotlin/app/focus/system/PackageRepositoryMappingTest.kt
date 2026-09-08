package app.focus.system

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageRepositoryMappingTest {

    @Test
    fun packageInfoDistinctByPackageName() {
        val apps = listOf(
            PackageInfo("a", "App A", false),
            PackageInfo("a", "App A duplicate", false),
            PackageInfo("b", "App B", true),
        ).distinctBy { it.packageName }

        assertEquals(2, apps.size)
    }
}
