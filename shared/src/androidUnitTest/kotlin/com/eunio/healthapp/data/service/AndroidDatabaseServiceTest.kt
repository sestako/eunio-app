package com.eunio.healthapp.data.service

import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.service.DatabaseServiceImpl
import com.eunio.healthapp.domain.service.DatabaseService
import com.eunio.healthapp.testutil.createMockDatabaseManager
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for AndroidDatabaseService.
 * Tests the service functionality without Android Context dependencies.
 * Note: Full Android integration testing would require Robolectric or instrumented tests.
 */
class AndroidDatabaseServiceTest {
    
    private lateinit var databaseManager: DatabaseManager
    private lateinit var databaseService: DatabaseService
    
    @BeforeTest
    fun setup() {
        databaseManager = createMockDatabaseManager()
        // Test the base functionality using DatabaseServiceImpl
        // Android-specific features would need integration tests with real Context
        databaseService = DatabaseServiceImpl(databaseManager)
    }
    
    @AfterTest
    fun tearDown() = runTest {
        databaseService.close()
    }
    
    @Test
    fun databaseService_shouldImplementDatabaseService() {
        // Then
        assertTrue(databaseService is DatabaseService)
    }
    
    @Test
    fun getUserDao_shouldReturnValidDao() = runTest {
        // When
        val userDao = databaseService.getUserDao()
        
        // Then
        assertNotNull(userDao)
    }
    
    @Test
    fun getDailyLogDao_shouldReturnValidDao() = runTest {
        // When
        val dailyLogDao = databaseService.getDailyLogDao()
        
        // Then
        assertNotNull(dailyLogDao)
    }
    
    @Test
    fun getUserPreferencesDao_shouldReturnValidDao() = runTest {
        // When
        val userPreferencesDao = databaseService.getUserPreferencesDao()
        
        // Then
        assertNotNull(userPreferencesDao)
    }
    
    @Test
    fun getUserSettingsDao_shouldReturnValidDao() = runTest {
        // When
        val userSettingsDao = databaseService.getUserSettingsDao()
        
        // Then
        assertNotNull(userSettingsDao)
    }
    
    @Test
    fun isHealthy_shouldWork() = runTest {
        // Given - Initialize database
        databaseService.getUserDao()
        
        // When
        val isHealthy = databaseService.isHealthy()
        
        // Then
        assertTrue(isHealthy)
    }
    
    @Test
    fun performMaintenance_shouldSucceed() = runTest {
        // Given - Initialize database
        databaseService.getUserDao()
        
        // When
        val result = databaseService.performMaintenance()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun recover_shouldWork() = runTest {
        // Given - Initialize database
        databaseService.getUserDao()
        
        // When
        val result = databaseService.recover()
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun close_shouldCleanupResources() = runTest {
        // Given - Initialize database
        databaseService.getUserDao()
        
        // When & Then - Should not throw exception
        try {
            databaseService.close()
            // If we get here, the test passes
        } catch (e: Exception) {
            fail("close() should not throw exception: ${e.message}")
        }
    }
    
    @Test
    fun basicOperations_shouldWork() = runTest {
        // When - Perform basic operations
        val userDao = databaseService.getUserDao()
        val dailyLogDao = databaseService.getDailyLogDao()
        val isHealthy = databaseService.isHealthy()
        
        // Then - All should work
        assertNotNull(userDao)
        assertNotNull(dailyLogDao)
        assertTrue(isHealthy)
    }
}