package com.eunio.healthapp.data.remote.auth

import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface for authentication operations.
 * Provides methods for user authentication, registration, and session management.
 */
interface AuthService {
    
    /**
     * Observable authentication state that emits the current user or null if not authenticated
     */
    val authState: Flow<User?>
    
    /**
     * Gets the currently authenticated user
     * @return Result containing the current user or null if not authenticated
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * Signs up a new user with email and password
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @return Result containing the created user or error
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    
    /**
     * Signs in an existing user with email and password
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated user or error
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Signs out the current user
     * @return Result indicating success or error
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Sends a password reset email to the specified email address
     * @param email User's email address
     * @return Result indicating success or error
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Checks if a user is currently authenticated
     * @return true if user is authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean
}