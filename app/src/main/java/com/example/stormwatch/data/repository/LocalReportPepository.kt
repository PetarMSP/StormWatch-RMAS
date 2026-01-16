package com.example.stormwatch.data.repository

import com.example.stormwatch.data.model.LocalWeatherReport
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocalReportPepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val reportsRef = firestore.collection("localReports")

    fun addReport(report: LocalWeatherReport){
        val userId = auth.currentUser?.uid ?: return

        val newReport = report.copy(
            userID = userId
        )

        reportsRef.add(newReport)
    }

    fun getActiveReports(): Flow<List<LocalWeatherReport>> = callbackFlow{
         val now = System.currentTimeMillis()

        val listener = reportsRef.whereGreaterThan("endTime",now)
            .addSnapshotListener { snapshot,error ->
                if(error != null){
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.toObjects(LocalWeatherReport::class.java) ?: emptyList()
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }

}