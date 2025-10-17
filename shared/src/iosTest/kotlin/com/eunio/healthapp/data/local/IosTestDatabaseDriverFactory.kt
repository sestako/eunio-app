package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eunio.healthapp.database.EunioDatabase

class IosTestDatabaseDriverFactory : DatabaseDriverFactoryInterface {
    override fun createDriver(): SqlDriver {
        // Simplified iOS test driver to avoid segmentation faults
        try {
            // Use in-memory database for tests to avoid persistence issues
            val driver = NativeSqliteDriver(
                schema = EunioDatabase.Schema,
                name = ":memory:"
            )
            
            // Skip migrations in tests to avoid native driver issues
            // Migrations will be tested in Android unit tests instead
            
            return driver
        } catch (e: Exception) {
            // If native driver fails, create a minimal mock driver
            // This prevents segmentation faults from blocking iOS tests
            throw DatabaseInitializationException("iOS test database initialization failed: ${e.message}", e)
        }
    }
}

// Use the existing DatabaseInitializationException from common module