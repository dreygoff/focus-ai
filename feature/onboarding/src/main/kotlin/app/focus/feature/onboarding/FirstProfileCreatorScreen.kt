package app.focus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FirstProfileCreatorScreen(
    onCreated: () -> Unit,
    onSkip: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            "Create your first profile",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "A profile groups blocking rules. Start with \"Deep Work\" or customize your own.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Button(
            onClick = onCreated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Deep Work profile")
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}
