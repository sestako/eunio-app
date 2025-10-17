package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Handles conflict resolution for settings synchronization.
 * Provides different strategies for resolving conflicts between local and remote settings.
 */
class SettingsConflictResolver {
    
    /**
     * Resolves conflicts between local and remote settings using the specified strategy.
     * 
     * @param localSettings The local settings
     * @param remoteSettings The remote settings
     * @param strategy The conflict resolution strategy to use
     * @return Result containing the resolved settings
     */
    suspend fun resolveConflict(
        localSettings: UserSettings,
        remoteSettings: UserSettings,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): Result<UserSettings> {
        return try {
            val resolvedSettings = when (strategy) {
                ConflictResolutionStrategy.LAST_WRITE_WINS -> resolveLastWriteWins(localSettings, remoteSettings)
                ConflictResolutionStrategy.LOCAL_WINS -> localSettings
                ConflictResolutionStrategy.REMOTE_WINS -> remoteSettings
                ConflictResolutionStrategy.MERGE_FIELDS -> mergeSettings(localSettings, remoteSettings)
                ConflictResolutionStrategy.MANUAL_RESOLUTION -> {
                    // For manual resolution, we return the conflict for user decision
                    // In this case, we'll default to last write wins
                    resolveLastWriteWins(localSettings, remoteSettings)
                }
            }
            
            Result.success(resolvedSettings.withUpdate())
        } catch (e: Exception) {
            Result.error(SettingsError.SyncError("Conflict resolution failed: ${e.message}", cause = e))
        }
    }
    
