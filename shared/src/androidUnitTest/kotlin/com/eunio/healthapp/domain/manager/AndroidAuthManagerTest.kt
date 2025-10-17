package com.eunio.healthapp.domain.manager

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific tests for AndroidAuthManager.
 * Extends the common AuthManagerTest to ensure Android implementation meets the contract.
 */
class AndroidAuthManagerTest : AuthManagerTest() {
    
    override fun createAuthManager(): AuthManager = AndroidAuthManager()
    
    @Test
    fun `AndroidAuthManager should create mock users with correct platform-specific data`() {
        runBlocking {
            val authManager = AndroidAuthManager()
            val result = authManager.signUp("android@example.com", "password123", "Android User")
            
            assertTrue(result.isSuccess)
            val user = result.getOrNull()
            assertNotNull(user)
            assertEquals("android@example.com", user.email)
            assertEquals("Android User", user.name)
            assertTrue(user.id.startsWith("mock_"))
            assertNotNull(user.createdAt)
            assertNotNull(user.updatedAt)
        }
    }
    
    @Test
    fun `AndroidAuthManager should handle email validation correctly`() {
        runBlocking {
            val authManager = AndroidAuthManager()
            
            // Test various invalid email formats
            val invalidEmails = listOf(
                "plainaddress",
                "@missingdomain.com",
                "missing@.com",
                "missing@domain",
                "spaces @domain.com",
                "domain@.com"
            )
            
            for (email in invalidEmails) {
                val result = authManager.signIn(email, "password123")
                assertTrue(result.isError, "Email '$email' should be invalid")
            }
            
            // Test valid email formats
            val validEmails = listOf(
                "user@domain.com",
                "user.name@domain.com",
                "user+tag@domain.co.uk",
                "123@domain.org"
            )
            
            for (email in validEmails) {
                val result = authManager.signIn(email, "password123")
                assertTrue(result.isSuccess, "Email '$email' should be valid")
            }
        }
    }
    
    @Test
    fun `AndroidAuthManager should maintain consistent user IDs for same email`() {
        runBlocking {
            val authManager = AndroidAuthManager()
            val email = "consistent@example.com"
            
            val result1 = authManager.signUp(email, "password123", "User 1")
            val result2 = authManager.signUp(email, "password123", "User 2")
            
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            
            val user1 = result1.getOrNull()
            val user2 = result2.getOrNull()
            
            assertNotNull(user1)
            assertNotNull(user2)
            assertEquals(user1.id, user2.id, "Same email should generate same user ID")
        }
    }
    
    @Test
    fun `AndroidAuthManager should handle concurrent operations safely`() {
        runBlocking {
            val authManager = AndroidAuthManager()
            
            // Test multiple concurrent sign-in attempts
            val results = (1..10).map { index ->
                authManager.signIn("user$index@example.com", "password123")
            }
            
            // All operations should complete successfully
            results.forEach { result ->
                assertTrue(result.isSuccess, "Concurrent operation should succeed")
            }
        }
    }
}