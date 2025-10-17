package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.data.remote.FirestorePaths
import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests to verify data format consistency between iOS and Android implementations.
 * 
 * This test suite validates:
 * 1. DailyLogDto uses epoch days for dates
 * 2. DailyLogDto uses seconds for timestamps
 * 3. Field names match exactly between platforms
 * 4. FirestorePaths utility generates consistent paths
 * 5. Data serialization/deserialization works correctly
 */
class DailyLogDtoConsistencyTest {
    
    @Test
    fun `DailyLogDto uses epoch days for dates`() {
        // Given: A specific date
        val date = LocalDate(2025, 10, 14)
        val expectedEpochDays = date.toEpochDays().toLong()
        
        val dailyLog = createTestDailyLog(date = date)
        
        // When: Converting to DTO
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then: Date should be stored as epoch days
        assertEquals(expectedEpochDays, dto.dateEpochDays, "Date should be stored as epoch days")
        assertEquals(date.toEpochDays().toLong(), dto.dateEpochDays, "Epoch days should match LocalDate conversion")
    }
    
    @Test
    fun `DailyLogDto uses seconds for timestamps`() {
        // Given: Specific timestamps
        val createdAt = Instant.fromEpochSeconds(1728936000) // 2024-10-14 18:00:00 UTC
        val updatedAt = Instant.fromEpochSeconds(1728939600) // 2024-10-14 19:00:00 UTC
        
        val dailyLog = createTestDailyLog(
            createdAt = createdAt,
            updatedAt = updatedAt
        )
        
        // When: Converting to DTO
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then: Timestamps should be stored as epoch seconds
        assertEquals(1728936000L, dto.createdAt, "createdAt should be stored as epoch seconds")
        assertEquals(1728939600L, dto.updatedAt, "updatedAt should be stored as epoch seconds")
        assertEquals(createdAt.epochSeconds, dto.createdAt, "createdAt should match Instant conversion")
        assertEquals(updatedAt.epochSeconds, dto.updatedAt, "updatedAt should match Instant conversion")
    }
    
