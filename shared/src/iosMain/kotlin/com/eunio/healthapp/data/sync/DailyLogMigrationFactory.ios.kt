package com.eunio.healthapp.data.sync

/**
 * iOS implementation of DailyLogMigrationFactory.
 */
actual object DailyLogMigrationFactory {
    actual fun create(): DailyLogMigration {
        return IOSDailyLogMigration()
    }
}
