package com.example.stormwatch.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stormwatch.data.repository.AuthRepository
import com.example.stormwatch.data.repository.AuthResult
import com.google.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthResult?>(null)
    val authState: StateFlow<AuthResult?> = _authState

    private val _currentUserId = MutableStateFlow(repository.currentUserId())

    val currentUserId: StateFlow<String?> = _currentUserId
    val actionCodeSettings = ActionCodeSettings.newBuilder()
        .setUrl("https://stormwatch-appv1.web.app/verify")
        .setHandleCodeInApp(true)
        .setAndroidPackageName(
            "com.example.stormwatch",
            true,
            "12"
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

    fun register(username: String, email: String, password: String, photoUri: Uri?) {
        viewModelScope.launch {
            _authState.value = null
            val result = repository.signUpWithProfile(username, email, password, photoUri)
            _authState.value = result
            if (result is AuthResult.Success) {
                _currentUserId.value = repository.currentUserId()
            }
        }
    }

    fun sendVerificationLink(email: String) {
        viewModelScope.launch {
            _authState.value = AuthResult.Loading
            val result = repository.sendSignInLink(email, actionCodeSettings)
            _authState.value = result
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