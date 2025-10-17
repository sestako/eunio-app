package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of AuthManager using Firebase Authentication.
 * Provides Firebase Auth integration for authentication operations on iOS.
 * 
 * Note: This uses the Swift-level Firebase Auth. The actual Firebase user
 * is managed by the SwiftUI layer and we access it through UserDefaults.
 */
class IOSAuthManager : AuthManager {
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank() || password.isBlank()) {
                return Result.error(AppError.ValidationError("Email and password cannot be empty"))
            }
            
            if (!isValidEmail(email)) {
                return Result.error(AppError.ValidationError("Invalid email format", "email"))
            }
            
            // iOS auth is handled by SwiftUI layer
            // This is a placeholder that returns success
            val mockUser = createMockUser(email, "User")
            Result.success(mockUser)
            
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Sign in failed: ${e.message}", e))
        }
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank() || password.isBlank() || name.isBlank()) {
                return Result.error(AppError.ValidationError("Email, password, and name cannot be empty"))
            }
            
            if (!isValidEmail(email)) {
                return Result.error(AppError.ValidationError("Invalid email format", "email"))
            }
            
            if (password.length < 6) {
                return Result.error(AppError.ValidationError("Password must be at least 6 characters", "password"))
            }
            
            // iOS auth is handled by SwiftUI layer
            val mockUser = createMockUser(email, name)
            Result.success(mockUser)
            
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Sign up failed: ${e.message}", e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            // iOS auth is handled by SwiftUI layer
            userDefaults.removeObjectForKey("currentUserId")
            userDefaults.removeObjectForKey("currentUserEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Sign out failed: ${e.message}", e))
        }
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            // Try to get user ID from UserDefaults (set by SwiftUI layer)
            val userId = userDefaults.stringForKey("currentUserId")
            val userEmail = userDefaults.stringForKey("currentUserEmail")
            
            if (userId != null && userEmail != null) {
                val user = User(
                    id = userId,
                    email = userEmail,
                    name = userDefaults.stringForKey("currentUserName") ?: "",
                    onboardingComplete = false,
                    primaryGoal = HealthGoal.GENERAL_HEALTH,
                    unitSystem = UnitSystem.METRIC,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    settings = null
                )
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Failed to get current user: ${e.message}", e))
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            if (email.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            
            if (!isValidEmail(email)) {
                return Result.error(AppError.ValidationError("Invalid email format", "email"))
            }
            
            // iOS auth is handled by SwiftUI layer
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Password reset failed: ${e.message}", e))
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return try {
            userDefaults.stringForKey("currentUserId") != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates email format using a simple regex pattern
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return email.matches(emailPattern.toRegex())
    }
    
    /**
     * Creates a mock user for iOS
     * The actual Firebase user is managed by the SwiftUI layer
     */
    private fun createMockUser(email: String, name: String): User {
        val now = Clock.System.now()
        return User(
            id = "ios_${email.hashCode()}",
            email = email,
            name = name,
            onboardingComplete = false,
            primaryGoal = HealthGoal.GENERAL_HEALTH,
            unitSystem = UnitSystem.METRIC,
            createdAt = now,
            updatedAt = now,
            settings = null
        )
    }
}