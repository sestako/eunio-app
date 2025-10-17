package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.manager.ConflictResolutionStrategy
import com.eunio.healthapp.domain.manager.SettingsBackupManager
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Instant

/**
 * Use case for resolving settings conflicts with user choice options.
 * Provides different resolution strategies and handles the conflict resolution process.
 */
class ResolveSettingsConflictUseCase(
    private val settingsRepository: SettingsRepository,
    private val settingsBackupManager: SettingsBackupManager
) {
    
    /**
     * Detects if there's a conflict between local and remote settings.
     * 
     * @param userId The user ID to check for conflicts
     * @return Result containing conflict information or null if no conflict
     */
    suspend fun detectConflict(userId: String): Result<SettingsConflict?> {
        return try {
            val localSettings = settingsRepository.getUserSettings(userId).getOrNull()
            
            // Try to get remote settings for comparison
            // This would typically involve checking the remote data source
            // For now, we'll simulate this by checking if sync is needed
            
            if (localSettings != null && localSettings.needsSync()) {
                // There might be a conflict, but we need remote data to confirm
                // In a real implementation, this would fetch remote settings
                Result.success(null) // No conflict detected for now
            } else {
                Result.success(null) // No conflict
            }
        } catch (e: Exception) {
            Result.error(
                com.eunio.healthapp.domain.error.SettingsError.ConflictResolutionError(
                    "Failed to detect settings conflict: ${e.message}"
                )
            )
        }
    }
    
    /**
     * Resolves a settings conflict using the specified strategy.
     * 
     * @param conflict The conflict to resolve
     * @param strategy The resolution strategy chosen by the user
     * @return Result containing the resolved settings
     */
    suspend fun resolveConflict(
        conflict: SettingsConflict,
        strategy: ConflictResolutionStrategy
    ): Result<UserSettings> {
        return try {
            settingsBackupManager.resolveConflictWithUserChoice(
                userId = conflict.userId,
                localSettings = conflict.localSettings,
                remoteSettings = conflict.remoteSettings,
                strategy = strategy
            )
        } catch (e: Exception) {
            Result.error(
                com.eunio.healthapp.domain.error.SettingsError.ConflictResolutionError(
                    "Failed to resolve settings conflict: ${e.message}"
                )
            )
        }
    }
    
    /**
     * Gets recommended resolution strategy based on conflict analysis.
     * 
     * @param conflict The conflict to analyze
     * @return The recommended resolution strategy
     */
    fun getRecommendedStrategy(conflict: SettingsConflict): ConflictResolutionStrategy {
        return when {
            // If one version is significantly newer, prefer it
            conflict.localSettings.lastModified > conflict.remoteSettings.lastModified.plus(
                kotlin.time.Duration.parse("PT1H") // 1 hour difference
            ) -> ConflictResolutionStrategy.LOCAL_WINS
            
            conflict.remoteSettings.lastModified > conflict.localSettings.lastModified.plus(
                kotlin.time.Duration.parse("PT1H")
            ) -> ConflictResolutionStrategy.REMOTE_WINS
            
            // If timestamps are close, check for customization
            conflict.localSettings.hasCustomizations() && !conflict.remoteSettings.hasCustomizations() -> 
                ConflictResolutionStrategy.LOCAL_WINS
            
            !conflict.localSettings.hasCustomizations() && conflict.remoteSettings.hasCustomizations() -> 
                ConflictResolutionStrategy.REMOTE_WINS
            
            // If both have customizations, suggest merge
            conflict.localSettings.hasCustomizations() && conflict.remoteSettings.hasCustomizations() -> 
                ConflictResolutionStrategy.MERGE_FIELDS
            
            // Default to last write wins
            else -> ConflictResolutionStrategy.LAST_WRITE_WINS
        }
    }
    
    /**
     * Creates a preview of what the resolved settings would look like.
     * This helps users understand the impact of their choice.
     * 
     * @param conflict The conflict to preview
     * @param strategy The strategy to preview
     * @return Result containing preview of resolved settings
     */
    suspend fun previewResolution(
        conflict: SettingsConflict,
        strategy: ConflictResolutionStrategy
    ): Result<SettingsResolutionPreview> {
        return try {
            val previewSettings = when (strategy) {
                ConflictResolutionStrategy.LOCAL_WINS -> conflict.localSettings
                ConflictResolutionStrategy.REMOTE_WINS -> conflict.remoteSettings
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    if (conflict.localSettings.lastModified > conflict.remoteSettings.lastModified) {
                        conflict.localSettings
                    } else {
                        conflict.remoteSettings
                    }
                }
                ConflictResolutionStrategy.MERGE_FIELDS -> {
                    // This would create a merged version
                    // For preview purposes, we'll show the local settings
                    // In a real implementation, this would show the actual merge result
                    conflict.localSettings
                }
                ConflictResolutionStrategy.MANUAL_RESOLUTION -> {
                    // Manual resolution requires UI interaction
                    conflict.localSettings
                }
            }
            
            val changes = analyzeChanges(conflict.localSettings, previewSettings)
            
            Result.success(
                SettingsResolutionPreview(
                    resolvedSettings = previewSettings,
                    changes = changes,
                    strategy = strategy
                )
            )
        } catch (e: Exception) {
            Result.error(
                com.eunio.healthapp.domain.error.SettingsError.ConflictResolutionError(
                    "Failed to create resolution preview: ${e.message}"
                )
            )
        }
    }
    
    private fun analyzeChanges(
        current: UserSettings,
        resolved: UserSettings
    ): List<SettingsChange> {
        val changes = mutableListOf<SettingsChange>()
        
        // Compare unit preferences
        if (current.unitPreferences != resolved.unitPreferences) {
            changes.add(
                SettingsChange(
                    section = "Unit Preferences",
                    field = "Temperature/Weight Units",
                    oldValue = "${current.unitPreferences.temperatureUnit}/${current.unitPreferences.weightUnit}",
                    newValue = "${resolved.unitPreferences.temperatureUnit}/${resolved.unitPreferences.weightUnit}"
                )
            )
        }
        
        // Compare notification preferences
        if (current.notificationPreferences != resolved.notificationPreferences) {
            changes.add(
                SettingsChange(
                    section = "Notifications",
                    field = "Notification Settings",
                    oldValue = "Current notification configuration",
                    newValue = "Updated notification configuration"
                )
            )
        }
        
        // Compare cycle preferences
        if (current.cyclePreferences != resolved.cyclePreferences) {
            changes.add(
                SettingsChange(
                    section = "Cycle Settings",
                    field = "Cycle Parameters",
                    oldValue = "Cycle: ${current.cyclePreferences.averageCycleLength} days",
                    newValue = "Cycle: ${resolved.cyclePreferences.averageCycleLength} days"
                )
            )
        }
        
        // Compare privacy preferences
        if (current.privacyPreferences != resolved.privacyPreferences) {
            changes.add(
                SettingsChange(
                    section = "Privacy",
                    field = "Privacy Settings",
                    oldValue = "Current privacy configuration",
                    newValue = "Updated privacy configuration"
                )
            )
        }
        
        // Compare display preferences
        if (current.displayPreferences != resolved.displayPreferences) {
            changes.add(
                SettingsChange(
                    section = "Display",
                    field = "Display Settings",
                    oldValue = "Text size: ${current.displayPreferences.textSizeScale}",
                    newValue = "Text size: ${resolved.displayPreferences.textSizeScale}"
                )
            )
        }
        
        // Compare sync preferences
        if (current.syncPreferences != resolved.syncPreferences) {
            changes.add(
                SettingsChange(
                    section = "Sync",
                    field = "Sync Settings",
                    oldValue = "Auto sync: ${current.syncPreferences.autoSyncEnabled}",
                    newValue = "Auto sync: ${resolved.syncPreferences.autoSyncEnabled}"
                )
            )
        }
        
        return changes
    }
}

