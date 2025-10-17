package com.eunio.healthapp.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.eunio.healthapp.services.AnalyticsService
import com.eunio.healthapp.services.CrashlyticsService
import com.eunio.healthapp.network.RetryPolicy
import com.eunio.healthapp.network.withRetry
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of AuthService using Firebase Auth
 */
class AndroidAuthService(
    private val analyticsService: AnalyticsService,
    private val crashlyticsService: CrashlyticsService
) : AuthService {
    
    companion object {
        private const val TAG = "AuthService"
    }
    
    private val auth = FirebaseAuth.getInstance()
    
    init {
        Log.i(TAG, "AndroidAuthService initialized")
    }
    
    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = withRetry(
                policy = RetryPolicy.DEFAULT,
                shouldRetry = { error ->
                    // Only retry network errors, not auth errors (wrong password, etc.)
                    val message = error.message?.lowercase() ?: ""
                    message.contains("network") || 
                    message.contains("timeout") ||
                    message.contains("unavailable")
                },
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying signIn (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                auth.signInWithEmailAndPassword(email, password).await()
            }
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID returned"))
            analyticsService.logSignIn("email")
            crashlyticsService.setUserId(userId)
            crashlyticsService.log("User signed in: $email")
            Log.d(TAG, "Successfully signed in user: $email")
            Result.success(userId)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Sign in failed with Firebase auth error: ${e.errorCode}", e)
            crashlyticsService.recordException(e)
            Result.failure(Exception(mapAuthError(e)))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed with network error", e)
            crashlyticsService.recordException(e)
            Result.failure(Exception(AuthError.NetworkError.message))
        }
    }
    
    override suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("No user ID returned"))
            
            // Create user profile in Firestore
            val profile = com.eunio.healthapp.models.UserProfile.create(
                userId = userId,
                email = email,
                displayName = email.substringBefore("@")
            )
            val profileService = com.eunio.healthapp.services.AndroidUserProfileService()
            profileService.createProfile(profile)
            
            analyticsService.logSignUp("email")
            crashlyticsService.setUserId(userId)
            crashlyticsService.log("User signed up: $email")
            Log.d(TAG, "Successfully signed up user: $email")
            Result.success(userId)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Sign up failed with Firebase auth error: ${e.errorCode}", e)
            crashlyticsService.recordException(e)
            Result.failure(Exception(mapAuthError(e)))
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed with network error", e)
            crashlyticsService.recordException(e)
            Result.failure(Exception(AuthError.NetworkError.message))
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            analyticsService.logSignOut()
            Log.d(TAG, "Successfully signed out user")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.DEFAULT,
                shouldRetry = { error ->
                    // Only retry network errors
                    val message = error.message?.lowercase() ?: ""
                    message.contains("network") || 
                    message.contains("timeout") ||
                    message.contains("unavailable")
                },
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying resetPassword (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                auth.sendPasswordResetEmail(email).await()
            }
            Log.d(TAG, "Successfully sent password reset email to: $email")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Password reset failed with Firebase auth error: ${e.errorCode}", e)
            Result.failure(Exception(mapAuthError(e)))
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed with network error", e)
            Result.failure(Exception(AuthError.NetworkError.message))
        }
    }
    
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    override fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }
    
    override fun observeAuthState(callback: (String?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            callback(firebaseAuth.currentUser?.uid)
        }
    }
    
    private fun mapAuthError(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmail.message
            "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword.message
            "ERROR_USER_NOT_FOUND" -> AuthError.UserNotFound.message
            "ERROR_WRONG_PASSWORD" -> AuthError.WrongPassword.message
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyInUse.message
            "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password"
            else -> {
                // Check if message contains common error patterns
                val message = e.message?.lowercase() ?: ""
                when {
                    message.contains("password") && message.contains("incorrect") -> "Incorrect email or password"
                    message.contains("credential") && message.contains("incorrect") -> "Incorrect email or password"
                    message.contains("malformed") -> "Incorrect email or password"
                    message.contains("expired") -> "Session expired. Please try again"
                    else -> "Unable to sign in. Please check your credentials"
                }
            }
        }
    }
}
