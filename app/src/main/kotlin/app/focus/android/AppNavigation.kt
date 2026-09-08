package app.focus.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import app.focus.feature.home.HomeScreen
import app.focus.feature.home.HomeUiState
import app.focus.feature.home.HomeViewModel
import app.focus.feature.onboarding.OnboardingRoutes
import app.focus.feature.onboarding.onboardingGraph
import app.focus.feature.profiles.AppPickerRoute
import app.focus.feature.profiles.ProfileEditorScreen
import app.focus.feature.profiles.ProfilesRoute
import app.focus.feature.schedules.SchedulesScreen
import app.focus.feature.stats.StatsRoutes
import app.focus.feature.stats.StatsScreen

private object AppRoutes {
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val PROFILES_EDITOR = "profiles/editor"
    const val PROFILES_EDITOR_WITH_ID = "profiles/editor/{profileId}"
    const val PROFILES_APPS = "profiles/{profileId}/apps"
    const val SCHEDULES = "schedules"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit = { _, _ -> },
    onOnboardingComplete: () -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentSelectedIndex = remember(currentRoute ?: AppRoutes.HOME) {
        bottomNavIndexForRoute(currentRoute)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Focus") }) },
        bottomBar = {
            AppBottomBar(
                selectedIndex = currentSelectedIndex,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        restoreState = true
                        launchSingleTop = true
                    }
                },
            )
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
        ) {
            homeRoute(navController)
            profilesRoute(onNavigateToStartSession, navController)
            profileEditorRoutes(navController)
            composable(AppRoutes.SCHEDULES) {
                SchedulesScreen(
                    schedules = emptyList(),
                    onAddSchedule = {},
                    onEditSchedule = {},
                    onDeleteSchedule = {},
                )
            }
            composable(StatsRoutes.ROUTE) {
                StatsScreen(onBack = {})
            }
            onboardingGraph(
                navController = navController,
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(OnboardingRoutes.WELCOME) { inclusive = true }
                    }
                },
                onSkip = {
                    onOnboardingComplete()
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(OnboardingRoutes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    selectedIndex: Int,
    onNavigate: (String) -> Unit,
) {
    val bottomNavItems = listOf(
        BottomNavItem(AppRoutes.HOME, Icons.Default.Home),
        BottomNavItem(AppRoutes.PROFILES, Icons.Outlined.PersonOutline),
        BottomNavItem(AppRoutes.SCHEDULES, Icons.Outlined.EventNote),
        BottomNavItem(StatsRoutes.ROUTE, Icons.Default.Equalizer),
    )
    NavigationBar {
        bottomNavItems.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = {},
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
            )
        }
    }
}

private fun NavGraphBuilder.homeRoute(
    navController: NavHostController,
) {
    composable(AppRoutes.HOME) {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.isLoading,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300))
                },
                modifier = Modifier.fillMaxSize(),
            ) { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    HomeScreen(
                        uiState = HomeUiState(
                            activeSession = state.activeSession,
                            profiles = state.profiles,
                            todayFocusMinutes = state.todayFocusMinutes,
                            streakDays = state.streakDays,
                        ),
                        onStartSession = { profileId, durationMinutes ->
                            val resolvedProfileId = profileId
                                ?: state.profiles.firstOrNull()?.id
                                ?: return@HomeScreen
                            viewModel.startSession(resolvedProfileId, durationMinutes)
                        },
                        onPauseSession = { viewModel.pauseOrResumeSession() },
                        onStopSession = { viewModel.requestStopSession() },
                        onNavigateToProfiles = { navController.navigate(AppRoutes.PROFILES) },
                    )
                }
            }

            if (state.showStopConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { viewModel.dismissStopConfirmation() },
                    title = { androidx.compose.material3.Text("End session?") },
                    text = { androidx.compose.material3.Text("Your focus session will be cancelled.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.confirmStopSession() }) {
                            androidx.compose.material3.Text("End session")
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.dismissStopConfirmation() }) {
                            androidx.compose.material3.Text("Keep focusing")
                        }
                    },
                )
            }
        }
    }
}

private fun NavGraphBuilder.profilesRoute(
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit,
    navController: NavHostController,
) {
    composable(AppRoutes.PROFILES) {
        ProfilesRoute(
            onNavigateToStartSession = onNavigateToStartSession,
            onEditProfile = { profileId ->
                val route = if (profileId == null) {
                    AppRoutes.PROFILES_EDITOR
                } else {
                    "profiles/editor/$profileId"
                }
                navController.navigate(route)
            },
        )
    }
}

private fun NavGraphBuilder.profileEditorRoutes(navController: NavHostController) {
    composable(AppRoutes.PROFILES_EDITOR) {
        ProfileEditorScreen(
            onSave = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
            onPickApps = { profileId -> navController.navigate("profiles/$profileId/apps") },
        )
    }

    composable(
        route = AppRoutes.PROFILES_EDITOR_WITH_ID,
        arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
    ) {
        ProfileEditorScreen(
            onSave = { navController.popBackStack() },
            onCancel = { navController.popBackStack() },
            onPickApps = { profileId -> navController.navigate("profiles/$profileId/apps") },
        )
    }

    composable(
        route = AppRoutes.PROFILES_APPS,
        arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
    ) {
        AppPickerRoute(
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() },
        )
    }
}

private fun bottomNavIndexForRoute(currentRoute: String?): Int = when {
    currentRoute == AppRoutes.HOME -> 0
    currentRoute == AppRoutes.PROFILES -> 1
    currentRoute == AppRoutes.SCHEDULES -> 2
    currentRoute != null && currentRoute.startsWith(StatsRoutes.ROUTE) -> 3
    else -> 0
}

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
