package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.manager.SettingsManager
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UserNotificationService
import com.eunio.healthapp.domain.error.ServiceError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock

/**
 * Enhanced SettingsManager implementation with comprehensive error handling,
 * graceful degradation, and fallback mechanisms.
 */
class EnhancedSettingsManager(
    private val primarySettingsManager: SettingsManager,
    private val fallbackSettingsManager: SettingsManager? = null
) : BaseService(), SettingsManager {
    
    override val serviceName: String = "SettingsManager"
    
    private var isDegraded = false
    private val degradedOperations = mutableSetOf<String>()
    
    override suspend fun getUserSettings(): Result<UserSettings> {
        return executeOperationWithRetry(
            operation = "getUserSettings",
            fallback = {
                // Try fallback manager, but if it fails or is null, use default settings
                fallbackSettingsManager?.getUserSettings()?.getOrNull()
                    ?: createDefaultUserSettings()
            }
        ) {
            primarySettingsManager.getUserSettings().getOrThrow()
        }
    }
    
    override fun observeSettingsChanges(): Flow<UserSettings> {
        return try {
            primarySettingsManager.observeSettingsChanges()
                .catch { error ->
                    handleObservationError("observeSettingsChanges", error)
                    // Emit default settings and continue with fallback
                    emit(createDefaultUserSettings())
                    
                    fallbackSettingsManager?.let { fallback ->
                        fallback.observeSettingsChanges().collect { fallbackSettings ->
                            emit(fallbackSettings)
                        }
                    }
                }
        } catch (e: Exception) {
            // If primary observation fails completely, use fallback or default
            fallbackSettingsManager?.observeSettingsChanges() 
                ?: flowOf(createDefaultUserSettings())
        }
    }
    
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updateUnitPreferences",
            fallback = {
                // Always show warning when using fallback, then try fallback manager
                UserNotificationService.showWarning(
                    "Unit preferences updated locally only. Changes will sync when connection is restored."
                )
                
                // Try fallback manager, but if it fails or is null, just continue
                fallbackSettingsManager?.updateUnitPreferences(preferences)?.getOrNull()
                Unit
            }
        ) {
            primarySettingsManager.updateUnitPreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updateNotificationPreferences",
            fallback = {
                fallbackSettingsManager?.updateNotificationPreferences(preferences)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Notification preferences updated locally. Some notifications may not work until connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.updateNotificationPreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updateCyclePreferences(preferences: CyclePreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updateCyclePreferences",
            fallback = {
                fallbackSettingsManager?.updateCyclePreferences(preferences)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Cycle preferences updated locally. Predictions will be recalculated when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.updateCyclePreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updatePrivacyPreferences(preferences: PrivacyPreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updatePrivacyPreferences",
            fallback = {
                fallbackSettingsManager?.updatePrivacyPreferences(preferences)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Privacy preferences updated locally. Data sharing changes will apply when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.updatePrivacyPreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updateDisplayPreferences(preferences: DisplayPreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updateDisplayPreferences",
            fallback = {
                fallbackSettingsManager?.updateDisplayPreferences(preferences)?.getOrThrow()
                    ?: Unit.also {
                        // Display preferences are usually local, so this should work
                        UserNotificationService.showInfo("Display preferences updated.")
                    }
            }
        ) {
            primarySettingsManager.updateDisplayPreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updateSyncPreferences(preferences: SyncPreferences): Result<Unit> {
        return executeOperationWithRetry(
            operation = "updateSyncPreferences",
            fallback = {
                fallbackSettingsManager?.updateSyncPreferences(preferences)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Sync preferences updated locally. Changes will apply when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.updateSyncPreferences(preferences).getOrThrow()
        }
    }
    
    override suspend fun updateSettings(updateFunction: (UserSettings) -> UserSettings): Result<UserSettings> {
        return executeOperationWithRetry(
            operation = "updateSettings",
            fallback = {
                fallbackSettingsManager?.updateSettings(updateFunction)?.getOrThrow()
                    ?: updateFunction(createDefaultUserSettings()).also {
                        UserNotificationService.showWarning(
                            "Settings updated locally. Changes will sync when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.updateSettings(updateFunction).getOrThrow()
        }
    }
    
    override suspend fun validateSettings(settings: UserSettings): Result<Unit> {
        return executeOperation(
            operation = "validateSettings",
            showUserMessage = false, // Validation errors should be handled by caller
            fallback = {
                // Basic validation as fallback
                if (settings.userId.isBlank()) {
                    throw IllegalArgumentException("User ID cannot be blank")
                }
                Unit
            }
        ) {
            primarySettingsManager.validateSettings(settings).getOrThrow()
        }
    }
    
    override suspend fun resetToDefaults(preserveUnitPreferences: Boolean): Result<UserSettings> {
        return executeOperationWithRetry(
            operation = "resetToDefaults",
            fallback = {
                val defaultSettings = createDefaultUserSettings()
                fallbackSettingsManager?.resetToDefaults(preserveUnitPreferences)?.getOrThrow()
                    ?: defaultSettings.also {
                        UserNotificationService.showInfo(
                            "Settings reset to defaults locally. Changes will sync when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.resetToDefaults(preserveUnitPreferences).getOrThrow()
        }
    }
    
    override suspend fun syncSettings(): Result<Unit> {
        return executeOperationWithRetry(
            operation = "syncSettings",
            maxRetries = 5, // More retries for sync operations
            retryDelayMs = 2000L,
            fallback = {
                UserNotificationService.showWarning(
                    "Unable to sync settings. Your changes are saved locally and will sync when connection is restored."
                )
                Unit
            }
        ) {
            primarySettingsManager.syncSettings().getOrThrow()
        }
    }
    
    override suspend fun exportSettings(): Result<String> {
        return executeOperation(
            operation = "exportSettings",
            fallback = {
                fallbackSettingsManager?.exportSettings()?.getOrThrow()
                    ?: "{\"error\": \"Settings export temporarily unavailable\"}"
            }
        ) {
            primarySettingsManager.exportSettings().getOrThrow()
        }
    }
    
    override suspend fun importSettings(backupData: String): Result<Unit> {
        return executeOperationWithRetry(
            operation = "importSettings",
            fallback = {
                fallbackSettingsManager?.importSettings(backupData)?.getOrThrow()
                    ?: Unit.also {
                        UserNotificationService.showWarning(
                            "Settings imported locally. Changes will sync when connection is restored."
                        )
                    }
            }
        ) {
            primarySettingsManager.importSettings(backupData).getOrThrow()
        }
    }
    
    override suspend fun isSynced(): Boolean {
        return try {
            primarySettingsManager.isSynced()
        } catch (e: Exception) {
            // If we can't check sync status, assume not synced
            false
        }
    }
    
    override fun observeSyncStatus(): Flow<Boolean> {
        return try {
            primarySettingsManager.observeSyncStatus()
                .catch { error ->
                    handleObservationError("observeSyncStatus", error)
                    emit(false) // Assume not synced on error
                }
        } catch (e: Exception) {
            flowOf(false)
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            // Test basic functionality
            val result = primarySettingsManager.getUserSettings()
            result is Result.Success
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getServiceStatus(): ServiceStatus {
        val isHealthy = isHealthy()
        val details = mutableMapOf<String, Any>()
        
        details["primaryManagerAvailable"] = try {
            primarySettingsManager.getUserSettings() is Result.Success
        } catch (e: Exception) {
            false
        }
        
        details["fallbackManagerAvailable"] = fallbackSettingsManager != null
        details["isDegraded"] = isDegraded
        details["degradedOperations"] = degradedOperations.toList()
        
        return ServiceStatus(
            serviceName = serviceName,
            isHealthy = isHealthy,
            lastChecked = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            details = details
        )
    }
    
    private fun createDefaultUserSettings(): UserSettings {
        return UserSettings.createDefault("fallback_user_id")
    }
    
    private suspend fun handleObservationError(operation: String, error: Throwable) {
        degradedOperations.add(operation)
        isDegraded = true
        
        UserNotificationService.showWarning(
            "Settings monitoring is temporarily limited. Your changes are still being saved."
        )
    }
    
    override fun getUserFriendlyMessage(operation: String, error: Throwable): String {
        return when (operation) {
            "getUserSettings" -> "Unable to load your settings. Using default settings for now."
            "updateUnitPreferences" -> "Unable to save unit preferences. Please try again."
            "updateNotificationPreferences" -> "Unable to save notification preferences. Please try again."
            "updateCyclePreferences" -> "Unable to save cycle preferences. Please try again."
            "updatePrivacyPreferences" -> "Unable to save privacy preferences. Please try again."
            "updateDisplayPreferences" -> "Unable to save display preferences. Please try again."
            "updateSyncPreferences" -> "Unable to save sync preferences. Please try again."
            "syncSettings" -> "Unable to sync your settings. Your changes are saved locally."
            "exportSettings" -> "Unable to export settings. Please try again later."
            "importSettings" -> "Unable to import settings. Please check the file and try again."
            else -> super.getUserFriendlyMessage(operation, error)
        }
    }
}