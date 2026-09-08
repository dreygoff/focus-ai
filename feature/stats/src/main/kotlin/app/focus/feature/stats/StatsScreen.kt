package app.focus.feature.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object StatsRoutes {
    const val ROUTE = "stats"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today" to 0, "7 days" to 7, "30 days" to 30).forEach { (label, days) ->
                    FilterChip(
                        selected = days == 7,
                        onClick = {},
                        label = { Text(label) }
                    )
                }
            }

            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatSummaryCard(
                    label = "Sessions",
                    value = "0",
                    icon = Icons.Default.Equalizer,
                    modifier = Modifier.weight(1f)
                )
                StatSummaryCard(
                    label = "Focus",
                    value = "0 min",
                    icon = Icons.Default.Equalizer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Streak card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current streak", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("0 days", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Top blocked apps
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top blocked apps", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            // Export button
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export as CSV")
            }
        }
    }
}

@Composable
private fun StatSummaryCard(
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
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
