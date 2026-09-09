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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import app.focus.feature.home.HomeScreenCallbacks
import app.focus.feature.home.HomeUiState
import app.focus.feature.home.HomeViewModel
import app.focus.feature.onboarding.OnboardingRoutes
import app.focus.feature.profiles.AppPickerRoute
import app.focus.feature.profiles.ProfileEditorScreen
import app.focus.feature.profiles.ProfilesRoute
import app.focus.feature.schedules.ScheduleEditorScreen
import app.focus.feature.schedules.SchedulesScreen
import app.focus.feature.schedules.SchedulesViewModel
import app.focus.feature.settings.SettingsRoutes
import app.focus.feature.session.routes.SessionRoutes
import app.focus.feature.session.start.StartSessionRoute
import app.focus.feature.session.SessionSummaryRoute
import app.focus.feature.stats.StatsRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    mainViewModel: MainViewModel = hiltViewModel(),
    versionName: String = "1.0.0",
    onOnboardingComplete: () -> Unit = {},
    onLanguageChanged: (String) -> Unit = {},
) {
    val pendingSessionSummaryId by mainViewModel.pendingSessionSummaryId.collectAsStateWithLifecycle()
    LaunchedEffect(pendingSessionSummaryId) {
        val sessionId = pendingSessionSummaryId ?: return@LaunchedEffect
        mainViewModel.consumeSessionSummaryNavigation()
        navController.navigate(SessionRoutes.sessionSummary(sessionId)) {
            popUpTo(AppRoutes.HOME) { saveState = true }
            launchSingleTop = true
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentSelectedIndex = remember(currentRoute ?: AppRoutes.HOME) {
        bottomNavIndexForRoute(currentRoute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_app_title)) },
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = { navController.navigate(SettingsRoutes.SETTINGS) },
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_cd_settings),
                        )
                    }
                },
            )
        },
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
            startSessionRoute(navController)
            sessionSummaryRoute(navController)
            profilesRoute(navController)
            profileEditorRoutes(navController)
            schedulesRoute(navController)
            appSettingsAndOnboardingRoutes(
                navController = navController,
                versionName = versionName,
                onOnboardingComplete = onOnboardingComplete,
                onLanguageChanged = onLanguageChanged,
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
        BottomNavItem(AppRoutes.HOME, Icons.Default.Home, R.string.nav_cd_home),
        BottomNavItem(AppRoutes.PROFILES, Icons.Outlined.PersonOutline, R.string.nav_cd_profiles),
        BottomNavItem(AppRoutes.SCHEDULES, Icons.Outlined.EventNote, R.string.nav_cd_schedules),
        BottomNavItem(StatsRoutes.ROUTE, Icons.Default.Equalizer, R.string.nav_cd_stats),
    )
    NavigationBar {
        bottomNavItems.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(item.contentDescriptionRes)) },
                alwaysShowLabel = false,
                selected = selected,
                onClick = { if (!selected) onNavigate(item.route) },
            )
        }
    }
}

