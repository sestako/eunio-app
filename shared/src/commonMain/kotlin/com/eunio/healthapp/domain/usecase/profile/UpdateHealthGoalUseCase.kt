package com.eunio.healthapp.domain.usecase.profile

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Use case for updating user's primary health goal.
 * Handles goal change validation and impact on app functionality.
 */
class UpdateHealthGoalUseCase(
    private val userRepository: UserRepository
) {
    
    /**
     * Updates user's primary health goal
     * @param userId The ID of the user to update
     * @param newGoal The new primary health goal
     * @return Result containing the updated user or error
     */
    suspend operator fun invoke(
        userId: String,
        newGoal: HealthGoal
    ): Result<User> {
        return try {
            // Get current user
            val currentUserResult = userRepository.getCurrentUser()
            if (currentUserResult is Result.Error) {
                return currentUserResult
            }
            
            val currentUser = currentUserResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("User not found"))
            
            // Check if goal is actually changing
            if (currentUser.primaryGoal == newGoal) {
                return Result.success(currentUser)
            }
            
            // Create updated user
            val updatedUser = currentUser.copy(
                primaryGoal = newGoal
            )
            
            // Update user
            val updateResult = userRepository.updateUser(updatedUser)
            if (updateResult is Result.Error) {
                return updateResult
            }
            
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to update health goal: ${e.message}", cause = e))
        }
    }
    
    /**
     * Gets the impact description for changing to a specific health goal
     */
    fun getGoalChangeImpact(currentGoal: HealthGoal, newGoal: HealthGoal): String {
        if (currentGoal == newGoal) {
            return "No changes will be made to your current settings."
        }
        
        return when (newGoal) {
            HealthGoal.CONCEPTION -> {
                "Switching to conception tracking will:\n" +
                "• Enable fertility window predictions\n" +
                "• Adjust notification timing for optimal conception\n" +
                "• Focus insights on fertility patterns\n" +
                "• Recommend additional tracking parameters"
            }
            HealthGoal.CONTRACEPTION -> {
                "Switching to contraception will:\n" +
                "• Emphasize fertile window awareness\n" +
                "• Provide contraceptive effectiveness insights\n" +
                "• Adjust prediction algorithms for safety\n" +
                "• Focus on cycle regularity tracking"
            }
            HealthGoal.CYCLE_TRACKING -> {
                "Switching to general cycle tracking will:\n" +
                "• Provide comprehensive cycle analysis\n" +
                "• Balance all tracking features equally\n" +
                "• Offer general health insights\n" +
                "• Maintain flexible notification settings"
            }
            HealthGoal.GENERAL_HEALTH -> {
                "Switching to general health will:\n" +
                "• Broaden focus beyond reproductive health\n" +
                "• Include general wellness insights\n" +
                "• Adjust tracking recommendations\n" +
                "• Provide holistic health patterns"
            }
        }
    }
    
    /**
     * Gets available health goals with descriptions
     */
    fun getAvailableGoals(): List<Pair<HealthGoal, String>> {
        return listOf(
            HealthGoal.CONCEPTION to "Track fertility and optimize for conception",
            HealthGoal.CONTRACEPTION to "Monitor fertility for natural contraception",
            HealthGoal.CYCLE_TRACKING to "Understand and track menstrual cycles",
            HealthGoal.GENERAL_HEALTH to "Monitor overall reproductive and general health"
        )
    }
}