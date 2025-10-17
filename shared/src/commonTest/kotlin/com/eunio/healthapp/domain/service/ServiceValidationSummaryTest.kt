package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.model.notification.*
import com.eunio.healthapp.testutil.BaseKoinTest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.*

/**
 * Summary test for service operation validation.
 * Tests core service functionality through repository layer and model validation.
 */
class ServiceValidationSummaryTest : BaseKoinTest(), KoinComponent {
    
    private val userRepository: UserRepository by inject()
    
    @Test
    fun `Core service operations should work through repository layer`() = runTest {
        // Test user management
        val email = "service-test-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}@example.com"
        val password = "testPassword123"
        val name = "Service Test User"
        
        // Create user
        val createResult = userRepository.createUser(email, password, name)
        assertTrue(createResult.isSuccess, "User creation should succeed")
        
        val user = createResult.getOrNull()
        assertNotNull(user, "User should be created")
        assertEquals(email, user.email)
        assertEquals(name, user.name)
        
        // Get current user
        val currentUserResult = userRepository.getCurrentUser()
        assertTrue(currentUserResult.isSuccess, "Get current user should succeed")
        assertEquals(user.id, currentUserResult.getOrNull()?.id, "Current user should match created user")
        
        // Sign out and sign in
        userRepository.signOutUser()
        val signOutResult = userRepository.getCurrentUser()
        assertNull(signOutResult.getOrNull(), "Should be signed out")
        
        val signInResult = userRepository.signInUser(email, password)
        assertTrue(signInResult.isSuccess, "Sign in should succeed")
        assertEquals(email, signInResult.getOrNull()?.email, "Signed in user should match")
    }
    
    @Test
    fun `Service models should validate correctly`() = runTest {
        // Test UserSettings validation
        val validSettings = UserSettings.createDefault("test-user-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}")
        assertTrue(validSettings.isValid(), "Default settings should be valid")
        
        // Test UnitPreferences validation
        val unitPrefs = UnitPreferences.default()
        assertTrue(unitPrefs.isValid(), "Default unit preferences should be valid")
        
        // Test CyclePreferences validation
        val validCycle = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        assertTrue(validCycle.isValid(), "Valid cycle preferences should pass validation")
        
        val invalidCycle = CyclePreferences(
            averageCycleLength = -5, // Invalid
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        assertFalse(invalidCycle.isValid(), "Invalid cycle preferences should fail validation")
        
        // Test NotificationSetting validation
        val validNotification = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 0),
            daysInAdvance = 1
        )
        assertTrue(validNotification.isValid(), "Valid notification setting should pass validation")
        
        val invalidNotification = NotificationSetting(
            enabled = true,
            time = null, // Invalid - enabled but no time
            daysInAdvance = 1
        )
        assertFalse(invalidNotification.isValid(), "Invalid notification setting should fail validation")
    }
    
    @Test
    fun `Service error handling should be graceful`() = runTest {
        // Test input validation
        val emptyEmailResult = userRepository.createUser("", "password", "name")
        assertTrue(emptyEmailResult.isError, "Empty email should be rejected")
        
        val emptyPasswordResult = userRepository.createUser("test@example.com", "", "name")
        assertTrue(emptyPasswordResult.isError, "Empty password should be rejected")
        
        val emptyNameResult = userRepository.createUser("test@example.com", "password", "")
        assertTrue(emptyNameResult.isError, "Empty name should be rejected")
        
        // Test authentication with invalid credentials
        val invalidSignInResult = userRepository.signInUser("nonexistent@test.com", "wrongpassword")
        assertTrue(invalidSignInResult.isError, "Invalid credentials should be rejected")
        
        // Verify error messages are meaningful
        val error = invalidSignInResult.errorOrNull()
        assertNotNull(error, "Error should provide error details")
        assertTrue(error.message?.isNotBlank() == true, "Error should have meaningful message")
    }
    
    @Test
    fun `Service performance should be acceptable`() = runTest {
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        // Perform multiple operations
        repeat(5) {
            userRepository.getCurrentUser()
        }
        
        val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (1 second for 5 operations)
        assertTrue(duration < 1000, "Service operations should complete quickly, took ${duration}ms")
    }
    
    @Test
    fun `NotificationType enum should be properly configured`() = runTest {
        val allTypes = NotificationType.values()
        assertTrue(allTypes.isNotEmpty(), "Should have notification types")
        
        // Test each type has valid properties
        allTypes.forEach { type ->
            assertTrue(type.id.isNotBlank(), "Type ${type.name} should have valid ID")
            assertTrue(type.title.isNotBlank(), "Type ${type.name} should have valid title")
            assertTrue(type.body.isNotBlank(), "Type ${type.name} should have valid body")
            assertTrue(type.category.isNotBlank(), "Type ${type.name} should have valid category")
        }
        
        // Test lookup functionality
        val dailyLogging = NotificationType.fromId("daily_logging_reminder")
        assertEquals(NotificationType.DAILY_LOGGING, dailyLogging, "Should find type by ID")
        
        val nonExistent = NotificationType.fromId("non_existent")
        assertNull(nonExistent, "Should return null for non-existent ID")
        
        // Test category filtering
        val cycleTypes = NotificationType.getByCategory("cycle_tracking")
        assertTrue(cycleTypes.isNotEmpty(), "Should have cycle tracking notifications")
        assertTrue(cycleTypes.contains(NotificationType.PERIOD_PREDICTION), "Should include period prediction")
        assertTrue(cycleTypes.contains(NotificationType.OVULATION_ALERT), "Should include ovulation alert")
    }
    
    @Test
    fun `Settings models should support different configurations`() = runTest {
        val userId = "config-test-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
        
        // Test default settings
        val defaultSettings = UserSettings.createDefault(userId)
        assertTrue(defaultSettings.isValid(), "Default settings should be valid")
        assertFalse(defaultSettings.hasCustomizations(), "Default settings should not have customizations")
        
        // Test settings with defaults
        val withDefaults = UserSettings.createWithDefaults(userId)
        assertTrue(withDefaults.isValid(), "Settings with defaults should be valid")
        assertTrue(withDefaults.notificationPreferences.hasEnabledNotifications(), "Should have enabled notifications")
        
        // Test privacy-focused settings
        val privacyFocused = UserSettings.createPrivacyFocused(userId)
        assertTrue(privacyFocused.isValid(), "Privacy-focused settings should be valid")
        
        // Test locale-based unit preferences
        val usPrefs = UnitPreferences.fromLocale("en_US")
        assertTrue(usPrefs.isValid(), "US locale preferences should be valid")
        
        val metricPrefs = UnitPreferences.fromLocale("en_GB")
        assertTrue(metricPrefs.isValid(), "UK locale preferences should be valid")
    }
}