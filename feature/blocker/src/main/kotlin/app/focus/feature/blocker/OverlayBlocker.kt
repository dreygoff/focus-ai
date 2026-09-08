package app.focus.feature.blocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import app.focus.domain.model.LockMode
import app.focus.system.OverlayLifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow

class OverlayBlocker(private val context: Context) {

    companion object {
        private const val TAG = "OverlayBlocker"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isShowing = false

    val onBlockShown = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    data class BlockState(
        val profileName: String,
        val profileEmoji: String?,
        val remainingMillis: Long,
        val totalMillis: Long,
        val goalText: String?,
        val mode: LockMode,
        val attemptNumber: Int,
        val blockedPackageName: String,
        val blockedAppName: String,
        val bypassesRemaining: Int = -1,
    )

    fun show(blockState: BlockState) {
        try {
            dismiss()
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner
            val composeView = buildOverlayContent(blockState)
            overlayView = composeView
            windowManager?.addView(composeView, buildOverlayLayoutParams())
            isShowing = true
            onBlockShown.tryEmit(Unit)
            Log.d(TAG, "Overlay blocker shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay blocker", e)
            dismiss()
        }
    }

    private fun buildOverlayLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            flags = (
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            gravity = Gravity.CENTER
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private fun buildOverlayContent(
        blockState: BlockState,
    ): View = OverlayLifecycleOwner.createComposeView(
        parent = View(context),
        content = {
            BlockScreen(
                state = BlockUiState(
                    blockedPackage = blockState.blockedPackageName,
                    appName = blockState.blockedAppName,
                    profileName = blockState.profileName,
                    remainingMillis = blockState.remainingMillis,
                    attemptNumber = blockState.attemptNumber,
                    lockMode = blockState.mode,
                    bypassesRemaining = blockState.bypassesRemaining,
                ),
                        onAction = { action ->
                            when (action) {
                                BlockAction.ReturnToWork, BlockAction.OpenFocus -> {
                                    goHome()
                                    dismiss()
                                }
                                BlockAction.StartBypass, BlockAction.CancelBypass,
                                BlockAction.StartEmergencyExit, BlockAction.CancelEmergencyExit,
                                is BlockAction.SubmitBypassReason, is BlockAction.InputPhraseChar,
                                is BlockAction.InputEmergencyExitChar,
                                -> Unit
                            }
                        },
            )
        },
    )

    fun dismiss() {
        try {
            overlayView?.let { view -> windowManager?.removeView(view) }
            lifecycleOwner?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing overlay", e)
        } finally {
            overlayView = null
            lifecycleOwner = null
            isShowing = false
        }
    }

    fun isBlockerShowing(): Boolean = isShowing

    private fun goHome() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home from overlay", e)
        }
    }

    fun onDestroy() {
        dismiss()
    }
}
