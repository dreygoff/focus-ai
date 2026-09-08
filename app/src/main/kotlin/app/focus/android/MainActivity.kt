package app.focus.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import android.os.Bundle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.focus.android.shortcuts.ProfileShortcutsManager
import app.focus.designsystem.FocusTheme
import app.focus.domain.model.Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val showOnboarding by mainViewModel.showOnboarding.collectAsStateWithLifecycle()
            val theme by mainViewModel.theme.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
            val darkTheme = when (theme) {
                Theme.SYSTEM -> isSystemInDarkTheme()
                Theme.LIGHT -> false
                Theme.DARK -> true
            }

            FocusTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    androidx.compose.runtime.LaunchedEffect(intent) {
                        handleShortcutIntent(intent, mainViewModel)
                    }

                    when (showOnboarding) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        else -> AppNavigation(
                            navController = navController,
                            startDestination = if (showOnboarding == true) {
                                app.focus.feature.onboarding.OnboardingRoutes.WELCOME
                            } else {
                                "home"
                            },
                            onOnboardingComplete = mainViewModel::completeOnboarding,
                            versionName = BuildConfig.VERSION_NAME,
                            onLanguageChanged = { tag ->
                                app.focus.android.locale.AppLocaleController.apply(this@MainActivity, tag)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?, viewModel: MainViewModel) {
        if (intent?.action != ProfileShortcutsManager.ACTION_START_PROFILE) return
        val profileId = intent.getStringExtra(ProfileShortcutsManager.EXTRA_PROFILE_ID) ?: return
        viewModel.handleShortcutStart(profileId)
    }
}
