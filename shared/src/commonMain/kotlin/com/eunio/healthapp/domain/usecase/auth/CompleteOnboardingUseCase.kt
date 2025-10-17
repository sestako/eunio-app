package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.Validation
import com.eunio.healthapp.domain.util.ValidationResult

/**
 * Use case for completing user onboarding process.
 * Handles primary goal selection, validation of onboarding data, and unit system initialization.
 */
class CompleteOnboardingUseCase(
    private val userRepository: UserRepository,
    private val unitSystemInitializer: com.eunio.healthapp.domain.manager.UnitSystemInitializer
) {
    
    /**
     * Completes the onboarding process for a user by setting their primary health goal.
     * 
     * @param userId The ID of the user completing onboarding
     * @param primaryGoal The selected primary health goal
     * @param name Optional updated name for the user
     * @return Result indicating success or failure of the onboarding completion
     */
    suspend operator fun invoke(
        userId: String,
        primaryGoal: HealthGoal,
        name: String? = null
    ): Result<User> {
        // Validate input parameters
        val validationResult = validateOnboardingData(userId, primaryGoal, name)
        if (validationResult is ValidationResult.Error) {
            return Result.error(AppError.ValidationError("Validation failed: ${validationResult.errors.joinToString(", ")}"))
        }
        
        // Get current user to ensure they exist
        val currentUserResult = userRepository.getCurrentUser()
        if (currentUserResult is Result.Error) {
            return Result.error(AppError.AuthenticationError("Failed to get current user: ${currentUserResult.error.message}"))
        }
        
        val currentUser = (currentUserResult as Result.Success).data
        if (currentUser == null) {
            return Result.error(AppError.AuthenticationError("No authenticated user found"))
        }
        
        if (currentUser.id != userId) {
            return Result.error(AppError.AuthenticationError("User ID mismatch"))
        }
        
        // Check if onboarding is already complete
        if (currentUser.onboardingComplete) {
            return Result.error(AppError.ValidationError("Onboarding already completed for this user"))
        }
        
        // Initialize unit system based on device locale for new user
        val unitSystemResult = unitSystemInitializer.initializeForNewUser()
        if (unitSystemResult is Result.Error) {
            // Log the error but don't fail onboarding - unit system can be set later
            // In a real app, you might want to log this error for monitoring
        }
        
        // Complete onboarding
        return when (val result = userRepository.completeOnboarding(userId, primaryGoal)) {
            is Result.Success -> {
                // Get updated user after onboarding completion
                val updatedUserResult = userRepository.getCurrentUser()
                if (updatedUserResult is Result.Success && updatedUserResult.data != null) {
                    Result.success(updatedUserResult.data)
                } else {
                    Result.error(AppError.DataSyncError("Failed to retrieve updated user after onboarding"))
                }
            }
            is Result.Error -> Result.error(AppError.DataSyncError("Failed to complete onboarding: ${result.error.message}"))
        }
    }
    
    /**
     * Validates onboarding data before processing.
     * 
     * @param userId The user ID to validate
     * @param primaryGoal The selected health goal
     * @param name Optional name to validate
     * @return ValidationResult indicating success or specific validation errors
     */
    private fun validateOnboardingData(
        userId: String,
        primaryGoal: HealthGoal,
        name: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate user ID
        if (userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        // Validate name if provided
        name?.let { userName ->
            if (userName.isBlank()) {
                errors.add("Name cannot be blank")
            }
            if (userName.length > 100) {
                errors.add("Name cannot exceed 100 characters")
            }
        }
        
        // Primary goal validation is implicit through enum type safety
        // Additional business rules can be added here if needed
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Checks if a user has completed onboarding.
     * 
     * @param userId The ID of the user to check
     * @return Result containing true if onboarding is complete, false otherwise
     */
    suspend fun isOnboardingComplete(userId: String): Result<Boolean> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        return when (val result = userRepository.getCurrentUser()) {
            is Result.Success -> {
                val user = result.data
                if (user == null) {
                    Result.error(AppError.AuthenticationError("No authenticated user found"))
                } else if (user.id != userId) {
                    Result.error(AppError.AuthenticationError("User ID mismatch"))
                } else {
                    Result.success(user.onboardingComplete)
                }
            }
            is Result.Error -> Result.error(AppError.DataSyncError("Failed to check onboarding status: ${result.error.message}"))
        }
    }
}