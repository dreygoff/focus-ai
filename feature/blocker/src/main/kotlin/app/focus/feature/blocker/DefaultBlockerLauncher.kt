package app.focus.feature.blocker

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import app.focus.domain.model.LockMode
import app.focus.domain.usecase.BlockLauncher
import app.focus.domain.usecase.BlockRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBlockerLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : BlockLauncher {

    private val overlayBlocker = OverlayBlocker(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastBlockedPackage: String? = null
    private var lastBlockAtMillis: Long = 0L
    private var pendingFallback: Runnable? = null

    override fun show(request: BlockRequest) {
        val now = System.currentTimeMillis()
        if (request.blockedPackage == lastBlockedPackage &&
            now - lastBlockAtMillis < ANTI_LOOP_MS &&
            isShowing()
        ) {
            return
        }

        pendingFallback?.let { mainHandler.removeCallbacks(it) }
        lastBlockedPackage = request.blockedPackage
        lastBlockAtMillis = now

        if (request.forceOverlay) {
            showOverlay(request)
            return
        }

        BlockActivity.isInForeground = false
        val intent = Intent(context, BlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            )
            putExtra(BlockActivity.EXTRA_PROFILE_NAME, request.profileName)
            putExtra(BlockActivity.EXTRA_PROFILE_ID, request.profileId)
            putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, request.blockedPackage)
            putExtra(BlockActivity.EXTRA_BLOCKED_APP_NAME, request.blockedAppName)
            putExtra(BlockActivity.EXTRA_LOCK_MODE, lockModeName(request.lockMode))
            putExtra(BlockActivity.EXTRA_ATTEMPT_NUMBER, request.attemptNumber)
            putExtra(BlockActivity.EXTRA_REMAINING_MILLIS, request.remainingMillis)
            putExtra(BlockActivity.EXTRA_SESSION_ID, request.sessionId)
            putExtra(BlockActivity.EXTRA_BYPASSES_USED, request.bypassesUsed)
        }
        context.startActivity(intent)

        val fallback = Runnable {
            if (!BlockActivity.isInForeground) {
                showOverlay(request)
            }
        }
        pendingFallback = fallback
        mainHandler.postDelayed(fallback, ACTIVITY_FALLBACK_MS)
    }

    override fun dismiss() {
        pendingFallback?.let { mainHandler.removeCallbacks(it) }
        pendingFallback = null
        overlayBlocker.dismiss()
        lastBlockedPackage = null
    }

    override fun isShowing(): Boolean =
        BlockActivity.isInForeground || overlayBlocker.isBlockerShowing()

    private fun showOverlay(request: BlockRequest) {
        overlayBlocker.show(
            OverlayBlocker.BlockState(
                profileName = request.profileName,
                profileEmoji = null,
                remainingMillis = request.remainingMillis,
                totalMillis = request.remainingMillis,
                goalText = null,
                mode = request.lockMode,
                attemptNumber = request.attemptNumber,
                blockedPackageName = request.blockedPackage,
                blockedAppName = request.blockedAppName,
            ),
        )
    }

    private fun lockModeName(lockMode: LockMode): String = when (lockMode) {
        is LockMode.Hard -> "HARD"
        is LockMode.Soft -> "SOFT"
        else -> "SOFT"
    }

    companion object {
        private const val ANTI_LOOP_MS = 2_000L
        private const val ACTIVITY_FALLBACK_MS = 600L
    }
}
