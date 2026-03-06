package com.example.stormwatch.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object LinkSent : AuthResult()
    data object Loading : AuthResult()
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
    suspend  fun sendSignInLink(email: String, settings: ActionCodeSettings): AuthResult {
        return try {
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)

            auth.sendSignInLinkToEmail(email, settings).await()
            AuthResult.LinkSent
        } catch (e: Exception) {
            android.util.Log.e("AUTH_DEBUG", "Greška pri slanju: ${e.message}", e)
            AuthResult.Error(e.message ?: "Greška pri slanju linka")
        }
    }
    fun isSignInWithEmailLink(link: String): Boolean {
        return auth.isSignInWithEmailLink(link)
    }


    suspend fun signUpWithProfile(
        username: String,
        password: String,
        photoUri: Uri?,
        uid: String
    ): AuthResult {
        return try {
            if (password.isNotBlank()) {
                auth.currentUser?.updatePassword(password)?.await()
                Log.d("AUTH_DEBUG", "Lozinka uspešno upisana u Firebase Auth sistem.")
            }

            userRepository.createUserProfile(
                uid = uid,
                username = username,
                photoUri = photoUri
            )

            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AUTH_DEBUG", "Greška pri upisu lozinke: ${e.message}")
            AuthResult.Error(e.message ?: "Greška pri upisu u bazu")
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun changePassword(email: String): AuthResult {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "Firebase error: ${e.message}")
            AuthResult.Error(e.message ?: "Nepoznata greška")
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
    fun currentUserIdFlow(): Flow<String?> = callbackFlow {
        val auth = FirebaseAuth.getInstance()

        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)

        awaitClose { auth.removeAuthStateListener(listener) }
    }
}