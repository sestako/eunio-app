package com.eunio.healthapp.data.remote.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock

/**
 * Android implementation of AuthService using Firebase Authentication.
 * Handles user authentication, registration, and session management.
 */
class FirebaseAuthService(
    private val errorHandler: ErrorHandler
) : AuthService {
    
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    override val authState: Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let { mapFirebaseUserToUser(it) }
            trySend(user)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    
    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            val user = firebaseUser?.let { mapFirebaseUserToUser(it) }
            Result.success(user)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
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
            
            // Create user with Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.error(AppError.AuthenticationError("Failed to create user"))
            
            // Update user profile with display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            
            firebaseUser.updateProfile(profileUpdates).await()
            
            // Map to domain user
            val user = mapFirebaseUserToUser(firebaseUser, name)
            Result.success(user)
            
        } catch (e: FirebaseAuthException) {
            Result.error(mapFirebaseAuthException(e))
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
            
            // Sign in with Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.error(AppError.AuthenticationError("Failed to sign in"))
            
            // Map to domain user
            val user = mapFirebaseUserToUser(firebaseUser)
            Result.success(user)
            
        } catch (e: FirebaseAuthException) {
            Result.error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
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
            
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
            
        } catch (e: FirebaseAuthException) {
            Result.error(mapFirebaseAuthException(e))
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    override fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Maps a Firebase user to our domain User model
     */
    private fun mapFirebaseUserToUser(firebaseUser: FirebaseUser, displayName: String? = null): User {
        val now = Clock.System.now()
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = displayName ?: firebaseUser.displayName ?: "",
            onboardingComplete = false, // Will be updated during onboarding
            primaryGoal = HealthGoal.GENERAL_HEALTH, // Default goal, will be set during onboarding
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Maps Firebase authentication exceptions to our domain errors
     */
    private fun mapFirebaseAuthException(exception: FirebaseAuthException): AppError {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> AppError.ValidationError("Invalid email format", "email", exception)
            "ERROR_WEAK_PASSWORD" -> AppError.ValidationError("Password is too weak", "password", exception)
            "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.AuthenticationError("Email is already registered", exception)
            "ERROR_USER_NOT_FOUND" -> AppError.AuthenticationError("No account found with this email", exception)
            "ERROR_WRONG_PASSWORD" -> AppError.AuthenticationError("Incorrect password", exception)
            "ERROR_USER_DISABLED" -> AppError.AuthenticationError("This account has been disabled", exception)
            "ERROR_TOO_MANY_REQUESTS" -> AppError.AuthenticationError("Too many failed attempts. Please try again later", exception)
            "ERROR_NETWORK_REQUEST_FAILED" -> AppError.NetworkError("Network error. Please check your connection", exception)
            else -> AppError.AuthenticationError(exception.message ?: "Authentication failed", exception)
        }
    }
}