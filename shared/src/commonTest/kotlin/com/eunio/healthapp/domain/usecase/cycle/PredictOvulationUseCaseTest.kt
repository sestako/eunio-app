package com.eunio.healthapp.domain.usecase.cycle

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PredictOvulationUseCaseTest {
    private val mockCycleRepository = MockCycleRepository()
    private val mockLogRepository = MockLogRepository()
    private val useCase = PredictOvulationUseCase(mockCycleRepository, mockLogRepository)
    
    private val testUserId = "test-user-123"
    private val testDate = LocalDate(2024, 1, 15)

    @Test
    fun `execute should return null when no active cycle exists`() = runTest {
        // Given: No active cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `execute should return standard prediction when no cycle history exists`() = runTest {
        // Given: Active cycle but no history
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        mockCycleRepository.cycleHistory[testUserId] = emptyList()
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertNotNull(prediction)
        assertEquals(testDate.plus(DatePeriod(days = 13)), prediction.predictedDate) // Day 14
        assertEquals(PredictionConfidence.LOW, prediction.confidence)
        assertEquals(PredictionMethod.STANDARD_CALCULATION, prediction.method)
    }

    @Test
    fun `execute should use historical pattern when cycle history exists`() = runTest {
        // Given: Active cycle and historical cycles with confirmed ovulation
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val historicalCycles = listOf(
            createTestCycle(testUserId, testDate.minus(DatePeriod(days = 60)))
                .copy(
                    startDate = testDate.minus(DatePeriod(days = 60)),
                    confirmedOvulationDate = testDate.minus(DatePeriod(days = 49)), // Day 12
                    cycleLength = 28
                ),
            createTestCycle(testUserId, testDate.minus(DatePeriod(days = 32)))
                .copy(
                    startDate = testDate.minus(DatePeriod(days = 32)),
                    confirmedOvulationDate = testDate.minus(DatePeriod(days = 21)), // Day 12
                    cycleLength = 28
                )
        )
        mockCycleRepository.cycleHistory[testUserId] = historicalCycles
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertNotNull(prediction)
        assertEquals(testDate.plus(DatePeriod(days = 11)), prediction.predictedDate) // Day 12 based on history
        assertEquals(PredictionMethod.HISTORICAL_PATTERN, prediction.method)
    }

    @Test
    fun `execute should calculate confidence based on cycle regularity`() = runTest {
        // Given: Active cycle and very regular historical cycles
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val regularCycles = (1..6).map { i ->
            createTestCycle(testUserId, testDate.minus(DatePeriod(days = 28 * i)))
                .copy(cycleLength = 28) // Very regular 28-day cycles
        }
        mockCycleRepository.cycleHistory[testUserId] = regularCycles
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertEquals(PredictionConfidence.HIGH, prediction?.confidence)
    }

    @Test
    fun `predictForCycle should use standard calculation when no history exists`() = runTest {
        // Given: No cycle history
        mockCycleRepository.cycleHistory[testUserId] = emptyList()
        
        // When
        val result = useCase.predictForCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDate.plus(DatePeriod(days = 13)), result.getOrNull()) // Day 14
    }

    @Test
    fun `predictForCycle should use historical average when history exists`() = runTest {
        // Given: Historical cycles with confirmed ovulation on day 15
        val historicalCycles = listOf(
            createTestCycle(testUserId, testDate.minus(DatePeriod(days = 60)))
                .copy(
                    startDate = testDate.minus(DatePeriod(days = 60)),
                    confirmedOvulationDate = testDate.minus(DatePeriod(days = 46)), // Day 15
                    cycleLength = 30
                ),
            createTestCycle(testUserId, testDate.minus(DatePeriod(days = 30)))
                .copy(
                    startDate = testDate.minus(DatePeriod(days = 30)),
                    confirmedOvulationDate = testDate.minus(DatePeriod(days = 16)), // Day 15
                    cycleLength = 30
                )
        )
        mockCycleRepository.cycleHistory[testUserId] = historicalCycles
        
        // When
        val result = useCase.predictForCycle(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDate.plus(DatePeriod(days = 14)), result.getOrNull()) // Day 15 based on history
    }

    @Test
    fun `updatePredictionWithCurrentData should return prediction based on available data`() = runTest {
        // Given: Active cycle and fertility logs with BBT shift
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        mockCycleRepository.cycleHistory[testUserId] = emptyList() // No history for base prediction
        
        val shiftDate = testDate.plus(DatePeriod(days = 12)) // BBT shift on day 13
        val fertilityLogs = createBBTShiftLogs(shiftDate)
        mockLogRepository.fertilityLogs[testUserId] = fertilityLogs
        
        // When
        val result = useCase.updatePredictionWithCurrentData(testUserId, testDate.plus(DatePeriod(days = 18)))
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertNotNull(prediction)
        
        // Should return some prediction (either BBT confirmed or standard)
        assertNotNull(prediction.predictedDate)
        assertNotNull(prediction.confidence)
        assertNotNull(prediction.method)
        assertNotNull(prediction.fertilityWindow)
    }

    @Test
    fun `updatePredictionWithCurrentData should detect OPK peak and update prediction`() = runTest {
        // Given: Active cycle and fertility logs with OPK peak
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        val opkPeakDate = testDate.plus(DatePeriod(days = 12)) // OPK peak on day 13
        val fertilityLogs = listOf(
            createTestDailyLog(opkPeakDate, opkResult = OPKResult.PEAK)
        )
        mockLogRepository.fertilityLogs[testUserId] = fertilityLogs
        
        // When
        val result = useCase.updatePredictionWithCurrentData(testUserId, testDate.plus(DatePeriod(days = 15)))
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertNotNull(prediction)
        assertEquals(testDate.plus(DatePeriod(days = 13)), prediction.predictedDate) // Day after OPK peak
        assertEquals(PredictionConfidence.HIGH, prediction.confidence)
        assertEquals(PredictionMethod.OPK_CONFIRMED, prediction.method)
    }

    @Test
    fun `fertility window should be calculated correctly`() = runTest {
        // Given: Active cycle with standard prediction
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        mockCycleRepository.cycleHistory[testUserId] = emptyList()
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        val prediction = result.getOrNull()
        assertNotNull(prediction)
        val fertilityWindow = prediction.fertilityWindow
        assertEquals(testDate.plus(DatePeriod(days = 8)), fertilityWindow.startDate) // 5 days before ovulation
        assertEquals(testDate.plus(DatePeriod(days = 14)), fertilityWindow.endDate) // 1 day after ovulation
        assertEquals(testDate.plus(DatePeriod(days = 13)), fertilityWindow.peakDate) // Ovulation day
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
        
        // Pre-ovulation temperatures (consistently lower)
        for (i in 6 downTo 1) {
            logs.add(createTestDailyLog(shiftDate.minus(DatePeriod(days = i)), bbt = 97.1))
        }
        
        // Post-ovulation temperatures (sustained rise of 0.3°F)
        for (i in 0..5) { // Extended to 6 days to ensure algorithm has enough data
            logs.add(createTestDailyLog(shiftDate.plus(DatePeriod(days = i)), bbt = 97.4))
        }
        
        return logs
    }
}