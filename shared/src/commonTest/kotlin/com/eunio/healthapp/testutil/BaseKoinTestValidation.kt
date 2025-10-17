package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.repository.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test to validate that BaseKoinTest and test modules work correctly
 */
class BaseKoinTestValidation : BaseKoinTest(), KoinComponent {
    
    // Use Koin component injection instead of GlobalContext
    private val userRepository: UserRepository by inject()
    private val logRepository: LogRepository by inject()
    private val mockServices: MockServices by inject()
    
    @Test
    fun testKoinSetupAndDependencyInjection() {
        // Verify dependencies are not null
        assertNotNull(userRepository, "UserRepository should be injected")
        assertNotNull(logRepository, "LogRepository should be injected")
        assertNotNull(mockServices, "MockServices should be injected")
        
        // Verify they are mock implementations
        assertTrue(userRepository is MockUserRepository, "Should be mock implementation")
        assertTrue(logRepository is MockLogRepository, "Should be mock implementation")
    }
    
    @Test
    fun testMockServicesWork() {
        // Test that mock services are properly initialized
        assertNotNull(mockServices.userRepository, "User repository should be available")
        assertNotNull(mockServices.logRepository, "Log repository should be available")
        assertNotNull(mockServices.authService, "Auth service should be available")
        
        // Test that we can reset mock services
        mockServices.reset()
        // If reset doesn't throw an exception, it's working
    }
    
    @Test
    fun testKoinLifecycleManagement() {
        // Test that we can get the same instance multiple times (singleton behavior)
        val mockServices1: MockServices by inject()
        val mockServices2: MockServices by inject()
        
        assertTrue(mockServices1 === mockServices2, "Should return the same singleton instance")
    }
}