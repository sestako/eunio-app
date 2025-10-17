package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CalculateFertilityWindowUseCaseTest {
    
    private val mockLogRepository = MockLogRepository()
    private val mockCycleRepository = MockCycleRepository()
    private val calculateFertilityWindowUseCase = CalculateFertilityWindowUseCase(mockCycleRepository, mockLogRepository)
    
    @Test
    fun `invoke should fail when no active cycle exists`() = runTest {
        // Given
        val userId = "test-user"
        mockCycleRepository.currentCycle = null
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("No active cycle found") == true)
    }
    
    @Test
    fun `invoke should calculate fertility window for current cycle with default values`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = emptyList()
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        val fertilityWindow = result.data
        
        assertEquals(cycleStartDate, fertilityWindow.cycleStartDate)
        assertEquals(28, fertilityWindow.averageCycleLength) // Default cycle length
        assertEquals(LocalDate(2024, 1, 14), fertilityWindow.predictedOvulationDate) // Day 14 for 28-day cycle
        assertEquals(LocalDate(2024, 1, 9), fertilityWindow.fertilityWindowStart) // 5 days before ovulation
        assertEquals(LocalDate(2024, 1, 14), fertilityWindow.fertilityWindowEnd) // Ovulation day
        assertEquals(LocalDate(2024, 1, 13), fertilityWindow.peakFertilityDate) // Day before ovulation
    }
    
    @Test
    fun `invoke should calculate fertility window based on cycle history`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        
        // Create cycle history with 30-day cycles and ovulation on day 16
        val cycleHistory = listOf(
            Cycle(
                id = "cycle-2",
                userId = userId,
                startDate = LocalDate(2023, 12, 1),
                endDate = LocalDate(2023, 12, 30),
                predictedOvulationDate = null,
                confirmedOvulationDate = LocalDate(2023, 12, 16),
                cycleLength = 30,
                lutealPhaseLength = 14
            ),
            Cycle(
                id = "cycle-3",
                userId = userId,
                startDate = LocalDate(2023, 11, 1),
                endDate = LocalDate(2023, 11, 30),
                predictedOvulationDate = null,
                confirmedOvulationDate = LocalDate(2023, 11, 16),
                cycleLength = 30,
                lutealPhaseLength = 14
            )
        )
        
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        val fertilityWindow = result.data
        
        assertEquals(30, fertilityWindow.averageCycleLength)
        assertEquals(LocalDate(2024, 1, 16), fertilityWindow.predictedOvulationDate) // Day 16 based on history
        assertEquals(LocalDate(2024, 1, 11), fertilityWindow.fertilityWindowStart) // 5 days before
        assertEquals(LocalDate(2024, 1, 16), fertilityWindow.fertilityWindowEnd) // Ovulation day
    }
    
    @Test
    fun `invoke should calculate daily conception probabilities correctly`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = emptyList()
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        val probabilities = result.data.dailyConceptionProbabilities
        
        // Check that probabilities are assigned correctly
        assertEquals(0.10, probabilities[LocalDate(2024, 1, 9)]) // 5 days before ovulation
        assertEquals(0.16, probabilities[LocalDate(2024, 1, 10)]) // 4 days before
        assertEquals(0.14, probabilities[LocalDate(2024, 1, 11)]) // 3 days before
        assertEquals(0.20, probabilities[LocalDate(2024, 1, 12)]) // 2 days before
        assertEquals(0.25, probabilities[LocalDate(2024, 1, 13)]) // 1 day before (peak)
        assertEquals(0.15, probabilities[LocalDate(2024, 1, 14)]) // Ovulation day
    }
    
    @Test
    fun `invoke should calculate high confidence for regular cycles`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        
        // Create very regular cycle history (all 28 days)
        val cycleHistory = (1..6).map { i ->
            Cycle(
                id = "cycle-$i",
                userId = userId,
                startDate = LocalDate(2023, 12 - i, 1),
                endDate = LocalDate(2023, 12 - i, 28),
                predictedOvulationDate = null,
                confirmedOvulationDate = null,
                cycleLength = 28,
                lutealPhaseLength = 14
            )
        }
        
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        assertTrue(result.data.confidence >= 0.8) // High confidence for regular cycles
        assertTrue(result.data.recommendations.any { it.contains("very regular") })
    }
    
    @Test
    fun `invoke should calculate low confidence for irregular cycles`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        
        // Create very irregular cycle history (varying lengths)
        val cycleHistory = listOf(
            createCycle("cycle-2", userId, 18),
            createCycle("cycle-3", userId, 45),
            createCycle("cycle-4", userId, 22),
            createCycle("cycle-5", userId, 50),
            createCycle("cycle-6", userId, 15)
        )
        
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        val fertilityWindow = (result as Result.Success).data
        assertTrue(fertilityWindow.confidence <= 0.5) // Low confidence for irregular cycles
        assertTrue(fertilityWindow.recommendations.any { it.contains("variability") || it.contains("irregular") })
    }
    
    @Test
    fun `calculateForFutureCycle should calculate fertility window for future cycle`() = runTest {
        // Given
        val userId = "test-user"
        val futureCycleStart = LocalDate(2024, 2, 1)
        
        val cycleHistory = listOf(
            createCycle("cycle-1", userId, 30),
            createCycle("cycle-2", userId, 28),
            createCycle("cycle-3", userId, 29)
        )
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase.calculateForFutureCycle(userId, futureCycleStart)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        val fertilityWindow = result.data
        
        assertEquals(futureCycleStart, fertilityWindow.cycleStartDate)
        assertEquals(29, fertilityWindow.averageCycleLength) // Average of 30, 28, 29
        assertTrue(fertilityWindow.predictedOvulationDate.dayOfMonth >= 14) // Should be around day 15 (29-14)
    }
    
    @Test
    fun `calculateForFutureCycle should fail with insufficient cycle history`() = runTest {
        // Given
        val userId = "test-user"
        val futureCycleStart = LocalDate(2024, 2, 1)
        mockCycleRepository.cycleHistory = emptyList()
        
        // When
        val result = calculateFertilityWindowUseCase.calculateForFutureCycle(userId, futureCycleStart)
        
        // Then
        assertIs<Result.Error>(result)
        assertIs<AppError.ValidationError>(result.error)
        assertTrue(result.error.message?.contains("Insufficient cycle history") == true)
    }
    
    @Test
    fun `invoke should provide appropriate recommendations for short cycles`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        
        // Create short cycle history
        val cycleHistory = listOf(
            createCycle("cycle-2", userId, 20),
            createCycle("cycle-3", userId, 19),
            createCycle("cycle-4", userId, 21)
        )
        
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        assertTrue(result.data.recommendations.any { it.contains("shorter than average") })
        assertTrue(result.data.recommendations.any { it.contains("healthcare provider") })
    }
    
    @Test
    fun `invoke should provide appropriate recommendations for long cycles`() = runTest {
        // Given
        val userId = "test-user"
        val cycleStartDate = LocalDate(2024, 1, 1)
        val currentCycle = Cycle(
            id = "cycle-1",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        
        // Create long cycle history
        val cycleHistory = listOf(
            createCycle("cycle-2", userId, 38),
            createCycle("cycle-3", userId, 40),
            createCycle("cycle-4", userId, 36)
        )
        
        mockCycleRepository.currentCycle = currentCycle
        mockCycleRepository.cycleHistory = cycleHistory
        
        // When
        val result = calculateFertilityWindowUseCase(userId)
        
        // Then
        assertIs<Result.Success<FertilityWindow>>(result)
        assertTrue(result.data.recommendations.any { it.contains("longer than average") })
        assertTrue(result.data.recommendations.any { it.contains("ovulation may be irregular") })
    }
    
    private fun createCycle(id: String, userId: String, cycleLength: Int): Cycle {
        val startDate = LocalDate(2023, 12, 1)
        return Cycle(
            id = id,
            userId = userId,
            startDate = startDate,
            endDate = startDate.plus(DatePeriod(days = cycleLength - 1)),
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = cycleLength,
            lutealPhaseLength = 14
        )
    }
    
    private class MockLogRepository : LogRepository {
        // Not used in fertility window calculation tests
        override suspend fun saveDailyLog(log: DailyLog): Result<Unit> = Result.success(Unit)
        override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> = Result.success(null)
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
        override suspend fun getLogCount(userId: String): Result<Int> = Result.success(0)
        override suspend fun getLogsBySymptoms(userId: String, symptoms: List<com.eunio.healthapp.domain.model.Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> = Result.success(emptyList())
    }
    
    private class MockCycleRepository : CycleRepository {
        var currentCycle: Cycle? = null
        var cycleHistory: List<Cycle> = emptyList()
        
        override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
            return Result.success(currentCycle)
        }
        
        override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
            return Result.success(cycleHistory.take(limit))
        }
        
        // Other methods not used in these tests
        override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> = Result.error(AppError.UnknownError("Not implemented"))
        override suspend fun updateCycle(cycle: Cycle): Result<Unit> = Result.success(Unit)
        override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> = Result.success(Unit)
        override suspend fun getCyclesInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Cycle>> = Result.success(emptyList())
        override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> = Result.success(null)
        override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> = Result.success(null)
        override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> = Result.success(Unit)
    }
}