package app.focus.feature.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesListScreen(
    profiles: List<app.focus.domain.model.Profile> = emptyList(),
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit = { _, _ -> },
    onAddProfile: () -> Unit = {},
    onEditProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onToggleLockMode: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
                actions = {
                    IconButton(onClick = onAddProfile) {
                        Icon(Icons.Default.Add, contentDescription = "Add profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (profiles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No profiles yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Create your first focus profile to get started.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddProfile) { Text("Create Profile") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        onToggleLockMode = { onToggleLockMode(profile.id) },
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { onDeleteProfile(profile.id) },
                        onStartSession = { onNavigateToStartSession(profile.id, profile.defaultDurationMinutes) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: app.focus.domain.model.Profile,
    onToggleLockMode: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartSession: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(profile.colorArgb)
                .copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.emoji ?: "🎯", fontSize = androidx.compose.ui.text.TextStyle.Default.fontSize)
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            when (profile.lockMode) {
                                is app.focus.domain.model.LockMode.Soft -> "Soft lock"
                                is app.focus.domain.model.LockMode.Hard -> "Hard lock"
                                else -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Row {
                    IconButton(onClick = onToggleLockMode) {
                        Icon(
                            imageVector = if (profile.lockMode is app.focus.domain.model.LockMode.Hard) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle lock mode"
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        if (showMenu) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { onEdit(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color.Red) },
                                    onClick = { onDelete(); showMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Profile details
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("📱 ${profile.targetPackageNames.size} apps")
                Spacer(Modifier.width(16.dp))
                Text("⏱ ${profile.defaultDurationMinutes} min default")
            }

            Spacer(Modifier.height(8.dp))

            // Emergency exit info
            if (profile.lockMode is app.focus.domain.model.LockMode.Hard) {
                Text(
                    "Emergency: ${profile.emergencyExitMode.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }

            Spacer(Modifier.height(8.dp))

            // Start button
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start ${profile.defaultDurationMinutes} min session")
            }
        }
    }
}
