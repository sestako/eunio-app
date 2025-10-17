package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.presentation.state.ErrorHandlingState
import com.eunio.healthapp.presentation.state.ErrorState
import com.eunio.healthapp.presentation.util.PresentationErrorHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel that provides comprehensive error handling capabilities.
 * All settings ViewModels should extend this to ensure consistent error handling.
 */
abstract class BaseErrorHandlingViewModel<T : UiState> : BaseViewModel<T>() {
    
    protected val errorHandler = PresentationErrorHandler()
    
    private val _errorHandlingState = MutableStateFlow(ErrorHandlingState())
    val errorHandlingState: StateFlow<ErrorHandlingState> = _errorHandlingState.asStateFlow()
    
    private val _showErrorDialog = MutableSharedFlow<ErrorState>()
    val showErrorDialog: SharedFlow<ErrorState> = _showErrorDialog.asSharedFlow()
    
    private val _showSuccessMessage = MutableSharedFlow<String>()
    val showSuccessMessage: SharedFlow<String> = _showSuccessMessage.asSharedFlow()
    
    private val _showRetryPrompt = MutableSharedFlow<RetryPrompt>()
    val showRetryPrompt: SharedFlow<RetryPrompt> = _showRetryPrompt.asSharedFlow()
    
    init {
        // Observe error events from the error handler
        viewModelScope.launch {
            errorHandler.errorEvents.collect { errorEvent ->
                handleErrorEvent(errorEvent.errorState)
            }
        }
        
        // Observe success events from the error handler
        viewModelScope.launch {
            errorHandler.successEvents.collect { message ->
                _showSuccessMessage.emit(message)
            }
        }
    }
    
    /**
     * Handles an error with comprehensive error management
     */
    suspend fun handleError(
        error: Throwable,
        context: String = "",
        showDialog: Boolean = false,
        allowRetry: Boolean = true
    ) {
        val errorState = errorHandler.handleError(error, context, showToUser = true)
        
        if (showDialog) {
            _showErrorDialog.emit(errorState)
        }
        
        updateErrorState { currentState ->
            currentState.copy(
                currentError = errorState,
                isShowingError = true,
                errorHistory = currentState.errorHistory + errorState
            )
        }
        
        // Show retry prompt if appropriate
        if (allowRetry && shouldShowRetryPrompt(errorState)) {
            _showRetryPrompt.emit(
                RetryPrompt(
                    errorState = errorState,
                    context = context,
                    retryAction = { retryLastOperation() }
                )
            )
        }
    }
    
    /**
     * Handles successful operations
     */
    protected suspend fun handleSuccess(message: String) {
        errorHandler.handleSuccess(message)
        clearError()
    }
    
    /**
     * Retries the last failed operation
     */
    protected open suspend fun retryLastOperation() {
        val currentState = _errorHandlingState.value
        if (currentState.canRetry) {
            updateErrorState { state ->
                state.copy(
                    retryCount = state.retryCount + 1,
                    isShowingError = false
                )
            }
            
            // Subclasses should override this to implement specific retry logic
            onRetryRequested()
        }
    }
    
    /**
     * Called when a retry is requested. Subclasses should override this.
     */
    protected open suspend fun onRetryRequested() {
        // Default implementation - subclasses should override
    }
    
    /**
     * Clears the current error state
     */
    fun clearError() {
        updateErrorState { state ->
            state.copy(
                currentError = null,
                isShowingError = false,
                retryCount = 0
            )
        }
    }
    
    /**
     * Dismisses the current error without clearing it from history
     */
    fun dismissError() {
        updateErrorState { state ->
            state.copy(isShowingError = false)
        }
    }
    
    /**
     * Gets user-friendly error message for display
     */
    fun getUserFriendlyErrorMessage(): String? {
        return _errorHandlingState.value.currentError?.userFriendlyMessage
    }
    
    /**
     * Gets action suggestions for the current error
     */
    fun getErrorActionSuggestions(): List<String> {
        val currentError = _errorHandlingState.value.currentError
        return currentError?.let { errorHandler.getUserActionSuggestions(it) } ?: emptyList()
    }
    
    /**
     * Checks if the current error allows retry
     */
    fun canRetryCurrentError(): Boolean {
        return _errorHandlingState.value.canRetry
    }
    
    /**
     * Gets the retry delay for the current retry count
     */
    fun getRetryDelay(): Long {
        return errorHandler.getRetryDelay(_errorHandlingState.value.retryCount)
    }
    
    private fun handleErrorEvent(errorState: ErrorState) {
        updateErrorState { currentState ->
            currentState.copy(
                currentError = errorState,
                isShowingError = true,
                errorHistory = currentState.errorHistory + errorState
            )
        }
    }
    
    private fun updateErrorState(update: (ErrorHandlingState) -> ErrorHandlingState) {
        _errorHandlingState.value = update(_errorHandlingState.value)
    }
    
    private fun shouldShowRetryPrompt(errorState: ErrorState): Boolean {
        val currentState = _errorHandlingState.value
        return errorState.isRetryable && 
               currentState.retryCount < currentState.maxRetries &&
               !currentState.hasReachedMaxRetries
    }
}

/**
 * Represents a retry prompt that can be shown to the user
 */
data class RetryPrompt(
    val errorState: ErrorState,
    val context: String,
    val retryAction: suspend () -> Unit
)

/**
 * Extension function to safely execute operations with error handling
 */
suspend fun <T : UiState> BaseErrorHandlingViewModel<T>.safeExecute(
    context: String = "",
    showDialog: Boolean = false,
    operation: suspend () -> Unit
) {
    try {
        operation()
    } catch (e: Exception) {
        handleError(e, context, showDialog)
    }
}