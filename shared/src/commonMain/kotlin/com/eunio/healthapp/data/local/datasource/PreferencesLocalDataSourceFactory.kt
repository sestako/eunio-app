package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.data.local.DatabaseManager

/**
 * Factory interface for creating platform-specific PreferencesLocalDataSource implementations.
 * Allows for platform-specific optimizations while maintaining a common interface.
 */
interface PreferencesLocalDataSourceFactory {
    /**
     * Creates a platform-optimized PreferencesLocalDataSource implementation.
     * 
     * @param databaseManager The database manager for persistence
     * @return Platform-specific PreferencesLocalDataSource implementation
     */
    fun create(databaseManager: DatabaseManager): PreferencesLocalDataSource
}

/**
 * Expected platform-specific implementation of PreferencesLocalDataSourceFactory.
 */
expect class PlatformPreferencesLocalDataSourceFactory() : PreferencesLocalDataSourceFactory