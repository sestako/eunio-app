package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.SyncPreferences
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.SyncPreferencesUiState
import com.eunio.healthapp.presentation.state.SyncStatus
import com.eunio.healthapp.presentation.state.ConflictData
import com.eunio.healthapp.presentation.state.ConflictType
import com.eunio.healthapp.presentation.state.StorageUsage
import com.eunio.healthapp.presentation.state.NetworkStatus
import com.eunio.healthapp.presentation.state.ConnectionQuality
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * ViewModel for sync preferences screen with sync status and backup controls.
 * Manages sync settings, manual sync operations, and conflict resolution.
 */
class SyncPreferencesViewModel(
    private val settingsManager: SettingsManager,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<SyncPreferencesUiState>() {
    
    override val initialState = SyncPreferencesUiState()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showErrorMessage = MutableSharedFlow<String>()
    val showErrorMessage: SharedFlow<String> = _showErrorMessage.asSharedFlow()
    
    private val _syncCompleted = MutableSharedFlow<Unit>()
    val syncCompleted: SharedFlow<Unit> = _syncCompleted.asSharedFlow()
    
    private val _conflictDetected = MutableSharedFlow<ConflictData>()
    val conflictDetected: SharedFlow<ConflictData> = _conflictDetected.asSharedFlow()
    
    init {
        loadSyncPreferences()
        observeSettingsChanges()
        loadStorageUsage()
        observeNetworkStatus()
    }
    
    /**
     * Loads the current sync preferences.
     */
    private fun loadSyncPreferences() {
        viewModelScope.launch {
            updateState { it.copy(loadingState = LoadingState.Loading) }
            
            settingsManager.getUserSettings()
                .onSuccess { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.syncPreferences,
                            lastSyncTime = settings.syncPreferences.lastSyncTime,
                            loadingState = LoadingState.Success(settings.syncPreferences)
                        )
                    }
                }
                .onError { error ->
                    updateState { 
                        it.copy(
                            loadingState = LoadingState.Error("Failed to load sync preferences: ${error.message}")
                        )
                    }
                    _showErrorMessage.emit("Failed to load sync preferences")
                }
        }
    }
    
    /**
     * Observes settings changes for reactive updates.
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsManager.observeSettingsChanges()
                .catch { error ->
                    _showErrorMessage.emit("Settings sync error: ${error.message}")
                }
                .collect { settings ->
                    updateState { 
                        it.copy(
                            preferences = settings.syncPreferences,
                            lastSyncTime = settings.syncPreferences.lastSyncTime,
                            loadingState = LoadingState.Success(settings.syncPreferences)
                        )
                    }
                }
        }
    }
    
    /**
     * Loads storage usage information.
     */
    private fun loadStorageUsage() {
        viewModelScope.launch {
            try {
                // This would be implemented with actual storage service
                val storageUsage = StorageUsage(
                    totalUsed = 50 * 1024 * 1024, // 50 MB
                    backupSize = 30 * 1024 * 1024, // 30 MB
                    settingsSize = 1024 * 1024, // 1 MB
                    healthDataSize = 19 * 1024 * 1024, // 19 MB
                    totalAvailable = 5 * 1024 * 1024 * 1024 // 5 GB
                )
                
                updateState { it.copy(storageUsage = storageUsage) }
            } catch (e: Exception) {
                // Storage usage is optional, don't fail the whole screen
            }
        }
    }
    
    /**
     * Observes network status changes.
     */
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            // This would be implemented with actual network monitoring
            // For now, simulate network status
            val networkStatus = NetworkStatus(
                isWifiConnected = true,
                isMobileConnected = false,
                connectionQuality = ConnectionQuality.GOOD
            )
            
            updateState { it.copy(networkStatus = networkStatus) }
        }
    }
    
    /**
     * Toggles automatic sync preference.
     * 
     * @param enabled Whether automatic sync should be enabled
     */
    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(autoSyncEnabled = enabled)
            
            updateSyncPreferences(newPreferences)
        }
    }
    
    /**
     * Toggles WiFi-only sync preference.
     * 
     * @param enabled Whether sync should only occur on WiFi
     */
    fun toggleWifiOnlySync(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(wifiOnlySync = enabled)
            
            updateSyncPreferences(newPreferences)
        }
    }
    
    /**
     * Toggles cloud backup preference.
     * 
     * @param enabled Whether cloud backup should be enabled
     */
    fun toggleCloudBackup(enabled: Boolean) {
        viewModelScope.launch {
            val currentPreferences = uiState.value.preferences
            val newPreferences = currentPreferences.copy(cloudBackupEnabled = enabled)
            
            updateSyncPreferences(newPreferences)
        }
    }
    
    /**
     * Sets sync preferences to data conservative mode.
     */
    fun setDataConservativeMode() {
        viewModelScope.launch {
            val dataConservativePreferences = SyncPreferences.dataConservative()
            updateSyncPreferences(dataConservativePreferences)
        }
    }
    
    /**
     * Sets sync preferences to offline-first mode.
     */
    fun setOfflineFirstMode() {
        viewModelScope.launch {
            val offlineFirstPreferences = SyncPreferences.offlineFirst()
            updateSyncPreferences(offlineFirstPreferences)
        }
    }
    
    /**
     * Sets sync preferences to maximum availability mode.
     */
    fun setMaxAvailabilityMode() {
        viewModelScope.launch {
            val maxAvailabilityPreferences = SyncPreferences.maxAvailability()
            updateSyncPreferences(maxAvailabilityPreferences)
        }
    }
    
    /**
     * Updates sync preferences.
     */
    private suspend fun updateSyncPreferences(preferences: SyncPreferences) {
        updateState { it.copy(isUpdating = true) }
        
        settingsManager.updateSyncPreferences(preferences)
            .onSuccess {
                updateState { 
                    it.copy(
                        preferences = preferences,
                        isUpdating = false
                    )
                }
                _showSuccessMessage.emit("Sync preferences updated")
            }
            .onError { error ->
                updateState { it.copy(isUpdating = false) }
                _showErrorMessage.emit("Failed to update sync preferences: ${error.message}")
            }
    }
    
    /**
     * Triggers manual sync operation.
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            if (!uiState.value.canManualSync) {
                _showErrorMessage.emit("Cannot sync: check network connection and settings")
                return@launch
            }
            
            updateState { 
                it.copy(
                    isSyncing = true,
                    syncProgress = 0f,
                    syncStatus = SyncStatus.SYNCING
                )
            }
            
            try {
                // Simulate sync progress
                for (progress in 1..10) {
                    kotlinx.coroutines.delay(300)
                    updateState { it.copy(syncProgress = progress / 10f) }
                }
                
                // Perform actual sync
                settingsManager.syncSettings()
                    .onSuccess {
                        val currentTime = Clock.System.now()
                        val updatedPreferences = uiState.value.preferences.withLastSyncTime(currentTime)
                        
                        updateState { 
                            it.copy(
                                isSyncing = false,
                                syncProgress = 1f,
                                syncStatus = SyncStatus.SUCCESS,
                                lastSyncTime = currentTime,
                                preferences = updatedPreferences
                            )
                        }
                        
                        _syncCompleted.emit(Unit)
                        _showSuccessMessage.emit("Sync completed successfully")
                        
                        // Reset progress after a delay
                        kotlinx.coroutines.delay(2000)
                        updateState { it.copy(syncProgress = 0f, syncStatus = SyncStatus.IDLE) }
                    }
                    .onError { error ->
                        handleSyncError(error)
                    }
            } catch (e: Exception) {
                handleSyncError(e)
            }
        }
    }
    
    /**
     * Handles sync errors and potential conflicts.
     */
    private suspend fun handleSyncError(error: Throwable) {
        // Check if this is a conflict error
        if (error.message?.contains("conflict", ignoreCase = true) == true) {
            val conflictData = ConflictData(
                localVersion = "local_v1",
                remoteVersion = "remote_v1",
                conflictType = ConflictType.SETTINGS_MODIFIED,
                affectedSettings = listOf("Sync Preferences", "Notification Settings")
            )
            
            updateState { 
                it.copy(
                    isSyncing = false,
                    syncProgress = 0f,
                    syncStatus = SyncStatus.CONFLICT,
                    conflictData = conflictData
                )
            }
            
            _conflictDetected.emit(conflictData)
        } else {
            updateState { 
                it.copy(
                    isSyncing = false,
                    syncProgress = 0f,
                    syncStatus = SyncStatus.FAILED
                )
            }
            
            _showErrorMessage.emit("Sync failed: ${error.message}")
        }
    }
    
    /**
     * Resolves sync conflict by choosing local version.
     */
    fun resolveConflictWithLocal() {
        viewModelScope.launch {
            updateState { it.copy(isResolvingConflict = true) }
            
            try {
                // This would implement actual conflict resolution
                kotlinx.coroutines.delay(1000) // Simulate resolution time
                
                updateState { 
                    it.copy(
                        isResolvingConflict = false,
                        conflictData = null,
                        syncStatus = SyncStatus.SUCCESS
                    )
                }
                
                _showSuccessMessage.emit("Conflict resolved using local version")
            } catch (e: Exception) {
                updateState { it.copy(isResolvingConflict = false) }
                _showErrorMessage.emit("Failed to resolve conflict: ${e.message}")
            }
        }
    }
    
    /**
     * Resolves sync conflict by choosing remote version.
     */
    fun resolveConflictWithRemote() {
        viewModelScope.launch {
            updateState { it.copy(isResolvingConflict = true) }
            
            try {
                // This would implement actual conflict resolution
                kotlinx.coroutines.delay(1000) // Simulate resolution time
                
                updateState { 
                    it.copy(
                        isResolvingConflict = false,
                        conflictData = null,
                        syncStatus = SyncStatus.SUCCESS
                    )
                }
                
                _showSuccessMessage.emit("Conflict resolved using remote version")
            } catch (e: Exception) {
                updateState { it.copy(isResolvingConflict = false) }
                _showErrorMessage.emit("Failed to resolve conflict: ${e.message}")
            }
        }
    }
    
    /**
     * Dismisses the current conflict without resolving.
     */
    fun dismissConflict() {
        updateState { 
            it.copy(
                conflictData = null,
                syncStatus = SyncStatus.IDLE
            )
        }
    }
    
    /**
     * Refreshes sync preferences and status.
     */
    fun refresh() {
        loadSyncPreferences()
        loadStorageUsage()
        observeNetworkStatus()
    }
    
    /**
     * Gets sync mode description based on current preferences.
     */
    fun getSyncModeDescription(): String {
        val preferences = uiState.value.preferences
        return when {
            !preferences.autoSyncEnabled && !preferences.cloudBackupEnabled -> "Offline Only"
            preferences.wifiOnlySync && preferences.autoSyncEnabled -> "WiFi Sync Only"
            preferences.autoSyncEnabled && preferences.cloudBackupEnabled -> "Full Sync Enabled"
            preferences.cloudBackupEnabled && !preferences.autoSyncEnabled -> "Backup Only"
            else -> "Custom Configuration"
        }
    }
    
    /**
     * Gets recommendations for sync settings optimization.
     */
    fun getSyncRecommendations(): List<String> {
        val preferences = uiState.value.preferences
        val networkStatus = uiState.value.networkStatus
        val recommendations = mutableListOf<String>()
        
        if (!preferences.autoSyncEnabled) {
            recommendations.add("Enable auto-sync for seamless data synchronization")
        }
        
        if (!preferences.cloudBackupEnabled) {
            recommendations.add("Enable cloud backup to protect your data")
        }
        
        if (!preferences.wifiOnlySync && !networkStatus.isWifiConnected) {
            recommendations.add("Consider WiFi-only sync to save mobile data")
        }
        
        if (preferences.isFirstSync()) {
            recommendations.add("Perform your first sync to backup your data")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Your sync settings are optimized")
        }
        
        return recommendations
    }
    
    /**
     * Checks if sync settings need attention.
     */
    fun needsAttention(): Boolean {
        val preferences = uiState.value.preferences
        val state = uiState.value
        
        return !preferences.isSyncEnabled() || 
               state.syncStatus == SyncStatus.FAILED ||
               state.hasConflict ||
               preferences.isFirstSync()
    }
}