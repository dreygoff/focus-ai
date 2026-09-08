package app.focus.feature.permissions

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar as TB
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight as FW
import androidx.compose.ui.unit.dp

data class PermissionCheckItem(
    val id: String,
    val titleRes: String,
    val descriptionRes: String,
    val isGranted: Boolean,
    val isOptional: Boolean,
    val permissionType: app.focus.domain.model.PermissionType
) {
    companion object {
        fun from(id: String, type: app.focus.domain.model.PermissionType, granted: Boolean): PermissionCheckItem {
            val (title, description, isOpt) = when (type) {
                app.focus.domain.model.PermissionType.MANDATORY -> when (id) {
                    "usage_stats" -> Triple("Usage Stats Access", "Allows Focus to detect which apps are currently running. Required for blocking.", false)
                    "overlay" -> Triple("Display Over Other Apps", "Allows the block screen to appear over other apps when target apps are opened.", false)
                    else -> Triple(id, "Required permission", false)
                }
                app.focus.domain.model.PermissionType.RECOMMENDED -> when (id) {
                    "accessibility" -> Triple("Accessibility Service", "Provides faster and more reliable app detection than the fallback polling method.", true)
                    "notifications" -> Triple("Notifications", "Shows session status and alerts during focus sessions.", true)
                    "battery" -> Triple("Ignore Battery Optimization", "Prevents the system from killing Focus in background.", true)
                    else -> Triple(id, "Recommended for better performance", true)
                }
                app.focus.domain.model.PermissionType.OPTIONAL -> when (id) {
                    "exact_alarm" -> Triple("Exact Alarms", "Enables precise scheduling for automatic session start/stop.", true)
                    "device_admin" -> Triple("Device Admin", "Protects Focus from being uninstalled during hard lock sessions.", true)
                    else -> Triple(id, "Optional enhancement", true)
                }
            }
            return PermissionCheckItem(
                id = id, titleRes = title, descriptionRes = description,
                isGranted = granted, isOptional = isOpt, permissionType = type
            )
        }

        fun getActionIntent(id: String): Intent? {
            return when (id) {
                "usage_stats" -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                else -> null
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onRequestPermission: (String) -> Unit,
    onAllGranted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is PermissionsUiState.Ready && state.mandatoryGranted) {
            onAllGranted()
        }
    }

    when (state) {
        is PermissionsUiState.Loading -> LoadingScreen()
        is PermissionsUiState.Ready -> ReadyPermissionScreen(
            items = state.permissions, mandatoryGranted = state.mandatoryGranted,
            onRequestPermission = { id ->
                val intent = PermissionCheckItem.getActionIntent(id)
                if (id == "notifications") {
                    androidx.core.content.ContextCompat.startActivity(
                        context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }, null
                    )
                } else if (intent != null) {
                    context.startActivity(intent)
                }
                onRequestPermission(id)
            },
            onAllGranted = onAllGranted,
            onBack = onBack,
        )
        is PermissionsUiState.Error -> ErrorScreen(state.message, onBack)
    }
}

@Composable private fun LoadingScreen() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Checking permissions...")
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable private fun ReadyPermissionScreen(
    items: List<PermissionCheckItem>, mandatoryGranted: Boolean,
    onRequestPermission: (String) -> Unit, onAllGranted: () -> Unit, onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { TB(title = { Text("Permissions") }) }) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = if (mandatoryGranted) "All required permissions granted" else "Required permissions missing",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp), fontWeight = FW.Medium
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(items, key = { it.id }) { item ->
                    PermissionRow(item = item, onGrantClick = { onRequestPermission(item.id) })
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    if (mandatoryGranted) {
                        OutlinedButton(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), onClick = onAllGranted) { Text("Continue") }
                    } else {
                        OutlinedButton(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), onClick = onBack) { Text("Go Back") }
                    }
                }
            }
        }
    }
}

@Composable private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable private fun PermissionRow(item: PermissionCheckItem, onGrantClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (item.id) {
                        "usage_stats" -> Icons.Default.Shield
                        "overlay" -> Icons.Default.Lock
                        "accessibility" -> Icons.Default.CheckCircle
                        "notifications" -> Icons.Default.Notifications
                        else -> Icons.Default.Lock
                    }
                    Icon(icon, contentDescription = null, tint = if (item.isGranted) Color.Green else Color.Gray)
                    Text(text = item.titleRes, style = MaterialTheme.typography.bodyLarge, fontWeight = FW.Medium, modifier = Modifier.padding(start = 8.dp))
                }
                if (!item.isOptional && !item.isGranted) {
                    Text(text = "Required", style = MaterialTheme.typography.labelSmall, color = Color.Red, modifier = Modifier.padding(top = 2.dp))
                }
                Text(text = item.descriptionRes, style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
            }
            if (item.isGranted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                    Text("Granted", style = MaterialTheme.typography.bodySmall.copy(color = Color.Green))
                }
            } else if (!item.isOptional) {
                androidx.compose.material3.Button(onClick = onGrantClick) { Text("Grant") }
            } else {
                Text("Skip", style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray))
            }
        }
    }
}
