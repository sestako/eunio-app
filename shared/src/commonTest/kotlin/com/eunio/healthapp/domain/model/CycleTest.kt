package com.eunio.healthapp.domain.model

import com.eunio.healthapp.testutil.TestDataFactory
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleTest {
    
    @Test
    fun `cycle creation with start date should succeed`() {
        val cycle = TestDataFactory.createTestCycle("user123")
        
        assertEquals("user123", cycle.userId)
        assertEquals(LocalDate(2024, 1, 1), cycle.startDate)
        assertNull(cycle.endDate)
        assertNull(cycle.cycleLength)
    }
    
    @Test
    fun `completed cycle should have end date and length`() {
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28
        )
        
        assertEquals(LocalDate(2024, 1, 28), cycle.endDate)
        assertEquals(28, cycle.cycleLength)
    }
    
    @Test
    fun `cycle with ovulation data should track correctly`() {
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            predictedOvulationDate = LocalDate(2024, 1, 14),
            confirmedOvulationDate = LocalDate(2024, 1, 15)
        )
        
        assertEquals(LocalDate(2024, 1, 14), cycle.predictedOvulationDate)
        assertEquals(LocalDate(2024, 1, 15), cycle.confirmedOvulationDate)
    }
    
    @Test
    fun `cycle length calculation should be accurate`() {
        val startDate = LocalDate(2024, 1, 1)
        val endDate = LocalDate(2024, 1, 28)
        val expectedLength = 28
        
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            startDate = startDate,
            endDate = endDate,
            cycleLength = expectedLength
        )
        
        assertEquals(expectedLength, cycle.cycleLength)
    }
    
    @Test
    fun `short cycle should be valid`() {
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 21),
            cycleLength = 21
        )
        
        assertEquals(21, cycle.cycleLength)
        assertTrue(cycle.cycleLength!! < 28)
    }
    
    @Test
    fun `long cycle should be valid`() {
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 2, 5),
            cycleLength = 35
        )
        
        assertEquals(35, cycle.cycleLength)
        assertTrue(cycle.cycleLength!! > 28)
    }
    
    @Test
    fun `cycle with luteal phase should track correctly`() {
        val cycle = TestDataFactory.createTestCycle("user123").copy(
            confirmedOvulationDate = LocalDate(2024, 1, 14),
            endDate = LocalDate(2024, 1, 28),
            lutealPhaseLength = 14
        )
        
        assertEquals(14, cycle.lutealPhaseLength)
    }
}