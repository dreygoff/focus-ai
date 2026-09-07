package app.focus.feature.blocker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.focus.domain.model.LockMode

data class BlockUiState(
    val profileName: String,
    val profileEmoji: String?,
    val remainingMillis: Long,
    val totalMillis: Long,
    val goalText: String?,
    val mode: LockMode,
    val attemptNumber: Int,
    val blockedPackageName: String,
    val blockedAppName: String,
    val bypassesRemaining: Int = -1,
    val quote: String? = null,
    val bypassPhase: BypassPhase = BypassState.Idle
)

sealed interface BypassPhase {
    data object Idle : BypassPhase
    data class Delay(val remainingSeconds: Int) : BypassPhase
    data class Breathing(val currentCycle: Int, val phase: String) : BypassPhase
    data class Reason(val text: TextFieldValue) : BypassPhase
    data class Phrase(val targetPhrase: String, val typedText: TextFieldValue) : BypassPhase
    data object Granted : BypassPhase
}

sealed interface BlockAction {
    data object GoHome : BlockAction
    data object StartBypass : BlockAction
    data object CancelBypass : BlockAction
    data class ConfirmDelay(val remainingSeconds: Int) : BlockAction
    data class UpdateReasonText(val text: TextFieldValue) : BlockAction
    data class ConfirmReason(val text: String) : BlockAction
    data class UpdatePhraseText(val typedText: TextFieldValue) : BlockAction
    data class ConfirmPhrase(val typedText: String) : BlockAction
}