private fun NavGraphBuilder.schedulesRoute(navController: NavHostController) {
    composable(AppRoutes.SCHEDULES) {
        val viewModel: SchedulesViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        SchedulesScreen(
            schedules = state.schedules,
            onAddSchedule = { navController.navigate(AppRoutes.SCHEDULE_EDITOR) },
            onEditSchedule = { id -> navController.navigate("schedules/editor/$id") },
            onDeleteSchedule = viewModel::deleteSchedule,
            onToggleEnabled = viewModel::toggleEnabled,
        )
    }

    composable(AppRoutes.SCHEDULE_EDITOR) {
        val viewModel: SchedulesViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ScheduleEditorScreen(
            profiles = state.profiles,
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = AppRoutes.SCHEDULE_EDITOR_WITH_ID,
        arguments = listOf(navArgument("scheduleId") { type = NavType.StringType }),
    ) { entry ->
        val scheduleId = entry.arguments?.getString("scheduleId")
        val viewModel: SchedulesViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val existing = state.schedules.firstOrNull { it.id == scheduleId }
        ScheduleEditorScreen(
            profiles = state.profiles,
            existing = existing,
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.homeRoute(
    navController: NavHostController,
) {
    composable(AppRoutes.HOME) {
        val viewModel: HomeViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val loadingDescription = stringResource(R.string.cd_loading)

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.isLoading,
                transitionSpec = {
                    fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300))
                },
                modifier = Modifier.fillMaxSize(),
            ) { isLoading ->
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = loadingDescription },
                        )
                    }
                } else {
                    HomeScreen(
                        uiState = HomeUiState(
                            activeSession = state.activeSession,
                            pomodoroPhase = state.pomodoroPhase,
                            phaseEndAtMillis = state.phaseEndAtMillis,
                            profiles = state.profiles,
                            todayFocusMinutes = state.todayFocusMinutes,
                            streakDays = state.streakDays,
                        ),
                        callbacks = HomeScreenCallbacks(
                            onStartSession = { profileId, durationMinutes ->
                                navController.navigate(
                                    SessionRoutes.start(profileId, durationMinutes),
                                )
                            },
                            onStartPomodoro = { profileId ->
                                val resolvedProfileId = profileId
                                    ?: state.profiles.firstOrNull()?.id
                                    ?: return@HomeScreenCallbacks
                                viewModel.startPomodoroSession(resolvedProfileId)
                            },
                            onPauseSession = { viewModel.pauseOrResumeSession() },
                            onStopSession = { viewModel.requestStopSession() },
                            onNavigateToProfiles = { navController.navigate(AppRoutes.PROFILES) },
                        ),
                    )
                }
            }

            if (state.showStopConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { viewModel.dismissStopConfirmation() },
                    title = { androidx.compose.material3.Text(stringResource(R.string.dialog_stop_session_title)) },
                    text = { androidx.compose.material3.Text(stringResource(R.string.dialog_stop_session_body)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.confirmStopSession() }) {
                            androidx.compose.material3.Text(stringResource(R.string.dialog_stop_session_confirm))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.dismissStopConfirmation() }) {
                            androidx.compose.material3.Text(stringResource(R.string.dialog_stop_session_dismiss))
                        }
                    },
                )
            }

            if (state.showHardLockConfirmation) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { viewModel.dismissHardLockConfirmation() },
                    title = { androidx.compose.material3.Text(stringResource(R.string.dialog_hard_lock_title)) },
                    text = {
                        androidx.compose.material3.Text(stringResource(R.string.dialog_hard_lock_body))
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.confirmHardLockStart() }) {
                            androidx.compose.material3.Text(stringResource(R.string.dialog_hard_lock_confirm))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.dismissHardLockConfirmation() }) {
                            androidx.compose.material3.Text(stringResource(R.string.dialog_cancel))
                        }
                    },
                )
            }
        }
    }
}

private fun NavGraphBuilder.sessionSummaryRoute(navController: NavHostController) {
    composable(
        route = SessionRoutes.SESSION_SUMMARY,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) { entry ->
        val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
        SessionSummaryRoute(
            sessionId = sessionId,
            onBackToHome = {
                navController.popBackStack(AppRoutes.HOME, inclusive = false)
            },
        )
    }
}

private fun NavGraphBuilder.startSessionRoute(navController: NavHostController) {
    composable(
        route = SessionRoutes.START_SESSION,
        arguments = listOf(
            navArgument(SessionRoutes.ARG_PROFILE_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(SessionRoutes.ARG_DURATION_MINUTES) {
                type = NavType.IntType
                defaultValue = DEFAULT_START_DURATION_MINUTES
            },
        ),
    ) {
        StartSessionRoute(
            onBack = { navController.popBackStack() },
            onSessionStarted = {
                navController.popBackStack(AppRoutes.HOME, inclusive = false)
            },
        )
    }
}

private fun NavGraphBuilder.profilesRoute(
    navController: NavHostController,
) {
    composable(AppRoutes.PROFILES) {
        ProfilesRoute(
            onNavigateToStartSession = { profileId, durationMinutes ->
                navController.navigate(SessionRoutes.start(profileId, durationMinutes))
            },
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
    val contentDescriptionRes: Int,
)
