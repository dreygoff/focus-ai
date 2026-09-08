package app.focus.feature.permissions

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.system.PermissionChecker

private const val GRANTED_COLOR = 0xFF2E7D32

private data class ReadyScreenContent(
    val items: List<PermissionCheckItem>,
    val mandatoryGranted: Boolean,
    val showRestrictedSettingsHint: Boolean,
    val onRequestPermission: (String) -> Unit,
    val onAllGranted: () -> Unit,
    val onBack: () -> Unit,
)

@Composable
fun PermissionsRoute(
    onAllGranted: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showDisclosure by viewModel.showAccessibilityDisclosure.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAccessibilityDisclosure,
            title = { Text(stringResource(R.string.accessibility_disclosure_title)) },
            text = { Text(stringResource(R.string.accessibility_disclosure_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::acceptAccessibilityDisclosure) {
                    Text(stringResource(R.string.accessibility_disclosure_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAccessibilityDisclosure) {
                    Text(stringResource(R.string.accessibility_disclosure_cancel))
                }
            },
        )
    }

    PermissionsScreen(
        state = state,
        onRequestPermission = viewModel::onPermissionClick,
        onAllGranted = onAllGranted,
        onBack = onBack,
        onRetry = viewModel::refreshPermissions,
    )
}

@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onRequestPermission: (String) -> Unit,
    onAllGranted: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
) {
    when (state) {
        is PermissionsUiState.Loading -> LoadingScreen()
        is PermissionsUiState.Ready -> ReadyPermissionScreen(
            ReadyScreenContent(
                items = state.permissions,
                mandatoryGranted = state.mandatoryGranted,
                showRestrictedSettingsHint = state.showRestrictedSettingsHint,
                onRequestPermission = onRequestPermission,
                onAllGranted = onAllGranted,
                onBack = onBack,
            ),
        )
        is PermissionsUiState.Error -> ErrorScreen(state.message, onRetry)
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.permissions_loading))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyPermissionScreen(content: ReadyScreenContent) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_title)) },
                navigationIcon = {
                    IconButton(onClick = content.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.permissions_cd_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PermissionStatusCard(content.mandatoryGranted)
            if (content.showRestrictedSettingsHint) {
                RestrictedSettingsHintCard()
            }
            PermissionList(content)
        }
    }
}

@Composable
private fun PermissionStatusCard(mandatoryGranted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(
                if (mandatoryGranted) R.string.permissions_all_granted else R.string.permissions_missing,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RestrictedSettingsHintCard() {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.restricted_settings_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.restricted_settings_body),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PermissionList(content: ReadyScreenContent) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(content.items, key = { it.id }) { item ->
            PermissionRow(item = item, onGrantClick = { content.onRequestPermission(item.id) })
            Spacer(Modifier.height(8.dp))
        }
        item {
            Spacer(Modifier.height(16.dp))
            PermissionFooter(
                mandatoryGranted = content.mandatoryGranted,
                onAllGranted = content.onAllGranted,
                onBack = content.onBack,
            )
        }
    }
}

@Composable
private fun PermissionFooter(
    mandatoryGranted: Boolean,
    onAllGranted: () -> Unit,
    onBack: () -> Unit,
) {
    val label = if (mandatoryGranted) R.string.permissions_continue else R.string.permissions_back
    val action = if (mandatoryGranted) onAllGranted else onBack
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        onClick = action,
    ) {
        Text(stringResource(label))
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.permissions_retry)) }
    }
}

@Composable
private fun PermissionRow(item: PermissionCheckItem, onGrantClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        permissionIcon(item.id),
                        contentDescription = stringResource(item.titleRes),
                        tint = if (item.isGranted) Color(GRANTED_COLOR) else Color.Gray,
                    )
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (!item.isOptional && !item.isGranted) {
                    Text(
                        text = stringResource(R.string.permissions_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = stringResource(item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            PermissionRowAction(item = item, onGrantClick = onGrantClick)
        }
    }
}

@Composable
private fun PermissionRowAction(item: PermissionCheckItem, onGrantClick: () -> Unit) {
    when {
        item.isGranted -> {
            Text(
                stringResource(R.string.permissions_granted),
                style = MaterialTheme.typography.bodySmall,
                color = Color(GRANTED_COLOR),
            )
        }
        item.isOptional -> {
            TextButton(onClick = onGrantClick) {
                Text(stringResource(R.string.permissions_skip))
            }
        }
        else -> {
            Button(onClick = onGrantClick) {
                Text(stringResource(R.string.permissions_grant))
            }
        }
    }
}

private fun permissionIcon(id: String) = when (id) {
    PermissionChecker.ID_USAGE_STATS -> Icons.Default.Shield
    PermissionChecker.ID_OVERLAY -> Icons.Default.Lock
    PermissionChecker.ID_NOTIFICATIONS -> Icons.Default.Notifications
    else -> Icons.Default.CheckCircle
}
