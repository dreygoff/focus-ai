package app.focus.domain.model

sealed interface EmergencyExitStep {
    data class Delay(val untilMillis: Long, val remainingMillis: Long) : EmergencyExitStep
    data class Retype(val targetText: String, val typedText: String = "") : EmergencyExitStep
}
