package com.eunio.healthapp.domain.util

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for StructuredLogger utility
 */
class StructuredLoggerTest {
    
    @Test
    fun testLogOperationEnum() {
        // Verify all required log operations exist
        val operations = StructuredLogger.LogOperation.values()
        
        assertTrue(operations.contains(StructuredLogger.LogOperation.SAVE_START))
        assertTrue(operations.contains(StructuredLogger.LogOperation.FIRESTORE_WRITE))
        assertTrue(operations.contains(StructuredLogger.LogOperation.LOAD_RESULT))
        assertTrue(operations.contains(StructuredLogger.LogOperation.SYNC_RESULT))
    }
    
    @Test
    fun testLogStructuredWithEnum() {
        // Test that logging with enum doesn't throw
        val data = mapOf(
            "userId" to "user123",
            "logId" to "2025-10-04",
            "dateEpochDays" to 20259
        )
        
        // Should not throw
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_START,
            data = data
        )
    }
    
    @Test
    fun testLogStructuredWithString() {
        // Test that logging with string operation doesn't throw
        val data = mapOf(
            "path" to "users/user123/dailyLogs/2025-10-04",
            "status" to "SUCCESS",
            "latencyMs" to 245
        )
        
        // Should not throw
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = "FIRESTORE_WRITE",
            data = data
        )
    }
    
    @Test
    fun testLogStructuredWithNullValues() {
        // Test that null values are handled correctly
        val data = mapOf(
            "userId" to "user123",
            "error" to null,
            "latencyMs" to 100
        )
        
        // Should not throw
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.LOAD_RESULT,
            data = data
        )
    }
    
    @Test
    fun testLogStructuredWithMixedTypes() {
        // Test that different data types are handled correctly
        val data = mapOf(
            "userId" to "user123",
            "found" to true,
            "docUpdatedAt" to 1696392305L,
            "localUpdatedAt" to 1696392000L,
            "latencyMs" to 150.5
        )
        
        // Should not throw
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SYNC_RESULT,
            data = data
        )
    }
    
    @Test
    fun testLogStructuredWithEmptyData() {
        // Test that empty data map is handled correctly
        val data = emptyMap<String, Any?>()
        
        // Should not throw
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_START,
            data = data
        )
    }
    
    @Test
    fun testAllRequiredLogFormats() {
        // Test SAVE_START format
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SAVE_START,
            data = mapOf(
                "userId" to "user123",
                "logId" to "2025-10-04",
                "dateEpochDays" to 20259
            )
        )
        
        // Test FIRESTORE_WRITE format
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.FIRESTORE_WRITE,
            data = mapOf(
                "path" to "users/user123/dailyLogs/2025-10-04",
                "status" to "SUCCESS",
                "latencyMs" to 245
            )
        )
        
        // Test LOAD_RESULT format
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.LOAD_RESULT,
            data = mapOf(
                "path" to "users/user123/dailyLogs/2025-10-04",
                "found" to true,
                "docUpdatedAt" to 1696392305L,
                "localUpdatedAt" to 1696392000L
            )
        )
        
        // Test SYNC_RESULT format
        StructuredLogger.logStructured(
            tag = "DailyLogSync",
            operation = StructuredLogger.LogOperation.SYNC_RESULT,
            data = mapOf(
                "direction" to "REMOTE_TO_LOCAL",
                "merged" to false,
                "winner" to "REMOTE",
                "reason" to "Remote updatedAt is newer"
            )
        )
    }
}
