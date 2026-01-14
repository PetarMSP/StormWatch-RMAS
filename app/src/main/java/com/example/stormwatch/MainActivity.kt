package com.example.stormwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stormwatch.ui.navigation.AppNavigation
import com.example.stormwatch.ui.theme.StormWatchTheme
import com.example.stormwatch.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StormWatchTheme {
                val viewModel: MainViewModel = viewModel()
                AppNavigation(viewModel)
            }
        }
    }
}