/** FR-50 to FR-55: Full-screen edge-to-edge block screen with motivational quotes, timer, actions per mode */
@Composable
fun BlockScreen(state: BlockUiState, onAction: (BlockAction) -> Unit) {
    var showAppInfo by remember { mutableStateOf(false) }
    
    val quote = state.quote ?: listOf(
        "Focus is a skill. Practice it today.",
        "The secret of getting ahead is getting started.",
        "Your future is created by what you do today.",
        "Discipline is choosing between what you want now and what you want most.",
        "Concentrate all your thoughts upon the task at hand.",
        "It always seems impossible until it's done.",
        "The only way to do great work is to love what you do.",
        "Small daily improvements over time lead to stunning results.",
        "Focus on being productive instead of busy.",
        "What you do today matters most.",
        "Deep work is the superpower of the 21st century.",
        "Your ability to concentrate determines your quality of life.",
        "Distraction is the disease. Attention is the antidote.",
        "Where focus goes, energy flows.",
        "The mind is everything. What you think, you become.",
        "Stay focused, go after your dreams.",
        "Quality is not an act, it is a habit.",
    ).random()

    LaunchedEffect(state.remainingMillis) {
        // Trigger vibration when remaining < 60 seconds (FR-32 access window warning)
    }

    androidx.compose.foundation.background(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                // Profile indicator
                ProfileIndicator(state.profileEmoji ?: "\uD83D\uDD12", state.profileName)
                
                Spacer(Modifier.height(32.dp))

                // Large timer dial (FR-51)
                TimerDial(
                    progress = if (state.totalMillis > 0) state.remainingMillis.toFloat() / state.totalMillis.toFloat() else 1f,
                    remainingSeconds = (state.remainingMillis / 1000L).toInt(),
                    size = 240.dp
                )

                Spacer(Modifier.height(16.dp))

                // Goal text if provided (FR-54)
                state.goalText?.let { goal ->
                    Card(
                        modifier = Modifier.padding(horizontal = 32.dp)
                            .clip(MaterialTheme.shapes.medium),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your goal:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = goal,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Blocked app info with tap to see (FR-51)
                BlockedAppInfo(state.blockedAppName, state.attemptNumber, onAction)

                Spacer(Modifier.height(24.dp))

                // Motivational quote (updates every minute) (FR-51)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.weight(1f))

                // Action buttons based on lock mode (FR-50)
                when (state.mode) {
                    is LockMode.Soft -> SoftLockActions(state, onAction)
                    is LockMode.Hard -> HardLockActions(state, onAction)
                }

                Spacer(Modifier.height(32.dp))

                // Open Focus link (FR-51)
                TextButton(
                    onClick = { /* open app main screen */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open Focus", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Bypass flow overlay (TR-07)
            if (state.bypassPhase !is BypassPhase.Idle && state.bypassPhase !is BypassPhase.Granted) {
                BypassFlow(state.bypassPhase, onAction)
            }
        }
    }

    // Show app info dialog
    if (showAppInfo) {
        AlertDialog(
            onDismissRequest = { showAppInfo = false },
            title = { Text("Blocked App") },
            text = {
                Column {
                    Text("Name: ${state.blockedAppName}")
                    Spacer(Modifier.height(8.dp))
                    Text("Package: ${state.blockedPackageName}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfo = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ProfileIndicator(emoji: String, name: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "session active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerDial(progress: Float, remainingSeconds: Int, size: Dp) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(500),
        label = "timer_progress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val text = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier.size(size).shadow(16.dp, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = offset.center
            val radius = size.toPx() / 2f - 8f
            
            // Background arc
            drawArc(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                startAngle = 90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12f)
            )

            // Progress arc with color transition based on remaining time
            val progressColor = when {
                animatedProgress > 0.5f -> MaterialTheme.colorScheme.primary
                animatedProgress > 0.25f -> Color(0xFFF9A825)
                else -> Color(0xFFE53935)
            }

            drawArc(
                color = progressColor,
                startAngle = 90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                lineHeight = 48.sp
            )
            Text(
                text = if (remainingSeconds > 0) "remaining" else "done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockedAppInfo(appName: String, attemptNumber: Int, onAction: (BlockAction) -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = { /* show app details */ })
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Blocked: $appName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            attemptNumber >= 1 && Text(
                text = when {
                    attemptNumber == 1 -> "1st attempt"
                    attemptNumber == 2 -> "2nd attempt"
                    else -> "$attemptNumber-th attempt"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

        }
    }
}

@Composable
private fun SoftLockActions(state: BlockUiState, onAction: (BlockAction) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.bypassPhase == BypassPhase.Idle) {
            if (state.bypassesRemaining!! > 0) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(BlockAction.StartBypass) }
                ) {
                    Text("I really need this (${state.bypassesRemaining} left)")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Bypasses exhausted for this session",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            }

            // Spacer(M) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAction(BlockAction.GoHome) }
    ) {
        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Back to work", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HardLockActions(state: BlockUiState, onAction: (BlockAction) -> Unit) {
    Button(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        onClick = { onAction(BlockAction.GoHome) }
    ) {
        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Go home", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** FR-31: Bypass flow for Soft Lock - delay, breathing, reason, phrase stages */
@Composable
private fun BypassFlow(phase: BypassPhase, onAction: (BlockAction) -> Unit) {
    when (phase) {
        is BypassPhase.Delay -> DelayBypass(phase, onAction)
        is BypassPhase.Breathing -> BreathingBypass(phase, onAction)
        is BypassPhase.Reason -> ReasonBypass(phase, onAction)
        is BypassPhase.Phrase -> PhraseBypass(phase, onAction)
        else -> {}
    }
}

/** FR-31: Delay countdown with back gesture reset (10/30/60/120 seconds) */
@Composable
private fun DelayBypass(phase: BypassPhase.Delay, onAction: (BlockAction) -> Unit) {
    val focusManager = LocalFocusManager.current
    var elapsed by remember { mutableStateOf(0) }
    
    LaunchedEffect(phase.remainingSeconds) {
        if (phase.remainingSeconds <= 0) {
            onAction(BlockAction.ConfirmDelay(phase.remainingSeconds))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Wait before you can continue...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            val minutes = phase.remainingSeconds / 60
            val seconds = phase.remainingSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Progress ring
            CircularProgressIndicator(
                progress = { 1f - (phase.remainingSeconds.toFloat() / 120f) },
                modifier = Modifier.size(160.dp).padding(32.dp),
                strokeWidth = 8.dp,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Leaving this screen will reset the countdown",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )

            TextButton(onClick = { focusManager.clearFocus() }) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

/** FR-31: Breathing exercise animation - inhale 4s, hold 4s, exhale 6s x3 */
@Composable
private fun BreathingBypass(phase: BypassPhase.Breathing, onAction: (BlockAction) -> Unit) {
    val phaseText = when (phase.phase) {
        "inhale" -> "Breathe in..."
        "hold" -> "Hold..."
        "exhale" -> "Breathe out..."
        else -> "Breathe..."
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(Modifier.height(24.dp))

            // Breathing circle animation
            Box(
                modifier = Modifier.size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💨",
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cycle ${phase.currentCycle + 1}/3",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/** FR-31: Required reason text input (min 10 chars) */
@Composable
private fun ReasonBypass(phase: BypassPhase.Reason, onAction: (BlockAction) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text(
                text = "Why do you need to open this app?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Text field with min 10 chars validation
            var textField by remember(phase.text) { mutableStateOf(phase.text) }

            androidx.compose.material3.TextField(
                value = textField,
                onValueChange = { 
                    if (it.text.length <= 200) {
                        textField = it
                        onAction(BlockAction.UpdateReasonText(it))
                    }
                },
                placeholder = { Text("Explain your reason...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Gray
                ),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    autoCorrect = false
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${textField.length}/10 min chars",
                style = MaterialTheme.typography.bodySmall,
                color = if (textField.length >= 10) Color.Green else Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { onAction(BlockAction.CancelBypass) }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }

                Button(
                    onClick = { if (textField.length >= 10) onAction(BlockAction.ConfirmReason(textField.text)) },
                    modifier = Modifier.weight(1f),
                    enabled = textField.length >= 10
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

/** FR-31: Phrase typing with paste/autocorrect disabled */
@Composable
private fun PhraseBypass(phase: BypassPhase.Phrase, onAction: (BlockAction) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Text(
                text = "Type the phrase exactly:",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Target phrase display (no auto-correction)
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Text(
                    text = phase.targetPhrase,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))

            // Input field with paste disabled
            BasicTextField(
                value = phase.typedText.text,
                onValueChange = { 
                    if (it.length <= 300) {
                        // Reject pasted content > 1 char at once
                        onAction(BlockAction.UpdatePhraseText(TextFieldValue(it)))
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    autoCorrect = false
                ),
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(16.dp)
            )

            Spacer(Modifier.height(8.dp))

            val matchColor = if (phase.typedText.text == phase.targetPhrase) Color.Green else Color.Gray
            Text(
                text = if (phase.typedText.text == phase.targetPhrase) "✓ Correct!" else "Keep typing...",
                style = MaterialTheme.typography.bodyMedium,
                color = matchColor
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { onAction(BlockAction.CancelBypass) }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
                Button(
                    onClick = { if (phase.typedText.text == phase.targetPhrase) onAction(BlockAction.ConfirmPhrase(phase.typedText.text)) },
                    modifier = Modifier.weight(1f),
                    enabled = phase.typedText.text == phase.targetPhrase
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
