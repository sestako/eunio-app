package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.*

class GetDailyLogUseCaseTest {
    
    private lateinit var mockLogRepository: MockLogRepository
    private lateinit var getDailyLogUseCase: GetDailyLogUseCase
    
    @BeforeTest
    fun setup() {
        mockLogRepository = MockLogRepository()
        getDailyLogUseCase = GetDailyLogUseCase(mockLogRepository)
    }
    
    @Test
    fun `invoke should return daily log when it exists`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        val expectedLog = createTestDailyLog(userId, date)
        mockLogRepository.getDailyLogResult = Result.success(expectedLog)
        
        // When
        val result = getDailyLogUseCase(userId, date)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLog, result.getOrNull())
        assertEquals(1, mockLogRepository.getDailyLogCallCount)
        assertEquals(userId, mockLogRepository.lastUserId)
        assertEquals(date, mockLogRepository.lastDate)
    }
    
    @Test
    fun `invoke should return null when log does not exist`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        mockLogRepository.getDailyLogResult = Result.success(null)
        
        // When
        val result = getDailyLogUseCase(userId, date)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(1, mockLogRepository.getDailyLogCallCount)
    }
    
    @Test
    fun `invoke should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        val date = LocalDate(2024, 1, 15)
        
        // When
        val result = getDailyLogUseCase(userId, date)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("User ID cannot be blank"))
        assertEquals(0, mockLogRepository.getDailyLogCallCount)
    }
    
    @Test
    fun `invoke should handle repository error`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        mockLogRepository.getDailyLogResult = Result.error(AppError.NetworkError("Network error"))
        
        // When
        val result = getDailyLogUseCase(userId, date)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.DataSyncError>(error)
        assertTrue(error.message.contains("Failed to retrieve daily log"))
        assertEquals(1, mockLogRepository.getDailyLogCallCount)
    }
    
    @Test
    fun `getDailyLogOrError should return log when it exists`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        val expectedLog = createTestDailyLog(userId, date)
        mockLogRepository.getDailyLogResult = Result.success(expectedLog)
        
        // When
        val result = getDailyLogUseCase.getDailyLogOrError(userId, date)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLog, result.getOrNull())
    }
    
    @Test
    fun `getDailyLogOrError should fail when log does not exist`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        mockLogRepository.getDailyLogResult = Result.success(null)
        
        // When
        val result = getDailyLogUseCase.getDailyLogOrError(userId, date)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("No log found for date"))
    }
    
    @Test
    fun `hasLogForDate should return true when log exists`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        val expectedLog = createTestDailyLog(userId, date)
        mockLogRepository.getDailyLogResult = Result.success(expectedLog)
        
        // When
        val result = getDailyLogUseCase.hasLogForDate(userId, date)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }
    
    @Test
    fun `hasLogForDate should return false when log does not exist`() = runTest {
        // Given
        val userId = "test-user-1"
        val date = LocalDate(2024, 1, 15)
        mockLogRepository.getDailyLogResult = Result.success(null)
        
        // When
        val result = getDailyLogUseCase.hasLogForDate(userId, date)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == false)
    }
    
    @Test
    fun `hasLogForDate should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        val date = LocalDate(2024, 1, 15)
        
        // When
        val result = getDailyLogUseCase.hasLogForDate(userId, date)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `getDailyLogsForDates should return map of logs for multiple dates`() = runTest {
        // Given
        val userId = "test-user-1"
        val dates = listOf(
            LocalDate(2024, 1, 15),
            LocalDate(2024, 1, 16),
            LocalDate(2024, 1, 17)
        )
        val logs = dates.map { createTestDailyLog(userId, it) }
        
        // Mock repository to return different logs for different dates
        var callIndex = 0
        mockLogRepository.getDailyLogCallback = { _, _ ->
            val log = if (callIndex < logs.size) logs[callIndex] else null
            callIndex++
            Result.success(log)
        }
        
        // When
        val result = getDailyLogUseCase.getDailyLogsForDates(userId, dates)
        
        // Then
        assertTrue(result.isSuccess)
        val logsMap = result.getOrNull()!!
        assertEquals(3, logsMap.size)
        assertEquals(logs[0], logsMap[dates[0]])
        assertEquals(logs[1], logsMap[dates[1]])
        assertEquals(logs[2], logsMap[dates[2]])
    }
    
    @Test
    fun `getDailyLogsForDates should return empty map for empty dates list`() = runTest {
        // Given
        val userId = "test-user-1"
        val dates = emptyList<LocalDate>()
        
        // When
        val result = getDailyLogUseCase.getDailyLogsForDates(userId, dates)
        
        // Then
        assertTrue(result.isSuccess)
        val logsMap = result.getOrNull()!!
        assertTrue(logsMap.isEmpty())
        assertEquals(0, mockLogRepository.getDailyLogCallCount)
    }
    
    @Test
    fun `getDailyLogsForDates should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        val dates = listOf(LocalDate(2024, 1, 15))
        
        // When
        val result = getDailyLogUseCase.getDailyLogsForDates(userId, dates)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `getDailyLogsForDates should fail when too many dates requested`() = runTest {
        // Given
        val userId = "test-user-1"
        val dates = (1..101).map { LocalDate(2024, 1, it % 28 + 1) }
        
        // When
        val result = getDailyLogUseCase.getDailyLogsForDates(userId, dates)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Cannot retrieve more than 100 logs at once"))
    }
    
    @Test
    fun `getMostRecentLog should return most recent log`() = runTest {
        // Given
        val userId = "test-user-1"
        val recentLog = createTestDailyLog(userId, LocalDate(2024, 1, 15))
        mockLogRepository.getRecentLogsResult = Result.success(listOf(recentLog))
        
        // When
        val result = getDailyLogUseCase.getMostRecentLog(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(recentLog, result.getOrNull())
        assertEquals(1, mockLogRepository.getRecentLogsCallCount)
        assertEquals(1, mockLogRepository.lastLimit)
    }
    
    @Test
    fun `getMostRecentLog should return null when no logs exist`() = runTest {
        // Given
        val userId = "test-user-1"
        mockLogRepository.getRecentLogsResult = Result.success(emptyList())
        
        // When
        val result = getDailyLogUseCase.getMostRecentLog(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getMostRecentLog should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        
        // When
        val result = getDailyLogUseCase.getMostRecentLog(userId)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("User ID cannot be blank"))
    }
    
    private fun createTestDailyLog(userId: String, date: LocalDate): DailyLog {
        return DailyLog(
            id = "test-log-${date}",
            userId = userId,
            date = date,
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS),
            mood = Mood.NEUTRAL,
            sexualActivity = SexualActivity(occurred = false),
            bbt = 36.5,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Test log",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
}

