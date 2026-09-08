package app.focus.feature.profiles

import app.focus.core.testing.FakeProfileRepository
import app.focus.core.testing.FakeSessionRepository
import app.focus.domain.model.LockMode
import app.focus.domain.usecase.CreateProfileUseCase
import app.focus.domain.usecase.ObserveActiveSessionsUseCase
import app.focus.domain.usecase.RealClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        profileRepository = FakeProfileRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsSeedProfiles() = runTest {
        val viewModel = ProfilesViewModel(
            profileRepository = profileRepository,
            createProfileUseCase = CreateProfileUseCase(profileRepository, RealClock()),
            observeActiveSessionsUseCase = ObserveActiveSessionsUseCase(FakeSessionRepository()),
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.profiles.size)
    }

    @Test
    fun createProfileAddsEntry() = runTest {
        val viewModel = ProfilesViewModel(
            profileRepository = profileRepository,
            createProfileUseCase = CreateProfileUseCase(profileRepository, RealClock()),
            observeActiveSessionsUseCase = ObserveActiveSessionsUseCase(FakeSessionRepository()),
        )
        advanceUntilIdle()
        viewModel.createProfile("Study", LockMode.Soft)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.profiles.size)
    }
}
