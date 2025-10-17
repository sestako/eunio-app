package com.eunio.healthapp.data.sync

import com.eunio.healthapp.domain.util.StructuredLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of DailyLogMigration using Firebase Firestore SDK.
 */
class AndroidDailyLogMigration(
    private val firestore: FirebaseFirestore
) : BaseDailyLogMigration() {
    
    override suspend fun queryLegacyCollection(userId: String): Map<String, Map<String, Any?>> {
        val documents = mutableMapOf<String, Map<String, Any?>>()
        
        try {
            // Query legacy path: daily_logs/{userId}/logs/
            val querySnapshot = firestore.collection("daily_logs")
                .document(userId)
                .collection("logs")
                .get()
                .await()
            
            for (document in querySnapshot.documents) {
                val data = document.data
                if (data != null) {
                    documents[document.id] = data
                }
            }
        } catch (e: Exception) {
            StructuredLogger.logStructured(logTag, "QUERY_LEGACY_ERROR", mapOf(
                "userId" to userId,
                "error" to (e.message ?: "Unknown error")
            ))
            throw e
        }
        
        return documents
    }
    
    override suspend fun checkDocumentExists(userId: String, logId: String): Boolean {
        return try {
            // Check new path: users/{userId}/dailyLogs/{logId}
            val document = firestore.collection("users")
                .document(userId)
                .collection("dailyLogs")
                .document(logId)
                .get()
                .await()
            
            document.exists()
        } catch (e: Exception) {
            StructuredLogger.logStructured(logTag, "CHECK_EXISTS_ERROR", mapOf(
                "userId" to userId,
                "logId" to logId,
                "error" to (e.message ?: "Unknown error")
            ))
            false // Assume doesn't exist on error to attempt migration
        }
    }
    
    override suspend fun copyDocument(
        userId: String,
        logId: String,
        documentData: Map<String, Any?>
    ) {
        try {
            // Write to new path: users/{userId}/dailyLogs/{logId}
            firestore.collection("users")
                .document(userId)
                .collection("dailyLogs")
                .document(logId)
                .set(documentData)
                .await()
            
            StructuredLogger.logStructured(logTag, "COPY_SUCCESS", mapOf(
                "userId" to userId,
                "logId" to logId,
                "fieldCount" to documentData.size
            ))
        } catch (e: Exception) {
            StructuredLogger.logStructured(logTag, "COPY_ERROR", mapOf(
                "userId" to userId,
                "logId" to logId,
                "error" to (e.message ?: "Unknown error")
            ))
            throw e
        }
    }
}
