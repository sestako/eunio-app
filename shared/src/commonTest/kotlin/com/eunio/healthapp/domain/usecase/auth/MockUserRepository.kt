package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result

/**
 * Mock implementation of UserRepository for testing purposes
 */
class MockUserRepository : UserRepository {
    
    var getCurrentUserResult: Result<User?> = Result.success(null)
    var updateUserResult: Result<Unit> = Result.success(Unit)
    var completeOnboardingResult: Result<Unit> = Result.success(Unit)
    var createUserResult: Result<User>? = null
    var signInUserResult: Result<User>? = null
    var signOutUserResult: Result<Unit> = Result.success(Unit)
    var deleteUserResult: Result<Unit> = Result.success(Unit)
    
    // Track method calls for verification
    var lastUpdatedUser: User? = null
    var lastOnboardingUserId: String? = null
    var lastOnboardingGoal: HealthGoal? = null
    var lastCreateUserEmail: String? = null
    var lastCreateUserPassword: String? = null
    var lastCreateUserName: String? = null
    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastDeletedUserId: String? = null
    
    override suspend fun getCurrentUser(): Result<User?> {
        return getCurrentUserResult
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        lastUpdatedUser = user
        return updateUserResult
    }
    
    override suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit> {
        lastOnboardingUserId = userId
        lastOnboardingGoal = primaryGoal
        return completeOnboardingResult
    }
    
    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        lastCreateUserEmail = email
        lastCreateUserPassword = password
        lastCreateUserName = name
        return createUserResult ?: Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError("Mock not configured"))
    }
    
    override suspend fun signInUser(email: String, password: String): Result<User> {
        lastSignInEmail = email
        lastSignInPassword = password
        return signInUserResult ?: Result.error(com.eunio.healthapp.domain.error.AppError.UnknownError("Mock not configured"))
    }
    
    override suspend fun signOutUser(): Result<Unit> {
        return signOutUserResult
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        lastDeletedUserId = userId
        return deleteUserResult
    }
}