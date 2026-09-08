package app.focus.feature.stats

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.focus.domain.usecase.StatsPeriod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatsRoutes {
    const val ROUTE = "stats"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val csv = pendingExport
        if (uri != null && csv != null) {
            writeCsv(context, uri, csv)
        }
        pendingExport = null
        viewModel.clearExportRequest()
    }

    LaunchedEffect(uiState.exportCsv) {
        val csv = uiState.exportCsv ?: return@LaunchedEffect
        pendingExport = csv
        exportLauncher.launch("focus_stats_${System.currentTimeMillis()}.csv")
        viewModel.clearExportRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {},
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading && uiState.dashboard == null) {
            StatsLoadingState(Modifier.fillMaxSize().padding(paddingValues))
            return@Scaffold
        }

        StatsContent(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun StatsLoadingState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun StatsContent(
    modifier: Modifier,
    uiState: StatsViewModel.UiState,
    viewModel: StatsViewModel,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PeriodSelector(uiState.period, viewModel::selectPeriod) }
        item { uiState.dashboard?.let { SummaryCards(it) } }
        item { uiState.dashboard?.let { StreakCard(it.currentStreak, it.bestStreak) } }
        item { uiState.dashboard?.let { FocusChartCard(it.dailyStats) } }
        item { uiState.dashboard?.let { TopBlockedAppsCard(it.topBlockedApps) } }
        item { EventLogSection(uiState.events, uiState.eventFilter, viewModel::selectEventFilter) }
        item {
            Button(onClick = viewModel::requestExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_export_csv))
            }
        }
    }
}

@Composable
private fun FocusChartCard(dailyStats: List<app.focus.domain.model.DailyStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_focus_minutes), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FocusMinutesBarChart(dailyStats)
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(periodLabel(period)) },
            )
        }
    }
}

@Composable
private fun periodLabel(period: StatsPeriod): String = when (period) {
    StatsPeriod.TODAY -> stringResource(R.string.stats_period_today)
    StatsPeriod.WEEK -> stringResource(R.string.stats_period_week)
    StatsPeriod.MONTH -> stringResource(R.string.stats_period_month)
}

@Composable
private fun SummaryCards(dashboard: app.focus.domain.usecase.StatsDashboard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatSummaryCard(
            stringResource(R.string.stats_kpi_sessions),
            "${dashboard.sessionsCompleted}",
            Modifier.weight(1f),
        )
        StatSummaryCard(
            stringResource(R.string.stats_kpi_focus),
            stringResource(R.string.stats_focus_minutes_value, dashboard.totalFocusMinutes),
            Modifier.weight(1f),
        )
        StatSummaryCard(
            stringResource(R.string.stats_kpi_blocks),
            "${dashboard.blockAttempts}",
            Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatSummaryCard(
            stringResource(R.string.stats_kpi_bypasses),
            "${dashboard.bypasses}",
            Modifier.weight(1f),
        )
        StatSummaryCard(
            stringResource(R.string.stats_kpi_completed),
            stringResource(R.string.stats_completed_percent, dashboard.completionRatePercent),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StreakCard(current: Int, best: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.stats_current_streak), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.stats_streak_days, current, current),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.stats_best_streak), style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.stats_streak_days, best, best),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun TopBlockedAppsCard(apps: List<app.focus.domain.usecase.BlockedAppStat>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_top_blocked), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (apps.isEmpty()) {
                Text(
                    stringResource(R.string.stats_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            } else {
                apps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(app.packageName, style = MaterialTheme.typography.bodyMedium)
                        Text("${app.attempts}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventLogSection(
    events: List<app.focus.domain.model.EventLog>,
    filter: StatsViewModel.EventFilter,
    onFilterSelected: (StatsViewModel.EventFilter) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_event_log), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsViewModel.EventFilter.entries.forEach { entry ->
                    FilterChip(
                        selected = filter == entry,
                        onClick = { onFilterSelected(entry) },
                        label = { Text(eventFilterLabel(entry)) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text(
                    stringResource(R.string.stats_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            } else {
                events.take(EVENT_LOG_PREVIEW_LIMIT).forEach { event ->
                    EventLogRow(event)
                }
            }
        }
    }
}

@Composable
private fun eventFilterLabel(filter: StatsViewModel.EventFilter): String = when (filter) {
    StatsViewModel.EventFilter.ALL -> stringResource(R.string.stats_filter_all)
    StatsViewModel.EventFilter.SESSIONS -> stringResource(R.string.stats_filter_sessions)
    StatsViewModel.EventFilter.BLOCKS -> stringResource(R.string.stats_filter_blocks)
    StatsViewModel.EventFilter.BYPASSES -> stringResource(R.string.stats_filter_bypasses)
}

@Composable
private fun EventLogRow(event: app.focus.domain.model.EventLog) {
    val time = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(event.timestamp))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$time · ${event.type.name}", style = MaterialTheme.typography.bodyMedium)
        event.packageName?.let { pkg ->
            Text(pkg, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        event.payload?.let { payload ->
            Text(payload, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun StatSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.Equalizer,
                contentDescription = stringResource(R.string.stats_cd_kpi),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

private fun writeCsv(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(content.toByteArray())
    }
}

private const val EVENT_LOG_PREVIEW_LIMIT = 50
