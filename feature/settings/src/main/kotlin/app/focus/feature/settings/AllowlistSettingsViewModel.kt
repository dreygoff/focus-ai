package app.focus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.focus.data.SystemAllowlistQualifier
import app.focus.domain.usecase.AllowlistRepository
import app.focus.system.PackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllowlistSettingsViewModel @Inject constructor(
    private val allowlistRepository: AllowlistRepository,
    private val packageRepository: PackageRepository,
    @SystemAllowlistQualifier private val systemPackages: Set<String>,
) : ViewModel() {

    data class AppRow(
        val packageName: String,
        val label: String,
        val isAllowlisted: Boolean,
        val isSystem: Boolean,
    )

    data class UiState(
        val apps: List<AppRow> = emptyList(),
        val filter: String = "",
    )

    private val filter = MutableStateFlow("")

    val uiState: StateFlow<UiState> = combine(
        allowlistRepository.observeAllowlist(),
        packageRepository.observeInstalledApps(),
        filter,
    ) { allowlist, installed, query ->
        val normalized = query.trim().lowercase()
        val rows = installed
            .sortedBy { it.appName.lowercase() }
            .map { app ->
                AppRow(
                    packageName = app.packageName,
                    label = app.appName,
                    isAllowlisted = allowlist.contains(app.packageName),
                    isSystem = app.packageName in systemPackages,
                )
            }
            .filter { row ->
                normalized.isEmpty() ||
                    row.label.lowercase().contains(normalized) ||
                    row.packageName.lowercase().contains(normalized)
            }
        UiState(apps = rows, filter = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), UiState())

    fun setFilter(value: String) {
        filter.value = value
    }

    fun toggle(packageName: String, currentlyAllowlisted: Boolean, isSystem: Boolean) {
        if (isSystem) return
        viewModelScope.launch {
            if (currentlyAllowlisted) {
                allowlistRepository.removePackage(packageName)
            } else {
                allowlistRepository.addPackage(packageName)
            }
        }
    }

    companion object {
        private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
