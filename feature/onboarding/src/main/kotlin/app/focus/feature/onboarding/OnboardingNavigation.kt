package app.focus.feature.onboarding

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

object OnboardingRoutes {
    const val GRAPH = "onboarding"
    const val WELCOME = "onboarding/welcome"
    const val PICK_APPS = "onboarding/pick_apps"
    const val PERMISSIONS = "onboarding/permissions"
    const val FIRST_PROFILE = "onboarding/first_profile"
}

fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    navigation(
        route = OnboardingRoutes.GRAPH,
        startDestination = OnboardingRoutes.WELCOME,
    ) {
        composable(OnboardingRoutes.WELCOME) {
            WelcomeScreen(
                onNext = { navController.navigate(OnboardingRoutes.PICK_APPS) },
                onSkip = onSkip,
            )
        }
        composable(OnboardingRoutes.PICK_APPS) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(OnboardingRoutes.GRAPH)
            }
            val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
            PickAppsScreen(
                viewModel = viewModel,
                onDone = { navController.navigate(OnboardingRoutes.PERMISSIONS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(OnboardingRoutes.PERMISSIONS) {
            app.focus.feature.permissions.PermissionsRoute(
                onAllGranted = { navController.navigate(OnboardingRoutes.FIRST_PROFILE) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(OnboardingRoutes.FIRST_PROFILE) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(OnboardingRoutes.GRAPH)
            }
            val viewModel: OnboardingViewModel = hiltViewModel(parentEntry)
            FirstProfileCreatorScreen(
                viewModel = viewModel,
                onCreated = onComplete,
                onSkip = onComplete,
            )
        }
    }
}
