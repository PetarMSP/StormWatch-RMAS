package com.example.stormwatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.stormwatch.ui.screen.HomeScreen
import com.example.stormwatch.viewmodel.MainViewModel

@Composable
fun AppNavigation(viewModel: MainViewModel) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(viewModel)
        }
    }
}
