package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

/**
 * Interface for Firestore database operations.
 * Provides CRUD operations for all main entities with offline-first sync strategy.
 */
interface FirestoreService {
    
    // User operations
    suspend fun getUser(userId: String): Result<User?>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    
    // Cycle operations
    suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?>
    suspend fun getCurrentCycle(userId: String): Result<Cycle?>
    suspend fun getCycleHistory(userId: String, limit: Int = 50): Result<List<Cycle>>
    suspend fun saveCycle(cycle: Cycle): Result<Unit>
    suspend fun updateCycle(cycle: Cycle): Result<Unit>
    suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit>
    
    // DailyLog operations
    suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?>
    suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?>
    suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>>
    suspend fun getRecentLogs(userId: String, limit: Int = 30): Result<List<DailyLog>>
    suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit>
    suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit>
    suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit>
    
    // Insight operations
    suspend fun getInsight(userId: String, insightId: String): Result<Insight?>
    suspend fun getUnreadInsights(userId: String): Result<List<Insight>>
    suspend fun getInsightHistory(userId: String, limit: Int = 100): Result<List<Insight>>
    suspend fun saveInsight(insight: Insight): Result<Unit>
    suspend fun updateInsight(insight: Insight): Result<Unit>
    suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit>
    suspend fun deleteInsight(userId: String, insightId: String): Result<Unit>
    
    // Batch operations for sync
    suspend fun batchSaveUsers(users: List<User>): Result<Unit>
    suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit>
    suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit>
    suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit>
    
    // Health Report operations
    suspend fun getHealthReport(reportId: String): Result<HealthReport?>
    suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>>
    suspend fun saveHealthReport(report: HealthReport): Result<Unit>
    suspend fun updateHealthReport(report: HealthReport): Result<Unit>
    suspend fun deleteHealthReport(reportId: String): Result<Unit>
    suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String>
    suspend fun revokeShareableLink(reportId: String): Result<Unit>
    
    // Settings operations
    suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit>
    suspend fun deleteUserSettings(userId: String): Result<Unit>
    suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit>
    suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>>
    suspend fun deleteSettingsHistory(userId: String): Result<Unit>
    
    // Sync operations
    suspend fun getLastSyncTimestamp(userId: String): Result<Long?>
    suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit>
    suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData>
}

/**
 * Data class representing synchronized data from Firestore
 */
data class SyncData(
    val users: List<User> = emptyList(),
    val cycles: List<Cycle> = emptyList(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val insights: List<Insight> = emptyList(),
    val deletedDocuments: List<DeletedDocument> = emptyList()
)

/**
 * Represents a deleted document for sync purposes
 */
data class DeletedDocument(
    val id: String,
    val collection: String,
    val deletedAt: Long
)

/**
 * Represents the result of a sync operation with conflict resolution
 */
data class SyncConflictResult(
    val resolvedData: SyncData,
    val conflicts: List<SyncConflict>,
    val resolutionStrategy: ConflictResolutionStrategy
)

/**
 * Represents a conflict between local and remote data
 */
data class SyncConflict(
    val type: SyncConflictType,
    val localItem: Any,
    val remoteItem: Any,
    val conflictField: List<String>
)

/**
 * Types of sync conflicts that can occur
 */
enum class SyncConflictType {
    USER,
    CYCLE,
    DAILY_LOG,
    INSIGHT
}

/**
 * Strategies for resolving sync conflicts
 */
enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    MANUAL_RESOLUTION,
    MERGE_FIELDS,
    REMOTE_WINS,
    LOCAL_WINS
}