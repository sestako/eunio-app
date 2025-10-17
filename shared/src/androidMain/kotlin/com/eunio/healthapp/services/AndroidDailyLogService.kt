package com.eunio.healthapp.services

import android.util.Log
import com.eunio.healthapp.data.remote.FirestorePaths
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.network.RetryPolicy
import com.eunio.healthapp.network.withRetry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Android platform-specific implementation of DailyLogService.
 * 
 * ⚠️ IMPORTANT: This service is ONLY for platform-specific UI testing purposes.
 * 
 * Production code MUST use the shared Kotlin architecture:
 * - ViewModels should use shared UseCases (GetDailyLogUseCase, SaveDailyLogUseCase)
 * - UseCases delegate to LogRepository
 * - LogRepository handles offline-first architecture, conflict resolution, and Firebase sync
 * 
 * This service bypasses the repository layer and should NOT be used in production screens.
 * It is maintained for:
 * 1. Platform-specific testing (DailyLogTestScreen)
 * 2. Direct Firebase integration testing
 * 3. Debugging cross-platform sync issues
 * 
 * For production usage, see:
 * - DailyLoggingViewModel (uses shared UseCases)
 * - GetDailyLogUseCase / SaveDailyLogUseCase (uses LogRepository)
 * - LogRepositoryImpl (single source of truth for data operations)
 */
class AndroidDailyLogService : DailyLogService {
    
    companion object {
        private const val TAG = "DailyLogService"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    
    init {
        Log.i(TAG, "AndroidDailyLogService initialized")
    }
    
    override suspend fun createLog(log: DailyLog): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.AGGRESSIVE, // User data - critical operation
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying createLog (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                val logMap = dailyLogToMap(log)
                val path = FirestorePaths.dailyLogDoc(log.userId, log.id)
                // Use FirestorePaths utility for consistent path generation
                firestore.document(path)
                    .set(logMap)
                    .await()
            }
            Log.d(TAG, "Successfully created log for user ${log.userId} on ${log.date}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create log after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getLog(userId: String, logId: String): Result<DailyLog?> {
        return try {
            val log = withRetry(policy = RetryPolicy.DEFAULT) { // Read operation
                val path = FirestorePaths.dailyLogDoc(userId, logId)
                // Use FirestorePaths utility for consistent path generation
                val snapshot = firestore.document(path)
                    .get()
                    .await()
                
                if (!snapshot.exists()) {
                    return@withRetry null
                }
                
                mapToDailyLog(snapshot.data ?: return@withRetry null)
            }
            Log.d(TAG, "Successfully retrieved log $logId for user $userId")
            Result.success(log)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get log after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getLogsByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            val logs = withRetry(policy = RetryPolicy.DEFAULT) { // Read operation
                val collectionPath = FirestorePaths.dailyLogsCollection(userId)
                // Use FirestorePaths utility for consistent path generation
                val snapshot = firestore.collection(collectionPath)
                    .whereGreaterThanOrEqualTo("date", startDate.toString())
                    .whereLessThanOrEqualTo("date", endDate.toString())
                    .get()
                    .await()
                
                snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { mapToDailyLog(it) }
                }
            }
            Log.d(TAG, "Successfully retrieved ${logs.size} logs for user $userId from $startDate to $endDate")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get logs by date range after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateLog(log: DailyLog): Result<Unit> {
        return try {
            withRetry(policy = RetryPolicy.AGGRESSIVE) { // Critical write operation
                val updatedLog = log.copy(updatedAt = kotlinx.datetime.Clock.System.now())
                val logMap = dailyLogToMap(updatedLog)
                val path = FirestorePaths.dailyLogDoc(log.userId, log.id)
                // Use FirestorePaths utility for consistent path generation
                firestore.document(path)
                    .set(logMap)
                    .await()
            }
            Log.d(TAG, "Successfully updated log ${log.id} for user ${log.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update log after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteLog(userId: String, logId: String): Result<Unit> {
        return try {
            withRetry(policy = RetryPolicy.AGGRESSIVE) { // Critical delete operation
                val path = FirestorePaths.dailyLogDoc(userId, logId)
                // Use FirestorePaths utility for consistent path generation
                firestore.document(path)
                    .delete()
                    .await()
            }
            Log.d(TAG, "Successfully deleted log $logId for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log after retries", e)
            Result.failure(e)
        }
    }
    
    private fun dailyLogToMap(log: DailyLog): Map<String, Any?> {
        return mapOf(
            "id" to log.id,
            "userId" to log.userId,
            "date" to log.date.toString(),
            "periodFlow" to log.periodFlow?.name,
            "symptoms" to log.symptoms.map { it.name },
            "mood" to log.mood?.name,
            "sexualActivity" to log.sexualActivity?.let {
                mapOf(
                    "occurred" to it.occurred,
                    "protection" to it.protection?.name
                )
            },
            "bbt" to log.bbt,
            "cervicalMucus" to log.cervicalMucus?.name,
            "opkResult" to log.opkResult?.name,
            "notes" to log.notes,
            "createdAt" to log.createdAt.toEpochMilliseconds(),
            "updatedAt" to log.updatedAt.toEpochMilliseconds()
        )
    }
    
    private fun mapToDailyLog(data: Map<String, Any>): DailyLog {
        return DailyLog(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            date = LocalDate.parse(data["date"] as? String ?: ""),
            periodFlow = (data["periodFlow"] as? String)?.let { PeriodFlow.valueOf(it) },
            symptoms = (data["symptoms"] as? List<*>)?.mapNotNull { 
                (it as? String)?.let { name -> Symptom.valueOf(name) }
            } ?: emptyList(),
            mood = (data["mood"] as? String)?.let { Mood.valueOf(it) },
            sexualActivity = (data["sexualActivity"] as? Map<*, *>)?.let { sa ->
                SexualActivity(
                    occurred = sa["occurred"] as? Boolean ?: false,
                    protection = (sa["protection"] as? String)?.let { Protection.valueOf(it) }
                )
            },
            bbt = (data["bbt"] as? Number)?.toDouble(),
            cervicalMucus = (data["cervicalMucus"] as? String)?.let { CervicalMucus.valueOf(it) },
            opkResult = (data["opkResult"] as? String)?.let { OPKResult.valueOf(it) },
            notes = data["notes"] as? String,
            createdAt = Instant.fromEpochMilliseconds(data["createdAt"] as? Long ?: 0L),
            updatedAt = Instant.fromEpochMilliseconds(data["updatedAt"] as? Long ?: 0L)
        )
    }
}
