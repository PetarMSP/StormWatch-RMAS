package com.example.stormwatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.stormwatch.ui.screen.*
import com.example.stormwatch.viewmodel.AuthViewModel
import com.example.stormwatch.viewmodel.MainViewModel

object Routes {
    const val START = "start"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"

    const val REPORT_DETAIL = "reportDetail"
    fun reportDetail(reportId: String) = "$REPORT_DETAIL/$reportId"
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel, authViewModel: AuthViewModel) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.START
    ) {
        composable(Routes.START) { StartScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController, authViewModel) }
        composable(Routes.REGISTER) { RegisterScreen(navController, authViewModel) }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = mainViewModel,
                onOpenReport = { reportId ->
                    navController.navigate(Routes.reportDetail(reportId))
                }
            )
        }

        composable(
            route = "${Routes.REPORT_DETAIL}/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: return@composable

            androidx.compose.runtime.LaunchedEffect(reportId) {
                mainViewModel.openReport(reportId)
            }

            LocalReportScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}