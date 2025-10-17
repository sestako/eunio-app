package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCycleUseCaseTest {
    private val mockCycleRepository = MockCycleRepository()
    private val mockLogRepository = MockLogRepository()
    private val useCase = UpdateCycleUseCase(mockCycleRepository, mockLogRepository)
    
    private val testUserId = "test-user-123"
    private val testDate = LocalDate(2024, 1, 15)

    @Test
    fun `completeCycle should return error when no active cycle exists`() = runTest {
        // Given: No active cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.completeCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `completeCycle should return error when end date is before start date`() = runTest {
        // Given: Active cycle that started after the proposed end date
        val cycle = createTestCycle(testUserId, testDate.plus(DatePeriod(days = 5)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.completeCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `completeCycle should return error for invalid cycle length`() = runTest {
        // Given: Active cycle with too short length (10 days)
        val cycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 9)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.completeCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `completeCycle should successfully complete cycle with valid data`() = runTest {
        // Given: Active cycle with valid 28-day length
        val cycleStart = testDate.minus(DatePeriod(days = 27)) // 28-day cycle
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.completeCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val completedCycle = result.getOrNull()
        assertNotNull(completedCycle)
        assertEquals(testDate, completedCycle.endDate)
        assertEquals(28, completedCycle.cycleLength)
    }

    @Test
    fun `completeCycle should calculate luteal phase length when ovulation is confirmed`() = runTest {
        // Given: Active cycle with confirmed ovulation
        val cycleStart = testDate.minus(DatePeriod(days = 27))
        val ovulationDate = testDate.minus(DatePeriod(days = 14)) // 14 days ago
        val cycle = createTestCycle(testUserId, cycleStart)
            .copy(confirmedOvulationDate = ovulationDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.completeCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val completedCycle = result.getOrNull()
        assertEquals(14, completedCycle?.lutealPhaseLength) // 14 days from ovulation to end
    }

    @Test
    fun `confirmOvulation should return error when no active cycle exists`() = runTest {
        // Given: No active cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.confirmOvulation(testUserId, testDate, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `confirmOvulation should return error when ovulation date is before cycle start`() = runTest {
        // Given: Active cycle that started after the proposed ovulation date
        val cycle = createTestCycle(testUserId, testDate.plus(DatePeriod(days = 5)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.confirmOvulation(testUserId, testDate, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `confirmOvulation should return error when ovulation date is in the future`() = runTest {
        // Given: Active cycle
        val cycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 10)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        val futureDate = testDate.plus(DatePeriod(days = 5))
        
        // When
        val result = useCase.confirmOvulation(testUserId, futureDate, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `confirmOvulation should successfully confirm ovulation with valid date`() = runTest {
        // Given: Active cycle
        val cycleStart = testDate.minus(DatePeriod(days = 15))
        val ovulationDate = testDate.minus(DatePeriod(days = 2))
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.confirmOvulation(testUserId, ovulationDate, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertEquals(ovulationDate, updatedCycle?.confirmedOvulationDate)
    }

    @Test
    fun `confirmOvulation should calculate luteal phase length for completed cycle`() = runTest {
        // Given: Completed cycle
        val cycleStart = testDate.minus(DatePeriod(days = 28))
        val cycleEnd = testDate.minus(DatePeriod(days = 1))
        val ovulationDate = testDate.minus(DatePeriod(days = 15))
        val cycle = createTestCycle(testUserId, cycleStart)
            .copy(endDate = cycleEnd)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.confirmOvulation(testUserId, ovulationDate, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertEquals(14, updatedCycle?.lutealPhaseLength) // 14 days from ovulation to end
    }

    @Test
    fun `updatePredictedOvulation should return error when no active cycle exists`() = runTest {
        // Given: No active cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.updatePredictedOvulation(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `updatePredictedOvulation should return error when predicted date is before cycle start`() = runTest {
        // Given: Active cycle that started after the predicted date
        val cycle = createTestCycle(testUserId, testDate.plus(DatePeriod(days = 5)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.updatePredictedOvulation(testUserId, testDate)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }

    @Test
    fun `updatePredictedOvulation should successfully update predicted ovulation`() = runTest {
        // Given: Active cycle
        val cycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 10)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        val predictedDate = testDate.plus(DatePeriod(days = 3))
        
        // When
        val result = useCase.updatePredictedOvulation(testUserId, predictedDate)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertEquals(predictedDate, updatedCycle?.predictedOvulationDate)
    }

    @Test
    fun `recalculateCycleMetrics should detect ovulation from BBT shift`() = runTest {
        // Given: Active cycle and fertility logs with BBT shift
        val cycleStart = testDate.minus(DatePeriod(days = 20))
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val bbtShiftLogs = createBBTShiftLogs(testDate.minus(DatePeriod(days = 6))) // BBT shift 6 days ago
        mockLogRepository.fertilityLogs[testUserId] = bbtShiftLogs
        
        // When
        val result = useCase.recalculateCycleMetrics(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertNotNull(updatedCycle?.confirmedOvulationDate)
        assertEquals(testDate.minus(DatePeriod(days = 7)), updatedCycle?.confirmedOvulationDate) // Day before BBT shift
    }

    @Test
    fun `recalculateCycleMetrics should detect ovulation from OPK peak`() = runTest {
        // Given: Active cycle and fertility logs with OPK peak
        val cycleStart = testDate.minus(DatePeriod(days = 20))
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val opkPeakDate = testDate.minus(DatePeriod(days = 8))
        val fertilityLogs = listOf(
            createTestDailyLog(opkPeakDate, opkResult = OPKResult.PEAK)
        )
        mockLogRepository.fertilityLogs[testUserId] = fertilityLogs
        
        // When
        val result = useCase.recalculateCycleMetrics(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertEquals(testDate.minus(DatePeriod(days = 7)), updatedCycle?.confirmedOvulationDate) // Day after OPK peak
    }

    @Test
    fun `recalculateCycleMetrics should not overwrite existing confirmed ovulation`() = runTest {
        // Given: Active cycle with already confirmed ovulation
        val cycleStart = testDate.minus(DatePeriod(days = 20))
        val existingOvulation = testDate.minus(DatePeriod(days = 10))
        val cycle = createTestCycle(testUserId, cycleStart)
            .copy(confirmedOvulationDate = existingOvulation)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val bbtShiftLogs = createBBTShiftLogs(testDate.minus(DatePeriod(days = 6)))
        mockLogRepository.fertilityLogs[testUserId] = bbtShiftLogs
        
        // When
        val result = useCase.recalculateCycleMetrics(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedCycle = result.getOrNull()
        assertEquals(existingOvulation, updatedCycle?.confirmedOvulationDate) // Should not change
    }

    private fun createTestCycle(userId: String, startDate: LocalDate): Cycle {
        return Cycle(
            id = "cycle-${startDate}",
            userId = userId,
            startDate = startDate
        )
    }

    private fun createTestDailyLog(
        date: LocalDate,
        bbt: Double? = null,
        opkResult: OPKResult? = null
    ): DailyLog {
        return DailyLog(
            id = "log-${date}",
            userId = testUserId,
            date = date,
            bbt = bbt,
            opkResult = opkResult,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }

    private fun createBBTShiftLogs(shiftDate: LocalDate): List<DailyLog> {
        val logs = mutableListOf<DailyLog>()
        
        // Pre-ovulation temperatures (lower)
        for (i in 6 downTo 1) {
            logs.add(createTestDailyLog(shiftDate.minus(DatePeriod(days = i)), bbt = 97.2 + (i * 0.05)))
        }
        
        // Post-ovulation temperatures (higher) - sustained rise
        for (i in 0..3) {
            logs.add(createTestDailyLog(shiftDate.plus(DatePeriod(days = i)), bbt = 97.8 + (i * 0.02)))
        }
        
        return logs
    }
}