package com.example.stormwatch.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.stormwatch.LocalIsSerbian
import com.example.stormwatch.t
import com.example.stormwatch.viewmodel.AuthViewModel
import com.example.stormwatch.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onLanguageChange: (Boolean) -> Unit
) {
    val profile by mainViewModel.currentUserProfile.collectAsState()
    val username = profile?.username.orEmpty()
    val photoUrl = profile?.photoUrl.orEmpty()
    val isSerbianNow = LocalIsSerbian.current
    val backgroundGradient = Brush.verticalGradient(listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)))

    var uploading by remember { mutableStateOf(false) }
    var resetMsg by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploading = true
            mainViewModel.updateMyProfilePhoto(uri)
            uploading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            t(isSerbianNow,"Podešavanja", "Settings"),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = t(isSerbianNow,"Nazad", "Back"), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = t(isSerbianNow,"Profilna slika", "Profile photo"),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(30.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (username.isBlank()) t(isSerbianNow,"Učitavam...", "Loading...") else username,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    t(isSerbianNow,"Član StormWatch zajednice", "StormWatch Community Member"),
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (uploading) {
                    Spacer(Modifier.height(10.dp))
                    CircularProgressIndicator(color = Color.White)
                }

                resetMsg?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Color.White, fontSize = 12.sp)
                }

                Spacer(Modifier.height(30.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(8.dp)) {

                        SettingsRow(
                            Icons.Default.AddAPhoto,
                            t(isSerbianNow,"Promeni sliku", "Change photo")
                        ) {
                            pickImageLauncher.launch("image/*")
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        SettingsRow(
                            Icons.Default.Lock,
                            t(isSerbianNow,"Promeni lozinku (mail)", "Change password (email)")
                        ) {
                            authViewModel.sendPasswordResetToCurrentUser { ok ->
                                resetMsg =
                                    if (ok) t(isSerbianNow,"Poslat reset link na email.", "Reset link sent to email.")
                                    else t(isSerbianNow,"Ne mogu da pošaljem (nema email-a?).", "Can't send (no email?).")
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, null, tint = Color.White)
                            Spacer(Modifier.width(16.dp))
                            Text(
                                t(isSerbianNow,"Jezik", "Language"),
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isSerbianNow,
                                onCheckedChange = { onLanguageChange(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Yellow)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text(t(isSerbianNow,"Odjavi se", "Logout"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White)
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
    }
}