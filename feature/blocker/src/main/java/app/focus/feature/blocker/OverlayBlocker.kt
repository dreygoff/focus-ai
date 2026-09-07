package app.focus.feature.blocker

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateRegistry
import androidx.lifecycle.SavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeSavedStateRegistryOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.compose.ui.platform.ViewTreeLifecycleOwner
import androidx.compose.ui.platform.ViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewTreeViewModelStoreOwner
import app.focus.domain.model.BlockDecision
import app.focus.domain.model.LockMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * OverlayBlocker per TR-02 fallback: shows blocking screen via WindowManager overlay.
 * Used when BlockActivity fails to launch within timeout (vendor restrictions).
 */
class OverlayBlocker(private val context: Context) {

    companion object {
        private const val TAG = "OverlayBlocker"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isShowing = false
    
    // Event to notify when blocker is shown (used by Detector for latency measurement)
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
        val bypassesRemaining: Int = -1
    )

    fun show(blockState: BlockState) {
        try {
            // Release existing overlay first
            dismiss()

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val params = WindowManager.LayoutParams().apply {
                type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
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
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            // Create ComposeView for overlay content per TR-02
            val composeView = ComposeView(context).apply {
                id = View.generateViewId()
                
                // Set up lifecycle owners per TR-02
                setParentLifecycleOwner(OverlayLifecycleProvider())
                ViewTreeLifecycleOwner.set(this, OverlayLifecycleProvider())
                ViewTreeSavedStateRegistryOwner.set(this, OverlaySavedStateProvider())
                ViewTreeViewModelStoreOwner.set(this, OverlayViewModelProvider())

                setContent {
                    BlockScreen(state = blockState.toUiState()) { action ->
                        when (action) {
                            is BlockAction.GoHome -> {
                                goHome()
                                dismiss()
                            }
                            else -> {}
                        }
                    }
                }
            }

            overlayView = composeView
            windowManager?.addView(composeView, params)
            isShowing = true

            Log.d(TAG, "Overlay blocker shown")
            onBlockShown.emit(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay blocker", e)
            dismiss()
        }
    }

    fun dismiss() {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
            overlayView = null
            isShowing = false
            Log.d(TAG, "Overlay blocker dismissed")
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing overlay", e)
        }
    }

    fun isBlockerShowing(): Boolean = isShowing

    private fun goHome() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home from overlay", e)
        }
    }

    private fun BlockState.toUiState(): BlockUiState = BlockUiState(
        profileName = profileName,
        profileEmoji = profileEmoji,
        remainingMillis = remainingMillis,
        totalMillis = totalMillis,
        goalText = goalText,
        mode = mode,
        attemptNumber = attemptNumber,
        blockedPackageName = blockedPackageName,
        blockedAppName = blockedAppName,
        bypassesRemaining = bypassesRemaining
    )

    /** Cleanup */
    fun onDestroy() {
        dismiss()
    }
}

/** Lightweight LifecycleOwner for overlay Compose per TR-02 */
private class OverlayLifecycleProvider : androidx.lifecycle.LifecycleOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    
    init {
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
    }

    override fun getLifecycle(): androidx.lifecycle.Lifecycle = lifecycleRegistry
}

/** Lightweight SavedStateRegistryOwner for overlay */
private class OverlaySavedStateProvider : androidx.savedstate.SavedStateRegistryOwner {
    private val savedStateRegistry = androidx.savedstate.SavedStateRegistry()
    
    override fun getSavedStateRegistry(): androidx.savedstate.SavedStateRegistry = savedStateRegistry
}

/** Lightweight ViewModelStoreOwner for overlay */
private class OverlayViewModelProvider : ViewModelStoreOwner {
    private val viewModelStore = androidx.lifecycle.ViewModelStore()
    
    override fun getViewModelStore(): androidx.lifecycle.ViewModelStore = viewModelStore
}
