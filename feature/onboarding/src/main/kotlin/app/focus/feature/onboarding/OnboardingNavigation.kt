package app.focus.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

object OnboardingRoutes {
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
    composable(OnboardingRoutes.WELCOME) {
        WelcomeScreen(
            onNext = { navController.navigate(OnboardingRoutes.PICK_APPS) },
            onSkip = onSkip,
        )
    }
    composable(OnboardingRoutes.PICK_APPS) {
        PickAppsScreen(onDone = { navController.navigate(OnboardingRoutes.PERMISSIONS) })
    }
    composable(OnboardingRoutes.PERMISSIONS) {
        app.focus.feature.permissions.PermissionsRoute(
            onAllGranted = {
                navController.navigate(OnboardingRoutes.FIRST_PROFILE) {
                    popUpTo(OnboardingRoutes.WELCOME) { inclusive = true }
                }
            },
            onBack = onSkip,
        )
    }
    composable(OnboardingRoutes.FIRST_PROFILE) {
        FirstProfileCreatorScreen(onCreated = onComplete, onSkip = onComplete)
    }
}
