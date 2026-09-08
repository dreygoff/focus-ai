package app.focus.feature.permissions

import app.focus.datastore.UserSettingsRepository
import app.focus.system.PermissionChecker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var userSettingsRepository: UserSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        permissionChecker = mockk(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)
        every { permissionChecker.permissions } returns MutableStateFlow(emptySet())
        coEvery { userSettingsRepository.isAccessibilityDisclosureAccepted() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshPermissions_mandatoryMissingWhenOverlayDenied() = runTest {
        every { permissionChecker.refreshPermissions() } returns emptySet()
        every { permissionChecker.areMandatoryGranted() } returns false

        val viewModel = PermissionsViewModel(permissionChecker, userSettingsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PermissionsUiState.Ready
        assertFalse(state.mandatoryGranted)
    }

    @Test
    fun refreshPermissions_mandatoryGranted() = runTest {
        every { permissionChecker.refreshPermissions() } returns emptySet()
        every { permissionChecker.areMandatoryGranted() } returns true
        every { permissionChecker.isAccessibilityGranted() } returns true

        val viewModel = PermissionsViewModel(permissionChecker, userSettingsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PermissionsUiState.Ready
        assertTrue(state.mandatoryGranted)
    }
}
