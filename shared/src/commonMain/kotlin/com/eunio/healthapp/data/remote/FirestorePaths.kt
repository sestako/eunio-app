package com.eunio.healthapp.data.remote

/**
 * Centralized utility for generating consistent Firestore collection and document paths.
 * 
 * This ensures all platforms (iOS, Android, shared Kotlin) use the same path structure
 * for Firebase operations, preventing sync issues caused by path inconsistencies.
 * 
 * Standard path structure:
 * - Users: `users/{userId}`
 * - Daily Logs: `users/{userId}/dailyLogs/{logId}`
 * - Cycles: `users/{userId}/cycles/{cycleId}`
 * - Insights: `users/{userId}/insights/{insightId}`
 */
object FirestorePaths {
    
    // Collection names
    private const val USERS_COLLECTION = "users"
    private const val DAILY_LOGS_COLLECTION = "dailyLogs"
    private const val CYCLES_COLLECTION = "cycles"
    private const val INSIGHTS_COLLECTION = "insights"
    private const val HEALTH_REPORTS_COLLECTION = "healthReports"
    private const val SYNC_METADATA_COLLECTION = "syncMetadata"
    private const val SHARED_REPORTS_COLLECTION = "sharedReports"
    
    /**
     * Generates the Firestore document path for a specific user.
     * 
     * @param userId The user ID
     * @return Document path: `users/{userId}`
     * 
     * Example: `users/abc123`
     */
    fun userDoc(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "$USERS_COLLECTION/$userId"
    }
    
    /**
     * Generates the Firestore collection path for all daily logs of a user.
     * 
     * @param userId The user ID
     * @return Collection path: `users/{userId}/dailyLogs`
     * 
     * Example: `users/abc123/dailyLogs`
     */
    fun dailyLogsCollection(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION"
    }
    
    /**
     * Generates the Firestore document path for a specific daily log.
     * 
     * @param userId The user ID
     * @param logId The log ID (format: yyyy-MM-dd in UTC)
     * @return Full document path: `users/{userId}/dailyLogs/{logId}`
     * 
     * Example: `users/abc123/dailyLogs/2025-12-10`
     */
    fun dailyLogDoc(userId: String, logId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(logId.isNotBlank()) { "Log ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$DAILY_LOGS_COLLECTION/$logId"
    }
    
    /**
     * Generates the Firestore collection path for all cycles of a user.
     * 
     * @param userId The user ID
     * @return Collection path: `users/{userId}/cycles`
     * 
     * Example: `users/abc123/cycles`
     */
    fun cyclesCollection(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$CYCLES_COLLECTION"
    }
    
    /**
     * Generates the Firestore document path for a specific cycle.
     * 
     * @param userId The user ID
     * @param cycleId The cycle ID
     * @return Full document path: `users/{userId}/cycles/{cycleId}`
     * 
     * Example: `users/abc123/cycles/cycle_001`
     */
    fun cycleDoc(userId: String, cycleId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(cycleId.isNotBlank()) { "Cycle ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$CYCLES_COLLECTION/$cycleId"
    }
    
    /**
     * Generates the Firestore collection path for all insights of a user.
     * 
     * @param userId The user ID
     * @return Collection path: `users/{userId}/insights`
     * 
     * Example: `users/abc123/insights`
     */
    fun insightsCollection(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$INSIGHTS_COLLECTION"
    }
    
    /**
     * Generates the Firestore document path for a specific insight.
     * 
     * @param userId The user ID
     * @param insightId The insight ID
     * @return Full document path: `users/{userId}/insights/{insightId}`
     * 
     * Example: `users/abc123/insights/insight_001`
     */
    fun insightDoc(userId: String, insightId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(insightId.isNotBlank()) { "Insight ID cannot be blank" }
        return "$USERS_COLLECTION/$userId/$INSIGHTS_COLLECTION/$insightId"
    }
    
    /**
     * Generates the Firestore document path for a health report.
     * 
     * @param reportId The report ID
     * @return Document path: `healthReports/{reportId}`
     * 
     * Example: `healthReports/report_001`
     */
    fun healthReportDoc(reportId: String): String {
        require(reportId.isNotBlank()) { "Report ID cannot be blank" }
        return "$HEALTH_REPORTS_COLLECTION/$reportId"
    }
    
    /**
     * Generates the Firestore document path for sync metadata.
     * 
     * @param userId The user ID
     * @return Document path: `syncMetadata/{userId}`
     * 
     * Example: `syncMetadata/abc123`
     */
    fun syncMetadataDoc(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "$SYNC_METADATA_COLLECTION/$userId"
    }
    
    /**
     * Generates the Firestore document path for a shared report.
     * 
     * @param shareId The share ID
     * @return Document path: `sharedReports/{shareId}`
     * 
     * Example: `sharedReports/share_001`
     */
    fun sharedReportDoc(shareId: String): String {
        require(shareId.isNotBlank()) { "Share ID cannot be blank" }
        return "$SHARED_REPORTS_COLLECTION/$shareId"
    }
    
    /**
     * Returns the collection name for health reports.
     * 
     * @return Collection name: `healthReports`
     */
    fun healthReportsCollection(): String {
        return HEALTH_REPORTS_COLLECTION
    }
    
    /**
     * Returns the collection name for shared reports.
     * 
     * @return Collection name: `sharedReports`
     */
    fun sharedReportsCollection(): String {
        return SHARED_REPORTS_COLLECTION
    }
    
    /**
     * Returns the collection name for users.
     * 
     * @return Collection name: `users`
     */
    fun usersCollection(): String {
        return USERS_COLLECTION
    }
    
    // Legacy path support (for migration purposes only)
    
    /**
     * Generates the legacy Firestore collection path for daily logs.
     * 
     * **DEPRECATED**: This path is only for migration purposes.
     * Use [dailyLogsCollection] for all new operations.
     * 
     * @param userId The user ID
     * @return Legacy collection path: `daily_logs/{userId}/logs`
     */
    @Deprecated(
        message = "Legacy path for migration only. Use dailyLogsCollection() instead.",
        replaceWith = ReplaceWith("dailyLogsCollection(userId)")
    )
    fun legacyDailyLogsCollection(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        return "daily_logs/$userId/logs"
    }
    
    /**
     * Generates the legacy Firestore document path for a daily log.
     * 
     * **DEPRECATED**: This path is only for migration purposes.
     * Use [dailyLogDoc] for all new operations.
     * 
     * @param userId The user ID
     * @param logId The log ID
     * @return Legacy document path: `daily_logs/{userId}/logs/{logId}`
     */
    @Deprecated(
        message = "Legacy path for migration only. Use dailyLogDoc() instead.",
        replaceWith = ReplaceWith("dailyLogDoc(userId, logId)")
    )
    fun legacyDailyLogDoc(userId: String, logId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(logId.isNotBlank()) { "Log ID cannot be blank" }
        return "daily_logs/$userId/logs/$logId"
    }
}
