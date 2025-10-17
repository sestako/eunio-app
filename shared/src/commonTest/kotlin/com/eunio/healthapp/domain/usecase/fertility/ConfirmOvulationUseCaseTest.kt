package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.CervicalMucus
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfirmOvulationUseCaseTest {
    
    private val mockLogRepository = MockLogRepository()
    private val mockCycleRepository = MockCycleRepository()
    private val confirmOvulationUseCase = ConfirmOvulationUseCase(mockLogRepository, mockCycleRepository)
    
    @Test
    fun `invoke should return no confirmation for insufficient data`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        val confirmation = (result as Result.Success).data
        assertEquals(false, confirmation.isConfirmed)
        assertEquals(0.0, confirmation.confidence)
        assertEquals(null, confirmation.ovulationDate)
        assertTrue(confirmation.recommendations.any { it.contains("Insufficient data") })
    }
    
    @Test
    fun `invoke should confirm ovulation with high confidence when all indicators align`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val ovulationDate = LocalDate(2024, 1, 14)
        
        // Create comprehensive fertility data with all indicators pointing to same ovulation date
        val logs = listOf(
            // BBT pattern - low temps before ovulation, rise after
            createLogWithBBT(userId, LocalDate(2024, 1, 12), 36.1),
            createLogWithBBT(userId, LocalDate(2024, 1, 13), 36.2),
            createLogWithBBT(userId, LocalDate(2024, 1, 14), 36.0),
            // BBT rise after ovulation (ovulation on day 14, rise starts day 15)
            createLogWithBBT(userId, LocalDate(2024, 1, 15), 36.5),
            createLogWithBBT(userId, LocalDate(2024, 1, 16), 36.6),
            createLogWithBBT(userId, LocalDate(2024, 1, 17), 36.7),
            
            // Cervical mucus pattern (peak on day 14, drying after)
            createLogWithMucus(userId, LocalDate(2024, 1, 12), CervicalMucus.CREAMY),
            createLogWithMucus(userId, LocalDate(2024, 1, 13), CervicalMucus.WATERY),
            createLogWithMucus(userId, LocalDate(2024, 1, 14), CervicalMucus.EGG_WHITE), // Peak mucus
            createLogWithMucus(userId, LocalDate(2024, 1, 15), CervicalMucus.STICKY), // Drying pattern
            
            // OPK pattern - surge on day 13 so ovulation predicted for day 14
            createLogWithOPK(userId, LocalDate(2024, 1, 11), OPKResult.NEGATIVE),
            createLogWithOPK(userId, LocalDate(2024, 1, 12), OPKResult.POSITIVE),
            createLogWithOPK(userId, LocalDate(2024, 1, 13), OPKResult.PEAK), // LH surge
            createLogWithOPK(userId, LocalDate(2024, 1, 14), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        val confirmation = (result as Result.Success).data
        assertEquals(true, confirmation.isConfirmed)
        assertTrue(confirmation.confidence >= 0.6)
        assertEquals(ovulationDate, confirmation.ovulationDate)
        assertTrue(confirmation.supportingIndicators.size >= 2)
        assertTrue(mockCycleRepository.confirmedOvulations.containsKey(cycleId))
    }
    
    @Test
    fun `invoke should provide moderate confidence with partial indicator alignment`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create data with only BBT and mucus indicators (no OPK)
        val logs = listOf(
            // BBT pattern
            createLogWithBBT(userId, LocalDate(2024, 1, 12), 36.2),
            createLogWithBBT(userId, LocalDate(2024, 1, 13), 36.1),
            createLogWithBBT(userId, LocalDate(2024, 1, 14), 36.3),
            createLogWithBBT(userId, LocalDate(2024, 1, 15), 36.6), // Temperature rise
            createLogWithBBT(userId, LocalDate(2024, 1, 16), 36.7),
            createLogWithBBT(userId, LocalDate(2024, 1, 17), 36.8),
            
            // Cervical mucus pattern
            createLogWithMucus(userId, LocalDate(2024, 1, 13), CervicalMucus.WATERY),
            createLogWithMucus(userId, LocalDate(2024, 1, 14), CervicalMucus.EGG_WHITE),
            createLogWithMucus(userId, LocalDate(2024, 1, 15), CervicalMucus.STICKY)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        assertTrue(result.data.confidence > 0.3)
        assertTrue(result.data.supportingIndicators.size >= 1)
    }
    
    @Test
    fun `invoke should detect BBT temperature shift pattern correctly`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create clear BBT shift pattern
        val logs = listOf(
            createLogWithBBT(userId, LocalDate(2024, 1, 10), 36.1),
            createLogWithBBT(userId, LocalDate(2024, 1, 11), 36.2),
            createLogWithBBT(userId, LocalDate(2024, 1, 12), 36.0), // Pre-ovulation low
            createLogWithBBT(userId, LocalDate(2024, 1, 13), 36.5), // Rise starts
            createLogWithBBT(userId, LocalDate(2024, 1, 14), 36.6), // Sustained rise
            createLogWithBBT(userId, LocalDate(2024, 1, 15), 36.7), // Continued high
            createLogWithBBT(userId, LocalDate(2024, 1, 16), 36.8)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        val confirmation = (result as Result.Success).data
        assertTrue(confirmation.supportingIndicators.any { it.contains("BBT") && it.contains("temperature rise") })
        assertEquals(LocalDate(2024, 1, 12), confirmation.ovulationDate) // Day before temp rise
    }
    
    @Test
    fun `invoke should detect cervical mucus peak and drying pattern`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create clear mucus pattern
        val logs = listOf(
            createLogWithMucus(userId, LocalDate(2024, 1, 12), CervicalMucus.STICKY),
            createLogWithMucus(userId, LocalDate(2024, 1, 13), CervicalMucus.CREAMY),
            createLogWithMucus(userId, LocalDate(2024, 1, 14), CervicalMucus.WATERY),
            createLogWithMucus(userId, LocalDate(2024, 1, 15), CervicalMucus.EGG_WHITE), // Peak
            createLogWithMucus(userId, LocalDate(2024, 1, 16), CervicalMucus.STICKY), // Drying
            createLogWithMucus(userId, LocalDate(2024, 1, 17), CervicalMucus.DRY)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        val confirmation = (result as Result.Success).data
        assertTrue(confirmation.supportingIndicators.any { it.contains("Cervical Mucus") && it.contains("Peak fertile mucus") })
        assertEquals(LocalDate(2024, 1, 15), confirmation.ovulationDate)
    }
    
    @Test
    fun `invoke should detect OPK surge and predict ovulation timing`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val surgeDate = LocalDate(2024, 1, 14)
        
        // Create OPK surge pattern
        val logs = listOf(
            createLogWithOPK(userId, LocalDate(2024, 1, 12), OPKResult.NEGATIVE),
            createLogWithOPK(userId, LocalDate(2024, 1, 13), OPKResult.NEGATIVE),
            createLogWithOPK(userId, surgeDate, OPKResult.PEAK), // LH surge
            createLogWithOPK(userId, LocalDate(2024, 1, 15), OPKResult.NEGATIVE),
            createLogWithOPK(userId, LocalDate(2024, 1, 16), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        assertTrue(result.data.supportingIndicators.any { it.contains("OPK") && it.contains("Peak LH surge") })
        assertEquals(surgeDate.plus(DatePeriod(days = 1)), result.data.ovulationDate) // Ovulation day after surge
    }
    
    @Test
    fun `invoke should handle conflicting indicators and find consensus`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        
        // Create slightly conflicting data (1-2 days apart)
        val logs = listOf(
            // BBT suggests ovulation on day 14
            createLogWithBBT(userId, LocalDate(2024, 1, 13), 36.2),
            createLogWithBBT(userId, LocalDate(2024, 1, 14), 36.6),
            createLogWithBBT(userId, LocalDate(2024, 1, 15), 36.7),
            
            // Mucus suggests ovulation on day 15
            createLogWithMucus(userId, LocalDate(2024, 1, 14), CervicalMucus.WATERY),
            createLogWithMucus(userId, LocalDate(2024, 1, 15), CervicalMucus.EGG_WHITE),
            createLogWithMucus(userId, LocalDate(2024, 1, 16), CervicalMucus.STICKY),
            
            // OPK suggests ovulation on day 15 (surge on day 14)
            createLogWithOPK(userId, LocalDate(2024, 1, 14), OPKResult.PEAK),
            createLogWithOPK(userId, LocalDate(2024, 1, 15), OPKResult.NEGATIVE)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        // Should find consensus around day 14-15
        assertTrue(result.data.ovulationDate in listOf(LocalDate(2024, 1, 14), LocalDate(2024, 1, 15)))
        assertTrue(result.data.confidence > 0.5) // Should still have reasonable confidence
    }
    
    @Test
    fun `invoke should update cycle with confirmed ovulation when confidence is high`() = runTest {
        // Given
        val userId = "test-user"
        val cycleId = "cycle-1"
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 31)
        val expectedOvulationDate = LocalDate(2024, 1, 15)
        
        // Create high-confidence data
        val logs = listOf(
            createLogWithBBT(userId, LocalDate(2024, 1, 14), 36.2),
            createLogWithBBT(userId, LocalDate(2024, 1, 15), 36.6),
            createLogWithBBT(userId, LocalDate(2024, 1, 16), 36.7),
            createLogWithMucus(userId, LocalDate(2024, 1, 14), CervicalMucus.EGG_WHITE),
            createLogWithMucus(userId, LocalDate(2024, 1, 15), CervicalMucus.STICKY),
            createLogWithOPK(userId, LocalDate(2024, 1, 14), OPKResult.PEAK)
        )
        mockLogRepository.fertilityLogs = logs
        
        // When
        val result = confirmOvulationUseCase(userId, cycleId, startDate, endDate)
        
        // Then
        assertIs<Result.Success<OvulationConfirmation>>(result)
        assertTrue(result.data.isConfirmed)
        assertEquals(expectedOvulationDate, mockCycleRepository.confirmedOvulations[cycleId])
    }
    
    private fun createLogWithBBT(userId: String, date: LocalDate, bbt: Double): DailyLog {
        return DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            bbt = bbt,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    private fun createLogWithMucus(userId: String, date: LocalDate, mucus: CervicalMucus): DailyLog {
        return DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            cervicalMucus = mucus,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    private fun createLogWithOPK(userId: String, date: LocalDate, opk: OPKResult): DailyLog {
        return DailyLog(
            id = "${userId}_${date}",
            userId = userId,
            date = date,
            opkResult = opk,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    private class MockLogRepository : LogRepository {
        var fertilityLogs = emptyList<DailyLog>()
        
        override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
            return Result.success(fertilityLogs)
        }
        
        // Other methods not used in these tests
        override suspend fun saveDailyLog(log: DailyLog): Result<Unit> = Result.success(Unit)
        override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> = Result.success(null)
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getLogCount(userId: String): Result<Int> = Result.success(0)
        override suspend fun getLogsBySymptoms(userId: String, symptoms: List<com.eunio.healthapp.domain.model.Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> = Result.success(emptyList())
    }
    
    private class MockCycleRepository : CycleRepository {
        val confirmedOvulations = mutableMapOf<String, LocalDate>()
        
        override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> {
            confirmedOvulations[cycleId] = ovulationDate
            return Result.success(Unit)
        }
        
        // Other methods not used in these tests
        override suspend fun getCurrentCycle(userId: String): Result<Cycle?> = Result.success(null)
        override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> = Result.success(emptyList())
        override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> = Result.error(AppError.UnknownError("Not implemented"))
        override suspend fun updateCycle(cycle: Cycle): Result<Unit> = Result.success(Unit)
        override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getCyclesInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Cycle>> = Result.success(emptyList())
        override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> = Result.success(null)
        override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> = Result.success(null)
    }
}