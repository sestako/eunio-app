package com.eunio.healthapp.domain.model

import com.eunio.healthapp.testutil.TestDataFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {
    
    @Test
    fun `user creation with valid data should succeed`() {
        val user = TestDataFactory.createTestUser("user123")
        
        assertEquals("user123", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.name)
        assertTrue(user.onboardingComplete)
        assertEquals(HealthGoal.CYCLE_TRACKING, user.primaryGoal)
        assertEquals(UnitSystem.METRIC, user.unitSystem)
    }
    
    @Test
    fun `user with incomplete onboarding should have correct state`() {
        val user = TestDataFactory.createTestUser("user123").copy(
            onboardingComplete = false
        )
        
        assertFalse(user.onboardingComplete)
    }
    
    @Test
    fun `user update should preserve id and creation time`() {
        val originalUser = TestDataFactory.createTestUser("user123")
        val updatedUser = originalUser.copy(
            name = "Updated Name",
            updatedAt = Clock.System.now()
        )
        
        assertEquals(originalUser.id, updatedUser.id)
        assertEquals(originalUser.createdAt, updatedUser.createdAt)
        assertEquals("Updated Name", updatedUser.name)
    }
    
    @Test
    fun `user with different health goals should be valid`() {
        val conceptionUser = TestDataFactory.createTestUser("user1").copy(
            primaryGoal = HealthGoal.CONCEPTION
        )
        val contraceptionUser = TestDataFactory.createTestUser("user2").copy(
            primaryGoal = HealthGoal.CONTRACEPTION
        )
        
        assertEquals(HealthGoal.CONCEPTION, conceptionUser.primaryGoal)
        assertEquals(HealthGoal.CONTRACEPTION, contraceptionUser.primaryGoal)
    }
    
    @Test
    fun `user with different unit systems should be valid`() {
        val metricUser = TestDataFactory.createTestUser("user1").copy(
            unitSystem = UnitSystem.METRIC
        )
        val imperialUser = TestDataFactory.createTestUser("user2").copy(
            unitSystem = UnitSystem.IMPERIAL
        )
        
        assertEquals(UnitSystem.METRIC, metricUser.unitSystem)
        assertEquals(UnitSystem.IMPERIAL, imperialUser.unitSystem)
    }
}