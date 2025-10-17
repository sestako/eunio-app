package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ValidationTest {
    
    // User Validation Tests
    
    @Test
    fun `validateUser succeeds for valid user`() {
        val validUser = User(
            id = "user123",
            email = "test@example.com",
            name = "John Doe",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(validUser)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateUser fails for blank user ID`() {
        val invalidUser = User(
            id = "",
            email = "test@example.com",
            name = "John Doe",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `validateUser fails for blank email`() {
        val invalidUser = User(
            id = "user123",
            email = "",
            name = "John Doe",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Email cannot be blank"))
    }
    
    @Test
    fun `validateUser fails for invalid email format`() {
        val invalidUser = User(
            id = "user123",
            email = "invalid-email",
            name = "John Doe",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Email format is invalid"))
    }
    
    @Test
    fun `validateUser fails for blank name`() {
        val invalidUser = User(
            id = "user123",
            email = "test@example.com",
            name = "",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Name cannot be blank"))
    }
    
    @Test
    fun `validateUser accumulates multiple errors`() {
        val invalidUser = User(
            id = "",
            email = "invalid-email",
            name = "",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateUser(invalidUser)
        assertTrue(result is ValidationResult.Error)
        assertEquals(3, result.errors.size)
        assertTrue(result.errors.contains("User ID cannot be blank"))
        assertTrue(result.errors.contains("Email format is invalid"))
        assertTrue(result.errors.contains("Name cannot be blank"))
    }
    
    // Cycle Validation Tests
    
    @Test
    fun `validateCycle succeeds for valid cycle`() {
        val validCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(validCycle)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateCycle fails for blank cycle ID`() {
        val invalidCycle = Cycle(
            id = "",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Cycle ID cannot be blank"))
    }
    
    @Test
    fun `validateCycle fails for blank user ID`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `validateCycle fails when end date is before start date`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 28),
            endDate = LocalDate(2024, 1, 1),
            cycleLength = 28,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("End date cannot be before start date"))
    }
    
    @Test
    fun `validateCycle fails for cycle length below minimum`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 20,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Cycle length should be between 21 and 35 days"))
    }
    
    @Test
    fun `validateCycle fails for cycle length above maximum`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 36,
            lutealPhaseLength = 14
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Cycle length should be between 21 and 35 days"))
    }
    
    @Test
    fun `validateCycle fails for luteal phase length below minimum`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 9
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Luteal phase length should be between 10 and 16 days"))
    }
    
    @Test
    fun `validateCycle fails for luteal phase length above maximum`() {
        val invalidCycle = Cycle(
            id = "cycle123",
            userId = "user123",
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 28),
            cycleLength = 28,
            lutealPhaseLength = 17
        )
        
        val result = Validation.validateCycle(invalidCycle)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Luteal phase length should be between 10 and 16 days"))
    }
    
    // Daily Log Validation Tests
    
    @Test
    fun `validateDailyLog succeeds for valid daily log`() {
        val validDailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 36.5,
            notes = "Feeling good today",
            symptoms = listOf(Symptom.HEADACHE, Symptom.FATIGUE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(validDailyLog)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateDailyLog fails for blank daily log ID`() {
        val invalidDailyLog = DailyLog(
            id = "",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 36.5,
            notes = "Feeling good today",
            symptoms = listOf(Symptom.HEADACHE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Daily log ID cannot be blank"))
    }
    
    @Test
    fun `validateDailyLog fails for blank user ID`() {
        val invalidDailyLog = DailyLog(
            id = "log123",
            userId = "",
            date = LocalDate(2024, 1, 1),
            bbt = 36.5,
            notes = "Feeling good today",
            symptoms = listOf(Symptom.HEADACHE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `validateDailyLog fails for BBT below minimum`() {
        val invalidDailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 34.0,
            notes = "Feeling good today",
            symptoms = listOf(Symptom.HEADACHE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("BBT should be between 35.0°C and 40.0°C"))
    }
    
    @Test
    fun `validateDailyLog fails for BBT above maximum`() {
        val invalidDailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 41.0,
            notes = "Feeling good today",
            symptoms = listOf(Symptom.HEADACHE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("BBT should be between 35.0°C and 40.0°C"))
    }
    
    @Test
    fun `validateDailyLog fails for notes exceeding maximum length`() {
        val longNotes = "a".repeat(1001)
        val invalidDailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 36.5,
            notes = longNotes,
            symptoms = listOf(Symptom.HEADACHE),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Notes cannot exceed 1000 characters"))
    }
    
    @Test
    fun `validateDailyLog fails for too many symptoms`() {
        val tooManySymptoms = listOf(
            Symptom.CRAMPS, Symptom.HEADACHE, Symptom.BLOATING, Symptom.BREAST_TENDERNESS,
            Symptom.ACNE, Symptom.MOOD_SWINGS, Symptom.FATIGUE, Symptom.NAUSEA,
            Symptom.BACK_PAIN, Symptom.FOOD_CRAVINGS, Symptom.CRAMPS // 11 symptoms (duplicate to exceed limit)
        )
        val invalidDailyLog = DailyLog(
            id = "log123",
            userId = "user123",
            date = LocalDate(2024, 1, 1),
            bbt = 36.5,
            notes = "Feeling good today",
            symptoms = tooManySymptoms,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val result = Validation.validateDailyLog(invalidDailyLog)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Cannot log more than 10 symptoms per day"))
    }
    
    // Insight Validation Tests
    
    @Test
    fun `validateInsight succeeds for valid insight`() {
        val validInsight = Insight(
            id = "insight123",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85
        )
        
        val result = Validation.validateInsight(validInsight)
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `validateInsight fails for blank insight ID`() {
        val invalidInsight = Insight(
            id = "",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Insight ID cannot be blank"))
    }
    
    @Test
    fun `validateInsight fails for blank user ID`() {
        val invalidInsight = Insight(
            id = "insight123",
            userId = "",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("User ID cannot be blank"))
    }
    
    @Test
    fun `validateInsight fails for blank insight text`() {
        val invalidInsight = Insight(
            id = "insight123",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 0.85
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Insight text cannot be blank"))
    }
    
    @Test
    fun `validateInsight fails for confidence below minimum`() {
        val invalidInsight = Insight(
            id = "insight123",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = -0.1
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Confidence should be between 0.0 and 1.0"))
    }
    
    @Test
    fun `validateInsight fails for confidence above maximum`() {
        val invalidInsight = Insight(
            id = "insight123",
            userId = "user123",
            generatedDate = Clock.System.now(),
            insightText = "Your cycle is regular",
            type = InsightType.PATTERN_RECOGNITION,
            confidence = 1.1
        )
        
        val result = Validation.validateInsight(invalidInsight)
        assertTrue(result is ValidationResult.Error)
        assertTrue(result.errors.contains("Confidence should be between 0.0 and 1.0"))
    }
    
    // Email Validation Tests
    
    @Test
    fun `isValidEmail returns true for valid emails`() {
        assertTrue(Validation.isValidEmail("test@example.com"))
        assertTrue(Validation.isValidEmail("user.name@domain.co.uk"))
        assertTrue(Validation.isValidEmail("user+tag@example.org"))
        assertTrue(Validation.isValidEmail("123@example.com"))
    }
    
    @Test
    fun `isValidEmail returns false for invalid emails`() {
        assertFalse(Validation.isValidEmail("invalid-email"))
        assertFalse(Validation.isValidEmail("@example.com"))
        assertFalse(Validation.isValidEmail("test@"))
        assertFalse(Validation.isValidEmail("test.example.com"))
        assertFalse(Validation.isValidEmail(""))
    }
    
    @Test
    fun `isValidEmail detects malicious patterns`() {
        assertFalse(Validation.isValidEmail("test';DROP TABLE users;--@example.com"))
        assertFalse(Validation.isValidEmail("test<script>alert('xss')</script>@example.com"))
        assertFalse(Validation.isValidEmail("test@example.com';DELETE FROM users;--"))
        assertFalse(Validation.isValidEmail("javascript:alert('xss')@example.com"))
    }
    
    // Password Validation Tests
    
    @Test
    fun `validatePassword succeeds for valid password`() {
        val result = Validation.validatePassword("validpassword123")
        assertTrue(result.isValid)
        assertEquals("Password is valid", result.message)
    }
    
    @Test
    fun `validatePassword fails for short password`() {
        val result = Validation.validatePassword("short")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 6 characters long", result.message)
    }
    
    @Test
    fun `validatePassword fails for long password`() {
        val longPassword = "a".repeat(129)
        val result = Validation.validatePassword(longPassword)
        assertFalse(result.isValid)
        assertEquals("Password must be less than 128 characters", result.message)
    }
    
    @Test
    fun `validatePassword fails for blank password`() {
        val result = Validation.validatePassword("")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 6 characters long", result.message)
    }
    
    @Test
    fun `validatePassword fails for whitespace-only password`() {
        val result = Validation.validatePassword("      ")
        assertFalse(result.isValid)
        assertEquals("Password cannot be empty", result.message)
    }
}