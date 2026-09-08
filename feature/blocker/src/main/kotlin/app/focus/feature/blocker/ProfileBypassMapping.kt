package app.focus.feature.blocker

import app.focus.domain.model.Profile
import app.focus.domain.model.bypassflow.BypassFlowConfig

internal fun Profile.toBypassFlowConfig(): BypassFlowConfig = BypassFlowConfig(
    delayEnabled = bypassDelaySeconds > 0,
    delaySeconds = if (bypassDelaySeconds > 0) bypassDelaySeconds else 30,
    breathingEnabled = bypassBreathingEnabled,
    reasonRequired = bypassReasonRequired,
    reasonMinLength = 10,
    phraseRequired = !bypassPhrase.isNullOrBlank(),
    phraseText = bypassPhrase,
    bypassLimitPerSession = bypassLimitPerSession,
    accessWindowMinutes = accessWindowMinutes,
)

internal fun bypassesRemaining(profile: Profile, bypassesUsed: Int): Int {
    if (profile.bypassLimitPerSession < 0) return -1
    return (profile.bypassLimitPerSession - bypassesUsed).coerceAtLeast(0)
}
