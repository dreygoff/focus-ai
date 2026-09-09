package app.focus.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
fun ActiveSessionScreen(
    session: app.focus.domain.model.Session,
    remainingSeconds: Int,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onOpenFocus: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            session.profileNameSnapshot,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        TimerDialBig(
            secondsRemaining = remainingSeconds,
            totalSeconds = (
                ((session.plannedEndAt ?: session.startedAt) - session.startedAt)
                    .coerceAtLeast(0) / 1000
                ).toInt(),
        )

        session.goalText?.let {
            Card(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (session.lockMode is app.focus.domain.model.LockMode.Soft) {
                Button(onClick = onPause) { Text(stringResource(R.string.session_pause)) }
                Button(onClick = onStop) { Text(stringResource(R.string.session_finish)) }
            } else {
                Text(
                    stringResource(R.string.session_hard_lock_no_pause),
                    color = Color.Red,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.session_blocked_attempts, session.blockAttempts),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TimerDialBig(secondsRemaining: Int, totalSeconds: Int) {
    val minutes = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val formatted = "%02d:%02d".format(minutes, secs)
    val timerLabel = stringResource(R.string.session_cd_timer)
    val timerDescription = stringResource(R.string.session_cd_timer_remaining, timerLabel, formatted)
    Text(
        formatted,
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.semantics {
            contentDescription = timerDescription
        },
    )
}

@Composable
fun SessionSummaryScreen(
    session: app.focus.domain.model.Session,
    currentStreakDays: Int = 0,
    remainingSessionMinutes: Int? = null,
    onBackToHome: () -> Unit,
) {
    val focusMinutes = sessionFocusMinutes(session)
    val streakDays = currentStreakDays

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            stringResource(R.string.session_complete_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(16.dp)) {
                if (focusMinutes > 0) {
                    Text(stringResource(R.string.session_summary_duration, focusMinutes))
                }
                Text(stringResource(R.string.session_blocked_attempts, session.blockAttempts))
                Text(stringResource(R.string.session_bypasses_used, session.bypassesUsed))
                Text(pluralStringResource(R.plurals.session_streak_days, streakDays, streakDays))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onBackToHome) {
            Text(stringResource(R.string.session_back_home))
        }
    }
}

private fun sessionFocusMinutes(session: app.focus.domain.model.Session): Int {
    val endAt = session.actualEndAt ?: session.plannedEndAt ?: return 0
    val durationMs = (endAt - session.startedAt).coerceAtLeast(0)
    return TimeUnit.MILLISECONDS.toMinutes(durationMs).toInt()
}
