package com.eunio.healthapp.domain.model

import com.eunio.healthapp.domain.util.Validation
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComprehensiveModelTest {
    
    private val json = Json { prettyPrint = true }
    
    @Test
    fun testUserModelSerialization() {
        val user = User(
            id = "user-123",
            email = "jane.doe@example.com",
            name = "Jane Doe",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CONCEPTION,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Test serialization
        val serialized = json.encodeToString(user)
        assertNotNull(serialized)
        assertTrue(serialized.contains("jane.doe@example.com"))
        assertTrue(serialized.contains("CONCEPTION"))
        
        // Test deserialization
        val deserialized = json.decodeFromString<User>(serialized)
        assertEquals(user.id, deserialized.id)
        assertEquals(user.email, deserialized.email)
        assertEquals(user.primaryGoal, deserialized.primaryGoal)
    }
    
    @Test
    fun testAllHealthGoalEnumValues() {
        val goals = HealthGoal.values()
        assertEquals(4, goals.size)
        assertTrue(goals.contains(HealthGoal.CONCEPTION))
        assertTrue(goals.contains(HealthGoal.CONTRACEPTION))
        assertTrue(goals.contains(HealthGoal.CYCLE_TRACKING))
        assertTrue(goals.contains(HealthGoal.GENERAL_HEALTH))
    }
    
    @Test
    fun testCycleModelWithAllFields() {
        val cycle = Cycle(
            id = "cycle-456",
            userId = "user-123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            predictedOvulationDate = LocalDate(2024, 1, 14),
            confirmedOvulationDate = LocalDate(2024, 1, 15),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        // Test serialization
        val serialized = json.encodeToString(cycle)
        val deserialized = json.decodeFromString<Cycle>(serialized)
        
        assertEquals(cycle.id, deserialized.id)
        assertEquals(cycle.startDate, deserialized.startDate)
        assertEquals(cycle.endDate, deserialized.endDate)
        assertEquals(cycle.predictedOvulationDate, deserialized.predictedOvulationDate)
        assertEquals(cycle.confirmedOvulationDate, deserialized.confirmedOvulationDate)
        assertEquals(cycle.cycleLength, deserialized.cycleLength)
        assertEquals(cycle.lutealPhaseLength, deserialized.lutealPhaseLength)
    }
    
    @Test
    fun testDailyLogWithAllHealthTrackingFields() {
        val sexualActivity = SexualActivity(
            occurred = true,
            protection = Protection.CONDOM
        )
        
        val dailyLog = DailyLog(
            id = "log-789",
            userId = "user-123",
            date = LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE, Symptom.BLOATING, Symptom.MOOD_SWINGS),
            mood = Mood.IRRITABLE,
            sexualActivity = sexualActivity,
            bbt = 36.9, // Valid Celsius temperature
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.POSITIVE,
            notes = "Heavy flow day with significant symptoms",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Test all fields are properly set
        assertEquals("log-789", dailyLog.id)
        assertEquals(PeriodFlow.HEAVY, dailyLog.periodFlow)
        assertEquals(4, dailyLog.symptoms.size)
        assertTrue(dailyLog.symptoms.contains(Symptom.CRAMPS))
        assertTrue(dailyLog.symptoms.contains(Symptom.MOOD_SWINGS))
        assertEquals(Mood.IRRITABLE, dailyLog.mood)
        assertNotNull(dailyLog.sexualActivity)
        assertTrue(dailyLog.sexualActivity!!.occurred)
        assertEquals(Protection.CONDOM, dailyLog.sexualActivity!!.protection)
        assertEquals(36.9, dailyLog.bbt)
        assertEquals(CervicalMucus.EGG_WHITE, dailyLog.cervicalMucus)
        assertEquals(OPKResult.POSITIVE, dailyLog.opkResult)
        
        // Test serialization
        val serialized = json.encodeToString(dailyLog)
        val deserialized = json.decodeFromString<DailyLog>(serialized)
        
        assertEquals(dailyLog.id, deserialized.id)
        assertEquals(dailyLog.symptoms.size, deserialized.symptoms.size)
        assertEquals(dailyLog.sexualActivity?.occurred, deserialized.sexualActivity?.occurred)
        assertEquals(dailyLog.bbt, deserialized.bbt)
    }
    
    @Test
    fun testAllSymptomEnumValues() {
        val symptoms = Symptom.values()
        assertEquals(10, symptoms.size)
        assertTrue(symptoms.contains(Symptom.CRAMPS))
        assertTrue(symptoms.contains(Symptom.HEADACHE))
        assertTrue(symptoms.contains(Symptom.BLOATING))
        assertTrue(symptoms.contains(Symptom.BREAST_TENDERNESS))
        assertTrue(symptoms.contains(Symptom.ACNE))
        assertTrue(symptoms.contains(Symptom.MOOD_SWINGS))
        assertTrue(symptoms.contains(Symptom.FATIGUE))
        assertTrue(symptoms.contains(Symptom.NAUSEA))
        assertTrue(symptoms.contains(Symptom.BACK_PAIN))
        assertTrue(symptoms.contains(Symptom.FOOD_CRAVINGS))
    }
    
    @Test
    fun testAllMoodEnumValues() {
        val moods = Mood.values()
        assertEquals(8, moods.size)
        assertTrue(moods.contains(Mood.HAPPY))
        assertTrue(moods.contains(Mood.SAD))
        assertTrue(moods.contains(Mood.ANXIOUS))
        assertTrue(moods.contains(Mood.IRRITABLE))
        assertTrue(moods.contains(Mood.CALM))
        assertTrue(moods.contains(Mood.ENERGETIC))
        assertTrue(moods.contains(Mood.TIRED))
        assertTrue(moods.contains(Mood.NEUTRAL))
    }
    
    @Test
    fun testSexualActivityModel() {
        val activity1 = SexualActivity(occurred = false)
        assertFalse(activity1.occurred)
        assertEquals(null, activity1.protection)
        
        val activity2 = SexualActivity(occurred = true, protection = Protection.BIRTH_CONTROL)
        assertTrue(activity2.occurred)
        assertEquals(Protection.BIRTH_CONTROL, activity2.protection)
        
        // Test serialization
        val serialized = json.encodeToString(activity2)
        val deserialized = json.decodeFromString<SexualActivity>(serialized)
        assertEquals(activity2.occurred, deserialized.occurred)
        assertEquals(activity2.protection, deserialized.protection)
    }
    
    @Test
    fun testInsightModelWithAllFields() {
        val insight = Insight(
            id = "insight-101",
            userId = "user-123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle length has been consistently 28 days for the past 3 months, indicating a regular pattern.",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            relatedLogIds = listOf("log-1", "log-2", "log-3"),
            confidence = 0.92,
            actionable = true
        )
        
        assertEquals("insight-101", insight.id)
        assertEquals(InsightType.PATTERN_RECOGNITION, insight.type)
        assertFalse(insight.isRead)
        assertEquals(3, insight.relatedLogIds.size)
        assertEquals(0.92, insight.confidence)
        assertTrue(insight.actionable)
        
        // Test serialization
        val serialized = json.encodeToString(insight)
        val deserialized = json.decodeFromString<Insight>(serialized)
        
        assertEquals(insight.id, deserialized.id)
        assertEquals(insight.insightText, deserialized.insightText)
        assertEquals(insight.type, deserialized.type)
        assertEquals(insight.relatedLogIds.size, deserialized.relatedLogIds.size)
        assertEquals(insight.confidence, deserialized.confidence)
    }
    
    @Test
    fun testAllInsightTypeEnumValues() {
        val types = InsightType.values()
        assertEquals(4, types.size)
        assertTrue(types.contains(InsightType.PATTERN_RECOGNITION))
        assertTrue(types.contains(InsightType.EARLY_WARNING))
        assertTrue(types.contains(InsightType.CYCLE_PREDICTION))
        assertTrue(types.contains(InsightType.FERTILITY_WINDOW))
    }
    
    @Test
    fun testValidationWithValidData() {
        val validUser = User(
            id = "valid-user",
            email = "valid@example.com",
            name = "Valid User",
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(validUser)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun testValidationWithInvalidData() {
        val invalidUser = User(
            id = "",
            email = "not-an-email",
            name = "",
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        val errors = result.errors
        assertTrue(errors.size >= 3)
        assertTrue(errors.any { it.contains("ID cannot be blank") })
        assertTrue(errors.any { it.contains("Email format is invalid") })
        assertTrue(errors.any { it.contains("Name cannot be blank") })
    }
    
    @Test
    fun testCycleValidation() {
        val validCycle = Cycle(
            id = "valid-cycle",
            userId = "user-123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(validCycle)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun testCycleValidationWithInvalidLength() {
        val invalidCycle = Cycle(
            id = "invalid-cycle",
            userId = "user-123",
            startDate = LocalDate(2024, 1, 1),
            cycleLength = 50, // Invalid - too long
            lutealPhaseLength = 20 // Invalid - too long
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        val errors = result.errors
        assertTrue(errors.any { it.contains("Cycle length should be between 21 and 35 days") })
        assertTrue(errors.any { it.contains("Luteal phase length should be between 10 and 16 days") })
    }
    
    @Test
    fun testDailyLogValidation() {
        val validLog = DailyLog(
            id = "valid-log",
            userId = "user-123",
            date = LocalDate(2024, 1, 15),
            bbt = 36.8, // Valid Celsius temperature
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(validLog)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun testDailyLogValidationWithInvalidBBT() {
        val invalidLog = DailyLog(
            id = "invalid-log",
            userId = "user-123",
            date = LocalDate(2024, 1, 15),
            bbt = 45.0, // Invalid - too high
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidLog)
        assertTrue(result is ValidationResult.Error)
        val errors = result.errors
        assertTrue(errors.any { it.contains("BBT should be between 35.0°C and 40.0°C") })
    }
    
    @Test
    fun testInsightValidation() {
        val validInsight = Insight(
            id = "valid-insight",
            userId = "user-123",
            generatedDate = Clock.System.now(),
            insightText = "Valid insight text",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85
        )
        
        val result = Validation.validateInsight(validInsight)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun testInsightValidationWithInvalidConfidence() {
        val invalidInsight = Insight(
            id = "invalid-insight",
            userId = "user-123",
            generatedDate = Clock.System.now(),
            insightText = "",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 1.5 // Invalid - too high
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        val errors = result.errors
        assertTrue(errors.any { it.contains("Insight text cannot be blank") })
        assertTrue(errors.any { it.contains("Confidence should be between 0.0 and 1.0") })
    }
}