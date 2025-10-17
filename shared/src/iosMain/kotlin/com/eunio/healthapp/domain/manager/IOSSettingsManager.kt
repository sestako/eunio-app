package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific implementation of SettingsManager.
 * Uses NSUserDefaults as a fallback when repository operations fail.
 * Provides basic read/write operations with repository integration.
 */
class IOSSettingsManager(
    private val settingsRepository: SettingsRepository
) : SettingsManager {
    
    companion object {
        private const val KEY_USER_SETTINGS = "eunio_user_settings"
        private const val KEY_SYNC_STATUS = "eunio_sync_status"
    }
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    private val _settingsFlow = MutableStateFlow<UserSettings?>(null)
    private val _syncStatusFlow = MutableStateFlow(false)
    
    override suspend fun getUserSettings(): Result<UserSettings> {
        return try {
            // Try repository first
            val repositoryResult = settingsRepository.getUserSettings()
            
            if (repositoryResult.isSuccess) {
                val settings = repositoryResult.getOrNull()
                if (settings != null) {
                    _settingsFlow.value = settings
                    _syncStatusFlow.value = settings.syncStatus == SyncStatus.SYNCED
                    return Result.success(settings)
                }
            }
            
            // Fallback to NSUserDefaults
            val settingsJson = userDefaults.stringForKey(KEY_USER_SETTINGS)
            if (settingsJson != null) {
                try {
                    val settings = Json.decodeFromString<UserSettings>(settingsJson)
                    _settingsFlow.value = settings
                    _syncStatusFlow.value = userDefaults.boolForKey(KEY_SYNC_STATUS)
                    return Result.success(settings)
                } catch (e: Exception) {
                    // If JSON parsing fails, create defaults
                }
            }
            
            // Create default settings if nothing found
            val defaultSettings = UserSettings.createDefault("default_user")
            _settingsFlow.value = defaultSettings
            _syncStatusFlow.value = false
            
            // Try to save defaults to repository
            settingsRepository.saveUserSettings(defaultSettings)
            
            Result.success(defaultSettings)
            
        } catch (e: Exception) {
            Result.error(
                SettingsError.PersistenceError(
                    message = "Failed to get user settings: ${e.message}",
                    operation = "getUserSettings",
                    cause = e
                )
            )
        }
    }
    
    override fun observeSettingsChanges(): Flow<UserSettings> {
        return _settingsFlow
            .filterNotNull()
            .distinctUntilChanged()
    }
    
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
        return updateSettingsSection("updateUnitPreferences") { currentSettings ->
            currentSettings.copy(
                unitPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return updateSettingsSection("updateNotificationPreferences") { currentSettings ->
            currentSettings.copy(
                notificationPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
        return updateSettingsSection("updateCyclePreferences") { currentSettings ->
            currentSettings.copy(
                cyclePreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> {
        return updateSettingsSection("updatePrivacyPreferences") { currentSettings ->
            currentSettings.copy(
                privacyPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> {
        return updateSettingsSection("updateDisplayPreferences") { currentSettings ->
            currentSettings.copy(
                displayPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
        return updateSettingsSection("updateSyncPreferences") { currentSettings ->
            currentSettings.copy(
                syncPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }
    }
    
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        return try {
            val currentResult = getUserSettings()
            if (currentResult.isError) {
                return Result.error(currentResult.errorOrNull()!!)
            }
            
            val currentSettings = currentResult.getOrThrow()
            val updatedSettings = updateFunction(currentSettings).copy(
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save to repository first
            val repositoryResult = settingsRepository.saveUserSettings(updatedSettings)
            
            // Always save to NSUserDefaults as backup
            saveToUserDefaults(updatedSettings)
            
            if (repositoryResult.isSuccess) {
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
                Result.success(updatedSettings)
            } else {
                // Repository failed, but NSUserDefaults succeeded
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = false
                Result.success(updatedSettings)
            }
            
        } catch (e: Exception) {
            Result.error(
                SettingsError.ValidationError(
                    message = "Failed to update settings: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        return try {
            // Basic validation - can be extended
            if (settings.userId.isBlank()) {
                return Result.error(
                    SettingsError.ValidationError(
                        message = "User ID cannot be blank",
                        field = "userId"
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(
                SettingsError.ValidationError(
                    message = "Settings validation error: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> {
        return try {
            val currentSettings = if (preserveUnitPreferences) {
                getUserSettings().getOrNull()
            } else null
            
            val defaultSettings = if (preserveUnitPreferences && currentSettings != null) {
                UserSettings.createDefault(currentSettings.userId).copy(
                    unitPreferences = currentSettings.unitPreferences
                )
            } else {
                UserSettings.createDefault("default_user")
            }
            
            // Save to repository
            settingsRepository.saveUserSettings(defaultSettings)
            
            // Save to NSUserDefaults
            saveToUserDefaults(defaultSettings)
            
            _settingsFlow.value = defaultSettings
            _syncStatusFlow.value = false
            
            Result.success(defaultSettings)
            
        } catch (e: Exception) {
            Result.error(
                SettingsError.ValidationError(
                    message = "Failed to reset settings: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        return try {
            settingsRepository.syncSettings()
                .onSuccess { 
                    // Refresh settings after sync
                    getUserSettings()
                }
        } catch (e: Exception) {
            Result.error(
                SettingsError.SyncError(
                    message = "Settings sync error: ${e.message}",
                    operation = "syncSettings",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun exportSettings(): Result<String> {
        return try {
            val settings = getUserSettings().getOrNull()
            if (settings != null) {
                val json = Json.encodeToString(settings)
                Result.success(json)
            } else {
                Result.error(
                    SettingsError.ExportError(
                        message = "No settings found to export",
                        exportType = "json"
                    )
                )
            }
        } catch (e: Exception) {
            Result.error(
                SettingsError.ExportError(
                    message = "Failed to export settings: ${e.message}",
                    exportType = "json",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun importSettings(backupData: String): Result<Unit> {
        return try {
            val importedSettings = Json.decodeFromString<UserSettings>(backupData)
            
            // Update with current timestamp and sync status
            val finalSettings = importedSettings.copy(
                lastModified = Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save to repository
            settingsRepository.saveUserSettings(finalSettings)
            
            // Save to NSUserDefaults
            saveToUserDefaults(finalSettings)
            
            _settingsFlow.value = finalSettings
            _syncStatusFlow.value = false
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.error(
                SettingsError.ExportError(
                    message = "Failed to import settings: ${e.message}",
                    exportType = "IMPORT",
                    cause = e
                )
            )
        }
    }
    
    override suspend fun isSynced(): Boolean {
        return try {
            val settings = getUserSettings().getOrNull()
            settings?.syncStatus == SyncStatus.SYNCED
        } catch (e: Exception) {
            false
        }
    }
    
    override fun observeSyncStatus(): Flow<Boolean> {
        return _syncStatusFlow.asStateFlow()
    }
    
    /**
     * Helper function to update a specific settings section with proper error handling
     */
    private suspend fun updateSettingsSection(
        operation: String,
        updateFunction: (UserSettings) -> UserSettings
    ): Result<Unit> {
        return try {
            val currentResult = getUserSettings()
            if (currentResult.isError) {
                return Result.error(currentResult.errorOrNull()!!)
            }
            
            val currentSettings = currentResult.getOrThrow()
            val updatedSettings = updateFunction(currentSettings)
            
            // Save to repository first
            val repositoryResult = settingsRepository.saveUserSettings(updatedSettings)
            
            // Always save to NSUserDefaults as backup
            saveToUserDefaults(updatedSettings)
            
            if (repositoryResult.isSuccess) {
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
            } else {
                // Repository failed, but NSUserDefaults succeeded
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = false
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.error(
                SettingsError.ValidationError(
                    message = "Failed to update settings in $operation: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    /**
     * Saves settings to NSUserDefaults as a fallback storage mechanism
     */
    private fun saveToUserDefaults(settings: UserSettings) {
        try {
            val json = Json.encodeToString(settings)
            userDefaults.setObject(json, KEY_USER_SETTINGS)
            userDefaults.setBool(settings.syncStatus == SyncStatus.SYNCED, KEY_SYNC_STATUS)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // Log error but don't fail the operation
            println("Warning: Failed to save settings to NSUserDefaults: ${e.message}")
        }
    }
}