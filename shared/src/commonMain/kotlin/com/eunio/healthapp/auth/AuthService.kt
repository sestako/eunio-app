package com.eunio.healthapp.auth

/**
 * Authentication service interface for Firebase Auth
 * Provides cross-platform authentication functionality
 */
interface AuthService {
    
    /**
     * Sign in with email and password
     * @return Result with user ID on success, error message on failure
     */
    suspend fun signIn(email: String, password: String): Result<String>
    
    /**
     * Sign up with email and password
     * @return Result with user ID on success, error message on failure
     */
    suspend fun signUp(email: String, password: String): Result<String>
    
    /**
     * Sign out current user
     * @return Result with Unit on success, error message on failure
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Send password reset email
     * @return Result with Unit on success, error message on failure
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Get current user ID
     * @return User ID if signed in, null otherwise
     */
    fun getCurrentUserId(): String?
    
    /**
     * Check if user is signed in
     * @return true if signed in, false otherwise
     */
    fun isSignedIn(): Boolean
    
    /**
     * Observe authentication state changes
     * @param callback Called when auth state changes with user ID (null if signed out)
     */
    fun observeAuthState(callback: (String?) -> Unit)
}

/**
 * Authentication result data class
 */
data class AuthResult(
    val userId: String,
    val email: String?
)

/**
 * Authentication error types
 */
sealed class AuthError(val message: String) {
    object InvalidEmail : AuthError("Invalid email address")
    object WeakPassword : AuthError("Password must be at least 6 characters")
    object UserNotFound : AuthError("No account found with this email")
    object WrongPassword : AuthError("Incorrect password")
    object EmailAlreadyInUse : AuthError("An account already exists with this email")
    object NetworkError : AuthError("Network error. Please check your connection")
    data class Unknown(val error: String) : AuthError(error)
}
