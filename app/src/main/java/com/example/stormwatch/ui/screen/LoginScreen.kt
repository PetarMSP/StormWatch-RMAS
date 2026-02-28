package com.example.stormwatch.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stormwatch.data.repository.AuthResult
import com.example.stormwatch.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    val backgroundGradient = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))

    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            navController.navigate("home") { popUpTo("login") { inclusive = true } }
        }
    }

    Box(Modifier.fillMaxSize().background(backgroundGradient)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Dobrodošli nazad",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(20.dp)) {

                    LoginInputField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Default.Email
                    )

                    Spacer(Modifier.height(16.dp))

                    LoginInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Lozinka",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        showPass = showPass,
                        onTogglePass = { showPass = !showPass }
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { authViewModel.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2193b0)
                        ),

                        enabled = authState !is AuthResult.Loading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (authState is AuthResult.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF2193b0),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("PRIJAVI SE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }


            if (authState is AuthResult.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = (authState as AuthResult.Error).message,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = { navController.navigate("register") }) {
                Text("Nemate nalog? Registrujte se", color = Color.White)
            }
        }
    }
}

@Composable
fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    showPass: Boolean = false,
    onTogglePass: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(icon, null, tint = Color.White) },
        visualTransformation = if (isPassword && !showPass) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePass) {
                    Icon(
                        imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}