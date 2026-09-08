package app.focus.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HomeUiState(
    val activeSession: app.focus.domain.model.Session? = null,
    val profiles: List<app.focus.domain.model.Profile> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val streakDays: Int = 0
)

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartSession: (profileId: String?, durationMinutes: Int) -> Unit,
    onPauseSession: () -> Unit,
    onStopSession: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(uiState.profiles.firstOrNull()?.colorArgb ?: 0xFF2F6F6D.toInt()),
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Focus",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onNavigateToProfiles) {
                    Icon(Icons.Default.Person, contentDescription = "Profiles")
                }
            }

            // Stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Today",
                    value = "${uiState.todayFocusMinutes}",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Streak",
                    value = "${uiState.streakDays}d",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active session or start button
            if (uiState.activeSession != null && uiState.activeSession.status is app.focus.domain.model.SessionStatus.Running) {
                ActiveSessionCard(
                    sessionId = uiState.activeSession.id,
                    profileId = uiState.activeSession.profileId,
                    plannedEndAt = uiState.activeSession.plannedEndAt,
                    onStart = { onStartSession(null, 25) },
                    onPause = onPauseSession,
                    onStop = onStopSession
                )
            } else {
                StartSessionCard(
                    profiles = uiState.profiles,
                    onQuickStart = { duration -> onStartSession(null, duration) },
                    onSelectProfile = { profileId -> onStartSession(profileId, 25) }
                )
            }

            // Profiles preview
            if (uiState.profiles.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Quick Start",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    uiState.profiles.take(3).forEach { profile ->
                        QuickStartButton(
                            name = profile.name,
                            emoji = profile.emoji ?: "🎯",
                            onClick = { onStartSession(profile.id, profile.defaultDurationMinutes) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun ActiveSessionCard(
    sessionId: String,
    profileId: String?,
    plannedEndAt: Long?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Session Active", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            // Circular progress indicator
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = 0.6f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp
                )
                Text("4:30", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) { Text("Quick Start") }
                OutlinedButton(onClick = onPause) { Text("Pause") }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun StartSessionCard(
    profiles: List<app.focus.domain.model.Profile>,
    onQuickStart: (Int) -> Unit,
    onSelectProfile: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ready to focus?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            // Duration presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(15, 25, 45, 60).forEach { duration ->
                    Button(
                        onClick = { onQuickStart(duration) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$duration min")
                    }
                }
            }

            if (profiles.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Or select a profile:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(profile.emoji ?: "🎯", fontSize = androidx.compose.ui.text.TextStyle.Default.fontSize)
                        Text(" ${profile.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Button(onClick = { onSelectProfile(profile.id) }) { Text("Start") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStartButton(
    name: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text("$emoji $name")
    }
}
