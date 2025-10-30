package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.data.local.DatabaseManager

/**
 * iOS implementation of PreferencesLocalDataSourceFactory.
 * Creates IOSPreferencesLocalDataSource with NSUserDefaults caching.
 */
actual class PlatformPreferencesLocalDataSourceFactory actual constructor() : PreferencesLocalDataSourceFactory {
    
    actual override fun create(databaseManager: DatabaseManager): PreferencesLocalDataSource {
        return IOSPreferencesLocalDataSource(databaseManager)
    }
}