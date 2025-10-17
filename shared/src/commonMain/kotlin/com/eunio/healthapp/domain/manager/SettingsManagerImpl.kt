package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.usecase.cycle.PredictOvulationUseCase
import com.eunio.healthapp.domain.usecase.cycle.UpdateCycleUseCase
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Implementation of SettingsManager that provides centralized settings management.
 * Handles reactive state management, validation, automatic backups, and automatic updates to dependent systems.
 */
class SettingsManagerImpl(
    private val settingsRepository: SettingsRepository,
    private val settingsBackupManager: SettingsBackupManager,
    private val notificationManager: NotificationManager,
    private val predictOvulationUseCase: PredictOvulationUseCase,
    private val updateCycleUseCase: UpdateCycleUseCase,
    private val currentUserId: () -> String
) : SettingsManager {
    
    private val _settingsFlow = MutableStateFlow<UserSettings?>(null)
    private val _syncStatusFlow = MutableStateFlow(false)
    
    override suspend fun getUserSettings(): Result<UserSettings> {
        return try {
            val userId = currentUserId()
            val result = settingsRepository.getUserSettings(userId)
            
            result.onSuccess { settings ->
                if (settings != null) {
                    _settingsFlow.value = settings
                    _syncStatusFlow.value = settings.syncStatus == SyncStatus.SYNCED
                }
            }
            
            // If no settings found, create defaults
            if (result.isSuccess && result.getOrNull() == null) {
                val defaultSettings = UserSettings.createDefault(userId)
                settingsRepository.saveUserSettings(defaultSettings)
                    .onSuccess { 
                        _settingsFlow.value = defaultSettings
                        _syncStatusFlow.value = false
                    }
                Result.success(defaultSettings)
            } else {
                result.map { it ?: UserSettings.createDefault(userId) }
            }
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
        // Validate notification preferences first
        if (!preferences.isValid()) {
            return Result.error(
                SettingsError.ValidationError(
                    message = "Invalid notification preferences",
                    field = "notificationPreferences"
                )
            )
        }
        
        return updateSettingsSection("updateNotificationPreferences") { currentSettings ->
            currentSettings.copy(
                notificationPreferences = preferences,
                lastModified = Clock.System.now()
            )
        }.onSuccess {
            // Update system notifications after successful settings save
            val notificationResult = notificationManager.updateNotificationSchedule(preferences)
            if (notificationResult.isFailure) {
                // Log the notification error but don't fail the settings update
                // The settings were saved successfully, notification scheduling is secondary
                println("Warning: Failed to update notification schedule: ${notificationResult.exceptionOrNull()?.message}")
            }
        }
    }
    
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
        // Validate cycle preferences
        if (!preferences.isValid()) {
            val errors = preferences.getValidationErrors()
            return Result.error(
                SettingsError.ValidationError(
                    message = "Invalid cycle preferences: ${errors.joinToString(", ")}",
                    field = "cyclePreferences"
                )
            )
        }
        
        return updateSettingsSection("updateCyclePreferences") { currentSettings ->
            currentSettings.copy(
                cyclePreferences = preferences,
                lastModified = Clock.System.now()
            )
        }.onSuccess {
            // Trigger cycle prediction recalculation after successful settings save
            triggerCyclePredictionUpdate(preferences)
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
        // Validate display preferences
        if (!preferences.isValid()) {
            val errors = preferences.getValidationErrors()
            return Result.error(
                SettingsError.ValidationError(
                    message = "Invalid display preferences: ${errors.joinToString(", ")}",
                    field = "displayPreferences"
                )
            )
        }
        
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
            val updatedSettings = updateFunction(currentSettings).withUpdate()
            
            // Validate the updated settings
            validateSettings(updatedSettings).onError { error ->
                return Result.error(error)
            }
            
            // Save the updated settings
            settingsRepository.saveUserSettings(updatedSettings)
                .onSuccess { 
                    _settingsFlow.value = updatedSettings
                    _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
                }
                .onError { error ->
                    return Result.error(
                        SettingsError.PersistenceError(
                            message = "Failed to save updated settings: ${error.message}",
                            operation = "updateSettings",
                            cause = error
                        )
                    )
                }
            
            Result.success(updatedSettings)
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
            if (!settings.isValid()) {
                val errors = settings.getValidationErrors()
                return Result.error(
                    SettingsError.ValidationError(
                        message = "Settings validation failed: ${errors.joinToString(", ")}"
                    )
                )
            }
            
            // Additional business logic validation
            if (settings.cyclePreferences.isCustomized && !settings.cyclePreferences.isValid()) {
                return Result.error(
                    SettingsError.ValidationError(
                        message = "Custom cycle preferences are invalid",
                        field = "cyclePreferences"
                    )
                )
            }
            
            if (settings.displayPreferences.textSizeScale < DisplayPreferences.MIN_TEXT_SCALE ||
                settings.displayPreferences.textSizeScale > DisplayPreferences.MAX_TEXT_SCALE) {
                return Result.error(
                    SettingsError.ValidationError(
                        message = "Text size scale must be between ${DisplayPreferences.MIN_TEXT_SCALE} and ${DisplayPreferences.MAX_TEXT_SCALE}",
                        field = "textSizeScale",
                        value = settings.displayPreferences.textSizeScale
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
            val userId = currentUserId()
            val currentSettings = if (preserveUnitPreferences) {
                getUserSettings().getOrNull()
            } else null
            
            val defaultSettings = if (preserveUnitPreferences && currentSettings != null) {
                UserSettings.createDefault(userId).copy(
                    unitPreferences = currentSettings.unitPreferences
                )
            } else {
                UserSettings.createDefault(userId)
            }
            
            settingsRepository.saveUserSettings(defaultSettings)
                .onSuccess { 
                    _settingsFlow.value = defaultSettings
                    _syncStatusFlow.value = false
                    
                    // Update notifications to match reset preferences
                    notificationManager.updateNotificationSchedule(defaultSettings.notificationPreferences)
                }
                .onError { error ->
                    return Result.error(
                        SettingsError.PersistenceError(
                            message = "Failed to reset settings to defaults: ${error.message}",
                            operation = "resetToDefaults",
                            cause = error
                        )
                    )
                }
            
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
                .onError { error ->
                    return Result.error(
                        SettingsError.SyncError(
                            message = "Failed to sync settings: ${error.message}",
                            operation = "syncSettings",
                            cause = error
                        )
                    )
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
            val userId = currentUserId()
            settingsBackupManager.exportSettings(userId, includeMetadata = true)
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
            val userId = currentUserId()
            
            // Parse the imported settings directly
            val importedSettings = kotlinx.serialization.json.Json.decodeFromString<UserSettings>(backupData)
            
            // Update with current user ID and sync status
            val finalSettings = importedSettings.copy(
                userId = userId,
                lastModified = kotlinx.datetime.Clock.System.now(),
                syncStatus = SyncStatus.PENDING
            )
            
            // Save the imported settings
            val saveResult = settingsRepository.saveUserSettings(finalSettings)
            
            saveResult.onSuccess {
                // Update dependent systems with imported settings
                notificationManager.updateNotificationSchedule(finalSettings.notificationPreferences)
                if (finalSettings.cyclePreferences.isCustomized) {
                    triggerCyclePredictionUpdate(finalSettings.cyclePreferences)
                }
            }
            
            saveResult
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
            
            // Validate the updated settings
            validateSettings(updatedSettings).onError { error ->
                return Result.error(error)
            }
            
            settingsRepository.saveUserSettings(updatedSettings)
                .onSuccess { 
                    _settingsFlow.value = updatedSettings
                    _syncStatusFlow.value = updatedSettings.syncStatus == SyncStatus.SYNCED
                    
                    // Create automatic backup after successful settings save
                    try {
                        settingsBackupManager.createAutomaticBackup(updatedSettings)
                        // Note: We don't fail the settings update if backup fails
                        // Backup failures are logged but don't affect the main operation
                    } catch (e: Exception) {
                        // Log backup failure but continue with successful settings update
                        // In a real app, this would be logged to analytics/crash reporting
                    }
                }
                .onError { error ->
                    return Result.error(
                        SettingsError.PersistenceError(
                            message = "Failed to save settings during $operation: ${error.message}",
                            operation = operation,
                            cause = error
                        )
                    )
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
     * Triggers cycle prediction recalculation when cycle preferences change
     */
    private suspend fun triggerCyclePredictionUpdate(preferences: CyclePreferences) {
        try {
            val userId = currentUserId()
            
            // Recalculate current cycle metrics with new preferences
            updateCycleUseCase.recalculateCycleMetrics(userId)
                .onError { error ->
                    println("Warning: Failed to recalculate cycle metrics: ${error.message}")
                }
            
            // Update ovulation predictions with new cycle parameters
            predictOvulationUseCase.updatePredictionWithCurrentData(userId)
                .onError { error ->
                    println("Warning: Failed to update ovulation predictions: ${error.message}")
                }
        } catch (e: Exception) {
            // Log the error but don't fail the settings update
            println("Warning: Failed to trigger cycle prediction update: ${e.message}")
        }
    }
}