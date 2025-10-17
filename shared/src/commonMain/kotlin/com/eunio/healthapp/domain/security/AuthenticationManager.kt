package com.eunio.healthapp.domain.security

import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Manages authentication state and security validation
 */
interface AuthenticationManager {
    
    /**
     * Gets the current authenticated user
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * Observes authentication state changes
     */
    fun observeAuthState(): Flow<AuthState>
    
    /**
     * Validates if the current user is authenticated
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Validates if the current user can access specific user data
     */
    suspend fun canAccessUserData(userId: String): Boolean
    
    /**
     * Validates session token expiry
     */
    suspend fun validateSession(): Result<Boolean>
    
    /**
     * Forces re-authentication for sensitive operations
     */
    suspend fun requireRecentAuthentication(): Result<Boolean>
    
    /**
     * Signs out the current user securely
     */
    suspend fun signOut(): Result<Unit>
}

/**
 * Authentication state representation
 */
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Security validation utilities
 */
object SecurityValidator {
    
    /**
     * Validates that a user ID matches the authenticated user
     */
    fun validateUserAccess(authenticatedUserId: String?, requestedUserId: String): Boolean {
        return authenticatedUserId != null && authenticatedUserId == requestedUserId
    }
    
    /**
     * Validates data access permissions
     */
    fun validateDataAccess(userId: String, dataOwnerId: String): Boolean {
        return userId == dataOwnerId
    }
    
    /**
     * Checks if an operation requires elevated permissions
     */
    fun requiresElevatedPermissions(operation: SecurityOperation): Boolean {
        return when (operation) {
            SecurityOperation.DELETE_USER_DATA -> true
            SecurityOperation.EXPORT_HEALTH_DATA -> true
            SecurityOperation.CHANGE_EMAIL -> true
            SecurityOperation.DELETE_ACCOUNT -> true
            else -> false
        }
    }
}

/**
 * Security operations that may require special handling
 */
enum class SecurityOperation {
    READ_USER_DATA,
    WRITE_USER_DATA,
    DELETE_USER_DATA,
    EXPORT_HEALTH_DATA,
    CHANGE_EMAIL,
    DELETE_ACCOUNT,
    GENERATE_REPORT
}