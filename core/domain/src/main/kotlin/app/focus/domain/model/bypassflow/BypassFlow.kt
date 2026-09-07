package app.focus.domain.model.bypassflow

import app.focus.domain.model.BreathingPhase
import app.focus.domain.model.BypassState

/**
 * Finite State Machine for the bypass flow (TR-07).
 * Manages anti-tantrum friction: delay → breathing → reason → phrase → granted.
 */
class BypassFlow(
    private val config: BypassFlowConfig,
    private val packageName: String,
    initialBypassesUsed: Int = 0
) {
    private var _state: BypassState = BypassState.Idle
    private var bypassesUsed: Int = initialBypassesUsed

    // Internal state for delay
    private var delayRemaining: Int = 0
    private var delayTotalSeconds: Int = 0

    // Internal state for breathing
    private var breathingCurrentCycle: Int = 0
    private var breathingPhaseInCycle: BreathingPhase = BreathingPhase.INHALE
    private var breathingTimeLeftInMs: Long = 0
    private val phaseDurations = longArrayOf(4000L, 4000L, 6000L) // inhale, hold, exhale

    // Internal state for phrase
    private var phraseTarget: String = ""
    private var phraseEntered: String = ""

    /** The currently active bypass state. */
    fun getState(): BypassState = _state

    /** Whether the bypass flow is currently in progress (waiting on delay, breathing, etc.). */
    fun isInProgress(): Boolean {
        return _state !is BypassState.Idle &&
                _state !is BypassState.Granted &&
                _state !is BypassState.Denied
    }

    /** Start the bypass flow. Returns the initial state. */
    fun start(): BypassState {
        // Check limit first
        if (config.bypassLimitPerSession >= 0 && bypassesUsed >= config.bypassLimitPerSession) {
            _state = BypassState.Denied
            return _state
        }

        when {
            config.delayEnabled -> {
                delayRemaining = config.delaySeconds
                delayTotalSeconds = config.delaySeconds
                _state = BypassState.Delay(delayRemaining, delayTotalSeconds)
            }
            config.breathingEnabled && config.reasonRequired -> {
                breathingCurrentCycle = 0
                breathingPhaseInCycle = BreathingPhase.INHALE
                breathingTimeLeftInMs = phaseDurations[0]
                _state = BypassState.Breathing(breathingCurrentCycle, breathingPhaseInCycle, breathingTimeLeftInMs.toInt())
            }
            config.breathingEnabled && !config.reasonRequired -> {
                breathingCurrentCycle = 0
                breathingPhaseInCycle = BreathingPhase.INHALE
                breathingTimeLeftInMs = phaseDurations[0]
                _state = BypassState.Breathing(breathingCurrentCycle, breathingPhaseInCycle, breathingTimeLeftInMs.toInt())
            }
            !config.breathingEnabled && config.reasonRequired -> {
                _state = BypassState.Reason("", config.reasonMinLength)
            }
            else -> {
                _state = BypassState.Granted
            }
        }

        return _state
    }

    /** Tick the delay timer by 1 second. Returns new state or null if not in delay. */
    fun tickDelay(): BypassState? {
        return when (_state) {
            is BypassState.Delay -> {
                delayRemaining--
                if (delayRemaining <= 0) {
                    // Delay complete, move to next phase
                    if (config.breathingEnabled && config.reasonRequired) {
                        breathingCurrentCycle = 0
                        breathingPhaseInCycle = BreathingPhase.INHALE
                        breathingTimeLeftInMs = phaseDurations[0]
                        _state = BypassState.Breathing(breathingCurrentCycle, breathingPhaseInCycle, breathingTimeLeftInMs.toInt())
                    } else if (config.reasonRequired) {
                        _state = BypassState.Reason("", config.reasonMinLength)
                    } else {
                        _state = BypassState.Granted
                    }
                    _state
                } else {
                    _state = BypassState.Delay(delayRemaining, delayTotalSeconds)
                    _state
                }
            }
            else -> null
        }
    }

    /** Tick the breathing timer by deltaMs. Returns true when all cycles complete. */
    fun tickBreathing(deltaMs: Long): Boolean {
        return when (_state) {
            is BypassState.Breathing -> {
                var remaining = deltaMs
                while (remaining > 0) {
                    breathingTimeLeftInMs -= remaining.coerceAtMost(breathingTimeLeftInMs)
                    if (breathingTimeLeftInMs <= 0) {
                        // Move to next phase
                        val phaseIndex = breathingPhaseInCycle.ordinal + 1
                        if (phaseIndex < phaseDurations.size) {
                            breathingPhaseInCycle = BreathingPhase.entries[phaseIndex]
                            breathingTimeLeftInMs = phaseDurations[phaseIndex]
                        } else {
                            // Phase complete, move to next cycle or finish
                            breathingCurrentCycle++
                            if (breathingCurrentCycle >= config.breathingCycles) {
                                // All cycles complete
                                if (config.reasonRequired) {
                                    _state = BypassState.Reason("", config.reasonMinLength)
                                } else {
                                    _state = BypassState.Granted
                                }
                                return true
                            } else {
                                breathingPhaseInCycle = BreathingPhase.INHALE
                                breathingTimeLeftInMs = phaseDurations[0]
                            }
                        }
                    }
                    remaining = 0
                }

                _state = BypassState.Breathing(breathingCurrentCycle, breathingPhaseInCycle, breathingTimeLeftInMs.toInt())
                false
            }
            else -> false
        }
    }

    /** Submit a typed reason. Returns true if valid and bypass is granted. */
    fun submitReason(text: String): Boolean {
        return when (_state) {
            is BypassState.Reason -> {
                if (text.length < config.reasonMinLength) {
                    _state = BypassState.Reason(text, config.reasonMinLength)
                    false
                } else if (config.phraseRequired && config.phraseText != null) {
                    phraseTarget = config.phraseText
                    phraseEntered = ""
                    _state = BypassState.Phrase("", phraseTarget, 0)
                    true
                } else {
                    bypassesUsed++
                    _state = BypassState.Granted
                    true
                }
            }
            else -> false
        }
    }

    /** Input a character for the phrase. Returns new state or null. */
    fun inputPhraseCharacter(char: Char): BypassState? {
        return when (_state) {
            is BypassState.Phrase -> {
                val currentIndex = (state as BypassState.Phrase).characterIndex
                if (currentIndex < phraseTarget.length) {
                    if (char == phraseTarget[currentIndex].lowercaseChar()) {
                        // Correct character
                        phraseEntered += char
                        if (phraseEntered.length == phraseTarget.length) {
                            // Phrase complete
                            bypassesUsed++
                            _state = BypassState.Granted
                            _state
                        } else {
                            _state = BypassState.Phrase(phraseEntered, phraseTarget, phraseEntered.length)
                            _state
                        }
                    } else {
                        // Wrong character - reset to idle (anti-tantrum)
                        _state = BypassState.Idle
                        _state
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /** Check if bypass limit allows another attempt. */
    fun checkBypassLimit(): Boolean {
        return config.bypassLimitPerSession < 0 || bypassesUsed < config.bypassLimitPerSession
    }

    /** Get the access window end time if currently granted, null otherwise. */
    fun getAccessWindowEndAt(): Long? {
        return when (_state) {
            is BypassState.Granted -> System.currentTimeMillis() + config.accessWindowMinutes * 60_000L
            else -> null
        }
    }

    /** Reset to idle state (e.g., user left screen). */
    fun resetToIdle(): BypassState {
        _state = BypassState.Idle
        delayRemaining = 0
        breathingCurrentCycle = 0
        phraseEntered = ""
        return _state
    }
}
