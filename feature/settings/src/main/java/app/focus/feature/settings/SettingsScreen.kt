package app.focus.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Chip
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onNavigateToPermissions: () -> Unit,
    onExportCsv: () -> Unit,
    onClearData: () -> Unit,
    onViewLicenses: () -> Unit,
    onProtectionInfo: () -> Unit,
    onDiagScreen: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        SectionTitle("Permissions")
        ListItemPerm("Manage app permissions", onNavigateToPermissions)

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Appearance")
        // Theme selection would be handled by DataStore in a full implementation
        SwitchRow("Dynamic Color", true)

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Block Screen")
        SwitchRow("Show quotes", true)
        SwitchRow("Vibration on block", false)
        SwitchRow("Sound on block", false)

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Allowlist")
        ListItemPerm("Manage allowlist apps", {})

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Data & Privacy")
        OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
            Text("Export CSV")
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(onClick = onClearData, modifier = Modifier.fillMaxWidth()) {
            Text("Clear all data")
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("About")
        Text("Focus v1.0.0 (Build 1)", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        ListItemPerm("Open-Source Licenses", onViewLicenses)
        ListItemPerm("Privacy Policy", {})
        ListItemPerm("How Protection Works", onProtectionInfo)

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDiagScreen) {
            Text("Diagnostics", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp), fontWeight = FontWeight.Medium)
}

@Composable
private fun ListItemPerm(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = {})
    }
}

object SettingsRoutes {
    const val ROUTE = "settings"
    const val PERMISSIONS = "$ROUTE/permissions"
    const val LICENSES = "$ROUTE/licenses"
}
