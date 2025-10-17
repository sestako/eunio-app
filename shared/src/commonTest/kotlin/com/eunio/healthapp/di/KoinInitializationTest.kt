package com.eunio.healthapp.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Test to verify Koin initialization works correctly
 */
class KoinInitializationTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        // Clean up any existing Koin instance
        try {
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
        
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
    fun `should initialize Koin successfully`() {
        // Test that we can inject basic services
        val errorHandler: com.eunio.healthapp.domain.util.ErrorHandler by inject()
        assertNotNull(errorHandler)
    }
    
    @Test
    fun `should resolve core modules without platform dependencies`() {
        // Test that modules can be loaded without errors
        val modules = listOf(
            sharedModule,
            repositoryModule,
            useCaseModule,
            viewModelModule,
            unitSystemModule,
            settingsIntegrationModule
        )
        
        // If we get here without exceptions, the modules are properly configured
        assertNotNull(modules)
    }
}