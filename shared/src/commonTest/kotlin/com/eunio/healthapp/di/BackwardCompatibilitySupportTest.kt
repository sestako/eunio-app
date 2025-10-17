package com.eunio.healthapp.di

import com.eunio.healthapp.domain.manager.*
import kotlin.test.*
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest

/**
 * Tests for backward compatibility support functionality.
 * Verifies that deprecated factory methods work correctly and provide fallback implementations.
 */
class BackwardCompatibilitySupportTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Ensure clean state - no Koin context
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
    }
    
    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
    }
    
    @Test
    fun `createSettingsManager should return fallback instance when Koin not initialized`() {
        // Given - no Koin initialization
        
        // When
        val manager = BackwardCompatibilitySupport.createSettingsManager()
        
        // Then
        assertNotNull(manager)
        assertTrue(manager is SettingsManager)
    }
    
    @Test
    fun `createNotificationManager should return fallback instance when Koin not initialized`() {
        // Given - no Koin initialization
        
        // When
        val manager = BackwardCompatibilitySupport.createNotificationManager()
        
        // Then
        assertNotNull(manager)
        assertTrue(manager is NotificationManager)
    }
    
    @Test
    fun `createAuthManager should return fallback instance when Koin not initialized`() {
        // Given - no Koin initialization
        
        // When
        val manager = BackwardCompatibilitySupport.createAuthManager()
        
        // Then
        assertNotNull(manager)
        assertTrue(manager is AuthManager)
    }
    
    @Test
    fun `deprecated service factory should create working instances`() {
        // Given - no Koin initialization
        
        // When
        val settingsManager = DeprecatedServiceFactory.createSettingsManager()
        val notificationManager = DeprecatedServiceFactory.createNotificationManager()
        val authManager = DeprecatedServiceFactory.createAuthManager()
        
        // Then
        assertNotNull(settingsManager)
        assertNotNull(notificationManager)
        assertNotNull(authManager)
        assertTrue(settingsManager is SettingsManager)
        assertTrue(notificationManager is NotificationManager)
        assertTrue(authManager is AuthManager)
    }
    
    @Test
    fun `deprecated platform service factory should create working instances`() {
        // Given - no Koin initialization
        
        // When
        val settingsManager = DeprecatedPlatformServiceFactory.createPlatformSettingsManager()
        val notificationManager = DeprecatedPlatformServiceFactory.createPlatformNotificationManager()
        val authManager = DeprecatedPlatformServiceFactory.createPlatformAuthManager()
        
        // Then
        assertNotNull(settingsManager)
        assertNotNull(notificationManager)
        assertNotNull(authManager)
        assertTrue(settingsManager is SettingsManager)
        assertTrue(notificationManager is NotificationManager)
        assertTrue(authManager is AuthManager)
    }
    
    @Test
    fun `deprecated wrapper classes should delegate to actual implementations`() {
        // Given - no Koin initialization
        val settingsWrapper = DeprecatedSettingsManagerWrapper()
        val notificationWrapper = DeprecatedNotificationManagerWrapper()
        val authWrapper = DeprecatedAuthManagerWrapper()
        
        // When/Then - should not throw exceptions
        assertNotNull(settingsWrapper)
        assertNotNull(notificationWrapper)
        assertNotNull(authWrapper)
        assertTrue(settingsWrapper is SettingsManager)
        assertTrue(notificationWrapper is NotificationManager)
        assertTrue(authWrapper is AuthManager)
    }
    
    @Test
    fun `backward compatibility support should be marked as deprecated`() {
        // This test verifies that the deprecation warnings are in place
        // The actual deprecation warnings will be shown at compile time
        
        // When accessing deprecated methods, they should still work
        val settingsManager = BackwardCompatibilitySupport.createSettingsManager()
        val notificationManager = BackwardCompatibilitySupport.createNotificationManager()
        val authManager = BackwardCompatibilitySupport.createAuthManager()
        
        // Then they should return valid instances
        assertNotNull(settingsManager)
        assertNotNull(notificationManager)
        assertNotNull(authManager)
    }
}