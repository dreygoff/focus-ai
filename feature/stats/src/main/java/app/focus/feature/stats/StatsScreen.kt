package app.focus.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(onBack: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("7 дней", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            Text("30 дней", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            Text("Сегодня", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        }
        Row(Modifier.padding(vertical = 16.dp)) {
            StatCard("\uD83D\uDFAF", "4", "Сессий")
            StatCard("\u23F1", "2ч", "Фокус мин")
            StatCard("\uD83D\uDEAB", "23", "Блокировок")
            StatCard("\uD83D\uDD25", "7", "Серия")
        }
        Spacer(Modifier.height(16.dp))
        Text("Диаграмма по дням", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        BarChart()
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String) {
    Card(modifier = Modifier.weight(1f).padding(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(icon, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text(value, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun BarChart() {
    val heights = listOf(60f, 45f, 80f, 30f, 90f, 50f, 70f)
    val days = listOf("П", "В", "С", "Ч", "П", "С", "В")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(32.dp, heights[i].dp).background(MaterialTheme.colorScheme.primary.copy(0.6f)))
                Text(day, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}

object StatsRoutes {
    const val ROUTE = "stats"
    const val EVENT_LOG = "$ROUTE/event_log"
}
