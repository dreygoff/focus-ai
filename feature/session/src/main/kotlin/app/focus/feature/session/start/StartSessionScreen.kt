package app.focus.feature.session.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.focus.domain.model.LockMode
import app.focus.feature.session.R
import java.time.ZonedDateTime

private const val MIN_DURATION_MINUTES = 1
private const val MAX_DURATION_MINUTES = 1440
private const val DEFAULT_UNTIL_HOUR = 18
private const val DEFAULT_UNTIL_MINUTE = 0
private const val MAX_HOUR = 23
private const val MAX_MINUTE = 59
private const val PRESET_15_MIN = 15
private const val PRESET_25_MIN = 25
private const val PRESET_45_MIN = 45
private const val PRESET_60_MIN = 60
private const val PRESET_90_MIN = 90
private const val PRESET_120_MIN = 120
private val PRESET_DURATIONS_MINUTES = intArrayOf(
    PRESET_15_MIN,
    PRESET_25_MIN,
    PRESET_45_MIN,
    PRESET_60_MIN,
    PRESET_90_MIN,
    PRESET_120_MIN,
)

enum class DurationMode { PRESET, CUSTOM, UNTIL_TIME, INFINITE }

data class StartSessionUiState(
    val selectedProfileId: String? = null,
    val durationMinutes: Int = PRESET_25_MIN,
    val goalText: String = "",
    val pomodoroEnabled: Boolean = false,
    val durationMode: DurationMode = DurationMode.PRESET,
    val untilHour: Int = DEFAULT_UNTIL_HOUR,
    val untilMinute: Int = DEFAULT_UNTIL_MINUTE,
    val infinite: Boolean = false,
    val plannedEndAtMillis: Long? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSessionScreen(
    profiles: List<app.focus.domain.model.Profile>,
    uiState: StartSessionUiState,
    onAction: (StartSessionEvent) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val selectedProfile = profiles.firstOrNull { it.id == uiState.selectedProfileId }
    val isSoftLock = selectedProfile?.lockMode is LockMode.Soft
    var customMinutes by remember(uiState.durationMinutes) {
        mutableIntStateOf(uiState.durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES))
    }
    var untilHourText by remember { mutableStateOf(uiState.untilHour.toString()) }
    var untilMinuteText by remember { mutableStateOf(uiState.untilMinute.toString().padStart(2, '0')) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.session_start_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isNotEmpty()) {
            Text(
                stringResource(R.string.session_profile),
                style = MaterialTheme.typography.titleMedium,
            )
            profiles.forEach { profile ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                    Checkbox(
                        checked = uiState.selectedProfileId == profile.id,
                        onCheckedChange = { if (it) onAction(StartSessionEvent.SelectProfile(profile.id)) },
                    )
                    Text(
                        stringResource(
                            R.string.session_profile_option,
                            profile.emoji ?: "\uD83D\uDD12",
                            profile.name,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            stringResource(R.string.session_duration),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PRESET_DURATIONS_MINUTES.forEach { mins ->
                FilterChip(
                    selected = uiState.durationMode == DurationMode.PRESET && uiState.durationMinutes == mins,
                    onClick = {
                        onAction(StartSessionEvent.SetDurationMode(DurationMode.PRESET))
                        onAction(StartSessionEvent.SetDuration(mins))
                    },
                    label = { Text(stringResource(R.string.session_duration_min, mins)) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = uiState.durationMode == DurationMode.CUSTOM,
            onClick = { onAction(StartSessionEvent.SetDurationMode(DurationMode.CUSTOM)) },
            label = { Text(stringResource(R.string.session_duration_custom)) },
        )
        if (uiState.durationMode == DurationMode.CUSTOM) {
            Text(stringResource(R.string.session_duration_custom_value, customMinutes))
            Slider(
                value = customMinutes.toFloat(),
                onValueChange = {
                    customMinutes = it.toInt()
                    onAction(StartSessionEvent.SetDuration(customMinutes))
                },
                valueRange = MIN_DURATION_MINUTES.toFloat()..MAX_DURATION_MINUTES.toFloat(),
                steps = 0,
            )
        }

        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = uiState.durationMode == DurationMode.UNTIL_TIME,
            onClick = { onAction(StartSessionEvent.SetDurationMode(DurationMode.UNTIL_TIME)) },
            label = { Text(stringResource(R.string.session_duration_until)) },
        )
        if (uiState.durationMode == DurationMode.UNTIL_TIME) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = untilHourText,
                    onValueChange = { untilHourText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.session_until_hour)) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = untilMinuteText,
                    onValueChange = { untilMinuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.session_until_minute)) },
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                onClick = {
                    val hour = untilHourText.toIntOrNull()?.coerceIn(0, MAX_HOUR) ?: DEFAULT_UNTIL_HOUR
                    val minute = untilMinuteText.toIntOrNull()?.coerceIn(0, MAX_MINUTE) ?: DEFAULT_UNTIL_MINUTE
                    onAction(StartSessionEvent.SetUntilTime(hour, minute))
                    onAction(StartSessionEvent.SetPlannedEndAt(computeUntilTimeMillis(hour, minute)))
                },
            ) {
                Text(stringResource(R.string.session_apply_until_time))
            }
        }

        if (isSoftLock) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.durationMode == DurationMode.INFINITE,
                    onCheckedChange = {
                        if (it) {
                            onAction(StartSessionEvent.SetDurationMode(DurationMode.INFINITE))
                            onAction(StartSessionEvent.SetInfinite(true))
                        } else {
                            onAction(StartSessionEvent.SetInfinite(false))
                            onAction(StartSessionEvent.SetDurationMode(DurationMode.PRESET))
                        }
                    },
                )
                Text(stringResource(R.string.session_duration_infinite))
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.goalText,
            onValueChange = { onAction(StartSessionEvent.SetGoal(it)) },
            label = { Text(stringResource(R.string.session_goal_optional)) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (selectedProfile?.lockMode is LockMode.Hard) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Text(
                    stringResource(R.string.session_hard_lock_warning),
                    modifier = Modifier.padding(8.dp),
                    color = Color.Red,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.session_start))
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.session_back))
        }
    }
}

private fun computeUntilTimeMillis(hour: Int, minute: Int): Long {
    val now = ZonedDateTime.now()
    var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!target.isAfter(now)) {
        target = target.plusDays(1)
    }
    return target.toInstant().toEpochMilli()
}

sealed interface StartSessionEvent {
    data class SelectProfile(val id: String) : StartSessionEvent
    data class SetDuration(val minutes: Int) : StartSessionEvent
    data class SetGoal(val text: String) : StartSessionEvent
    data class TogglePomodoro(val enabled: Boolean) : StartSessionEvent
    data class SetDurationMode(val mode: DurationMode) : StartSessionEvent
    data class SetUntilTime(val hour: Int, val minute: Int) : StartSessionEvent
    data class SetPlannedEndAt(val epochMillis: Long) : StartSessionEvent
    data class SetInfinite(val enabled: Boolean) : StartSessionEvent
}
