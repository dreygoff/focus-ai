package app.focus.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.data.ProfileSeeder
import app.focus.domain.usecase.AllowlistRepository
import app.focus.domain.usecase.ProfileRepository
import app.focus.feature.profiles.AppSortOrder
import app.focus.system.PackageInfo
import app.focus.system.PackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val packageRepository: PackageRepository,
    private val profileRepository: ProfileRepository,
    allowlistRepository: AllowlistRepository,
) : ViewModel() {

    data class AppRow(
        val packageName: String,
        val appName: String,
        val isSystemApp: Boolean,
        val isAllowlisted: Boolean,
        val isSelected: Boolean,
        val usageMinutesLast7Days: Long,
    )

    data class PickAppsUiState(
        val isLoading: Boolean = true,
        val searchQuery: String = "",
        val sortOrder: AppSortOrder = AppSortOrder.USAGE,
        val selectedPackages: Set<String> = emptySet(),
        val userApps: List<AppRow> = emptyList(),
        val systemApps: List<AppRow> = emptyList(),
        val systemSectionExpanded: Boolean = false,
    )

    data class FirstProfileUiState(
        val selectedCount: Int = 0,
        val isSaving: Boolean = false,
    )

    private data class FilterState(
        val searchQuery: String,
        val sortOrder: AppSortOrder,
        val selectedPackages: Set<String>,
        val systemSectionExpanded: Boolean,
    )

    private val searchQuery = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(AppSortOrder.USAGE)
    private val selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val systemExpanded = MutableStateFlow(false)
    private var recommendedPreselectApplied = false

    private val _pickAppsState = MutableStateFlow(PickAppsUiState())
    val pickAppsState: StateFlow<PickAppsUiState> = _pickAppsState.asStateFlow()

    private val _firstProfileState = MutableStateFlow(FirstProfileUiState())
    val firstProfileState: StateFlow<FirstProfileUiState> = _firstProfileState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                packageRepository.observeInstalledApps(),
                allowlistRepository.observeAllowlist(),
                combine(searchQuery, sortOrder, selectedPackages, systemExpanded, ::FilterState),
            ) { apps, allowlist, filter ->
                buildPickAppsState(apps, allowlist, filter)
            }.collect { state ->
                _pickAppsState.value = state
                _firstProfileState.value = _firstProfileState.value.copy(
                    selectedCount = state.selectedPackages.size,
                )
            }
        }
    }

    private fun applyRecommendedPreselect(apps: List<PackageInfo>) {
        if (recommendedPreselectApplied || apps.isEmpty()) return
        recommendedPreselectApplied = true
        val installed = apps.map { it.packageName }.toSet()
        selectedPackages.value = RECOMMENDED_DISTRACTION_PACKAGES.intersect(installed)
    }

    private fun buildPickAppsState(
        apps: List<PackageInfo>,
        allowlist: Set<String>,
        filter: FilterState,
    ): PickAppsUiState {
        applyRecommendedPreselect(apps)
        val selected = selectedPackages.value
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
                isSelected = selected.contains(app.packageName),
                usageMinutesLast7Days = app.usageMinutesLast7Days,
            )
        }
        return PickAppsUiState(
            isLoading = apps.isEmpty(),
            searchQuery = filter.searchQuery,
            sortOrder = filter.sortOrder,
            selectedPackages = selected,
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

    fun finalizeDeepWorkProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            _firstProfileState.value = _firstProfileState.value.copy(isSaving = true)
            profileRepository.updateTargetApps(
                ProfileSeeder.ID_DEEP_WORK,
                selectedPackages.value.toList(),
            )
            _firstProfileState.value = _firstProfileState.value.copy(isSaving = false)
            onComplete()
        }
    }

    companion object {
        val RECOMMENDED_DISTRACTION_PACKAGES = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.google.android.youtube",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.vk.android",
            "com.discord",
            "com.netflix.mediaclient",
            "com.spotify.music",
        )
    }
}
