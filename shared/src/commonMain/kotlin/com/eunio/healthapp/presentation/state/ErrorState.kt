package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.SettingsError

/**
 * Represents different types of error states in the UI with user-friendly messages
 * and recovery actions.
 */
sealed class ErrorState {
    abstract val message: String
    abstract val isRetryable: Boolean
    abstract val userFriendlyMessage: String
    
    /**
     * Network connectivity errors
     */
    data class NetworkError(
        override val message: String,
        val canRetry: Boolean = true
    ) : ErrorState() {
        override val isRetryable: Boolean = canRetry
        override val userFriendlyMessage: String = 
            "Connection issue. Please check your internet connection and try again."
    }
    
    /**
     * Settings validation errors
     */
    data class ValidationError(
        override val message: String,
        val field: String? = null
    ) : ErrorState() {
        override val isRetryable: Boolean = false
        override val userFriendlyMessage: String = 
            field?.let { "Please check your $it and try again." } 
                ?: "Please check your input and try again."
    }
    
    /**
     * Settings synchronization errors
     */
    data class SyncError(
        override val message: String,
        val canRetrySync: Boolean = true
    ) : ErrorState() {
        override val isRetryable: Boolean = canRetrySync
        override val userFriendlyMessage: String = 
            "Sync failed. Your changes are saved locally and will sync when connection is restored."
    }
    
    /**
     * Notification permission or scheduling errors
     */
    data class NotificationError(
        override val message: String,
        val requiresPermission: Boolean = false
    ) : ErrorState() {
        override val isRetryable: Boolean = requiresPermission
        override val userFriendlyMessage: String = 
            if (requiresPermission) "Notification permission is required. Please enable it in settings."
            else "Unable to schedule notifications. Please try again."
    }
    
    /**
     * Data export/import errors
     */
    data class ExportError(
        override val message: String,
        val exportType: String? = null
    ) : ErrorState() {
        override val isRetryable: Boolean = true
        override val userFriendlyMessage: String = 
            "Failed to export your data. Please try again or contact support if the issue persists."
    }
    
    /**
     * Settings backup/restore errors
     */
    data class BackupError(
        override val message: String,
        val isRestoreError: Boolean = false
    ) : ErrorState() {
        override val isRetryable: Boolean = true
        override val userFriendlyMessage: String = 
            if (isRestoreError) "Failed to restore your settings. Please try again or use manual import."
            else "Failed to backup your settings. Your data is still safe locally."
    }
    
    /**
     * Generic errors with fallback handling
     */
    data class GenericError(
        override val message: String,
        val canRetry: Boolean = true
    ) : ErrorState() {
        override val isRetryable: Boolean = canRetry
        override val userFriendlyMessage: String = 
            "Something went wrong. Please try again or contact support if the issue persists."
    }
    
    companion object {
        /**
         * Converts domain errors to UI error states with appropriate user messaging
         */
        fun fromAppError(error: AppError): ErrorState {
            return when (error) {
                is AppError.NetworkError -> NetworkError(error.message)
                is AppError.ValidationError -> ValidationError(error.message, error.field)
                is AppError.DatabaseError -> SyncError(error.message)
                is AppError.DataSyncError -> SyncError(error.message)
                is AppError.PermissionError -> NotificationError(
                    error.message, 
                    requiresPermission = true
                )
                is SettingsError.ValidationError -> ValidationError(error.message, error.field)
                is SettingsError.SyncError -> SyncError(error.message)
                is SettingsError.NotificationError -> NotificationError(
                    error.message,
                    requiresPermission = error.message.contains("permission", ignoreCase = true) ||
                                       error.message.contains("denied", ignoreCase = true)
                )
                is SettingsError.ExportError -> ExportError(error.message, error.exportType)
                is SettingsError.BackupError -> BackupError(
                    error.message, 
                    isRestoreError = error.backupType?.contains("RESTORE") == true
                )
                else -> GenericError(error.message ?: "An unexpected error occurred")
            }
        }
    }
}

/**
 * Represents the current error handling state of a screen or component
 */
data class ErrorHandlingState(
    val currentError: ErrorState? = null,
    val isShowingError: Boolean = false,
    val errorHistory: List<ErrorState> = emptyList(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3
) {
    val canRetry: Boolean = currentError?.isRetryable == true && retryCount < maxRetries
    val shouldShowRetryButton: Boolean = canRetry
    val hasReachedMaxRetries: Boolean = retryCount >= maxRetries
}