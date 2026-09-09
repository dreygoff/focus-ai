package app.focus.feature.onboarding

import app.focus.domain.usecase.AllowlistRepository
import app.focus.domain.usecase.ProfileRepository
import app.focus.system.PackageInfo
import app.focus.system.PackageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun preselectsInstalledRecommendedApps() = runTest {
        val packageRepository = mockk<PackageRepository>()
        val profileRepository = mockk<ProfileRepository>(relaxed = true)
        val allowlistRepository = mockk<AllowlistRepository>()
        every { allowlistRepository.observeAllowlist() } returns flowOf(emptySet())
        every { packageRepository.observeInstalledApps() } returns MutableStateFlow(
            listOf(
                PackageInfo(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    isSystemApp = false,
                    usageMinutesLast7Days = 120,
                ),
                PackageInfo(
                    packageName = "com.example.notes",
                    appName = "Notes",
                    isSystemApp = false,
                    usageMinutesLast7Days = 5,
                ),
            ),
        )

        val viewModel = OnboardingViewModel(
            packageRepository = packageRepository,
            profileRepository = profileRepository,
            allowlistRepository = allowlistRepository,
        )

        advanceUntilIdle()

        assertTrue(viewModel.pickAppsState.value.selectedPackages.contains("com.instagram.android"))
        assertEquals(1, viewModel.pickAppsState.value.selectedPackages.size)
    }

    @Test
    fun finalizeDeepWorkProfileUpdatesSeededProfile() = runTest {
        val packageRepository = mockk<PackageRepository>()
        val profileRepository = mockk<ProfileRepository>()
        val allowlistRepository = mockk<AllowlistRepository>()
        every { allowlistRepository.observeAllowlist() } returns flowOf(emptySet())
        every { packageRepository.observeInstalledApps() } returns flowOf(emptyList())
        coEvery { profileRepository.updateTargetApps(any(), any()) } returns Unit

        val viewModel = OnboardingViewModel(
            packageRepository = packageRepository,
            profileRepository = profileRepository,
            allowlistRepository = allowlistRepository,
        )
        viewModel.toggleSelection("com.instagram.android", isAllowlisted = false)
        advanceUntilIdle()
        viewModel.finalizeDeepWorkProfile {}
        advanceUntilIdle()

        coVerify {
            profileRepository.updateTargetApps(
                app.focus.data.ProfileSeeder.ID_DEEP_WORK,
                listOf("com.instagram.android"),
            )
        }
    }
}
