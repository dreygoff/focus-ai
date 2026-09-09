package app.focus.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.focus.domain.model.SettingsShortcut

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileHardLockOptionsSection(
    deviceAdminProtection: Boolean,
    blockNewApps: Boolean,
    allowedShortcuts: Set<SettingsShortcut>,
    editingLocked: Boolean,
    onDeviceAdminProtectionChange: (Boolean) -> Unit,
    onBlockNewAppsChange: (Boolean) -> Unit,
    onToggleShortcut: (SettingsShortcut) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.profile_editor_hard_options), style = MaterialTheme.typography.titleMedium)

        HardLockSwitchRow(
            label = stringResource(R.string.profile_editor_device_admin),
            description = stringResource(R.string.profile_editor_device_admin_hint),
            checked = deviceAdminProtection,
            enabled = !editingLocked,
            onCheckedChange = onDeviceAdminProtectionChange,
        )

        HardLockSwitchRow(
            label = stringResource(R.string.profile_editor_block_new_apps),
            description = stringResource(R.string.profile_editor_block_new_apps_hint),
            checked = blockNewApps,
            enabled = !editingLocked,
            onCheckedChange = onBlockNewAppsChange,
        )

        Text(stringResource(R.string.profile_editor_settings_shortcuts), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsShortcut.entries.forEach { shortcut ->
                FilterChip(
                    selected = shortcut in allowedShortcuts,
                    onClick = { if (!editingLocked) onToggleShortcut(shortcut) },
                    label = { Text(settingsShortcutLabel(shortcut)) },
                    enabled = !editingLocked,
                )
            }
        }
    }
}

@Composable
private fun HardLockSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val stateLabel = stringResource(
        if (checked) R.string.profile_editor_switch_on else R.string.profile_editor_switch_off,
    )
    val switchDescription = stringResource(R.string.profile_editor_cd_switch, label, stateLabel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .semantics { contentDescription = switchDescription },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun settingsShortcutLabel(shortcut: SettingsShortcut): String = when (shortcut) {
    SettingsShortcut.WIFI -> stringResource(R.string.profile_shortcut_wifi)
    SettingsShortcut.BLUETOOTH -> stringResource(R.string.profile_shortcut_bluetooth)
    SettingsShortcut.SOUND -> stringResource(R.string.profile_shortcut_sound)
    SettingsShortcut.NOTIFICATIONS -> stringResource(R.string.profile_shortcut_notifications)
    SettingsShortcut.CELLULAR -> stringResource(R.string.profile_shortcut_cellular)
}
