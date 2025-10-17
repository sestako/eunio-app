package com.eunio.healthapp.domain.model

import com.eunio.healthapp.testutil.TestDataFactory
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyLogTest {
    
    @Test
    fun `daily log creation with all fields should succeed`() {
        val log = TestDataFactory.createTestDailyLog("user123")
        
        assertEquals("user123", log.userId)
        assertEquals(LocalDate(2024, 1, 15), log.date)
        assertEquals(98.6, log.bbt)
        assertEquals(CervicalMucus.CREAMY, log.cervicalMucus)
        assertEquals(true, log.sexualActivity?.occurred)
        assertEquals(Protection.CONDOM, log.sexualActivity?.protection)
        assertEquals("Test notes", log.notes)
        assertTrue(log.symptoms.contains(Symptom.CRAMPS))
        assertTrue(log.symptoms.contains(Symptom.HEADACHE))
        assertEquals(Mood.HAPPY, log.mood)
    }
    
    @Test
    fun `daily log with minimal data should be valid`() {
        val log = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 15),
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
        
        assertEquals("log123", log.id)
        assertEquals("user123", log.userId)
        assertNull(log.bbt)
        assertNull(log.cervicalMucus)
        assertNull(log.sexualActivity)
        assertNull(log.notes)
        assertTrue(log.symptoms.isEmpty())
        assertNull(log.mood)
    }
    
    @Test
    fun `daily log with period flow should track correctly`() {
        val log = TestDataFactory.createTestDailyLog("user123").copy(
            periodFlow = PeriodFlow.HEAVY
        )
        
        assertEquals(PeriodFlow.HEAVY, log.periodFlow)
    }
    
    @Test
    fun `daily log with multiple symptoms should store all`() {
        val symptoms = listOf(
            Symptom.CRAMPS,
            Symptom.HEADACHE,
            Symptom.BLOATING,
            Symptom.BREAST_TENDERNESS
        )
        val log = TestDataFactory.createTestDailyLog("user123").copy(
            symptoms = symptoms
        )
        
        assertEquals(4, log.symptoms.size)
        assertTrue(log.symptoms.containsAll(symptoms))
    }
    
    @Test
    fun `daily log with BBT should validate temperature range`() {
        val normalTemp = TestDataFactory.createTestDailyLog("user123").copy(
            bbt = 98.6
        )
        val lowTemp = TestDataFactory.createTestDailyLog("user123").copy(
            bbt = 96.0
        )
        val highTemp = TestDataFactory.createTestDailyLog("user123").copy(
            bbt = 100.0
        )
        
        assertEquals(98.6, normalTemp.bbt)
        assertEquals(96.0, lowTemp.bbt)
        assertEquals(100.0, highTemp.bbt)
    }
    
    @Test
    fun `daily log with sexual activity should track protection`() {
        val protectedActivity = SexualActivity(
            occurred = true,
            protection = Protection.CONDOM
        )
        val unprotectedActivity = SexualActivity(
            occurred = true,
            protection = Protection.NONE
        )
        
        val log1 = TestDataFactory.createTestDailyLog("user123").copy(
            sexualActivity = protectedActivity
        )
        val log2 = TestDataFactory.createTestDailyLog("user123").copy(
            sexualActivity = unprotectedActivity
        )
        
        assertEquals(Protection.CONDOM, log1.sexualActivity?.protection)
        assertEquals(Protection.NONE, log2.sexualActivity?.protection)
    }
}