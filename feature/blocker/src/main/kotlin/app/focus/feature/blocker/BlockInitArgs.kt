package app.focus.feature.blocker

import app.focus.domain.model.LockMode

data class BlockInitArgs(
    val sessionId: String,
    val profileId: String,
    val blockedPackage: String,
    val blockedAppName: String,
    val profileName: String,
    val remainingMillis: Long,
    val attemptNumber: Int,
    val lockMode: LockMode,
    val bypassesUsed: Int,
)
