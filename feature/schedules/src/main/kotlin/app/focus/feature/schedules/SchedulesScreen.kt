package app.focus.feature.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    schedules: List<app.focus.domain.model.Schedule> = emptyList(),
    onAddSchedule: () -> Unit = {},
    onEditSchedule: (String) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onToggleEnabled: (app.focus.domain.model.Schedule) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedules_title)) },
                actions = {
                    IconButton(onClick = onAddSchedule) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.schedules_cd_add),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (schedules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.schedules_empty_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.schedules_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddSchedule) {
                    Text(stringResource(R.string.schedules_add))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { onEditSchedule(schedule.id) },
                        onDelete = { onDeleteSchedule(schedule.id) },
                        onToggleEnabled = { onToggleEnabled(schedule) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: app.focus.domain.model.Schedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
) {
    val dayNames = stringArrayResource(R.array.schedule_day_names)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    schedule.label ?: stringResource(R.string.schedules_default_label),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.schedules_cd_delete),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val days = formatDayNames(schedule.daysOfWeekMask, dayNames)
            Text(
                stringResource(R.string.schedules_days, days),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(4.dp))

            val startTime = "${schedule.startMinuteOfDay / 60}:${String.format("%02d", schedule.startMinuteOfDay % 60)}"
            val endTime = "${schedule.endMinuteOfDay / 60}:${String.format("%02d", schedule.endMinuteOfDay % 60)}"
            Text("$startTime → $endTime", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (schedule.enabled) {
                        stringResource(R.string.schedules_enabled)
                    } else {
                        stringResource(R.string.schedules_disabled)
                    },
                )
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { onToggleEnabled() },
                )
            }
        }
    }
}

private fun formatDayNames(mask: Int, dayNames: Array<String>): String =
    dayNames.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString(", ")
