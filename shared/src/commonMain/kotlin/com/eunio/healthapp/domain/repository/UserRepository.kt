package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.util.Result

/**
 * Repository interface for user-related data operations.
 * Handles user authentication, profile management, and onboarding.
 */
interface UserRepository {
    
    /**
     * Retrieves the currently authenticated user.
     * Returns null if no user is authenticated.
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * Updates the user profile information.
     * @param user The updated user data
     */
    suspend fun updateUser(user: User): Result<Unit>
    
    /**
     * Completes the user onboarding process by setting their primary health goal.
     * @param userId The ID of the user completing onboarding
     * @param primaryGoal The selected primary health goal
     */
    suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit>
    
    /**
     * Creates a new user account with the provided information.
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     */
    suspend fun createUser(email: String, password: String, name: String): Result<User>
    
    /**
     * Signs in an existing user with email and password.
     * @param email User's email address
     * @param password User's password
     */
    suspend fun signInUser(email: String, password: String): Result<User>
    
    /**
     * Signs out the current user.
     */
    suspend fun signOutUser(): Result<Unit>
    
    /**
     * Deletes the user account and all associated data.
     * @param userId The ID of the user to delete
     */
    suspend fun deleteUser(userId: String): Result<Unit>
}