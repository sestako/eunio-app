package com.eunio.healthapp.data.local

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

class DatabaseManagerTest {
    
    private lateinit var databaseManager: DatabaseManager
    
    @BeforeTest
    fun setup() {
        val testDriverFactory = AndroidTestDatabaseDriverFactory()
        databaseManager = DatabaseManager(testDriverFactory)
    }
    
    @AfterTest
    fun tearDown() {
        databaseManager.closeDatabase()
    }
    
    @Test
    fun databaseManager_shouldInitializeDatabaseOnFirstAccess() {
        // Given - Fresh database manager
        assertFalse(databaseManager.isDatabaseInitialized())
        
        // When - Access database for first time
        val database = databaseManager.getDatabase()
        
        // Then - Database should be initialized
        assertNotNull(database)
        assertTrue(databaseManager.isDatabaseInitialized())
    }
    
    @Test
    fun getUserDao_shouldReturnFunctionalDao() = runTest {
        // Given
        val userDao = databaseManager.getUserDao()
        val testUser = createTestUser("user-dao-test-${Clock.System.now().toEpochMilliseconds()}")
        
        // When
        userDao.insertUser(testUser)
        val retrievedUser = userDao.getUserById(testUser.id)
        
        // Then
        assertNotNull(retrievedUser)
        assertEquals(testUser.id, retrievedUser.id)
        assertEquals(testUser.email, retrievedUser.email)
    }
    
    @Test
    fun getDailyLogDao_shouldReturnFunctionalDao() = runTest {
        // Given
        val dailyLogDao = databaseManager.getDailyLogDao()
        val userDao = databaseManager.getUserDao()
        
        // Create unique IDs for this test
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val userId = "user-daily-log-test-$timestamp"
        val logId = "log-daily-log-test-$timestamp"
        
        // First insert the user (required for foreign key constraint)
        val testUser = createTestUser(userId)
        userDao.insertUser(testUser)
        
        // Then test the daily log
        val testLog = createTestDailyLog(logId, userId)
        
        // When
        dailyLogDao.insertDailyLog(testLog)
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(testLog.userId, testLog.date)
        
        // Then
        assertNotNull(retrievedLog)
        assertEquals(testLog.id, retrievedLog.id)
        assertEquals(testLog.userId, retrievedLog.userId)
    }
    
    @Test
    fun closeDatabase_shouldCleanupResources() {
        // Given - Initialize database
        databaseManager.getDatabase()
        assertTrue(databaseManager.isDatabaseInitialized())
        
        // When
        databaseManager.closeDatabase()
        
        // Then
        assertFalse(databaseManager.isDatabaseInitialized())
    }
    
    @Test
    fun reinitializeDatabase_shouldCreateNewDatabaseInstance() = runTest {
        // Given - Initialize and use database
        val userDao1 = databaseManager.getUserDao()
        val testUser = createTestUser("reinit-test-${Clock.System.now().toEpochMilliseconds()}")
        userDao1.insertUser(testUser)
        
        // When - Reinitialize
        val newDatabase = databaseManager.reinitializeDatabase()
        val userDao2 = databaseManager.getUserDao()
        
        // Then - New database should be empty (in-memory)
        assertNotNull(newDatabase)
        assertTrue(databaseManager.isDatabaseInitialized())
        val retrievedUser = userDao2.getUserById(testUser.id)
        assertNull(retrievedUser) // Data should be gone in new in-memory database
    }
    
    @Test
    fun multipleAccess_shouldReturnSameDatabaseInstance() {
        // Given & When
        val database1 = databaseManager.getDatabase()
        val database2 = databaseManager.getDatabase()
        
        // Then
        assertSame(database1, database2)
    }
    
    @Test
    fun daoInstances_shouldWorkWithSameDatabase() = runTest {
        // Given
        val userDao = databaseManager.getUserDao()
        val dailyLogDao = databaseManager.getDailyLogDao()
        
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val userId = "dao-instances-test-$timestamp"
        val logId = "log-dao-instances-test-$timestamp"
        
        val testUser = createTestUser(userId)
        val testLog = createTestDailyLog(logId, userId)
        
        // When - Insert data using different DAOs
        userDao.insertUser(testUser)
        dailyLogDao.insertDailyLog(testLog)
        
        // Then - Both should be accessible
        val retrievedUser = userDao.getUserById(testUser.id)
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(testLog.userId, testLog.date)
        
        assertNotNull(retrievedUser)
        assertNotNull(retrievedLog)
        assertEquals(testUser.id, retrievedLog.userId)
    }
    
    private fun createTestUser(id: String = "test-user-id"): User {
        val now = Clock.System.now()
        return User(
            id = id,
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = false,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = now,
            updatedAt = now
        )
    }
    
    private fun createTestDailyLog(
        id: String = "test-log-id",
        userId: String = "test-user-id"
    ): DailyLog {
        val now = Clock.System.now()
        return DailyLog(
            id = id,
            userId = userId,
            date = LocalDate(2024, 1, 1),
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS),
            mood = Mood.NEUTRAL,
            sexualActivity = null,
            bbt = 98.2,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Test notes",
            createdAt = now,
            updatedAt = now
        )
    }
}