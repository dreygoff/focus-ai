package app.focus.feature.blocker

import app.focus.domain.model.BypassState
import app.focus.domain.model.EmergencyExitMode
import app.focus.domain.model.EmergencyExitStep
import app.focus.domain.model.LockMode
import app.focus.domain.model.SettingsShortcut

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
    val goalText: String? = null,
    val motivationalQuote: String? = null,
    val profileEmoji: String? = null,
    val allowedSettingsShortcuts: Set<SettingsShortcut> = emptySet(),
    val settingsShortcutIntentAction: String? = null,
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
    data class OpenSettingsShortcut(val shortcut: SettingsShortcut) : BlockAction
}
