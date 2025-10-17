package com.eunio.healthapp.testutil

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for tests that require Koin dependency injection support.
 * Provides proper setup and teardown of Koin context with test modules.
 */
abstract class BaseKoinTest {
    
    /**
     * Override this to provide custom test modules.
     * Default implementation uses the standard test module.
     */
    protected open fun getTestModules(): List<Module> = listOf(testModule)
    
    /**
     * Override this for custom setup after Koin initialization
     */
    protected open fun setup() {}
    
    /**
     * Override this for custom teardown before Koin cleanup
     */
    protected open fun teardown() {}
    
    @BeforeTest
    fun baseSetup() {
        // Stop any existing Koin instance to ensure clean state
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if no Koin instance was running
        }
        
        // Start Koin with test modules
        startKoin {
            modules(getTestModules())
        }
        
        // Call subclass setup
        setup()
    }
    
    @AfterTest
    fun baseTeardown() {
        // Call subclass teardown first
        teardown()
        
        // Stop Koin to clean up resources
        try {
            stopKoin()
        } catch (e: Exception) {
            // Log but don't fail the test if cleanup fails
            println("Warning: Failed to stop Koin during test cleanup: ${e.message}")
        }
    }
}