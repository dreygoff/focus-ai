package app.focus.feature.blocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.focus.domain.model.BreathingPhase
import app.focus.domain.model.BypassState
import app.focus.domain.model.EmergencyExitStep
import app.focus.domain.model.LockMode
import app.focus.domain.model.SettingsShortcut
import java.util.concurrent.TimeUnit

@Composable
fun BlockScreen(
    state: BlockUiState,
    onAction: (BlockAction) -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BlockHeader(state)

            when (val step = state.bypassStep) {
                null -> when (val emergency = state.emergencyExitStep) {
                    null -> BlockMainActions(state, onAction)
                    else -> EmergencyExitContent(emergency, onAction)
                }
                is BypassState.Granted -> BypassGrantedContent(onAction)
                else -> BypassStepContent(step = step, onAction = onAction)
            }

            TextButton(onClick = { onAction(BlockAction.OpenFocus) }) {
                Text(stringResource(R.string.block_open_focus))
            }
        }
    }
}

@Composable
private fun BlockMainActions(
    state: BlockUiState,
    onAction: (BlockAction) -> Unit,
) {
    val isHardLock = state.lockMode is LockMode.Hard
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { onAction(BlockAction.ReturnToWork) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.block_return_to_work))
        }

        if (!isHardLock) {
            if (state.bypassLimitReached) {
                Text(
                    stringResource(R.string.block_bypass_exhausted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedButton(
                    onClick = { onAction(BlockAction.StartBypass) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.block_bypass_cta))
                }
                if (state.bypassesRemaining >= 0) {
                    Text(
                        stringResource(R.string.block_bypasses_remaining, state.bypassesRemaining),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else if (state.emergencyExitMode != app.focus.domain.model.EmergencyExitMode.NONE) {
            OutlinedButton(
                onClick = { onAction(BlockAction.StartEmergencyExit) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.block_emergency_exit))
            }
        }

        if (isHardLock && state.allowedSettingsShortcuts.isNotEmpty()) {
            HardLockSettingsShortcuts(
                shortcuts = state.allowedSettingsShortcuts,
                onShortcut = { onAction(BlockAction.OpenSettingsShortcut(it)) },
            )
        }
    }
}

@Composable
private fun HardLockSettingsShortcuts(
    shortcuts: Set<SettingsShortcut>,
    onShortcut: (SettingsShortcut) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.block_allowed_settings),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        shortcuts.sortedBy { it.ordinal }.forEach { shortcut ->
            OutlinedButton(
                onClick = { onShortcut(shortcut) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(settingsShortcutLabel(shortcut))
            }
        }
    }
}

@Composable
private fun settingsShortcutLabel(shortcut: SettingsShortcut): String = when (shortcut) {
    SettingsShortcut.WIFI -> stringResource(R.string.block_shortcut_wifi)
    SettingsShortcut.BLUETOOTH -> stringResource(R.string.block_shortcut_bluetooth)
    SettingsShortcut.SOUND -> stringResource(R.string.block_shortcut_sound)
    SettingsShortcut.NOTIFICATIONS -> stringResource(R.string.block_shortcut_notifications)
    SettingsShortcut.CELLULAR -> stringResource(R.string.block_shortcut_cellular)
}

@Composable
private fun EmergencyExitContent(
    step: EmergencyExitStep,
    onAction: (BlockAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (step) {
            is EmergencyExitStep.Delay -> {
                val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(step.remainingMillis.coerceAtLeast(0))
                Text(
                    stringResource(R.string.block_emergency_delay, remainingMinutes + 1),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onAction(BlockAction.CancelEmergencyExit) }) {
                    Text(stringResource(R.string.block_emergency_cancel))
                }
            }
            is EmergencyExitStep.Retype -> EmergencyRetypeStep(step, onAction)
        }
    }
}

