package com.eunio.healthapp.data.sync

/**
 * Factory for creating platform-specific DailyLogMigration instances.
 * 
 * Usage:
 * ```kotlin
 * val migration = DailyLogMigrationFactory.create()
 * val result = migration.migrateLegacyLogs(userId)
 * println(result.summary())
 * ```
 */
expect object DailyLogMigrationFactory {
    /**
     * Creates a platform-specific DailyLogMigration instance.
     * 
     * - Android: Returns AndroidDailyLogMigration with Firebase Firestore
     * - iOS: Returns IOSDailyLogMigration (currently mock, needs Firebase iOS SDK integration)
     */
    fun create(): DailyLogMigration
}
