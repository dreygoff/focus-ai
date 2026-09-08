package app.focus.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ActiveSessionScreen(
    session: app.focus.domain.model.Session,
    remainingSeconds: Int,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onOpenFocus: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("${session.profileNameSnapshot}", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        TimerDialBig(
            secondsRemaining = remainingSeconds,
            totalSeconds = (
                ((session.plannedEndAt ?: session.startedAt) - session.startedAt)
                    .coerceAtLeast(0) / 1000
                ).toInt(),
        )
        
        session.goalText?.let { 
            Card(modifier = Modifier.padding(vertical = 8.dp)) { 
                Text(it, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp)) 
            } 
        }
        
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (session.lockMode is app.focus.domain.model.LockMode.Soft) {
                Button(onClick = onPause) { Text("Pause") }
                Button(onClick = onStop) { Text("Finish") }
            } else {
                Text("Hard Lock - no pause available", color = androidx.compose.ui.graphics.Color.Red)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        Text("Blocked attempts: ${session.blockAttempts}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TimerDialBig(secondsRemaining: Int, totalSeconds: Int) {
    val minutes = secondsRemaining / 60
    val secs = secondsRemaining % 60
    Text("${String.format("%02d:%02d", minutes, secs)}", 
        style = androidx.compose.material3.MaterialTheme.typography.displayMedium)
}

@Composable
fun SessionSummaryScreen(
    session: app.focus.domain.model.Session,
    remainingSessionMinutes: Int? = null,
    onBackToHome: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Session Complete!", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(Modifier.padding(16.dp)) {
                session.plannedEndAt?.let { endAt ->
                    Text("Duration: ${endAt - session.startedAt}ms")
                }
                Text("Blocked attempts: ${session.blockAttempts}")
                Text("Bypasses used: ${session.bypassesUsed}")
                Text("Streak: ${if (session.actualEndAt != null) 7 else 0} days")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBackToHome) { Text("Back to Home") }
    }
}

object SessionRoutes {
    const val START = "session/start"
    const val ACTIVE = "session/active/{sessionId}"
    const val SUMMARY = "session/summary/{sessionId}"
    const val POMODORO_CONFIG = "$START/pomodoro"
    
    fun activeRoute(sessionId: String) = "session/active/$sessionId"
    fun summaryRoute(sessionId: String) = "session/summary/$sessionId"
}
