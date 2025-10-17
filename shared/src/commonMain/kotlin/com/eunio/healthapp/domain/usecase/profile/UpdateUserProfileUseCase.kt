package com.eunio.healthapp.domain.usecase.profile

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Use case for updating user profile information.
 * Handles validation and persistence of profile changes.
 */
class UpdateUserProfileUseCase(
    private val userRepository: UserRepository
) {
    
    /**
     * Updates user profile with new information
     * @param userId The ID of the user to update
     * @param name New display name (optional)
     * @param email New email address (optional)
     * @return Result containing the updated user or error
     */
    suspend operator fun invoke(
        userId: String,
        name: String? = null,
        email: String? = null
    ): Result<User> {
        return try {
            // Get current user
            val currentUserResult = userRepository.getCurrentUser()
            if (currentUserResult is Result.Error) {
                return currentUserResult
            }
            
            val currentUser = currentUserResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("User not found"))
            
            // Validate inputs
            val validationResult = validateProfileUpdate(name, email)
            if (validationResult is Result.Error) {
                return validationResult
            }
            
            // Create updated user
            val updatedUser = currentUser.copy(
                name = name ?: currentUser.name,
                email = email ?: currentUser.email
            )
            
            // Update user
            val updateResult = userRepository.updateUser(updatedUser)
            if (updateResult is Result.Error) {
                return updateResult
            }
            
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to update user profile: ${e.message}", cause = e))
        }
    }
    
    /**
     * Validates profile update inputs
     */
    private fun validateProfileUpdate(name: String?, email: String?): Result<Unit> {
        // Validate name if provided
        name?.let { n ->
            if (n.isBlank()) {
                return Result.error(AppError.ValidationError("Name cannot be empty", "name"))
            }
            if (n.length < 2) {
                return Result.error(AppError.ValidationError("Name must be at least 2 characters", "name"))
            }
            if (n.length > 50) {
                return Result.error(AppError.ValidationError("Name cannot exceed 50 characters", "name"))
            }
        }
        
        // Validate email if provided
        email?.let { e ->
            if (e.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            if (!isValidEmail(e)) {
                return Result.error(AppError.ValidationError("Please enter a valid email address", "email"))
            }
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Simple email validation
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}