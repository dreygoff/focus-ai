package app.focus.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FirstProfileCreatorScreen(
    viewModel: OnboardingViewModel,
    onCreated: () -> Unit,
    onSkip: () -> Unit = {},
) {
    val state by viewModel.firstProfileState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingStepIndicator(currentStep = 4)

        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(R.string.onboarding_first_profile_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.onboarding_first_profile_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.onboarding_first_profile_selected, state.selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { viewModel.finalizeDeepWorkProfile(onCreated) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.onboarding_create_deep_work))
            }
        }

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        ) {
            Text(stringResource(R.string.onboarding_skip_for_now))
        }
    }
}
