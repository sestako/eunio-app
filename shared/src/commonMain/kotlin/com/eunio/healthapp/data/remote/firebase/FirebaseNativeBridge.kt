package com.eunio.healthapp.data.remote.firebase

/**
 * Platform-specific bridge to Firebase SDK.
 * This interface defines the contract for Firebase operations that need native SDK access.
 * 
 * - Android: Implemented using Firebase Android SDK
 * - iOS: Implemented using Firebase iOS SDK through Swift bridge
 */
expect class FirebaseNativeBridge() {
    
    /**
     * Save a daily log to Firestore.
     * 
     * @param userId The user ID
     * @param logId The log ID (document ID)
     * @param data The log data as a map
     * @return Result with Unit on success, or error message on failure
     */
    suspend fun saveDailyLog(
        userId: String,
        logId: String,
        data: Map<String, Any>
    ): Result<Unit>
    
    /**
     * Get a daily log from Firestore.
     * 
     * @param userId The user ID
     * @param logId The log ID (document ID)
     * @return Result with data map on success, null if not found, or error message on failure
     */
    suspend fun getDailyLog(
        userId: String,
        logId: String
    ): Result<Map<String, Any>?>
    
    /**
     * Get a daily log by date (epoch days).
     * 
     * @param userId The user ID
     * @param epochDays The date as epoch days since Unix epoch
     * @return Result with data map on success, null if not found, or error message on failure
     */
    suspend fun getDailyLogByDate(
        userId: String,
        epochDays: Long
    ): Result<Map<String, Any>?>
    
    /**
     * Get daily logs in a date range.
     * 
     * @param userId The user ID
     * @param startEpochDays Start date as epoch days
     * @param endEpochDays End date as epoch days
     * @return Result with list of data maps on success, or error message on failure
     */
    suspend fun getLogsInRange(
        userId: String,
        startEpochDays: Long,
        endEpochDays: Long
    ): Result<List<Map<String, Any>>>
    
    /**
     * Delete a daily log from Firestore.
     * 
     * @param userId The user ID
     * @param logId The log ID (document ID)
     * @return Result with Unit on success, or error message on failure
     */
    suspend fun deleteDailyLog(
        userId: String,
        logId: String
    ): Result<Unit>
    
    /**
     * Batch save multiple daily logs to Firestore using batch writes.
     * This is more efficient than individual writes and ensures atomicity.
     * 
     * @param userId The user ID
     * @param logsData List of log data maps to save
     * @return Result with Unit on success, or error message on failure
     */
    suspend fun batchSaveDailyLogs(
        userId: String,
        logsData: List<Map<String, Any>>
    ): Result<Unit>
    
    /**
     * Test the bridge connectivity.
     * @return true if the bridge is functional, false otherwise
     */
    fun testConnection(): Boolean
}
