package com.example.stormwatch.data.repository

import com.example.stormwatch.data.model.LocalWeatherReport
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
class LocalReportRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val reportsRef = firestore.collection("local_reports")

    suspend fun addReport(report: LocalWeatherReport) {
        val docRef = reportsRef.document()
        val reportWithId = report.copy(id = docRef.id)
        docRef.set(reportWithId).await()
    }

    suspend fun getReportById(reportId: String): LocalWeatherReport? {
        val snapshot = reportsRef.document(reportId).get().await()
        return snapshot.toObject(LocalWeatherReport::class.java)?.copy(id = snapshot.id)
    }

    suspend fun like(reportId: String, userId: String?) {
        if (userId == null) return

        val reportDoc = reportsRef.document(reportId)
        val voteDoc = reportDoc.collection("votes").document(userId)

        firestore.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteDoc)
            if (!voteSnapshot.exists()) {
                transaction.set(voteDoc, mapOf("type" to "like"))
                transaction.update(
                    reportDoc,
                    "likes",
                    com.google.firebase.firestore.FieldValue.increment(1)
                )
            }
        }.await()
    }

    suspend fun dislike(reportId: String, userId: String?) {
        if (userId == null) return

        val reportDoc = reportsRef.document(reportId)
        val voteDoc = reportDoc.collection("votes").document(userId)

        firestore.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteDoc)
            if (!voteSnapshot.exists()) {
                transaction.set(voteDoc, mapOf("type" to "dislike"))
                transaction.update(
                    reportDoc,
                    "dislikes",
                    com.google.firebase.firestore.FieldValue.increment(1)
                )
            }
        }.await()
    }

    suspend fun delete(reportId: String) {
        reportsRef.document(reportId).delete().await()
    }

    fun getActiveReports(): Flow<List<LocalWeatherReport>> = callbackFlow {
        val listener = reportsRef
            .whereEqualTo("active", true)
            .whereEqualTo("isProcessed", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }

                val now = System.currentTimeMillis()

                val reports = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(LocalWeatherReport::class.java)?.copy(id = doc.id)
                    }
                    ?.filter { r ->
                        val endExclusive = r.startTime + (r.durationHours.toLong() * 60L * 60L * 1000L)
                        endExclusive > now
                    }
                    ?: emptyList()

                trySend(reports)
            }

        awaitClose { listener.remove() }
    }
}