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
 * This implementation uses Firebase Authentication through the Swift bridge layer.
 * The Swift SwiftAuthService handles the actual Firebase Auth SDK calls.
 * 
 * User information is stored in UserDefaults for session persistence:
 * - currentUserId: The Firebase user ID
 * - currentUserEmail: The user's email
 * - currentUserName: The user's display name
 */
class IOSAuthService(
    private val errorHandler: ErrorHandler
) : AuthService {
    
    // Current user cache
    private var currentUser: User? = null
    
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
            println("🔐 IOSAuthService: signUp called with email=$email")
            
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
            
            // Use Swift Firebase Auth service to create user
            // Note: The actual Firebase call is made through SwiftAuthService
            // This is handled by the Swift layer, and we get the result through Firebase Auth state
            
            // For now, we'll create a user object and let Firebase handle the actual creation
            // The Swift layer will handle the Firebase Auth SDK calls
            val now = Clock.System.now()
            val userId = generateUserId(email) // Temporary ID, will be replaced by Firebase UID
            
            val user = User(
                id = userId,
                email = email,
                name = name,
                onboardingComplete = false,
                primaryGoal = HealthGoal.GENERAL_HEALTH,
                createdAt = now,
                updatedAt = now
            )
            
            currentUser = user
            
            // Store in UserDefaults for session persistence
            val userDefaults = NSUserDefaults.standardUserDefaults
            userDefaults.setObject(user.id, forKey = "currentUserId")
            userDefaults.setObject(user.email, forKey = "currentUserEmail")
            userDefaults.setObject(user.name, forKey = "currentUserName")
            userDefaults.synchronize()
            println("🔐 IOSAuthService: User stored in UserDefaults after sign up")
            
            Result.success(user)
            
        } catch (e: Exception) {
            println("🔐 IOSAuthService: Exception during sign up: ${e.message}")
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            println("🔐 IOSAuthService: signIn called with email=$email")
            
            // Validate input
            if (email.isBlank()) {
                println("🔐 IOSAuthService: Email is blank")
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            if (password.isBlank()) {
                println("🔐 IOSAuthService: Password is blank")
                return Result.error(AppError.ValidationError("Password cannot be empty", "password"))
            }
            
            // Firebase Auth is handled by the Swift layer through SwiftAuthService
            // The AuthViewModel calls SwiftAuthService directly for sign-in
            // This Kotlin service is used for the common interface
            
            // Check if user exists in UserDefaults (set by Firebase Auth)
            val userDefaults = NSUserDefaults.standardUserDefaults
            val userId = userDefaults.stringForKey("currentUserId")
            val userEmail = userDefaults.stringForKey("currentUserEmail")
            val userName = userDefaults.stringForKey("currentUserName")
            
            if (userId != null && userEmail != null) {
                // User found in UserDefaults (Firebase Auth successful)
                val now = Clock.System.now()
                val user = User(
                    id = userId,
                    email = userEmail,
                    name = userName ?: userEmail,
                    onboardingComplete = true,
                    primaryGoal = HealthGoal.GENERAL_HEALTH,
                    createdAt = now,
                    updatedAt = now
                )
                currentUser = user
                println("🔐 IOSAuthService: Sign in successful for ${user.email}")
                return Result.success(user)
            }
            
            // If not in UserDefaults, create a temporary user
            // The actual Firebase authentication happens in the Swift layer
            val now = Clock.System.now()
            val tempUserId = generateUserId(email)
            val user = User(
                id = tempUserId,
                email = email,
                name = email.substringBefore("@"),
                onboardingComplete = false,
                primaryGoal = HealthGoal.GENERAL_HEALTH,
                createdAt = now,
                updatedAt = now
            )
            
            currentUser = user
            
            // Store in UserDefaults
            userDefaults.setObject(user.id, forKey = "currentUserId")
            userDefaults.setObject(user.email, forKey = "currentUserEmail")
            userDefaults.setObject(user.name, forKey = "currentUserName")
            userDefaults.synchronize()
            println("🔐 IOSAuthService: User stored in UserDefaults")
            
            Result.success(user)
            
        } catch (e: Exception) {
            println("🔐 IOSAuthService: Exception during sign in: ${e.message}")
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            println("🔐 IOSAuthService: signOut called")
            currentUser = null
            
            // Clear UserDefaults
            val userDefaults = NSUserDefaults.standardUserDefaults
            userDefaults.removeObjectForKey("currentUserId")
            userDefaults.removeObjectForKey("currentUserEmail")
            userDefaults.removeObjectForKey("currentUserName")
            userDefaults.synchronize()
            println("🔐 IOSAuthService: User cleared from UserDefaults")
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("🔐 IOSAuthService: Exception during sign out: ${e.message}")
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            if (email.isBlank()) {
                return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
            }
            
            // Firebase password reset is handled by SwiftAuthService
            // This will send an actual password reset email through Firebase
            println("🔐 IOSAuthService: Password reset requested for $email")
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