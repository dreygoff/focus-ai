package app.focus.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingPermissionsScreen(
    onAllGranted: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Text(
            "Enable permissions",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Focus needs several permissions to work properly. We'll guide you through them.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Button(
            onClick = onAllGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant all permissions")
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Maybe later")
        }
    }
}
