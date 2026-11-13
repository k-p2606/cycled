package com.example.cycled.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cycled.ui.home.HomeRoute
import com.example.cycled.ui.log.LogRoute

/** Route names for every screen in the app. */
object CycledDestinations {
    const val HOME = "home"
    const val LOG = "log"
}

/**
 * The app's screens. Both get the same [viewModel], so a day saved on Log shows
 * on Home as soon as you go back.
 */
@Composable
fun CycledNavHost(
    viewModel: CycleViewModel,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = CycledDestinations.HOME) {
        composable(CycledDestinations.HOME) {
            HomeRoute(
                viewModel = viewModel,
                onLogClick = {
                    navController.navigate(CycledDestinations.LOG) { launchSingleTop = true }
                },
            )
        }
        composable(CycledDestinations.LOG) {
            LogRoute(viewModel = viewModel, onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
