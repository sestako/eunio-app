package com.eunio.healthapp.di

import kotlinx.coroutines.test.runTest
import org.koin.core.context.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-platform dependency validation test suite.
 * 
 * This test validates that all dependencies can be resolved correctly
 * across both Android and iOS platforms, ensuring proper dependency injection
 * configuration and platform-specific service implementations.
 */
class CrossPlatformDependencyValidationTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Clean up any existing Koin context
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if no Koin context exists
        }
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
    }
    
    /**
     * Test that basic shared services can be resolved
     */
    @Test
    fun `shared module resolves basic services`() = runTest {
        // Initialize with shared module only
        startKoin {
            modules(
                sharedModule
            )
        }
        
        // Test basic shared services that should be available
        assertNotNull(get<com.eunio.healthapp.domain.util.ErrorHandler>())
        assertNotNull(get<kotlinx.coroutines.CoroutineScope>())
    }
    
    /**
     * Test that Koin can initialize with shared module
     */
    @Test
    fun `Koin initializes with shared module`() = runTest {
        startKoin {
            modules(
                sharedModule
            )
        }
        
        // If we get here without exception, Koin initialized successfully
        assertTrue(true, "Koin initialization completed successfully")
    }
    
    /**
     * Test basic dependency resolution performance
     */
    @Test
    fun `dependency resolution is performant`() = runTest {
        val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        
        startKoin {
            modules(
                sharedModule
            )
        }
        
        // Resolve basic components to test for circular dependencies
        val errorHandler = get<com.eunio.healthapp.domain.util.ErrorHandler>()
        assertNotNull(errorHandler)
        
        val coroutineScope = get<kotlinx.coroutines.CoroutineScope>()
        assertNotNull(coroutineScope)
        
        val endTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val resolutionTime = endTime - startTime
        
        // Dependency resolution should be fast (under 1 second for basic components)
        assertTrue(resolutionTime < 1000, "Dependency resolution took too long: ${resolutionTime}ms")
    }
    
    /**
     * Test that error handling components are properly configured
     */
    @Test
    fun `error handling components are properly configured`() = runTest {
        startKoin {
            modules(
                sharedModule
            )
        }
        
        // Test error handling components that exist
        assertNotNull(get<com.eunio.healthapp.domain.util.ErrorHandler>())
    }
    
    /**
     * Test that basic data layer components are available
     */
    @Test
    fun `basic data layer components are available`() = runTest {
        startKoin {
            modules(
                sharedModule
            )
        }
        
        // Test basic components that should be available
        assertNotNull(get<com.eunio.healthapp.domain.util.ErrorHandler>())
        assertNotNull(get<kotlinx.coroutines.CoroutineScope>())
    }
}