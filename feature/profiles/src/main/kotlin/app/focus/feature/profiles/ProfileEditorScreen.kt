package app.focus.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onPickApps: (String) -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val loadedProfile by viewModel.profile.collectAsStateWithLifecycle()
    val editingLocked by viewModel.editingLocked.collectAsStateWithLifecycle()
    val profileId = loadedProfile?.id ?: remember { UUID.randomUUID().toString() }

    var name by remember(loadedProfile?.id) { mutableStateOf(loadedProfile?.name ?: "") }
    var lockMode by remember(loadedProfile?.id) {
        mutableStateOf(loadedProfile?.lockMode ?: LockMode.Soft)
    }
    var duration by remember(loadedProfile?.id) {
        mutableIntStateOf(loadedProfile?.defaultDurationMinutes ?: 25)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (loadedProfile != null) {
                            stringResource(R.string.profile_editor_edit_title)
                        } else {
                            stringResource(R.string.profile_editor_new_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_editor_back),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (editingLocked) {
                Text(
                    stringResource(R.string.profile_editor_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (!editingLocked) name = it },
                label = { Text(stringResource(R.string.profile_editor_name)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !editingLocked,
            )

            Text(stringResource(R.string.profile_editor_lock_mode), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = lockMode is LockMode.Soft,
                    onClick = { if (!editingLocked) lockMode = LockMode.Soft },
                    label = { Text(stringResource(R.string.profile_editor_soft_lock)) },
                    enabled = !editingLocked,
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = lockMode is LockMode.Hard,
                    onClick = { if (!editingLocked) lockMode = LockMode.Hard },
                    label = { Text(stringResource(R.string.profile_editor_hard_lock)) },
                    enabled = !editingLocked,
                )
            }

            Text(stringResource(R.string.profile_editor_duration), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = duration == mins,
                        onClick = { if (!editingLocked) duration = mins },
                        label = { Text(stringResource(R.string.profile_editor_duration_minutes, mins)) },
                        enabled = !editingLocked,
                    )
                }
            }

            OutlinedButton(
                onClick = { onPickApps(profileId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !editingLocked,
            ) {
                Text(
                    stringResource(
                        R.string.profile_editor_apps_selected,
                        loadedProfile?.targetPackageNames?.size ?: 0,
                    ),
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val profile = buildProfile(
                        profileId = profileId,
                        name = name,
                        lockMode = lockMode,
                        duration = duration,
                        loadedProfile = loadedProfile,
                    )
                    viewModel.save(profile)
                    onSave()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !editingLocked,
            ) {
                Text(stringResource(R.string.profile_editor_save))
            }
        }
    }
}

private fun buildProfile(
    profileId: String,
    name: String,
    lockMode: LockMode,
    duration: Int,
    loadedProfile: Profile?,
): Profile = Profile(
    id = profileId,
    name = name,
    emoji = loadedProfile?.emoji ?: "🎯",
    colorArgb = loadedProfile?.colorArgb ?: 0xFF2F6F6D.toInt(),
    lockMode = lockMode,
    defaultDurationMinutes = duration,
    bypassDelaySeconds = loadedProfile?.bypassDelaySeconds ?: 30,
    bypassBreathingEnabled = loadedProfile?.bypassBreathingEnabled ?: true,
    bypassReasonRequired = loadedProfile?.bypassReasonRequired ?: true,
    bypassPhrase = loadedProfile?.bypassPhrase,
    bypassLimitPerSession = loadedProfile?.bypassLimitPerSession ?: 3,
    accessWindowMinutes = loadedProfile?.accessWindowMinutes ?: 5,
    bypassAppliesToAllApps = loadedProfile?.bypassAppliesToAllApps ?: false,
    emergencyExitMode = loadedProfile?.emergencyExitMode ?: app.focus.domain.model.EmergencyExitMode.NONE,
    blockNewApps = loadedProfile?.blockNewApps ?: (lockMode is LockMode.Hard),
    deviceAdminProtection = loadedProfile?.deviceAdminProtection ?: (lockMode is LockMode.Hard),
    allowedSettingsShortcuts = loadedProfile?.allowedSettingsShortcuts ?: emptySet(),
    hideTargetNotifications = loadedProfile?.hideTargetNotifications ?: false,
    targetPackageNames = loadedProfile?.targetPackageNames ?: emptyList(),
)
