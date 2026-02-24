package com.example.stormwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stormwatch.ui.navigation.AppNavigation
import com.example.stormwatch.ui.theme.StormWatchTheme
import com.example.stormwatch.viewmodel.MainViewModel
import com.example.stormwatch.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        setContent {
            StormWatchTheme {

                val mainViewModel: MainViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()

                AppNavigation(mainViewModel, authViewModel)
            }
        }
    }

    private fun createNotificationChannel(activity: MainActivity) {
        // Tvoja logika za notifikacije
    }
}

