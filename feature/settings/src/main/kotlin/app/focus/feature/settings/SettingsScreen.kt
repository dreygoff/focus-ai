package app.focus.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.domain.model.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionName: String,
    onBack: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenAllowlist: () -> Unit = {},
    onOpenProtectionInfo: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onLanguageChanged: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val csv = pendingExport
        if (uri != null && csv != null) {
            writeCsv(context, uri, csv)
        }
        pendingExport = null
        viewModel.clearExportRequest()
    }

    LaunchedEffect(uiState.exportCsv) {
        val csv = uiState.exportCsv ?: return@LaunchedEffect
        pendingExport = csv
        exportLauncher.launch("focus_stats_${System.currentTimeMillis()}.csv")
        viewModel.clearExportRequest()
    }

    uiState.messageKey?.let { key ->
        val text = when (key) {
            "stats_cleared" -> stringResource(R.string.settings_stats_cleared)
            "data_deleted" -> stringResource(R.string.settings_data_deleted)
            else -> key
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text(stringResource(R.string.settings_ok))
                }
            },
            text = { Text(text) },
        )
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            confirmButton = {
                TextButton(onClick = viewModel::dismissMessage) {
                    Text(stringResource(R.string.settings_ok))
                }
            },
            text = { Text(message) },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_stats),
            body = stringResource(R.string.settings_clear_stats_confirm),
            onConfirm = {
                showClearConfirm = false
                viewModel.clearStatistics()
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all),
            body = stringResource(R.string.settings_delete_all_confirm),
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAllData()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.hardLockActive) {
                Text(
                    text = stringResource(R.string.settings_hard_lock_restricted),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
            }

            SettingsSectionTitle(stringResource(R.string.settings_section_permissions))
            SettingsNavRow(
                label = stringResource(R.string.settings_permissions),
                onClick = onOpenPermissions,
                enabled = true,
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
            ThemeOption(stringResource(R.string.settings_theme_system), Theme.SYSTEM, uiState.theme, viewModel::setTheme)
            ThemeOption(stringResource(R.string.settings_theme_light), Theme.LIGHT, uiState.theme, viewModel::setTheme)
            ThemeOption(stringResource(R.string.settings_theme_dark), Theme.DARK, uiState.theme, viewModel::setTheme)
            SettingsSwitchRow(
                label = stringResource(R.string.settings_dynamic_color),
                checked = uiState.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
            LanguageSelector(
                currentTag = uiState.languageTag,
                onSelect = { tag ->
                    viewModel.setLanguageTag(tag)
                    onLanguageChanged(tag)
                },
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_blocking))
            SettingsSwitchRow(
                label = stringResource(R.string.settings_block_vibration),
                checked = uiState.blockVibration,
                onCheckedChange = viewModel::setBlockVibration,
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_block_sound),
                checked = uiState.blockSound,
                onCheckedChange = viewModel::setBlockSound,
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_quotes),
                checked = uiState.quotesEnabled,
                onCheckedChange = viewModel::setQuotesEnabled,
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_allowlist))
            SettingsNavRow(
                label = stringResource(R.string.settings_allowlist),
                onClick = onOpenAllowlist,
                enabled = !uiState.hardLockActive,
                disabledHint = stringResource(R.string.settings_hard_lock_restricted),
            )

            SettingsSectionTitle(stringResource(R.string.settings_section_data))
            OutlinedButton(
                onClick = viewModel::requestExport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Storage, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_export_csv))
            }
            TextButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.hardLockActive,
            ) {
                Text(
                    text = stringResource(R.string.settings_clear_stats),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.hardLockActive,
            ) {
                Text(
                    text = stringResource(R.string.settings_delete_all),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            SettingsSectionTitle(stringResource(R.string.settings_section_about))
            SettingsNavRow(
                label = stringResource(R.string.settings_protection_link),
                onClick = onOpenProtectionInfo,
            )
            SettingsNavRow(
                label = stringResource(R.string.settings_privacy_title),
                onClick = onOpenPrivacy,
            )
            SettingsNavRow(
                label = stringResource(R.string.settings_licenses_title),
                onClick = onOpenLicenses,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_version))
                Text(versionName, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun ThemeOption(
    label: String,
    value: Theme,
    selected: Theme,
    onSelect: (Theme) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LanguageSelector(
    currentTag: String,
    onSelect: (String) -> Unit,
) {
    val languages = listOf("ru" to R.string.settings_language_ru, "en" to R.string.settings_language_en)
    Column {
        Text(stringResource(R.string.settings_language))
        languages.forEach { (tag, labelRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(tag) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = currentTag == tag, onClick = { onSelect(tag) })
                Text(stringResource(labelRes), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    disabledHint: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
            if (!enabled && disabledHint != null) {
                Text(disabledHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.settings_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

private fun writeCsv(context: Context, uri: Uri, csv: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(csv.toByteArray(Charsets.UTF_8))
    }
}
