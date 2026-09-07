package app.focus.feature.schedules

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SchedulesScreen(
    schedules: List<app.focus.domain.model.Schedule>,
    onAddSchedule: () -> Unit,
    onEditSchedule: (app.focus.domain.model.Schedule) -> Unit,
    onToggleSchedule: (app.focus.domain.model.Schedule) -> Unit
) {
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAddSchedule) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Schedules", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            if (schedules.isEmpty()) {
                Text("No schedules yet. Add one to automate your focus sessions.", modifier = Modifier.padding(16.dp))
            } else {
                schedules.forEach { schedule ->
                    ScheduleCard(schedule, onToggle = { onToggleSchedule(schedule) }, onClick = { onEditSchedule(schedule) })
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: app.focus.domain.model.Schedule,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(schedule.label ?: "Schedule", style = MaterialTheme.typography.titleMedium)
                val days = getDaysString(schedule.daysOfWeekMask)
                Text("Days: $days", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
                Text("${formatMinutes(schedule.startMinuteOfDay)} - ${formatMinutes(schedule.endMinuteOfDay)}", 
                    style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Gray)
            }
            Switch(checked = schedule.enabled, onCheckedChange = { if (it) onToggle() })
        }
    }
}

private fun getDaysString(mask: Int): String {
    val days = mutableListOf<String>()
    for (i in 0..6) { if (mask and (1 shl i) != 0) days.add(listOf("M","T","W","T","F","S","S")[i]) }
    return days.joinToString(", ")
}

private fun formatMinutes(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    return String.format("%02d:%02d", h, m)
}

object SchedulesRoutes {
    const val ROUTE = "schedules"
    const val EDITOR = "$ROUTE/editor/{scheduleId}"
    
    fun routeWithId(id: String) = "$EDITOR/$id"
}
