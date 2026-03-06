package com.example.stormwatch.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.stormwatch.data.repository.AuthResult
import com.example.stormwatch.ui.navigation.Routes
import com.example.stormwatch.viewmodel.AuthViewModel

@Composable
fun VerificationScreen(
    email: String,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val background = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.START) {
                    inclusive = true
                }
            }
            authViewModel.resetAuthState()
        }
    }

    Box(Modifier.fillMaxSize().background(background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Proverite inbox!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Poslali smo magični link na $email.\nKliknite na njega u vašoj email aplikaciji.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    if (authState is AuthResult.Loading) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Verifikacija u toku...", color = Color.White.copy(alpha = 0.7f))
                    } else {
                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            // Prikaz greške ako link ne valja
            if (authState is AuthResult.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = (authState as AuthResult.Error).message,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            TextButton(onClick = {
                // authViewModel.sendVerificationLink(username, email, pass, photo, context)
            }) {
                Text("Nije stigao mejl? Pošalji ponovo", color = Color.White)
            }
        }
    }
}