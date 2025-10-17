package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting the current authenticated user.
 * Provides both one-time and observable access to user authentication state.
 * Handles user state management for onboarding and profile access.
 */
class GetCurrentUserUseCase(
    private val userRepository: UserRepository,
    private val authService: AuthService
) {
    
    /**
     * Gets the current authenticated user as a one-time operation
     * @return Result containing the current user or null if not authenticated
     */
    suspend operator fun invoke(): Result<User?> {
        return userRepository.getCurrentUser()
    }
    
    /**
     * Observes the authentication state changes
     * @return Flow that emits the current user or null when authentication state changes
     */
    fun observeAuthState(): Flow<User?> {
        return authService.authState
    }
    
    /**
     * Checks if a user is currently authenticated
     * @return true if user is authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return authService.isAuthenticated()
    }
    
    /**
     * Gets the current user and checks if onboarding is complete
     * @return Result containing a pair of (User?, Boolean) where Boolean indicates onboarding completion
     */
    suspend fun getCurrentUserWithOnboardingStatus(): Result<Pair<User?, Boolean>> {
        return when (val result = userRepository.getCurrentUser()) {
            is Result.Success -> {
                val user = result.data
                val onboardingComplete = user?.onboardingComplete ?: false
                Result.success(Pair(user, onboardingComplete))
            }
            is Result.Error -> result
        }
    }
    
    /**
     * Checks if the current user needs to complete onboarding
     * @return Result containing true if onboarding is needed, false otherwise
     */
    suspend fun needsOnboarding(): Result<Boolean> {
        return when (val result = userRepository.getCurrentUser()) {
            is Result.Success -> {
                val user = result.data
                if (user == null) {
                    Result.success(false) // No user means no onboarding needed (should authenticate first)
                } else {
                    Result.success(!user.onboardingComplete)
                }
            }
            is Result.Error -> result
        }
    }
}