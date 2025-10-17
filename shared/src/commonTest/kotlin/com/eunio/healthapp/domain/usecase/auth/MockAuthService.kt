package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock implementation of AuthService for testing purposes
 */
class MockAuthService : AuthService {
    
    var signUpResult: Result<User>? = null
    var signInResult: Result<User>? = null
    var signOutResult: Result<Unit> = Result.success(Unit)
    var getCurrentUserResult: Result<User?> = Result.success(null)
    var sendPasswordResetResult: Result<Unit> = Result.success(Unit)
    var isAuthenticatedValue: Boolean = false
    var authStateValue: User? = null
    
    // Track method calls for verification
    var lastSignUpEmail: String? = null
    var lastSignUpPassword: String? = null
    var lastSignUpName: String? = null
    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastPasswordResetEmail: String? = null
    
    override val authState: Flow<User?> = flowOf(authStateValue)
    
    override suspend fun getCurrentUser(): Result<User?> {
        return getCurrentUserResult
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        lastSignUpEmail = email
        lastSignUpPassword = password
        lastSignUpName = name
        return signUpResult ?: Result.error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("Mock not configured"))
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        lastSignInEmail = email
        lastSignInPassword = password
        return signInResult ?: Result.error(com.eunio.healthapp.domain.error.AppError.AuthenticationError("Mock not configured"))
    }
    
    override suspend fun signOut(): Result<Unit> {
        return signOutResult
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        lastPasswordResetEmail = email
        return sendPasswordResetResult
    }
    
    override fun isAuthenticated(): Boolean {
        return isAuthenticatedValue
    }
}