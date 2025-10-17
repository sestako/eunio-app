package com.eunio.healthapp.data.sync

import com.eunio.healthapp.data.repository.UserRepositoryImpl
import com.eunio.healthapp.data.repository.LogRepositoryImpl
import com.eunio.healthapp.domain.util.NetworkConnectivity

/**
 * Simple background sync service for testing
 */
class BackgroundSyncService(
    private val userRepository: UserRepositoryImpl,
    private val logRepository: LogRepositoryImpl,
    private val networkConnectivity: NetworkConnectivity
) {
    
    /**
     * Performs synchronization
     */
    suspend fun performSync(): Result<Unit> {
        return if (networkConnectivity.isConnected()) {
            try {
                val userResult = userRepository.syncPendingChanges()
                if (userResult.isError) {
                    return Result.failure(Exception("User sync failed"))
                }
                
                val logResult = logRepository.syncPendingChanges()
                if (logResult.isError) {
                    return Result.failure(Exception("Log sync failed"))
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No network connection"))
        }
    }
    
    /**
     * Performs a full sync (alias for performSync for compatibility)
     */
    suspend fun performFullSync(): Result<Unit> = performSync()
    
    /**
     * Performs sync with retry logic
     */
    suspend fun performSyncWithRetry(maxRetries: Int = 3): Result<Unit> {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = performSync()
                if (result.isSuccess) {
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
            }
        }
        
        return Result.failure(lastException ?: Exception("Sync failed after $maxRetries attempts"))
    }
    
    /**
     * Stops sync operations
     */
    fun stop() {
        // Stop any ongoing sync operations
        // In a real implementation, this would cancel any running jobs
    }
    
    /**
     * Gets sync status
     */
    fun getSyncStatus(): SyncStatusInfo {
        return SyncStatusInfo(
            isInProgress = false,
            lastSyncTime = kotlinx.datetime.Clock.System.now(),
            pendingChanges = 0,
            hasErrors = false
        )
    }
}

/**
 * Sync status information
 */
data class SyncStatusInfo(
    val isInProgress: Boolean,
    val lastSyncTime: kotlinx.datetime.Instant,
    val pendingChanges: Int,
    val hasErrors: Boolean
)