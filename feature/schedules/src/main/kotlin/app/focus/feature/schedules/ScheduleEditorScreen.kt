package app.focus.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.focus.domain.model.Schedule
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    profiles: List<app.focus.domain.model.Profile>,
    existing: Schedule? = null,
    onSave: (Schedule) -> Unit,
    onBack: () -> Unit,
) {
    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var profileId by remember(existing, profiles) {
        mutableStateOf(existing?.profileId ?: profiles.firstOrNull()?.id.orEmpty())
    }
    var startHour by remember(existing) { mutableIntStateOf((existing?.startMinuteOfDay ?: 9 * 60) / 60) }
    var startMinute by remember(existing) { mutableIntStateOf((existing?.startMinuteOfDay ?: 9 * 60) % 60) }
    var endHour by remember(existing) { mutableIntStateOf((existing?.endMinuteOfDay ?: 17 * 60) / 60) }
    var endMinute by remember(existing) { mutableIntStateOf((existing?.endMinuteOfDay ?: 17 * 60) % 60) }
    var daysMask by remember(existing) { mutableIntStateOf(existing?.daysOfWeekMask ?: 0b0111110) }
    var allowSkipDay by remember(existing) { mutableStateOf(existing?.allowSkipDay ?: false) }
    var enabled by remember(existing) { mutableStateOf(existing?.enabled ?: true) }

    val dayLabels = stringArrayResource(R.array.schedule_day_names)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) {
                            stringResource(R.string.schedule_editor_new)
                        } else {
                            stringResource(R.string.schedule_editor_edit)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.schedule_editor_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.schedule_editor_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.schedule_editor_profile), style = MaterialTheme.typography.titleSmall)
            profiles.forEach { profile ->
                FilterChip(
                    selected = profileId == profile.id,
                    onClick = { profileId = profile.id },
                    label = { Text(profile.name) },
                )
            }

            Text(stringResource(R.string.schedule_editor_days), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayLabels.forEachIndexed { index, day ->
                    FilterChip(
                        selected = daysMask and (1 shl index) != 0,
                        onClick = {
                            daysMask = if (daysMask and (1 shl index) != 0) {
                                daysMask and (1 shl index).inv()
                            } else {
                                daysMask or (1 shl index)
                            }
                        },
                        label = { Text(day) },
                    )
                }
            }

            TimeRow(
                title = stringResource(R.string.schedule_editor_start),
                hour = startHour,
                minute = startMinute,
            ) { h, m ->
                startHour = h
                startMinute = m
            }
            TimeRow(
                title = stringResource(R.string.schedule_editor_end),
                hour = endHour,
                minute = endMinute,
            ) { h, m ->
                endHour = h
                endMinute = m
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.schedule_editor_allow_skip))
                Switch(checked = allowSkipDay, onCheckedChange = { allowSkipDay = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.schedule_editor_enabled))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Button(
                onClick = {
                    onSave(
                        Schedule(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            profileId = profileId,
                            enabled = enabled,
                            daysOfWeekMask = daysMask,
                            startMinuteOfDay = startHour * 60 + startMinute,
                            endMinuteOfDay = endHour * 60 + endMinute,
                            allowSkipDay = allowSkipDay,
                            label = label.ifBlank { null },
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = profileId.isNotBlank() && daysMask != 0,
            ) {
                Text(stringResource(R.string.schedule_editor_save))
            }
        }
    }
}

@Composable
private fun TimeRow(
    title: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.width(48.dp))
        OutlinedTextField(
            value = hour.toString(),
            onValueChange = { onChange(it.toIntOrNull()?.coerceIn(0, 23) ?: hour, minute) },
            modifier = Modifier.width(72.dp),
            label = { Text(stringResource(R.string.schedule_editor_hour)) },
        )
        Text(":")
        OutlinedTextField(
            value = minute.toString(),
            onValueChange = { onChange(hour, it.toIntOrNull()?.coerceIn(0, 59) ?: minute) },
            modifier = Modifier.width(72.dp),
            label = { Text(stringResource(R.string.schedule_editor_minute)) },
        )
    }
}
