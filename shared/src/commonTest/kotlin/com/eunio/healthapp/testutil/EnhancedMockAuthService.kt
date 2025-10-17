package com.eunio.healthapp.testutil

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours

/**
 * Enhanced Mock Authentication Service that integrates with MockApiService
 * for realistic network behavior simulation including delays, timeouts, and retry mechanisms.
 * 
 * This service provides:
 * - Proper authentication flow with network delays
 * - Timeout handling for authentication requests
 * - Error handling for network failures during auth
 * - Retry mechanisms for failed authentication
 * 
 * Requirements: 3.2, 3.5
 */
class EnhancedMockAuthService(
    private val apiService: MockApiService
) : AuthService {
    
    private val _authState = MutableStateFlow<User?>(null)
    override val authState: Flow<User?> = _authState.asStateFlow()
    
    private val userDatabase = mutableMapOf<String, StoredUser>() // email -> StoredUser
    private val passwordResetTokens = mutableMapOf<String, ResetToken>() // email -> token
    private val activeSessions = mutableMapOf<String, AuthSession>() // userId -> session
    
    // Authentication configuration
    private var sessionTimeout: Duration = 24.hours
    private var requiresReauth = false
    private var maxAuthRetries = 3
    private var authRetryDelay: Duration = 1.seconds
    
    private data class StoredUser(
        val user: User,
        val passwordHash: String,
        val isLocked: Boolean = false,
        val failedAttempts: Int = 0,
        val lastFailedAttempt: kotlinx.datetime.Instant? = null
    )
    
    private data class ResetToken(
        val token: String,
        val email: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        val expiresAt: kotlinx.datetime.Instant = Clock.System.now().plus(1.hours)
    )
    
    private data class AuthSession(
        val userId: String,
        val token: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        val expiresAt: kotlinx.datetime.Instant = Clock.System.now().plus(24.hours),
        val isValid: Boolean = true
    )
    
    override suspend fun getCurrentUser(): Result<User?> {
        val result = apiService.executeRequest("getCurrentUser") {
            val currentUser = _authState.value
            if (currentUser != null) {
                val session = activeSessions[currentUser.id]
                if (session == null || !session.isValid || Clock.System.now() > session.expiresAt) {
                    // Session expired or invalid
                    _authState.value = null
                    activeSessions.remove(currentUser.id)
                    throw Exception("Session expired")
                }
            }
            currentUser
        }
        
        return when (result) {
            is Result.Success -> result
            is Result.Error -> Result.Error(AppError.NetworkError(result.error.message ?: "Unknown error"))
        }
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        val result = apiService.executeRequestWithRetry("signUp") {
            // Input validation
            if (email.isBlank() || password.isBlank() || name.isBlank()) {
                throw Exception("All fields are required")
            }
            
            if (!isValidEmail(email)) {
                throw Exception("Invalid email format")
            }
            
            if (!isValidPassword(password)) {
                throw Exception("Password must be at least 8 characters long")
            }
            
            // Check if user already exists
            if (userDatabase.containsKey(email.lowercase())) {
                throw Exception("User with this email already exists")
            }
            
            // Create new user
            val userId = "auth_user_${Clock.System.now().toEpochMilliseconds()}"
            val newUser = User(
                id = userId,
                email = email,
                name = name,
                onboardingComplete = false,
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                unitSystem = UnitSystem.METRIC,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            // Store user with password hash
            userDatabase[email.lowercase()] = StoredUser(
                user = newUser,
                passwordHash = hashPassword(password)
            )
            
            // Create session
            val session = AuthSession(
                userId = userId,
                token = generateSessionToken(),
                expiresAt = Clock.System.now().plus(sessionTimeout)
            )
            activeSessions[userId] = session
            
            // Update auth state
            _authState.value = newUser
            
            newUser
        }
        
        return when (result) {
            is Result.Success -> result
            is Result.Error -> {
                val errorMessage = result.error.message ?: "Unknown error"
                val mappedError = when {
                    errorMessage.contains("already exists") -> 
                        AppError.ValidationError("User already exists")
                    errorMessage.contains("required") -> 
                        AppError.ValidationError(errorMessage)
                    errorMessage.contains("Invalid") -> 
                        AppError.ValidationError(errorMessage)
                    else -> AppError.AuthenticationError("Sign up failed: $errorMessage")
                }
                Result.Error(mappedError)
            }
        }
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        val result = apiService.executeRequestWithRetry("signIn") {
            // Input validation
            if (email.isBlank() || password.isBlank()) {
                throw Exception("Email and password are required")
            }
            
            // Check for re-authentication requirement
            if (requiresReauth) {
                throw Exception("Re-authentication required")
            }
            
            // Find user
            val storedUser = userDatabase[email.lowercase()]
                ?: throw Exception("Invalid credentials")
            
            // Check if account is locked
            if (storedUser.isLocked) {
                throw Exception("Account is temporarily locked due to too many failed attempts")
            }
            
            // Verify password
            if (!verifyPassword(password, storedUser.passwordHash)) {
                // Update failed attempts
                val updatedUser = storedUser.copy(
                    failedAttempts = storedUser.failedAttempts + 1,
                    lastFailedAttempt = Clock.System.now(),
                    isLocked = storedUser.failedAttempts + 1 >= 5 // Lock after 5 failed attempts
                )
                userDatabase[email.lowercase()] = updatedUser
                throw Exception("Invalid credentials")
            }
            
            // Reset failed attempts on successful login
            if (storedUser.failedAttempts > 0) {
                userDatabase[email.lowercase()] = storedUser.copy(
                    failedAttempts = 0,
                    lastFailedAttempt = null,
                    isLocked = false
                )
            }
            
            // Create or update session
            val session = AuthSession(
                userId = storedUser.user.id,
                token = generateSessionToken(),
                expiresAt = Clock.System.now().plus(sessionTimeout)
            )
            activeSessions[storedUser.user.id] = session
            
            // Update auth state
            _authState.value = storedUser.user
            
            storedUser.user
        }
        
        return when (result) {
            is Result.Success -> result
            is Result.Error -> {
                val errorMessage = result.error.message ?: "Unknown error"
                val mappedError = when {
                    errorMessage.contains("required") -> 
                        AppError.ValidationError(errorMessage)
                    errorMessage.contains("locked") -> 
                        AppError.AuthenticationError("Account locked")
                    errorMessage.contains("Re-authentication") -> 
                        AppError.AuthenticationError("Re-authentication required")
                    else -> AppError.AuthenticationError("Sign in failed: $errorMessage")
                }
                Result.Error(mappedError)
            }
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        val result = apiService.executeRequest("signOut") {
            val currentUser = _authState.value
            if (currentUser != null) {
                activeSessions.remove(currentUser.id)
            }
            _authState.value = null
        }
        
        return when (result) {
            is Result.Success -> result
            is Result.Error -> Result.Error(AppError.AuthenticationError("Sign out failed: ${result.error.message ?: "Unknown error"}"))
        }
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val result = apiService.executeRequestWithRetry("sendPasswordResetEmail") {
            if (email.isBlank()) {
                throw Exception("Email is required")
            }
            
            if (!isValidEmail(email)) {
                throw Exception("Invalid email format")
            }
            
            // Check if user exists (in real implementation, we might not reveal this)
            val userExists = userDatabase.containsKey(email.lowercase())
            if (!userExists) {
                // In production, we might still return success to avoid revealing user existence
                // For testing, we'll return an error
                throw Exception("User not found")
            }
            
            // Generate reset token
            val resetToken = ResetToken(
                token = generateResetToken(),
                email = email.lowercase()
            )
            passwordResetTokens[email.lowercase()] = resetToken
            
            // In real implementation, this would send an email
            // For testing, we just store the token
        }
        
        return when (result) {
            is Result.Success -> result
            is Result.Error -> {
                val errorMessage = result.error.message ?: "Unknown error"
                val mappedError = when {
                    errorMessage.contains("required") -> 
                        AppError.ValidationError(errorMessage)
                    errorMessage.contains("Invalid") -> 
                        AppError.ValidationError(errorMessage)
                    errorMessage.contains("not found") -> 
                        AppError.AuthenticationError("User not found")
                    else -> AppError.NetworkError("Failed to send reset email: $errorMessage")
                }
                Result.Error(mappedError)
            }
        }
    }
    
    override fun isAuthenticated(): Boolean {
        val currentUser = _authState.value ?: return false
        val session = activeSessions[currentUser.id] ?: return false
        return session.isValid && Clock.System.now() <= session.expiresAt
    }
    
    // Configuration methods for testing
    
    /**
     * Sets the session timeout duration.
     */
    fun setSessionTimeout(timeout: Duration) {
        this.sessionTimeout = timeout
    }
    
    /**
     * Forces re-authentication requirement.
     */
    fun setRequiresReauth(requires: Boolean) {
        this.requiresReauth = requires
    }
    
    /**
     * Configures retry behavior for authentication requests.
     */
    fun setAuthRetryConfiguration(maxRetries: Int, retryDelay: Duration) {
        this.maxAuthRetries = maxRetries
        this.authRetryDelay = retryDelay
        
        // Update API service retry configuration
        apiService.setRetryConfiguration(maxRetries, retryDelay, useExponentialBackoff = true)
    }
    
    /**
     * Simulates session expiration for testing.
     */
    fun expireCurrentSession() {
        val currentUser = _authState.value
        if (currentUser != null) {
            val session = activeSessions[currentUser.id]
            if (session != null) {
                activeSessions[currentUser.id] = session.copy(
                    expiresAt = Clock.System.now().minus(1.seconds),
                    isValid = false
                )
            }
        }
    }
    
    /**
     * Gets the number of registered users for testing.
     */
    fun getUserCount(): Int = userDatabase.size
    
    /**
     * Gets the password reset token for testing.
     */
    fun getPasswordResetToken(email: String): String? {
        return passwordResetTokens[email.lowercase()]?.token
    }
    
    /**
     * Clears all authentication data for testing.
     */
    fun clear() {
        userDatabase.clear()
        passwordResetTokens.clear()
        activeSessions.clear()
        _authState.value = null
        requiresReauth = false
        sessionTimeout = 24.hours
        maxAuthRetries = 3
        authRetryDelay = 1.seconds
    }
    
    // Private helper methods
    
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
    
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
    
    private fun hashPassword(password: String): String {
        // Simple hash simulation for testing
        return "hash_${password.hashCode()}"
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
    
    private fun generateSessionToken(): String {
        return "session_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    private fun generateResetToken(): String {
        return "reset_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }

}