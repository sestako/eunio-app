package com.eunio.healthapp.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Simple test to verify Koin initialization works without complex dependencies
 */
class SimpleKoinTest {
    
    @Test
    fun `test Koin can start with basic modules`() {
        var initializationSuccessful = false
        
        try {
            // Clean up any existing Koin instance
            try { stopKoin() } catch (e: Exception) { /* ignore */ }
            
            // Try to start Koin with just the shared module
            startKoin {
                modules(sharedModule)
            }
            
            initializationSuccessful = true
            println("✅ Koin initialization successful")
            
        } catch (e: Exception) {
            println("❌ Koin initialization failed: ${e.message}")
            e.printStackTrace()
        } finally {
            // Clean up
            try { stopKoin() } catch (e: Exception) { /* ignore */ }
        }
        
        assertTrue(initializationSuccessful, "Koin should initialize successfully")
    }
}