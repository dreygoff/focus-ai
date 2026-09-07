package app.focus.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import app.focus.feature.home.HomeScreen
import app.focus.feature.home.HomeViewModel
import app.focus.feature.onboarding.OnboardingRoutes
import app.focus.feature.onboarding.onboardingGraph
import app.focus.feature.profiles.ProfilesListScreen
import app.focus.feature.schedules.SchedulesScreen
import app.focus.feature.stats.StatsRoutes
import app.focus.feature.stats.StatsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    onNavigateToStartSession: (profileId: String?, durationMinutes: Int) -> Unit = { _, _ -> },
    shouldShowOnboarding: Boolean = false
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("home", Icons.Default.Home),
        BottomNavItem("profiles", Icons.Default.PersonOutline),
        BottomNavItem("schedules", Icons.Default.EventNote),
        BottomNavItem("stats", Icons.Default.Equalizer)
    )

    val currentSelectedIndex = remember(currentRoute ?: "home") {
        when {
            currentRoute != null && currentRoute == "home" -> 0
            currentRoute != null && currentRoute == "profiles" -> 1
            currentRoute != null && currentRoute == "schedules" -> 2
            currentRoute != null && currentRoute.startsWith(StatsRoutes.ROUTE) -> 3
            else -> 0
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Focus") }) },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    val selected = currentSelectedIndex == index
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = {},
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    restoreState = true
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (shouldShowOnboarding) OnboardingRoutes.WELCOME else "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                AnimatedContent(
                    targetState = state.isLoading,
                    transitionSpec = { tween(durationMillis = 300) }
                ) { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        HomeScreen(
                            uiState = app.focus.feature.home.HomeUiState(
                                activeSession = state.activeSession,
                                profiles = state.profiles,
                                todayFocusMinutes = state.todayFocusMinutes,
                                streakDays = state.streakDays
                            ),
                            onStartSession = onNavigateToStartSession,
                            onPauseSession = {},
                            onStopSession = { viewModel.cancelSession() },
                            onNavigateToProfiles = { navController.navigate("profiles") }
                        )
                    }
                }
            }

            composable("profiles") {
                ProfilesListScreen(
                    onNavigateToStartSession = onNavigateToStartSession,
                    onBack = {}
                )
            }

            composable("schedules") {
                SchedulesScreen(
                    schedules = emptyList(),
                    onAddSchedule = {},
                    onEditSchedule = {},
                    onToggleSchedule = {}
                )
            }

            composable(StatsRoutes.ROUTE) {
                StatsScreen(onBack = {})
            }

            onboardingGraph(
                onComplete = { navController.navigate("profiles") { popUpTo("home") { inclusive = true } } },
                onSkip = { navController.navigate("profiles") { popUpTo("home") { inclusive = true } } }
            )
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
