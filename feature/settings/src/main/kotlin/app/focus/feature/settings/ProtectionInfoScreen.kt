package app.focus.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionInfoScreen(
    onBack: () -> Unit = {},
    viewModel: ProtectionInfoViewModel = hiltViewModel(),
) {
    val deviceAdminActive by viewModel.deviceAdminActive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDeviceAdminStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.protection_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.protection_info_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.protection_info_body), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.protection_info_limitations_title), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.protection_info_limitations_body), style = MaterialTheme.typography.bodyMedium)

            DeviceAdminCard(
                active = deviceAdminActive,
                onDeactivate = viewModel::deactivateDeviceAdmin,
                onOpenSettings = {
                    context.startActivity(viewModel.deviceAdminSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
            )
        }
    }
}

@Composable
private fun DeviceAdminCard(
    active: Boolean,
    onDeactivate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.protection_device_admin_title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(
                    if (active) R.string.protection_device_admin_active else R.string.protection_device_admin_inactive,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (active) {
                Button(onClick = onDeactivate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.protection_device_admin_deactivate))
                }
            } else {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.protection_device_admin_open_settings))
                }
            }
        }
    }
}
