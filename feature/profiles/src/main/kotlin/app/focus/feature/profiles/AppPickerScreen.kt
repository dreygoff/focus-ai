package app.focus.feature.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppPickerRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppPickerScreen(
        state = state,
        onSearchChange = viewModel::setSearchQuery,
        onSortChange = viewModel::setSortOrder,
        onToggleSystemSection = viewModel::toggleSystemSection,
        onToggleApp = viewModel::toggleSelection,
        onDone = { viewModel.saveSelection(onDone) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    state: AppPickerViewModel.AppPickerUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (AppSortOrder) -> Unit,
    onToggleSystemSection: () -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onDone) {
                        Text(stringResource(R.string.app_picker_done))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.app_picker_search)) },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.sortOrder == AppSortOrder.ALPHABETICAL,
                    onClick = { onSortChange(AppSortOrder.ALPHABETICAL) },
                    label = { Text(stringResource(R.string.app_picker_sort_alpha)) },
                )
                FilterChip(
                    selected = state.sortOrder == AppSortOrder.USAGE,
                    onClick = { onSortChange(AppSortOrder.USAGE) },
                    label = { Text(stringResource(R.string.app_picker_sort_usage)) },
                )
            }
            Text(
                stringResource(R.string.app_picker_selected_count, state.selectedPackages.size),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.userApps, key = { it.packageName }) { app ->
                    AppPickerRow(app = app, onToggle = { onToggleApp(app.packageName, app.isAllowlisted) })
                }
                if (state.systemApps.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onToggleSystemSection)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.app_picker_system_section),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (state.systemSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                            )
                        }
                    }
                    if (state.systemSectionExpanded) {
                        items(state.systemApps, key = { "sys-${it.packageName}" }) { app ->
                            AppPickerRow(app = app, onToggle = { onToggleApp(app.packageName, app.isAllowlisted) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppPickerViewModel.AppRow,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !app.isAllowlisted, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = app.isSelected,
            onCheckedChange = { if (!app.isAllowlisted) onToggle() },
            enabled = !app.isAllowlisted,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.isAllowlisted) {
                Text(
                    stringResource(R.string.app_picker_allowlisted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ProfilesRoute(
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit,
    onEditProfile: (String?) -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfilesListScreen(
        profiles = state.profiles,
        editingLocked = state.editingLocked,
        editingLockedReason = state.editingLockedReason,
        onNavigateToStartSession = onNavigateToStartSession,
        onAddProfile = { if (!state.editingLocked) onEditProfile(null) },
        onEditProfile = { id -> if (!state.editingLocked) onEditProfile(id) },
        onDeleteProfile = viewModel::deleteProfile,
    )
}
