package app.focus.domain.internal.blockdecision

import app.focus.domain.model.BlockDecision
import app.focus.domain.model.BlockReason

/**
 * BlockDecisionEngine per TR-06 — pure Kotlin engine for determining whether to block an app.
 * Rules evaluated by priority:
 * 1. No active session or in pomodoro break → Allow
 * 2. Package in system allowlist (FR-60) → Allow
 * 3. Package in user allowlist (FR-61) → Allow
 * 4. Active phone call → Allow for dialer package
 * 5. Active access window exists for package → Allow
 * 6. Package is target of session OR hard-lock extra → Block(reason)
 * 7. Otherwise → Allow
 */
class BlockDecisionEngine(
    private val systemAllowlist: Set<String>,
    private val userAllowlistProvider: () -> Set<String>,
    private val accessWindowLookup: (String) -> Map<String, Long>?,
    private val sessionState: SessionCheckState
) {

    data class SessionCheckState(
        val hasActiveSession: Boolean,
        val isHardLock: Boolean,
        val sessionId: String?,
        val targetPackages: Set<String>,
        val hardLockExtraPackages: Set<String> = emptySet(),
        val inPomodoroBreak: Boolean = false,
        val inCallPackage: String? = null // non-null while a phone call is active
    )

    fun decide(packageName: String): BlockDecision {
        // Rule 1: No active session or in pomodoro break → Allow
        if (!sessionState.hasActiveSession || sessionState.inPomodoroBreak) {
            return BlockDecision.Allow
        }

        // Rule 2: System allowlist (FR-60)
        if (packageName in systemAllowlist) {
            return BlockDecision.Allow
        }

        // Rule 3: User allowlist (FR-61)
        val userAllowlist = userAllowlistProvider()
        if (packageName in userAllowlist) {
            return BlockDecision.Allow
        }

        // Rule 4: Phone call is active — allow the dialer package
        sessionState.inCallPackage?.let { inCallPkg ->
            if (packageName == inCallPkg) {
                return BlockDecision.Allow
            }
        }

        // Rule 5: Active access window for this package
        val windows = accessWindowLookup(sessionState.sessionId ?: "")
        val now = System.currentTimeMillis()
        if (!windows.isNullOrEmpty()) {
            val pkgExpireTime = windows[packageName]
            if (pkgExpireTime != null && now <= pkgExpireTime) {
                return BlockDecision.Allow
            }
        }

        // Rule 6: Target app of active session OR hard-lock extra package
        val isTarget = packageName in sessionState.targetPackages
        val isHardLockExtra = if (sessionState.isHardLock) {
            packageName in sessionState.hardLockExtraPackages
        } else false

        if (isTarget || isHardLockExtra) {
            return BlockDecision.Block(
                reason = if (isHardLockExtra && !isTarget) {
                    BlockReason.HARD_LOCK_EXTRA
                } else {
                    BlockReason.TARGET_APP
                },
                packageName = packageName
            )
        }

        // Rule 7: Otherwise → Allow
        return BlockDecision.Allow
    }

    private fun Map<*, *>?.isNullOrEmpty(): Boolean = this.isNullOrEmpty()
}
