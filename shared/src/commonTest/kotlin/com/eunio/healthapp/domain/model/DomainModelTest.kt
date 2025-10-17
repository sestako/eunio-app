package com.eunio.healthapp.domain.model

import com.eunio.healthapp.domain.util.Validation
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainModelTest {
    
    @Test
    fun testUserCreation() {
        val user = User(
            id = "user123",
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CONCEPTION,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        assertEquals("user123", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.name)
        assertEquals(HealthGoal.CONCEPTION, user.primaryGoal)
        assertTrue(user.onboardingComplete)
    }
    
    @Test
    fun testCycleCreation() {
        val cycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        assertEquals("cycle123", cycle.id)
        assertEquals("user123", cycle.userId)
        assertEquals(LocalDate(2024, 1, 1), cycle.startDate)
        assertEquals(LocalDate(2024, 1, 28), cycle.endDate)
        assertEquals(28, cycle.cycleLength)
        assertEquals(14, cycle.lutealPhaseLength)
    }
    
    @Test
    fun testDailyLogCreation() {
        val dailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 15),
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
            mood = Mood.HAPPY,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            bbt = 36.5,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Feeling good today",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        assertEquals("log123", dailyLog.id)
        assertEquals("user123", dailyLog.userId)
        assertEquals(LocalDate(2024, 1, 15), dailyLog.date)
        assertEquals(PeriodFlow.MEDIUM, dailyLog.periodFlow)
        assertEquals(2, dailyLog.symptoms.size)
        assertTrue(dailyLog.symptoms.contains(Symptom.CRAMPS))
        assertEquals(36.5, dailyLog.bbt)
    }
    
    @Test
    fun testInsightCreation() {
        val insight = Insight(
            id = "insight123",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle appears to be regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85,
            actionable = true
        )
        
        assertEquals("insight123", insight.id)
        assertEquals("user123", insight.userId)
        assertEquals("Your cycle appears to be regular", insight.insightText)
        assertEquals(InsightType.PATTERN_RECOGNITION, insight.type)
        assertEquals(0.85, insight.confidence)
        assertTrue(insight.actionable)
    }
    
    @Test
    fun testUserValidation() {
        val validUser = User(
            id = "user123",
            email = "test@example.com",
            name = "Test User",
            primaryGoal = HealthGoal.CONCEPTION,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(validUser)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun testUserValidationFailure() {
        val invalidUser = User(
            id = "",
            email = "invalid-email",
            name = "",
            primaryGoal = HealthGoal.CONCEPTION,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        val errors = result.errors
        assertTrue(errors.any { it.contains("ID cannot be blank") })
        assertTrue(errors.any { it.contains("Email format is invalid") })
        assertTrue(errors.any { it.contains("Name cannot be blank") })
    }
}