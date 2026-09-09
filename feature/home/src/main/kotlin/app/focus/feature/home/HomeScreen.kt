package app.focus.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class HomeUiState(
    val activeSession: app.focus.domain.model.Session? = null,
    val pomodoroPhase: String? = null,
    val phaseEndAtMillis: Long? = null,
    val profiles: List<app.focus.domain.model.Profile> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val streakDays: Int = 0,
)

data class HomeScreenCallbacks(
    val onStartSession: (profileId: String?, durationMinutes: Int) -> Unit,
    val onStartPomodoro: (profileId: String?) -> Unit = {},
    val onPauseSession: () -> Unit,
    val onStopSession: () -> Unit,
    val onNavigateToProfiles: () -> Unit,
)

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    callbacks: HomeScreenCallbacks,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(uiState.profiles.firstOrNull()?.colorArgb ?: 0xFF2F6F6D.toInt()),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(onClick = callbacks.onNavigateToProfiles) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.home_cd_profiles),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = stringResource(R.string.home_stat_today),
                    value = uiState.todayFocusMinutes.toString(),
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = stringResource(R.string.home_stat_streak),
                    value = stringResource(R.string.home_streak_days, uiState.streakDays),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.activeSession != null &&
                (uiState.activeSession.status is app.focus.domain.model.SessionStatus.Running ||
                    uiState.activeSession.status is app.focus.domain.model.SessionStatus.Paused)
            ) {
                val isPaused = uiState.activeSession.status is app.focus.domain.model.SessionStatus.Paused
                ActiveSessionCard(
                    ActiveSessionCardState(
                        sessionId = uiState.activeSession.id,
                        profileId = uiState.activeSession.profileId,
                        startedAt = uiState.activeSession.startedAt,
                        plannedEndAt = uiState.activeSession.plannedEndAt,
                        isPaused = isPaused,
                        pomodoroPhase = uiState.pomodoroPhase,
                        phaseEndAtMillis = uiState.phaseEndAtMillis,
                    ),
                    onPause = callbacks.onPauseSession,
                    onStop = callbacks.onStopSession,
                )
            } else {
                StartSessionCard(
                    profiles = uiState.profiles,
                    onQuickStart = { duration -> callbacks.onStartSession(null, duration) },
                    onSelectProfile = { profileId -> callbacks.onStartSession(profileId, 25) },
                    onStartPomodoro = { profileId ->
                        val resolved = profileId ?: uiState.profiles.firstOrNull()?.id
                        if (resolved != null) callbacks.onStartPomodoro(resolved)
                    },
                )
            }

            if (uiState.profiles.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.home_quick_start),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    uiState.profiles.take(3).forEach { profile ->
                        QuickStartButton(
                            name = profile.name,
                            emoji = profile.emoji ?: "🎯",
                            onClick = { callbacks.onStartSession(profile.id, profile.defaultDurationMinutes) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

private data class ActiveSessionCardState(
    val sessionId: String,
    val profileId: String?,
    val startedAt: Long,
    val plannedEndAt: Long?,
    val isPaused: Boolean,
    val pomodoroPhase: String? = null,
    val phaseEndAtMillis: Long? = null,
)

private fun formatTimerSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return "%02d:%02d".format(minutes, secs)
}

private fun sessionEndAtMillis(state: ActiveSessionCardState): Long? =
    state.phaseEndAtMillis?.takeIf { it > 0L } ?: state.plannedEndAt

private fun sessionRemainingSeconds(state: ActiveSessionCardState, nowMillis: Long): Int? {
    val endAt = sessionEndAtMillis(state) ?: return null
    return ((endAt - nowMillis).coerceAtLeast(0L) / 1000L).toInt()
}

private fun sessionProgress(state: ActiveSessionCardState, nowMillis: Long): Float {
    val endAt = sessionEndAtMillis(state) ?: return 0f
    val totalMs = (endAt - state.startedAt).coerceAtLeast(1L)
    val remainingMs = (endAt - nowMillis).coerceAtLeast(0L)
    return (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

@Composable
private fun pomodoroPhaseLabel(phase: String?): String? = when (phase) {
    "FOCUS" -> stringResource(R.string.home_pomodoro_focus)
    "SHORT_BREAK" -> stringResource(R.string.home_pomodoro_short_break)
    "LONG_BREAK" -> stringResource(R.string.home_pomodoro_long_break)
    "BREAK" -> stringResource(R.string.home_pomodoro_break)
    else -> null
}

@Composable
private fun ActiveSessionCard(
    state: ActiveSessionCardState,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    var tick by remember(state.sessionId) { mutableIntStateOf(0) }
    LaunchedEffect(state.sessionId, state.isPaused, state.phaseEndAtMillis, state.plannedEndAt) {
        while (!state.isPaused) {
            delay(1_000)
            tick++
        }
    }
    val nowMillis = remember(tick, state.isPaused) { System.currentTimeMillis() }
    val remainingSeconds = sessionRemainingSeconds(state, nowMillis)
    val displayTime = remainingSeconds?.let(::formatTimerSeconds) ?: "—"
    val timerDescription = if (state.isPaused && remainingSeconds != null) {
        stringResource(R.string.home_cd_session_timer_paused, formatTimerSeconds(remainingSeconds))
    } else if (remainingSeconds != null) {
        stringResource(R.string.home_cd_session_timer, formatTimerSeconds(remainingSeconds))
    } else {
        stringResource(R.string.home_cd_session_no_limit)
    }
    val progress = sessionProgress(state, nowMillis)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (state.isPaused) {
                    stringResource(R.string.home_session_paused)
                } else {
                    stringResource(R.string.home_session_active)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            pomodoroPhaseLabel(state.pomodoroPhase)?.let { phaseLabel ->
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.home_pomodoro_phase, phaseLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = timerDescription },
                contentAlignment = Alignment.Center,
            ) {
                if (remainingSeconds != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                    )
                }
                Text(
                    displayTime,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPause) {
                    Text(
                        if (state.isPaused) {
                            stringResource(R.string.home_resume)
                        } else {
                            stringResource(R.string.home_pause)
                        },
                    )
                }
                OutlinedButton(onClick = onStop) {
                    Text(stringResource(R.string.home_stop))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartSessionCard(
    profiles: List<app.focus.domain.model.Profile>,
    onQuickStart: (Int) -> Unit,
    onSelectProfile: (String) -> Unit,
    onStartPomodoro: (String?) -> Unit,
) {
    val useStackedDurations = LocalConfiguration.current.fontScale >= LARGE_FONT_SCALE_THRESHOLD
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.home_ready), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            val durations = listOf(15, 25, 45, 60)
            if (useStackedDurations) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    durations.forEach { duration ->
                        Button(onClick = { onQuickStart(duration) }) {
                            Text(stringResource(R.string.home_duration_min, duration))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    durations.forEach { duration ->
                        Button(
                            onClick = { onQuickStart(duration) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.home_duration_min, duration))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onStartPomodoro(profiles.firstOrNull()?.id) },
                modifier = Modifier.fillMaxWidth(),
                enabled = profiles.isNotEmpty(),
            ) {
                Text(stringResource(R.string.home_pomodoro_start))
            }

            if (profiles.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.home_select_profile), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(profile.emoji ?: "🎯", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                        Text(
                            " ${profile.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { onSelectProfile(profile.id) }) {
                            Text(stringResource(R.string.home_start))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStartButton(
    name: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.home_quick_start_profile, "$emoji $name")
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = label },
    ) {
        Text(label)
    }
}

private const val LARGE_FONT_SCALE_THRESHOLD = 1.3f
