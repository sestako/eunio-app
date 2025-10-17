package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.dao.DailyLogDao
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LogRepositoryImplTest {
    
    private val firestoreService = mockk<FirestoreService>()
    private val dailyLogDao = mockk<DailyLogDao>()
    private val errorHandler = mockk<ErrorHandler>()
    
    private val repository = LogRepositoryImpl(
        firestoreService = firestoreService,
        dailyLogDao = dailyLogDao,
        errorHandler = errorHandler
    )
    
    private val testUserId = "test-user-id"
    private val testDate = LocalDate(2024, 1, 15)
    private val testLog = DailyLog(
        id = "test-log-id",
        userId = testUserId,
        date = testDate,
        periodFlow = PeriodFlow.MEDIUM,
        symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
        mood = Mood.NEUTRAL,
        bbt = 98.2,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @Test
    fun `saveDailyLog saves locally and syncs to remote`() = runTest {
        // Given
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.updateSyncStatus(any(), any()) } returns Unit
        coEvery { firestoreService.saveDailyLog(any()) } returns Result.success(Unit)
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.saveDailyLog(testLog)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dailyLogDao.insertOrUpdate(any()) }
        coVerify { dailyLogDao.updateSyncStatus(any(), "PENDING") }
        coVerify { firestoreService.saveDailyLog(any()) }
        coVerify { dailyLogDao.markAsSynced(testLog.id) }
    }
    
    @Test
    fun `saveDailyLog succeeds even if remote sync fails`() = runTest {
        // Given
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.updateSyncStatus(any(), any()) } returns Unit
        coEvery { firestoreService.saveDailyLog(any()) } returns Result.error(AppError.NetworkError("Network error"))
        
        // When
        val result = repository.saveDailyLog(testLog)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dailyLogDao.insertOrUpdate(any()) }
        coVerify { dailyLogDao.updateSyncStatus(any(), "PENDING") }
        coVerify(exactly = 0) { dailyLogDao.markAsSynced(any()) }
    }
    
    @Test
    fun `saveDailyLog fails when date is in future`() = runTest {
        // Given
        val futureLog = testLog.copy(date = LocalDate(2025, 12, 31))
        every { errorHandler.createValidationError("Cannot log data for future dates", "date") } returns 
            AppError.ValidationError("Cannot log data for future dates", "date")
        
        // When
        val result = repository.saveDailyLog(futureLog)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `saveDailyLog fails when BBT is out of range`() = runTest {
        // Given
        val invalidLog = testLog.copy(bbt = 110.0)
        every { errorHandler.createValidationError("BBT must be between 95°F and 105°F", "bbt") } returns 
            AppError.ValidationError("BBT must be between 95°F and 105°F", "bbt")
        
        // When
        val result = repository.saveDailyLog(invalidLog)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getDailyLog returns log from local cache when available`() = runTest {
        // Given
        coEvery { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) } returns testLog
        coEvery { firestoreService.getDailyLogByDate(testUserId, testDate) } returns Result.success(testLog)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getDailyLog(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testLog, result.getOrNull())
        coVerify { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) }
    }
    
    @Test
    fun `getDailyLog fetches from remote when not in local cache`() = runTest {
        // Given
        coEvery { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) } returns null
        coEvery { firestoreService.getDailyLogByDate(testUserId, testDate) } returns Result.success(testLog)
        coEvery { dailyLogDao.insertOrUpdate(testLog) } returns Unit
        coEvery { dailyLogDao.markAsSynced(testLog.id) } returns Unit
        
        // When
        val result = repository.getDailyLog(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testLog, result.getOrNull())
        coVerify { firestoreService.getDailyLogByDate(testUserId, testDate) }
        coVerify { dailyLogDao.insertOrUpdate(testLog) }
    }
    
    @Test
    fun `getDailyLog returns null when log doesn't exist`() = runTest {
        // Given
        coEvery { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) } returns null
        coEvery { firestoreService.getDailyLogByDate(testUserId, testDate) } returns Result.success(null)
        
        // When
        val result = repository.getDailyLog(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getLogsInRange returns logs from date range`() = runTest {
        // Given
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val logs = listOf(testLog)
        
        coEvery { dailyLogDao.getDailyLogsByDateRange(testUserId, startDate, endDate) } returns logs
        coEvery { firestoreService.getLogsInRange(testUserId, startDate, endDate) } returns Result.success(logs)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getLogsInRange(testUserId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(logs, result.getOrNull())
        coVerify { dailyLogDao.getDailyLogsByDateRange(testUserId, startDate, endDate) }
    }
    
    @Test
    fun `getLogsInRange fails when end date is before start date`() = runTest {
        // Given
        val startDate = LocalDate(2024, 1, 31)
        val endDate = LocalDate(2024, 1, 1)
        every { errorHandler.createValidationError("End date cannot be before start date", "dateRange") } returns 
            AppError.ValidationError("End date cannot be before start date", "dateRange")
        
        // When
        val result = repository.getLogsInRange(testUserId, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getRecentLogs returns recent logs with limit`() = runTest {
        // Given
        val logs = listOf(testLog)
        coEvery { dailyLogDao.getDailyLogsByUserId(testUserId) } returns logs
        coEvery { firestoreService.getRecentLogs(testUserId, 30) } returns Result.success(logs)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getRecentLogs(testUserId, 30)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(logs, result.getOrNull())
    }
    
    @Test
    fun `getRecentLogs fails with invalid limit`() = runTest {
        // Given
        every { errorHandler.createValidationError("Limit must be positive", "limit") } returns 
            AppError.ValidationError("Limit must be positive", "limit")
        
        // When
        val result = repository.getRecentLogs(testUserId, 0)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `deleteDailyLog removes from remote and local`() = runTest {
        // Given
        coEvery { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) } returns testLog
        coEvery { firestoreService.getDailyLogByDate(testUserId, testDate) } returns Result.success(testLog)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        coEvery { firestoreService.deleteDailyLog(testUserId, testLog.id) } returns Result.success(Unit)
        coEvery { dailyLogDao.deleteDailyLog(testLog.id) } returns Unit
        
        // When
        val result = repository.deleteDailyLog(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.deleteDailyLog(testUserId, testLog.id) }
        coVerify { dailyLogDao.deleteDailyLog(testLog.id) }
    }
    
    @Test
    fun `deleteDailyLog fails when log doesn't exist`() = runTest {
        // Given
        coEvery { dailyLogDao.getDailyLogByUserIdAndDate(testUserId, testDate) } returns null
        coEvery { firestoreService.getDailyLogByDate(testUserId, testDate) } returns Result.success(null)
        
        // When
        val result = repository.deleteDailyLog(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `getPeriodLogsInRange filters logs with period flow`() = runTest {
        // Given
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val periodLog = testLog.copy(periodFlow = PeriodFlow.HEAVY)
        val nonPeriodLog = testLog.copy(id = "log2", periodFlow = null)
        val allLogs = listOf(periodLog, nonPeriodLog)
        
        coEvery { dailyLogDao.getDailyLogsByDateRange(testUserId, startDate, endDate) } returns allLogs
        coEvery { firestoreService.getLogsInRange(testUserId, startDate, endDate) } returns Result.success(allLogs)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getPeriodLogsInRange(testUserId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        val periodLogs = result.getOrNull()!!
        assertEquals(1, periodLogs.size)
        assertEquals(periodLog, periodLogs.first())
    }
    
    @Test
    fun `getBBTLogsInRange filters logs with BBT data`() = runTest {
        // Given
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val bbtLog = testLog.copy(bbt = 98.5)
        val nonBbtLog = testLog.copy(id = "log2", bbt = null)
        val allLogs = listOf(bbtLog, nonBbtLog)
        
        coEvery { dailyLogDao.getDailyLogsByDateRange(testUserId, startDate, endDate) } returns allLogs
        coEvery { firestoreService.getLogsInRange(testUserId, startDate, endDate) } returns Result.success(allLogs)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getBBTLogsInRange(testUserId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        val bbtLogs = result.getOrNull()!!
        assertEquals(1, bbtLogs.size)
        assertEquals(bbtLog, bbtLogs.first())
    }
    
    @Test
    fun `getFertilityLogsInRange filters logs with fertility indicators`() = runTest {
        // Given
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val fertilityLog = testLog.copy(cervicalMucus = CervicalMucus.EGG_WHITE)
        val nonFertilityLog = testLog.copy(id = "log2", cervicalMucus = null, opkResult = null)
        val allLogs = listOf(fertilityLog, nonFertilityLog)
        
        coEvery { dailyLogDao.getDailyLogsByDateRange(testUserId, startDate, endDate) } returns allLogs
        coEvery { firestoreService.getLogsInRange(testUserId, startDate, endDate) } returns Result.success(allLogs)
        coEvery { dailyLogDao.insertOrUpdate(any()) } returns Unit
        coEvery { dailyLogDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getFertilityLogsInRange(testUserId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        val fertilityLogs = result.getOrNull()!!
        assertEquals(1, fertilityLogs.size)
        assertEquals(fertilityLog, fertilityLogs.first())
    }
    
    @Test
    fun `getLogCount returns total number of logs for user`() = runTest {
        // Given
        val logs = listOf(testLog, testLog.copy(id = "log2"))
        coEvery { dailyLogDao.getDailyLogsByUserId(testUserId) } returns logs
        
        // When
        val result = repository.getLogCount(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }
    
    @Test
    fun `getLogsBySymptoms filters logs by symptoms`() = runTest {
        // Given
        val crampsLog = testLog.copy(symptoms = listOf(Symptom.CRAMPS))
        val headacheLog = testLog.copy(id = "log2", symptoms = listOf(Symptom.HEADACHE))
        val noSymptomsLog = testLog.copy(id = "log3", symptoms = emptyList())
        val allLogs = listOf(crampsLog, headacheLog, noSymptomsLog)
        
        coEvery { dailyLogDao.getDailyLogsByUserId(testUserId) } returns allLogs
        
        // When
        val result = repository.getLogsBySymptoms(testUserId, listOf(Symptom.CRAMPS))
        
        // Then
        assertTrue(result.isSuccess)
        val matchingLogs = result.getOrNull()!!
        assertEquals(1, matchingLogs.size)
        assertEquals(crampsLog, matchingLogs.first())
    }
    
    @Test
    fun `getLogsBySymptoms fails when no symptoms specified`() = runTest {
        // Given
        every { errorHandler.createValidationError("At least one symptom must be specified", "symptoms") } returns 
            AppError.ValidationError("At least one symptom must be specified", "symptoms")
        
        // When
        val result = repository.getLogsBySymptoms(testUserId, emptyList())
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `syncPendingChanges syncs all pending logs to remote`() = runTest {
        // Given
        val pendingLogs = listOf(testLog)
        coEvery { dailyLogDao.getPendingSync() } returns pendingLogs
        coEvery { firestoreService.updateDailyLog(testLog) } returns Result.success(Unit)
        coEvery { dailyLogDao.markAsSynced(testLog.id) } returns Unit
        
        // When
        val result = repository.syncPendingChanges()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dailyLogDao.getPendingSync() }
        coVerify { firestoreService.updateDailyLog(testLog) }
        coVerify { dailyLogDao.markAsSynced(testLog.id) }
    }
}