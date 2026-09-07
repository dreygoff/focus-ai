package app.focus.android.navigation

import androidx.compose.foundation.layout.Box as LBox
import androidx.compose.material.icons.Icons as MIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier as Mod
import androidx.compose.ui.unit.dp as Dp
import androidx.navigation.NavController as NavCtrl
import androidx.navigation.compose.NavHost as NH
import androidx.navigation.compose.composable as comp
import androidx.navigation.compose.currentBackStackEntryAsState as CBS

sealed class BottomNavItem(val route: String, val icon: String, val label: String) {
    data object Home : BottomNavItem(AppDestinations.Home.ROUTE, "\uD83C\uDFE0", "Home")
    data object Profiles : BottomNavItem(AppDestinations.Profiles.ROUTE, "\uD83D\uDCC2", "Profiles")
    data object Schedules : BottomNavItem(AppDestinations.Schedules.ROUTE, "\uD83D\uDCC5", "Schedules")
    data object Stats : BottomNavItem(AppDestinations.Stats.ROUTE, "\uD83D\uDCCA", "Stats")
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Home, BottomNavItem.Profiles, BottomNavItem.Schedules, BottomNavItem.Stats)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var selectedItem by remember { mutableInt(items.indexOfFirst { it.route == currentRoute }.takeIf { it != -1 } ?: 0) }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                items.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        icon = { Text(item.icon) },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item.route) {
                                popUpTo(items.first().route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    ) { outerPadding ->
        Column(modifier = Mod.fillMaxSize()) {
            NH(
                navController = navController,
                startDestination = BottomNavItem.Home.route
            ) {
                comp(BottomNavItem.Home.route) { HomeScreen() }
                comp(BottomNavItem.Profiles.route) { ProfilesListScreen() }
                comp("${AppDestinations.Schedules.ROUTE}/{scheduleId}") { entry -> ScheduleEditorScreen(entry.arguments?.getString("scheduleId")) }
                comp(BottomNavItem.Stats.route) { StatsScreen() }
            }
        }
    }
}

@Composable
private fun HomeScreen() {}
@Composable
private fun ProfilesListScreen() {}
@Composable
private fun ScheduleEditorScreen(scheduleId: String?) {}
@Composable
private fun StatsScreen() {}
