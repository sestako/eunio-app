package com.eunio.healthapp.data.local.dao

import com.eunio.healthapp.data.local.AndroidTestDatabaseDriverFactory
import com.eunio.healthapp.data.local.DatabaseManager
import com.eunio.healthapp.data.local.DatabaseMigrations
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPreferencesDaoTest {
    
    private lateinit var databaseManager: DatabaseManager
    private lateinit var userPreferencesDao: UserPreferencesDao
    private lateinit var userDao: UserDao
    
    @Before
    fun setup() {
        val driverFactory = AndroidTestDatabaseDriverFactory()
        databaseManager = DatabaseManager(driverFactory)
        userPreferencesDao = databaseManager.getUserPreferencesDao()
        userDao = databaseManager.getUserDao()
    }
    
    @After
    fun teardown() {
        databaseManager.closeDatabase()
    }
    
    @Test
    fun `insertPreferences and getPreferencesByUserId work correctly`() = runTest {
        // First insert a user
        userDao.insertUser(
            id = "user1",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        // Retrieve preferences
        val preferences = userPreferencesDao.getPreferencesByUserId("user1")
        
        assertNotNull(preferences)
        assertEquals("user1", preferences.userId)
        assertEquals("IMPERIAL", preferences.unitSystem)
        assertEquals(1L, preferences.isManuallySet)
        assertEquals(1234567890L, preferences.lastModified)
        assertEquals("PENDING", preferences.syncStatus)
    }
    
    @Test
    fun `updatePreferences works correctly`() = runTest {
        // First insert a user
        userDao.insertUser(
            id = "user1",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert initial preferences
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "METRIC",
            isManuallySet = 0,
            lastModified = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Update preferences
        userPreferencesDao.updatePreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567900,
            syncStatus = "PENDING"
        )
        
        // Retrieve updated preferences
        val preferences = userPreferencesDao.getPreferencesByUserId("user1")
        
        assertNotNull(preferences)
        assertEquals("IMPERIAL", preferences.unitSystem)
        assertEquals(1L, preferences.isManuallySet)
        assertEquals(1234567900L, preferences.lastModified)
        assertEquals("PENDING", preferences.syncStatus)
    }
    
    @Test
    fun `updateSyncStatus works correctly`() = runTest {
        // First insert a user
        userDao.insertUser(
            id = "user1",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences with PENDING status
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        // Update sync status
        userPreferencesDao.updateSyncStatus("user1", "SYNCED")
        
        // Retrieve preferences
        val preferences = userPreferencesDao.getPreferencesByUserId("user1")
        
        assertNotNull(preferences)
        assertEquals("SYNCED", preferences.syncStatus)
    }
    
    @Test
    fun `getPendingSyncPreferences returns only pending preferences`() = runTest {
        // Insert users
        userDao.insertUser(
            id = "user1",
            email = "test1@example.com",
            name = "Test User 1",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        userDao.insertUser(
            id = "user2",
            email = "test2@example.com",
            name = "Test User 2",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences - one pending, one synced
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        userPreferencesDao.insertPreferences(
            userId = "user2",
            unitSystem = "METRIC",
            isManuallySet = 0,
            lastModified = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Get pending preferences
        val pendingPreferences = userPreferencesDao.getPendingSyncPreferences()
        
        assertEquals(1, pendingPreferences.size)
        assertEquals("user1", pendingPreferences[0].userId)
        assertEquals("PENDING", pendingPreferences[0].syncStatus)
    }
    
    @Test
    fun `deletePreferencesByUserId works correctly`() = runTest {
        // First insert a user
        userDao.insertUser(
            id = "user1",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        // Verify preferences exist
        val preferencesBeforeDelete = userPreferencesDao.getPreferencesByUserId("user1")
        assertNotNull(preferencesBeforeDelete)
        
        // Delete preferences
        userPreferencesDao.deletePreferencesByUserId("user1")
        
        // Verify preferences are deleted
        val preferencesAfterDelete = userPreferencesDao.getPreferencesByUserId("user1")
        assertNull(preferencesAfterDelete)
    }
    
    @Test
    fun `getAllPreferences returns all preferences`() = runTest {
        // Insert users
        userDao.insertUser(
            id = "user1",
            email = "test1@example.com",
            name = "Test User 1",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        userDao.insertUser(
            id = "user2",
            email = "test2@example.com",
            name = "Test User 2",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences for both users
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        userPreferencesDao.insertPreferences(
            userId = "user2",
            unitSystem = "METRIC",
            isManuallySet = 0,
            lastModified = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Get all preferences
        val allPreferences = userPreferencesDao.getAllPreferences()
        
        assertEquals(2, allPreferences.size)
        assertTrue(allPreferences.any { it.userId == "user1" })
        assertTrue(allPreferences.any { it.userId == "user2" })
    }
    
    @Test
    fun `deleteAllPreferences works correctly`() = runTest {
        // Insert users
        userDao.insertUser(
            id = "user1",
            email = "test1@example.com",
            name = "Test User 1",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        userDao.insertUser(
            id = "user2",
            email = "test2@example.com",
            name = "Test User 2",
            onboardingComplete = 1,
            primaryGoal = "CYCLE_TRACKING",
            unitSystem = "METRIC",
            createdAt = 1234567890,
            updatedAt = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Insert preferences for both users
        userPreferencesDao.insertPreferences(
            userId = "user1",
            unitSystem = "IMPERIAL",
            isManuallySet = 1,
            lastModified = 1234567890,
            syncStatus = "PENDING"
        )
        
        userPreferencesDao.insertPreferences(
            userId = "user2",
            unitSystem = "METRIC",
            isManuallySet = 0,
            lastModified = 1234567890,
            syncStatus = "SYNCED"
        )
        
        // Verify preferences exist
        val preferencesBeforeDelete = userPreferencesDao.getAllPreferences()
        assertEquals(2, preferencesBeforeDelete.size)
        
        // Delete all preferences
        userPreferencesDao.deleteAllPreferences()
        
        // Verify all preferences are deleted
        val preferencesAfterDelete = userPreferencesDao.getAllPreferences()
        assertEquals(0, preferencesAfterDelete.size)
    }
}