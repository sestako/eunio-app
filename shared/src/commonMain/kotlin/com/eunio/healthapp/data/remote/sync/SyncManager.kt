package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * Manages synchronization between local SQLite database and remote Firestore.
 * Implements offline-first strategy with conflict resolution.
 */
class SyncManager(
    private val firestoreService: FirestoreService,
    private val userDao: UserDao,
    private val dailyLogDao: DailyLogDao,
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Performs full synchronization for a user
     */
    suspend fun syncUserData(userId: String): Result<SyncResult> {
        return try {
            val syncResult = SyncResult()
            
            // Get last sync timestamp
            val lastSyncResult = firestoreService.getLastSyncTimestamp(userId)
            val lastSyncTimestamp = when (lastSyncResult) {
                is Result.Success -> lastSyncResult.data ?: 0L
                is Result.Error -> {
                    // If we can't get sync timestamp, do full sync
                    0L
                }
            }
            
            // Sync users
            val userSyncResult = syncUsers(userId, lastSyncTimestamp)
            syncResult.mergeWith(userSyncResult)
            
            // Sync daily logs
            val logSyncResult = syncDailyLogs(userId, lastSyncTimestamp)
            syncResult.mergeWith(logSyncResult)
            
            // Update last sync timestamp
            val currentTimestamp = Clock.System.now().epochSeconds
            firestoreService.updateLastSyncTimestamp(userId, currentTimestamp)
            
            Result.success(syncResult)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Syncs pending local changes to remote
     */
    suspend fun syncPendingChanges(userId: String): Result<SyncResult> {
        return try {
            val syncResult = SyncResult()
            
            // Get pending local changes
            val pendingLogs = dailyLogDao.getPendingSync()
            
            // Upload pending daily logs
            pendingLogs.forEach { log ->
                val uploadResult = firestoreService.saveDailyLog(log)
                when (uploadResult) {
                    is Result.Success -> {
                        // Mark as synced in local database
                        dailyLogDao.markAsSynced(log.id)
                        syncResult.uploadedLogs++
                    }
                    is Result.Error -> {
                        syncResult.errors.add(uploadResult.error)
                    }
                }
            }
            
            Result.success(syncResult)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Downloads and applies remote changes since last sync
     */
    suspend fun downloadRemoteChanges(userId: String): Result<SyncResult> {
        return try {
            val syncResult = SyncResult()
            
            // Get last sync timestamp
            val lastSyncResult = firestoreService.getLastSyncTimestamp(userId)
            val lastSyncTimestamp = when (lastSyncResult) {
                is Result.Success -> lastSyncResult.data ?: 0L
                is Result.Error -> 0L
            }
            
            // Get changed documents from remote
            val changesResult = firestoreService.getChangedDocumentsSince(userId, lastSyncTimestamp)
            when (changesResult) {
                is Result.Success -> {
                    val syncData = changesResult.data
                    
                    // Apply user changes
                    syncData.users.forEach { user ->
                        val conflictResult = resolveUserConflict(user)
                        when (conflictResult) {
                            is ConflictResolution.UseRemote -> {
                                userDao.insertOrUpdate(user)
                                syncResult.downloadedUsers++
                            }
                            is ConflictResolution.UseLocal -> {
                                // Keep local version, upload to remote
                                firestoreService.updateUser(conflictResult.localData as User)
                                syncResult.uploadedUsers++
                            }
                            is ConflictResolution.Merge -> {
                                userDao.insertOrUpdate(conflictResult.mergedData as User)
                                firestoreService.updateUser(conflictResult.mergedData as User)
                                syncResult.mergedUsers++
                            }
                        }
                    }
                    
                    // Apply daily log changes
                    syncData.dailyLogs.forEach { log ->
                        val conflictResult = resolveDailyLogConflict(log)
                        when (conflictResult) {
                            is ConflictResolution.UseRemote -> {
                                dailyLogDao.insertOrUpdate(log)
                                syncResult.downloadedLogs++
                            }
                            is ConflictResolution.UseLocal -> {
                                // Keep local version, upload to remote
                                val localLog = conflictResult.localData as DailyLog
                                firestoreService.updateDailyLog(localLog)
                                syncResult.uploadedLogs++
                            }
                            is ConflictResolution.Merge -> {
                                val mergedLog = conflictResult.mergedData as DailyLog
                                dailyLogDao.insertOrUpdate(mergedLog)
                                firestoreService.updateDailyLog(mergedLog)
                                syncResult.mergedLogs++
                            }
                        }
                    }
                }
                is Result.Error -> {
                    syncResult.errors.add(changesResult.error)
                }
            }
            
            Result.success(syncResult)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Resolves conflicts for User entities
     */
    private suspend fun resolveUserConflict(remoteUser: User): ConflictResolution {
        val localUser = userDao.getUserById(remoteUser.id)
        
        return when {
            localUser == null -> ConflictResolution.UseRemote
            localUser.updatedAt > remoteUser.updatedAt -> ConflictResolution.UseLocal(localUser)
            localUser.updatedAt < remoteUser.updatedAt -> ConflictResolution.UseRemote
            else -> {
                // Same timestamp, merge non-conflicting fields
                val merged = mergeUsers(localUser, remoteUser)
                ConflictResolution.Merge(merged)
            }
        }
    }
    
    /**
     * Resolves conflicts for DailyLog entities
     */
    private suspend fun resolveDailyLogConflict(remoteLog: DailyLog): ConflictResolution {
        val localLog = dailyLogDao.getDailyLogById(remoteLog.id)
        
        return when {
            localLog == null -> ConflictResolution.UseRemote
            localLog.updatedAt > remoteLog.updatedAt -> ConflictResolution.UseLocal(localLog)
            localLog.updatedAt < remoteLog.updatedAt -> ConflictResolution.UseRemote
            else -> {
                // Same timestamp, merge non-conflicting fields
                val merged = mergeDailyLogs(localLog, remoteLog)
                ConflictResolution.Merge(merged)
            }
        }
    }
    
    /**
     * Merges two User entities, preferring non-null values
     */
    private fun mergeUsers(local: User, remote: User): User {
        return User(
            id = local.id,
            email = if (remote.email.isNotBlank()) remote.email else local.email,
            name = if (remote.name.isNotBlank()) remote.name else local.name,
            onboardingComplete = remote.onboardingComplete || local.onboardingComplete,
            primaryGoal = remote.primaryGoal, // Prefer remote for goal changes
            createdAt = if (local.createdAt < remote.createdAt) local.createdAt else remote.createdAt,
            updatedAt = Clock.System.now()
        )
    }
    
    /**
     * Merges two DailyLog entities, preferring non-null values
     */
    private fun mergeDailyLogs(local: DailyLog, remote: DailyLog): DailyLog {
        return DailyLog(
            id = local.id,
            userId = local.userId,
            date = local.date,
            periodFlow = remote.periodFlow ?: local.periodFlow,
            symptoms = if (remote.symptoms.isNotEmpty()) remote.symptoms else local.symptoms,
            mood = remote.mood ?: local.mood,
            sexualActivity = remote.sexualActivity ?: local.sexualActivity,
            bbt = remote.bbt ?: local.bbt,
            cervicalMucus = remote.cervicalMucus ?: local.cervicalMucus,
            opkResult = remote.opkResult ?: local.opkResult,
            notes = if (!remote.notes.isNullOrBlank()) remote.notes else local.notes,
            createdAt = if (local.createdAt < remote.createdAt) local.createdAt else remote.createdAt,
            updatedAt = Clock.System.now()
        )
    }
    
    private suspend fun syncUsers(userId: String, lastSyncTimestamp: Long): SyncResult {
        val result = SyncResult()
        
        // This would be implemented based on specific sync requirements
        // For now, just return empty result
        
        return result
    }
    
    private suspend fun syncDailyLogs(userId: String, lastSyncTimestamp: Long): SyncResult {
        val result = SyncResult()
        
        // This would be implemented based on specific sync requirements
        // For now, just return empty result
        
        return result
    }
    
    /**
     * Creates a flow that emits sync status updates
     */
    fun syncStatusFlow(userId: String): Flow<SyncStatus> = flow {
        emit(SyncStatus.Starting)
        
        try {
            emit(SyncStatus.UploadingChanges)
            val uploadResult = syncPendingChanges(userId)
            
            emit(SyncStatus.DownloadingChanges)
            val downloadResult = downloadRemoteChanges(userId)
            
            when {
                uploadResult.isError -> emit(SyncStatus.Error(uploadResult.errorOrNull()!!))
                downloadResult.isError -> emit(SyncStatus.Error(downloadResult.errorOrNull()!!))
                else -> emit(SyncStatus.Completed)
            }
        } catch (e: Exception) {
            emit(SyncStatus.Error(errorHandler.handleError(e)))
        }
    }
}

/**
 * Represents the result of a conflict resolution
 */
sealed class ConflictResolution {
    object UseRemote : ConflictResolution()
    data class UseLocal(val localData: Any) : ConflictResolution()
    data class Merge(val mergedData: Any) : ConflictResolution()
}

/**
 * Represents the result of a sync operation
 */
data class SyncResult(
    var uploadedUsers: Int = 0,
    var downloadedUsers: Int = 0,
    var mergedUsers: Int = 0,
    var uploadedLogs: Int = 0,
    var downloadedLogs: Int = 0,
    var mergedLogs: Int = 0,
    var uploadedCycles: Int = 0,
    var downloadedCycles: Int = 0,
    var mergedCycles: Int = 0,
    var uploadedInsights: Int = 0,
    var downloadedInsights: Int = 0,
    var mergedInsights: Int = 0,
    val errors: MutableList<AppError> = mutableListOf()
) {
    fun mergeWith(other: SyncResult) {
        uploadedUsers += other.uploadedUsers
        downloadedUsers += other.downloadedUsers
        mergedUsers += other.mergedUsers
        uploadedLogs += other.uploadedLogs
        downloadedLogs += other.downloadedLogs
        mergedLogs += other.mergedLogs
        uploadedCycles += other.uploadedCycles
        downloadedCycles += other.downloadedCycles
        mergedCycles += other.mergedCycles
        uploadedInsights += other.uploadedInsights
        downloadedInsights += other.downloadedInsights
        mergedInsights += other.mergedInsights
        errors.addAll(other.errors)
    }
    
    val hasErrors: Boolean get() = errors.isNotEmpty()
    val totalOperations: Int get() = uploadedUsers + downloadedUsers + mergedUsers + 
                                          uploadedLogs + downloadedLogs + mergedLogs +
                                          uploadedCycles + downloadedCycles + mergedCycles +
                                          uploadedInsights + downloadedInsights + mergedInsights
}

/**
 * Represents the status of a sync operation
 */
sealed class SyncStatus {
    object Starting : SyncStatus()
    object UploadingChanges : SyncStatus()
    object DownloadingChanges : SyncStatus()
    object Completed : SyncStatus()
    data class Error(val error: AppError) : SyncStatus()
}