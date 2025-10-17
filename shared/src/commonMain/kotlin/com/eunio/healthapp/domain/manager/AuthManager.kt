package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result

/**
 * Manages core authentication operations including sign-in, sign-up, and password management.
 * This interface focuses on authentication actions, while AuthenticationManager handles state and security validation.
 */
interface AuthManager {
    
    /**
     * Signs in a user with email and password
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated User or an error
     */
    suspend fun signIn(email: String, password: String): Result<User>
    
    /**
     * Signs up a new user with email and password
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @return Result containing the newly created User or an error
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    
    /**
     * Signs out the current user
     * @return Result indicating success or failure
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Gets the currently authenticated user
     * @return Result containing the current User or null if not authenticated
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * Sends a password reset email to the specified address
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Checks if a user is currently authenticated
     * @return true if authenticated, false otherwise
     */
    suspend fun isAuthenticated(): Boolean
}