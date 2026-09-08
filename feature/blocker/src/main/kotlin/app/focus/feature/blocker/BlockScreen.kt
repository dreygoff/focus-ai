package app.focus.feature.blocker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BlockScreen(
    blockedPackage: String,
    appName: String,
    profileName: String,
    onReturnToWork: () -> Unit = {},
    onBypassAttempt: (reason: String) -> Boolean = { false }
) {
    var bypassText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        contentColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.weight(1f))

            // Icon and title
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Red
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "App blocked",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                appName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "$profileName session active",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(24.dp))

            // Return to work button
            Button(
                onClick = onReturnToWork,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to work")
            }

            // Bypass attempt section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val granted = onBypassAttempt(bypassText)
                        if (granted) {
                            // Bypass granted, clear text
                            bypassText = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I really need this...")
                }

                OutlinedTextField(
                    value = bypassText,
                    onValueChange = { bypassText = it },
                    label = { Text("Enter reason (min 10 chars):") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
