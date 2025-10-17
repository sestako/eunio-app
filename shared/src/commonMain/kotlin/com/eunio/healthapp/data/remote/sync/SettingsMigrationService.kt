package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Handles migration of settings between different schema versions.
 * Ensures backward compatibility and smooth upgrades.
 */
class SettingsMigrationService {
    
    companion object {
        private const val CURRENT_VERSION = 1
        private const val MIN_SUPPORTED_VERSION = 1
        private const val MIN_MIGRATION_VERSION = 0  // Minimum version we can migrate FROM
    }
    
    /**
     * Migrates settings from an older version to the current version.
     * 
     * @param settings The settings to migrate
     * @param fromVersion The version to migrate from
     * @param toVersion The version to migrate to (defaults to current)
     * @return Result containing the migrated settings
     */
    suspend fun migrateSettings(
        settings: UserSettings,
        fromVersion: Int,
        toVersion: Int = CURRENT_VERSION
    ): Result<UserSettings> {
        return try {
            if (fromVersion < MIN_MIGRATION_VERSION) {
                return Result.error(
                    SettingsError.MigrationError(
                        "Settings version $fromVersion is no longer supported. Minimum migration version is $MIN_MIGRATION_VERSION",
                        fromVersion = fromVersion,
                        toVersion = toVersion
                    )
                )
            }
            
            if (fromVersion == toVersion) {
                return Result.success(settings)
            }
            
            var migratedSettings = settings
            
            // Apply migrations step by step
            for (version in fromVersion until toVersion) {
                migratedSettings = when (version) {
                    0 -> migrateFromV0ToV1(migratedSettings)
                    // Add more migration cases as needed
                    else -> migratedSettings
                }
            }
            
            // Update version and timestamp
            val finalSettings = migratedSettings.copy(
                version = toVersion,
                lastModified = Clock.System.now()
            )
            
            Result.success(finalSettings)
        } catch (e: Exception) {
            Result.error(
                SettingsError.MigrationError(
                    "Failed to migrate settings from version $fromVersion to $toVersion: ${e.message}",
                    fromVersion = fromVersion,
                    toVersion = toVersion,
                    cause = e
                )
            )
        }
    }
    
    /**
     * Checks if settings need migration.
     * 
     * @param settings The settings to check
     * @return True if migration is needed, false otherwise
     */
    fun needsMigration(settings: UserSettings): Boolean {
        return settings.version < CURRENT_VERSION
    }
    
    /**
     * Gets the current schema version.
     * 
     * @return The current version number
     */
    fun getCurrentVersion(): Int = CURRENT_VERSION
    
    /**
     * Gets the minimum supported version.
     * 
     * @return The minimum supported version number
     */
    fun getMinSupportedVersion(): Int = MIN_SUPPORTED_VERSION
    
    /**
     * Validates that a settings object is compatible with the current version.
     * 
     * @param settings The settings to validate
     * @return Result indicating if the settings are valid
     */
    fun validateSettingsVersion(settings: UserSettings): Result<Unit> {
        return when {
            settings.version < MIN_SUPPORTED_VERSION -> {
                Result.error(
                    SettingsError.MigrationError(
                        "Settings version ${settings.version} is no longer supported",
                        fromVersion = settings.version,
                        toVersion = CURRENT_VERSION
                    )
                )
            }
            settings.version > CURRENT_VERSION -> {
                Result.error(
                    SettingsError.MigrationError(
                        "Settings version ${settings.version} is from a newer app version",
                        fromVersion = settings.version,
                        toVersion = CURRENT_VERSION
                    )
                )
            }
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Creates default settings for a specific version.
     * 
     * @param userId The user ID
     * @param version The version to create defaults for
     * @return Default settings for the specified version
     */
    fun createDefaultSettingsForVersion(userId: String, version: Int): UserSettings {
        return when (version) {
            1 -> UserSettings.createDefault(userId)
            else -> UserSettings.createDefault(userId)
        }
    }
    
    /**
     * Gets migration information for a specific version upgrade.
     * 
     * @param fromVersion The source version
     * @param toVersion The target version
     * @return Migration information
     */
    fun getMigrationInfo(fromVersion: Int, toVersion: Int): MigrationInfo {
        val changes = mutableListOf<String>()
        val breakingChanges = mutableListOf<String>()
        
        for (version in fromVersion until toVersion) {
            when (version) {
                0 -> {
                    changes.add("Added comprehensive settings structure")
                    changes.add("Added notification preferences")
                    changes.add("Added cycle preferences")
                    changes.add("Added privacy preferences")
                    changes.add("Added display preferences")
                    changes.add("Added sync preferences")
                }
                // Add more version-specific changes as needed
            }
        }
        
        return MigrationInfo(
            fromVersion = fromVersion,
            toVersion = toVersion,
            changes = changes,
            breakingChanges = breakingChanges,
            requiresUserInput = breakingChanges.isNotEmpty()
        )
    }
    
    // Migration methods for specific version upgrades
    
    private fun migrateFromV0ToV1(settings: UserSettings): UserSettings {
        // V0 to V1: Initial comprehensive settings structure
        // This would handle migration from a basic settings structure to the full one
        // Preserve all existing user customizations
        
        return settings.copy(
            // Preserve all existing preferences - they already have the user's customizations
            unitPreferences = settings.unitPreferences,
            notificationPreferences = settings.notificationPreferences,
            cyclePreferences = settings.cyclePreferences,
            privacyPreferences = settings.privacyPreferences,
            displayPreferences = settings.displayPreferences,
            syncPreferences = settings.syncPreferences,
            version = 1
        )
    }
    
    // Future migration methods would be added here
    // private fun migrateFromV1ToV2(settings: UserSettings): UserSettings { ... }
    // private fun migrateFromV2ToV3(settings: UserSettings): UserSettings { ... }
}

/**
 * Information about a settings migration
 */
data class MigrationInfo(
    val fromVersion: Int,
    val toVersion: Int,
    val changes: List<String>,
    val breakingChanges: List<String>,
    val requiresUserInput: Boolean
)

/**
 * Result of a migration operation
 */
data class MigrationResult(
    val migratedSettings: UserSettings,
    val migrationInfo: MigrationInfo,
    val success: Boolean,
    val errors: List<String> = emptyList()
)