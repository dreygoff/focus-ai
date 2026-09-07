package app.focus.domain.model

sealed interface BlockDecision {
    data object Allow : BlockDecision
    data class Block(
        val reason: BlockReason = BlockReason.TARGET_APP,
        val packageName: String? = null
    ) : BlockDecision
}

enum class BlockReason { TARGET_APP, HARD_LOCK_EXTRA, SETTINGS_BLOCKED, LAUNCHER_BLOCKED }
