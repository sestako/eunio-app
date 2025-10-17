package com.eunio.healthapp.domain.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

/**
 * Android-specific implementation of SettingsManager.
 * Uses SharedPreferences as a fallback when repository operations fail.
 * Provides basic read/write operations with repository integration.
 */
class AndroidSettingsManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : SettingsManager {
    
    companion object {
        private const val TAG = "SettingsManager"
        private const val PREFS_NAME = "eunio_settings"
        private const val KEY_USER_SETTINGS = "user_settings"
        private const val KEY_SYNC_STATUS = "sync_status"
    }
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
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
            
            // Fallback to SharedPreferences
            val settingsJson = sharedPrefs.getString(KEY_USER_SETTINGS, null)
            if (settingsJson != null) {
                try {
                    val settings = Json.decodeFromString<UserSettings>(settingsJson)
                    _settingsFlow.value = settings
                    _syncStatusFlow.value = sharedPrefs.getBoolean(KEY_SYNC_STATUS, false)
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
            
            // Always save to SharedPreferences as backup
            saveToSharedPreferences(updatedSettings)
            
            if (repositoryResult.isSuccess) {
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
                Result.success(updatedSettings)
            } else {
                // Repository failed, but SharedPreferences succeeded
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
            
            // Save to SharedPreferences
            saveToSharedPreferences(defaultSettings)
            
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
            
            // Save to SharedPreferences
            saveToSharedPreferences(finalSettings)
            
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
            
            // Always save to SharedPreferences as backup
            saveToSharedPreferences(updatedSettings)
            
            if (repositoryResult.isSuccess) {
                _settingsFlow.value = updatedSettings
                _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
            } else {
                // Repository failed, but SharedPreferences succeeded
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
     * Saves settings to SharedPreferences as a fallback storage mechanism
     */
    private fun saveToSharedPreferences(settings: UserSettings) {
        try {
            val json = Json.encodeToString(settings)
            sharedPrefs.edit()
                .putString(KEY_USER_SETTINGS, json)
                .putBoolean(KEY_SYNC_STATUS, settings.syncStatus == SyncStatus.SYNCED)
                .apply()
        } catch (e: Exception) {
            // Log error but don't fail the operation
            Log.w(TAG, "Failed to save settings to SharedPreferences: ${e.message}", e)
        }
    }
}