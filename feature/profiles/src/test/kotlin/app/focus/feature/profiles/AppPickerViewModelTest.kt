package app.focus.feature.profiles

import androidx.lifecycle.SavedStateHandle
import app.focus.core.testing.FakeAllowlistRepository
import app.focus.core.testing.FakeProfileRepository
import app.focus.system.PackageInfo
import app.focus.system.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPickerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var allowlistRepository: FakeAllowlistRepository
    private val appsFlow = MutableStateFlow<List<PackageInfo>>(emptyList())
    private val packageRepository = object : PackageRepository {
        override fun observeInstalledApps() = appsFlow
        override suspend fun clearCache() = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        profileRepository = FakeProfileRepository()
        allowlistRepository = FakeAllowlistRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsUserAndSystemAppsSeparately() = runTest {
        appsFlow.value = listOf(
            PackageInfo("com.example.app", "Example", false),
            PackageInfo("com.android.systemui", "System UI", true),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.userApps.size)
        assertEquals(1, viewModel.uiState.value.systemApps.size)
    }

    @Test
    fun allowlistedAppCannotBeSelected() = runTest {
        appsFlow.value = listOf(PackageInfo("com.android.systemui", "System UI", true))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelection("com.android.systemui", isAllowlisted = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.selectedPackages.contains("com.android.systemui"))
    }

    @Test
    fun toggleSelectionUpdatesSelectedPackages() = runTest {
        appsFlow.value = listOf(PackageInfo("com.example.app", "Example", false))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelection("com.example.app", isAllowlisted = false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedPackages.contains("com.example.app"))
    }

    @Test
    fun saveSelectionUpdatesProfileTargets() = runTest {
        appsFlow.value = listOf(PackageInfo("com.example.app", "Example", false))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelection("com.example.app", isAllowlisted = false)
        viewModel.saveSelection {}
        advanceUntilIdle()

        val profile = profileRepository.getProfile("seed-work")
        assertTrue(profile?.targetPackageNames?.contains("com.example.app") == true)
    }

    private fun createViewModel(): AppPickerViewModel {
        val handle = SavedStateHandle(mapOf("profileId" to "seed-work"))
        return AppPickerViewModel(
            savedStateHandle = handle,
            packageRepository = packageRepository,
            profileRepository = profileRepository,
            allowlistRepository = allowlistRepository,
        )
    }
}
