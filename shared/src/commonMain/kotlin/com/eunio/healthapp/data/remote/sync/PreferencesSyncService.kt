package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Background service for synchronizing user preferences between local and remote storage.
 * Handles automatic sync when connectivity is restored and periodic sync operations.
 */
class PreferencesSyncService(
    private val preferencesRepository: PreferencesRepository,
    private val networkConnectivity: NetworkConnectivity,
    private val errorHandler: UnitSystemErrorHandler = UnitSystemErrorHandler(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    private val _syncStatus = MutableStateFlow(PreferencesSyncStatus.IDLE)
    val syncStatus: StateFlow<PreferencesSyncStatus> = _syncStatus.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow<Instant?>(null)
    val lastSyncTime: StateFlow<Instant?> = _lastSyncTime.asStateFlow()
    
    private var syncJob: Job? = null
    private var connectivityMonitorJob: Job? = null
    
    private val syncConfig = SyncConfiguration()
    
    /**
     * Starts the background sync service.
     * Monitors connectivity changes and performs automatic sync when connected.
     */
    fun start() {
        if (connectivityMonitorJob?.isActive == true) {
            return // Already started
        }
        
        connectivityMonitorJob = coroutineScope.launch {
            networkConnectivity.observeConnectivity()
                .distinctUntilChanged()
                .collect { isConnected ->
                    if (isConnected) {
                        onConnectivityRestored()
                    } else {
                        onConnectivityLost()
                    }
                }
        }
        
        // Start periodic sync
        startPeriodicSync()
    }
    
    /**
     * Stops the background sync service.
     */
    fun stop() {
        connectivityMonitorJob?.cancel()
        syncJob?.cancel()
        _syncStatus.value = PreferencesSyncStatus.IDLE
    }
    
    /**
     * Manually triggers a sync operation.
     * @param force If true, ignores sync intervals and forces immediate sync
     * @return Result indicating success or failure
     */
    suspend fun triggerSync(force: Boolean = false): Result<Unit> {
        if (!force && !shouldSync()) {
            return Result.success(Unit)
        }
        
        return performSync()
    }
    
    /**
     * Handles conflict resolution when local and remote preferences differ.
     * Uses last-write-wins strategy with timestamp comparison.
     */
    suspend fun resolveConflicts(): Result<Unit> {
        return try {
            _syncStatus.value = PreferencesSyncStatus.RESOLVING_CONFLICTS
            
            if (!networkConnectivity.isConnected()) {
                _syncStatus.value = PreferencesSyncStatus.FAILED
                return Result.error(errorHandler.createNetworkError(
                    message = "Cannot resolve conflicts without network connection",
                    operation = "resolveConflicts"
                ))
            }
            
            // This would be implemented by the repository
            // For now, delegate to the repository's sync method
            val result = preferencesRepository.syncPreferences()
            
            if (result.isSuccess) {
                _syncStatus.value = PreferencesSyncStatus.SYNCED
                _lastSyncTime.value = Clock.System.now()
            } else {
                _syncStatus.value = PreferencesSyncStatus.FAILED
            }
            
            result
        } catch (e: Exception) {
            _syncStatus.value = PreferencesSyncStatus.FAILED
            Result.error(errorHandler.handleUnitSystemError(e, "resolveConflicts"))
        }
    }
    
    /**
     * Gets the current sync statistics.
     */
    fun getSyncStats(): SyncStats {
        return SyncStats(
            status = _syncStatus.value,
            lastSyncTime = _lastSyncTime.value,
            nextScheduledSync = getNextScheduledSyncTime(),
            isConnected = networkConnectivity.isConnected()
        )
    }
    
    private suspend fun onConnectivityRestored() {
        delay(syncConfig.connectivityRestoreDelay.inWholeMilliseconds)
        
        if (networkConnectivity.hasStableConnection()) {
            performSync()
        }
    }
    
    private fun onConnectivityLost() {
        _syncStatus.value = PreferencesSyncStatus.OFFLINE
    }
    
    private fun startPeriodicSync() {
        syncJob = coroutineScope.launch {
            while (isActive) {
                delay(syncConfig.periodicSyncInterval.inWholeMilliseconds)
                
                if (shouldSync() && networkConnectivity.isConnected()) {
                    performSync()
                }
            }
        }
    }
    
    private suspend fun performSync(): Result<Unit> {
        return try {
            _syncStatus.value = PreferencesSyncStatus.SYNCING
            
            val result = preferencesRepository.syncPreferences()
            
            if (result.isSuccess) {
                _syncStatus.value = PreferencesSyncStatus.SYNCED
                _lastSyncTime.value = Clock.System.now()
            } else {
                _syncStatus.value = PreferencesSyncStatus.FAILED
                
                // Schedule retry for retryable errors
                val error = result.errorOrNull()
                if (error is UnitSystemError && errorHandler.isRetryable(error)) {
                    scheduleRetry(error)
                }
            }
            
            result
        } catch (e: Exception) {
            _syncStatus.value = PreferencesSyncStatus.FAILED
            val unitSystemError = errorHandler.handleUnitSystemError(e, "performSync")
            
            if (errorHandler.isRetryable(unitSystemError)) {
                scheduleRetry(unitSystemError)
            }
            
            Result.error(unitSystemError)
        }
    }
    
    private fun scheduleRetry(error: UnitSystemError) {
        val retryDelay = errorHandler.getRetryDelay(error, 1)
        
        coroutineScope.launch {
            delay(retryDelay.seconds)
            
            if (networkConnectivity.isConnected()) {
                performSync()
            }
        }
    }
    
    private fun shouldSync(): Boolean {
        val lastSync = _lastSyncTime.value
        val now = Clock.System.now()
        
        return when {
            lastSync == null -> true
            _syncStatus.value == PreferencesSyncStatus.FAILED -> true
            (now - lastSync) >= syncConfig.minSyncInterval -> true
            else -> false
        }
    }
    
    private fun getNextScheduledSyncTime(): Instant? {
        val lastSync = _lastSyncTime.value ?: return null
        return lastSync + syncConfig.periodicSyncInterval
    }
}

/**
 * Represents the current status of the sync service
 */
enum class PreferencesSyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE,
    RESOLVING_CONFLICTS
}

/**
 * Configuration for sync operations
 */
data class SyncConfiguration(
    val periodicSyncInterval: kotlin.time.Duration = 15.minutes,
    val minSyncInterval: kotlin.time.Duration = 1.minutes,
    val connectivityRestoreDelay: kotlin.time.Duration = 2.seconds,
    val maxRetryAttempts: Int = 3,
    val enableBackgroundSync: Boolean = true
)

/**
 * Statistics about sync operations
 */
data class SyncStats(
    val status: PreferencesSyncStatus,
    val lastSyncTime: Instant?,
    val nextScheduledSync: Instant?,
    val isConnected: Boolean
)