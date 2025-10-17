package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.manager.AuthManager
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Simple mock implementation of AuthManager for testing.
 */
class MockAuthManager : AuthManager {
    override suspend fun signIn(email: String, password: String): Result<User> {
        val now = Clock.System.now()
        return Result.Success(
            User(
                id = "test-user-123",
                email = email,
                name = "Test User",
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                createdAt = now,
                updatedAt = now
            )
        )
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        val now = Clock.System.now()
        return Result.Success(
            User(
                id = "test-user-123",
                email = email,
                name = name,
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                createdAt = now,
                updatedAt = now
            )
        )
    }
    
    override suspend fun signOut(): Result<Unit> = 
        Result.Success(Unit)
    
    override suspend fun resetPassword(email: String): Result<Unit> = 
        Result.Success(Unit)
    
    override suspend fun getCurrentUser(): Result<User?> {
        val now = Clock.System.now()
        return Result.Success(
            User(
                id = "test-user-123",
                email = "test@example.com",
                name = "Test User",
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                createdAt = now,
                updatedAt = now
            )
        )
    }
    
    override suspend fun isAuthenticated(): Boolean = true
}

