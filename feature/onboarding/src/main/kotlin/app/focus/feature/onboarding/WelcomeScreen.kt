package app.focus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            "Focus",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            "Your offline digital wellbeing companion. No internet, no analytics — everything stays on your device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get started")
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip")
        }
    }
}
