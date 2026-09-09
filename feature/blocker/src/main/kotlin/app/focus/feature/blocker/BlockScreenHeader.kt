package app.focus.feature.blocker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

@Composable
internal fun BlockHeader(state: BlockUiState) {
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(state.remainingMillis.coerceAtLeast(0))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Block,
            contentDescription = stringResource(R.string.block_cd_blocked),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.block_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        )
        BlockHeaderDetails(state, remainingMinutes)
        if (state.tamperMessage != null) {
            Text(
                state.tamperMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BlockHeaderDetails(state: BlockUiState, remainingMinutes: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        state.profileEmoji?.let { emoji ->
            Text(emoji, style = MaterialTheme.typography.displaySmall)
        }
        Text(state.appName, style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.block_profile_active, state.profileName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.goalText?.let { goal ->
            Spacer(Modifier.height(12.dp))
            Text(
                goal,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.motivationalQuote != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.motivationalQuote,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.remainingMillis > 0) {
            Text(
                stringResource(R.string.block_time_remaining, remainingMinutes),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (state.attemptNumber > 0) {
            Text(
                stringResource(R.string.block_attempt_count, state.attemptNumber),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
