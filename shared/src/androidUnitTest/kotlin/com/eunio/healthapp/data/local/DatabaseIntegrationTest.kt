package com.eunio.healthapp.data.local

import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

class DatabaseIntegrationTest {
    
    private lateinit var database: EunioDatabase
    private lateinit var userDao: UserDao
    private lateinit var dailyLogDao: DailyLogDao
    
    @BeforeTest
    fun setup() {
        val testDriverFactory = AndroidTestDatabaseDriverFactory()
        database = EunioDatabase(testDriverFactory.createDriver())
        userDao = UserDao(database)
        dailyLogDao = DailyLogDao(database)
    }
    
    @Test
    fun fullDatabaseWorkflow_shouldWorkCorrectly() = runTest {
        // Given - Create a user
        val now = Clock.System.now()
        val user = User(
            id = "test-user-123",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = now,
            updatedAt = now
        )
        
        // When - Insert user
        userDao.insertUser(user)
        
        // Then - Verify user exists
        val retrievedUser = userDao.getUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(user.email, retrievedUser.email)
        assertEquals(user.name, retrievedUser.name)
        assertEquals(user.primaryGoal, retrievedUser.primaryGoal)
        
        // Given - Create a daily log for the user
        val dailyLog = DailyLog(
            id = "log-123",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
            mood = Mood.NEUTRAL,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            bbt = 98.6,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Feeling good today",
            createdAt = now,
            updatedAt = now
        )
        
        // When - Insert daily log
        dailyLogDao.insertDailyLog(dailyLog)
        
        // Then - Verify daily log exists and data is correct
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(user.id, dailyLog.date)
        assertNotNull(retrievedLog)
        assertEquals(dailyLog.id, retrievedLog.id)
        assertEquals(dailyLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(dailyLog.symptoms, retrievedLog.symptoms)
        assertEquals(dailyLog.mood, retrievedLog.mood)
        assertEquals(dailyLog.sexualActivity, retrievedLog.sexualActivity)
        assertEquals(dailyLog.bbt, retrievedLog.bbt)
        assertEquals(dailyLog.cervicalMucus, retrievedLog.cervicalMucus)
        assertEquals(dailyLog.opkResult, retrievedLog.opkResult)
        assertEquals(dailyLog.notes, retrievedLog.notes)
        
        // Test sync status functionality
        val pendingUsers = userDao.getPendingSyncUsers()
        assertEquals(1, pendingUsers.size)
        assertEquals(user.id, pendingUsers.first().id)
        
        val pendingLogs = dailyLogDao.getPendingSyncLogs()
        assertEquals(1, pendingLogs.size)
        assertEquals(dailyLog.id, pendingLogs.first().id)
        
        // Mark as synced
        userDao.updateSyncStatus(user.id, "SYNCED")
        dailyLogDao.updateSyncStatus(dailyLog.id, "SYNCED")
        
        // Verify no pending items
        assertTrue(userDao.getPendingSyncUsers().isEmpty())
        assertTrue(dailyLogDao.getPendingSyncLogs().isEmpty())
    }
}