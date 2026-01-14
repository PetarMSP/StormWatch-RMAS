package com.example.stormwatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.stormwatch.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val weather by viewModel.weather.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        if(weather == null){
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp),
                   verticalArrangement = Arrangement.spacedBy(12.dp))
            {
                Text(
                    text = weather!!.city,
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "${weather!!.temperature} °C",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(text = weather!!.description)
                Text(text = "Vetar: ${weather!!.windSpeed} m/s")
                Text(text ="Vlaznost: ${weather!!.humidity} %")
            }
        }
    }
}