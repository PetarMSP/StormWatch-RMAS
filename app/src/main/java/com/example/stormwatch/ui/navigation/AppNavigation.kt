package com.example.stormwatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.stormwatch.ui.screen.*
import com.example.stormwatch.viewmodel.AuthViewModel
import com.example.stormwatch.viewmodel.LanguageViewModel
import com.example.stormwatch.viewmodel.MainViewModel

object Routes {
    const val START = "start"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MAP = "map"
    const val REPORT_CREATE = "reportCreate"
    const val VERIFICATION = "verification"

    const val SETTINGS = "settings"
    fun verification(email: String) = "$VERIFICATION/$email"
    fun reportCreate(timestamp: Long) = "$REPORT_CREATE/$timestamp"

    const val REPORT_DETAIL = "reportDetail"
    fun reportDetail(reportId: String) = "$REPORT_DETAIL/$reportId"
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController,
    langVm: LanguageViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.START
    ) {
        composable(Routes.START) { StartScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController, authViewModel) }
        composable(Routes.REGISTER) { RegisterScreen(navController, authViewModel) }

        composable(
            route = "${Routes.VERIFICATION}/{email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""

            VerificationScreen(
                email = email,
                authViewModel = authViewModel,
                navController = navController
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = mainViewModel,
                onOpenReport = { reportId -> navController.navigate(Routes.reportDetail(reportId)) },
                onOpenMap = { navController.navigate(Routes.MAP) },
                onCreateReport = { timestamp -> navController.navigate(Routes.reportCreate(timestamp)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                mainViewModel = mainViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.START) { popUpTo(0) }
                },
                onLanguageChange = { langVm.setLanguageSerbian(it) }
            )
        }

        composable(
            route = "${Routes.REPORT_CREATE}/{timestamp}",
            arguments = listOf(navArgument("timestamp") { type = NavType.LongType })
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
            LocalReportCreateScreen(
                viewModel = mainViewModel,
                selectedTimestamp = timestamp,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                viewModel = mainViewModel,
                onOpenReport = { reportId -> navController.navigate(Routes.reportDetail(reportId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.REPORT_DETAIL}/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: return@composable
            LocalReportScreen(
                viewModel = mainViewModel,
                reportId = reportId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}