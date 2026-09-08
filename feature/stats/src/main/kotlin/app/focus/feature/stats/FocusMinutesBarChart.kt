package app.focus.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import app.focus.domain.model.DailyStats

@Composable
fun FocusMinutesBarChart(
    dailyStats: List<DailyStats>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxMinutes = dailyStats.maxOfOrNull { it.focusMinutes }?.coerceAtLeast(1) ?: 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT.dp),
    ) {
        if (dailyStats.isEmpty()) return@Canvas

        val barGap = size.width * BAR_GAP_RATIO
        val barWidth = (size.width - barGap * (dailyStats.size + 1)) / dailyStats.size
        dailyStats.forEachIndexed { index, day ->
            val fraction = day.focusMinutes.toFloat() / maxMinutes.toFloat()
            val barHeight = size.height * fraction
            val left = barGap + index * (barWidth + barGap)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(BAR_CORNER_RADIUS, BAR_CORNER_RADIUS),
            )
        }
    }
}

private const val CHART_HEIGHT = 160
private const val BAR_GAP_RATIO = 0.02f
private const val BAR_CORNER_RADIUS = 8f
