    package com.example.stormwatch.viewmodel

    import android.net.Uri
    import android.util.Log
    import androidx.compose.runtime.Composable
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.stormwatch.data.repository.AuthRepository
    import com.example.stormwatch.data.repository.AuthResult
    import com.example.stormwatch.data.repository.UserRepository
    import com.google.firebase.Firebase
    import com.google.firebase.auth.ActionCodeSettings
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.auth
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await

    class AuthViewModel : ViewModel() {
        private val repository = AuthRepository()

        private val _authState = MutableStateFlow<AuthResult?>(null)
        val authState: StateFlow<AuthResult?> = _authState

        private val _currentUserId = MutableStateFlow(repository.currentUserId())

        //val currentUserId: StateFlow<String?> = _currentUserId
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://stormwatch-appv1.firebaseapp.com/")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.example.stormwatch",
                true, // installIfNotAvailable
                "12"   // minimum version
            )
            .build()

        fun login(email: String, password: String) {
            viewModelScope.launch {
                val result = repository.login(email, password)
                _authState.value = result
                if (result is AuthResult.Success) {
                    _currentUserId.value = repository.currentUserId()
                }
            }
        }
        fun sendVerificationLink(
            username: String,
            email: String,
            password: String,
            photoUri: Uri?,
            context: android.content.Context
        ) {
            val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("pending_email", email)
            editor.putString("pending_username", username)
            editor.putString("pending_password", password)
            editor.putString("pending_photo", photoUri?.toString())
            editor.apply()

            viewModelScope.launch {
                _authState.value = AuthResult.Loading
                val result = repository.sendSignInLink(email, actionCodeSettings)
                _authState.value = result
            }
        }

        fun isSignInWithEmailLink(link: String): Boolean {
            return repository.isSignInWithEmailLink(link)
        }

        fun completeSignInWithLink(email: String, link: String, context: android.content.Context) {
            viewModelScope.launch {
                _authState.value = AuthResult.Loading
                try {
                    val result = Firebase.auth.signInWithEmailLink(email, link).await()
                    val uid = result.user?.uid ?: throw Exception("UID nije pronađen")

                    val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                    val savedUsername = prefs.getString("pending_username", email.substringBefore("@")) ?: "User"
                    val savedPassword = prefs.getString("pending_password", "") ?: ""
                    val savedPhotoStr = prefs.getString("pending_photo", null)
                    val savedPhotoUri = if (savedPhotoStr != null) Uri.parse(savedPhotoStr) else null

                    val signUpResult = repository.signUpWithProfile(
                        username = savedUsername,
                        password = savedPassword,
                        photoUri = savedPhotoUri,
                        uid = uid
                    )

                    if (signUpResult is AuthResult.Success) {
                        _currentUserId.value = uid
                        _authState.value = AuthResult.Success
                        prefs.edit().clear().apply()
                        Log.d("AUTH_DEBUG", "Uspešno kreiran profil sa lozinkom!")
                    } else if (signUpResult is AuthResult.Error) {
                        _authState.value = signUpResult
                    }

                } catch (e: Exception) {
                    Log.e("AUTH_DEBUG", "Greška: ${e.message}")
                    _authState.value = AuthResult.Error(e.message ?: "Greška")
                }
            }
        }
        fun sendPasswordResetToCurrentUser(onDone: (Boolean) -> Unit) {
            val email = FirebaseAuth.getInstance().currentUser?.email

            if (email.isNullOrBlank()) {
                Log.w("AUTH_DEBUG", "Pokušaj reseta lozinke, ali email trenutnog korisnika je null.")
                onDone(false)
                return
            }

            viewModelScope.launch {
                Log.d("AUTH_DEBUG", "Pokrećem reset lozinke za: $email")

                val res = repository.changePassword(email)

                if (res is AuthResult.Success) {
                    Log.d("AUTH_DEBUG", "Uspešno poslat zahtev Firebase-u.")
                    onDone(true)
                } else if (res is AuthResult.Error) {
                    Log.e("AUTH_DEBUG", "Greška pri resetu: ${res.message}")
                    onDone(false)
                }
            }
        }
        fun logout() {
            viewModelScope.launch {
                repository.logout()
                _currentUserId.value = null
            }
        }

        fun resetAuthState() {
            _authState.value = null
        }
    }