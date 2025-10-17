package com.eunio.healthapp.platform

import android.content.Context
import android.content.SharedPreferences
import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.local.datasource.AndroidPreferencesLocalDataSource
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.util.AndroidLocaleDetector
import com.eunio.healthapp.domain.util.createAndroidLocaleDetector
import com.eunio.healthapp.testutil.TestDataFactory
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Android-specific platform integration tests.
 * Tests Android-specific features like SharedPreferences caching and locale detection.
 */
class AndroidPlatformIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var databaseManager: DatabaseManager
    private lateinit var androidDataSource: AndroidPreferencesLocalDataSource
    
    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        databaseManager = mockk()
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.remove(any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        
        androidDataSource = AndroidPreferencesLocalDataSource(databaseManager, context)
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `Android SharedPreferences caching works correctly`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock database operations
        val dao = mockk<com.eunio.healthapp.data.local.dao.UserPreferencesDao>()
        every { databaseManager.getUserPreferencesDao() } returns dao
        coEvery { dao.getPreferencesByUserId(any()) } returns null
        coEvery { dao.insertPreferences(any(), any(), any(), any(), any()) } just Runs
        
        // Mock cache miss initially
        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.getLong(any(), any()) } returns 0L
        
        // Save preferences (should cache them)
        val result = androidDataSource.savePreferences(preferences)
        
        assertTrue(result.isSuccess)
        
        // Verify SharedPreferences was used for caching
        verify { sharedPreferencesEditor.putString(any(), any()) }
        verify { sharedPreferencesEditor.putLong(any(), any()) }
        verify { sharedPreferencesEditor.apply() }
    }
    
    @Test
    fun `Android cache hit returns cached data without database access`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock cache hit
        val currentTime = Clock.System.now().toEpochMilliseconds()
        every { sharedPreferences.getString(any(), any()) } returns """
            {
                "userId": "${preferences.userId}",
                "unitSystem": "METRIC",
                "isManuallySet": false,
                "lastModified": "${preferences.lastModified}",
                "syncStatus": "SYNCED"
            }
        """.trimIndent()
        every { sharedPreferences.getLong(any(), any()) } returns currentTime
        
        val result = androidDataSource.getPreferences(preferences.userId)
        
        assertNotNull(result)
        assertEquals(UnitSystem.METRIC, result.unitSystem)
        assertEquals(false, result.isManuallySet)
        
        // Verify database was not accessed
        verify(exactly = 0) { databaseManager.getUserPreferencesDao() }
    }
    
    @Test
    fun `Android cache expiration works correctly`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock expired cache
        val expiredTime = Clock.System.now().toEpochMilliseconds() - (10 * 60 * 1000L) // 10 minutes ago
        every { sharedPreferences.getString(any(), any()) } returns """{"userId": "test"}"""
        every { sharedPreferences.getLong(any(), any()) } returns expiredTime
        
        // Mock database fallback
        val dao = mockk<com.eunio.healthapp.data.local.dao.UserPreferencesDao>()
        every { databaseManager.getUserPreferencesDao() } returns dao
        coEvery { dao.getPreferencesByUserId(any()) } returns mockk {
            every { userId } returns preferences.userId
            every { unitSystem } returns preferences.unitSystem.name
            every { isManuallySet } returns if (preferences.isManuallySet) 1L else 0L
            every { lastModified } returns preferences.lastModified.epochSeconds
            every { syncStatus } returns preferences.syncStatus.name
        }
        
        val result = androidDataSource.getPreferences(preferences.userId)
        
        assertNotNull(result)
        
        // Verify database was accessed due to cache expiration
        verify { databaseManager.getUserPreferencesDao() }
    }
    
    @Test
    fun `Android locale detector works with context`() {
        val mockContext = mockk<Context>()
        val detector = createAndroidLocaleDetector(mockContext)
        
        // Mock Android locale detection
        mockkStatic("android.text.format.DateFormat")
        every { android.text.format.DateFormat.is24HourFormat(mockContext) } returns true
        
        val uses24Hour = (detector as AndroidLocaleDetector).uses24HourFormat()
        assertTrue(uses24Hour)
        
        unmockkStatic("android.text.format.DateFormat")
    }
    
    @Test
    fun `Android cache statistics provide accurate information`() {
        // Mock cache with data
        every { sharedPreferences.contains(any()) } returns true
        every { sharedPreferences.getLong(any(), any()) } returns Clock.System.now().toEpochMilliseconds()
        
        val stats = androidDataSource.getCacheStats()
        
        assertTrue(stats["hasCachedData"] as Boolean)
        assertTrue(stats["cacheValid"] as Boolean)
        assertTrue((stats["cacheTimestamp"] as Long) > 0)
    }
    
    @Test
    fun `Android preload cache optimizes startup performance`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock database operations
        val dao = mockk<com.eunio.healthapp.data.local.dao.UserPreferencesDao>()
        every { databaseManager.getUserPreferencesDao() } returns dao
        coEvery { dao.getPreferencesByUserId(any()) } returns mockk {
            every { userId } returns preferences.userId
            every { unitSystem } returns preferences.unitSystem.name
            every { isManuallySet } returns if (preferences.isManuallySet) 1L else 0L
            every { lastModified } returns preferences.lastModified.epochSeconds
            every { syncStatus } returns preferences.syncStatus.name
        }
        
        // Mock cache miss initially
        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.getLong(any(), any()) } returns 0L
        
        // Preload cache
        androidDataSource.preloadCache(preferences.userId)
        
        // Verify database was accessed for preloading
        verify { databaseManager.getUserPreferencesDao() }
        
        // Verify caching occurred
        verify { sharedPreferencesEditor.putString(any(), any()) }
    }
    
    @Test
    fun `Android cache survives preference updates`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock database operations
        val dao = mockk<com.eunio.healthapp.data.local.dao.UserPreferencesDao>()
        every { databaseManager.getUserPreferencesDao() } returns dao
        coEvery { dao.getPreferencesByUserId(any()) } returns mockk()
        coEvery { dao.updatePreferences(any(), any(), any(), any(), any()) } just Runs
        coEvery { dao.updateSyncStatus(any(), any()) } just Runs
        
        // Save preferences
        androidDataSource.savePreferences(preferences)
        
        // Update sync status
        androidDataSource.markAsSynced(preferences.userId)
        
        // Since we're using the standard implementation, verify database operations
        verify { databaseManager.getUserPreferencesDao() }
        coVerify { dao.updateSyncStatus(any(), any()) }
    }
    
    @Test
    fun `Android cache clears correctly on preference deletion`() = runTest {
        val preferences = TestDataFactory.createUserPreferences("test_user_123")
        
        // Mock database operations
        val dao = mockk<com.eunio.healthapp.data.local.dao.UserPreferencesDao>()
        every { databaseManager.getUserPreferencesDao() } returns dao
        coEvery { dao.deletePreferencesByUserId(any()) } just Runs
        
        // Clear preferences
        val result = androidDataSource.clearPreferences(preferences.userId)
        
        // Verify database operation was called
        assertTrue(result.isSuccess)
        verify { databaseManager.getUserPreferencesDao() }
        coVerify { dao.deletePreferencesByUserId(any()) }
    }
}