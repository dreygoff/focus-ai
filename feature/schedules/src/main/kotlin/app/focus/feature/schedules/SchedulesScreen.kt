package app.focus.feature.schedules

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    schedules: List<app.focus.domain.model.Schedule> = emptyList(),
    onAddSchedule: () -> Unit = {},
    onEditSchedule: (String) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules") },
                actions = {
                    IconButton(onClick = onAddSchedule) {
                        Icon(Icons.Default.Add, contentDescription = "Add schedule")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (schedules.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No schedules yet", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Create a schedule to automatically start focus sessions.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddSchedule) { Text("Add Schedule") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { onEditSchedule(schedule.id) },
                        onDelete = { onDeleteSchedule(schedule.id) }
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
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    schedule.label ?: "Schedule",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Days of week indicator
            val days = getDayNames(schedule.daysOfWeekMask)
            Text("Days: $days", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(4.dp))

            // Time range
            val startTime = "${schedule.startMinuteOfDay / 60}:${String.format("%02d", schedule.startMinuteOfDay % 60)}"
            val endTime = "${schedule.endMinuteOfDay / 60}:${String.format("%02d", schedule.endMinuteOfDay % 60)}"
            Text("$startTime → $endTime", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            // Enabled/disabled switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(if (schedule.enabled) "Enabled" else "Disabled")
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = {}
                )
            }
        }
    }
}

private fun getDayNames(mask: Int): String {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString(", ")
}
