package app.focus.feature.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.feature.profiles.AppSortOrder
import app.focus.feature.profiles.icon.AppPackageIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickAppsScreen(
    viewModel: OnboardingViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.pickAppsState.collectAsStateWithLifecycle()
    val loadingDescription = stringResource(R.string.onboarding_cd_loading_apps)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_pick_apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            OnboardingStepIndicator(currentStep = 2, modifier = Modifier.padding(top = 8.dp))

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text(stringResource(R.string.onboarding_pick_apps_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.sortOrder == AppSortOrder.ALPHABETICAL,
                    onClick = { viewModel.setSortOrder(AppSortOrder.ALPHABETICAL) },
                    label = { Text(stringResource(R.string.onboarding_pick_apps_sort_alpha)) },
                )
                FilterChip(
                    selected = state.sortOrder == AppSortOrder.USAGE,
                    onClick = { viewModel.setSortOrder(AppSortOrder.USAGE) },
                    label = { Text(stringResource(R.string.onboarding_pick_apps_sort_usage)) },
                )
            }

            Text(
                stringResource(R.string.onboarding_pick_apps_selected_count, state.selectedPackages.size),
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.userApps.isEmpty() && state.systemApps.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.onboarding_pick_apps_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(state.userApps, key = { it.packageName }) { app ->
                        OnboardingAppRow(
                            app = app,
                            onToggle = { viewModel.toggleSelection(app.packageName, app.isAllowlisted) },
                        )
                    }
                    if (state.systemApps.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = viewModel::toggleSystemSection)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.onboarding_pick_apps_system_section),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    if (state.systemSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = stringResource(
                                        if (state.systemSectionExpanded) {
                                            R.string.onboarding_pick_apps_cd_collapse_system
                                        } else {
                                            R.string.onboarding_pick_apps_cd_expand_system
                                        },
                                    ),
                                )
                            }
                        }
                        if (state.systemSectionExpanded) {
                            items(state.systemApps, key = { "sys-${it.packageName}" }) { app ->
                                OnboardingAppRow(
                                    app = app,
                                    onToggle = { viewModel.toggleSelection(app.packageName, app.isAllowlisted) },
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.material3.Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = !state.isLoading,
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Composable
private fun OnboardingAppRow(
    app: OnboardingViewModel.AppRow,
    onToggle: () -> Unit,
) {
    val rowDescription = stringResource(
        if (app.isSelected) {
            R.string.onboarding_pick_apps_cd_selected
        } else {
            R.string.onboarding_pick_apps_cd_not_selected
        },
        app.appName,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = app.isSelected,
                onValueChange = { if (!app.isAllowlisted) onToggle() },
                enabled = !app.isAllowlisted,
                role = Role.Checkbox,
            )
            .semantics { contentDescription = rowDescription }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = app.isSelected,
            onCheckedChange = null,
            enabled = !app.isAllowlisted,
        )
        AppPackageIcon(
            packageName = app.packageName,
            contentDescription = app.appName,
            modifier = Modifier.size(40.dp).padding(end = 12.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.isAllowlisted) {
                Text(
                    stringResource(R.string.onboarding_pick_apps_allowlisted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
