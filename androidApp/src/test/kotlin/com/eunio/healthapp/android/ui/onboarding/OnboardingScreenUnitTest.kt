package com.eunio.healthapp.android.ui.onboarding

import com.eunio.healthapp.domain.model.HealthGoal
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for onboarding screen logic.
 */
class OnboardingScreenUnitTest {
    
    @Test
    fun healthGoal_allValuesAvailable() {
        val goals = HealthGoal.values()
        
        assertEquals(4, goals.size)
        assertTrue(goals.contains(HealthGoal.CONCEPTION))
        assertTrue(goals.contains(HealthGoal.CONTRACEPTION))
        assertTrue(goals.contains(HealthGoal.CYCLE_TRACKING))
        assertTrue(goals.contains(HealthGoal.GENERAL_HEALTH))
    }
    
    @Test
    fun validateEmail_correctFormat() {
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org"
        )
        
        validEmails.forEach { email ->
            assertTrue("Email $email should be valid", isValidEmail(email))
        }
    }
    
    @Test
    fun validateEmail_incorrectFormat() {
        val invalidEmails = listOf(
            "",
            "invalid",
            "@example.com",
            "test@",
            "test.example.com"
        )
        
        invalidEmails.forEach { email ->
            assertFalse("Email $email should be invalid", isValidEmail(email))
        }
    }
    
    @Test
    fun validatePassword_correctLength() {
        val validPasswords = listOf(
            "123456",
            "password123",
            "verylongpassword"
        )
        
        validPasswords.forEach { password ->
            assertTrue("Password $password should be valid", isValidPassword(password))
        }
    }
    
    @Test
    fun validatePassword_incorrectLength() {
        val invalidPasswords = listOf(
            "",
            "12345",
            "abc"
        )
        
        invalidPasswords.forEach { password ->
            assertFalse("Password $password should be invalid", isValidPassword(password))
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val atIndex = email.indexOf("@")
        if (atIndex <= 0 || atIndex == email.length - 1) return false
        val dotIndex = email.lastIndexOf(".")
        return dotIndex > atIndex && dotIndex < email.length - 1
    }
    
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}