package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LogBBTUseCaseTest {
    
    private val mockLogRepository = MockLogRepository()
    private val logBBTUseCase = LogBBTUseCase(mockLogRepository)
    
    @Test
    fun `invoke should successfully log BBT for valid temperature`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val temperature = 36.5
        
        // When
        val result = logBBTUseCase(userId, date, temperature)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertTrue(mockLogRepository.savedLogs.isNotEmpty())
        assertEquals(temperature, mockLogRepository.savedLogs.first().bbt)
    }
    
    @Test
    fun `invoke should fail for temperature below minimum threshold`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val temperature = 34.0 // Below 35.0°C threshold
        
        // When
        val result = logBBTUseCase(userId, date, temperature)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("BBT cannot be below 35.0°C") == true)
    }
    
    @Test
    fun `invoke should fail for temperature above maximum threshold`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val temperature = 41.0 // Above 40.0°C threshold
        
        // When
        val result = logBBTUseCase(userId, date, temperature)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("BBT cannot be above 40.0°C") == true)
    }
    
    @Test
    fun `invoke should fail for future date`() = runTest {
        // Given
        val userId = "test-user"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val futureDate = today.plus(DatePeriod(days = 1))
        val temperature = 36.5
        
        // When
        val result = logBBTUseCase(userId, futureDate, temperature)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("Cannot log BBT for future dates") == true)
    }
    
    @Test
    fun `invoke should update existing log when log already exists for date`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val existingLog = DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            bbt = 36.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        mockLogRepository.existingLogs[date] = existingLog
        
        val newTemperature = 36.8
        
        // When
        val result = logBBTUseCase(userId, date, newTemperature)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertEquals(newTemperature, mockLogRepository.savedLogs.first().bbt)
        assertEquals(existingLog.id, mockLogRepository.savedLogs.first().id)
    }
    
    @Test
    fun `validateBBT should return success for normal temperature range`() {
        // Given
        val normalTemperatures = listOf(36.0, 36.5, 37.0, 37.5)
        
        normalTemperatures.forEach { temperature ->
            // When
            val result = logBBTUseCase.validateBBT(temperature)
            
            // Then
            assertIs<ValidationResult.Success>(result)
        }
    }
    
    @Test
    fun `validateBBT should return error for temperatures with excessive precision`() {
        // Given
        val temperature = 36.123 // Too many decimal places
        
        // When
        val result = logBBTUseCase.validateBBT(temperature)
        
        // Then
        assertIs<ValidationResult.Error>(result)
        assertTrue(result.errors.any { it.contains("0.1°C precision") })
    }
    
    @Test
    fun `validateBBT should provide warnings for unusual but not invalid temperatures`() {
        // Given
        val lowTemperature = 35.8 // Below normal but above minimum
        val highTemperature = 38.2 // Above normal but below maximum
        
        // When
        val lowResult = logBBTUseCase.validateBBT(lowTemperature)
        val highResult = logBBTUseCase.validateBBT(highTemperature)
        
        // Then
        assertIs<ValidationResult.Error>(lowResult)
        assertTrue(lowResult.errors.any { it.contains("unusually low") })
        
        assertIs<ValidationResult.Error>(highResult)
        assertTrue(highResult.errors.any { it.contains("may indicate fever") })
    }
    
    @Test
    fun `fahrenheitToCelsius should correctly convert temperature`() {
        // Given
        val fahrenheit = 98.6
        val expectedCelsius = 37.0
        
        // When
        val result = logBBTUseCase.fahrenheitToCelsius(fahrenheit)
        
        // Then
        assertEquals(expectedCelsius, result, 0.1)
    }
    
    @Test
    fun `validateMeasurementTiming should provide guidance for optimal timing`() {
        // Given
        val optimalHour = 7 // 7 AM - optimal time
        val lateHour = 11 // 11 AM - too late
        val earlyHour = 3 // 3 AM - too early
        
        // When
        val optimalResult = logBBTUseCase.validateMeasurementTiming(optimalHour)
        val lateResult = logBBTUseCase.validateMeasurementTiming(lateHour)
        val earlyResult = logBBTUseCase.validateMeasurementTiming(earlyHour)
        
        // Then
        assertIs<ValidationResult.Success>(optimalResult)
        
        assertIs<ValidationResult.Error>(lateResult)
        assertTrue(lateResult.errors.any { it.contains("after 10:00 AM") })
        
        assertIs<ValidationResult.Error>(earlyResult)
        assertTrue(earlyResult.errors.any { it.contains("between 5:00-9:00 AM") })
    }
    
    private class MockLogRepository : LogRepository {
        val savedLogs = mutableListOf<DailyLog>()
        val existingLogs = mutableMapOf<LocalDate, DailyLog>()
        
        override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
            savedLogs.add(log)
            return Result.success(Unit)
        }
        
        override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
            return Result.success(existingLogs[date])
        }
        
        // Other methods not used in these tests
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getLogCount(userId: String): Result<Int> = Result.success(0)
        override suspend fun getLogsBySymptoms(userId: String, symptoms: List<com.eunio.healthapp.domain.model.Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> = Result.success(emptyList())
    }
}