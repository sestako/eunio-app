package com.eunio.healthapp.data.sync

import kotlin.test.*

/**
 * Tests for DailyLogMigration utility.
 * 
 * These tests use a mock implementation to verify the migration logic
 * without requiring actual Firebase connectivity.
 */
class DailyLogMigrationTest {
    
    private lateinit var migration: MockDailyLogMigration
    
    @BeforeTest
    fun setup() {
        migration = MockDailyLogMigration()
    }
    
    @Test
    fun `migrateLegacyLogs with no legacy data returns success with zero counts`() = runTest {
        // Given: No legacy data
        val userId = "user123"
        
        // When: Migration is run
        val result = migration.migrateLegacyLogs(userId)
        
        // Then: Success with zero counts
        assertTrue(result.success)
        assertEquals(0, result.migratedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `migrateLegacyLogs successfully migrates documents`() = runTest {
        // Given: Legacy data exists
        val userId = "user123"
        migration.addLegacyDocument(userId, "2025-10-01", mapOf(
            "logId" to "2025-10-01",
            "dateEpochDays" to 20259L,
            "notes" to "Test note"
        ))
        migration.addLegacyDocument(userId, "2025-10-02", mapOf(
            "logId" to "2025-10-02",
            "dateEpochDays" to 20260L,
            "notes" to "Another note"
        ))
        
        // When: Migration is run
        val result = migration.migrateLegacyLogs(userId)
        
        // Then: Documents are migrated
        assertTrue(result.success)
        assertEquals(2, result.migratedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(0, result.errorCount)
        assertTrue(result.errors.isEmpty())
        
        // Verify documents exist at new path
        assertTrue(migration.documentExistsAtNewPath(userId, "2025-10-01"))
        assertTrue(migration.documentExistsAtNewPath(userId, "2025-10-02"))
    }
    
    @Test
    fun `migrateLegacyLogs skips documents that already exist at new path`() = runTest {
        // Given: Legacy data and some documents already at new path
        val userId = "user123"
        migration.addLegacyDocument(userId, "2025-10-01", mapOf(
            "logId" to "2025-10-01",
            "notes" to "Old note"
        ))
        migration.addLegacyDocument(userId, "2025-10-02", mapOf(
            "logId" to "2025-10-02",
            "notes" to "Another note"
        ))
        
        // Document already exists at new path
        migration.addNewDocument(userId, "2025-10-01", mapOf(
            "logId" to "2025-10-01",
            "notes" to "Existing note"
        ))
        
        // When: Migration is run
        val result = migration.migrateLegacyLogs(userId)
        
        // Then: One document migrated, one skipped
        assertTrue(result.success)
        assertEquals(1, result.migratedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(0, result.errorCount)
        
        // Verify existing document was not overwritten
        val existingDoc = migration.getNewDocument(userId, "2025-10-01")
        assertEquals("Existing note", existingDoc?.get("notes"))
    }
    
    @Test
    fun `migrateLegacyLogs is idempotent - can be run multiple times safely`() = runTest {
        // Given: Legacy data
        val userId = "user123"
        migration.addLegacyDocument(userId, "2025-10-01", mapOf(
            "logId" to "2025-10-01",
            "notes" to "Test note"
        ))
        
        // When: Migration is run twice
        val result1 = migration.migrateLegacyLogs(userId)
        val result2 = migration.migrateLegacyLogs(userId)
        
        // Then: First run migrates, second run skips
        assertEquals(1, result1.migratedCount)
        assertEquals(0, result1.skippedCount)
        
        assertEquals(0, result2.migratedCount)
        assertEquals(1, result2.skippedCount)
        
        // Both runs succeed
        assertTrue(result1.success)
        assertTrue(result2.success)
    }
    
    @Test
    fun `migrateLegacyLogs continues on individual document errors`() = runTest {
        // Given: Legacy data with one document that will fail
        val userId = "user123"
        migration.addLegacyDocument(userId, "2025-10-01", mapOf(
            "logId" to "2025-10-01",
            "notes" to "Good note"
        ))
        migration.addLegacyDocument(userId, "error-doc", mapOf(
            "logId" to "error-doc",
            "notes" to "This will fail"
        ))
        migration.addLegacyDocument(userId, "2025-10-03", mapOf(
            "logId" to "2025-10-03",
            "notes" to "Another good note"
        ))
        
        // Configure mock to fail on specific document
        migration.setFailOnCopy("error-doc")
        
        // When: Migration is run
        val result = migration.migrateLegacyLogs(userId)
        
        // Then: Good documents migrated, error document recorded
        assertTrue(result.success) // Still success if some documents migrated
        assertEquals(2, result.migratedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(1, result.errorCount)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("error-doc"))
    }
    
    @Test
    fun `migrateLegacyLogs handles blank userId`() = runTest {
        // When/Then: Blank userId throws exception
        assertFailsWith<IllegalArgumentException> {
            migration.migrateLegacyLogs("")
        }
    }
    
    @Test
    fun `MigrationResult summary provides readable output`() {
        // Given: Migration result with various counts
        val result = MigrationResult(
            success = true,
            migratedCount = 5,
            skippedCount = 2,
            errorCount = 1,
            errors = listOf("Failed to migrate log xyz: Network error")
        )
        
        // When: Summary is generated
        val summary = result.summary()
        
        // Then: Summary contains all information
        assertTrue(summary.contains("completed"))
        assertTrue(summary.contains("Migrated: 5"))
        assertTrue(summary.contains("Skipped: 2"))
        assertTrue(summary.contains("Errors: 1"))
        assertTrue(summary.contains("Failed to migrate log xyz"))
    }
    
    @Test
    fun `MigrationResult summary handles zero skipped count`() {
        // Given: Migration result with no skipped documents
        val result = MigrationResult(
            success = true,
            migratedCount = 5,
            skippedCount = 0,
            errorCount = 0,
            errors = emptyList()
        )
        
        // When: Summary is generated
        val summary = result.summary()
        
        // Then: Skipped line is not included
        assertFalse(summary.contains("Skipped:"))
    }
    
    @Test
    fun `migrateLegacyLogs logs progress for each document`() = runTest {
        // Given: Multiple legacy documents
        val userId = "user123"
        for (i in 1..5) {
            migration.addLegacyDocument(userId, "2025-10-0$i", mapOf(
                "logId" to "2025-10-0$i",
                "notes" to "Note $i"
            ))
        }
        
        // When: Migration is run
        val result = migration.migrateLegacyLogs(userId)
        
        // Then: All documents migrated
        assertEquals(5, result.migratedCount)
        assertTrue(result.success)
        
        // Verify mock's internal operations were logged
        val logs = migration.getLogEntries()
        assertTrue(logs.any { it.contains("QUERY_LEGACY") })
        assertTrue(logs.any { it.contains("CHECK_EXISTS") })
        assertTrue(logs.count { it.contains("COPY") } == 5)
    }
}

/**
 * Mock implementation of DailyLogMigration for testing.
 */
private class MockDailyLogMigration : BaseDailyLogMigration() {
    
    private val legacyDocuments = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
    private val newDocuments = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()
    private val failOnCopyIds = mutableSetOf<String>()
    private val logEntries = mutableListOf<String>()
    
    fun addLegacyDocument(userId: String, logId: String, data: Map<String, Any?>) {
        legacyDocuments.getOrPut(userId) { mutableMapOf() }[logId] = data
    }
    
    fun addNewDocument(userId: String, logId: String, data: Map<String, Any?>) {
        newDocuments.getOrPut(userId) { mutableMapOf() }[logId] = data
    }
    
    fun setFailOnCopy(logId: String) {
        failOnCopyIds.add(logId)
    }
    
    fun documentExistsAtNewPath(userId: String, logId: String): Boolean {
        return newDocuments[userId]?.containsKey(logId) ?: false
    }
    
    fun getNewDocument(userId: String, logId: String): Map<String, Any?>? {
        return newDocuments[userId]?.get(logId)
    }
    
    fun getLogEntries(): List<String> = logEntries.toList()
    
    override suspend fun queryLegacyCollection(userId: String): Map<String, Map<String, Any?>> {
        logEntries.add("QUERY_LEGACY: userId=$userId")
        return legacyDocuments[userId]?.toMap() ?: emptyMap()
    }
    
    override suspend fun checkDocumentExists(userId: String, logId: String): Boolean {
        logEntries.add("CHECK_EXISTS: userId=$userId, logId=$logId")
        return newDocuments[userId]?.containsKey(logId) ?: false
    }
    
    override suspend fun copyDocument(
        userId: String,
        logId: String,
        documentData: Map<String, Any?>
    ) {
        logEntries.add("COPY: userId=$userId, logId=$logId")
        
        if (failOnCopyIds.contains(logId)) {
            throw Exception("Simulated copy failure for $logId")
        }
        
        newDocuments.getOrPut(userId) { mutableMapOf() }[logId] = documentData
    }
}

/**
 * Simple test coroutine runner for common tests
 */
private fun runTest(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking {
        block()
    }
}
