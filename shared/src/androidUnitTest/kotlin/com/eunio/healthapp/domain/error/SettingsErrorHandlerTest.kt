package com.eunio.healthapp.domain.error

import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsErrorHandlerTest {
    
    private val errorHandler = SettingsErrorHandler()
    
    @Test
    fun `handleSettingsError should return existing SettingsError unchanged`() {
        val originalError = SettingsError.ValidationError("Original error")
        val result = errorHandler.handleSettingsError(originalError)
        
        assertEquals(originalError, result)
    }
    
    @Test
    fun `handleSettingsError should map IllegalArgumentException to ValidationError`() {
        val exception = IllegalArgumentException("Invalid argument")
        val result = errorHandler.handleSettingsError(exception)
        
        assertTrue(result is SettingsError.ValidationError)
        assertEquals("Invalid argument", result.message)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleSettingsError should map SecurityException to NotificationError`() {
        val exception = SecurityException("Permission denied")
        val result = errorHandler.handleSettingsError(exception)
        
        assertTrue(result is SettingsError.NotificationError)
        assertTrue(result.message.contains("Notification permission denied"))
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleSettingsError should map SerializationException to PersistenceError`() {
        val exception = SerializationException("Serialization failed")
        val result = errorHandler.handleSettingsError(exception, "saveSettings")
        
        assertTrue(result is SettingsError.PersistenceError)
        assertTrue(result.message.contains("serialize"))
        assertEquals("saveSettings", (result as SettingsError.PersistenceError).operation)
        assertEquals(exception, result.cause)
    }
    

    
    @Test
    fun `handleSettingsError should map unknown exceptions to SyncError`() {
        val exception = RuntimeException("Unknown error")
        val result = errorHandler.handleSettingsError(exception, "unknownOperation")
        
        assertTrue(result is SettingsError.SyncError)
        assertEquals("Unknown error", result.message)
        assertEquals("unknownOperation", result.operation)
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleValidationError should create ValidationError with all parameters`() {
        val result = errorHandler.handleValidationError(
            message = "Invalid value",
            field = "testField",
            value = "testValue",
            cause = IllegalArgumentException("Test cause")
        )
        
        assertEquals("Invalid value", result.message)
        assertEquals("testField", result.field)
        assertEquals("testValue", result.value)
        assertTrue(result.cause is IllegalArgumentException)
    }
    
    @Test
    fun `handleSyncConflict should create ConflictResolutionError with version info`() {
        val result = errorHandler.handleSyncConflict(
            message = "Conflict detected",
            localVersion = 123L,
            remoteVersion = 456L,
            conflictData = "test data"
        )
        
        assertEquals("Conflict detected", result.message)
        assertEquals(123L, result.localVersion)
        assertEquals(456L, result.remoteVersion)
    }
    
    @Test
    fun `handleNotificationError should create NotificationError with type`() {
        val result = errorHandler.handleNotificationError(
            message = "Permission error",
            notificationType = "DAILY_REMINDER",
            cause = SecurityException("Test")
        )
        
        assertEquals("Permission error", result.message)
        assertEquals("DAILY_REMINDER", result.notificationType)
        assertTrue(result.cause is SecurityException)
    }
    
    @Test
    fun `handleBackupError should create BackupError with type`() {
        val result = errorHandler.handleBackupError(
            message = "Backup failed",
            backupType = "MANUAL",
            cause = RuntimeException("Test")
        )
        
        assertEquals("Backup failed", result.message)
        assertEquals("MANUAL", result.backupType)
        assertTrue(result.cause is RuntimeException)
    }
    
    @Test
    fun `handleMigrationError should create MigrationError with version info`() {
        val result = errorHandler.handleMigrationError(
            message = "Migration failed",
            fromVersion = 1,
            toVersion = 2,
            cause = RuntimeException("Test")
        )
        
        assertEquals("Migration failed", result.message)
        assertEquals(1, result.fromVersion)
        assertEquals(2, result.toVersion)
        assertTrue(result.cause is RuntimeException)
    }
    
    @Test
    fun `getUserFriendlyMessage should return appropriate messages for each error type`() {
        val validationError = SettingsError.ValidationError("Test", field = "testField")
        val syncError = SettingsError.SyncError("Test")
        val notificationError = SettingsError.NotificationError("Test")
        val exportError = SettingsError.ExportError("Test")
        val persistenceError = SettingsError.PersistenceError("Test")
        val conflictError = SettingsError.ConflictResolutionError("Test")
        val backupError = SettingsError.BackupError("Test")
        val migrationError = SettingsError.MigrationError("Test")
        
        assertTrue(errorHandler.getUserFriendlyMessage(validationError).contains("Invalid setting value"))
        assertTrue(errorHandler.getUserFriendlyMessage(syncError).contains("sync your settings"))
        assertTrue(errorHandler.getUserFriendlyMessage(notificationError).contains("Notification settings"))
        assertTrue(errorHandler.getUserFriendlyMessage(exportError).contains("export your settings"))
        assertTrue(errorHandler.getUserFriendlyMessage(persistenceError).contains("save your settings"))
        assertTrue(errorHandler.getUserFriendlyMessage(conflictError).contains("conflicting changes"))
        assertTrue(errorHandler.getUserFriendlyMessage(backupError).contains("backup your settings"))
        assertTrue(errorHandler.getUserFriendlyMessage(migrationError).contains("update your settings"))
    }
    
    @Test
    fun `isRecoverable should return correct values for each error type`() {
        assertTrue(errorHandler.isRecoverable(SettingsError.ValidationError("Test")))
        assertTrue(errorHandler.isRecoverable(SettingsError.SyncError("Test")))
        assertFalse(errorHandler.isRecoverable(SettingsError.NotificationError("Test")))
        assertTrue(errorHandler.isRecoverable(SettingsError.ExportError("Test")))
        assertTrue(errorHandler.isRecoverable(SettingsError.PersistenceError("Test")))
        assertFalse(errorHandler.isRecoverable(SettingsError.ConflictResolutionError("Test")))
        assertTrue(errorHandler.isRecoverable(SettingsError.BackupError("Test")))
        assertFalse(errorHandler.isRecoverable(SettingsError.MigrationError("Test")))
    }
    
    @Test
    fun `requiresUserAttention should return correct values for each error type`() {
        assertTrue(errorHandler.requiresUserAttention(SettingsError.ValidationError("Test")))
        assertFalse(errorHandler.requiresUserAttention(SettingsError.SyncError("Test")))
        assertTrue(errorHandler.requiresUserAttention(SettingsError.NotificationError("Test")))
        assertTrue(errorHandler.requiresUserAttention(SettingsError.ExportError("Test")))
        assertFalse(errorHandler.requiresUserAttention(SettingsError.PersistenceError("Test")))
        assertTrue(errorHandler.requiresUserAttention(SettingsError.ConflictResolutionError("Test")))
        assertFalse(errorHandler.requiresUserAttention(SettingsError.BackupError("Test")))
        assertTrue(errorHandler.requiresUserAttention(SettingsError.MigrationError("Test")))
    }
}