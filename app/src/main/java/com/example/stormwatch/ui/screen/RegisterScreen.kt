package com.example.stormwatch.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.stormwatch.data.repository.AuthResult
import com.example.stormwatch.util.createImageUri
import com.example.stormwatch.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, authViewModel: AuthViewModel) {

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    val backgroundGradient = Brush.verticalGradient(
        listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) photoUri = cameraTempUri
        cameraTempUri = null
    }

    LaunchedEffect(authState) {
        if (authState is AuthResult.Success) {
            navController.navigate("home") { popUpTo("register") { inclusive = true } }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Napravite nalog",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )

                Text(
                    text = "Pridružite se StormWatch zajednici",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(32.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { showPicker = true },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        if (photoUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = Color(0xFF2193b0)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        RegisterInputField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Korisničko ime",
                            icon = Icons.Default.Person
                        )

                        Spacer(Modifier.height(16.dp))

                        RegisterInputField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email adresa",
                            icon = Icons.Default.Email
                        )

                        Spacer(Modifier.height(16.dp))

                        RegisterInputField(
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
                            onClick = { authViewModel.register(username, email, password, photoUri) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF2193b0)
                            ),
                            enabled = authState !is AuthResult.Loading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                        ) {
                            if (authState is AuthResult.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF2193b0),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("REGISTRUJ SE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                authState?.let {
                    if (it is AuthResult.Error) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                it.message,
                                color = Color.White,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Već imate nalog? Prijavite se", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Dodaj sliku") },
            text = { Text("Izaberi izvor profilne slike") },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galerija") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPicker = false
                    cameraTempUri = createImageUri(context)
                    cameraLauncher.launch(cameraTempUri!!)
                }) { Text("Kamera") }
            }
        )
    }
}

@Composable
fun RegisterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
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
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            cursorColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}