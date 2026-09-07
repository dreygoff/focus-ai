package app.focus.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartSession: (String, Int) -> Unit,
    onPauseSession: () -> Unit,
    onStopSession: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            uiState.activeSession?.let { session ->
                RunningSessionCard(
                    session = session,
                    onPause = onPauseSession,
                    onStop = onStopSession
                )
            } ?: run {
                Text(
                    "Start your focus session",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                uiState.profiles.forEach { profile ->
                    ProfileChip(name = "${profile.emoji ?: "\uD83D\uDE80"} ${profile.name}") {
                        onStartSession(profile.id, 25)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun RunningSessionCard(
    session: app.focus.domain.model.Session,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${session.profileNameSnapshot}: ${session.lockMode}")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPause) { Text("Pause") }
                Button(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun ProfileChip(name: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(name)
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeSession: app.focus.domain.model.Session? = null,
    val profiles: List<app.focus.domain.model.Profile> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val streakDays: Int = 0,
)
