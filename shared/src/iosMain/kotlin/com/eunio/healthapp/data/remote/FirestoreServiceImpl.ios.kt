package com.eunio.healthapp.data.remote

import com.eunio.healthapp.data.remote.dto.*
import com.eunio.healthapp.data.remote.firebase.FirebaseNativeBridge
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.StructuredLogger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * iOS implementation of FirestoreService using Firebase iOS SDK via Swift bridge.
 * 
 * This implementation uses the FirebaseNativeBridge to communicate with Swift's FirebaseIOSBridge,
 * which handles all Firebase operations using the native iOS SDK.
 * 
 * All operations use the same Firestore paths as Android: users/{userId}/dailyLogs/{logId}
 * Data format is consistent: epoch days for dates, seconds for timestamps, v=1 for schema version.
 */
class FirestoreServiceImpl(
    private val errorHandler: ErrorHandler,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : FirestoreService {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CYCLES_COLLECTION = "cycles"
        private const val DAILY_LOGS_COLLECTION = "dailyLogs"
        private const val INSIGHTS_COLLECTION = "insights"
        private const val SYNC_METADATA_COLLECTION = "syncMetadata"
        private const val LOG_TAG = "FirestoreService.iOS"
    }
    
    private val bridge = FirebaseNativeBridge()
    
    // Mock storage for non-daily-log operations (to be replaced in future tasks)
    private val mockUsers = mutableMapOf<String, User>()
    private val mockCycles = mutableMapOf<String, Cycle>()
    private val mockInsights = mutableMapOf<String, Insight>()
    private val mockSyncTimestamps = mutableMapOf<String, Long>()
    
    // User operations (mock for now)
    override suspend fun getUser(userId: String): Result<User?> {
        return Result.catching(errorHandler) {
            mockUsers[userId]
        }
    }
    
    override suspend fun saveUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            mockUsers[user.id] = user
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            mockUsers[user.id] = user
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            mockUsers.remove(userId)
            mockCycles.entries.removeAll { it.value.userId == userId }
            mockInsights.entries.removeAll { it.value.userId == userId }
            mockSyncTimestamps.remove(userId)
        }
    }
    
    // Cycle operations (mock for now)
    override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            mockCycles[cycleId]?.takeIf { it.userId == userId }
        }
    }
    
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            mockCycles.values.find { it.userId == userId && it.endDate == null }
        }
    }
    
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        return Result.catching(errorHandler) {
            mockCycles.values
                .filter { it.userId == userId }
                .sortedByDescending { it.startDate }
                .take(limit)
        }
    }
    
    override suspend fun saveCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            mockCycles[cycle.id] = cycle
        }
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            mockCycles[cycle.id] = cycle
        }
    }
    
    override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            mockCycles.remove(cycleId)
        }
    }
    
    // DailyLog operations - Using Firebase iOS SDK via Swift bridge
    
    override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> {
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION/$logId"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_DAILY_LOG_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            if (logId.isBlank()) {
                throw IllegalArgumentException("Log ID cannot be blank")
            }
            
            val result = bridge.getDailyLog(userId, logId)
            val dailyLog = result.getOrThrow()?.let { data ->
                mapToDailyLog(data, userId)
            }
            
            if (dailyLog != null) {
                StructuredLogger.logStructured(
                    tag = LOG_TAG,
                    operation = "GET_DAILY_LOG_SUCCESS",
                    data = mapOf(
                        "userId" to userId,
                        "logId" to logId,
                        "path" to path,
                        "dateEpochDays" to dailyLog.date.toEpochDays(),
                        "updatedAt" to dailyLog.updatedAt.epochSeconds
                    )
                )
            } else {
                StructuredLogger.logStructured(
                    tag = LOG_TAG,
                    operation = "GET_DAILY_LOG_NOT_FOUND",
                    data = mapOf(
                        "userId" to userId,
                        "logId" to logId,
                        "path" to path
                    )
                )
            }
            
            dailyLog
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_DAILY_LOG_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
        val epochDays = date.toEpochDays().toLong()
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_DAILY_LOG_BY_DATE_START",
            data = mapOf(
                "userId" to userId,
                "date" to date.toString(),
                "dateEpochDays" to epochDays,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            
            val result = bridge.getDailyLogByDate(userId, epochDays)
            val dailyLog = result.getOrThrow()?.let { data ->
                mapToDailyLog(data, userId)
            }
            
            if (dailyLog != null) {
                StructuredLogger.logStructured(
                    tag = LOG_TAG,
                    operation = "GET_DAILY_LOG_BY_DATE_SUCCESS",
                    data = mapOf(
                        "userId" to userId,
                        "date" to date.toString(),
                        "dateEpochDays" to epochDays,
                        "logId" to dailyLog.id,
                        "path" to path,
                        "updatedAt" to dailyLog.updatedAt.epochSeconds
                    )
                )
            } else {
                StructuredLogger.logStructured(
                    tag = LOG_TAG,
                    operation = "GET_DAILY_LOG_BY_DATE_NOT_FOUND",
                    data = mapOf(
                        "userId" to userId,
                        "date" to date.toString(),
                        "dateEpochDays" to epochDays,
                        "path" to path
                    )
                )
            }
            
            dailyLog
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_DAILY_LOG_BY_DATE_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "date" to date.toString(),
                    "dateEpochDays" to epochDays,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        val startEpochDays = startDate.toEpochDays().toLong()
        val endEpochDays = endDate.toEpochDays().toLong()
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_LOGS_IN_RANGE_START",
            data = mapOf(
                "userId" to userId,
                "startDate" to startDate.toString(),
                "endDate" to endDate.toString(),
                "startEpochDays" to startEpochDays,
                "endEpochDays" to endEpochDays,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            
            val result = bridge.getLogsInRange(userId, startEpochDays, endEpochDays)
            val logs = result.getOrThrow().map { data ->
                mapToDailyLog(data, userId)
            }
            
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_LOGS_IN_RANGE_SUCCESS",
                data = mapOf(
                    "userId" to userId,
                    "startDate" to startDate.toString(),
                    "endDate" to endDate.toString(),
                    "startEpochDays" to startEpochDays,
                    "endEpochDays" to endEpochDays,
                    "path" to path,
                    "logsCount" to logs.size
                )
            )
            
            logs
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_LOGS_IN_RANGE_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "startDate" to startDate.toString(),
                    "endDate" to endDate.toString(),
                    "startEpochDays" to startEpochDays,
                    "endEpochDays" to endEpochDays,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_RECENT_LOGS_START",
            data = mapOf(
                "userId" to userId,
                "limit" to limit,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            
            // For recent logs, query last 90 days
            val now = kotlinx.datetime.Clock.System.now()
            val endDate = now.toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
            val startDate = LocalDate.fromEpochDays(endDate.toEpochDays() - 90)
            
            val startEpochDays = startDate.toEpochDays().toLong()
            val endEpochDays = endDate.toEpochDays().toLong()
            val result = bridge.getLogsInRange(userId, startEpochDays, endEpochDays)
            val logs = result.getOrThrow()
                .map { data -> mapToDailyLog(data, userId) }
                .sortedByDescending { it.date }
                .take(limit)
            
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_RECENT_LOGS_SUCCESS",
                data = mapOf(
                    "userId" to userId,
                    "limit" to limit,
                    "path" to path,
                    "logsCount" to logs.size,
                    "startDate" to startDate.toString(),
                    "endDate" to endDate.toString()
                )
            )
            
            logs
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_RECENT_LOGS_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "limit" to limit,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
        val path = "$USERS_COLLECTION/${dailyLog.userId}/$DAILY_LOGS_COLLECTION/${dailyLog.id}"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "SAVE_DAILY_LOG_START",
            data = mapOf(
                "userId" to dailyLog.userId,
                "logId" to dailyLog.id,
                "path" to path,
                "dateEpochDays" to dailyLog.date.toEpochDays(),
                "createdAt" to dailyLog.createdAt.epochSeconds,
                "updatedAt" to dailyLog.updatedAt.epochSeconds
            )
        )
        
        return Result.catching(errorHandler) {
            if (dailyLog.userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            if (dailyLog.id.isBlank()) {
                throw IllegalArgumentException("Log ID cannot be blank")
            }
            
            val dto = DailyLogDto.fromDomain(dailyLog)
            val data = dtoToMap(dto)
            val result = bridge.saveDailyLog(dailyLog.userId, dailyLog.id, data)
            result.getOrThrow()
            
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "SAVE_DAILY_LOG_SUCCESS",
                data = mapOf(
                    "userId" to dailyLog.userId,
                    "logId" to dailyLog.id,
                    "path" to path,
                    "dateEpochDays" to dailyLog.date.toEpochDays(),
                    "updatedAt" to dailyLog.updatedAt.epochSeconds
                )
            )
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "SAVE_DAILY_LOG_ERROR",
                data = mapOf(
                    "userId" to dailyLog.userId,
                    "logId" to dailyLog.id,
                    "path" to path,
                    "dateEpochDays" to dailyLog.date.toEpochDays(),
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> {
        // For Firestore, update is the same as save (setData with merge)
        return saveDailyLog(dailyLog)
    }
    
    override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> {
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION/$logId"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "DELETE_DAILY_LOG_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            if (logId.isBlank()) {
                throw IllegalArgumentException("Log ID cannot be blank")
            }
            
            val result = bridge.deleteDailyLog(userId, logId)
            result.getOrThrow()
            
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "DELETE_DAILY_LOG_SUCCESS",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "path" to path
                )
            )
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "DELETE_DAILY_LOG_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    // Batch operations
    override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> {
        if (logs.isEmpty()) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "BATCH_SAVE_DAILY_LOGS_EMPTY",
                data = mapOf(
                    "logsCount" to 0
                )
            )
            return Result.success(Unit)
        }
        
        val userId = logs.first().userId
        val path = "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "BATCH_SAVE_DAILY_LOGS_START",
            data = mapOf(
                "userId" to userId,
                "logsCount" to logs.size,
                "path" to path
            )
        )
        
        return Result.catching(errorHandler) {
            // Validate all logs belong to the same user
            if (logs.any { it.userId != userId }) {
                throw IllegalArgumentException("All logs in batch must belong to the same user")
            }
            
            // Convert all logs to DTOs and then to maps
            val logsData = logs.map { log ->
                if (log.userId.isBlank()) {
                    throw IllegalArgumentException("User ID cannot be blank")
                }
                if (log.id.isBlank()) {
                    throw IllegalArgumentException("Log ID cannot be blank")
                }
                
                val dto = DailyLogDto.fromDomain(log)
                dtoToMap(dto)
            }
            
            // Use the bridge to batch save
            val result = bridge.batchSaveDailyLogs(userId, logsData)
            result.getOrThrow()
            
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "BATCH_SAVE_DAILY_LOGS_SUCCESS",
                data = mapOf(
                    "userId" to userId,
                    "logsCount" to logs.size,
                    "path" to path,
                    "logIds" to logs.map { it.id }
                )
            )
        }.onError { error ->
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "BATCH_SAVE_DAILY_LOGS_ERROR",
                data = mapOf(
                    "userId" to userId,
                    "logsCount" to logs.size,
                    "path" to path,
                    "error" to error.message.orEmpty(),
                    "errorType" to error::class.simpleName.orEmpty()
                )
            )
        }
    }
    
    /**
     * Converts a DailyLogDto to a Map for the Swift bridge.
     */
    private fun dtoToMap(dto: DailyLogDto): Map<String, Any> {
        return buildMap {
            put("logId", dto.logId)
            put("dateEpochDays", dto.dateEpochDays)
            put("createdAt", dto.createdAt)
            put("updatedAt", dto.updatedAt)
            dto.periodFlow?.let { put("periodFlow", it) }
            dto.symptoms?.let { if (it.isNotEmpty()) put("symptoms", it) }
            dto.mood?.let { put("mood", it) }
            dto.sexualActivity?.let { 
                put("sexualActivity", mapOf(
                    "occurred" to it.occurred,
                    "protection" to (it.protection ?: "")
                ).filterValues { v -> v != "" })
            }
            dto.bbt?.let { put("bbt", it) }
            dto.cervicalMucus?.let { put("cervicalMucus", it) }
            dto.opkResult?.let { put("opkResult", it) }
            dto.notes?.let { put("notes", it) }
            put("v", dto.v)
        }
    }
    
    /**
     * Converts a Map from the Swift bridge to a DailyLog domain model.
     */
    private fun mapToDailyLog(data: Map<String, Any>, userId: String): DailyLog {
        val logId = data["id"] as? String ?: data["logId"] as? String 
            ?: throw IllegalStateException("Missing log ID in data")
        val dateEpochDays = (data["dateEpochDays"] as? Number)?.toLong()
            ?: throw IllegalStateException("Missing dateEpochDays in data")
        val createdAt = (data["createdAt"] as? Number)?.toLong()
            ?: throw IllegalStateException("Missing createdAt in data")
        val updatedAt = (data["updatedAt"] as? Number)?.toLong()
            ?: throw IllegalStateException("Missing updatedAt in data")
        
        val dto = DailyLogDto(
            logId = logId,
            dateEpochDays = dateEpochDays,
            createdAt = createdAt,
            updatedAt = updatedAt,
            periodFlow = data["periodFlow"] as? String,
            symptoms = (data["symptoms"] as? List<*>)?.mapNotNull { it as? String },
            mood = data["mood"] as? String,
            sexualActivity = (data["sexualActivity"] as? Map<*, *>)?.let { sa ->
                SexualActivityDto(
                    occurred = sa["occurred"] as? Boolean ?: false,
                    protection = sa["protection"] as? String
                )
            },
            bbt = (data["bbt"] as? Number)?.toDouble(),
            cervicalMucus = data["cervicalMucus"] as? String,
            opkResult = data["opkResult"] as? String,
            notes = data["notes"] as? String,
            v = (data["v"] as? Number)?.toInt() ?: 1
        )
        
        return dto.toDomain(logId, userId)
    }
    
    // Insight operations (mock for now)
    override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> {
        return Result.catching(errorHandler) {
            mockInsights[insightId]?.takeIf { it.userId == userId }
        }
    }
    
    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            mockInsights.values
                .filter { it.userId == userId && !it.isRead }
                .sortedByDescending { it.generatedDate }
        }
    }
    
    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            mockInsights.values
                .filter { it.userId == userId }
                .sortedByDescending { it.generatedDate }
                .take(limit)
        }
    }
    
    override suspend fun saveInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            mockInsights[insight.id] = insight
        }
    }
    
    override suspend fun updateInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            mockInsights[insight.id] = insight
        }
    }
    
    override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            mockInsights[insightId]?.let { insight ->
                if (insight.userId == userId) {
                    mockInsights[insightId] = insight.copy(isRead = true)
                }
            }
        }
    }
    
    override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            mockInsights.remove(insightId)
        }
    }
    
    // Batch operations for other entities (mock for now)
    override suspend fun batchSaveUsers(users: List<User>): Result<Unit> {
        return Result.catching(errorHandler) {
            users.forEach { user ->
                mockUsers[user.id] = user
            }
        }
    }
    
    override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> {
        return Result.catching(errorHandler) {
            cycles.forEach { cycle ->
                mockCycles[cycle.id] = cycle
            }
        }
    }
    
    override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> {
        return Result.catching(errorHandler) {
            insights.forEach { insight ->
                mockInsights[insight.id] = insight
            }
        }
    }
    
    // Sync operations (mock for now)
    override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> {
        return Result.catching(errorHandler) {
            mockSyncTimestamps[userId]
        }
    }
    
    override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> {
        return Result.catching(errorHandler) {
            mockSyncTimestamps[userId] = timestamp
        }
    }
    
    override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData> {
        return Result.catching(errorHandler) {
            // Mock implementation
            SyncData(
                users = emptyList(),
                cycles = emptyList(),
                dailyLogs = emptyList(),
                insights = emptyList(),
                deletedDocuments = emptyList()
            )
        }
    }

    // Health Report operations (mock for now)
    private val mockHealthReports = mutableMapOf<String, HealthReport>()

    override suspend fun getHealthReport(reportId: String): Result<HealthReport?> {
        return Result.catching(errorHandler) {
            mockHealthReports[reportId]
        }
    }

    override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> {
        return Result.catching(errorHandler) {
            mockHealthReports.values.filter { it.userId == userId }
                .sortedByDescending { it.generatedDate }
        }
    }

    override suspend fun saveHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            mockHealthReports[report.id] = report
        }
    }

    override suspend fun updateHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            mockHealthReports[report.id] = report
        }
    }

    override suspend fun deleteHealthReport(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            mockHealthReports.remove(reportId)
        }
    }

    override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> {
        return Result.catching(errorHandler) {
            // Mock implementation - return a fake shareable link
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            "https://eunio.app/share/$reportId?expires=${now + (expirationDays * 24 * 60 * 60 * 1000)}"
        }
    }

    override suspend fun revokeShareableLink(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - no-op for now
        }
    }

    // Settings operations (mock for now)
    override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - no-op for now
        }
    }

    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - no-op for now
        }
    }

    override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - no-op for now
        }
    }

    override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>> {
        return Result.catching(errorHandler) {
            // Mock implementation - return empty list
            emptyList()
        }
    }

    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - no-op for now
        }
    }
}
