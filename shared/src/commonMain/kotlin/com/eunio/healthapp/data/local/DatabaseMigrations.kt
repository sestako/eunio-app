package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.SqlDriver

object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2
     * Add unit system support to User table and create UserPreferences table
     */
    val migration1to2 = AfterVersion(1) { driver: SqlDriver ->
        // Check if unitSystem column exists before adding it
        val columnExists = try {
            driver.executeQuery(
                null,
                "SELECT unitSystem FROM User LIMIT 1",
                { cursor -> cursor.next() },
                0
            ).value
            true
        } catch (e: Exception) {
            false
        }
        
        // Add unitSystem column to existing User table only if it doesn't exist
        if (!columnExists) {
            driver.execute(
                null,
                """
                ALTER TABLE User ADD COLUMN unitSystem TEXT NOT NULL DEFAULT 'METRIC';
                """.trimIndent(),
                0
            )
        }
        
        // Check if UserPreferences table exists before creating it
        val tableExists = try {
            driver.executeQuery(
                null,
                "SELECT name FROM sqlite_master WHERE type='table' AND name='UserPreferences'",
                { cursor -> cursor.next() },
                0
            ).value
            true
        } catch (e: Exception) {
            false
        }
        
        // Create UserPreferences table only if it doesn't exist
        if (!tableExists) {
            driver.execute(
                null,
                """
                CREATE TABLE UserPreferences (
                    userId TEXT NOT NULL PRIMARY KEY,
                    unitSystem TEXT NOT NULL DEFAULT 'METRIC',
                    isManuallySet INTEGER NOT NULL DEFAULT 0,
                    lastModified INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
                );
                """.trimIndent(),
                0
            )
            
            // Create index for faster preference lookups
            driver.execute(
                null,
                """
                CREATE INDEX idx_user_preferences_sync_status ON UserPreferences(syncStatus);
                """.trimIndent(),
                0
            )
        }
    }
    
    /**
     * Migration from version 2 to 3
     * Add comprehensive UserSettings table and SettingsBackup table for enhanced settings functionality
     */
    val migration2to3 = AfterVersion(2) { driver: SqlDriver ->
        // Check if UserSettings table exists
        val userSettingsExists = try {
            driver.executeQuery(
                null,
                "SELECT name FROM sqlite_master WHERE type='table' AND name='UserSettings'",
                { cursor -> cursor.next() },
                0
            ).value
            true
        } catch (e: Exception) {
            false
        }
        
        // Create UserSettings table only if it doesn't exist
        if (!userSettingsExists) {
            driver.execute(
                null,
                """
                CREATE TABLE UserSettings (
                    userId TEXT NOT NULL PRIMARY KEY,
                    unitPreferences TEXT NOT NULL,
                    notificationPreferences TEXT NOT NULL,
                    cyclePreferences TEXT NOT NULL,
                    privacyPreferences TEXT NOT NULL,
                    displayPreferences TEXT NOT NULL,
                    syncPreferences TEXT NOT NULL,
                    lastModified INTEGER NOT NULL,
                    syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                    version INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
                );
                """.trimIndent(),
                0
            )
            
            // Create performance optimization indexes for UserSettings
            driver.execute(
                null,
                """
                CREATE INDEX idx_user_settings_sync_status ON UserSettings(syncStatus);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_user_settings_last_modified ON UserSettings(lastModified DESC);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_user_settings_version ON UserSettings(version);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_user_settings_composite ON UserSettings(userId, syncStatus, lastModified);
                """.trimIndent(),
                0
            )
        }
        
        // Check if SettingsBackup table exists
        val settingsBackupExists = try {
            driver.executeQuery(
                null,
                "SELECT name FROM sqlite_master WHERE type='table' AND name='SettingsBackup'",
                { cursor -> cursor.next() },
                0
            ).value
            true
        } catch (e: Exception) {
            false
        }
        
        // Create SettingsBackup table only if it doesn't exist
        if (!settingsBackupExists) {
            driver.execute(
                null,
                """
                CREATE TABLE SettingsBackup (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    userId TEXT NOT NULL,
                    settingsData TEXT NOT NULL,
                    backupType TEXT NOT NULL DEFAULT 'MANUAL',
                    createdAt INTEGER NOT NULL,
                    dataSize INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (userId) REFERENCES User(id) ON DELETE CASCADE
                );
                """.trimIndent(),
                0
            )
            
            // Create performance optimization indexes for SettingsBackup
            driver.execute(
                null,
                """
                CREATE INDEX idx_settings_backup_user ON SettingsBackup(userId);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_settings_backup_created_at ON SettingsBackup(createdAt DESC);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_settings_backup_type ON SettingsBackup(backupType);
                """.trimIndent(),
                0
            )
            
            driver.execute(
                null,
                """
                CREATE INDEX idx_settings_backup_composite ON SettingsBackup(userId, backupType, createdAt);
                """.trimIndent(),
                0
            )
        }
    }
    
    /**
     * Migration from version 3 to 4
     * Add sync-related columns to DailyLog table for offline sync support
     */
    val migration3to4 = AfterVersion(3) { driver: SqlDriver ->
        // Check if sync columns already exist
        val syncColumnsExist = try {
            driver.executeQuery(
                null,
                "SELECT isSynced FROM DailyLog LIMIT 1",
                { cursor -> cursor.next() },
                0
            ).value
            true
        } catch (e: Exception) {
            false
        }
        
        // Add sync columns only if they don't exist
        if (!syncColumnsExist) {
            // Add isSynced column
            driver.execute(
                null,
                """
                ALTER TABLE DailyLog ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0;
                """.trimIndent(),
                0
            )
            
            // Add pendingSync column
            driver.execute(
                null,
                """
                ALTER TABLE DailyLog ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1;
                """.trimIndent(),
                0
            )
            
            // Add lastSyncAttempt column
            driver.execute(
                null,
                """
                ALTER TABLE DailyLog ADD COLUMN lastSyncAttempt INTEGER;
                """.trimIndent(),
                0
            )
            
            // Add syncRetryCount column
            driver.execute(
                null,
                """
                ALTER TABLE DailyLog ADD COLUMN syncRetryCount INTEGER NOT NULL DEFAULT 0;
                """.trimIndent(),
                0
            )
            
            println("✅ Migration 3→4: Added sync columns to DailyLog table")
        } else {
            println("ℹ️ Migration 3→4: Sync columns already exist, skipping")
        }
    }
    
    /**
     * Get all available migrations
     * Add new migrations to this array when schema changes are needed
     */
    fun getAllMigrations(): Array<AfterVersion> {
        return arrayOf(
            migration1to2,
            migration2to3,
            migration3to4
        )
    }
    
    /**
     * Get the current database version
     * Increment this when adding new migrations
     */
    const val CURRENT_VERSION = 4
}