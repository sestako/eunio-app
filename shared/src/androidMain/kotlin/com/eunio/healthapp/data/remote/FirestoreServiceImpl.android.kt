package com.eunio.healthapp.data.remote

import com.eunio.healthapp.data.remote.dto.*
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

/**
 * Android implementation of FirestoreService using Firebase Firestore SDK.
 * Provides CRUD operations with proper error handling and data transformation.
 */
class FirestoreServiceImpl(
    private val firestore: FirebaseFirestore,
    private val errorHandler: ErrorHandler,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FirestoreService {
    
    companion object {
        // Note: Path generation now uses FirestorePaths utility for consistency
        // These constants are kept for backward compatibility but should not be used directly
        @Deprecated("Use FirestorePaths utility instead")
        private const val USERS_COLLECTION = "users"
        @Deprecated("Use FirestorePaths utility instead")
        private const val CYCLES_COLLECTION = "cycles"
        @Deprecated("Use FirestorePaths utility instead")
        private const val DAILY_LOGS_COLLECTION = "dailyLogs"
        @Deprecated("Use FirestorePaths utility instead")
        private const val INSIGHTS_COLLECTION = "insights"
        @Deprecated("Use FirestorePaths utility instead")
        private const val SYNC_METADATA_COLLECTION = "syncMetadata"
    }
    
    // User operations
    override suspend fun getUser(userId: String): Result<User?> {
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userDto = document.toObject(UserDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize user document for ID: $userId")
                userDto.toDomain(document.id)
            } else {
                null
            }
        }
    }
    
    override suspend fun saveUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            if (user.id.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            if (user.email.isBlank()) {
                throw IllegalArgumentException("User email cannot be blank")
            }
            
            val userDto = UserDto.fromDomain(user)
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userDto)
                .await()
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            val userDto = UserDto.fromDomain(user)
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userDto)
                .await()
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            val batch = firestore.batch()
            
            // Delete user document
            val userRef = firestore.collection(USERS_COLLECTION).document(userId)
            batch.delete(userRef)
            
            // Delete all user's subcollections
            deleteUserSubcollections(batch, userId)
            
            batch.commit().await()
        }
    }
    
    // Cycle operations
    override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CYCLES_COLLECTION)
                .document(cycleId)
                .get()
                .await()
            
            if (document.exists()) {
                val cycleDto = document.toObject(CycleDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize cycle document")
                cycleDto.toDomain(document.id, userId)
            } else {
                null
            }
        }
    }
    
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CYCLES_COLLECTION)
                .whereEqualTo("endDate", null)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val cycleDto = document.toObject(CycleDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize cycle document")
                cycleDto.toDomain(document.id, userId)
            } else {
                null
            }
        }
    }
    
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CYCLES_COLLECTION)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(CycleDto::class.java)?.toDomain(document.id, userId)
            }
        }
    }
    
    override suspend fun saveCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            val cycleDto = CycleDto.fromDomain(cycle)
            firestore.collection(USERS_COLLECTION)
                .document(cycle.userId)
                .collection(CYCLES_COLLECTION)
                .document(cycle.id)
                .set(cycleDto)
                .await()
        }
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            val cycleDto = CycleDto.fromDomain(cycle)
            firestore.collection(USERS_COLLECTION)
                .document(cycle.userId)
                .collection(CYCLES_COLLECTION)
                .document(cycle.id)
                .set(cycleDto)
                .await()
        }
    }
    
    override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CYCLES_COLLECTION)
                .document(cycleId)
                .delete()
                .await()
        }
    }
    
    // DailyLog operations
    override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> {
        return Result.catching(errorHandler) {
            val path = FirestorePaths.dailyLogDoc(userId, logId)
            val pathParts = path.split("/")
            
            val document = firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .document(pathParts[3])
                .get()
                .await()
            
            if (document.exists()) {
                val logDto = document.toObject(DailyLogDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize daily log document")
                logDto.toDomain(document.id, userId)
            } else {
                null
            }
        }
    }
    
    override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
        val epochDays = date.toEpochDays().toLong()
        val collectionPath = FirestorePaths.dailyLogsCollection(userId)
        android.util.Log.d("FirestoreService.Android", "GET_DAILY_LOG_BY_DATE_START - userId: $userId, date: $date, dateEpochDays: $epochDays, path: $collectionPath")
        
        return try {
            val pathParts = collectionPath.split("/")
            
            val querySnapshot = firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .whereEqualTo("dateEpochDays", epochDays)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val logDto = document.toObject(DailyLogDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize daily log document")
                val dailyLog = logDto.toDomain(document.id, userId)
                android.util.Log.d("FirestoreService.Android", "GET_DAILY_LOG_BY_DATE_SUCCESS - userId: $userId, logId: ${dailyLog.id}, dateEpochDays: $epochDays")
                Result.success(dailyLog)
            } else {
                android.util.Log.d("FirestoreService.Android", "GET_DAILY_LOG_BY_DATE_NOT_FOUND - userId: $userId, dateEpochDays: $epochDays")
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService.Android", "GET_DAILY_LOG_BY_DATE_ERROR - userId: $userId, dateEpochDays: $epochDays, error: ${e.message}", e)
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.catching(errorHandler) {
            val startEpochDays = startDate.toEpochDays().toLong()
            val endEpochDays = endDate.toEpochDays().toLong()
            val collectionPath = FirestorePaths.dailyLogsCollection(userId)
            val pathParts = collectionPath.split("/")
            
            val querySnapshot = firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .whereGreaterThanOrEqualTo("dateEpochDays", startEpochDays)
                .whereLessThanOrEqualTo("dateEpochDays", endEpochDays)
                .orderBy("dateEpochDays", Query.Direction.DESCENDING)
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(DailyLogDto::class.java)?.toDomain(document.id, userId)
            }
        }
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        return Result.catching(errorHandler) {
            val collectionPath = FirestorePaths.dailyLogsCollection(userId)
            val pathParts = collectionPath.split("/")
            
            val querySnapshot = firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .orderBy("dateEpochDays", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(DailyLogDto::class.java)?.toDomain(document.id, userId)
            }
        }
    }
    
    override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
        val path = FirestorePaths.dailyLogDoc(dailyLog.userId, dailyLog.id)
        android.util.Log.d("FirestoreService.Android", "SAVE_DAILY_LOG_START - userId: ${dailyLog.userId}, logId: ${dailyLog.id}, path: $path, dateEpochDays: ${dailyLog.date.toEpochDays()}")
        
        return try {
            val logDto = DailyLogDto.fromDomain(dailyLog)
            val pathParts = path.split("/")
            
            android.util.Log.d("FirestoreService.Android", "SAVE_DAILY_LOG_DTO - logId: ${logDto.logId}, dateEpochDays: ${logDto.dateEpochDays}, createdAt: ${logDto.createdAt}, updatedAt: ${logDto.updatedAt}")
            
            // Convert DTO to Map for Firebase (Firebase doesn't work well with Kotlin data classes directly)
            val data = hashMapOf<String, Any?>(
                "logId" to logDto.logId,
                "dateEpochDays" to logDto.dateEpochDays,
                "createdAt" to logDto.createdAt,
                "updatedAt" to logDto.updatedAt,
                "v" to logDto.v
            )
            
            // Add optional fields only if not null
            logDto.periodFlow?.let { data["periodFlow"] = it }
            logDto.symptoms?.let { data["symptoms"] = it }
            logDto.mood?.let { data["mood"] = it }
            logDto.bbt?.let { data["bbt"] = it }
            logDto.cervicalMucus?.let { data["cervicalMucus"] = it }
            logDto.opkResult?.let { data["opkResult"] = it }
            logDto.notes?.let { data["notes"] = it }
            logDto.sexualActivity?.let { 
                data["sexualActivity"] = hashMapOf<String, Any?>(
                    "occurred" to it.occurred,
                    "protection" to it.protection
                )
            }
            
            android.util.Log.d("FirestoreService.Android", "🔵 About to call firestore.set() with Map - path: ${pathParts.joinToString("/")}")
            
            // Try using Dispatchers.IO to ensure we're not blocking the main thread
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                withTimeout(10000) { // 10 second timeout
                    firestore.collection(pathParts[0])
                        .document(pathParts[1])
                        .collection(pathParts[2])
                        .document(pathParts[3])
                        .set(data)
                        .await()
                }
            }
            
            android.util.Log.d("FirestoreService.Android", "SAVE_DAILY_LOG_SUCCESS - userId: ${dailyLog.userId}, logId: ${dailyLog.id}, updatedAt: ${dailyLog.updatedAt.epochSeconds}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreService.Android", "SAVE_DAILY_LOG_ERROR - userId: ${dailyLog.userId}, logId: ${dailyLog.id}, error: ${e.message}", e)
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> {
        return Result.catching(errorHandler) {
            val logDto = DailyLogDto.fromDomain(dailyLog)
            val path = FirestorePaths.dailyLogDoc(dailyLog.userId, dailyLog.id)
            val pathParts = path.split("/")
            
            firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .document(pathParts[3])
                .set(logDto)
                .await()
        }
    }
    
    override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            val path = FirestorePaths.dailyLogDoc(userId, logId)
            val pathParts = path.split("/")
            
            firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .document(pathParts[3])
                .delete()
                .await()
        }
    }
    
    // Insight operations
    override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> {
        return Result.catching(errorHandler) {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .document(insightId)
                .get()
                .await()
            
            if (document.exists()) {
                val insightDto = document.toObject(InsightDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize insight document")
                insightDto.toDomain(document.id, userId)
            } else {
                null
            }
        }
    }
    
    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .whereEqualTo("isRead", false)
                .orderBy("generatedDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(InsightDto::class.java)?.toDomain(document.id, userId)
            }
        }
    }
    
    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .orderBy("generatedDate", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                document.toObject(InsightDto::class.java)?.toDomain(document.id, userId)
            }
        }
    }
    
    override suspend fun saveInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            val insightDto = InsightDto.fromDomain(insight)
            firestore.collection(USERS_COLLECTION)
                .document(insight.userId)
                .collection(INSIGHTS_COLLECTION)
                .document(insight.id)
                .set(insightDto)
                .await()
        }
    }
    
    override suspend fun updateInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            val insightDto = InsightDto.fromDomain(insight)
            firestore.collection(USERS_COLLECTION)
                .document(insight.userId)
                .collection(INSIGHTS_COLLECTION)
                .document(insight.id)
                .set(insightDto)
                .await()
        }
    }
    
    override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .document(insightId)
                .update("isRead", true)
                .await()
        }
    }
    
    override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .document(insightId)
                .delete()
                .await()
        }
    }
    
    // Batch operations for sync
    override suspend fun batchSaveUsers(users: List<User>): Result<Unit> {
        return Result.catching(errorHandler) {
            val batch = firestore.batch()
            
            users.forEach { user ->
                val userDto = UserDto.fromDomain(user)
                val userRef = firestore.collection(USERS_COLLECTION).document(user.id)
                batch.set(userRef, userDto)
            }
            
            batch.commit().await()
        }
    }
    
    override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> {
        return Result.catching(errorHandler) {
            val batch = firestore.batch()
            
            cycles.forEach { cycle ->
                val cycleDto = CycleDto.fromDomain(cycle)
                val cycleRef = firestore.collection(USERS_COLLECTION)
                    .document(cycle.userId)
                    .collection(CYCLES_COLLECTION)
                    .document(cycle.id)
                batch.set(cycleRef, cycleDto)
            }
            
            batch.commit().await()
        }
    }
    
    override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> {
        return Result.catching(errorHandler) {
            val batch = firestore.batch()
            
            logs.forEach { log ->
                val logDto = DailyLogDto.fromDomain(log)
                val path = FirestorePaths.dailyLogDoc(log.userId, log.id)
                val pathParts = path.split("/")
                
                val logRef = firestore.collection(pathParts[0])
                    .document(pathParts[1])
                    .collection(pathParts[2])
                    .document(pathParts[3])
                batch.set(logRef, logDto)
            }
            
            batch.commit().await()
        }
    }
    
    override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> {
        return Result.catching(errorHandler) {
            val batch = firestore.batch()
            
            insights.forEach { insight ->
                val insightDto = InsightDto.fromDomain(insight)
                val insightRef = firestore.collection(USERS_COLLECTION)
                    .document(insight.userId)
                    .collection(INSIGHTS_COLLECTION)
                    .document(insight.id)
                batch.set(insightRef, insightDto)
            }
            
            batch.commit().await()
        }
    }
    
    // Sync operations
    override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> {
        return Result.catching(errorHandler) {
            val document = firestore.collection(SYNC_METADATA_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                document.getLong("lastSyncTimestamp")
            } else {
                null
            }
        }
    }
    
    override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(SYNC_METADATA_COLLECTION)
                .document(userId)
                .set(mapOf("lastSyncTimestamp" to timestamp))
                .await()
        }
    }
    
    override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData> {
        return Result.catching(errorHandler) {
            val users = mutableListOf<User>()
            val cycles = mutableListOf<Cycle>()
            val dailyLogs = mutableListOf<DailyLog>()
            val insights = mutableListOf<Insight>()
            
            // Get user if updated since timestamp
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val userDto = userDoc.toObject(UserDto::class.java)
                if (userDto != null && userDto.updatedAt > timestamp) {
                    users.add(userDto.toDomain(userDoc.id))
                }
            }
            
            // Get cycles updated since timestamp - use server timestamp for better sync
            val cyclesQuery = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(CYCLES_COLLECTION)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            cyclesQuery.documents.forEach { doc ->
                val cycleDto = doc.toObject(CycleDto::class.java)
                if (cycleDto != null) {
                    cycles.add(cycleDto.toDomain(doc.id, userId))
                }
            }
            
            // Get daily logs updated since timestamp with proper ordering
            val collectionPath = FirestorePaths.dailyLogsCollection(userId)
            val pathParts = collectionPath.split("/")
            
            val logsQuery = firestore.collection(pathParts[0])
                .document(pathParts[1])
                .collection(pathParts[2])
                .whereGreaterThan("updatedAt", timestamp)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            logsQuery.documents.forEach { doc ->
                val logDto = doc.toObject(DailyLogDto::class.java)
                if (logDto != null) {
                    dailyLogs.add(logDto.toDomain(doc.id, userId))
                }
            }
            
            // Get insights updated since timestamp
            val insightsQuery = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INSIGHTS_COLLECTION)
                .whereGreaterThan("generatedDate", timestamp)
                .orderBy("generatedDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            insightsQuery.documents.forEach { doc ->
                val insightDto = doc.toObject(InsightDto::class.java)
                if (insightDto != null) {
                    insights.add(insightDto.toDomain(doc.id, userId))
                }
            }
            
            SyncData(
                users = users,
                cycles = cycles,
                dailyLogs = dailyLogs,
                insights = insights,
                deletedDocuments = emptyList() // Would need separate tracking for deletions
            )
        }
    }
    
    /**
     * Enhanced sync operation with conflict resolution strategy.
     * Implements last-write-wins with user notification for critical conflicts.
     */
    suspend fun syncWithConflictResolution(
        userId: String,
        localData: SyncData,
        lastSyncTimestamp: Long
    ): Result<SyncConflictResult> {
        return Result.catching(errorHandler) {
            val remoteChanges = getChangedDocumentsSince(userId, lastSyncTimestamp).getOrThrow()
            val conflicts = mutableListOf<SyncConflict>()
            
            // Detect conflicts in daily logs (most critical for user data)
            localData.dailyLogs.forEach { localLog ->
                val remoteLog = remoteChanges.dailyLogs.find { it.id == localLog.id }
                if (remoteLog != null && remoteLog.updatedAt != localLog.updatedAt) {
                    conflicts.add(
                        SyncConflict(
                            type = SyncConflictType.DAILY_LOG,
                            localItem = localLog,
                            remoteItem = remoteLog,
                            conflictField = detectConflictFields(localLog, remoteLog)
                        )
                    )
                }
            }
            
            // Apply conflict resolution strategy
            val resolvedData = resolveConflicts(localData, remoteChanges, conflicts)
            
            // Batch upload resolved data
            batchSaveDailyLogs(resolvedData.dailyLogs).getOrThrow()
            batchSaveCycles(resolvedData.cycles).getOrThrow()
            batchSaveInsights(resolvedData.insights).getOrThrow()
            
            SyncConflictResult(
                resolvedData = resolvedData,
                conflicts = conflicts,
                resolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
            )
        }
    }
    
    private fun detectConflictFields(local: DailyLog, remote: DailyLog): List<String> {
        val conflicts = mutableListOf<String>()
        
        if (local.periodFlow != remote.periodFlow) conflicts.add("periodFlow")
        if (local.symptoms != remote.symptoms) conflicts.add("symptoms")
        if (local.mood != remote.mood) conflicts.add("mood")
        if (local.bbt != remote.bbt) conflicts.add("bbt")
        if (local.cervicalMucus != remote.cervicalMucus) conflicts.add("cervicalMucus")
        if (local.opkResult != remote.opkResult) conflicts.add("opkResult")
        if (local.notes != remote.notes) conflicts.add("notes")
        
        return conflicts
    }
    
    private fun resolveConflicts(
        localData: SyncData,
        remoteData: SyncData,
        conflicts: List<SyncConflict>
    ): SyncData {
        // Last-write-wins strategy: use the item with the most recent updatedAt timestamp
        val resolvedLogs = mutableListOf<DailyLog>()
        
        // Merge local and remote logs, resolving conflicts
        val allLogIds = (localData.dailyLogs.map { it.id } + remoteData.dailyLogs.map { it.id }).toSet()
        
        allLogIds.forEach { logId ->
            val localLog = localData.dailyLogs.find { it.id == logId }
            val remoteLog = remoteData.dailyLogs.find { it.id == logId }
            
            when {
                localLog != null && remoteLog != null -> {
                    // Conflict: use most recent
                    resolvedLogs.add(
                        if (localLog.updatedAt > remoteLog.updatedAt) localLog else remoteLog
                    )
                }
                localLog != null -> resolvedLogs.add(localLog)
                remoteLog != null -> resolvedLogs.add(remoteLog)
            }
        }
        
        return SyncData(
            users = if (remoteData.users.isNotEmpty()) remoteData.users else localData.users,
            cycles = remoteData.cycles.ifEmpty { localData.cycles },
            dailyLogs = resolvedLogs,
            insights = remoteData.insights.ifEmpty { localData.insights },
            deletedDocuments = remoteData.deletedDocuments
        )
    }
    
    // Health Report operations
    override suspend fun getHealthReport(reportId: String): Result<HealthReport?> {
        return Result.catching(errorHandler) {
            val document = firestore.collection("healthReports")
                .document(reportId)
                .get()
                .await()
            
            if (document.exists()) {
                val reportDto = document.toObject(HealthReportDto::class.java)
                    ?: throw IllegalStateException("Failed to deserialize health report document for ID: $reportId")
                reportDto.toDomain(document.id)
            } else {
                null
            }
        }
    }

    override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> {
        return Result.catching(errorHandler) {
            val query = firestore.collection("healthReports")
                .whereEqualTo("userId", userId)
                .orderBy("generatedDate", Query.Direction.DESCENDING)
                .get()
                .await()
            
            query.documents.mapNotNull { doc ->
                val reportDto = doc.toObject(HealthReportDto::class.java)
                reportDto?.toDomain(doc.id)
            }
        }
    }

    override suspend fun saveHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            val reportDto = HealthReportDto.fromDomain(report)
            firestore.collection("healthReports")
                .document(report.id)
                .set(reportDto)
                .await()
        }
    }

    override suspend fun updateHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            val reportDto = HealthReportDto.fromDomain(report)
            firestore.collection("healthReports")
                .document(report.id)
                .set(reportDto)
                .await()
        }
    }

    override suspend fun deleteHealthReport(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection("healthReports")
                .document(reportId)
                .delete()
                .await()
        }
    }

    override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> {
        return Result.catching(errorHandler) {
            // Create a shareable link document
            val shareData = mapOf(
                "reportId" to reportId,
                "expiresAt" to System.currentTimeMillis() + (expirationDays * 24 * 60 * 60 * 1000),
                "createdAt" to System.currentTimeMillis()
            )
            
            val shareDoc = firestore.collection("sharedReports")
                .add(shareData)
                .await()
            
            // Return a shareable URL (in production, this would be a proper URL)
            "https://eunio.app/shared/${shareDoc.id}"
        }
    }

    override suspend fun revokeShareableLink(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            val query = firestore.collection("sharedReports")
                .whereEqualTo("reportId", reportId)
                .get()
                .await()
            
            val batch = firestore.batch()
            query.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    // Settings operations
    override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("settings", settings)
                .await()
        }
    }

    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("settings", null)
                .await()
        }
    }

    override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("settingsHistory")
                .document(historyId)
                .set(historyData)
                .await()
        }
    }

    override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("settingsHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            querySnapshot.documents.map { doc ->
                doc.id to (doc.data ?: emptyMap())
            }
        }
    }

    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("settingsHistory")
                .get()
                .await()
            
            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    private suspend fun deleteUserSubcollections(batch: WriteBatch, userId: String) {
        // Delete cycles
        val cyclesQuery = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CYCLES_COLLECTION)
            .get()
            .await()
        
        cyclesQuery.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        
        // Delete daily logs
        val logsQuery = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DAILY_LOGS_COLLECTION)
            .get()
            .await()
        
        logsQuery.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        
        // Delete insights
        val insightsQuery = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(INSIGHTS_COLLECTION)
            .get()
            .await()
        
        insightsQuery.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
    }
}