package com.eunio.healthapp.data.sync

import com.eunio.healthapp.data.remote.FirestorePaths
import com.eunio.healthapp.domain.util.StructuredLogger

/**
 * Utility for migrating daily logs from legacy Firebase path to new standardized path.
 * 
 * Legacy path: `daily_logs/{userId}/logs/{logId}`
 * New path: `users/{userId}/dailyLogs/{logId}`
 * 
 * This migration is idempotent and can be safely run multiple times.
 */
interface DailyLogMigration {
    /**
     * Migrates all daily logs for a user from legacy path to new path.
     * 
     * @param userId The user ID whose logs should be migrated
     * @return MigrationResult containing counts and any errors encountered
     */
    suspend fun migrateLegacyLogs(userId: String): MigrationResult
}

/**
 * Result of a migration operation
 * 
 * @property success Whether the migration completed without critical errors
 * @property migratedCount Number of documents successfully migrated
 * @property skippedCount Number of documents skipped (already exist at new path)
 * @property errorCount Number of documents that failed to migrate
 * @property errors List of error messages for failed migrations
 */
data class MigrationResult(
    val success: Boolean,
    val migratedCount: Int,
    val skippedCount: Int = 0,
    val errorCount: Int,
    val errors: List<String>
) {
    /**
     * Returns a human-readable summary of the migration
     */
    fun summary(): String {
        return buildString {
            appendLine("Migration ${if (success) "completed" else "failed"}")
            appendLine("Migrated: $migratedCount documents")
            if (skippedCount > 0) {
                appendLine("Skipped: $skippedCount documents (already exist)")
            }
            if (errorCount > 0) {
                appendLine("Errors: $errorCount documents")
                errors.forEach { error ->
                    appendLine("  - $error")
                }
            }
        }
    }
}

/**
 * Platform-agnostic base implementation with common logic.
 * Platform-specific implementations provide the actual Firestore access.
 */
abstract class BaseDailyLogMigration : DailyLogMigration {
    
    protected val logTag = "DailyLogMigration"
    
    override suspend fun migrateLegacyLogs(userId: String): MigrationResult {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        
        StructuredLogger.logStructured(logTag, "MIGRATION_START", mapOf("userId" to userId))
        
        var migratedCount = 0
        var skippedCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Get legacy collection path
            @Suppress("DEPRECATION")
            val legacyPath = FirestorePaths.legacyDailyLogsCollection(userId)
            val newPath = FirestorePaths.dailyLogsCollection(userId)
            
            StructuredLogger.logStructured(logTag, "MIGRATION_PATHS", mapOf(
                "legacyPath" to legacyPath,
                "newPath" to newPath
            ))
            
            // Query all documents from legacy collection
            val legacyDocuments = queryLegacyCollection(userId)
            
            StructuredLogger.logStructured(logTag, "MIGRATION_FOUND", mapOf(
                "userId" to userId,
                "documentCount" to legacyDocuments.size
            ))
            
            if (legacyDocuments.isEmpty()) {
                StructuredLogger.logStructured(logTag, "MIGRATION_NO_DATA", mapOf("userId" to userId))
                return MigrationResult(
                    success = true,
                    migratedCount = 0,
                    skippedCount = 0,
                    errorCount = 0,
                    errors = emptyList()
                )
            }
            
            // Process each document
            for ((logId, documentData) in legacyDocuments) {
                try {
                    // Check if document already exists at new path
                    val existsAtNewPath = checkDocumentExists(userId, logId)
                    
                    if (existsAtNewPath) {
                        StructuredLogger.logStructured(logTag, "MIGRATION_SKIP", mapOf(
                            "userId" to userId,
                            "logId" to logId,
                            "reason" to "Document already exists at new path"
                        ))
                        skippedCount++
                        continue
                    }
                    
                    // Copy document to new path
                    copyDocument(userId, logId, documentData)
                    
                    migratedCount++
                    StructuredLogger.logStructured(logTag, "MIGRATION_SUCCESS", mapOf(
                        "userId" to userId,
                        "logId" to logId,
                        "progress" to "$migratedCount/${legacyDocuments.size}"
                    ))
                    
                } catch (e: Exception) {
                    errorCount++
                    val errorMsg = "Failed to migrate log $logId: ${e.message}"
                    errors.add(errorMsg)
                    StructuredLogger.logStructured(logTag, "MIGRATION_ERROR", mapOf(
                        "userId" to userId,
                        "logId" to logId,
                        "error" to (e.message ?: "Unknown error")
                    ))
                }
            }
            
            val success = errorCount == 0 || (migratedCount + skippedCount) > 0
            
            StructuredLogger.logStructured(logTag, "MIGRATION_COMPLETE", mapOf(
                "userId" to userId,
                "success" to success,
                "migratedCount" to migratedCount,
                "skippedCount" to skippedCount,
                "errorCount" to errorCount
            ))
            
            return MigrationResult(
                success = success,
                migratedCount = migratedCount,
                skippedCount = skippedCount,
                errorCount = errorCount,
                errors = errors
            )
            
        } catch (e: Exception) {
            val errorMsg = "Migration failed for user $userId: ${e.message}"
            StructuredLogger.logStructured(logTag, "MIGRATION_FATAL", mapOf(
                "userId" to userId,
                "error" to (e.message ?: "Unknown error")
            ))
            
            return MigrationResult(
                success = false,
                migratedCount = migratedCount,
                skippedCount = skippedCount,
                errorCount = errorCount + 1,
                errors = errors + errorMsg
            )
        }
    }
    
    /**
     * Platform-specific: Query all documents from the legacy collection
     * @return Map of logId to document data
     */
    protected abstract suspend fun queryLegacyCollection(userId: String): Map<String, Map<String, Any?>>
    
    /**
     * Platform-specific: Check if a document exists at the new path
     */
    protected abstract suspend fun checkDocumentExists(userId: String, logId: String): Boolean
    
    /**
     * Platform-specific: Copy a document from legacy path to new path
     */
    protected abstract suspend fun copyDocument(
        userId: String,
        logId: String,
        documentData: Map<String, Any?>
    )
}
