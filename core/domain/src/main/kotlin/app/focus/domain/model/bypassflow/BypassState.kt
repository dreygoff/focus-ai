package app.focus.domain.model

sealed interface BypassState {
    data object Idle : BypassState
    data class Delay(val remainingSeconds: Int, val totalTimeSeconds: Int) : BypassState
    data class Breathing(val currentCycle: Int, val phaseInCycle: BreathingPhase, val timeLeftInPhase: Int) : BypassState
    data class Reason(val text: String, val minLength: Int = 0) : BypassState
    data class Phrase(val typedText: String, val targetPhrase: String, val characterIndex: Int) : BypassState
    data object Granted : BypassState
    data object Denied : BypassState
    data object LimitReached : BypassState
}

enum class BreathingPhase { INHALE, HOLD, EXHALE }
