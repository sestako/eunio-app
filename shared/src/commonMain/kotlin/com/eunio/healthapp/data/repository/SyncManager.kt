package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.sync.SyncState
import com.eunio.healthapp.domain.model.SyncStatistics
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Manages synchronization between local and remote data sources.
 * Coordinates sync operations across different repositories and handles conflicts.
 */
class SyncManager(
    private val userRepository: UserRepositoryImpl,
    private val logRepository: LogRepositoryImpl,
    private val networkConnectivity: NetworkConnectivity,
    private val errorHandler: ErrorHandler,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _syncStatistics = MutableStateFlow(SyncStatistics())
    val syncStatistics: StateFlow<SyncStatistics> = _syncStatistics.asStateFlow()

    /**
     * Performs a full synchronization of all data
     */
    suspend fun performFullSync(): Result<SyncStatistics> {
        return try {
            if (!networkConnectivity.isConnected()) {
                return Result.Error(errorHandler.createNetworkError("No network connection available"))
            }

            _syncState.value = SyncState.SYNCING
            val startTime = Clock.System.now()

            // Sync user data first
            val userSyncResult = userRepository.syncPendingChanges()
            if (userSyncResult.isError) {
                _syncState.value = SyncState.ERROR
                return Result.Error(userSyncResult.errorOrNull()!!)
            }

            // Sync log data
            val logSyncResult = logRepository.syncPendingChanges()
            if (logSyncResult.isError) {
                _syncState.value = SyncState.ERROR
                return Result.Error(logSyncResult.errorOrNull()!!)
            }

            val endTime = Clock.System.now()
            val statistics = SyncStatistics(
                lastSyncTime = endTime,
                syncDuration = endTime - startTime,
                isOnline = true
            )
            
            _syncStatistics.value = statistics
            _syncState.value = SyncState.COMPLETED

            Result.Success(statistics)
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            Result.Error(errorHandler.handleError(e))
        }
    }

    /**
     * Forces synchronization regardless of current state
     */
    suspend fun forceSync(): Result<SyncStatistics> {
        return try {
            _syncState.value = SyncState.SYNCING
            val startTime = Clock.System.now()

            // Force sync user data
            val userSyncResult = userRepository.syncPendingChanges()
            if (userSyncResult.isError) {
                _syncState.value = SyncState.ERROR
                return Result.Error(userSyncResult.errorOrNull()!!)
            }

            // Force sync log data
            val logSyncResult = logRepository.syncPendingChanges()
            if (logSyncResult.isError) {
                _syncState.value = SyncState.ERROR
                return Result.Error(logSyncResult.errorOrNull()!!)
            }

            val endTime = Clock.System.now()
            val statistics = SyncStatistics(
                lastSyncTime = endTime,
                syncDuration = endTime - startTime,
                isOnline = true
            )
            
            _syncStatistics.value = statistics
            _syncState.value = SyncState.COMPLETED

            Result.Success(statistics)
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            Result.Error(errorHandler.handleError(e))
        }
    }

    /**
     * Checks if there are pending changes to sync
     */
    suspend fun hasPendingChanges(): Boolean {
        return try {
            // This would check all repositories for pending changes
            // For now, return false as placeholder
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets current sync status information
     */
    suspend fun getSyncStatus(): SyncStatistics? {
        return try {
            _syncStatistics.value
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if sync is currently in progress
     */
    fun isSyncInProgress(): Boolean = _syncState.value == SyncState.SYNCING
}