package com.example.stormwatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stormwatch.viewmodel.AuthViewModel
import com.example.stormwatch.data.repository.AuthResult

@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState){
        if(authState is AuthResult.Success){
            navController.navigate("home") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { authViewModel.register(email, password) }) {
            Text("Register")
        }
        Spacer(modifier = Modifier.height(8.dp))
        authState?.let {
            if(it is AuthResult.Error){
                Text(it.message, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Već imate nalog? Prijavite se")
        }
    }
}
