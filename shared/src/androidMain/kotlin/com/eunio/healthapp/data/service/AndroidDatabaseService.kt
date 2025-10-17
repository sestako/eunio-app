package com.eunio.healthapp.data.service

import android.content.Context
import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.domain.service.DatabaseServiceException
import java.io.File

/**
 * Android-specific implementation of DatabaseService with platform-specific
 * optimizations and error handling.
 */
class AndroidDatabaseService(
    private val databaseManager: DatabaseManager,
    private val context: Context
) : DatabaseService by DatabaseServiceImpl(databaseManager) {
    
    override suspend fun performMaintenance(): Result<Unit> {
        return try {
            // Perform base maintenance first
            val baseResult = DatabaseServiceImpl(databaseManager).performMaintenance()
            if (baseResult.isFailure) {
                return baseResult
            }
            
            // Android-specific maintenance
            performAndroidSpecificMaintenance()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseServiceException("Android database maintenance failed", e))
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        // Check base health first
        val baseHealthy = DatabaseServiceImpl(databaseManager).isHealthy()
        if (!baseHealthy) {
            return false
        }
        
        // Android-specific health checks
        return performAndroidHealthChecks()
    }
    
    /**
     * Performs Android-specific maintenance operations
     */
    private fun performAndroidSpecificMaintenance() {
        try {
            // Check available storage space
            val dbFile = context.getDatabasePath("eunio_database.db")
            val availableSpace = dbFile.parentFile?.usableSpace ?: 0L
            val requiredSpace = 50 * 1024 * 1024L // 50MB minimum
            
            if (availableSpace < requiredSpace) {
                throw DatabaseServiceException("Insufficient storage space for database operations")
            }
            
            // Could add more Android-specific maintenance here:
            // - WAL checkpoint operations
            // - Cache cleanup
            // - Temporary file cleanup
            
        } catch (e: Exception) {
            throw DatabaseServiceException("Android maintenance operations failed", e)
        }
    }
    
    /**
     * Performs Android-specific health checks
     */
    private fun performAndroidHealthChecks(): Boolean {
        return try {
            // Check if database file exists and is accessible
            val dbFile = context.getDatabasePath("eunio_database.db")
            if (dbFile.exists() && !dbFile.canRead()) {
                return false
            }
            
            // Check available memory
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsageRatio = usedMemory.toDouble() / maxMemory.toDouble()
            
            // If memory usage is above 90%, consider unhealthy
            if (memoryUsageRatio > 0.9) {
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}