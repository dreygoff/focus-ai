package app.focus.system

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewTreeLifecycleOwner
import androidx.compose.ui.platform.ViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewTreeViewModelStoreOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateRegistryOwner
import androidx.lifecycle.ViewModelStoreOwner

object OverlayLifecycleOwner : ViewTreeLifecycleOwner, ViewTreeSavedStateRegistryOwner, ViewTreeViewModelStoreOwner {

        var lifecycleOwnerRef: androidx.lifecycle.LifecycleOwner? = null

        override fun getLifecycle(): androidx.lifecycle.Lifecycle {
            return lifecycleOwnerRef?.lifecycle
                ?: object : androidx.lifecycle.Lifecycle {
                    override fun addObserver(p0: androidx.lifecycle.LifecycleObserver) = Unit
                    override fun removeObserver(p0: androidx.lifecycle.LifecycleObserver) = Unit
                    override fun getCurrentState(): androidx.lifecycle.Lifecycle.State = androidx.lifecycle.Lifecycle.State.INITIALIZED
                    override fun getLifecycle(): androidx.lifecycle.Lifecycle = this
                }
        }

        override fun getSavedStateRegistry(): androidx.savedstate.SavedStateRegistry {
            val owner = lifecycleOwnerRef as? androidx.lifecycle.SavedStateRegistryOwner
            return owner?.savedStateRegistry
                ?: object : androidx.savedstate.SavedStateRegistry {
                    override var isConsumed: Boolean = false
                    override fun saveActiveState() = Unit
                    override fun consumeRestoredStateForKey(key: String) = null
                    override fun getSavedStateProvider(key: String): androidx.savedstate.SavedStateRegistry.SavedStateProvider? = null
                    override fun registerSavedStateProvider(key: String, provider: androidx.savedstate.SavedStateRegistry.SavedStateProvider) = Unit
                    override fun unregisterSavedStateProvider(key: String) = Unit
                }
        }

        override fun getViewModelStore(): androidx.lifecycle.ViewModelStore {
            val owner = lifecycleOwnerRef as? androidx.lifecycle.ViewModelStoreOwner
            return owner?.viewModelStore ?: androidx.lifecycle.ViewModelStore()
        }

        @Composable
        fun SetupOverlayCompose(
            parent: View,
            content: @Composable () -> Unit
        ) {
            val composeView = ComposeView(parent.context).apply {
                id = android.view.View.generateViewId()
            }

            lifecycleOwnerRef = androidx.lifecycle.LifecycleRegistryOwner(parent.context)

            ViewTreeLifecycleOwner.set(composeView, androidx.lifecycle.findViewTreeLifecycleOwner(parent) ?: this)
            ViewTreeSavedStateRegistryOwner.set(
                composeView,
                androidx.lifecycle.findViewTreeSavedStateRegistryOwner(parent) ?: this
            )
            ViewTreeViewModelStoreOwner.set(composeView, androidx.lifecycle.findViewTreeViewModelStoreOwner(parent) ?: this)

            composeView.setContent(content)
        }

        private class LifecycleRegistryOwner(private val context: android.content.Context) : androidx.lifecycle.LifecycleOwner {
            private val registry = androidx.lifecycle.LifecycleRegistry(this)
            override fun getLifecycle(): androidx.lifecycle.Lifecycle = registry
            init {
                registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
            }
        }

    companion object {
        private const val TAG = "OverlayLifecycleOwner"
    }
}
