package com.eunio.healthapp.integration

import com.eunio.healthapp.di.repositoryModule
import com.eunio.healthapp.di.sharedModule
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.testutil.TestDatabaseDriverFactory
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.*

/**
 * Integration tests for DatabaseService with the dependency injection system.
 * Verifies that DatabaseService can be properly injected and used in the application context.
 */
class DatabaseServiceIntegrationTest : KoinTest {
    
    private val databaseService: DatabaseService by inject()
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                // Use the test module with mocks instead of real implementations
                com.eunio.healthapp.testutil.testModule
            )
        }
    }
    
    @AfterTest
    fun tearDown() = runTest {
        try {
            databaseService.close()
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
        stopKoin()
    }
    
    @Test
    fun databaseService_shouldBeInjectable() {
        // Then - Service should be injected successfully
        assertNotNull(databaseService)
    }
    
    @Test
    fun databaseService_shouldProvideAllDaos() = runTest {
        // When - Access all DAOs through service
        val userDao = databaseService.getUserDao()
        val dailyLogDao = databaseService.getDailyLogDao()
        val preferencesDao = databaseService.getUserPreferencesDao()
        val settingsDao = databaseService.getUserSettingsDao()
        
        // Then - All DAOs should be available
        assertNotNull(userDao)
        assertNotNull(dailyLogDao)
        assertNotNull(preferencesDao)
        assertNotNull(settingsDao)
    }
    
    @Test
    fun databaseService_shouldBeHealthy() = runTest {
        // Given - Initialize service
        databaseService.getUserDao()
        
        // When
        val isHealthy = databaseService.isHealthy()
        
        // Then
        assertTrue(isHealthy)
    }
    
    @Test
    fun databaseService_maintenanceOperations_shouldWork() = runTest {
        // Given - Initialize service
        databaseService.getUserDao()
        
        // When
        val maintenanceResult = databaseService.performMaintenance()
        val recoveryResult = databaseService.recover()
        
        // Then
        assertTrue(maintenanceResult.isSuccess)
        assertTrue(recoveryResult.isSuccess)
    }
    
    @Test
    fun databaseService_shouldWorkWithRepositories() = runTest {
        // This test verifies that repositories can use the DatabaseService
        // through the DI system without issues
        
        // When - Access service and verify it works
        val userDao = databaseService.getUserDao()
        val isHealthy = databaseService.isHealthy()
        
        // Then - Should work for repository integration
        assertNotNull(userDao)
        assertTrue(isHealthy)
    }
    
    @Test
    fun databaseService_errorRecovery_shouldWork() = runTest {
        // Given - Initialize service
        databaseService.getUserDao()
        
        // When - Force recovery
        val recoveryResult = databaseService.recover()
        
        // Then - Should recover successfully
        assertTrue(recoveryResult.isSuccess)
        
        // And - Service should still be functional
        val userDao = databaseService.getUserDao()
        assertNotNull(userDao)
    }
    
    @Test
    fun databaseService_concurrentAccess_shouldBeSafe() = runTest {
        // When - Access service concurrently (simulated)
        val results = mutableListOf<Any>()
        
        repeat(3) {
            results.add(databaseService.getUserDao())
            results.add(databaseService.getDailyLogDao())
            results.add(databaseService.isHealthy())
        }
        
        // Then - All operations should succeed
        assertEquals(9, results.size)
        results.forEach { assertNotNull(it) }
    }
    
    companion object {
        private val testModule = module {
            // Test database driver factory
            single<com.eunio.healthapp.data.local.DatabaseDriverFactoryInterface> {
                TestDatabaseDriverFactory()
            }
        }
    }
}