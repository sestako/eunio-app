package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Common test suite for AuthManager implementations.
 * This abstract class defines the contract that all AuthManager implementations must satisfy.
 */
abstract class AuthManagerTest {
    
    /**
     * Create an instance of the AuthManager implementation to test
     */
    abstract fun createAuthManager(): AuthManager
    
    @Test
    fun `signIn with valid credentials should return success`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signIn("test@example.com", "password123")
        
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user.email)
    }
    
    @Test
    fun `signIn with empty email should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signIn("", "password123")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Email and password cannot be empty"))
    }
    
    @Test
    fun `signIn with empty password should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signIn("test@example.com", "")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Email and password cannot be empty"))
    }
    
    @Test
    fun `signIn with invalid email format should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signIn("invalid-email", "password123")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertEquals("email", error.field)
        assertTrue(error.message.contains("Invalid email format"))
    }
    
    @Test
    fun `signUp with valid data should return success`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("newuser@example.com", "password123", "New User")
        
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("newuser@example.com", user.email)
        assertEquals("New User", user.name)
    }
    
    @Test
    fun `signUp with empty email should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("", "password123", "New User")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Email, password, and name cannot be empty"))
    }
    
    @Test
    fun `signUp with empty password should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("test@example.com", "", "New User")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Email, password, and name cannot be empty"))
    }
    
    @Test
    fun `signUp with empty name should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("test@example.com", "password123", "")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Email, password, and name cannot be empty"))
    }
    
    @Test
    fun `signUp with invalid email format should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("invalid-email", "password123", "New User")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertEquals("email", error.field)
        assertTrue(error.message.contains("Invalid email format"))
    }
    
    @Test
    fun `signUp with short password should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signUp("test@example.com", "123", "New User")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertEquals("password", error.field)
        assertTrue(error.message.contains("Password must be at least 6 characters"))
    }
    
    @Test
    fun `signOut should return success`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.signOut()
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `getCurrentUser should return null when not authenticated`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.getCurrentUser()
        
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNull(user)
    }
    
    @Test
    fun `resetPassword with valid email should return success`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.resetPassword("test@example.com")
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `resetPassword with empty email should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.resetPassword("")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertEquals("email", error.field)
        assertTrue(error.message.contains("Email cannot be empty"))
    }
    
    @Test
    fun `resetPassword with invalid email format should return validation error`() = runTest {
        val authManager = createAuthManager()
        val result = authManager.resetPassword("invalid-email")
        
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertEquals("email", error.field)
        assertTrue(error.message.contains("Invalid email format"))
    }
    
    @Test
    fun `isAuthenticated should return false when not authenticated`() = runTest {
        val authManager = createAuthManager()
        val isAuthenticated = authManager.isAuthenticated()
        
        assertFalse(isAuthenticated)
    }
}