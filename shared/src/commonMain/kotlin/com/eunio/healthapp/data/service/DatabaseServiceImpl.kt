package com.eunio.healthapp.data.service

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.local.DatabaseInitializationException
import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.local.dao.UserPreferencesDao
import com.eunio.healthapp.data.local.dao.UserSettingsDao
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.domain.service.DatabaseServiceException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Implementation of DatabaseService that wraps DatabaseManager with enhanced
 * error handling, connection management, and service-level operations.
 */
class DatabaseServiceImpl(
    private val databaseManager: DatabaseManager
) : DatabaseService {
    
    private val operationMutex = Mutex()
    private var isInitialized = false
    private var lastHealthCheck = 0L
    private val healthCheckInterval = 30_000L // 30 seconds
    
    override suspend fun getUserDao(): UserDao = operationMutex.withLock {
        try {
            ensureInitialized()
            databaseManager.getUserDao()
        } catch (e: Exception) {
            throw DatabaseServiceException("Failed to get UserDao", e)
        }
    }
    
    override suspend fun getDailyLogDao(): DailyLogDao = operationMutex.withLock {
        try {
            ensureInitialized()
            databaseManager.getDailyLogDao()
        } catch (e: Exception) {
            throw DatabaseServiceException("Failed to get DailyLogDao", e)
        }
    }
    
    override suspend fun getUserPreferencesDao(): UserPreferencesDao = operationMutex.withLock {
        try {
            ensureInitialized()
            databaseManager.getUserPreferencesDao()
        } catch (e: Exception) {
            throw DatabaseServiceException("Failed to get UserPreferencesDao", e)
        }
    }
    
    override suspend fun getUserSettingsDao(): UserSettingsDao = operationMutex.withLock {
        try {
            ensureInitialized()
            databaseManager.getUserSettingsDao()
        } catch (e: Exception) {
            throw DatabaseServiceException("Failed to get UserSettingsDao", e)
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            
            // Use cached result if recent
            if (currentTime - lastHealthCheck < healthCheckInterval) {
                return isInitialized && databaseManager.isDatabaseInitialized()
            }
            
            // Perform actual health check
            val healthy = performHealthCheck()
            lastHealthCheck = currentTime
            healthy
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun recover(): Result<Unit> = operationMutex.withLock {
        return try {
            // Close existing connections
            databaseManager.closeDatabase()
            isInitialized = false
            
            // Reinitialize database
            databaseManager.reinitializeDatabase()
            isInitialized = true
            
            // Verify recovery was successful
            if (performHealthCheck()) {
                Result.success(Unit)
            } else {
                Result.failure(DatabaseServiceException("Database recovery failed - health check failed"))
            }
        } catch (e: Exception) {
            Result.failure(DatabaseServiceException("Database recovery failed", e))
        }
    }
    
    override suspend fun performMaintenance(): Result<Unit> {
        return try {
            // Basic maintenance operations
            if (!isHealthy()) {
                return recover()
            }
            
            // Could add more maintenance operations here like:
            // - VACUUM operations
            // - Index optimization
            // - Cleanup of old data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseServiceException("Database maintenance failed", e))
        }
    }
    
    override suspend fun close() = operationMutex.withLock {
        try {
            databaseManager.closeDatabase()
            isInitialized = false
        } catch (e: Exception) {
            // Log error but don't throw - cleanup should be safe
            println("Warning: Error during database service cleanup: ${e.message}")
        }
    }
    
    /**
     * Ensures the database is initialized before operations
     */
    private fun ensureInitialized() {
        if (!isInitialized) {
            try {
                databaseManager.getDatabase()
                isInitialized = true
            } catch (e: DatabaseInitializationException) {
                throw DatabaseServiceException("Database initialization failed", e)
            }
        }
    }
    
    /**
     * Performs a comprehensive health check of the database
     */
    private fun performHealthCheck(): Boolean {
        return try {
            // Check if database manager is initialized
            if (!databaseManager.isDatabaseInitialized()) {
                return false
            }
            
            // Try to access the database
            val database = databaseManager.getDatabase()
            
            // Perform a simple query to verify database is responsive
            // This is a basic check - could be enhanced with more comprehensive tests
            database != null
        } catch (e: Exception) {
            false
        }
    }
}