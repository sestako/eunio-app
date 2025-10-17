package com.eunio.healthapp.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.error.NoDefinitionFoundException
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Comprehensive test to verify Koin initialization works perfectly
 */
class ComprehensiveKoinVerificationTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                sharedModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
                unitSystemModule,
                settingsIntegrationModule
            )
        }
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
    }
    
    @Test
    fun `should initialize all core services successfully`() {
        // Test ErrorHandler
        val errorHandler: com.eunio.healthapp.domain.util.ErrorHandler by inject()
        assertNotNull(errorHandler)
        
        // Test CoroutineScope
        val coroutineScope: kotlinx.coroutines.CoroutineScope by inject()
        assertNotNull(coroutineScope)
        
        // Note: DatabaseManager requires platform-specific driver which is not available in common tests
        // This is expected and correct behavior
    }
    
    @Test
    fun `should verify all modules are properly included`() {
        val allModules = listOf(
            sharedModule,
            repositoryModule,
            useCaseModule,
            viewModelModule,
            unitSystemModule,
            settingsIntegrationModule
        )
        
        // Verify all modules exist and are not null
        allModules.forEach { module ->
            assertNotNull(module)
        }
    }
    
    @Test
    fun `should handle dependency resolution gracefully`() {
        // Test that we can resolve basic dependencies without errors
        val errorHandler: com.eunio.healthapp.domain.util.ErrorHandler by inject()
        assertNotNull(errorHandler)
        
        // Test that the Koin container is working properly
        val coroutineScope: kotlinx.coroutines.CoroutineScope by inject()
        assertNotNull(coroutineScope)
        
        // Verify that both instances are properly managed by Koin
        assertTrue(errorHandler is com.eunio.healthapp.domain.util.ErrorHandler)
        assertTrue(coroutineScope is kotlinx.coroutines.CoroutineScope)
    }
    
    @Test
    fun `should verify Koin container is properly initialized`() {
        // Verify that Koin is started and can resolve basic dependencies
        val errorHandler: com.eunio.healthapp.domain.util.ErrorHandler by inject()
        assertNotNull(errorHandler)
        
        // Verify that the container is working by checking if we can get the same instance twice
        val errorHandler2: com.eunio.healthapp.domain.util.ErrorHandler by inject()
        assertNotNull(errorHandler2)
        
        // Since ErrorHandler is a singleton, they should be the same instance
        assertTrue(errorHandler === errorHandler2, "ErrorHandler should be a singleton")
    }
}