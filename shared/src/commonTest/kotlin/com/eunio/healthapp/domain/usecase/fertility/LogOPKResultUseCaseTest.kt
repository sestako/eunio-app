package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
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

class LogOPKResultUseCaseTest {
    
    private val mockLogRepository = MockLogRepository()
    private val logOPKResultUseCase = LogOPKResultUseCase(mockLogRepository)
    
    @Test
    fun `invoke should successfully log OPK result`() = runTest {
        // Given
        val userId = "test-user"
        val date = LocalDate(2024, 1, 15)
        val opkResult = OPKResult.POSITIVE
        
        // When
        val result = logOPKResultUseCase(userId, date, opkResult)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertTrue(mockLogRepository.savedLogs.isNotEmpty())
        assertEquals(opkResult, mockLogRepository.savedLogs.first().opkResult)
    }
    
    @Test
    fun `invoke should fail for future date`() = runTest {
        // Given
        val userId = "test-user"
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val futureDate = today.plus(DatePeriod(days = 1))
        val opkResult = OPKResult.NEGATIVE
        
        // When
        val result = logOPKResultUseCase(userId, futureDate, opkResult)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("Cannot log OPK result for future dates") == true)
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
            opkResult = OPKResult.NEGATIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        mockLogRepository.existingLogs[date] = existingLog
        
        val newOpkResult = OPKResult.POSITIVE
        
        // When
        val result = logOPKResultUseCase(userId, date, newOpkResult)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        assertEquals(newOpkResult, mockLogRepository.savedLogs.first().opkResult)
        assertEquals(existingLog.id, mockLogRepository.savedLogs.first().id)
    }
    
    @Test
    fun `analyzeOPKPattern should return insufficient data for empty logs`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = logOPKResultUseCase.analyzeOPKPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OPKPatternAnalysis>>(result)
        val analysis = (result as Result.Success).data
        assertEquals(0, analysis.totalTests)
        assertEquals(false, analysis.surgeDetected)
        assertTrue(analysis.recommendations.any { it.contains("Start OPK testing") })
    }
    
    @Test
    fun `analyzeOPKPattern should detect LH surge from peak result`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val surgeDate = LocalDate(2024, 1, 15)
        
        val logs = listOf(
            createOPKLog(userId, LocalDate(2024, 1, 12), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 13), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 14), OPKResult.POSITIVE),
            createOPKLog(userId, surgeDate, OPKResult.PEAK),
            createOPKLog(userId, LocalDate(2024, 1, 16), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logOPKResultUseCase.analyzeOPKPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OPKPatternAnalysis>>(result)
        val analysis = (result as Result.Success).data
        assertEquals(5, analysis.totalTests)
        assertEquals(true, analysis.surgeDetected)
        assertTrue(analysis.surgeDates.contains(surgeDate))
        assertTrue(analysis.predictedOvulationDates.contains(surgeDate.plus(DatePeriod(days = 1))))
        assertTrue(analysis.recommendations.any { it.contains("LH surge") })
    }
    
    @Test
    fun `analyzeOPKPattern should detect LH surge from negative to positive transition`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val surgeDate = LocalDate(2024, 1, 15)
        
        val logs = listOf(
            createOPKLog(userId, LocalDate(2024, 1, 12), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 13), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 14), OPKResult.NEGATIVE),
            createOPKLog(userId, surgeDate, OPKResult.POSITIVE), // Transition from negative to positive
            createOPKLog(userId, LocalDate(2024, 1, 16), OPKResult.POSITIVE),
            createOPKLog(userId, LocalDate(2024, 1, 17), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logOPKResultUseCase.analyzeOPKPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OPKPatternAnalysis>>(result)
        val analysis = (result as Result.Success).data
        assertEquals(true, analysis.surgeDetected)
        assertTrue(analysis.surgeDates.contains(surgeDate))
    }
    
    @Test
    fun `analyzeOPKPattern should detect multiple surges and provide appropriate recommendations`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        val logs = listOf(
            createOPKLog(userId, LocalDate(2024, 1, 10), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 11), OPKResult.POSITIVE), // First surge
            createOPKLog(userId, LocalDate(2024, 1, 12), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 13), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 14), OPKResult.POSITIVE), // Second surge (close together)
            createOPKLog(userId, LocalDate(2024, 1, 15), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logOPKResultUseCase.analyzeOPKPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OPKPatternAnalysis>>(result)
        val analysis = (result as Result.Success).data
        assertEquals(true, analysis.surgeDetected)
        assertEquals(2, analysis.surgeDates.size)
        assertTrue(analysis.recommendations.any { it.contains("Multiple LH surges detected close together") })
        assertTrue(analysis.recommendations.any { it.contains("PCOS") })
    }
    
    @Test
    fun `analyzeOPKPattern should handle no surge detected`() = runTest {
        // Given
        val userId = "test-user"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        val logs = listOf(
            createOPKLog(userId, LocalDate(2024, 1, 10), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 11), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 12), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 13), OPKResult.NEGATIVE),
            createOPKLog(userId, LocalDate(2024, 1, 14), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = logOPKResultUseCase.analyzeOPKPattern(userId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OPKPatternAnalysis>>(result)
        val analysis = (result as Result.Success).data
        assertEquals(false, analysis.surgeDetected)
        assertTrue(analysis.surgeDates.isEmpty())
        assertTrue(analysis.recommendations.any { it.contains("No LH surge detected") })
    }
    
    @Test
    fun `getResultDescription should provide educational information for each result type`() {
        // Test all OPK result types
        val testCases = mapOf(
            OPKResult.NEGATIVE to "No LH surge detected",
            OPKResult.POSITIVE to "LH surge detected",
            OPKResult.PEAK to "Peak LH surge detected"
        )
        
        testCases.forEach { (result, expectedContent) ->
            // When
            val description = logOPKResultUseCase.getResultDescription(result)
            
            // Then
            assertTrue(description.contains(expectedContent))
        }
    }
    
    @Test
    fun `calculateOptimalTestingWindow should provide appropriate testing window for different cycle lengths`() {
        // Test different cycle lengths
        val testCases = mapOf(
            21 to Pair(8, 11),   // Short cycle
            28 to Pair(11, 18),  // Average cycle
            35 to Pair(18, 25)   // Long cycle
        )
        
        testCases.forEach { (cycleLength, expectedWindow) ->
            // When
            val result = logOPKResultUseCase.calculateOptimalTestingWindow(cycleLength)
            
            // Then
            assertEquals(expectedWindow.first, result.first)
            assertEquals(expectedWindow.second, result.second)
        }
    }
    
    @Test
    fun `calculateOptimalTestingWindow should not start before day 8`() {
        // Given - very short cycle
        val shortCycle = 15
        
        // When
        val result = logOPKResultUseCase.calculateOptimalTestingWindow(shortCycle)
        
        // Then
        assertTrue(result.first >= 8) // Should not start before day 8
    }
    
    private fun createOPKLog(userId: String, date: LocalDate, opkResult: OPKResult): DailyLog {
        return DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            opkResult = opkResult,
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