package com.eunio.healthapp.domain.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock

/**
 * Service for showing non-blocking user messages and notifications.
 * Provides a way to communicate service errors and status to users without crashing the app.
 */
object UserNotificationService {
    
    private var _messages = MutableSharedFlow<UserMessage>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val messages: SharedFlow<UserMessage> get() = _messages.asSharedFlow()
    
    /**
     * Reset the service for testing purposes
     */
    fun resetForTesting() {
        _messages = MutableSharedFlow<UserMessage>(
            replay = 1,
            extraBufferCapacity = 1
        )
    }
    
    /**
     * Show an error message to the user.
     */
    suspend fun showError(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ) {
        _messages.emit(
            UserMessage(
                text = message,
                type = MessageType.ERROR,
                actionLabel = actionLabel,
                action = action
            )
        )
    }
    
    /**
     * Show a warning message to the user.
     */
    suspend fun showWarning(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ) {
        _messages.emit(
            UserMessage(
                text = message,
                type = MessageType.WARNING,
                actionLabel = actionLabel,
                action = action
            )
        )
    }
    
    /**
     * Show an info message to the user.
     */
    suspend fun showInfo(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ) {
        _messages.emit(
            UserMessage(
                text = message,
                type = MessageType.INFO,
                actionLabel = actionLabel,
                action = action
            )
        )
    }
    
    /**
     * Show a success message to the user.
     */
    suspend fun showSuccess(
        message: String,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ) {
        _messages.emit(
            UserMessage(
                text = message,
                type = MessageType.SUCCESS,
                actionLabel = actionLabel,
                action = action
            )
        )
    }
    
    /**
     * Show a service degraded message to the user.
     */
    suspend fun showServiceDegraded(
        serviceName: String,
        availableFeatures: List<String>,
        unavailableFeatures: List<String>
    ) {
        val message = buildString {
            append("$serviceName is running with limited functionality. ")
            if (availableFeatures.isNotEmpty()) {
                append("Available: ${availableFeatures.joinToString(", ")}. ")
            }
            if (unavailableFeatures.isNotEmpty()) {
                append("Temporarily unavailable: ${unavailableFeatures.joinToString(", ")}.")
            }
        }
        
        showWarning(message)
    }
    
    /**
     * Show a service unavailable message to the user.
     */
    suspend fun showServiceUnavailable(
        serviceName: String,
        fallbackMessage: String? = null
    ) {
        val message = buildString {
            append("$serviceName is temporarily unavailable. ")
            fallbackMessage?.let { append(it) }
        }
        
        showError(message)
    }
}

/**
 * Represents a user message with type and optional action.
 */
data class UserMessage(
    val text: String,
    val type: MessageType,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

/**
 * Types of user messages for different styling and behavior.
 */
enum class MessageType {
    ERROR,
    WARNING,
    INFO,
    SUCCESS
}