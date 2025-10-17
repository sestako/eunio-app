package com.eunio.healthapp.presentation.util

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.presentation.state.ErrorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Centralized error handler for the presentation layer.
 * Provides consistent error handling, user feedback, and recovery mechanisms.
 */
class PresentationErrorHandler {
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    private val _successEvents = MutableSharedFlow<String>()
    val successEvents: SharedFlow<String> = _successEvents.asSharedFlow()
    
    /**
     * Handles an error and converts it to appropriate UI state and user feedback
     */
    suspend fun handleError(
        error: Throwable,
        context: String = "",
        showToUser: Boolean = true
    ): ErrorState {
        val errorState = when (error) {
            is AppError -> ErrorState.fromAppError(error)
            is Exception -> ErrorState.GenericError(
                error.message ?: "An unexpected error occurred",
                canRetry = true
            )
            else -> ErrorState.GenericError("An unexpected error occurred")
        }
        
        if (showToUser) {
            _errorEvents.emit(
                ErrorEvent(
                    errorState = errorState,
                    context = context,
                    timestamp = kotlinx.datetime.Clock.System.now()
                )
            )
        }
        
        return errorState
    }
    
    /**
     * Handles successful operations with user feedback
     */
    suspend fun handleSuccess(message: String) {
        _successEvents.emit(message)
    }
    
    /**
     * Determines if an error should trigger a retry mechanism
     */
    fun shouldRetry(error: ErrorState, retryCount: Int, maxRetries: Int = 3): Boolean {
        return error.isRetryable && retryCount < maxRetries
    }
    
    /**
     * Gets retry delay based on retry count (exponential backoff)
     */
    fun getRetryDelay(retryCount: Int): Long {
        return minOf(1000L * (1L shl retryCount), 30000L) // Max 30 seconds
    }
    
    /**
     * Determines if an error should be logged for debugging
     */
    fun shouldLog(error: ErrorState): Boolean {
        return when (error) {
            is ErrorState.ValidationError -> false // User input errors don't need logging
            is ErrorState.NetworkError -> false // Network errors are expected
            else -> true
        }
    }
    
    /**
     * Gets appropriate user action suggestions based on error type
     */
    fun getUserActionSuggestions(error: ErrorState): List<String> {
        return when (error) {
            is ErrorState.NetworkError -> listOf(
                "Check your internet connection",
                "Try again in a few moments",
                "Switch to WiFi if using mobile data"
            )
            is ErrorState.ValidationError -> listOf(
                "Review your input",
                "Check required fields",
                "Ensure values are within valid ranges"
            )
            is ErrorState.SyncError -> listOf(
                "Your changes are saved locally",
                "Sync will resume when connection is restored",
                "Try manual sync later"
            )
            is ErrorState.NotificationError -> if (error.requiresPermission) {
                listOf(
                    "Enable notification permissions in device settings",
                    "Restart the app after enabling permissions"
                )
            } else {
                listOf("Try again", "Check notification settings")
            }
            is ErrorState.ExportError -> listOf(
                "Ensure you have enough storage space",
                "Try exporting to a different location",
                "Contact support if issue persists"
            )
            is ErrorState.BackupError -> if (error.isRestoreError) {
                listOf(
                    "Try importing settings manually",
                    "Check if backup file is valid",
                    "Contact support for assistance"
                )
            } else {
                listOf(
                    "Your data is safe locally",
                    "Try backup again later",
                    "Check cloud storage permissions"
                )
            }
            is ErrorState.GenericError -> listOf(
                "Try again",
                "Restart the app if issue persists",
                "Contact support if problem continues"
            )
        }
    }
}

/**
 * Represents an error event that occurred in the UI
 */
data class ErrorEvent(
    val errorState: ErrorState,
    val context: String,
    val timestamp: kotlinx.datetime.Instant
)

/**
 * Extension functions for easier error handling in ViewModels
 */
suspend fun PresentationErrorHandler.handleSettingsError(
    error: Throwable,
    operation: String
): ErrorState {
    return handleError(error, "Settings $operation", showToUser = true)
}

suspend fun PresentationErrorHandler.handleSyncError(
    error: Throwable
): ErrorState {
    return handleError(error, "Data synchronization", showToUser = true)
}

suspend fun PresentationErrorHandler.handleNotificationError(
    error: Throwable
): ErrorState {
    return handleError(error, "Notification management", showToUser = true)
}