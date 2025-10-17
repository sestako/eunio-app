package com.eunio.healthapp.domain.error

/**
 * Sealed class hierarchy for settings-specific errors.
 * Extends AppError to integrate with the existing error handling system.
 */
sealed class SettingsError : AppError() {
    
    /**
     * Settings validation errors for invalid preference values
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        val value: Any? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings synchronization errors between local and remote storage
     */
    data class SyncError(
        override val message: String,
        val operation: String? = null,
        val conflictData: Any? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Notification-related errors including permission and scheduling issues
     */
    data class NotificationError(
        override val message: String,
        val notificationType: String? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Data export and import errors
     */
    data class ExportError(
        override val message: String,
        val exportType: String? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings persistence errors for local storage operations
     */
    data class PersistenceError(
        override val message: String,
        val operation: String? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings conflict resolution errors during sync operations
     */
    data class ConflictResolutionError(
        override val message: String,
        val localVersion: Long? = null,
        val remoteVersion: Long? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings backup and restore errors
     */
    data class BackupError(
        override val message: String,
        val backupType: String? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings migration errors when upgrading between versions
     */
    data class MigrationError(
        override val message: String,
        val fromVersion: Int? = null,
        val toVersion: Int? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
    
    /**
     * Settings conflict errors during concurrent modifications
     */
    data class ConflictError(
        override val message: String,
        val expectedVersion: Long? = null,
        val actualVersion: Long? = null,
        override val cause: Throwable? = null
    ) : SettingsError()
}