package com.example.stormwatch.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val userRepository: UserRepository = UserRepository()
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }
    suspend fun signUpWithProfile(
        username: String,
        email: String,
        password: String,
        photoUri: Uri?
    ): AuthResult {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = res.user?.uid ?: return AuthResult.Error("Ne mogu da dobijem UID.")

            userRepository.createUserProfile(
                uid = uid,
                username = username,
                photoUri = photoUri
            )

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun changePassword(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun mapFirebaseError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "Email ili lozinka nisu ispravni"
            is FirebaseAuthUserCollisionException -> "Nalog već postoji sa navedenim email-om"
            is FirebaseAuthWeakPasswordException -> "Lozinka je previše slaba"
            else -> "Došlo je do greške. Pokušaj ponovo."
        }
    }
}