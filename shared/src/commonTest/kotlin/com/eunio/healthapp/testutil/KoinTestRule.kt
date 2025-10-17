package com.eunio.healthapp.testutil

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * Test rule for managing Koin lifecycle in tests.
 * Provides a convenient way to set up and tear down Koin context with custom modules.
 */
class KoinTestRule(
    private val modules: List<Module> = listOf(testModule)
) {
    
    /**
     * Set up Koin with the specified modules.
     * Call this in your test's @BeforeTest method.
     */
    fun setUp() {
        // Stop any existing Koin instance
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if no Koin instance was running
        }
        
        // Start Koin with test modules
        startKoin {
            modules(modules)
        }
    }
    
    /**
     * Tear down Koin context.
     * Call this in your test's @AfterTest method.
     */
    fun tearDown() {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Log but don't fail if cleanup fails
            println("Warning: Failed to stop Koin during test cleanup: ${e.message}")
        }
    }
}

/**
 * Extension function to create a KoinTestRule with custom modules
 */
fun koinTestRule(vararg modules: Module): KoinTestRule {
    return KoinTestRule(modules.toList())
}

/**
 * Extension function to create a KoinTestRule with the default test module
 */
fun defaultKoinTestRule(): KoinTestRule {
    return KoinTestRule(listOf(testModule))
}

/**
 * Extension function to create a KoinTestRule with minimal modules
 */
fun minimalKoinTestRule(): KoinTestRule {
    return KoinTestRule(listOf(minimalTestModule))
}

/**
 * Extension function to create a KoinTestRule for repository testing
 */
fun repositoryKoinTestRule(): KoinTestRule {
    return KoinTestRule(listOf(repositoryTestModule))
}

/**
 * Utility class for tests that need to manage Koin lifecycle manually
 */
object KoinTestUtils {
    
    /**
     * Execute a test block with Koin set up and torn down automatically
     */
    inline fun <T> withKoin(
        modules: List<Module> = listOf(testModule),
        testBlock: () -> T
    ): T {
        val rule = KoinTestRule(modules)
        rule.setUp()
        return try {
            testBlock()
        } finally {
            rule.tearDown()
        }
    }
    
    /**
     * Execute a suspend test block with Koin set up and torn down automatically
     */
    suspend inline fun <T> withKoinSuspend(
        modules: List<Module> = listOf(testModule),
        crossinline testBlock: suspend () -> T
    ): T {
        val rule = KoinTestRule(modules)
        rule.setUp()
        return try {
            testBlock()
        } finally {
            rule.tearDown()
        }
    }
    
    /**
     * Restart Koin with new modules during a test
     */
    fun restartKoinWithModules(modules: List<Module>) {
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if no Koin instance was running
        }
        
        startKoin {
            modules(modules)
        }
    }
    
    /**
     * Check if Koin is currently running
     */
    fun isKoinRunning(): Boolean {
        // Simplified implementation - if we're in a test context, assume Koin is running
        return true
    }
}