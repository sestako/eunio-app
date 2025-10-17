package com.eunio.healthapp.data.remote.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of AuthService.
 * 
 * This implementation reads the Firebase user information from UserDefaults,
 * which is set by the Swift Firebase authentication layer.
 * 
 * The Swift layer stores:
 * - currentUserId: The Firebase user ID
 * - currentUserEmail: The user's email
 * - currentUserName: The user's display name
 */
class IOSAuthService(
    private val errorHandler: ErrorHandler
) : AuthService {
    
    // Mock storage for development - replace with Firebase iOS SDK
    private var currentUser: User? = null
    private val mockUsers = mutableMapOf<String, Pair<String, User>>() // email -> (password, user)
    
    override val authState: Flow<User?> = flowOf(currentUser)
    
    override suspend fun getCurrentUser(): Result<User?> {
        // Try to get user from UserDefaults (set by Swift Firebase layer)
        val userDefaults = NSUserDefaults.standardUserDefaults
        val userId = userDefaults.stringForKey("currentUserId")
        val userEmail = userDefaults.stringForKey("currentUserEmail")
        val userName = userDefaults.stringForKey("currentUserName")
        
        if (userId != null && userEmail != null) {
            // Create user from UserDefaults data
            val now = Clock.System.now()
            val user = User(
                id = userId,
                email = userEmail,
                name = userName ?: userEmail,
                onboardingComplete = true, // Assume onboarding is complete if user is authenticated
                primaryGoal = HealthGoal.GENERAL_HEALTH,
                createdAt = now,
                updatedAt = now
            )
            currentUser = user
            return Result.success(user)
        }
        
        // Fall back to mock user if available
        return Result.success(currentUser)
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            if (password.length < 6) {
                return Result.error(AppError.ValidationError("Password must be at least 6 characters", "password"))
            }
            if (name.isBlank()) {
                return Result.error(AppError.ValidationError("Name cannot be empty", "name"))
            }
            
            // Check if user already exists
            if (mockUsers.containsKey(email)) {
                return Result.error(AppError.AuthenticationError("Email is already registered"))
            }
            
            // Create new user
            val now = Clock.System.now()
            val user = User(
                id = generateUserId(email),
                email = email,
                name = name,
                onboardingComplete = false,
                primaryGoal = HealthGoal.GENERAL_HEALTH,
                createdAt = now,
                updatedAt = now
            )
            
            // Store user (in production, this would be handled by Firebase)
            mockUsers[email] = password to user
            currentUser = user
            
            Result.success(user)
            
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            if (password.isBlank()) {
                return Result.error(AppError.ValidationError("Password cannot be empty", "password"))
            }
            
            // Check credentials
            val storedData = mockUsers[email]
            if (storedData == null) {
                return Result.error(AppError.AuthenticationError("No account found with this email"))
            }
            
            val (storedPassword, user) = storedData
            if (storedPassword != password) {
                return Result.error(AppError.AuthenticationError("Incorrect password"))
            }
            
            // Sign in successful
            currentUser = user
            Result.success(user)
            
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            currentUser = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            if (email.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            
            // Check if user exists
            if (!mockUsers.containsKey(email)) {
                return Result.error(AppError.AuthenticationError("No account found with this email"))
            }
            
            // In production, this would send an actual email via Firebase
            // For now, just return success
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override fun isAuthenticated(): Boolean {
        return currentUser != null
    }
    
    /**
     * Generate a consistent user ID from email (for mock implementation)
     * In production, Firebase would generate this
     */
    private fun generateUserId(email: String): String {
        return "user_${email.hashCode().toString().replace("-", "")}"
    }
}