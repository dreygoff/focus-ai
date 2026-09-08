package app.focus.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenProtectionInfo: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Appearance section
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            var themeSelected by remember { mutableIntStateOf(0) }
            listOf("System" to 0, "Dark" to 1, "Light" to 2).forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeSelected == value,
                        onClick = { themeSelected = value }
                    )
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }

                // Dynamic color toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dynamic color")
                    Switch(checked = true, onCheckedChange = {})
                }

                // Language selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Language")
                    Text("English", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                Spacer(Modifier.height(8.dp))
            }

            // Data section
            Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Storage, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export data as CSV")
            }

            TextButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear statistics", color = Color.Red)
            }

            // About section
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            TextButton(
                onClick = onOpenProtectionInfo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_protection_link))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Version")
                Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            TextButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open source licenses")
            }
        }
    }
}
