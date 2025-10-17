package com.eunio.healthapp.domain.usecase.profile

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.until

/**
 * Use case for retrieving user account statistics and usage information.
 */
class GetUserStatisticsUseCase(
    private val userRepository: UserRepository
) {
    
    /**
     * Gets comprehensive user statistics
     * @return Result containing UserStatistics or error
     */
    suspend operator fun invoke(): Result<UserStatistics> {
        return try {
            val currentUserResult = userRepository.getCurrentUser()
            if (currentUserResult is Result.Error) {
                return currentUserResult
            }
            
            val user = currentUserResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("User not found"))
            
            val statistics = calculateStatistics(user)
            Result.success(statistics)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError("Failed to get user statistics: ${e.message}", cause = e))
        }
    }
    
    private fun calculateStatistics(user: User): UserStatistics {
        val now = Clock.System.now()
        val daysSinceCreation = (now.toEpochMilliseconds() - user.createdAt.toEpochMilliseconds()) / (24 * 60 * 60 * 1000)
        val daysSinceLastUpdate = (now.toEpochMilliseconds() - user.updatedAt.toEpochMilliseconds()) / (24 * 60 * 60 * 1000)
        
        return UserStatistics(
            accountCreatedAt = user.createdAt,
            lastUpdatedAt = user.updatedAt,
            daysSinceCreation = daysSinceCreation.toInt(),
            daysSinceLastUpdate = daysSinceLastUpdate.toInt(),
            currentGoal = user.primaryGoal,
            onboardingCompleted = user.onboardingComplete,
            // These would be calculated from actual usage data in a real implementation
            totalLogsEntered = 0, // TODO: Calculate from daily logs
            cyclesTracked = 0, // TODO: Calculate from cycle data
            insightsGenerated = 0, // TODO: Calculate from insights data
            lastLoginDate = now // TODO: Track actual login dates
        )
    }
}

/**
 * Data class containing user account statistics
 */
data class UserStatistics(
    val accountCreatedAt: Instant,
    val lastUpdatedAt: Instant,
    val daysSinceCreation: Int,
    val daysSinceLastUpdate: Int,
    val currentGoal: com.eunio.healthapp.domain.model.HealthGoal,
    val onboardingCompleted: Boolean,
    val totalLogsEntered: Int,
    val cyclesTracked: Int,
    val insightsGenerated: Int,
    val lastLoginDate: Instant
) {
    
    /**
     * Gets a formatted account age string
     */
    fun getAccountAgeString(): String {
        return when {
            daysSinceCreation < 1 -> "Less than a day"
            daysSinceCreation < 7 -> "$daysSinceCreation days"
            daysSinceCreation < 30 -> "${daysSinceCreation / 7} weeks"
            daysSinceCreation < 365 -> "${daysSinceCreation / 30} months"
            else -> "${daysSinceCreation / 365} years"
        }
    }
    
    /**
     * Gets a formatted last activity string
     */
    fun getLastActivityString(): String {
        return when {
            daysSinceLastUpdate < 1 -> "Today"
            daysSinceLastUpdate == 1 -> "Yesterday"
            daysSinceLastUpdate < 7 -> "$daysSinceLastUpdate days ago"
            daysSinceLastUpdate < 30 -> "${daysSinceLastUpdate / 7} weeks ago"
            else -> "${daysSinceLastUpdate / 30} months ago"
        }
    }
    
    /**
     * Gets engagement level based on usage statistics
     */
    fun getEngagementLevel(): EngagementLevel {
        val totalActivity = totalLogsEntered + cyclesTracked + insightsGenerated
        
        return when {
            totalActivity >= 100 -> EngagementLevel.HIGH
            totalActivity >= 30 -> EngagementLevel.MEDIUM
            totalActivity >= 10 -> EngagementLevel.LOW
            else -> EngagementLevel.NEW
        }
    }
}

enum class EngagementLevel {
    NEW, LOW, MEDIUM, HIGH
}