package app.focus.feature.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profile: app.focus.domain.model.Profile? = null,
    onSave: (app.focus.domain.model.Profile) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var lockMode by remember { mutableStateOf(profile?.lockMode ?: app.focus.domain.model.LockMode.Soft) }
    var duration by remember { mutableIntStateOf(profile?.defaultDurationMinutes ?: 25) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profile != null) "Edit Profile" else "New Profile") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Lock mode selector
            Text("Lock Mode", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = lockMode is app.focus.domain.model.LockMode.Soft,
                    onClick = { lockMode = app.focus.domain.model.LockMode.Soft },
                    label = { Text("Soft Lock") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = lockMode is app.focus.domain.model.LockMode.Hard,
                    onClick = { lockMode = app.focus.domain.model.LockMode.Hard },
                    label = { Text("Hard Lock") }
                )
            }

            // Duration preset
            Text("Default duration", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = duration == mins,
                        onClick = { duration = mins },
                        label = { Text("$mins min") }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    val newProfile = app.focus.domain.model.Profile(
                        id = profile?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name,
                        emoji = "🎯",
                        colorArgb = 0xFF2F6F6D.toInt(),
                        lockMode = lockMode,
                        defaultDurationMinutes = duration,
                        bypassDelaySeconds = 30,
                        bypassBreathingEnabled = true,
                        bypassReasonRequired = true,
                        bypassPhrase = null,
                        bypassLimitPerSession = 3,
                        accessWindowMinutes = 5,
                        bypassAppliesToAllApps = false,
                        emergencyExitMode = app.focus.domain.model.EmergencyExitMode.NONE,
                        blockNewApps = lockMode is app.focus.domain.model.LockMode.Hard,
                        deviceAdminProtection = lockMode is app.focus.domain.model.LockMode.Hard,
                        allowedSettingsShortcuts = emptySet(),
                        hideTargetNotifications = false,
                        targetPackageNames = profile?.targetPackageNames ?: emptyList()
                    )
                    onSave(newProfile)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
