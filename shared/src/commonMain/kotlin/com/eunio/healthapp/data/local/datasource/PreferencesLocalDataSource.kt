package com.eunio.healthapp.data.local.datasource

import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result

/**
 * Local data source interface for user preferences operations.
 * Handles local storage of user preferences using SQLDelight.
 */
interface PreferencesLocalDataSource {
    
    /**
     * Retrieves user preferences for the current user.
     * @return UserPreferences if found, null if not found
     */
    suspend fun getPreferences(): UserPreferences?
    
    /**
     * Retrieves user preferences for a specific user.
     * @param userId The user ID to get preferences for
     * @return UserPreferences if found, null if not found
     */
    suspend fun getPreferences(userId: String): UserPreferences?
    
    /**
     * Saves user preferences to local storage.
     * @param preferences The preferences to save
     * @return Result indicating success or failure
     */
    suspend fun savePreferences(preferences: UserPreferences): Result<Unit>
    
    /**
     * Retrieves all preferences that have pending sync status.
     * @return List of UserPreferences with PENDING sync status
     */
    suspend fun getPendingSyncPreferences(): List<UserPreferences>
    
    /**
     * Marks preferences as synced for a specific user.
     * @param userId The user ID to mark as synced
     * @return Result indicating success or failure
     */
    suspend fun markAsSynced(userId: String): Result<Unit>
    
    /**
     * Marks preferences as failed sync for a specific user.
     * @param userId The user ID to mark as failed
     * @return Result indicating success or failure
     */
    suspend fun markAsFailed(userId: String): Result<Unit>
    
    /**
     * Clears all preferences from local storage.
     * @return Result indicating success or failure
     */
    suspend fun clearPreferences(): Result<Unit>
    
    /**
     * Clears preferences for a specific user.
     * @param userId The user ID to clear preferences for
     * @return Result indicating success or failure
     */
    suspend fun clearPreferences(userId: String): Result<Unit>
}