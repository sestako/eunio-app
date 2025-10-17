package com.eunio.healthapp.domain.error

import kotlinx.serialization.SerializationException

/**
 * Error handler for settings-specific error scenarios.
 * Provides consistent error mapping and user-friendly error messages.
 */
class SettingsErrorHandler {
    
    /**
     * Maps generic exceptions to specific SettingsError types
     */
    fun handleSettingsError(error: Throwable, operation: String? = null): SettingsError {
        return when (error) {
            is SettingsError -> error
            is SerializationException -> SettingsError.PersistenceError(
                message = "Failed to serialize/deserialize settings: ${error.message}",
                operation = operation,
                cause = error
            )
            is IllegalArgumentException -> SettingsError.ValidationError(
                message = error.message ?: "Invalid settings value",
                cause = error
            )
            is Exception -> {
                if (error::class.simpleName == "SecurityException") {
                    SettingsError.NotificationError(
                        message = "Notification permission denied: ${error.message}",
                        cause = error
                    )
                } else {
                    SettingsError.SyncError(
                        message = error.message ?: "Unknown settings error",
                        operation = operation,
                        cause = error
                    )
                }
            }
            else -> SettingsError.SyncError(
                message = error.message ?: "Unknown settings error occurred",
                operation = operation,
                cause = error
            )
        }
    }
    
    /**
     * Handles validation errors with specific field information
     */
    fun handleValidationError(
        message: String,
        field: String? = null,
        value: Any? = null,
        cause: Throwable? = null
    ): SettingsError.ValidationError {
        return SettingsError.ValidationError(
            message = message,
            field = field,
            value = value,
            cause = cause
        )
    }
    
    /**
     * Handles sync conflict errors with version information
     */
    fun handleSyncConflict(
        message: String,
        localVersion: Long? = null,
        remoteVersion: Long? = null,
        conflictData: Any? = null
    ): SettingsError.ConflictResolutionError {
        return SettingsError.ConflictResolutionError(
            message = message,
            localVersion = localVersion,
            remoteVersion = remoteVersion,
            cause = null
        )
    }
    
    /**
     * Handles notification permission errors
     */
    fun handleNotificationError(
        message: String,
        notificationType: String? = null,
        cause: Throwable? = null
    ): SettingsError.NotificationError {
        return SettingsError.NotificationError(
            message = message,
            notificationType = notificationType,
            cause = cause
        )
    }
    
    /**
     * Handles backup and restore errors
     */
    fun handleBackupError(
        message: String,
        backupType: String? = null,
        cause: Throwable? = null
    ): SettingsError.BackupError {
        return SettingsError.BackupError(
            message = message,
            backupType = backupType,
            cause = cause
        )
    }
    
    /**
     * Handles settings migration errors
     */
    fun handleMigrationError(
        message: String,
        fromVersion: Int? = null,
        toVersion: Int? = null,
        cause: Throwable? = null
    ): SettingsError.MigrationError {
        return SettingsError.MigrationError(
            message = message,
            fromVersion = fromVersion,
            toVersion = toVersion,
            cause = cause
        )
    }
    
    /**
     * Provides user-friendly error messages for different error types
     */
    fun getUserFriendlyMessage(error: SettingsError): String {
        return when (error) {
            is SettingsError.ValidationError -> {
                val fieldInfo = error.field?.let { " for $it" } ?: ""
                "Invalid setting value$fieldInfo. Please check your input and try again."
            }
            is SettingsError.SyncError -> {
                "Unable to sync your settings. Please check your internet connection and try again."
            }
            is SettingsError.NotificationError -> {
                "Notification settings could not be updated. Please check your notification permissions."
            }
            is SettingsError.ExportError -> {
                "Failed to export your settings data. Please try again or contact support."
            }
            is SettingsError.PersistenceError -> {
                "Unable to save your settings. Please try again."
            }
            is SettingsError.ConflictResolutionError -> {
                "Your settings have conflicting changes. Please review and resolve the conflicts."
            }
            is SettingsError.BackupError -> {
                "Failed to backup your settings. Please check your storage permissions and try again."
            }
            is SettingsError.MigrationError -> {
                "Unable to update your settings to the latest version. Please contact support."
            }
            is SettingsError.ConflictError -> {
                "Settings conflict detected. Your changes conflict with recent updates from another device."
            }
        }
    }
    
    /**
     * Determines if an error is recoverable and the user should retry
     */
    fun isRecoverable(error: SettingsError): Boolean {
        return when (error) {
            is SettingsError.ValidationError -> true
            is SettingsError.SyncError -> true
            is SettingsError.NotificationError -> false // Usually requires user action
            is SettingsError.ExportError -> true
            is SettingsError.PersistenceError -> true
            is SettingsError.ConflictResolutionError -> false // Requires user decision
            is SettingsError.BackupError -> true
            is SettingsError.MigrationError -> false // Usually requires app update
            is SettingsError.ConflictError -> false // Requires conflict resolution
        }
    }
    
    /**
     * Determines if an error requires immediate user attention
     */
    fun requiresUserAttention(error: SettingsError): Boolean {
        return when (error) {
            is SettingsError.ValidationError -> true
            is SettingsError.SyncError -> false // Can be handled in background
            is SettingsError.NotificationError -> true
            is SettingsError.ExportError -> true
            is SettingsError.PersistenceError -> false
            is SettingsError.ConflictResolutionError -> true
            is SettingsError.BackupError -> false
            is SettingsError.MigrationError -> true
            is SettingsError.ConflictError -> true
        }
    }
}