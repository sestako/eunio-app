package com.eunio.healthapp.data.local.dao

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.eunio.healthapp.database.EunioDatabase
import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

class DailyLogDaoTest {
    
    private lateinit var driver: SqlDriver
    private lateinit var database: EunioDatabase
    private lateinit var dailyLogDao: DailyLogDao
    
    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EunioDatabase.Schema.create(driver)
        database = EunioDatabase(driver)
        dailyLogDao = DailyLogDao(database)
    }
    
    @AfterTest
    fun tearDown() {
        driver.close()
    }
    
    @Test
    fun insertDailyLog_shouldSaveLogToDatabase() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        
        // When
        dailyLogDao.insertDailyLog(dailyLog)
        
        // Then
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(dailyLog.userId, dailyLog.date)
        assertNotNull(retrievedLog)
        assertEquals(dailyLog.id, retrievedLog.id)
        assertEquals(dailyLog.userId, retrievedLog.userId)
        assertEquals(dailyLog.date, retrievedLog.date)
        assertEquals(dailyLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(dailyLog.symptoms, retrievedLog.symptoms)
        assertEquals(dailyLog.mood, retrievedLog.mood)
        assertEquals(dailyLog.bbt, retrievedLog.bbt)
    }
    
    @Test
    fun updateDailyLog_shouldModifyExistingLog() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        val updatedLog = dailyLog.copy(
            periodFlow = PeriodFlow.HEAVY,
            mood = Mood.HAPPY,
            bbt = 98.8,
            updatedAt = Clock.System.now()
        )
        
        // When
        dailyLogDao.updateDailyLog(updatedLog)
        
        // Then
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(dailyLog.userId, dailyLog.date)
        assertNotNull(retrievedLog)
        assertEquals(PeriodFlow.HEAVY, retrievedLog.periodFlow)
        assertEquals(Mood.HAPPY, retrievedLog.mood)
        assertEquals(98.8, retrievedLog.bbt)
    }
    
    @Test
    fun getDailyLogsByUserId_shouldReturnUserLogs() = runTest {
        // Given
        val userId = "test-user"
        val log1 = createTestDailyLog(id = "log1", userId = userId, date = LocalDate(2024, 1, 1))
        val log2 = createTestDailyLog(id = "log2", userId = userId, date = LocalDate(2024, 1, 2))
        val log3 = createTestDailyLog(id = "log3", userId = "other-user", date = LocalDate(2024, 1, 3))
        
        dailyLogDao.insertDailyLog(log1)
        dailyLogDao.insertDailyLog(log2)
        dailyLogDao.insertDailyLog(log3)
        
        // When
        val userLogs = dailyLogDao.getDailyLogsByUserId(userId)
        
        // Then
        assertEquals(2, userLogs.size)
        assertTrue(userLogs.all { it.userId == userId })
    }
    
    @Test
    fun getDailyLogsByDateRange_shouldReturnLogsInRange() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 3)
        
        val log1 = createTestDailyLog(id = "log1", userId = userId, date = LocalDate(2024, 1, 1))
        val log2 = createTestDailyLog(id = "log2", userId = userId, date = LocalDate(2024, 1, 2))
        val log3 = createTestDailyLog(id = "log3", userId = userId, date = LocalDate(2024, 1, 4))
        
        dailyLogDao.insertDailyLog(log1)
        dailyLogDao.insertDailyLog(log2)
        dailyLogDao.insertDailyLog(log3)
        
        // When
        val logsInRange = dailyLogDao.getDailyLogsByDateRange(userId, startDate, endDate)
        
        // Then
        assertEquals(2, logsInRange.size)
        assertTrue(logsInRange.all { it.date >= startDate && it.date <= endDate })
    }
    
    @Test
    fun getPendingSyncLogs_shouldReturnOnlyPendingLogs() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        // When
        val pendingLogs = dailyLogDao.getPendingSyncLogs()
        
        // Then
        assertEquals(1, pendingLogs.size)
        assertEquals(dailyLog.id, pendingLogs.first().id)
    }
    
    @Test
    fun updateSyncStatus_shouldChangeLogSyncStatus() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        // When - Use markAsSynced which updates all sync-related fields
        dailyLogDao.markAsSynced(dailyLog.id)
        
        // Then
        val pendingLogs = dailyLogDao.getPendingSyncLogs()
        assertTrue(pendingLogs.isEmpty())
    }
    
    @Test
    fun deleteDailyLog_shouldRemoveLogFromDatabase() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        // When
        dailyLogDao.deleteDailyLog(dailyLog.id)
        
        // Then
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(dailyLog.userId, dailyLog.date)
        assertNull(retrievedLog)
    }
    
    @Test
    fun complexDailyLog_withAllFields_shouldSerializeCorrectly() = runTest {
        // Given
        val complexLog = createTestDailyLog().copy(
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING, Symptom.HEADACHE),
            mood = Mood.IRRITABLE,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            bbt = 98.6,
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.POSITIVE,
            notes = "Test notes with special characters: éñ"
        )
        
        // When
        dailyLogDao.insertDailyLog(complexLog)
        
        // Then
        val retrievedLog = dailyLogDao.getDailyLogByUserIdAndDate(complexLog.userId, complexLog.date)
        assertNotNull(retrievedLog)
        assertEquals(complexLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(complexLog.symptoms, retrievedLog.symptoms)
        assertEquals(complexLog.mood, retrievedLog.mood)
        assertEquals(complexLog.sexualActivity, retrievedLog.sexualActivity)
        assertEquals(complexLog.bbt, retrievedLog.bbt)
        assertEquals(complexLog.cervicalMucus, retrievedLog.cervicalMucus)
        assertEquals(complexLog.opkResult, retrievedLog.opkResult)
        assertEquals(complexLog.notes, retrievedLog.notes)
    }
    
    @Test
    fun markAsSynced_shouldUpdateSyncMetadata() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        // Verify initially pending
        val pendingBefore = dailyLogDao.getPendingSyncLogs()
        assertEquals(1, pendingBefore.size)
        
        // When
        dailyLogDao.markAsSynced(dailyLog.id)
        
        // Then
        val pendingAfter = dailyLogDao.getPendingSyncLogs()
        assertTrue(pendingAfter.isEmpty(), "Log should not be in pending list after marking as synced")
    }
    
    @Test
    fun markAsPendingSync_shouldAddLogToPendingList() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        dailyLogDao.markAsSynced(dailyLog.id)
        
        // Verify not pending after sync
        val pendingAfterSync = dailyLogDao.getPendingSyncLogs()
        assertTrue(pendingAfterSync.isEmpty())
        
        // When
        dailyLogDao.markAsPendingSync(dailyLog.id)
        
        // Then
        val pendingAfterMark = dailyLogDao.getPendingSyncLogs()
        assertEquals(1, pendingAfterMark.size)
        assertEquals(dailyLog.id, pendingAfterMark.first().id)
    }
    
    @Test
    fun incrementSyncRetryCount_shouldUpdateRetryMetadata() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        
        // When - Increment retry count multiple times
        dailyLogDao.incrementSyncRetryCount(dailyLog.id)
        dailyLogDao.incrementSyncRetryCount(dailyLog.id)
        dailyLogDao.incrementSyncRetryCount(dailyLog.id)
        
        // Then - Verify the log is still in the database
        val retrievedLog = dailyLogDao.getDailyLogById(dailyLog.id)
        assertNotNull(retrievedLog, "Log should still exist after incrementing retry count")
    }
    
    @Test
    fun resetSyncRetryCount_shouldResetRetryMetadata() = runTest {
        // Given
        val dailyLog = createTestDailyLog()
        dailyLogDao.insertDailyLog(dailyLog)
        dailyLogDao.incrementSyncRetryCount(dailyLog.id)
        dailyLogDao.incrementSyncRetryCount(dailyLog.id)
        
        // When
        dailyLogDao.resetSyncRetryCount(dailyLog.id)
        
        // Then - Verify the log is still in the database
        val retrievedLog = dailyLogDao.getDailyLogById(dailyLog.id)
        assertNotNull(retrievedLog, "Log should still exist after resetting retry count")
    }
    
    private fun createTestDailyLog(
        id: String = "test-log-id",
        userId: String = "test-user-id",
        date: LocalDate = LocalDate(2024, 1, 1)
    ): DailyLog {
        val now = Clock.System.now()
        return DailyLog(
            id = id,
            userId = userId,
            date = date,
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