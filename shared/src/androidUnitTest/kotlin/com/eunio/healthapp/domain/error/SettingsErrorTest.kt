package com.eunio.healthapp.domain.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsErrorTest {
    
    @Test
    fun `ValidationError should contain field and value information`() {
        val error = SettingsError.ValidationError(
            message = "Invalid cycle length",
            field = "averageCycleLength",
            value = 50,
            cause = IllegalArgumentException("Value out of range")
        )
        
        assertEquals("Invalid cycle length", error.message)
        assertEquals("averageCycleLength", error.field)
        assertEquals(50, error.value)
        assertNotNull(error.cause)
        assertTrue(error.cause is IllegalArgumentException)
    }
    
    @Test
    fun `SyncError should contain operation and conflict data`() {
        val conflictData = mapOf("local" to "value1", "remote" to "value2")
        val error = SettingsError.SyncError(
            message = "Sync conflict detected",
            operation = "updateSettings",
            conflictData = conflictData
        )
        
        assertEquals("Sync conflict detected", error.message)
        assertEquals("updateSettings", error.operation)
        assertEquals(conflictData, error.conflictData)
    }
    
    @Test
    fun `NotificationError should contain notification type`() {
        val error = SettingsError.NotificationError(
            message = "Permission denied",
            notificationType = "DAILY_LOGGING",
            cause = SecurityException("Notification permission required")
        )
        
        assertEquals("Permission denied", error.message)
        assertEquals("DAILY_LOGGING", error.notificationType)
        assertTrue(error.cause is SecurityException)
    }
    
    @Test
    fun `ExportError should contain export type`() {
        val error = SettingsError.ExportError(
            message = "Failed to export data",
            exportType = "JSON",
            cause = java.io.IOException("File write error")
        )
        
        assertEquals("Failed to export data", error.message)
        assertEquals("JSON", error.exportType)
        assertTrue(error.cause is java.io.IOException)
    }
    
    @Test
    fun `PersistenceError should contain operation information`() {
        val error = SettingsError.PersistenceError(
            message = "Database write failed",
            operation = "saveSettings"
        )
        
        assertEquals("Database write failed", error.message)
        assertEquals("saveSettings", error.operation)
    }
    
    @Test
    fun `ConflictResolutionError should contain version information`() {
        val error = SettingsError.ConflictResolutionError(
            message = "Cannot resolve conflict",
            localVersion = 1234567890L,
            remoteVersion = 1234567891L
        )
        
        assertEquals("Cannot resolve conflict", error.message)
        assertEquals(1234567890L, error.localVersion)
        assertEquals(1234567891L, error.remoteVersion)
    }
    
    @Test
    fun `BackupError should contain backup type`() {
        val error = SettingsError.BackupError(
            message = "Backup creation failed",
            backupType = "AUTOMATIC"
        )
        
        assertEquals("Backup creation failed", error.message)
        assertEquals("AUTOMATIC", error.backupType)
    }
    
    @Test
    fun `MigrationError should contain version information`() {
        val error = SettingsError.MigrationError(
            message = "Migration failed",
            fromVersion = 1,
            toVersion = 2
        )
        
        assertEquals("Migration failed", error.message)
        assertEquals(1, error.fromVersion)
        assertEquals(2, error.toVersion)
    }
    
    @Test
    fun `SettingsError should extend AppError`() {
        val error = SettingsError.ValidationError("Test error")
        assertTrue(error is AppError)
    }
    
    @Test
    fun `SettingsError should be throwable`() {
        val error = SettingsError.SyncError("Test sync error")
        assertTrue(error is Throwable)
        assertTrue(error is Exception)
    }
}