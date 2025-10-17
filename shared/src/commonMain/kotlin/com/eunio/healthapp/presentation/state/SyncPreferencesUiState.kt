package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.settings.SyncPreferences
import com.eunio.healthapp.presentation.viewmodel.UiState
import kotlinx.datetime.Instant

/**
 * UI state for sync preferences screen.
 */
data class SyncPreferencesUiState(
    val preferences: SyncPreferences = SyncPreferences.default(),
    val loadingState: LoadingState = LoadingState.Idle,
    val isUpdating: Boolean = false,
    val isSyncing: Boolean = false,
    val isResolvingConflict: Boolean = false,
    val syncProgress: Float = 0f,
    val lastSyncTime: Instant? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val conflictData: ConflictData? = null,
    val storageUsage: StorageUsage? = null,
    val networkStatus: NetworkStatus = NetworkStatus.UNKNOWN
) : UiState {
    
    /**
     * Whether the preferences are currently loading.
     */
    val isLoading: Boolean
        get() = loadingState is LoadingState.Loading
    
    /**
     * Whether the preferences are enabled for interaction.
     */
    val isEnabled: Boolean
        get() = !isLoading && !isUpdating && !isSyncing && !isResolvingConflict
    
    /**
     * Error message if loading failed.
     */
    val errorMessage: String?
        get() = (loadingState as? LoadingState.Error)?.message
    
    /**
     * Whether preferences have been loaded successfully.
     */
    val hasPreferences: Boolean
        get() = loadingState is LoadingState.Success<*>
    
    /**
     * Whether sync is in progress.
     */
    val isSyncInProgress: Boolean
        get() = isSyncing && syncProgress > 0f && syncProgress < 1f
    
    /**
     * Whether sync is complete.
     */
    val isSyncComplete: Boolean
        get() = isSyncing && syncProgress >= 1f
    
    /**
     * Whether there's a sync conflict to resolve.
     */
    val hasConflict: Boolean
        get() = conflictData != null
    
    /**
     * Whether sync is effectively enabled.
     */
    val isSyncEnabled: Boolean
        get() = preferences.isSyncEnabled()
    
    /**
     * Whether sync should occur based on current network conditions.
     */
    fun shouldSync(): Boolean {
        return preferences.shouldSync(
            isWifiConnected = networkStatus.isWifiConnected,
            isMobileConnected = networkStatus.isMobileConnected
        )
    }
    
    /**
     * Gets the sync status description.
     */
    fun getSyncStatusDescription(): String {
        return when (syncStatus) {
            SyncStatus.IDLE -> "Ready to sync"
            SyncStatus.SYNCING -> "Syncing data..."
            SyncStatus.SUCCESS -> "Sync completed successfully"
            SyncStatus.FAILED -> "Sync failed"
            SyncStatus.CONFLICT -> "Sync conflict detected"
            SyncStatus.OFFLINE -> "Offline - will sync when connected"
        }
    }
    
    /**
     * Gets the last sync time description.
     */
    fun getLastSyncDescription(): String {
        return when {
            lastSyncTime == null -> "Never synced"
            isSyncInProgress -> "Syncing now..."
            else -> "Last synced: ${formatRelativeTime(lastSyncTime)}"
        }
    }
    
    /**
     * Gets the sync progress percentage.
     */
    fun getSyncProgressPercentage(): Int = (syncProgress * 100).toInt()
    
    /**
     * Gets the network status description.
     */
    fun getNetworkStatusDescription(): String {
        return when {
            networkStatus.isWifiConnected -> "Connected to WiFi"
            networkStatus.isMobileConnected -> "Connected to mobile data"
            else -> "No internet connection"
        }
    }
    
    /**
     * Whether manual sync is available.
     */
    val canManualSync: Boolean
        get() = isEnabled && !isSyncing && (networkStatus.isWifiConnected || 
                (networkStatus.isMobileConnected && !preferences.wifiOnlySync))
    
    /**
     * Whether backup is enabled and working.
     */
    val isBackupWorking: Boolean
        get() = preferences.cloudBackupEnabled && isSyncEnabled && 
                syncStatus != SyncStatus.FAILED
    
    private fun formatRelativeTime(time: Instant): String {
        // This would be implemented with proper relative time formatting
        // For now, return a placeholder
        return "recently"
    }
}

/**
 * Sync status enumeration.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED,
    CONFLICT,
    OFFLINE
}

/**
 * Conflict data for sync conflicts.
 */
data class ConflictData(
    val localVersion: String,
    val remoteVersion: String,
    val conflictType: ConflictType,
    val affectedSettings: List<String>
)

/**
 * Types of sync conflicts.
 */
enum class ConflictType {
    SETTINGS_MODIFIED,
    VERSION_MISMATCH,
    DATA_CORRUPTION
}

/**
 * Storage usage information.
 */
data class StorageUsage(
    val totalUsed: Long,
    val backupSize: Long,
    val settingsSize: Long,
    val healthDataSize: Long,
    val totalAvailable: Long
) {
    /**
     * Gets the usage percentage.
     */
    fun getUsagePercentage(): Int {
        return if (totalAvailable > 0) {
            ((totalUsed.toDouble() / totalAvailable.toDouble()) * 100).toInt()
        } else 0
    }
    
    /**
     * Formats size in human readable format.
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024 * 1024)} GB"
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}

/**
 * Network status information.
 */
data class NetworkStatus(
    val isWifiConnected: Boolean = false,
    val isMobileConnected: Boolean = false,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
) {
    companion object {
        val UNKNOWN = NetworkStatus()
    }
}

/**
 * Connection quality enumeration.
 */
enum class ConnectionQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}