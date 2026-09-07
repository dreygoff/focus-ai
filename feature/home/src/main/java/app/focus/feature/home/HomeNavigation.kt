package app.focus.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object HomeRoutes {
    const val ROUTE = "home"
    const val START_SESSION_ROUTE = "home/start/{profileId}/{durationMinutes}"
}

fun NavGraphBuilder.homeGraph(
    uiState: HomeUiState,
    onNavigateToSession: (String, Int) -> Unit,
    onPauseSession: () -> Unit = {},
    onStopSession: () -> Unit = {},
    onNavigateToProfiles: () -> Unit = {}
) {
    composable(HomeRoutes.ROUTE) {
        HomeScreen(
            uiState = uiState,
            onStartSession = onNavigateToSession,
            onPauseSession = onPauseSession,
            onStopSession = onStopSession,
            onNavigateToProfiles = onNavigateToProfiles
        )
    }
}
