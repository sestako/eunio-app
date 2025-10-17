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

class GetLogHistoryUseCaseTest {
    
    private lateinit var mockLogRepository: MockLogRepository
    private lateinit var getLogHistoryUseCase: GetLogHistoryUseCase
    
    @BeforeTest
    fun setup() {
        mockLogRepository = MockLogRepository()
        getLogHistoryUseCase = GetLogHistoryUseCase(mockLogRepository)
    }
    
    @Test
    fun `invoke should return logs in date range`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedLogs = listOf(
            createTestDailyLog(userId, LocalDate(2024, 1, 15)),
            createTestDailyLog(userId, LocalDate(2024, 1, 20))
        )
        mockLogRepository.getLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getLogsInRangeCallCount)
        assertEquals(userId, mockLogRepository.lastUserId)
        assertEquals(startDate, mockLogRepository.lastStartDate)
        assertEquals(endDate, mockLogRepository.lastEndDate)
    }
    
    @Test
    fun `invoke should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = getLogHistoryUseCase(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("User ID cannot be blank"))
        assertEquals(0, mockLogRepository.getLogsInRangeCallCount)
    }
    
    @Test
    fun `invoke should fail when start date is after end date`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 31)
        val endDate = LocalDate(2024, 1, 1)
        
        // When
        val result = getLogHistoryUseCase(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Start date cannot be after end date"))
        assertEquals(0, mockLogRepository.getLogsInRangeCallCount)
    }
    
    @Test
    fun `invoke should fail when date range exceeds 2 years`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2020, 1, 1)
        val endDate = LocalDate(2023, 1, 1)
        
        // When
        val result = getLogHistoryUseCase(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Date range cannot exceed 2 years"))
        assertEquals(0, mockLogRepository.getLogsInRangeCallCount)
    }
    
    @Test
    fun `invoke should handle repository error`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        mockLogRepository.getLogsInRangeResult = Result.error(AppError.NetworkError("Network error"))
        
        // When
        val result = getLogHistoryUseCase(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.DataSyncError>(error)
        assertTrue(error.message.contains("Failed to retrieve log history"))
        assertEquals(1, mockLogRepository.getLogsInRangeCallCount)
    }
    
    @Test
    fun `getRecentLogs should return recent logs with default limit`() = runTest {
        // Given
        val userId = "test-user-1"
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getRecentLogsResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getRecentLogs(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getRecentLogsCallCount)
        assertEquals(30, mockLogRepository.lastLimit) // Default limit
    }
    
    @Test
    fun `getRecentLogs should return recent logs with custom limit`() = runTest {
        // Given
        val userId = "test-user-1"
        val customLimit = 7
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getRecentLogsResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getRecentLogs(userId, customLimit)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(customLimit, mockLogRepository.lastLimit)
    }
    
    @Test
    fun `getRecentLogs should fail when days is zero or negative`() = runTest {
        // Given
        val userId = "test-user-1"
        
        // When
        val result = getLogHistoryUseCase.getRecentLogs(userId, 0)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Number of days must be positive"))
    }
    
    @Test
    fun `getRecentLogs should fail when days exceeds maximum`() = runTest {
        // Given
        val userId = "test-user-1"
        
        // When
        val result = getLogHistoryUseCase.getRecentLogs(userId, 366)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Cannot retrieve more than 365 days"))
    }
    
    @Test
    fun `getCurrentMonthLogs should return logs for current month`() = runTest {
        // Given
        val userId = "test-user-1"
        val referenceDate = LocalDate(2024, 6, 15)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 6, 10)))
        mockLogRepository.getLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getCurrentMonthLogs(userId, referenceDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(LocalDate(2024, 6, 1), mockLogRepository.lastStartDate)
        assertEquals(LocalDate(2024, 6, 30), mockLogRepository.lastEndDate)
    }
    
    @Test
    fun `getPreviousMonthLogs should return logs for previous month`() = runTest {
        // Given
        val userId = "test-user-1"
        val referenceDate = LocalDate(2024, 6, 15)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 5, 10)))
        mockLogRepository.getLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getPreviousMonthLogs(userId, referenceDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(LocalDate(2024, 5, 1), mockLogRepository.lastStartDate)
        assertEquals(LocalDate(2024, 5, 31), mockLogRepository.lastEndDate)
    }
    
    @Test
    fun `getPeriodLogs should return period logs in range`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getPeriodLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getPeriodLogs(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getPeriodLogsInRangeCallCount)
    }
    
    @Test
    fun `getBBTLogs should return BBT logs in range`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getBBTLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getBBTLogs(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getBBTLogsInRangeCallCount)
    }
    
    @Test
    fun `getFertilityLogs should return fertility logs in range`() = runTest {
        // Given
        val userId = "test-user-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getFertilityLogsInRangeResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getFertilityLogs(userId, startDate, endDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getFertilityLogsInRangeCallCount)
    }
    
    @Test
    fun `getLogsBySymptoms should return logs with specified symptoms`() = runTest {
        // Given
        val userId = "test-user-1"
        val symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE)
        val expectedLogs = listOf(createTestDailyLog(userId, LocalDate(2024, 1, 15)))
        mockLogRepository.getLogsBySymptomsResult = Result.success(expectedLogs)
        
        // When
        val result = getLogHistoryUseCase.getLogsBySymptoms(userId, symptoms)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedLogs, result.getOrNull())
        assertEquals(1, mockLogRepository.getLogsBySymptomsCallCount)
        assertEquals(symptoms, mockLogRepository.lastSymptoms)
    }
    
    @Test
    fun `getLogsBySymptoms should fail when symptoms list is empty`() = runTest {
        // Given
        val userId = "test-user-1"
        val symptoms = emptyList<Symptom>()
        
        // When
        val result = getLogHistoryUseCase.getLogsBySymptoms(userId, symptoms)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("At least one symptom must be specified"))
    }
    
    @Test
    fun `getLogsBySymptoms should validate date range when provided`() = runTest {
        // Given
        val userId = "test-user-1"
        val symptoms = listOf(Symptom.CRAMPS)
        val startDate = LocalDate(2024, 1, 31)
        val endDate = LocalDate(2024, 1, 1) // Invalid range
        
        // When
        val result = getLogHistoryUseCase.getLogsBySymptoms(userId, symptoms, startDate, endDate)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertIs<AppError.ValidationError>(error)
        assertTrue(error.message.contains("Start date cannot be after end date"))
    }
    
    @Test
    fun `getLogCount should return total log count`() = runTest {
        // Given
        val userId = "test-user-1"
        val expectedCount = 42
        mockLogRepository.getLogCountResult = Result.success(expectedCount)
        
        // When
        val result = getLogHistoryUseCase.getLogCount(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedCount, result.getOrNull())
        assertEquals(1, mockLogRepository.getLogCountCallCount)
    }
    
    @Test
    fun `getLogCount should fail when user ID is blank`() = runTest {
        // Given
        val userId = ""
        
        // When
        val result = getLogHistoryUseCase.getLogCount(userId)
        
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

