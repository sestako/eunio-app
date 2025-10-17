package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.CervicalMucus
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
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

class LogCervicalMucusUseCaseTest {
    
    private val mockLogRepository = MockLogRepository()
    private val logCervicalMucusUseCase = LogCervicalMucusUseCase(mockLogRepository)
    
    @Test
    fun `invoke should successfully log cervical mucus`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val mucusType = CervicalMucus.EGG_WHITE
        
        // When
        val result = logCervicalMucusUseCase(userId, date, mucusType)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertTrue(mockLogRepository.savedLogs.isNotEmpty())
        assertEquals(mucusType, mockLogRepository.savedLogs.first().cervicalMucus)
    }
    
    @Test
    fun `invoke should fail for future date`() = runTest {
        // Given
        val userId = "test-user"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val futureDate = today.plus(DatePeriod(days = 1))
        val mucusType = CervicalMucus.CREAMY
        
        // When
        val result = logCervicalMucusUseCase(userId, futureDate, mucusType)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("Cannot log cervical mucus for future dates") == true)
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
            cervicalMucus = CervicalMucus.DRY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        mockLogRepository.existingLogs[date] = existingLog
        
        val newMucusType = CervicalMucus.EGG_WHITE
        
        // When
        val result = logCervicalMucusUseCase(userId, date, newMucusType)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertEquals(newMucusType, mockLogRepository.savedLogs.first().cervicalMucus)
        assertEquals(existingLog.id, mockLogRepository.savedLogs.first().id)
    }
    
    @Test
    fun `categorizeFertilityPotential should correctly categorize mucus types`() {
        // Test all mucus types and their expected fertility indicators
        val testCases = mapOf(
            CervicalMucus.DRY to FertilityIndicator.LOW,
            CervicalMucus.STICKY to FertilityIndicator.LOW,
            CervicalMucus.CREAMY to FertilityIndicator.MODERATE,
            CervicalMucus.WATERY to FertilityIndicator.HIGH,
            CervicalMucus.EGG_WHITE to FertilityIndicator.PEAK
        )
        
        testCases.forEach { (mucusType, expectedIndicator) ->
            // When
            val result = logCervicalMucusUseCase.categorizeFertilityPotential(mucusType)
            
            // Then
            assertEquals(expectedIndicator, result)
        }
    }
    
    @Test
    fun `getMucusDescription should provide educational information`() {
        // Given
        val mucusType = CervicalMucus.EGG_WHITE
        
        // When
        val description = logCervicalMucusUseCase.getMucusDescription(mucusType)
        
        // Then
        assertTrue(description.contains("Peak fertility"))
        assertTrue(description.contains("ovulation likely"))
    }
    
    @Test
    fun `analyzeMucusPattern should return insufficient data for empty logs`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = logCervicalMucusUseCase.analyzeMucusPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<MucusPatternAnalysis>>(result)
        assertEquals(0, result.data.totalObservations)
        assertEquals(FertilityTrend.INSUFFICIENT_DATA, result.data.fertilityTrend)
        assertTrue(result.data.recommendations.any { it.contains("Continue tracking") })
    }
    
    @Test
    fun `analyzeMucusPattern should detect regular ovulation pattern`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create logs with regular ovulation pattern
        val logs = listOf(
            createDailyLog(userId, LocalDate(2024, 1, 10), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 12), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 14), CervicalMucus.CREAMY),
            createDailyLog(userId, LocalDate(2024, 1, 16), CervicalMucus.WATERY),
            createDailyLog(userId, LocalDate(2024, 1, 17), CervicalMucus.EGG_WHITE),
            createDailyLog(userId, LocalDate(2024, 1, 18), CervicalMucus.EGG_WHITE),
            createDailyLog(userId, LocalDate(2024, 1, 19), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 25), CervicalMucus.EGG_WHITE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logCervicalMucusUseCase.analyzeMucusPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<MucusPatternAnalysis>>(result)
        assertEquals(8, result.data.totalObservations)
        assertEquals(FertilityTrend.REGULAR_OVULATION, result.data.fertilityTrend)
        assertTrue(result.data.peakFertilityDays.contains(LocalDate(2024, 1, 17)))
        assertTrue(result.data.peakFertilityDays.contains(LocalDate(2024, 1, 18)))
        assertTrue(result.data.peakFertilityDays.contains(LocalDate(2024, 1, 25)))
    }
    
    @Test
    fun `analyzeMucusPattern should detect irregular ovulation pattern`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create logs with irregular pattern (some high fertility but limited peak)
        val logs = listOf(
            createDailyLog(userId, LocalDate(2024, 1, 10), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 12), CervicalMucus.WATERY),
            createDailyLog(userId, LocalDate(2024, 1, 14), CervicalMucus.CREAMY),
            createDailyLog(userId, LocalDate(2024, 1, 16), CervicalMucus.WATERY),
            createDailyLog(userId, LocalDate(2024, 1, 18), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 20), CervicalMucus.WATERY),
            createDailyLog(userId, LocalDate(2024, 1, 22), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 25), CervicalMucus.EGG_WHITE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logCervicalMucusUseCase.analyzeMucusPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<MucusPatternAnalysis>>(result)
        assertEquals(FertilityTrend.IRREGULAR_OVULATION, result.data.fertilityTrend)
        assertTrue(result.data.recommendations.any { it.contains("irregular") })
    }
    
    @Test
    fun `analyzeMucusPattern should detect anovulatory pattern`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create logs with no clear ovulation signs
        val logs = listOf(
            createDailyLog(userId, LocalDate(2024, 1, 10), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 12), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 14), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 16), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 18), CervicalMucus.CREAMY),
            createDailyLog(userId, LocalDate(2024, 1, 20), CervicalMucus.DRY),
            createDailyLog(userId, LocalDate(2024, 1, 22), CervicalMucus.STICKY),
            createDailyLog(userId, LocalDate(2024, 1, 25), CervicalMucus.DRY)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logCervicalMucusUseCase.analyzeMucusPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<MucusPatternAnalysis>>(result)
        assertEquals(FertilityTrend.ANOVULATORY_PATTERN, result.data.fertilityTrend)
        assertTrue(result.data.recommendations.any { it.contains("Limited signs of ovulation") })
    }
    
    private fun createDailyLog(userId: String, date: LocalDate, mucus: CervicalMucus): DailyLog {
        return DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            cervicalMucus = mucus,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    private class MockLogRepository : LogRepository {
        val savedLogs = mutableListOf<DailyLog>()
        val existingLogs = mutableMapOf<LocalDate, DailyLog>()
        var fertilityLogs = emptyList<DailyLog>()
        
        override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
            savedLogs.add(log)
            return Result.success(Unit)
        }
        
        override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
            return Result.success(existingLogs[date])
        }
        
        override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
            return Result.success(fertilityLogs)
        }
        
        // Other methods not used in these tests
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getLogCount(userId: String): Result<Int> = Result.success(0)
        override suspend fun getLogsBySymptoms(userId: String, symptoms: List<com.eunio.healthapp.domain.model.Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> = Result.success(emptyList())
    }
}