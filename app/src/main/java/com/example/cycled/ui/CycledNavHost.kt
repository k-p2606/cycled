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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cycled.ui.calendar.CalendarRoute
import com.example.cycled.ui.components.CycledIcons
import com.example.cycled.ui.components.TabBar
import com.example.cycled.ui.components.TabBarItem
import com.example.cycled.ui.home.HomeRoute
import com.example.cycled.ui.logs.LogsRoute
import com.example.cycled.ui.profile.ProfileRoute
import com.example.cycled.ui.theme.cycledColors
import java.time.LocalDate

/** Route names for every screen in the app. */
object CycledDestinations {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val LOGS = "logs"
    const val PROFILE = "profile"

    /** Calendar's optional argument: an ISO date (2026-11-08) to open with that day selected. */
    const val CALENDAR_DATE_ARG = "date"
    const val CALENDAR_PATTERN = "$CALENDAR?$CALENDAR_DATE_ARG={$CALENDAR_DATE_ARG}"
}

/**
 * The four tabs in the bottom bar, in order. [route] is what to navigate to; [pattern]
 * is the destination's route as registered (Calendar's includes its optional date).
 */
enum class TopLevelTab(val route: String, val pattern: String, val label: String, val icon: ImageVector) {
    HOME(CycledDestinations.HOME, CycledDestinations.HOME, "Home", CycledIcons.Home),
    CALENDAR(CycledDestinations.CALENDAR, CycledDestinations.CALENDAR_PATTERN, "Calendar", CycledIcons.Calendar),
    LOGS(CycledDestinations.LOGS, CycledDestinations.LOGS, "Logs", CycledIcons.Logs),
    PROFILE(CycledDestinations.PROFILE, CycledDestinations.PROFILE, "Profile", CycledIcons.Profile),
    ;

    companion object {
        /** The tab [destination] belongs to; Home if none matches. */
        fun of(destination: NavDestination?): TopLevelTab =
            entries.firstOrNull { tab -> destination?.hierarchy?.any { it.route == tab.pattern } == true } ?: HOME
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
                        navController.popBackStack(tab.pattern, inclusive = false)
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
 * Opens the Calendar tab on [date], selected. Like a tab switch it saves the tab being
 * left, but it starts Calendar fresh rather than restoring it: a restored entry would
 * keep its old arguments and ignore [date].
 */
fun NavHostController.openCalendarOn(date: LocalDate) {
    navigate("${CycledDestinations.CALENDAR}?${CycledDestinations.CALENDAR_DATE_ARG}=$date") {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
    }
}

/**
 * The app's screens. All of them get the same [viewModel], so a day logged on Calendar
 * shows on Home as soon as you go back.
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
                // "Log today" opens Calendar with today selected (D6).
                onLogTodayClick = { navController.openCalendarOn(viewModel.today()) },
            )
        }
        composable(
            CycledDestinations.CALENDAR_PATTERN,
            arguments = listOf(
                navArgument(CycledDestinations.CALENDAR_DATE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val openDate = entry.arguments?.getString(CycledDestinations.CALENDAR_DATE_ARG)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            CalendarRoute(viewModel = viewModel, openDate = openDate)
        }
        composable(CycledDestinations.LOGS) {
            // Tapping a day opens it on Calendar (D10).
            LogsRoute(viewModel = viewModel, onOpenDay = { navController.openCalendarOn(it) })
        }
        composable(CycledDestinations.PROFILE) {
            ProfileRoute()
        }
    }
}
