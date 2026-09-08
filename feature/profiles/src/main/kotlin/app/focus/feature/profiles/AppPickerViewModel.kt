package app.focus.feature.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.domain.usecase.AllowlistRepository
import app.focus.domain.usecase.ProfileRepository
import app.focus.system.PackageInfo
import app.focus.system.PackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppSortOrder { ALPHABETICAL, USAGE }

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val packageRepository: PackageRepository,
    private val profileRepository: ProfileRepository,
    allowlistRepository: AllowlistRepository,
) : ViewModel() {

    private val profileId: String = checkNotNull(savedStateHandle.get<String>("profileId"))

    data class AppPickerUiState(
        val isLoading: Boolean = true,
        val searchQuery: String = "",
        val sortOrder: AppSortOrder = AppSortOrder.ALPHABETICAL,
        val selectedPackages: Set<String> = emptySet(),
        val userApps: List<AppRow> = emptyList(),
        val systemApps: List<AppRow> = emptyList(),
        val systemSectionExpanded: Boolean = false,
    )

    data class AppRow(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean,
        val isAllowlisted: Boolean,
        val isSelected: Boolean,
        val usageMinutesLast7Days: Long,
    )

    private data class FilterState(
        val searchQuery: String,
        val sortOrder: AppSortOrder,
        val selectedPackages: Set<String>,
        val systemSectionExpanded: Boolean,
    )

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(AppSortOrder.ALPHABETICAL)
    private val selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val systemExpanded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile(profileId)
            selectedPackages.value = profile?.targetPackageNames?.toSet().orEmpty()
        }

        viewModelScope.launch {
            combine(
                packageRepository.observeInstalledApps(),
                allowlistRepository.observeAllowlist(),
                combine(searchQuery, sortOrder, selectedPackages, systemExpanded, ::FilterState),
            ) { apps, allowlist, filter ->
                buildUiState(apps, allowlist, filter)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        apps: List<PackageInfo>,
        allowlist: Set<String>,
        filter: FilterState,
    ): AppPickerUiState {
        val filtered = apps.filter { app ->
            filter.searchQuery.isBlank() ||
                app.appName.contains(filter.searchQuery, ignoreCase = true) ||
                app.packageName.contains(filter.searchQuery, ignoreCase = true)
        }
        val sorted = when (filter.sortOrder) {
            AppSortOrder.ALPHABETICAL -> filtered.sortedBy { it.appName.lowercase() }
            AppSortOrder.USAGE -> filtered.sortedByDescending { it.usageMinutesLast7Days }
        }
        val rows = sorted.map { app ->
            AppRow(
                packageName = app.packageName,
                appName = app.appName,
                isSystemApp = app.isSystemApp,
                isAllowlisted = allowlist.contains(app.packageName),
                isSelected = filter.selectedPackages.contains(app.packageName),
                usageMinutesLast7Days = app.usageMinutesLast7Days,
            )
        }
        return AppPickerUiState(
            isLoading = apps.isEmpty(),
            searchQuery = filter.searchQuery,
            sortOrder = filter.sortOrder,
            selectedPackages = filter.selectedPackages,
            userApps = rows.filter { !it.isSystemApp },
            systemApps = rows.filter { it.isSystemApp },
            systemSectionExpanded = filter.systemSectionExpanded,
        )
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOrder(order: AppSortOrder) {
        sortOrder.value = order
    }

    fun toggleSystemSection() {
        systemExpanded.value = !systemExpanded.value
    }

    fun toggleSelection(packageName: String, isAllowlisted: Boolean) {
        if (isAllowlisted) return
        selectedPackages.value = selectedPackages.value.toMutableSet().apply {
            if (contains(packageName)) remove(packageName) else add(packageName)
        }
    }

    fun saveSelection(onSaved: () -> Unit) {
        viewModelScope.launch {
            profileRepository.updateTargetApps(profileId, selectedPackages.value.toList())
            onSaved()
        }
    }
}
