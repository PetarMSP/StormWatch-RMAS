package com.example.stormwatch.data.repository

import android.net.Uri
import com.example.stormwatch.data.model.domain.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = firestore.collection("users")
    private val storageRef = FirebaseStorage.getInstance().reference

    suspend fun createUserProfile(
        uid: String,
        username: String,
        photoUri: Uri?
    ) {
        val photoUrl = if (photoUri != null) {
            uploadProfilePhoto(uid, photoUri)
        } else ""

        val user = UserProfile(
            uid = uid,
            username = username,
            photoUrl = photoUrl,
            weeklyScore = 0,
            goldTrophies = 0,
            silverTrophies = 0,
            bronzeTrophies = 0
        )

        usersRef.document(uid).set(user).await()
    }

    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val ref = storageRef.child("profilePhotos/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun getUser(uid: String): UserProfile? {
        val snapshot = usersRef.document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun updatePhoto(uid: String, photoUrl: String) {
        usersRef.document(uid).update("photoUrl", photoUrl).await()
    }

    suspend fun increaseWeeklyScore(uid: String, points: Int) {
        usersRef.document(uid)
            .update("weeklyScore", com.google.firebase.firestore.FieldValue.increment(points.toLong()))
            .await()
    }
}