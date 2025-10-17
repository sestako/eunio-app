package com.eunio.healthapp.domain.service

import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.local.dao.UserPreferencesDao
import com.eunio.healthapp.data.local.dao.UserSettingsDao

/**
 * Service interface for database operations and connection management.
 * 
 * Provides centralized access to all data access objects (DAOs) and manages
 * database lifecycle, connection health, and error recovery.
 */
interface DatabaseService {
    
    /**
     * Gets the User DAO for user-related database operations
     * @return UserDao instance for user data operations
     * @throws DatabaseServiceException if DAO cannot be created
     */
    suspend fun getUserDao(): UserDao
    
    /**
     * Gets the Daily Log DAO for daily logging database operations
     * @return DailyLogDao instance for daily log operations
     * @throws DatabaseServiceException if DAO cannot be created
     */
    suspend fun getDailyLogDao(): DailyLogDao
    
    /**
     * Gets the User Preferences DAO for preferences database operations
     * @return UserPreferencesDao instance for preferences operations
     * @throws DatabaseServiceException if DAO cannot be created
     */
    suspend fun getUserPreferencesDao(): UserPreferencesDao
    
    /**
     * Gets the User Settings DAO for settings database operations
     * @return UserSettingsDao instance for settings operations
     * @throws DatabaseServiceException if DAO cannot be created
     */
    suspend fun getUserSettingsDao(): UserSettingsDao
    
    /**
     * Checks if the database connection is healthy and operational
     * @return true if database is accessible and functional, false otherwise
     */
    suspend fun isHealthy(): Boolean
    
    /**
     * Attempts to recover from database connection issues
     * @return Result indicating success or failure of recovery attempt
     */
    suspend fun recover(): Result<Unit>
    
    /**
     * Performs database maintenance operations like cleanup and optimization
     * @return Result indicating success or failure of maintenance operations
     */
    suspend fun performMaintenance(): Result<Unit>
    
    /**
     * Closes database connections and cleans up resources
     * Should be called when the service is no longer needed
     */
    suspend fun close()
}

/**
 * Exception thrown when database service operations fail
 */
class DatabaseServiceException(message: String, cause: Throwable? = null) : Exception(message, cause)