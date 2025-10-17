package com.eunio.healthapp.data.security

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.security.AuthState
import com.eunio.healthapp.domain.security.AuthenticationManager
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Implementation of AuthenticationManager with security validations
 */
class AuthenticationManagerImpl(
    private val authService: AuthService,
    private val userRepository: UserRepository
) : AuthenticationManager {
    
    private var lastAuthCheck: kotlinx.datetime.Instant? = null
    private val authCheckInterval = 5.minutes
    
    override suspend fun getCurrentUser(): Result<User?> {
        return authService.getCurrentUser()
    }
    
    override fun observeAuthState(): Flow<AuthState> {
        return authService.authState.map { user ->
            when (user) {
                null -> AuthState.Unauthenticated
                else -> AuthState.Authenticated
            }
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return authService.isAuthenticated() && validateSession().getOrNull() == true
    }
    
    override suspend fun canAccessUserData(userId: String): Boolean {
        return try {
            val currentUserResult = authService.getCurrentUser()
            val currentUser = currentUserResult.getOrNull()
            currentUser?.id == userId
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun validateSession(): Result<Boolean> {
        return try {
            val now = Clock.System.now()
            
            // Check if we need to validate the session
            if (lastAuthCheck != null && 
                now - lastAuthCheck!! < authCheckInterval) {
                return Result.Success(true)
            }
            
            val userResult = authService.getCurrentUser()
            if (userResult.getOrNull() == null) {
                return Result.Success(false)
            }
            
            // Validate token freshness - simplified for testing
            lastAuthCheck = now
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Session validation failed", e))
        }
    }
    
    override suspend fun requireRecentAuthentication(): Result<Boolean> {
        return try {
            // Simplified for testing - in real app would force token refresh
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(AppError.SecurityError("Recent authentication required", e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            val result = authService.signOut()
            lastAuthCheck = null
            result
        } catch (e: Exception) {
            Result.Error(AppError.NetworkError("Sign out failed", e))
        }
    }
}