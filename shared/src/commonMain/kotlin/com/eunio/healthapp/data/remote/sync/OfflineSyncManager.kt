package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.util.NetworkConnectivity
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Manages offline synchronization for user preferences.
 * Handles connectivity monitoring, automatic sync when online, and graceful degradation when offline.
 */
class OfflineSyncManager(
    private val preferencesRepository: PreferencesRepository,
    private val networkConnectivity: NetworkConnectivity,
    private val errorHandler: UnitSystemErrorHandler = UnitSystemErrorHandler(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    
    private val _syncState = MutableStateFlow(OfflineSyncState.IDLE)
    val syncState: StateFlow<OfflineSyncState> = _syncState.asStateFlow()
    
    private val _connectivityState = MutableStateFlow(ConnectivityState.UNKNOWN)
    val connectivityState: StateFlow<ConnectivityState> = _connectivityState.asStateFlow()
    
    private val _syncMetrics = MutableStateFlow(SyncMetrics())
    val syncMetrics: StateFlow<SyncMetrics> = _syncMetrics.asStateFlow()
    
    private var connectivityJob: Job? = null
    private var syncJob: Job? = null
    private var retryJob: Job? = null
    
    private val config = OfflineSyncConfiguration()
    
    /**
     * Starts the offline sync manager
     */
    fun start() {
        if (connectivityJob?.isActive == true) {
            return // Already started
        }
        
        startConnectivityMonitoring()
        startPeriodicSync()
    }
    
    /**
     * Stops the offline sync manager
     */
    fun stop() {
        connectivityJob?.cancel()
        syncJob?.cancel()
        retryJob?.cancel()
        
        _syncState.value = OfflineSyncState.STOPPED
    }
    
    /**
     * Manually triggers a sync operation
     */
    suspend fun triggerSync(force: Boolean = false): Result<Unit> {
        return when {
            !networkConnectivity.isConnected() -> {
                _syncState.value = OfflineSyncState.OFFLINE
                handleOfflineMode()
            }
            
            !force && !shouldPerformSync() -> {
                Result.success(Unit)
            }
            
            else -> {
                performSync()
            }
        }
    }
    
    /**
     * Handles recovery from sync failures with exponential backoff
     */
    suspend fun recoverFromFailure(): Result<Unit> {
        return try {
            _syncState.value = OfflineSyncState.RECOVERING
            
            // Wait for stable connectivity
            if (!waitForStableConnectivity()) {
                return Result.error(errorHandler.createNetworkError(
                    message = "Could not establish stable connectivity for recovery",
                    operation = "recoverFromFailure"
                ))
            }
            
            // Attempt sync with conflict resolution
            val result = performSyncWithRetry()
            
            if (result.isSuccess) {
                _syncState.value = OfflineSyncState.SYNCED
                updateMetrics(success = true)
            } else {
                _syncState.value = OfflineSyncState.FAILED
                updateMetrics(success = false)
                scheduleRetry()
            }
            
            result
        } catch (e: Exception) {
            _syncState.value = OfflineSyncState.FAILED
            updateMetrics(success = false)
            Result.error(errorHandler.handleUnitSystemError(e, "recoverFromFailure"))
        }
    }
    
    /**
     * Gets current sync status and metrics
     */
    fun getStatus(): OfflineSyncStatus {
        return OfflineSyncStatus(
            syncState = _syncState.value,
            connectivityState = _connectivityState.value,
            metrics = _syncMetrics.value,
            isHealthy = isHealthy()
        )
    }
    
    private fun startConnectivityMonitoring() {
        connectivityJob = coroutineScope.launch {
            networkConnectivity.observeConnectivity()
                .distinctUntilChanged()
                .collect { isConnected ->
                    val newState = if (isConnected) {
                        ConnectivityState.CONNECTED
                    } else {
                        ConnectivityState.DISCONNECTED
                    }
                    
                    val previousState = _connectivityState.value
                    _connectivityState.value = newState
                    
                    handleConnectivityChange(previousState, newState)
                }
        }
    }
    
    private fun startPeriodicSync() {
        syncJob = coroutineScope.launch {
            while (isActive) {
                delay(config.periodicSyncInterval.inWholeMilliseconds)
                
                if (shouldPerformSync() && networkConnectivity.isConnected()) {
                    performSync()
                }
            }
        }
    }
    
    private suspend fun handleConnectivityChange(
        previous: ConnectivityState,
        current: ConnectivityState
    ) {
        when {
            previous == ConnectivityState.DISCONNECTED && current == ConnectivityState.CONNECTED -> {
                onConnectivityRestored()
            }
            
            previous == ConnectivityState.CONNECTED && current == ConnectivityState.DISCONNECTED -> {
                onConnectivityLost()
            }
        }
    }
    
    private suspend fun onConnectivityRestored() {
        _syncState.value = OfflineSyncState.CONNECTIVITY_RESTORED
        
        // Wait for stable connection before syncing
        delay(config.connectivityStabilizationDelay.inWholeMilliseconds)
        
        if (networkConnectivity.hasStableConnection()) {
            performSyncWithRetry()
        }
    }
    
    private suspend fun onConnectivityLost() {
        _syncState.value = OfflineSyncState.OFFLINE
        retryJob?.cancel() // Cancel any pending retries
        
        handleOfflineMode()
    }
    
    private suspend fun performSync(): Result<Unit> {
        return try {
            _syncState.value = OfflineSyncState.SYNCING
            
            val result = preferencesRepository.syncPreferences()
            
            if (result.isSuccess) {
                _syncState.value = OfflineSyncState.SYNCED
                updateMetrics(success = true)
            } else {
                _syncState.value = OfflineSyncState.FAILED
                updateMetrics(success = false)
                
                val error = result.errorOrNull()
                if (error is UnitSystemError && errorHandler.isRetryable(error)) {
                    scheduleRetry()
                }
            }
            
            result
        } catch (e: Exception) {
            _syncState.value = OfflineSyncState.FAILED
            updateMetrics(success = false)
            
            val unitSystemError = errorHandler.handleUnitSystemError(e, "performSync")
            if (errorHandler.isRetryable(unitSystemError)) {
                scheduleRetry()
            }
            
            Result.error(unitSystemError)
        }
    }
    
    private suspend fun performSyncWithRetry(): Result<Unit> {
        var lastError: UnitSystemError? = null
        
        repeat(config.maxRetryAttempts) { attempt ->
            val result = performSync()
            
            if (result.isSuccess) {
                return result
            }
            
            val error = result.errorOrNull()
            if (error is UnitSystemError) {
                lastError = error
                
                if (errorHandler.isRetryable(error) && attempt < config.maxRetryAttempts - 1) {
                    val delaySeconds = errorHandler.getRetryDelay(error, attempt + 1)
                    delay(delaySeconds.seconds)
                } else {
                    return result
                }
            } else {
                return result
            }
        }
        
        return Result.error(lastError ?: errorHandler.createSyncError(
            message = "Sync failed after ${config.maxRetryAttempts} attempts",
            operation = "performSyncWithRetry"
        ))
    }
    
    private suspend fun handleOfflineMode(): Result<Unit> {
        return try {
            // In offline mode, we just ensure that any pending operations are queued
            // The actual offline handling is done by the repository layer
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleUnitSystemError(e, "handleOfflineMode"))
        }
    }
    
    private suspend fun waitForStableConnectivity(): Boolean {
        var attempts = 0
        
        while (attempts < config.maxConnectivityWaitAttempts) {
            if (networkConnectivity.isConnected() && networkConnectivity.hasStableConnection()) {
                return true
            }
            
            delay(config.connectivityCheckInterval.inWholeMilliseconds)
            attempts++
        }
        
        return false
    }
    
    private fun scheduleRetry() {
        retryJob?.cancel()
        
        retryJob = coroutineScope.launch {
            val retryDelay = calculateRetryDelay()
            delay(retryDelay.inWholeMilliseconds)
            
            if (networkConnectivity.isConnected()) {
                performSync()
            }
        }
    }
    
    private fun calculateRetryDelay(): kotlin.time.Duration {
        val failureCount = _syncMetrics.value.consecutiveFailures
        val baseDelay = config.baseRetryDelay
        val maxDelay = config.maxRetryDelay
        
        val exponentialDelay = baseDelay * (1 shl minOf(failureCount, 6)) // Cap at 2^6
        return minOf(exponentialDelay, maxDelay)
    }
    
    private fun shouldPerformSync(): Boolean {
        val metrics = _syncMetrics.value
        val now = Clock.System.now()
        
        return when {
            metrics.lastSyncTime == null -> true
            _syncState.value == OfflineSyncState.FAILED -> true
            (now - metrics.lastSyncTime) >= config.minSyncInterval -> true
            else -> false
        }
    }
    
    private fun updateMetrics(success: Boolean) {
        val current = _syncMetrics.value
        val now = Clock.System.now()
        
        _syncMetrics.value = if (success) {
            current.copy(
                lastSyncTime = now,
                lastSuccessfulSync = now,
                totalSyncAttempts = current.totalSyncAttempts + 1,
                successfulSyncs = current.successfulSyncs + 1,
                consecutiveFailures = 0
            )
        } else {
            current.copy(
                lastSyncTime = now,
                totalSyncAttempts = current.totalSyncAttempts + 1,
                failedSyncs = current.failedSyncs + 1,
                consecutiveFailures = current.consecutiveFailures + 1
            )
        }
    }
    
    private fun isHealthy(): Boolean {
        val metrics = _syncMetrics.value
        val state = _syncState.value
        
        return when {
            state == OfflineSyncState.STOPPED -> false
            state == OfflineSyncState.FAILED && metrics.consecutiveFailures > config.maxConsecutiveFailures -> false
            metrics.lastSuccessfulSync == null -> true // Initial state
            else -> {
                val timeSinceLastSuccess = Clock.System.now() - metrics.lastSuccessfulSync
                timeSinceLastSuccess <= config.healthCheckThreshold
            }
        }
    }
}

