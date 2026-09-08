package app.focus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import app.focus.designsystem.FocusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusTheme(darkTheme = false, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val showOnboarding by mainViewModel.showOnboarding.collectAsStateWithLifecycle()
                    val navController = rememberNavController()

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
                        )
                    }
                }
            }
        }
    }
}
