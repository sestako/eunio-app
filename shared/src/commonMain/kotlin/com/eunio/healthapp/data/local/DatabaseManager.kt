package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.local.dao.UserPreferencesDao
import com.eunio.healthapp.data.local.dao.UserSettingsDao

class DatabaseManager(private val driverFactory: DatabaseDriverFactoryInterface) {
    
    private var database: EunioDatabase? = null
    private var driver: SqlDriver? = null
    
    private fun initializeDatabase(): EunioDatabase {
        try {
            val sqlDriver = driverFactory.createDriver()
            
            val db = EunioDatabase(sqlDriver)
            
            // Store references for proper cleanup
            driver = sqlDriver
            database = db
            
            return db
        } catch (e: Exception) {
            throw DatabaseInitializationException("Failed to initialize database", e)
        }
    }
    
    fun getDatabase(): EunioDatabase {
        return database ?: initializeDatabase()
    }
    
    fun getUserDao(): UserDao {
        return UserDao(getDatabase())
    }
    
    fun getDailyLogDao(): DailyLogDao {
        return DailyLogDao(getDatabase())
    }
    
    fun getUserPreferencesDao(): UserPreferencesDao {
        return UserPreferencesDao(getDatabase())
    }
    
    fun getUserSettingsDao(): UserSettingsDao {
        return UserSettingsDao(getDatabase())
    }
    
    /**
     * Closes the database connection and cleans up resources
     */
    fun closeDatabase() {
        try {
            driver?.close()
        } catch (e: Exception) {
            // Log error but don't throw - cleanup should be safe
            println("Warning: Error closing database driver: ${e.message}")
        } finally {
            database = null
            driver = null
        }
    }
    
    /**
     * Checks if the database is currently initialized
     */
    fun isDatabaseInitialized(): Boolean {
        return database != null && driver != null
    }
    
    /**
     * Forces database reinitialization (useful for testing or error recovery)
     */
    fun reinitializeDatabase(): EunioDatabase {
        closeDatabase()
        return initializeDatabase()
    }
}

/**
 * Exception thrown when database initialization fails
 */
class DatabaseInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)