@Composable
private fun EmergencyRetypeStep(
    step: EmergencyExitStep.Retype,
    onAction: (BlockAction) -> Unit,
) {
    var fieldText by remember(step.targetText) { mutableStateOf(step.typedText) }
    Text(
        stringResource(R.string.block_emergency_retype_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(step.targetText, style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = fieldText,
        onValueChange = { newValue ->
            if (newValue.length - fieldText.length > 1) return@OutlinedTextField
            if (newValue.length < fieldText.length) {
                fieldText = newValue
                return@OutlinedTextField
            }
            if (newValue.length > fieldText.length) {
                fieldText = newValue
                onAction(BlockAction.InputEmergencyExitChar(newValue.last()))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.block_emergency_retype_title)) },
        keyboardOptions = KeyboardOptions(autoCorrect = false),
    )
    TextButton(onClick = { onAction(BlockAction.CancelEmergencyExit) }) {
        Text(stringResource(R.string.block_emergency_cancel))
    }
}

@Composable
private fun BypassStepContent(
    step: BypassState,
    onAction: (BlockAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (step) {
            is BypassState.Delay -> {
                val bypassDelayDescription = stringResource(
                    R.string.block_cd_bypass_delay,
                    step.remainingSeconds,
                )
                Text(
                    stringResource(R.string.block_bypass_delay, step.remainingSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                val progress = if (step.totalTimeSeconds > 0) {
                    1f - step.remainingSeconds.toFloat() / step.totalTimeSeconds
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                            contentDescription = bypassDelayDescription
                        },
                )
            }
            is BypassState.Breathing -> {
                val phaseLabel = breathingPhaseLabel(step.phaseInCycle)
                val breathingDescription = stringResource(
                    R.string.block_cd_breathing,
                    step.currentCycle + 1,
                    phaseLabel,
                )
                Text(
                    stringResource(R.string.block_bypass_breathing, step.currentCycle + 1),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    phaseLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clearAndSetSemantics {
                            contentDescription = breathingDescription
                        },
                )
            }
            is BypassState.Reason -> ReasonStep(step, onAction)
            is BypassState.Phrase -> PhraseStep(step, onAction)
            is BypassState.Denied -> {
                Text(
                    stringResource(R.string.block_bypass_exhausted),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> Unit
        }

        TextButton(onClick = { onAction(BlockAction.CancelBypass) }) {
            Text(stringResource(R.string.block_bypass_cancel))
        }
    }
}

@Composable
private fun ReasonStep(
    step: BypassState.Reason,
    onAction: (BlockAction) -> Unit,
) {
    var text by remember(step) { mutableStateOf(step.text) }
    Text(stringResource(R.string.block_bypass_reason_title), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.block_bypass_reason_label)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(autoCorrect = false),
    )
    Button(
        onClick = { onAction(BlockAction.SubmitBypassReason(text)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = text.length >= step.minLength,
    ) {
        Text(stringResource(R.string.block_bypass_continue))
    }
}

@Composable
private fun PhraseStep(
    step: BypassState.Phrase,
    onAction: (BlockAction) -> Unit,
) {
    var fieldText by remember(step.targetPhrase) { mutableStateOf(step.typedText) }
    Text(stringResource(R.string.block_bypass_phrase_title), style = MaterialTheme.typography.titleMedium)
    Text(step.targetPhrase, style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = fieldText,
        onValueChange = { newValue ->
            if (newValue.length - fieldText.length > 1) return@OutlinedTextField
            if (newValue.length < fieldText.length) {
                fieldText = newValue
                return@OutlinedTextField
            }
            if (newValue.length > fieldText.length) {
                val char = newValue.last()
                fieldText = newValue
                onAction(BlockAction.InputPhraseChar(char))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.block_bypass_phrase_title)) },
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            capitalization = KeyboardCapitalization.Sentences,
        ),
    )
}

@Composable
private fun BypassGrantedContent(onAction: (BlockAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.block_bypass_granted),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { onAction(BlockAction.ReturnToWork) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.block_open_app))
        }
    }
}

@Composable
private fun breathingPhaseLabel(phase: BreathingPhase): String = when (phase) {
    BreathingPhase.INHALE -> stringResource(R.string.block_breathing_inhale)
    BreathingPhase.HOLD -> stringResource(R.string.block_breathing_hold)
    BreathingPhase.EXHALE -> stringResource(R.string.block_breathing_exhale)
}
