package app.focus.domain.model.bypassflow

/**
 * Configuration for a bypass attempt in the BypassFlow FSM.
 */
data class BypassFlowConfig(
    /** Enable delay before proceeding */
    val delayEnabled: Boolean = true,
    /** Minimum delay in seconds before user can proceed */
    val delaySeconds: Int = 30,
    /** Enable breathing animation cycle */
    val breathingEnabled: Boolean = false,
    /** Breathing cycle duration in milliseconds per phase (inhale/exhale) */
    val breathingCycleMs: Long = 14000L, // inhale(4s)+hold(4s)+exhale(6s) = 14s
    /** Number of complete cycles before proceeding */
    val breathingCycles: Int = 3,
    /** Require user to type a reason */
    val reasonRequired: Boolean = false,
    /** Minimum length for typed reason */
    val reasonMinLength: Int = 5,
    /** Require typing a specific phrase (anti-tantrum) */
    val phraseRequired: Boolean = false,
    /** The target phrase user must type correctly */
    val phraseText: String? = null,
    /** Maximum number of bypass attempts allowed per session. -1 for unlimited. */
    val bypassLimitPerSession: Int = 3,
    /** Access window duration in minutes if bypass is granted */
    val accessWindowMinutes: Int = 5,
) {
    fun hasLimit(): Boolean = bypassLimitPerSession >= 0

    companion object {
        val Default = BypassFlowConfig()
    }
}
