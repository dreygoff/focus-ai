package app.focus.android

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.focus.feature.onboarding.OnboardingRoutes
import app.focus.feature.onboarding.onboardingGraph
import app.focus.feature.settings.AllowlistSettingsScreen
import app.focus.feature.settings.DiagnosticsScreen
import app.focus.feature.settings.LicensesScreen
import app.focus.feature.settings.PrivacyPolicyScreen
import app.focus.feature.settings.ProtectionInfoScreen
import app.focus.feature.settings.SettingsRoutes
import app.focus.feature.settings.SettingsScreen
import app.focus.feature.stats.StatsRoutes
import app.focus.feature.stats.StatsScreen

internal fun NavGraphBuilder.appSettingsAndOnboardingRoutes(
    navController: NavHostController,
    versionName: String,
    onOnboardingComplete: () -> Unit,
    onLanguageChanged: (String) -> Unit,
) {
    composable(StatsRoutes.ROUTE) {
        StatsScreen()
    }
    composable(SettingsRoutes.SETTINGS) {
        SettingsScreen(
            versionName = versionName,
            onBack = { navController.popBackStack() },
            onOpenPermissions = { navController.navigate(SettingsRoutes.PERMISSIONS) },
            onOpenAllowlist = { navController.navigate(SettingsRoutes.ALLOWLIST) },
            onOpenProtectionInfo = { navController.navigate(SettingsRoutes.PROTECTION_INFO) },
            onOpenPrivacy = { navController.navigate(SettingsRoutes.PRIVACY) },
            onOpenLicenses = { navController.navigate(SettingsRoutes.LICENSES) },
            onOpenDiagnostics = { navController.navigate(SettingsRoutes.DIAGNOSTICS) },
            onLanguageChanged = onLanguageChanged,
        )
    }
    composable(SettingsRoutes.PERMISSIONS) {
        app.focus.feature.permissions.PermissionsRoute(
            onBack = { navController.popBackStack() },
        )
    }
    composable(SettingsRoutes.ALLOWLIST) {
        AllowlistSettingsScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.PROTECTION_INFO) {
        ProtectionInfoScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.PRIVACY) {
        PrivacyPolicyScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.LICENSES) {
        LicensesScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.DIAGNOSTICS) {
        DiagnosticsScreen(onBack = { navController.popBackStack() })
    }
    onboardingGraph(
        navController = navController,
        onComplete = {
            onOnboardingComplete()
            navController.navigate(AppRoutes.HOME) {
                popUpTo(OnboardingRoutes.GRAPH) { inclusive = true }
            }
        },
        onSkip = {
            onOnboardingComplete()
            navController.navigate(AppRoutes.HOME) {
                popUpTo(OnboardingRoutes.GRAPH) { inclusive = true }
            }
        },
    )
}

internal object AppRoutes {
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val PROFILES_EDITOR = "profiles/editor"
    const val PROFILES_EDITOR_WITH_ID = "profiles/editor/{profileId}"
    const val PROFILES_APPS = "profiles/{profileId}/apps"
    const val SCHEDULES = "schedules"
    const val SCHEDULE_EDITOR = "schedules/editor"
    const val SCHEDULE_EDITOR_WITH_ID = "schedules/editor/{scheduleId}"
}

internal const val DEFAULT_START_DURATION_MINUTES = 25
