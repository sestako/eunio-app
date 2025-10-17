package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.data.remote.FirestorePaths
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.StructuredLogger
import com.eunio.healthapp.domain.util.platformLogDebug
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Implementation of LogRepository that manages daily health log data.
 * Provides offline-first architecture with automatic sync capabilities.
 */
class LogRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val dailyLogDao: DailyLogDao,
    private val errorHandler: ErrorHandler
) : LogRepository {

    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        val startTime = Clock.System.now()
        
        return try {
            // Log operation start with structured logging
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SAVE_START,
                data = mapOf(
                    "userId" to log.userId,
                    "logId" to log.id,
                    "dateEpochDays" to log.date.toEpochDays()
                )
            )
            
            // Validate log data
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (log.date > today) {
                return Result.error(errorHandler.createValidationError("Cannot log data for future dates", "date"))
            }
            
            if (log.bbt != null && (log.bbt < 95.0 || log.bbt > 105.0)) {
                return Result.error(errorHandler.createValidationError("BBT must be between 95°F and 105°F", "bbt"))
            }
            
            // Update timestamp
            val updatedLog = log.copy(updatedAt = Clock.System.now())
            
            // 1. Save locally first (offline-first architecture)
            dailyLogDao.insertOrUpdate(updatedLog)
            
            // 2. Mark as pending sync (will be synced in background)
            dailyLogDao.updateSyncStatus(updatedLog.id, "PENDING")
            
            // 3. Attempt Firebase sync in background
            val remoteResult = firestoreService.saveDailyLog(updatedLog)
            val latencyMs = (Clock.System.now() - startTime).inWholeMilliseconds
            
            if (remoteResult.isSuccess) {
                // 4a. Mark as synced on success
                dailyLogDao.markAsSynced(updatedLog.id)
                
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
                    data = mapOf(
                        "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                        "status" to "SUCCESS",
                        "latencyMs" to latencyMs
                    )
                )
            } else {
                // 4b. Keep pending on failure (will be retried later)
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
                    data = mapOf(
                        "path" to FirestorePaths.dailyLogDoc(log.userId, log.id),
                        "status" to "FAILED",
                        "latencyMs" to latencyMs,
                        "error" to (remoteResult.errorOrNull()?.message ?: "Unknown error")
                    )
                )
                // Don't fail the operation - local save succeeded
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SAVE_ERROR,
                data = mapOf(
                    "userId" to log.userId,
                    "logId" to log.id,
                    "error" to (e.message ?: "Unknown error")
                )
            )
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        return try {
            val logId = date.toString() // yyyy-MM-dd format
            val path = FirestorePaths.dailyLogDoc(userId, logId)
            
            // Get local cache first to have it available for comparison
            val localLog = dailyLogDao.getDailyLogByUserIdAndDate(userId, date)
            
            // Query Firebase first (if online)
            val remoteResult = firestoreService.getDailyLogByDate(userId, date)
            
            if (remoteResult.isSuccess) {
                val remoteLog = remoteResult.getOrNull()
                
                if (remoteLog != null) {
                    // Conflict resolution: compare updatedAt timestamps
                    val winner = if (localLog == null) {
                        // No local version, use remote
                        StructuredLogger.logStructured(
                            tag = "DailyLogSync",
                            operation = StructuredLogger.LogOperation.SYNC_RESULT,
                            data = mapOf(
                                "direction" to "REMOTE_TO_LOCAL",
                                "merged" to false,
                                "winner" to "REMOTE",
                                "reason" to "No local version exists"
                            )
                        )
                        remoteLog
                    } else if (remoteLog.updatedAt > localLog.updatedAt) {
                        // Remote is newer - last-write-wins
                        StructuredLogger.logStructured(
                            tag = "DailyLogSync",
                            operation = StructuredLogger.LogOperation.SYNC_RESULT,
                            data = mapOf(
                                "direction" to "REMOTE_TO_LOCAL",
                                "merged" to false,
                                "winner" to "REMOTE",
                                "reason" to "Remote updatedAt is newer",
                                "remoteUpdatedAt" to remoteLog.updatedAt.epochSeconds,
                                "localUpdatedAt" to localLog.updatedAt.epochSeconds
                            )
                        )
                        remoteLog
                    } else {
                        // Local is newer or equal - keep local
                        StructuredLogger.logStructured(
                            tag = "DailyLogSync",
                            operation = StructuredLogger.LogOperation.SYNC_RESULT,
                            data = mapOf(
                                "direction" to "LOCAL_WINS",
                                "merged" to false,
                                "winner" to "LOCAL",
                                "reason" to "Local updatedAt is newer or equal",
                                "remoteUpdatedAt" to remoteLog.updatedAt.epochSeconds,
                                "localUpdatedAt" to localLog.updatedAt.epochSeconds
                            )
                        )
                        localLog
                    }
                    
                    // Update local cache with winner
                    dailyLogDao.insertOrUpdate(winner)
                    dailyLogDao.markAsSynced(winner.id)
                    
                    // Log the load result
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.LOAD_RESULT,
                        data = mapOf(
                            "path" to path,
                            "found" to true,
                            "docUpdatedAt" to remoteLog.updatedAt.epochSeconds,
                            "localUpdatedAt" to (localLog?.updatedAt?.epochSeconds ?: 0)
                        )
                    )
                    
                    return Result.success(winner)
                } else {
                    // No remote data found
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.LOAD_RESULT,
                        data = mapOf(
                            "path" to path,
                            "found" to false,
                            "docUpdatedAt" to null,
                            "localUpdatedAt" to (localLog?.updatedAt?.epochSeconds ?: 0)
                        )
                    )
                }
            } else {
                // Firebase unavailable or error - fall back to local cache
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.LOAD_RESULT,
                    data = mapOf(
                        "path" to path,
                        "found" to (localLog != null),
                        "docUpdatedAt" to null,
                        "localUpdatedAt" to (localLog?.updatedAt?.epochSeconds ?: 0),
                        "fallbackReason" to "Firebase unavailable"
                    )
                )
            }
            
            // Return local cache (may be null if no data exists)
            Result.success(localLog)
        } catch (e: Exception) {
            // On error, fall back to local cache for offline support
            try {
                val localLog = dailyLogDao.getDailyLogByUserIdAndDate(userId, date)
                
                StructuredLogger.logStructured(
                    tag = "DailyLogSync",
                    operation = StructuredLogger.LogOperation.LOAD_ERROR,
                    data = mapOf(
                        "userId" to userId,
                        "date" to date.toString(),
                        "error" to (e.message ?: "Unknown error"),
                        "fallbackToCache" to (localLog != null)
                    )
                )
                
                if (localLog != null) {
                    return Result.success(localLog)
                }
            } catch (cacheError: Exception) {
                // Ignore cache errors
            }
            Result.error(errorHandler.handleError(e))
        }
    }
    
    private fun logDebug(message: String) {
        println(message) // Fallback for all platforms
        platformLogDebug("SyncDebug", message) // Platform-specific logging
    }

    override suspend fun getLogsInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            if (endDate < startDate) {
                return Result.error(errorHandler.createValidationError("End date cannot be before start date", "dateRange"))
            }
            
            // Try local cache first
            val localLogs = dailyLogDao.getDailyLogsByDateRange(userId, startDate, endDate)
            
            // Attempt to sync missing data from remote
            val remoteResult = firestoreService.getLogsInRange(userId, startDate, endDate)
            if (remoteResult.isSuccess) {
                val remoteLogs = remoteResult.getOrThrow()
                
                // Cache remote logs locally
                for (remoteLog in remoteLogs) {
                    val existingLocal = localLogs.find { it.id == remoteLog.id }
                    if (existingLocal == null || existingLocal.updatedAt < remoteLog.updatedAt) {
                        dailyLogDao.insertOrUpdate(remoteLog)
                        dailyLogDao.markAsSynced(remoteLog.id)
                    }
                }
                
                // Return updated local data
                val updatedLocalLogs = dailyLogDao.getDailyLogsByDateRange(userId, startDate, endDate)
                return Result.success(updatedLocalLogs.sortedBy { it.date })
            }
            
            // Return local data if remote fails
            Result.success(localLogs.sortedBy { it.date })
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        return try {
            if (limit <= 0) {
                return Result.error(errorHandler.createValidationError("Limit must be positive", "limit"))
            }
            
            // Try local cache first
            val localLogs = dailyLogDao.getDailyLogsByUserId(userId)
                .sortedByDescending { it.date }
                .take(limit)
            
            // Attempt to sync recent data from remote
            val remoteResult = firestoreService.getRecentLogs(userId, limit)
            if (remoteResult.isSuccess) {
                val remoteLogs = remoteResult.getOrThrow()
                
                // Cache remote logs locally
                for (remoteLog in remoteLogs) {
                    val existingLocal = localLogs.find { it.id == remoteLog.id }
                    if (existingLocal == null || existingLocal.updatedAt < remoteLog.updatedAt) {
                        dailyLogDao.insertOrUpdate(remoteLog)
                        dailyLogDao.markAsSynced(remoteLog.id)
                    }
                }
                
                // Return updated local data
                val updatedLocalLogs = dailyLogDao.getDailyLogsByUserId(userId)
                    .sortedByDescending { it.date }
                    .take(limit)
                return Result.success(updatedLocalLogs)
            }
            
            // Return local data if remote fails
            Result.success(localLogs)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> {
        return try {
            // Get the log to delete
            val logResult = getDailyLog(userId, date)
            if (logResult.isError) {
                return logResult.map { }
            }
            
            val log = logResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("No log found for the specified date"))
            
            // Delete from remote first
            val remoteResult = firestoreService.deleteDailyLog(userId, log.id)
            if (remoteResult.isError) {
                return remoteResult
            }
            
            // Delete from local cache
            dailyLogDao.deleteDailyLog(log.id)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getPeriodLogsInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            val logsResult = getLogsInRange(userId, startDate, endDate)
            if (logsResult.isError) {
                return logsResult
            }
            
            val logs = logsResult.getOrThrow()
            val periodLogs = logs.filter { it.periodFlow != null }
            
            Result.success(periodLogs)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getBBTLogsInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            val logsResult = getLogsInRange(userId, startDate, endDate)
            if (logsResult.isError) {
                return logsResult
            }
            
            val logs = logsResult.getOrThrow()
            val bbtLogs = logs.filter { it.bbt != null }
            
            Result.success(bbtLogs)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getFertilityLogsInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            val logsResult = getLogsInRange(userId, startDate, endDate)
            if (logsResult.isError) {
                return logsResult
            }
            
            val logs = logsResult.getOrThrow()
            val fertilityLogs = logs.filter { 
                it.cervicalMucus != null || it.opkResult != null 
            }
            
            Result.success(fertilityLogs)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getLogCount(userId: String): Result<Int> {
        return try {
            val logs = dailyLogDao.getDailyLogsByUserId(userId)
            Result.success(logs.size)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getLogsBySymptoms(
        userId: String,
        symptoms: List<Symptom>,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Result<List<DailyLog>> {
        return try {
            if (symptoms.isEmpty()) {
                return Result.error(errorHandler.createValidationError("At least one symptom must be specified", "symptoms"))
            }
            
            // Get logs in range or all logs if no date range specified
            val logsResult = if (startDate != null && endDate != null) {
                getLogsInRange(userId, startDate, endDate)
            } else {
                val allLogs = dailyLogDao.getDailyLogsByUserId(userId)
                Result.success(allLogs)
            }
            
            if (logsResult.isError) {
                return logsResult
            }
            
            val logs = logsResult.getOrThrow()
            val matchingLogs = logs.filter { log ->
                symptoms.any { symptom -> log.symptoms.contains(symptom) }
            }
            
            Result.success(matchingLogs.sortedBy { it.date })
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    /**
     * Syncs a specific log from remote to local cache
     */
    private suspend fun syncLogFromRemote(userId: String, date: LocalDate) {
        try {
            val remoteResult = firestoreService.getDailyLogByDate(userId, date)
            if (remoteResult.isSuccess) {
                val remoteLog = remoteResult.getOrNull()
                if (remoteLog != null) {
                    dailyLogDao.insertOrUpdate(remoteLog)
                    dailyLogDao.markAsSynced(remoteLog.id)
                }
            }
        } catch (e: Exception) {
            // Ignore sync errors - will be retried later
        }
    }

    /**
     * Syncs pending local changes to remote with exponential backoff retry strategy.
     * 
     * This function queries all logs marked as pending sync and attempts to sync them
     * to Firebase. For each log, it will retry failed syncs with exponential backoff
     * (delays: 1s, 2s, 4s, 8s, 16s) up to a maximum of 5 attempts.
     * 
     * Logs that successfully sync are marked as synced. Logs that fail after all
     * retries remain in pending state and will be retried on the next sync cycle.
     * 
     * @return Result.success with sync statistics, or Result.error if the operation fails
     */
    suspend fun syncPendingChanges(): Result<SyncResult> {
        return try {
            val startTime = Clock.System.now()
            val pendingLogs = dailyLogDao.getPendingSync()
            
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SYNC_START,
                data = mapOf(
                    "pendingCount" to pendingLogs.size,
                    "timestamp" to startTime.epochSeconds
                )
            )
            
            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()
            
            for (log in pendingLogs) {
                val syncResult = syncLogWithRetry(log)
                
                if (syncResult.isSuccess) {
                    successCount++
                    dailyLogDao.markAsSynced(log.id)
                    
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.SYNC_SUCCESS,
                        data = mapOf(
                            "logId" to log.id,
                            "userId" to log.userId,
                            "dateEpochDays" to log.date.toEpochDays()
                        )
                    )
                } else {
                    failureCount++
                    val errorMsg = syncResult.errorOrNull()?.message ?: "Unknown error"
                    errors.add("${log.id}: $errorMsg")
                    
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.SYNC_FAILURE,
                        data = mapOf(
                            "logId" to log.id,
                            "userId" to log.userId,
                            "error" to errorMsg
                        )
                    )
                }
            }
            
            val duration = (Clock.System.now() - startTime).inWholeMilliseconds
            
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SYNC_COMPLETE,
                data = mapOf(
                    "totalLogs" to pendingLogs.size,
                    "successCount" to successCount,
                    "failureCount" to failureCount,
                    "durationMs" to duration
                )
            )
            
            Result.success(
                SyncResult(
                    totalLogs = pendingLogs.size,
                    successCount = successCount,
                    failureCount = failureCount,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = "DailyLogSync",
                operation = StructuredLogger.LogOperation.SYNC_ERROR,
                data = mapOf(
                    "error" to (e.message ?: "Unknown error")
                )
            )
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Syncs a single log with exponential backoff retry strategy.
     * 
     * Retry delays follow exponential backoff pattern:
     * - Attempt 1: immediate
     * - Attempt 2: 1 second delay
     * - Attempt 3: 2 seconds delay
     * - Attempt 4: 4 seconds delay
     * - Attempt 5: 8 seconds delay
     * 
     * @param log The daily log to sync
     * @param maxRetries Maximum number of retry attempts (default: 5)
     * @return Result indicating success or failure after all retries
     */
    private suspend fun syncLogWithRetry(
        log: DailyLog,
        maxRetries: Int = 5
    ): Result<Unit> {
        var lastError: AppError? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                // Calculate exponential backoff delay
                if (attempt > 0) {
                    val delayMs = calculateBackoffDelay(attempt)
                    
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.RETRY_ATTEMPT,
                        data = mapOf(
                            "logId" to log.id,
                            "attempt" to attempt + 1,
                            "maxRetries" to maxRetries,
                            "delayMs" to delayMs
                        )
                    )
                    
                    kotlinx.coroutines.delay(delayMs)
                }
                
                // Attempt to sync to Firebase
                val remoteResult = firestoreService.saveDailyLog(log)
                
                if (remoteResult.isSuccess) {
                    StructuredLogger.logStructured(
                        tag = "DailyLogSync",
                        operation = StructuredLogger.LogOperation.RETRY_SUCCESS,
                        data = mapOf(
                            "logId" to log.id,
                            "attempt" to attempt + 1,
                            "path" to FirestorePaths.dailyLogDoc(log.userId, log.id)
                        )
                    )
                    return Result.success(Unit)
                } else {
                    lastError = remoteResult.errorOrNull() as? AppError
                        ?: AppError.NetworkError("Sync failed")
                }
            } catch (e: Exception) {
                lastError = errorHandler.handleError(e)
            }
        }
        
        // All retries exhausted
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.RETRY_EXHAUSTED,
            data = mapOf(
                "logId" to log.id,
                "maxRetries" to maxRetries,
                "error" to (lastError?.message ?: "Unknown error")
            )
        )
        
        return Result.error(lastError ?: AppError.NetworkError("Sync failed after $maxRetries attempts"))
    }
    
    /**
     * Calculates exponential backoff delay in milliseconds.
     * 
     * Formula: baseDelay * 2^(attempt - 1)
     * - Attempt 1: 1000ms (1 second)
     * - Attempt 2: 2000ms (2 seconds)
     * - Attempt 3: 4000ms (4 seconds)
     * - Attempt 4: 8000ms (8 seconds)
     * - Attempt 5: 16000ms (16 seconds)
     * 
     * @param attempt The retry attempt number (1-based)
     * @return Delay in milliseconds
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelayMs = 1000L
        return baseDelayMs * (1 shl (attempt - 1)) // 2^(attempt-1)
    }
}

/**
 * Result of a sync operation containing statistics about the sync.
 */
data class SyncResult(
    val totalLogs: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
)