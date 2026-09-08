package app.focus.feature.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.focus.domain.model.LockMode

class BlockActivity : ComponentActivity() {

    companion object {
        private const val TAG = "BlockActivity"
        const val EXTRA_PROFILE_NAME = "profileName"
        const val EXTRA_BLOCKED_PACKAGE = "blockedPackage"
        const val EXTRA_BLOCKED_APP_NAME = "blockedAppName"
    }

    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowFlags()
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Focus"
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "unknown"
        val blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: blockedPackage

        setContent {
            BlockScreen(
                blockedPackage = blockedPackage,
                appName = blockedAppName,
                profileName = profileName,
                onReturnToWork = { goHome() },
                onBypassAttempt = { reason -> reason.length >= 10 },
            )
        }

        registerScreenReceiver()

        onBackPressedDispatcher.addCallback(this) {
            goHome()
            finish()
        }
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

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    Log.d(TAG, "User present")
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.RECEIVER_NOT_EXPORTED
            } else {
                0
            },
        )
    }


    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenReceiver?.let { receiver ->
            runCatching { unregisterReceiver(receiver) }
        }
        screenReceiver = null
    }
}