    /**
     * Detects conflicts between local and remote settings.
     * 
     * @param localSettings The local settings
     * @param remoteSettings The remote settings
     * @return List of detected conflicts
     */
    fun detectConflicts(
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): List<SettingsConflict> {
        val conflicts = mutableListOf<SettingsConflict>()
        
        // Check unit preferences conflicts
        if (localSettings.unitPreferences != remoteSettings.unitPreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.UNIT_PREFERENCES,
                    localValue = localSettings.unitPreferences,
                    remoteValue = remoteSettings.unitPreferences,
                    conflictFields = listOf("unitPreferences")
                )
            )
        }
        
        // Check notification preferences conflicts
        if (localSettings.notificationPreferences != remoteSettings.notificationPreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.NOTIFICATION_PREFERENCES,
                    localValue = localSettings.notificationPreferences,
                    remoteValue = remoteSettings.notificationPreferences,
                    conflictFields = listOf("notificationPreferences")
                )
            )
        }
        
        // Check cycle preferences conflicts
        if (localSettings.cyclePreferences != remoteSettings.cyclePreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.CYCLE_PREFERENCES,
                    localValue = localSettings.cyclePreferences,
                    remoteValue = remoteSettings.cyclePreferences,
                    conflictFields = listOf("cyclePreferences")
                )
            )
        }
        
        // Check privacy preferences conflicts
        if (localSettings.privacyPreferences != remoteSettings.privacyPreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.PRIVACY_PREFERENCES,
                    localValue = localSettings.privacyPreferences,
                    remoteValue = remoteSettings.privacyPreferences,
                    conflictFields = listOf("privacyPreferences")
                )
            )
        }
        
        // Check display preferences conflicts
        if (localSettings.displayPreferences != remoteSettings.displayPreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.DISPLAY_PREFERENCES,
                    localValue = localSettings.displayPreferences,
                    remoteValue = remoteSettings.displayPreferences,
                    conflictFields = listOf("displayPreferences")
                )
            )
        }
        
        // Check sync preferences conflicts
        if (localSettings.syncPreferences != remoteSettings.syncPreferences) {
            conflicts.add(
                SettingsConflict(
                    type = SettingsConflictType.SYNC_PREFERENCES,
                    localValue = localSettings.syncPreferences,
                    remoteValue = remoteSettings.syncPreferences,
                    conflictFields = listOf("syncPreferences")
                )
            )
        }
        
        return conflicts
    }
    
    /**
     * Determines if settings have conflicts that require resolution.
     * 
     * @param localSettings The local settings
     * @param remoteSettings The remote settings
     * @return True if conflicts exist, false otherwise
     */
    fun hasConflicts(localSettings: UserSettings, remoteSettings: UserSettings): Boolean {
        return detectConflicts(localSettings, remoteSettings).isNotEmpty()
    }
    
    /**
     * Gets the recommended conflict resolution strategy based on the types of conflicts.
     * 
     * @param conflicts The list of conflicts
     * @return Recommended resolution strategy
     */
    fun getRecommendedStrategy(conflicts: List<SettingsConflict>): ConflictResolutionStrategy {
        return when {
            conflicts.isEmpty() -> ConflictResolutionStrategy.LAST_WRITE_WINS
            conflicts.any { it.type == SettingsConflictType.PRIVACY_PREFERENCES } -> ConflictResolutionStrategy.MANUAL_RESOLUTION
            conflicts.any { it.type == SettingsConflictType.CYCLE_PREFERENCES } -> ConflictResolutionStrategy.MANUAL_RESOLUTION
            conflicts.size == 1 -> ConflictResolutionStrategy.LAST_WRITE_WINS
            else -> ConflictResolutionStrategy.MERGE_FIELDS
        }
    }
    
    private fun resolveLastWriteWins(
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): UserSettings {
        return if (localSettings.lastModified > remoteSettings.lastModified) {
            localSettings
        } else {
            remoteSettings
        }
    }
    
    private fun mergeSettings(
        localSettings: UserSettings,
        remoteSettings: UserSettings
    ): UserSettings {
        // Merge strategy: take the most recently modified field from each section
        val mergedUnitPrefs = if (localSettings.unitPreferences.isManuallySet) {
            localSettings.unitPreferences
        } else {
            remoteSettings.unitPreferences
        }
        
        val mergedNotificationPrefs = if (localSettings.notificationPreferences.hasEnabledNotifications()) {
            localSettings.notificationPreferences
        } else {
            remoteSettings.notificationPreferences
        }
        
        val mergedCyclePrefs = if (localSettings.cyclePreferences.isCustomized) {
            localSettings.cyclePreferences
        } else {
            remoteSettings.cyclePreferences
        }
        
        val mergedPrivacyPrefs = if (localSettings.privacyPreferences.hasDataCollectionEnabled()) {
            remoteSettings.privacyPreferences // Prefer remote for privacy (more restrictive)
        } else {
            localSettings.privacyPreferences
        }
        
        val mergedDisplayPrefs = if (localSettings.displayPreferences.hasAccessibilityFeaturesEnabled()) {
            localSettings.displayPreferences
        } else {
            remoteSettings.displayPreferences
        }
        
        val mergedSyncPrefs = if (localSettings.syncPreferences.autoSyncEnabled) {
            localSettings.syncPreferences
        } else {
            remoteSettings.syncPreferences
        }
        
        return UserSettings(
            userId = localSettings.userId,
            unitPreferences = mergedUnitPrefs,
            notificationPreferences = mergedNotificationPrefs,
            cyclePreferences = mergedCyclePrefs,
            privacyPreferences = mergedPrivacyPrefs,
            displayPreferences = mergedDisplayPrefs,
            syncPreferences = mergedSyncPrefs,
            lastModified = Clock.System.now(),
            syncStatus = localSettings.syncStatus,
            version = maxOf(localSettings.version, remoteSettings.version)
        )
    }
}

/**
 * Represents a conflict between local and remote settings
 */
data class SettingsConflict(
    val type: SettingsConflictType,
    val localValue: Any,
    val remoteValue: Any,
    val conflictFields: List<String>
)

/**
 * Types of settings conflicts that can occur
 */
enum class SettingsConflictType {
    UNIT_PREFERENCES,
    NOTIFICATION_PREFERENCES,
    CYCLE_PREFERENCES,
    PRIVACY_PREFERENCES,
    DISPLAY_PREFERENCES,
    SYNC_PREFERENCES
}

/**
 * Strategies for resolving settings conflicts
 */
enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    MANUAL_RESOLUTION,
    MERGE_FIELDS,
    REMOTE_WINS,
    LOCAL_WINS
}

/**
 * Result of a conflict resolution operation
 */
data class SettingsConflictResult(
    val resolvedSettings: UserSettings,
    val conflicts: List<SettingsConflict>,
    val resolutionStrategy: ConflictResolutionStrategy,
    val requiresUserInput: Boolean = false
)