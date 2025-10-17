package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.Result

/**
 * Repository interface for user preferences operations.
 * Handles preference persistence, synchronization, and offline-first data access.
 */
interface PreferencesRepository {
    
    /**
     * Retrieves user preferences with local-first fallback logic.
     * Attempts to get from local storage first, then falls back to remote if available.
     * @return Result containing UserPreferences if found, null if not found
     */
    suspend fun getUserPreferences(): Result<UserPreferences?>
    
    /**
     * Retrieves user preferences for a specific user.
     * @param userId The user ID to get preferences for
     * @return Result containing UserPreferences if found, null if not found
     */
    suspend fun getUserPreferences(userId: String): Result<UserPreferences?>
    
    /**
     * Saves user preferences with immediate local save and background sync.
     * Always saves to local storage first, then attempts remote sync if connected.
     * @param preferences The preferences to save
     * @return Result indicating success or failure of local save
     */
    suspend fun saveUserPreferences(preferences: UserPreferences): Result<Unit>
    
    /**
     * Performs batch synchronization of pending preferences to remote storage.
     * Syncs all preferences marked as PENDING to the remote data source.
     * @return Result indicating success or failure of sync operation
     */
    suspend fun syncPreferences(): Result<Unit>
    
    /**
     * Clears all user preferences from both local and remote storage.
     * Used for data cleanup during account deletion or reset.
     * @return Result indicating success or failure
     */
    suspend fun clearPreferences(): Result<Unit>
    
    /**
     * Clears preferences for a specific user.
     * @param userId The user ID to clear preferences for
     * @return Result indicating success or failure
     */
    suspend fun clearPreferences(userId: String): Result<Unit>
    
    /**
     * Performs bidirectional sync with conflict resolution.
     * Compares local and remote data and resolves conflicts using last-write-wins strategy.
     * @param userId The user ID to sync preferences for
     * @return Result indicating success or failure
     */
    suspend fun syncWithConflictResolution(userId: String): Result<Unit>
    
    /**
     * Handles graceful degradation when offline.
     * Ensures local operations continue to work and queues sync operations.
     * @return Result indicating success or failure
     */
    suspend fun handleOfflineMode(): Result<Unit>
    
    /**
     * Gets sync statistics for monitoring and debugging.
     * @return Result containing sync statistics
     */
    suspend fun getSyncStatistics(): Result<com.eunio.healthapp.domain.model.SyncStatistics>
    
    /**
     * Recovers from sync failures by attempting to sync pending preferences after network recovery.
     * Waits for network connectivity and then attempts to sync all pending preferences.
     * @return Result indicating success or failure of recovery operation
     */
    suspend fun recoverFromSyncFailure(): Result<Unit>
}