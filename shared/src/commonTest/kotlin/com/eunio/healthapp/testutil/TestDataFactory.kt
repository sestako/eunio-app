package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Enhanced factory for creating comprehensive test data objects
 */
class TestDataFactory {
    
    companion object {
        const val DEFAULT_USER_ID = "test_user_123"
        const val DEFAULT_EMAIL = "test@example.com"
        const val DEFAULT_NAME = "Test User"
        
        fun createTestUser(userId: String): User {
        return User(
            id = userId,
            email = "test@example.com",
            name = "Test User",
            onboardingComplete = true,
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        }
        
        fun createUser(
        id: String = "test_user_${Clock.System.now().toEpochMilliseconds()}",
        email: String = "test@example.com",
        name: String = "Test User",
        onboardingComplete: Boolean = true,
        primaryGoal: HealthGoal = HealthGoal.CYCLE_TRACKING,
        unitSystem: UnitSystem = UnitSystem.METRIC,
        lastModified: Long = Clock.System.now().toEpochMilliseconds()
    ): User {
        return User(
            id = id,
            email = email,
            name = name,
            onboardingComplete = onboardingComplete,
            primaryGoal = primaryGoal,
            unitSystem = unitSystem,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        }
        
        fun createTestDailyLog(userId: String): DailyLog {
        return DailyLog(
            id = "log_${userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userId,
            date = LocalDate(2024, 1, 15),
            bbt = 98.6,
            cervicalMucus = CervicalMucus.CREAMY,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            notes = "Test notes",
            symptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        }
        
        fun createDailyLog(
        id: String = "log_${Clock.System.now().toEpochMilliseconds()}",
        userId: String = "test_user",
        date: LocalDate = LocalDate(2024, 1, 15),
        bbt: Double? = 98.6,
        cervicalMucus: CervicalMucus? = CervicalMucus.CREAMY,
        sexualActivity: SexualActivity? = SexualActivity(occurred = true, protection = Protection.CONDOM),
        notes: String? = "Test notes",
        symptoms: List<Symptom> = listOf(Symptom.CRAMPS, Symptom.HEADACHE),
        mood: Mood? = Mood.HAPPY,
        lastModified: Long = Clock.System.now().toEpochMilliseconds()
    ): DailyLog {
        return DailyLog(
            id = id,
            userId = userId,
            date = date,
            bbt = bbt,
            cervicalMucus = cervicalMucus,
            sexualActivity = sexualActivity,
            notes = notes,
            symptoms = symptoms,
            mood = mood,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        }
        
        fun createTestCycle(userId: String): Cycle {
        return Cycle(
            id = "cycle_${userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userId,
            startDate = LocalDate(2024, 1, 1),
            endDate = null,
            cycleLength = null
        )
        }
        
        fun createTestInsight(userId: String): Insight {
        return Insight(
            id = "insight_${userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userId,
            generatedDate = Clock.System.now(),
            insightText = "This is a test insight about cycle patterns",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.85
        )
        }
        
        fun createTestHealthReport(userId: String): HealthReport {
        return HealthReport(
            id = "report_${userId}_${Clock.System.now().toEpochMilliseconds()}",
            userId = userId,
            generatedDate = Clock.System.now(),
            reportType = ReportType.MONTHLY,
            dateRange = DateRange(
                start = LocalDate(2024, 1, 1),
                end = LocalDate(2024, 1, 31)
            ),
            cycleSummary = CycleSummary(
                totalCycles = 2,
                averageCycleLength = 28.0,
                averageLutealPhase = 14.0,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(
                    averageOvulationDay = 14.0,
                    ovulationConfidenceRate = 0.8,
                    fertilityWindowAccuracy = 0.9
                )
            ),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = emptyList(),
                symptomTrends = emptyMap(),
                moodPatterns = MoodPatterns(
                    dominantMood = "Happy",
                    moodVariability = 0.3,
                    cyclePhaseCorrelations = emptyMap()
                ),
                bbtAnalysis = null
            ),
            insights = emptyList()
        )
        }
        
        fun createUserPreferences(userId: String): UserPreferences {
        return UserPreferences(
            userId = userId,
            unitSystem = UnitSystem.METRIC,
            isManuallySet = false,
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.SYNCED
        )
        }
    }
}