package app.focus.feature.session.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class StartSessionUiState(
    val selectedProfileId: String? = null,
    val durationMinutes: Int = 25,
    val goalText: String = "",
    val pomodoroEnabled: Boolean = false,
) {
    val isHardLock get() = targetPackagesList.isNotEmpty()
    val targetPackagesList: List<String> = if (selectedProfileId != null) listOf("instagram", "facebook") else emptyList()
}

@Composable
fun StartSessionScreen(
    profiles: List<app.focus.domain.model.Profile>,
    uiState: StartSessionUiState,
    onAction: (StartSessionEvent) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val selectedDurations = remember { mutableStateOf(25) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Start Focus Session", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isNotEmpty()) {
            Text("Profile", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            profiles.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    Checkbox(
                        checked = uiState.selectedProfileId == p.id,
                        onCheckedChange = { if (it) onAction(StartSessionEvent.SelectProfile(p.id)) }
                    )
                    Text("${p.emoji ?: "\uD83D\uDD12"} ${p.name}")
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Text("Duration (min)" , style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(15, 25, 45, 60, 90).forEach { mins ->
                Button(
                    onClick = { selectedDurations.value = mins; onAction(StartSessionEvent.SetDuration(mins)) },
                    modifier = Modifier.weight(1f),
                    enabled = true
                ) { Text("$mins min") }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.goalText,
            onValueChange = { onAction(StartSessionEvent.SetGoal(it)) },
            label = { Text("Goal (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (profiles.any { p -> uiState.selectedProfileId == p.id && p.lockMode is app.focus.domain.model.LockMode.Hard }) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Text(
                    "\u26A0\uFE0F Hard Lock will block all apps including Settings.",
                    modifier = Modifier.padding(8.dp),
                    color = Color.Red
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Start") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

sealed interface StartSessionEvent {
    data class SelectProfile(val id: String) : StartSessionEvent
    data class SetDuration(val minutes: Int) : StartSessionEvent
    data class SetGoal(val text: String) : StartSessionEvent
    data class TogglePomodoro(val enabled: Boolean) : StartSessionEvent
}
