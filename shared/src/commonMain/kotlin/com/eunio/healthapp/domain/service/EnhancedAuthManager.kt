package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UserNotificationService
import com.eunio.healthapp.domain.error.AppError
import kotlinx.datetime.Clock

/**
 * Enhanced AuthManager implementation with comprehensive error handling,
 * graceful degradation, and fallback mechanisms.
 */
class EnhancedAuthManager(
    private val primaryAuthManager: AuthManager,
    private val fallbackAuthManager: AuthManager? = null
) : BaseService(), AuthManager {
    
    override val serviceName: String = "AuthManager"
    
    private var lastKnownUser: User? = null
    private var isOfflineMode = false
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return executeOperationWithRetry(
            operation = "signIn",
            maxRetries = 2, // Fewer retries for auth operations
            fallback = {
                fallbackAuthManager?.signIn(email, password)?.getOrThrow()
                    ?: throw AppError.AuthenticationError("Authentication service temporarily unavailable")
            }
        ) {
            val result = primaryAuthManager.signIn(email, password)
            when (result) {
                is Result.Success -> {
                    lastKnownUser = result.data
                    isOfflineMode = false
                    result.data
                }
                is Result.Error -> throw result.error
            }
        }
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return executeOperationWithRetry(
            operation = "signUp",
            maxRetries = 2, // Fewer retries for auth operations
            fallback = {
                fallbackAuthManager?.signUp(email, password, name)?.getOrThrow()
                    ?: throw AppError.AuthenticationError("Account creation service temporarily unavailable")
            }
        ) {
            val result = primaryAuthManager.signUp(email, password, name)
            when (result) {
                is Result.Success -> {
                    lastKnownUser = result.data
                    isOfflineMode = false
                    result.data
                }
                is Result.Error -> throw result.error
            }
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return executeOperation(
            operation = "signOut",
            fallback = {
                // Always allow local sign out
                lastKnownUser = null
                isOfflineMode = false
                UserNotificationService.showInfo("Signed out locally. Remote sign out will complete when connection is restored.")
                Unit
            }
        ) {
            val result = primaryAuthManager.signOut()
            when (result) {
                is Result.Success -> {
                    lastKnownUser = null
                    isOfflineMode = false
                    result.data
                }
                is Result.Error -> throw result.error
            }
        }
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        return executeOperation(
            operation = "getCurrentUser",
            showUserMessage = false, // Don't show errors for user checks
            fallback = {
                // Return cached user in offline mode
                if (isOfflineMode && lastKnownUser != null) {
                    lastKnownUser
                } else {
                    fallbackAuthManager?.getCurrentUser()?.getOrThrow()
                }
            }
        ) {
            val result = primaryAuthManager.getCurrentUser()
            when (result) {
                is Result.Success -> {
                    lastKnownUser = result.data
                    result.data
                }
                is Result.Error -> throw result.error
            }
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return executeOperationWithRetry(
            operation = "resetPassword",
            maxRetries = 2,
            fallback = {
                fallbackAuthManager?.resetPassword(email)?.getOrThrow()
                    ?: throw AppError.AuthenticationError("Password reset service temporarily unavailable")
            }
        ) {
            val result = primaryAuthManager.resetPassword(email)
            when (result) {
                is Result.Success -> result.data
                is Result.Error -> throw result.error
            }
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return try {
            val currentUser = getCurrentUser()
            when (currentUser) {
                is Result.Success -> currentUser.data != null
                is Result.Error -> {
                    // If we can't check auth status, use cached info
                    lastKnownUser != null && isOfflineMode
                }
            }
        } catch (e: Exception) {
            // If we can't check auth status, assume not authenticated for safety
            false
        }
    }
    
    override suspend fun isHealthy(): Boolean {
        return try {
            // Test primary manager directly without fallbacks for health check
            val result = primaryAuthManager.getCurrentUser()
            result is Result.Success
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getServiceStatus(): ServiceStatus {
        val isHealthy = isHealthy()
        val details = mutableMapOf<String, Any>()
        
        details["isOfflineMode"] = isOfflineMode
        details["hasLastKnownUser"] = lastKnownUser != null
        details["fallbackManagerAvailable"] = fallbackAuthManager != null
        
        try {
            val isAuth = primaryAuthManager.isAuthenticated()
            details["isAuthenticated"] = isAuth
        } catch (e: Exception) {
            details["authCheckError"] = e.message ?: "Unknown error"
        }
        
        return ServiceStatus(
            serviceName = serviceName,
            isHealthy = isHealthy,
            lastChecked = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            details = details
        )
    }
    
    override suspend fun handleUserNotification(operation: String, error: Throwable) {
        // Handle network errors specially for auth
        if (error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true) {
            
            isOfflineMode = true
            
            when (operation) {
                "signIn", "signUp" -> {
                    UserNotificationService.showError(
                        "Unable to connect to authentication service. Please check your internet connection.",
                        actionLabel = "Retry"
                    )
                }
                "getCurrentUser" -> {
                    // Don't show error for user checks in offline mode
                    return
                }
                else -> {
                    UserNotificationService.showWarning(
                        "Authentication service is temporarily unavailable. Some features may be limited."
                    )
                }
            }
        } else {
            super.handleUserNotification(operation, error)
        }
    }
    
    override fun getUserFriendlyMessage(operation: String, error: Throwable): String {
        return when (operation) {
            "signIn" -> when {
                error.message?.contains("invalid", ignoreCase = true) == true -> 
                    "Invalid email or password. Please check your credentials and try again."
                error.message?.contains("network", ignoreCase = true) == true -> 
                    "Unable to sign in. Please check your internet connection."
                error.message?.contains("blocked", ignoreCase = true) == true -> 
                    "Your account has been temporarily blocked. Please try again later."
                else -> "Unable to sign in. Please try again."
            }
            "signUp" -> when {
                error.message?.contains("exists", ignoreCase = true) == true -> 
                    "An account with this email already exists. Please sign in instead."
                error.message?.contains("weak", ignoreCase = true) == true -> 
                    "Password is too weak. Please choose a stronger password."
                error.message?.contains("invalid", ignoreCase = true) == true -> 
                    "Please check your email and password and try again."
                else -> "Unable to create account. Please try again."
            }
            "resetPassword" -> when {
                error.message?.contains("not found", ignoreCase = true) == true -> 
                    "No account found with this email address."
                error.message?.contains("too many", ignoreCase = true) == true -> 
                    "Too many password reset attempts. Please try again later."
                else -> "Unable to send password reset email. Please try again."
            }
            "signOut" -> "Unable to sign out completely. You have been signed out locally."
            else -> super.getUserFriendlyMessage(operation, error)
        }
    }
}