/**
 * Represents a settings conflict between local and remote versions
 */
data class SettingsConflict(
    val userId: String,
    val localSettings: UserSettings,
    val remoteSettings: UserSettings,
    val conflictType: ConflictType,
    val detectedAt: Instant
)

/**
 * Types of settings conflicts
 */
enum class ConflictType {
    /** Both versions have been modified since last sync */
    CONCURRENT_MODIFICATIONS,
    /** Local version is newer but remote has different changes */
    LOCAL_NEWER_REMOTE_DIFFERENT,
    /** Remote version is newer but local has different changes */
    REMOTE_NEWER_LOCAL_DIFFERENT,
    /** Versions have same timestamp but different content */
    SAME_TIMESTAMP_DIFFERENT_CONTENT
}

/**
 * Preview of what settings would look like after conflict resolution
 */
data class SettingsResolutionPreview(
    val resolvedSettings: UserSettings,
    val changes: List<SettingsChange>,
    val strategy: ConflictResolutionStrategy
)

/**
 * Represents a change that would be made during conflict resolution
 */
data class SettingsChange(
    val section: String,
    val field: String,
    val oldValue: String,
    val newValue: String
)

// Extension function to check if settings have customizations
private fun UserSettings.hasCustomizations(): Boolean {
    val defaults = UserSettings.createDefault(this.userId)
    return this.unitPreferences.isManuallySet ||
           this.cyclePreferences.isCustomized ||
           this.notificationPreferences != defaults.notificationPreferences ||
           this.displayPreferences != defaults.displayPreferences ||
           this.privacyPreferences != defaults.privacyPreferences
}

// Extension function to check if settings need sync
private fun UserSettings.needsSync(): Boolean {
    return this.syncStatus != com.eunio.healthapp.domain.model.SyncStatus.SYNCED
}