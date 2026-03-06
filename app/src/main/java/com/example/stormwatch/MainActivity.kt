package com.example.stormwatch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.stormwatch.ui.navigation.AppNavigation
import com.example.stormwatch.ui.navigation.Routes
import com.example.stormwatch.ui.theme.StormWatchTheme
import com.example.stormwatch.viewmodel.AuthViewModel
import com.example.stormwatch.viewmodel.LanguageViewModel
import com.example.stormwatch.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp

val LocalIsSerbian = compositionLocalOf { true }
fun t(isSerbian: Boolean, sr: String, en: String) = if (isSerbian) sr else en

class MainActivity : ComponentActivity() {

    // Ova varijabla služi kao "most" između Android sistema i Compose-a
    private var intentData = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Kada stigne novi link, ažuriramo State i Compose to odmah vidi
        intentData.value = intent.dataString
        Log.d("AUTH_DEBUG", "onNewIntent POZVAN! New Data: ${intent.dataString}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Inicijalizujemo vrednost pri paljenju (ako je link pokrenuo ugašenu aplikaciju)
        intentData.value = intent.dataString

        createNotificationChannel(this)
        Log.d("AUTH_DEBUG", "onCreate Intent Data: ${intent?.data}")

        setContent {
            StormWatchTheme {
                val mainViewModel: MainViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()
                val langVm: LanguageViewModel = viewModel()

                val isSerbian by langVm.isSerbian.collectAsState()
                val navController = rememberNavController()

                LaunchedEffect(intentData.value) {
                    val currentLink = intentData.value
                    Log.d("AUTH_DEBUG", "LaunchedEffect proverava data: $currentLink")

                    if (currentLink != null && authViewModel.isSignInWithEmailLink(currentLink)) {
                        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                        val savedEmail = prefs.getString("pending_email", null)

                        Log.d("AUTH_DEBUG", "Pokušavam login za: $savedEmail")

                        if (!savedEmail.isNullOrBlank()) {
                            authViewModel.completeSignInWithLink(
                                email = savedEmail,
                                link = currentLink,
                                context = this@MainActivity
                            )
                            // Čistimo link nakon pokušaja
                            intentData.value = null
                        } else {
                            Log.e("AUTH_DEBUG", "Greška: Email nije pronađen u SharedPreferences!")
                        }
                    }
                }

                // Poseban LaunchedEffect za notifikacije/reportId
                LaunchedEffect(intent) {
                    val reportId = intent.getStringExtra("OPEN_REPORT_ID")
                    if (reportId != null) {
                        navController.navigate("${Routes.REPORT_DETAIL}/$reportId")
                        intent.removeExtra("OPEN_REPORT_ID")
                    }
                }

                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    mainViewModel.startObservingNewReports(context)
                }

                CompositionLocalProvider(LocalIsSerbian provides isSerbian) {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        authViewModel = authViewModel,
                        langVm = langVm,
                        navController = navController
                    )
                }
            }
        }
    }

    private fun createNotificationChannel(activity: MainActivity) {
        com.example.stormwatch.util.createNotificationChannel(activity)
    }
}