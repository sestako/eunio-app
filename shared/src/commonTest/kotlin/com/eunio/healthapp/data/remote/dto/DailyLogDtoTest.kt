package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for DailyLogDto conversion between domain model and DTO
 */
class DailyLogDtoTest {
    
    @Test
    fun `fromDomain converts DailyLog to DTO with all required fields`() {
        // Given
        val dailyLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = LocalDate(2025, 10, 4),
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
            mood = Mood.TIRED,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            bbt = 97.8,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Feeling tired today",
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then
        assertEquals("2025-10-04", dto.logId)
        assertEquals(dailyLog.date.toEpochDays().toLong(), dto.dateEpochDays)
        assertEquals(1696392000L, dto.createdAt)
        assertEquals(1696392305L, dto.updatedAt)
        assertEquals("MEDIUM", dto.periodFlow)
        assertEquals(listOf("CRAMPS", "HEADACHE"), dto.symptoms)
        assertEquals("TIRED", dto.mood)
        assertNotNull(dto.sexualActivity)
        assertEquals(97.8, dto.bbt)
        assertEquals("CREAMY", dto.cervicalMucus)
        assertEquals("NEGATIVE", dto.opkResult)
        assertEquals("Feeling tired today", dto.notes)
        assertEquals(1, dto.v)
    }
    
    @Test
    fun `fromDomain handles empty symptoms list correctly`() {
        // Given
        val dailyLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = LocalDate(2025, 10, 4),
            symptoms = emptyList(),
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then - empty list should be stored as null
        assertNull(dto.symptoms)
    }
    
    @Test
    fun `fromDomain handles null optional fields`() {
        // Given
        val dailyLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = LocalDate(2025, 10, 4),
            periodFlow = null,
            symptoms = emptyList(),
            mood = null,
            sexualActivity = null,
            bbt = null,
            cervicalMucus = null,
            opkResult = null,
            notes = null,
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then
        assertNull(dto.periodFlow)
        assertNull(dto.symptoms)
        assertNull(dto.mood)
        assertNull(dto.sexualActivity)
        assertNull(dto.bbt)
        assertNull(dto.cervicalMucus)
        assertNull(dto.opkResult)
        assertNull(dto.notes)
    }
    
    @Test
    fun `toDomain converts DTO to DailyLog with all fields`() {
        // Given
        val dto = DailyLogDto(
            logId = "2025-10-04",
            dateEpochDays = LocalDate(2025, 10, 4).toEpochDays().toLong(),
            createdAt = 1696392000L,
            updatedAt = 1696392305L,
            periodFlow = "MEDIUM",
            symptoms = listOf("CRAMPS", "HEADACHE"),
            mood = "TIRED",
            sexualActivity = SexualActivityDto(occurred = true, protection = "CONDOM"),
            bbt = 97.8,
            cervicalMucus = "CREAMY",
            opkResult = "NEGATIVE",
            notes = "Feeling tired today",
            v = 1
        )
        
        // When
        val dailyLog = dto.toDomain(userId = "user123")
        
        // Then
        assertEquals("2025-10-04", dailyLog.id)
        assertEquals("user123", dailyLog.userId)
        assertEquals(LocalDate(2025, 10, 4), dailyLog.date)
        assertEquals(PeriodFlow.MEDIUM, dailyLog.periodFlow)
        assertEquals(listOf(Symptom.CRAMPS, Symptom.HEADACHE), dailyLog.symptoms)
        assertEquals(Mood.TIRED, dailyLog.mood)
        assertNotNull(dailyLog.sexualActivity)
        assertEquals(true, dailyLog.sexualActivity?.occurred)
        assertEquals(Protection.CONDOM, dailyLog.sexualActivity?.protection)
        assertEquals(97.8, dailyLog.bbt)
        assertEquals(CervicalMucus.CREAMY, dailyLog.cervicalMucus)
        assertEquals(OPKResult.NEGATIVE, dailyLog.opkResult)
        assertEquals("Feeling tired today", dailyLog.notes)
        assertEquals(Instant.fromEpochSeconds(1696392000), dailyLog.createdAt)
        assertEquals(Instant.fromEpochSeconds(1696392305), dailyLog.updatedAt)
    }
    
