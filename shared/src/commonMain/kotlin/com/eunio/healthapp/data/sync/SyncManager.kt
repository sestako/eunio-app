package com.eunio.healthapp.data.sync

import com.eunio.healthapp.data.repository.LogRepositoryImpl
import com.eunio.healthapp.domain.util.platformLogDebug
import com.eunio.healthapp.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

/**
 * Manages automatic background sync of pending changes when network connectivity is restored.
 * 
 * This manager listens to network connectivity changes and automatically triggers
 * sync of pending logs when the device comes back online.
 */
class SyncManager(
    private val networkMonitor: NetworkMonitor,
    private val logRepository: LogRepositoryImpl,
    private val scope: CoroutineScope
) {
    private var syncJob: Job? = null
    private var monitoringJob: Job? = null
    
    /**
     * Start monitoring network changes and syncing when online
     */
    fun startMonitoring() {
        platformLogDebug("SyncManager", "🔄 Starting sync monitoring")
        
        // Start network monitoring
        networkMonitor.startMonitoring()
        
        // Listen to connectivity changes
        monitoringJob = scope.launch {
            var previousState: Boolean? = null
            networkMonitor.isConnected.collect { isConnected ->
                // Manual distinct check since distinctUntilChanged is deprecated on StateFlow
                if (previousState != isConnected) {
                    platformLogDebug("SyncManager", "📡 Network status changed: isConnected=$isConnected")
                    previousState = isConnected
                    
                    if (isConnected) {
                        // Device came online - trigger sync after a short delay
                        // Delay allows the network to stabilize
                        delay(2000)
                        triggerSync()
                    }
                }
            }
        }
    }
    
    /**
     * Stop monitoring network changes
     */
    fun stopMonitoring() {
        platformLogDebug("SyncManager", "⏹️ Stopping sync monitoring")
        monitoringJob?.cancel()
        syncJob?.cancel()
        networkMonitor.stopMonitoring()
    }
    
    /**
     * Manually trigger a sync operation
     */
    fun triggerSync() {
        // Cancel any existing sync job
        syncJob?.cancel()
        
        syncJob = scope.launch {
            platformLogDebug("SyncManager", "🔄 Starting background sync...")
            
            try {
                val result = logRepository.syncPendingChanges()
                
                if (result.isSuccess) {
                    val syncResult = result.getOrNull()
                    platformLogDebug(
                        "SyncManager",
                        "✅ Sync completed: ${syncResult?.successCount}/${syncResult?.totalLogs} logs synced"
                    )
                } else {
                    val error = result.errorOrNull()
                    platformLogDebug("SyncManager", "❌ Sync failed: ${error?.message}")
                }
            } catch (e: Exception) {
                platformLogDebug("SyncManager", "❌ Sync error: ${e.message}")
            }
        }
    }
}
