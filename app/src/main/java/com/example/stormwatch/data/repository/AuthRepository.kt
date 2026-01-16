package com.example.stormwatch.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Success: AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    //Login
    suspend fun login(email: String,password: String): AuthResult{
        return try {
            auth.signInWithEmailAndPassword(email,password).await()
            AuthResult.Success
        }catch (e: Exception){
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun signUp(email: String,password: String): AuthResult{
        return try {
            auth.createUserWithEmailAndPassword(email,password).await()
            AuthResult.Success
        }catch (e: Exception){
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    suspend fun logout(){
        auth.signOut()
    }

    suspend fun changePassword(email: String): AuthResult{
        return try{
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        }catch (e: Exception){
            AuthResult.Error(mapFirebaseError(e))
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid
    private fun mapFirebaseError(e: Exception): String{
        return when (e){
            is FirebaseAuthInvalidCredentialsException ->
                "Email ili lozinka nisu ispravni"
            is FirebaseAuthUserCollisionException ->
                "Nalog vec postoji sa navedenim email-om"
            is FirebaseAuthWeakPasswordException ->
                "Lozinka je previse slaba"
            else -> "Doslo je do greske.Molim vas pokusajte opet"
        }
    }

}