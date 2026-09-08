package app.focus.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.focus.domain.model.LockMode
import app.focus.domain.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesListScreen(
    profiles: List<Profile> = emptyList(),
    editingLocked: Boolean = false,
    editingLockedReason: String? = null,
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit = { _, _ -> },
    onAddProfile: () -> Unit = {},
    onEditProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onToggleLockMode: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                actions = {
                    IconButton(onClick = onAddProfile, enabled = !editingLocked) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.profiles_create))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (editingLocked) {
                Text(
                    text = editingLockedReason ?: stringResource(R.string.profiles_editing_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (profiles.isEmpty()) {
                EmptyProfilesState(onAddProfile = onAddProfile, editingLocked = editingLocked)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            editingLocked = editingLocked,
                            onToggleLockMode = { onToggleLockMode(profile.id) },
                            onEdit = { onEditProfile(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) },
                            onStartSession = {
                                onNavigateToStartSession(profile.id, profile.defaultDurationMinutes)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfilesState(onAddProfile: () -> Unit, editingLocked: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.profiles_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.profiles_empty_body), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddProfile, enabled = !editingLocked) {
            Text(stringResource(R.string.profiles_create))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    editingLocked: Boolean,
    onToggleLockMode: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartSession: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(profile.colorArgb).copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.emoji ?: "🎯")
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            when (profile.lockMode) {
                                is LockMode.Soft -> stringResource(R.string.profile_editor_soft_lock)
                                is LockMode.Hard -> stringResource(R.string.profile_editor_hard_lock)
                                else -> profile.lockMode.toString()
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleLockMode, enabled = !editingLocked) {
                        Icon(
                            imageVector = if (profile.lockMode is LockMode.Hard) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = stringResource(R.string.profiles_cd_toggle_lock),
                        )
                    }
                    IconButton(onClick = { showMenu = true }, enabled = !editingLocked) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.profiles_cd_more),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.profile_editor_edit_title)) },
                            onClick = { onEdit(); showMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.profiles_delete)) },
                            onClick = { onDelete(); showMenu = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_editor_apps_selected, profile.targetPackageNames.size))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.profile_editor_duration_minutes, profile.defaultDurationMinutes))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profiles_start_session, profile.defaultDurationMinutes))
            }
        }
    }
}