    @Test
    fun `DailyLogDto field names match Android exactly`() {
        // Given: A daily log with all fields populated
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
            mood = Mood.HAPPY,
            bbt = 36.5,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.POSITIVE,
            notes = "Test notes",
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM)
        )
        
        // When: Converting to DTO
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then: All field names should match expected names
        assertEquals("2025-10-14", dto.logId, "logId field should exist")
        assertNotNull(dto.dateEpochDays, "dateEpochDays field should exist")
        assertNotNull(dto.createdAt, "createdAt field should exist")
        assertNotNull(dto.updatedAt, "updatedAt field should exist")
        assertEquals("HEAVY", dto.periodFlow, "periodFlow field should exist and use enum name")
        assertEquals(listOf("CRAMPS", "HEADACHE"), dto.symptoms, "symptoms field should exist and use enum names")
        assertEquals("HAPPY", dto.mood, "mood field should exist and use enum name")
        assertEquals(36.5, dto.bbt, "bbt field should exist")
        assertEquals("CREAMY", dto.cervicalMucus, "cervicalMucus field should exist and use enum name")
        assertEquals("POSITIVE", dto.opkResult, "opkResult field should exist and use enum name")
        assertEquals("Test notes", dto.notes, "notes field should exist")
        assertNotNull(dto.sexualActivity, "sexualActivity field should exist")
        assertEquals(true, dto.sexualActivity?.occurred, "sexualActivity.occurred field should exist")
        assertEquals("CONDOM", dto.sexualActivity?.protection, "sexualActivity.protection field should exist and use enum name")
        assertEquals(1, dto.v, "v (version) field should exist")
    }
    
    @Test
    fun `FirestorePaths utility generates consistent paths`() {
        // Given: User and log IDs
        val userId = "test_user_123"
        val logId = "2025-10-14"
        
        // When: Generating paths
        val collectionPath = FirestorePaths.dailyLogsCollection(userId)
        val documentPath = FirestorePaths.dailyLogDoc(userId, logId)
        
        // Then: Paths should match expected format
        assertEquals("users/$userId/dailyLogs", collectionPath, "Collection path should match format")
        assertEquals("users/$userId/dailyLogs/$logId", documentPath, "Document path should match format")
        
        // Verify path components
        val pathParts = documentPath.split("/")
        assertEquals(4, pathParts.size, "Document path should have 4 components")
        assertEquals("users", pathParts[0], "First component should be 'users'")
        assertEquals(userId, pathParts[1], "Second component should be userId")
        assertEquals("dailyLogs", pathParts[2], "Third component should be 'dailyLogs'")
        assertEquals(logId, pathParts[3], "Fourth component should be logId")
    }
    
    @Test
    fun `Data serialization and deserialization preserves all fields`() {
        // Given: A daily log with all fields populated
        val originalLog = createTestDailyLog(
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.BLOATING, Symptom.FATIGUE),
            mood = Mood.ANXIOUS,
            bbt = 36.8,
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.NEGATIVE,
            notes = "Detailed test notes",
            sexualActivity = SexualActivity(occurred = true, protection = Protection.WITHDRAWAL)
        )
        
        // When: Converting to DTO and back to domain
        val dto = DailyLogDto.fromDomain(originalLog)
        val reconstructedLog = dto.toDomain(originalLog.id, originalLog.userId)
        
        // Then: All fields should be preserved
        assertEquals(originalLog.id, reconstructedLog.id, "ID should be preserved")
        assertEquals(originalLog.userId, reconstructedLog.userId, "User ID should be preserved")
        assertEquals(originalLog.date, reconstructedLog.date, "Date should be preserved")
        assertEquals(originalLog.periodFlow, reconstructedLog.periodFlow, "Period flow should be preserved")
        assertEquals(originalLog.symptoms, reconstructedLog.symptoms, "Symptoms should be preserved")
        assertEquals(originalLog.mood, reconstructedLog.mood, "Mood should be preserved")
        assertEquals(originalLog.bbt, reconstructedLog.bbt, "BBT should be preserved")
        assertEquals(originalLog.cervicalMucus, reconstructedLog.cervicalMucus, "Cervical mucus should be preserved")
        assertEquals(originalLog.opkResult, reconstructedLog.opkResult, "OPK result should be preserved")
        assertEquals(originalLog.notes, reconstructedLog.notes, "Notes should be preserved")
        assertEquals(originalLog.sexualActivity?.occurred, reconstructedLog.sexualActivity?.occurred, "Sexual activity occurred should be preserved")
        assertEquals(originalLog.sexualActivity?.protection, reconstructedLog.sexualActivity?.protection, "Sexual activity protection should be preserved")
        assertEquals(originalLog.createdAt, reconstructedLog.createdAt, "Created at should be preserved")
        assertEquals(originalLog.updatedAt, reconstructedLog.updatedAt, "Updated at should be preserved")
    }
    
    @Test
    fun `Null and empty fields are handled correctly`() {
        // Given: A daily log with minimal fields (nulls and empty lists)
        val minimalLog = createTestDailyLog(
            periodFlow = null,
            symptoms = emptyList(),
            mood = null,
            bbt = null,
            cervicalMucus = null,
            opkResult = null,
            notes = null,
            sexualActivity = null
        )
        
        // When: Converting to DTO and back
        val dto = DailyLogDto.fromDomain(minimalLog)
        val reconstructedLog = dto.toDomain(minimalLog.id, minimalLog.userId)
        
        // Then: Null and empty fields should be preserved
        assertEquals(null, reconstructedLog.periodFlow, "Null period flow should be preserved")
        assertTrue(reconstructedLog.symptoms.isEmpty(), "Empty symptoms list should be preserved")
        assertEquals(null, reconstructedLog.mood, "Null mood should be preserved")
        assertEquals(null, reconstructedLog.bbt, "Null BBT should be preserved")
        assertEquals(null, reconstructedLog.cervicalMucus, "Null cervical mucus should be preserved")
        assertEquals(null, reconstructedLog.opkResult, "Null OPK result should be preserved")
        assertEquals(null, reconstructedLog.notes, "Null notes should be preserved")
        assertEquals(null, reconstructedLog.sexualActivity, "Null sexual activity should be preserved")
    }
    
    @Test
    fun `DTO field types match expected types for Firestore`() {
        // Given: A daily log
        val dailyLog = createTestDailyLog()
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then: Field types should match Firestore expectations
        assertTrue(dto.logId is String, "logId should be String")
        assertTrue(dto.dateEpochDays is Long, "dateEpochDays should be Long")
        assertTrue(dto.createdAt is Long, "createdAt should be Long")
        assertTrue(dto.updatedAt is Long, "updatedAt should be Long")
        assertTrue(dto.periodFlow is String?, "periodFlow should be String?")
        assertTrue(dto.symptoms is List<String>?, "symptoms should be List<String>?")
        assertTrue(dto.mood is String?, "mood should be String?")
        assertTrue(dto.bbt is Double?, "bbt should be Double?")
        assertTrue(dto.cervicalMucus is String?, "cervicalMucus should be String?")
        assertTrue(dto.opkResult is String?, "opkResult should be String?")
        assertTrue(dto.notes is String?, "notes should be String?")
        assertTrue(dto.v is Int, "v should be Int")
    }
    
    @Test
    fun `Enum values are stored as uppercase strings`() {
        // Given: A daily log with enum values
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.LIGHT,
            symptoms = listOf(Symptom.ACNE),
            mood = Mood.SAD,
            cervicalMucus = CervicalMucus.STICKY,
            opkResult = OPKResult.PEAK,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.BIRTH_CONTROL)
        )
        
        // When: Converting to DTO
        val dto = DailyLogDto.fromDomain(dailyLog)
        
        // Then: Enum values should be uppercase strings
        assertEquals("LIGHT", dto.periodFlow, "PeriodFlow should be uppercase string")
        assertEquals(listOf("ACNE"), dto.symptoms, "Symptoms should be uppercase strings")
        assertEquals("SAD", dto.mood, "Mood should be uppercase string")
        assertEquals("STICKY", dto.cervicalMucus, "CervicalMucus should be uppercase string")
        assertEquals("PEAK", dto.opkResult, "OPKResult should be uppercase string")
        assertEquals("BIRTH_CONTROL", dto.sexualActivity?.protection, "Protection should be uppercase string")
    }
    
    @Test
    fun `Date conversion is consistent across platforms`() {
        // Test multiple dates to ensure consistency
        val testDates = listOf(
            LocalDate(2024, 1, 1),
            LocalDate(2024, 12, 31),
            LocalDate(2025, 10, 14),
            LocalDate(1970, 1, 1),
            LocalDate(2000, 1, 1)
        )
        
        testDates.forEach { date ->
            val expectedEpochDays = date.toEpochDays().toLong()
            val dailyLog = createTestDailyLog(date = date)
            val dto = DailyLogDto.fromDomain(dailyLog)
            
            assertEquals(
                expectedEpochDays,
                dto.dateEpochDays,
                "Date $date should convert to epoch days $expectedEpochDays"
            )
            
            // Verify round-trip conversion
            val reconstructed = dto.toDomain(dailyLog.id, dailyLog.userId)
            assertEquals(date, reconstructed.date, "Date should survive round-trip conversion")
        }
    }
    
    @Test
    fun `Timestamp conversion is consistent across platforms`() {
        // Test multiple timestamps to ensure consistency
        val testTimestamps = listOf(
            Instant.fromEpochSeconds(0),           // Unix epoch
            Instant.fromEpochSeconds(1000000000),  // 2001-09-09
            Instant.fromEpochSeconds(1728936000),  // 2024-10-14
            Instant.fromEpochSeconds(2000000000)   // 2033-05-18
        )
        
        testTimestamps.forEach { timestamp ->
            val dailyLog = createTestDailyLog(
                createdAt = timestamp,
                updatedAt = timestamp
            )
            val dto = DailyLogDto.fromDomain(dailyLog)
            
            assertEquals(
                timestamp.epochSeconds,
                dto.createdAt,
                "Timestamp $timestamp should convert to epoch seconds"
            )
            
            // Verify round-trip conversion
            val reconstructed = dto.toDomain(dailyLog.id, dailyLog.userId)
            assertEquals(timestamp, reconstructed.createdAt, "Timestamp should survive round-trip conversion")
        }
    }
    
    // Helper function to create test daily logs
    private fun createTestDailyLog(
        id: String = "2025-10-14",
        userId: String = "test_user_123",
        date: LocalDate = LocalDate(2025, 10, 14),
        periodFlow: PeriodFlow? = null,
        symptoms: List<Symptom> = emptyList(),
        mood: Mood? = null,
        bbt: Double? = null,
        cervicalMucus: CervicalMucus? = null,
        opkResult: OPKResult? = null,
        notes: String? = null,
        sexualActivity: SexualActivity? = null,
        createdAt: Instant = Instant.fromEpochSeconds(1728936000),
        updatedAt: Instant = Instant.fromEpochSeconds(1728936000)
    ): DailyLog {
        return DailyLog(
            id = id,
            userId = userId,
            date = date,
            periodFlow = periodFlow,
            symptoms = symptoms,
            mood = mood,
            sexualActivity = sexualActivity,
            bbt = bbt,
            cervicalMucus = cervicalMucus,
            opkResult = opkResult,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
