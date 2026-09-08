package app.focus.feature.blocker

import app.focus.domain.model.BypassState
import app.focus.domain.model.EmergencyExitMode
import app.focus.domain.model.EmergencyExitStep
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
    val bypassStep: app.focus.domain.model.BypassState? = null,
    val bypassGranted: Boolean = false,
    val tamperMessage: String? = null,
    val emergencyExitMode: EmergencyExitMode = EmergencyExitMode.NONE,
    val emergencyExitStep: EmergencyExitStep? = null,
    val sessionEnded: Boolean = false,
)

sealed interface BlockAction {
    data object ReturnToWork : BlockAction
    data object OpenFocus : BlockAction
    data object StartBypass : BlockAction
    data object CancelBypass : BlockAction
    data class SubmitBypassReason(val text: String) : BlockAction
    data class InputPhraseChar(val char: Char) : BlockAction
    data object StartEmergencyExit : BlockAction
    data object CancelEmergencyExit : BlockAction
    data class InputEmergencyExitChar(val char: Char) : BlockAction
}
