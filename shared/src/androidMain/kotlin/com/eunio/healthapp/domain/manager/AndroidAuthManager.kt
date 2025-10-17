package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Android implementation of AuthManager using Firebase Authentication.
 * Provides Firebase Auth integration for authentication operations.
 */
class AndroidAuthManager : AuthManager {
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank() || password.isBlank()) {
                return Result.error(AppError.ValidationError("Email and password cannot be empty"))
            }
            
            if (!isValidEmail(email)) {
                return Result.error(AppError.ValidationError("Invalid email format", "email"))
            }
            
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.error(AppError.AuthenticationError("Sign in failed"))
            
            val user = mapFirebaseUserToUser(firebaseUser)
            Result.success(user)
            
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
            
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.error(AppError.AuthenticationError("Sign up failed"))
            
            // Update user profile with name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            val user = mapFirebaseUserToUser(firebaseUser)
            Result.success(user)
            
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Sign up failed: ${e.message}", e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Sign out failed: ${e.message}", e))
        }
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val user = mapFirebaseUserToUser(firebaseUser)
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
            
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.AuthenticationError("Password reset failed: ${e.message}", e))
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        return try {
            firebaseAuth.currentUser != null
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
     * Maps Firebase user to domain User model
     */
    private fun mapFirebaseUserToUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        val now = Clock.System.now()
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            onboardingComplete = false, // This would come from Firestore user document
            primaryGoal = HealthGoal.GENERAL_HEALTH, // This would come from Firestore user document
            unitSystem = UnitSystem.METRIC, // This would come from user preferences
            createdAt = Instant.fromEpochMilliseconds(firebaseUser.metadata?.creationTimestamp ?: 0),
            updatedAt = now,
            settings = null
        )
    }
}