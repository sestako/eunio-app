package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result

/**
 * Remote data source interface for user preferences operations.
 * Handles cloud storage of user preferences using Firestore.
 */
interface PreferencesRemoteDataSource {
    
    /**
     * Retrieves user preferences from remote storage.
     * @param userId The user ID to get preferences for
     * @return Result containing UserPreferences if found, null if not found
     */
    suspend fun getPreferences(userId: String): Result<UserPreferences?>
    
    /**
     * Saves user preferences to remote storage.
     * @param preferences The preferences to save
     * @return Result indicating success or failure
     */
    suspend fun savePreferences(preferences: UserPreferences): Result<Unit>
    
    /**
     * Updates user preferences in remote storage.
     * @param preferences The preferences to update
     * @return Result indicating success or failure
     */
    suspend fun updatePreferences(preferences: UserPreferences): Result<Unit>
    
    /**
     * Deletes user preferences from remote storage.
     * @param userId The user ID to delete preferences for
     * @return Result indicating success or failure
     */
    suspend fun deletePreferences(userId: String): Result<Unit>
    
    /**
     * Clears all preferences from remote storage.
     * This is typically used for account deletion or data cleanup.
     * @return Result indicating success or failure
     */
    suspend fun clearPreferences(): Result<Unit>
    
    /**
     * Checks if preferences exist for a user in remote storage.
     * @param userId The user ID to check
     * @return Result containing true if preferences exist, false otherwise
     */
    suspend fun preferencesExist(userId: String): Result<Boolean>
    
    /**
     * Gets the last modified timestamp for user preferences.
     * Used for conflict resolution during sync operations.
     * @param userId The user ID to get timestamp for
     * @return Result containing the timestamp in epoch seconds, null if not found
     */
    suspend fun getLastModifiedTimestamp(userId: String): Result<Long?>
}