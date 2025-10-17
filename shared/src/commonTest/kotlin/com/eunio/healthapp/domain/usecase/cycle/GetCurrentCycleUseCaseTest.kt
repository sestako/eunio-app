package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetCurrentCycleUseCaseTest {
    private val mockCycleRepository = MockCycleRepository()
    private val useCase = GetCurrentCycleUseCase(mockCycleRepository)
    
    private val testUserId = "test-user-123"
    private val testDate = LocalDate(2024, 1, 15)

    @Test
    fun `execute should return current cycle when one exists`() = runTest {
        // Given: Active cycle exists
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(cycle, result.getOrNull())
    }

    @Test
    fun `execute should return null when no current cycle exists`() = runTest {
        // Given: No active cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.execute(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `executeWithInfo should return cycle info with calculated fields`() = runTest {
        // Given: Active cycle that started 10 days ago
        val cycleStart = testDate.minus(DatePeriod(days = 9)) // Day 10 of cycle
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.executeWithInfo(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val cycleInfo = result.getOrNull()
        assertNotNull(cycleInfo)
        assertEquals(cycle, cycleInfo.cycle)
        assertEquals(10, cycleInfo.currentDay)
        assertEquals(CyclePhase.FOLLICULAR, cycleInfo.phase)
        assertTrue(cycleInfo.isActive)
        assertNotNull(cycleInfo.daysUntilNextPeriod)
    }
    
    @Test
    fun `executeWithInfo should calculate correct cycle phases`() = runTest {
        // Test menstrual phase (day 3)
        val menstrualCycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 2)))
        mockCycleRepository.currentCycles[testUserId] = menstrualCycle
        var result = useCase.executeWithInfo(testUserId, testDate)
        assertEquals(CyclePhase.MENSTRUAL, result.getOrNull()?.phase)
        
        // Test follicular phase (day 8)
        val follicularCycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 7)))
        mockCycleRepository.currentCycles[testUserId] = follicularCycle
        result = useCase.executeWithInfo(testUserId, testDate)
        assertEquals(CyclePhase.FOLLICULAR, result.getOrNull()?.phase)
        
        // Test ovulation phase (day 14)
        val ovulationCycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 13)))
        mockCycleRepository.currentCycles[testUserId] = ovulationCycle
        result = useCase.executeWithInfo(testUserId, testDate)
        assertEquals(CyclePhase.OVULATION, result.getOrNull()?.phase)
        
        // Test luteal phase (day 20)
        val lutealCycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 19)))
        mockCycleRepository.currentCycles[testUserId] = lutealCycle
        result = useCase.executeWithInfo(testUserId, testDate)
        assertEquals(CyclePhase.LUTEAL, result.getOrNull()?.phase)
    }

    @Test
    fun `hasActiveCycle should return true when active cycle exists`() = runTest {
        // Given: Active cycle (no end date)
        val cycle = createTestCycle(testUserId, testDate)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.hasActiveCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `hasActiveCycle should return false when cycle is completed`() = runTest {
        // Given: Completed cycle (has end date)
        val cycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 28)))
            .copy(endDate = testDate.minus(DatePeriod(days = 1)))
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.hasActiveCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun `hasActiveCycle should return false when no cycle exists`() = runTest {
        // Given: No cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.hasActiveCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }
    
    @Test
    fun `getCurrentCycleLength should return calculated length for active cycle`() = runTest {
        // Given: Active cycle that started 15 days ago
        val cycleStart = testDate.minus(DatePeriod(days = 14)) // Day 15 of cycle
        val cycle = createTestCycle(testUserId, cycleStart)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.getCurrentCycleLength(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(15, result.getOrNull())
    }

    @Test
    fun `getCurrentCycleLength should return stored length for completed cycle`() = runTest {
        // Given: Completed cycle with stored length
        val cycle = createTestCycle(testUserId, testDate.minus(DatePeriod(days = 28)))
            .copy(endDate = testDate.minus(DatePeriod(days = 1)), cycleLength = 28)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.getCurrentCycleLength(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(28, result.getOrNull())
    }

    @Test
    fun `getCurrentCycleLength should return null when no cycle exists`() = runTest {
        // Given: No cycle
        mockCycleRepository.currentCycles.remove(testUserId)
        
        // When
        val result = useCase.getCurrentCycleLength(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `executeWithInfo should calculate days until next period correctly`() = runTest {
        // Given: Cycle on day 20 with 28-day length
        val cycleStart = testDate.minus(DatePeriod(days = 19)) // Day 20
        val cycle = createTestCycle(testUserId, cycleStart).copy(cycleLength = 28)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.executeWithInfo(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val cycleInfo = result.getOrNull()
        assertEquals(9, cycleInfo?.daysUntilNextPeriod) // 28 - 20 + 1 = 9
    }

    @Test
    fun `executeWithInfo should return null days until next period when cycle is overdue`() = runTest {
        // Given: Cycle on day 35 with expected 28-day length
        val cycleStart = testDate.minus(DatePeriod(days = 34)) // Day 35
        val cycle = createTestCycle(testUserId, cycleStart).copy(cycleLength = 28)
        mockCycleRepository.currentCycles[testUserId] = cycle
        
        // When
        val result = useCase.executeWithInfo(testUserId, testDate)
        
        // Then
        assertTrue(result.isSuccess)
        val cycleInfo = result.getOrNull()
        assertNull(cycleInfo?.daysUntilNextPeriod) // Can't predict when overdue
    }

    private fun createTestCycle(userId: String, startDate: LocalDate): Cycle {
        return Cycle(
            id = "cycle-${startDate}",
            userId = userId,
            startDate = startDate
        )
    }
}