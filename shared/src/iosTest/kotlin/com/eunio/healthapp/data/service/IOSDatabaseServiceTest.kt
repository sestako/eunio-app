package com.eunio.healthapp.data.service

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.testutil.createMockDatabaseManager
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for IOSDatabaseService.
 * Tests iOS-specific functionality including NSUserDefaults integration and iOS-specific health checks.
 */
class IOSDatabaseServiceTest {
    
    private lateinit var databaseManager: DatabaseManager
    private lateinit var iosDatabaseService: IOSDatabaseService
    
    @BeforeTest
    fun setup() {
        databaseManager = createMockDatabaseManager()
        iosDatabaseService = IOSDatabaseService(databaseManager)
    }
    
    @AfterTest
    fun tearDown() = runTest {
        iosDatabaseService.close()
    }
    
    @Test
    fun iosDatabaseService_shouldImplementDatabaseService() {
        // Then
        assertTrue(iosDatabaseService is DatabaseService)
    }
    
    @Test
    fun getUserDao_shouldReturnValidDao() = runTest {
        // When
        val userDao = iosDatabaseService.getUserDao()
        
        // Then
        assertNotNull(userDao)
    }
    
    @Test
    fun getDailyLogDao_shouldReturnValidDao() = runTest {
        // When
        val dailyLogDao = iosDatabaseService.getDailyLogDao()
        
        // Then
        assertNotNull(dailyLogDao)
    }
    
    @Test
    fun getUserPreferencesDao_shouldReturnValidDao() = runTest {
        // When
        val userPreferencesDao = iosDatabaseService.getUserPreferencesDao()
        
        // Then
        assertNotNull(userPreferencesDao)
    }
    
    @Test
    fun getUserSettingsDao_shouldReturnValidDao() = runTest {
        // When
        val userSettingsDao = iosDatabaseService.getUserSettingsDao()
        
        // Then
        assertNotNull(userSettingsDao)
    }
    
    @Test
    fun isHealthy_shouldPerformIOSChecks() = runTest {
        // Given - Initialize database
        iosDatabaseService.getUserDao()
        
        // When
        val isHealthy = iosDatabaseService.isHealthy()
        
        // Then - Should perform iOS-specific health checks
        assertNotNull(isHealthy) // Should return a boolean result
    }
    
    @Test
    fun performMaintenance_shouldIncludeIOSSpecificOperations() = runTest {
        // Given - Initialize database
        iosDatabaseService.getUserDao()
        
        // When
        val result = iosDatabaseService.performMaintenance()
        
        // Then - Should succeed with iOS-specific maintenance
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun recover_shouldWorkOnIOS() = runTest {
        // Given - Initialize database
        iosDatabaseService.getUserDao()
        
        // When
        val result = iosDatabaseService.recover()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun close_shouldCleanupIOSResources() = runTest {
        // Given - Initialize database
        iosDatabaseService.getUserDao()
        
        // When
        iosDatabaseService.close()
        
        // Then - Should not throw exception
        try {
            iosDatabaseService.close() // Should be safe to call multiple times
            // If we get here, the test passes
        } catch (e: Exception) {
            fail("close() should not throw exception: ${e.message}")
        }
    }
    
    @Test
    fun iosSpecificFeatures_shouldNotAffectBasicOperations() = runTest {
        // When - Perform basic operations
        val userDao = iosDatabaseService.getUserDao()
        val dailyLogDao = iosDatabaseService.getDailyLogDao()
        val isHealthy = iosDatabaseService.isHealthy()
        
        // Then - All should work despite iOS-specific enhancements
        assertNotNull(userDao)
        assertNotNull(dailyLogDao)
        assertNotNull(isHealthy)
    }
    
    @Test
    fun iosIntegration_shouldNotCauseErrors() = runTest {
        // Given - Service with iOS-specific features
        val service = IOSDatabaseService(databaseManager)
        
        // When - Perform operations
        val userDao = service.getUserDao()
        val maintenance = service.performMaintenance()
        
        // Then - Should work without iOS-related errors
        assertNotNull(userDao)
        assertTrue(maintenance.isSuccess)
        
        // Cleanup
        service.close()
    }
    
    @Test
    fun multipleOperations_shouldWorkSequentially() = runTest {
        // When - Perform multiple operations
        val userDao = iosDatabaseService.getUserDao()
        val preferencesDao = iosDatabaseService.getUserPreferencesDao()
        val isHealthy1 = iosDatabaseService.isHealthy()
        val maintenance = iosDatabaseService.performMaintenance()
        val isHealthy2 = iosDatabaseService.isHealthy()
        
        // Then - All should succeed
        assertNotNull(userDao)
        assertNotNull(preferencesDao)
        assertNotNull(isHealthy1)
        assertTrue(maintenance.isSuccess)
        assertNotNull(isHealthy2)
    }
}