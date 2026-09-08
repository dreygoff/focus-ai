package app.focus.feature.blocker

import app.focus.domain.model.BypassState
import app.focus.domain.model.LockMode

data class BlockUiState(
    val sessionId: String = "",
    val profileId: String = "",
    val blockedPackage: String = "",
    val appName: String = "",
    val profileName: String = "Focus",
    val remainingMillis: Long = 0L,
    val attemptNumber: Int = 1,
    val lockMode: LockMode = LockMode.Soft,
    val bypassesRemaining: Int = -1,
    val bypassLimitReached: Boolean = false,
    val bypassStep: BypassState? = null,
    val bypassGranted: Boolean = false,
)

sealed interface BlockAction {
    data object ReturnToWork : BlockAction
    data object OpenFocus : BlockAction
    data object StartBypass : BlockAction
    data object CancelBypass : BlockAction
    data class SubmitBypassReason(val text: String) : BlockAction
    data class InputPhraseChar(val char: Char) : BlockAction
}
