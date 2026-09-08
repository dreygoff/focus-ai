package app.focus.feature.blocker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focus.designsystem.FocusTheme
import app.focus.domain.model.LockMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    private val viewModel: BlockViewModel by viewModels()

    companion object {
        private const val TAG = "BlockActivity"
        const val EXTRA_PROFILE_NAME = "profileName"
        const val EXTRA_PROFILE_ID = "profileId"
        const val EXTRA_BLOCKED_PACKAGE = "blockedPackage"
        const val EXTRA_BLOCKED_APP_NAME = "blockedAppName"
        const val EXTRA_LOCK_MODE = "lockMode"
        const val EXTRA_ATTEMPT_NUMBER = "attemptNumber"
        const val EXTRA_REMAINING_MILLIS = "remainingMillis"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_BYPASSES_USED = "bypassesUsed"

        @Volatile
        var isInForeground: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID).orEmpty()
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()

        viewModel.initialize(
            BlockInitArgs(
                sessionId = sessionId,
                profileId = profileId,
                blockedPackage = blockedPackage,
                blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: blockedPackage,
                profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Focus",
                remainingMillis = intent.getLongExtra(EXTRA_REMAINING_MILLIS, 0L),
                attemptNumber = intent.getIntExtra(EXTRA_ATTEMPT_NUMBER, 1),
                lockMode = if (intent.getStringExtra(EXTRA_LOCK_MODE) == "HARD") LockMode.Hard else LockMode.Soft,
                bypassesUsed = intent.getIntExtra(EXTRA_BYPASSES_USED, 0),
            ),
        )

        setContent { BlockActivityContent(viewModel) }

        onBackPressedDispatcher.addCallback(this) {
            viewModel.resetBypassIfInProgress()
            goHome()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
    }

    override fun onPause() {
        isInForeground = false
        viewModel.resetBypassIfInProgress()
        super.onPause()
    }

    @Suppress("DEPRECATION")
    private fun setupWindowFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun goHome() {
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(homeIntent)
        }.onFailure { error ->
            Log.e(TAG, "Failed to go home", error)
        }
    }

    internal fun goHomePublic() = goHome()

    override fun onDestroy() {
        super.onDestroy()
        isInForeground = false
    }
}

@Composable
private fun BlockActivityContent(viewModel: BlockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as BlockActivity
    FocusTheme(darkTheme = false, dynamicColor = false) {
        LaunchedEffect(uiState.bypassGranted) {
            if (uiState.bypassGranted) {
                viewModel.launchBlockedAppIntent()?.let { launchIntent ->
                    runCatching { activity.startActivity(launchIntent) }
                }
            }
        }
        BlockScreen(
            state = uiState,
            onAction = { action ->
                when (action) {
                    BlockAction.ReturnToWork -> {
                        if (uiState.bypassGranted) {
                            activity.finish()
                        } else {
                            activity.goHomePublic()
                            activity.finish()
                        }
                    }
                    BlockAction.OpenFocus -> {
                        activity.goHomePublic()
                        activity.finish()
                    }
                    else -> viewModel.onAction(action)
                }
            },
        )
    }
}
