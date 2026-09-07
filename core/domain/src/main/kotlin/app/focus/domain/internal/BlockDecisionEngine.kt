package app.focus.domain.internal.blockdecision

import app.focus.domain.model.BlockDecision
import app.focus.domain.model.LockMode

/**
 * Determines whether a package should be allowed or blocked during a focus session.
 */
class BlockDecisionEngine(
    private val systemAllowlist: Set<String>,
    private val userAllowlist: Set<String>,
    private val accessWindowPkgLookup: Map<String, Long>,
) {

    fun decide(packageName: String, hasActiveSession: Boolean): BlockDecision {
        if (!hasActiveSession) {
            return BlockDecision.Allow
        }

        if (packageName in systemAllowlist) {
            return BlockDecision.Allow
        }

        if (userAllowlist.contains(packageName)) {
            return BlockDecision.Allow
        }

        val now = System.currentTimeMillis()
        for ((pkg, endTime) in accessWindowPkgLookup) {
            if (pkg == packageName && now <= endTime) {
                return BlockDecision.Allow
            }
        }

        return BlockDecision.Block(reason = app.focus.domain.model.BlockReason.TARGET_APP)
    }
}
