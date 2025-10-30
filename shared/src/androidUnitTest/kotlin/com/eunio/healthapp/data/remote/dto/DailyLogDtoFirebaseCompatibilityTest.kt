package com.eunio.healthapp.data.remote.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests to verify DailyLogDto is compatible with Firebase Android deserialization.
 * Firebase requires a no-argument constructor for toObject() to work.
 */
class DailyLogDtoFirebaseCompatibilityTest {
    
    @Test
    fun `DailyLogDto has no-arg constructor for Firebase compatibility`() {
        // Given: Firebase requires a no-arg constructor
        // When: Creating an instance with no arguments
        val dto = DailyLogDto()
        
        // Then: Instance should be created successfully
        assertNotNull(dto)
        assertEquals("", dto.logId)
        assertEquals(0L, dto.dateEpochDays)
        assertEquals(0L, dto.createdAt)
        assertEquals(0L, dto.updatedAt)
        assertEquals(1, dto.v)
    }
    
    @Test
    fun `SexualActivityDto has no-arg constructor for Firebase compatibility`() {
        // Given: Firebase requires a no-arg constructor
        // When: Creating an instance with no arguments
        val dto = SexualActivityDto()
        
        // Then: Instance should be created successfully
        assertNotNull(dto)
        assertEquals(false, dto.occurred)
        assertEquals(null, dto.protection)
    }
    
    @Test
    fun `DailyLogDto can be created with all parameters`() {
        // Given: All parameters
        val dto = DailyLogDto(
            logId = "2025-10-23",
            dateEpochDays = 20383L,
            createdAt = 1729699200L,
            updatedAt = 1729699200L,
            periodFlow = "LIGHT",
            symptoms = listOf("CRAMPS", "HEADACHE"),
            mood = "HAPPY",
            sexualActivity = SexualActivityDto(occurred = true, protection = "CONDOM"),
            bbt = 36.5,
            cervicalMucus = "CREAMY",
            opkResult = "POSITIVE",
            notes = "Test notes",
            v = 1
        )
        
        // Then: All fields should be set correctly
        assertEquals("2025-10-23", dto.logId)
        assertEquals(20383L, dto.dateEpochDays)
        assertEquals(1729699200L, dto.createdAt)
        assertEquals(1729699200L, dto.updatedAt)
        assertEquals("LIGHT", dto.periodFlow)
        assertEquals(listOf("CRAMPS", "HEADACHE"), dto.symptoms)
        assertEquals("HAPPY", dto.mood)
        assertNotNull(dto.sexualActivity)
        assertEquals(true, dto.sexualActivity?.occurred)
        assertEquals("CONDOM", dto.sexualActivity?.protection)
        assertEquals(36.5, dto.bbt)
        assertEquals("CREAMY", dto.cervicalMucus)
        assertEquals("POSITIVE", dto.opkResult)
        assertEquals("Test notes", dto.notes)
        assertEquals(1, dto.v)
    }
}
