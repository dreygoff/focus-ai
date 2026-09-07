package app.focus.feature.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfilesListScreen(
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profiles", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = {}) {}
        }
        Spacer(Modifier.size(8.dp))
        ProfileCard(name = "Work", emoji = "\uD83D\uDE80", mode = "SOFT")
    }
}

@Composable
fun PickAppsScreen(onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("Select apps to block", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            androidx.compose.material3.ElevatedButton(onClick = onDone) { Text("Done") }
        }
    }
}

@Composable
private fun ProfileCard(name: String, emoji: String, mode: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(mode, style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            }
        }
    }
}