/**
 * Represents the current state of offline synchronization
 */
enum class OfflineSyncState {
    IDLE,
    SYNCING,
    SYNCED,
    FAILED,
    OFFLINE,
    CONNECTIVITY_RESTORED,
    RECOVERING,
    STOPPED
}

/**
 * Represents the current connectivity state
 */
enum class ConnectivityState {
    UNKNOWN,
    CONNECTED,
    DISCONNECTED
}

/**
 * Configuration for offline sync operations
 */
data class OfflineSyncConfiguration(
    val periodicSyncInterval: kotlin.time.Duration = 15.minutes,
    val minSyncInterval: kotlin.time.Duration = 1.minutes,
    val connectivityStabilizationDelay: kotlin.time.Duration = 3.seconds,
    val connectivityCheckInterval: kotlin.time.Duration = 2.seconds,
    val maxRetryAttempts: Int = 3,
    val maxConnectivityWaitAttempts: Int = 10,
    val maxConsecutiveFailures: Int = 5,
    val baseRetryDelay: kotlin.time.Duration = 2.seconds,
    val maxRetryDelay: kotlin.time.Duration = 2.minutes,
    val healthCheckThreshold: kotlin.time.Duration = 1.hours
)

/**
 * Metrics for monitoring sync operations
 */
data class SyncMetrics(
    val lastSyncTime: Instant? = null,
    val lastSuccessfulSync: Instant? = null,
    val totalSyncAttempts: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val consecutiveFailures: Int = 0
)

/**
 * Complete status of the offline sync manager
 */
data class OfflineSyncStatus(
    val syncState: OfflineSyncState,
    val connectivityState: ConnectivityState,
    val metrics: SyncMetrics,
    val isHealthy: Boolean
)