    @Test
    fun `toDomain supports backward compatibility with document ID override`() {
        // Given - DTO with logId field
        val dto = DailyLogDto(
            logId = "2025-10-04",
            dateEpochDays = LocalDate(2025, 10, 4).toEpochDays().toLong(),
            createdAt = 1696392000L,
            updatedAt = 1696392305L,
            v = 1
        )
        
        // When - passing document ID explicitly (legacy pattern)
        val dailyLog = dto.toDomain(id = "legacy-doc-id", userId = "user123")
        
        // Then - should use the provided ID, not the DTO's logId
        assertEquals("legacy-doc-id", dailyLog.id)
        assertEquals("user123", dailyLog.userId)
    }
    
    @Test
    fun `toDomain handles null symptoms correctly`() {
        // Given
        val dto = DailyLogDto(
            logId = "2025-10-04",
            dateEpochDays = LocalDate(2025, 10, 4).toEpochDays().toLong(),
            createdAt = 1696392000L,
            updatedAt = 1696392305L,
            symptoms = null,
            v = 1
        )
        
        // When
        val dailyLog = dto.toDomain(userId = "user123")
        
        // Then - null symptoms should become empty list
        assertEquals(emptyList(), dailyLog.symptoms)
    }
    
    @Test
    fun `toDomain handles null optional fields`() {
        // Given
        val dto = DailyLogDto(
            logId = "2025-10-04",
            dateEpochDays = LocalDate(2025, 10, 4).toEpochDays().toLong(),
            createdAt = 1696392000L,
            updatedAt = 1696392305L,
            periodFlow = null,
            symptoms = null,
            mood = null,
            sexualActivity = null,
            bbt = null,
            cervicalMucus = null,
            opkResult = null,
            notes = null,
            v = 1
        )
        
        // When
        val dailyLog = dto.toDomain(userId = "user123")
        
        // Then
        assertNull(dailyLog.periodFlow)
        assertEquals(emptyList(), dailyLog.symptoms)
        assertNull(dailyLog.mood)
        assertNull(dailyLog.sexualActivity)
        assertNull(dailyLog.bbt)
        assertNull(dailyLog.cervicalMucus)
        assertNull(dailyLog.opkResult)
        assertNull(dailyLog.notes)
    }
    
    @Test
    fun `round trip conversion preserves all data`() {
        // Given
        val originalLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = LocalDate(2025, 10, 4),
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING, Symptom.FATIGUE),
            mood = Mood.ANXIOUS,
            sexualActivity = SexualActivity(occurred = false, protection = null),
            bbt = 98.2,
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.PEAK,
            notes = "Test notes",
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(originalLog)
        val convertedLog = dto.toDomain(userId = "user123")
        
        // Then
        assertEquals(originalLog, convertedLog)
    }
    
    @Test
    fun `dateEpochDays stores date as UTC epoch days`() {
        // Given
        val date = LocalDate(2025, 10, 4)
        val dailyLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = date,
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then
        val expectedEpochDays = date.toEpochDays().toLong()
        assertEquals(expectedEpochDays, dto.dateEpochDays)
        
        // Verify round trip
        val convertedDate = LocalDate.fromEpochDays(dto.dateEpochDays.toInt())
        assertEquals(date, convertedDate)
    }
    
    @Test
    fun `schema version is always 1`() {
        // Given
        val dailyLog = DailyLog(
            id = "2025-10-04",
            userId = "user123",
            date = LocalDate(2025, 10, 4),
            createdAt = Instant.fromEpochSeconds(1696392000),
            updatedAt = Instant.fromEpochSeconds(1696392305)
        )
        
        // When
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then
        assertEquals(1, dto.v)
    }
}
