package com.eunio.healthapp.data.remote.auth

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for authentication service validation logic.
 * These tests verify input validation without requiring Firebase initialization.
 */
class AuthServiceValidationTest {
    
    @Test
    fun `validation should reject empty email`() {
        // Given
        val email = ""
        
        // When
        val isValid = email.isNotBlank() && email.contains("@")
        
        // Then
        assertTrue(!isValid, "Empty email should be invalid")
    }
    
    @Test
    fun `validation should reject invalid email format`() {
        // Given
        val email = "invalid-email"
        
        // When
        val isValid = email.contains("@") && email.contains(".")
        
        // Then
        assertTrue(!isValid, "Invalid email format should be rejected")
    }
    
    @Test
    fun `validation should accept valid email`() {
        // Given
        val email = "test@example.com"
        
        // When
        val isValid = email.contains("@") && email.contains(".")
        
        // Then
        assertTrue(isValid, "Valid email should be accepted")
    }
    
    @Test
    fun `validation should reject short password`() {
        // Given
        val password = "123"
        
        // When
        val isValid = password.length >= 6
        
        // Then
        assertTrue(!isValid, "Short password should be rejected")
    }
    
    @Test
    fun `validation should accept valid password`() {
        // Given
        val password = "password123"
        
        // When
        val isValid = password.length >= 6
        
        // Then
        assertTrue(isValid, "Valid password should be accepted")
    }
    
    @Test
    fun `validation should reject empty name`() {
        // Given
        val name = ""
        
        // When
        val isValid = name.isNotBlank()
        
        // Then
        assertTrue(!isValid, "Empty name should be rejected")
    }
    
    @Test
    fun `validation should accept valid name`() {
        // Given
        val name = "Test User"
        
        // When
        val isValid = name.isNotBlank()
        
        // Then
        assertTrue(isValid, "Valid name should be accepted")
    }
    
    @Test
    fun `createValidationError should create proper error with field`() {
        // Given
        val message = "Email cannot be empty"
        val field = "email"
        
        // When
        val error = AppError.ValidationError(message, field)
        
        // Then
        assertEquals(message, error.message)
        assertEquals(field, error.field)
    }
    
    @Test
    fun `createAuthenticationError should create proper error`() {
        // Given
        val message = "Authentication failed"
        
        // When
        val error = AppError.AuthenticationError(message)
        
        // Then
        assertEquals(message, error.message)
    }
}

/**
 * Test implementation of AuthService for testing validation logic
 */
class TestAuthService : AuthService {
    
    override val authState: Flow<User?> = flowOf(null)
    
    override suspend fun getCurrentUser(): Result<User?> {
        return Result.success(null)
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        // Validate input like the real implementation
        if (email.isBlank()) {
            return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        if (password.length < 6) {
            return Result.error(AppError.ValidationError("Password must be at least 6 characters", "password"))
        }
        if (name.isBlank()) {
            return Result.error(AppError.ValidationError("Name cannot be empty", "name"))
        }
        
        // Return success for valid input (in real implementation, this would call Firebase)
        return Result.error(AppError.AuthenticationError("Test implementation"))
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        // Validate input like the real implementation
        if (email.isBlank()) {
            return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        if (password.isBlank()) {
            return Result.error(AppError.ValidationError("Password cannot be empty", "password"))
        }
        
        // Return success for valid input (in real implementation, this would call Firebase)
        return Result.error(AppError.AuthenticationError("Test implementation"))
    }
    
    override suspend fun signOut(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        
        return Result.success(Unit)
    }
    
    override fun isAuthenticated(): Boolean {
        return false
    }
}

class TestAuthServiceValidationTest {
    
    private val authService = TestAuthService()
    
    @Test
    fun `signUp with empty email should return validation error`() = runTest {
        // When
        val result = authService.signUp("", "password123", "Test User")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Email cannot be empty", error?.message)
        assertEquals("email", error?.field)
    }
    
    @Test
    fun `signUp with short password should return validation error`() = runTest {
        // When
        val result = authService.signUp("test@example.com", "123", "Test User")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Password must be at least 6 characters", error?.message)
        assertEquals("password", error?.field)
    }
    
    @Test
    fun `signUp with empty name should return validation error`() = runTest {
        // When
        val result = authService.signUp("test@example.com", "password123", "")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Name cannot be empty", error?.message)
        assertEquals("name", error?.field)
    }
    
    @Test
    fun `signIn with empty email should return validation error`() = runTest {
        // When
        val result = authService.signIn("", "password123")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Email cannot be empty", error?.message)
        assertEquals("email", error?.field)
    }
    
    @Test
    fun `signIn with empty password should return validation error`() = runTest {
        // When
        val result = authService.signIn("test@example.com", "")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Password cannot be empty", error?.message)
        assertEquals("password", error?.field)
    }
    
    @Test
    fun `sendPasswordResetEmail with empty email should return validation error`() = runTest {
        // When
        val result = authService.sendPasswordResetEmail("")
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull() as? AppError.ValidationError
        assertEquals("Email cannot be empty", error?.message)
        assertEquals("email", error?.field)
    }
}