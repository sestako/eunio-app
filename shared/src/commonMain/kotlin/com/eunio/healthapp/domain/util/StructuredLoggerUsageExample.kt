package com.eunio.healthapp.domain.util

/**
 * Example usage of StructuredLogger for daily log sync operations.
 * 
 * This file demonstrates the correct usage patterns for structured logging
 * in the daily log sync feature.
 */
object StructuredLoggerUsageExample {
    
    /**
     * Example: Logging the start of a save operation
     */
    fun exampleSaveStart(userId: String, logId: String, dateEpochDays: Long) {
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_START,
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "dateEpochDays" to dateEpochDays
            )
        )
        // Output: [DailyLogSync] SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
    }
    
    /**
     * Example: Logging a Firestore write operation
     */
    fun exampleFirestoreWrite(path: String, status: String, latencyMs: Long, error: String? = null) {
        val data = mutableMapOf<String, Any?>(
            "path" to path,
            "status" to status,
            "latencyMs" to latencyMs
        )
        
        if (error != null) {
            data["error"] = error
        }
        
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
            data = data
        )
        // Success output: [DailyLogSync] FIRESTORE_WRITE path=users/user123/dailyLogs/2025-10-04, status=SUCCESS, latencyMs=245
        // Error output: [DailyLogSync] FIRESTORE_WRITE path=users/user123/dailyLogs/2025-10-04, status=FAILED, latencyMs=245, error=Network timeout
    }
    
    /**
     * Example: Logging a load result
     */
    fun exampleLoadResult(
        path: String,
        found: Boolean,
        docUpdatedAt: Long?,
        localUpdatedAt: Long?
    ) {
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.LOAD_RESULT,
            data = mapOf(
                "path" to path,
                "found" to found,
                "docUpdatedAt" to docUpdatedAt,
                "localUpdatedAt" to localUpdatedAt
            )
        )
        // Output: [DailyLogSync] LOAD_RESULT path=users/user123/dailyLogs/2025-10-04, found=true, docUpdatedAt=1696392305, localUpdatedAt=1696392000
    }
    
    /**
     * Example: Logging a sync result with conflict resolution
     */
    fun exampleSyncResult(
        direction: String,
        merged: Boolean,
        winner: String,
        reason: String
    ) {
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SYNC_RESULT,
            data = mapOf(
                "direction" to direction,
                "merged" to merged,
                "winner" to winner,
                "reason" to reason
            )
        )
        // Output: [DailyLogSync] SYNC_RESULT direction=REMOTE_TO_LOCAL, merged=false, winner=REMOTE, reason=Remote updatedAt is newer
    }
    
    /**
     * Example: Logging an error
     */
    fun exampleSaveError(userId: String, logId: String, error: String) {
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_ERROR,
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "error" to error
            )
        )
        // Output: [DailyLogSync] SAVE_ERROR userId=user123, logId=2025-10-04, error=Database connection failed
    }
    
    /**
     * Example: Using string operation name for custom operations
     */
    fun exampleCustomOperation(operation: String, customData: Map<String, Any?>) {
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = operation,
            data = customData
        )
        // Output: [DailyLogSync] CUSTOM_OPERATION key1=value1, key2=value2
    }
}
