package app.focus.feature.session.start

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.feature.session.R

@Composable
fun StartSessionRoute(
    onBack: () -> Unit,
    onSessionStarted: () -> Unit,
    viewModel: StartSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val showHardLock by viewModel.showHardLockConfirmation.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                StartSessionNavEvent.SessionStarted -> onSessionStarted()
            }
        }
    }

    StartSessionScreen(
        profiles = profiles,
        uiState = uiState,
        onAction = viewModel::onEvent,
        onConfirm = viewModel::confirmStart,
        onBack = onBack,
    )

    if (showHardLock) {
        AlertDialog(
            onDismissRequest = viewModel::dismissHardLockConfirmation,
            title = { Text(stringResource(R.string.session_hard_lock_dialog_title)) },
            text = { Text(stringResource(R.string.session_hard_lock_dialog_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmHardLockStart) {
                    Text(stringResource(R.string.session_hard_lock_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissHardLockConfirmation) {
                    Text(stringResource(R.string.session_back))
                }
            },
        )
    }
}
