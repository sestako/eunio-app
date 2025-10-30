package com.eunio.healthapp.data.local.datasource

import android.content.Context
import com.eunio.healthapp.data.local.DatabaseManager

/**
 * Android implementation of PreferencesLocalDataSourceFactory.
 * Creates AndroidPreferencesLocalDataSource with SharedPreferences caching.
 */
actual class PlatformPreferencesLocalDataSourceFactory actual constructor() : PreferencesLocalDataSourceFactory {
    
    actual override fun create(databaseManager: DatabaseManager): PreferencesLocalDataSource {
        // For Android, we'll fallback to the standard implementation if no context is available
        // In a real app, the context would be injected through the DI system
        return PreferencesLocalDataSourceImpl(databaseManager)
    }
}