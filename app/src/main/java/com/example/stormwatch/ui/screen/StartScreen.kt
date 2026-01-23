package com.example.stormwatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun StartScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Dobrodošli u StormWatch", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.navigate("login") }) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { navController.navigate("register") }) {
            Text("Register")
        }
    }
}
