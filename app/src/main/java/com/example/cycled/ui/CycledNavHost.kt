package com.example.cycled.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cycled.ui.calendar.CalendarRoute
import com.example.cycled.ui.components.CycledIcons
import com.example.cycled.ui.components.TabBar
import com.example.cycled.ui.components.TabBarItem
import com.example.cycled.ui.home.HomeRoute
import com.example.cycled.ui.log.LogRoute
import com.example.cycled.ui.logs.LogsRoute
import com.example.cycled.ui.profile.ProfileRoute
import com.example.cycled.ui.theme.cycledColors

/** Route names for every screen in the app. */
object CycledDestinations {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val LOGS = "logs"
    const val PROFILE = "profile"

    /** The old Log screen, opened from Home. Goes away once Calendar logging works (D6). */
    const val LOG = "log"
}

/** The four tabs in the bottom bar, in order. */
enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(CycledDestinations.HOME, "Home", CycledIcons.Home),
    CALENDAR(CycledDestinations.CALENDAR, "Calendar", CycledIcons.Calendar),
    LOGS(CycledDestinations.LOGS, "Logs", CycledIcons.Logs),
    PROFILE(CycledDestinations.PROFILE, "Profile", CycledIcons.Profile),
    ;

    companion object {
        /**
         * The tab [destination] belongs to. Screens outside the tabs (the old Log screen,
         * opened from Home) count as Home.
         */
        fun of(destination: NavDestination?): TopLevelTab =
            entries.firstOrNull { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }
                ?: HOME
    }
}

/**
 * The app shell: every screen above a fixed bottom [TabBar]. Switching tabs saves the
 * tab you leave and restores the one you open (scroll position, Calendar's month, ...).
 */
@Composable
fun CycledApp(
    viewModel: CycleViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = TopLevelTab.of(backStackEntry?.destination)

    Scaffold(
        containerColor = MaterialTheme.cycledColors.sky,
        // Screens handle the status bar themselves; the tab bar handles the navigation bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            TabBar(
                items = TopLevelTab.entries.map { TabBarItem(it.label, it.icon) },
                selectedIndex = currentTab.ordinal,
                onSelect = { index ->
                    val tab = TopLevelTab.entries[index]
                    if (tab == currentTab) {
                        // Re-tapping the current tab goes back to its first screen.
                        navController.popBackStack(tab.route, inclusive = false)
                    } else {
                        navController.navigateToTab(tab)
                    }
                },
                showTopDivider = currentTab == TopLevelTab.PROFILE,
            )
        },
    ) { innerPadding ->
        CycledNavHost(
            viewModel = viewModel,
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

/**
 * Bottom-tab navigation: keep only the start destination under the tab, save the
 * state of the tab being left, and restore the target tab's saved state.
 */
fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The app's screens. All of them get the same [viewModel], so a day saved on Log
 * shows on Home and Calendar as soon as you go back.
 */
@Composable
fun CycledNavHost(
    viewModel: CycleViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CycledDestinations.HOME,
        modifier = modifier,
    ) {
        composable(CycledDestinations.HOME) {
            HomeRoute(
                viewModel = viewModel,
                onLogClick = {
                    navController.navigate(CycledDestinations.LOG) { launchSingleTop = true }
                },
                onCalendarClick = { navController.navigateToTab(TopLevelTab.CALENDAR) },
            )
        }
        composable(CycledDestinations.CALENDAR) {
            CalendarRoute(viewModel = viewModel, onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable(CycledDestinations.LOGS) {
            LogsRoute()
        }
        composable(CycledDestinations.PROFILE) {
            ProfileRoute()
        }
        composable(CycledDestinations.LOG) {
            LogRoute(viewModel = viewModel, onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
