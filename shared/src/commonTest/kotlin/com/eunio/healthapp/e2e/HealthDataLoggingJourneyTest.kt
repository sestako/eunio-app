package com.eunio.healthapp.e2e

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * End-to-end tests for core health data logging and viewing features.
 * Tests complete user journeys for logging various health data types and viewing insights.
 * 
 * Requirements: 3.3, 3.4
 */
class HealthDataLoggingJourneyTest {
    
    private lateinit var mockServices: MockServices
    private lateinit var testUser: User
    
    @BeforeTest
    fun setup() = runTest {
        mockServices = MockServices()
        
        // Create and set up a test user
        val signUpResult = mockServices.userRepository.createUser(
            "healthlogger@example.com",
            "HealthPassword123",
            "Health Logger"
        )
        assertTrue(signUpResult.isSuccess)
        testUser = signUpResult.getOrNull()!!
        
        // Complete onboarding
        mockServices.userRepository.completeOnboarding(testUser.id, HealthGoal.CYCLE_TRACKING)
    }
    
    // Daily Health Logging Journey Tests
    
    @Test
    fun `complete daily health logging journey works end-to-end`() = runTest {
        // Day 1: User logs basic information
        val day1Log = DailyLog(
            id = "day1-${testUser.id}",
            userId = testUser.id,
            date = LocalDate(2024, 1, 1),
            bbt = 97.8,
            mood = Mood.HAPPY,
            notes = "Starting my health tracking journey!",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day1Result = mockServices.logRepository.saveDailyLog(day1Log)
        assertTrue(day1Result.isSuccess, "Day 1 log should be saved successfully")
        
        // Day 2: User logs period start
        val day2Log = DailyLog(
            id = "day2-${testUser.id}",
            userId = testUser.id,
            date = LocalDate(2024, 1, 2),
            bbt = 97.6,
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
            mood = Mood.IRRITABLE,
            notes = "Period started today",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day2Result = mockServices.logRepository.saveDailyLog(day2Log)
        assertTrue(day2Result.isSuccess, "Day 2 log should be saved successfully")
        
        // Day 3-5: User continues logging period days
        val periodDays = (3..5).map { day ->
            DailyLog(
                id = "day$day-${testUser.id}",
                userId = testUser.id,
                date = LocalDate(2024, 1, day),
                bbt = 97.4 + (day * 0.1),
                periodFlow = when (day) {
                    3 -> PeriodFlow.HEAVY
                    4 -> PeriodFlow.MEDIUM
                    5 -> PeriodFlow.LIGHT
                    else -> null
                },
                symptoms = if (day <= 4) listOf(Symptom.CRAMPS) else emptyList(),
                mood = Mood.NEUTRAL,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        periodDays.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result.isSuccess, "Period day log should be saved successfully")
        }
        
        // Day 14: User logs ovulation indicators
        val ovulationLog = DailyLog(
            id = "day14-${testUser.id}",
            userId = testUser.id,
            date = LocalDate(2024, 1, 14),
            bbt = 98.2, // Temperature spike
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.POSITIVE,
            mood = Mood.HAPPY,
            notes = "Ovulation signs detected",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val ovulationResult = mockServices.logRepository.saveDailyLog(ovulationLog)
        assertTrue(ovulationResult.isSuccess, "Ovulation log should be saved successfully")
        
        // User views their logged data
        val recentLogsResult = mockServices.logRepository.getRecentLogs(testUser.id, 10)
        assertTrue(recentLogsResult.isSuccess)
        val recentLogs = recentLogsResult.getOrNull()!!
        
        assertEquals(6, recentLogs.size, "User should see all 6 logged days")
        
        // User views period-specific data
        val periodLogsResult = mockServices.logRepository.getPeriodLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(periodLogsResult.isSuccess)
        val periodLogs = periodLogsResult.getOrNull()!!
        
        assertEquals(4, periodLogs.size, "User should see 4 period days")
        assertTrue(periodLogs.all { it.periodFlow != null }, "All period logs should have flow data")
        
        // User views fertility data
        val fertilityLogsResult = mockServices.logRepository.getFertilityLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(fertilityLogsResult.isSuccess)
        val fertilityLogs = fertilityLogsResult.getOrNull()!!
        
        assertEquals(1, fertilityLogs.size, "User should see 1 fertility tracking day")
        assertEquals(CervicalMucus.EGG_WHITE, fertilityLogs.first().cervicalMucus)
        assertEquals(OPKResult.POSITIVE, fertilityLogs.first().opkResult)
        
        // System generates cycle insights
        val cycleInsight = Insight(
            id = "cycle-insight-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Your cycle appears to be 28 days with ovulation around day 14",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.85,
            actionable = true
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(cycleInsight)
        assertTrue(insightResult.isSuccess)
        
        val unreadInsightsResult = mockServices.insightRepository.getUnreadInsights(testUser.id)
        assertTrue(unreadInsightsResult.isSuccess)
        val insights = unreadInsightsResult.getOrNull()!!
        
        assertEquals(1, insights.size)
        assertEquals(InsightType.PATTERN_RECOGNITION, insights.first().type)
        assertTrue(insights.first().actionable)
    }
    
    @Test
    fun `fertility tracking journey works end-to-end`() = runTest {
        // User is trying to conceive and tracks fertility indicators
        
        // Cycle day 8: Start tracking
        val day8Log = DailyLog(
            id = "fertility-day8",
            userId = testUser.id,
            date = LocalDate(2024, 1, 8),
            bbt = 97.2,
            cervicalMucus = CervicalMucus.DRY,
            opkResult = OPKResult.NEGATIVE,
            notes = "Starting fertility tracking",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day8Result = mockServices.logRepository.saveDailyLog(day8Log)
        assertTrue(day8Result.isSuccess)
        
        // Cycle day 10: Mucus changes
        val day10Log = DailyLog(
            id = "fertility-day10",
            userId = testUser.id,
            date = LocalDate(2024, 1, 10),
            bbt = 97.3,
            cervicalMucus = CervicalMucus.STICKY,
            opkResult = OPKResult.NEGATIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day10Result = mockServices.logRepository.saveDailyLog(day10Log)
        assertTrue(day10Result.isSuccess)
        
        // Cycle day 12: Approaching ovulation
        val day12Log = DailyLog(
            id = "fertility-day12",
            userId = testUser.id,
            date = LocalDate(2024, 1, 12),
            bbt = 97.4,
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day12Result = mockServices.logRepository.saveDailyLog(day12Log)
        assertTrue(day12Result.isSuccess)
        
        // Cycle day 14: Ovulation day
        val ovulationDay = DailyLog(
            id = "fertility-ovulation",
            userId = testUser.id,
            date = LocalDate(2024, 1, 14),
            bbt = 98.1, // Temperature spike
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.POSITIVE,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.NONE),
            notes = "Peak fertility day!",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val ovulationResult = mockServices.logRepository.saveDailyLog(ovulationDay)
        assertTrue(ovulationResult.isSuccess)
        
        // Cycle day 15: Post-ovulation
        val day15Log = DailyLog(
            id = "fertility-day15",
            userId = testUser.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.3, // Sustained high temperature
            cervicalMucus = CervicalMucus.CREAMY,
            opkResult = OPKResult.NEGATIVE,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val day15Result = mockServices.logRepository.saveDailyLog(day15Log)
        assertTrue(day15Result.isSuccess)
        
        // User reviews fertility data
        val fertilityLogsResult = mockServices.logRepository.getFertilityLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(fertilityLogsResult.isSuccess)
        val fertilityLogs = fertilityLogsResult.getOrNull()!!
        
        assertEquals(5, fertilityLogs.size, "Should have 5 fertility tracking entries")
        
        // Verify ovulation detection
        val ovulationLogs = fertilityLogs.filter { it.opkResult == OPKResult.POSITIVE }
        assertEquals(1, ovulationLogs.size, "Should detect one ovulation day")
        assertEquals(LocalDate(2024, 1, 14), ovulationLogs.first().date)
        
        // User gets fertility insights
        val fertilityInsight = Insight(
            id = "fertility-insight-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Your fertile window was January 12-16, with ovulation on January 14",
            type = InsightType.FERTILITY_WINDOW,
            isRead = false,
            confidence = 0.9,
            actionable = true
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(fertilityInsight)
        assertTrue(insightResult.isSuccess)
        
        val fertilityInsightsResult = mockServices.insightRepository.getInsightsByType(
            testUser.id,
            InsightType.FERTILITY_WINDOW,
            5
        )
        assertTrue(fertilityInsightsResult.isSuccess)
        val insights = fertilityInsightsResult.getOrNull()!!
        
        assertEquals(1, insights.size)
        assertTrue(insights.first().confidence >= 0.9)
    }
    
    @Test
    fun `symptom tracking and pattern recognition works end-to-end`() = runTest {
        // User tracks various symptoms over multiple cycles
        
        val symptomLogs = listOf(
            // Cycle 1 - PMS symptoms
            DailyLog(
                id = "symptom-1", userId = testUser.id, date = LocalDate(2024, 1, 25),
                symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING), mood = Mood.IRRITABLE,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "symptom-2", userId = testUser.id, date = LocalDate(2024, 1, 26),
                symptoms = listOf(Symptom.HEADACHE, Symptom.FATIGUE), mood = Mood.SAD,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "symptom-3", userId = testUser.id, date = LocalDate(2024, 1, 27),
                symptoms = listOf(Symptom.CRAMPS, Symptom.BREAST_TENDERNESS), mood = Mood.IRRITABLE,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            
            // Cycle 2 - Similar pattern
            DailyLog(
                id = "symptom-4", userId = testUser.id, date = LocalDate(2024, 2, 22),
                symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING), mood = Mood.IRRITABLE,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "symptom-5", userId = testUser.id, date = LocalDate(2024, 2, 23),
                symptoms = listOf(Symptom.HEADACHE), mood = Mood.SAD,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            
            // Mid-cycle symptoms
            DailyLog(
                id = "symptom-6", userId = testUser.id, date = LocalDate(2024, 2, 14),
                symptoms = listOf(Symptom.BREAST_TENDERNESS), mood = Mood.HAPPY,
                notes = "Ovulation symptoms",
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            )
        )
        
        // User logs all symptoms
        symptomLogs.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result.isSuccess, "Symptom log should be saved successfully")
        }
        
        // User searches for specific symptoms
        val crampsLogsResult = mockServices.logRepository.getLogsBySymptoms(
            testUser.id,
            listOf(Symptom.CRAMPS),
            LocalDate(2024, 1, 1),
            LocalDate(2024, 2, 28)
        )
        assertTrue(crampsLogsResult.isSuccess)
        val crampsLogs = crampsLogsResult.getOrNull()!!
        
        assertEquals(3, crampsLogs.size, "Should find 3 logs with cramps")
        assertTrue(crampsLogs.all { it.symptoms.contains(Symptom.CRAMPS) })
        
        // User searches for headache symptoms
        val headacheLogsResult = mockServices.logRepository.getLogsBySymptoms(
            testUser.id,
            listOf(Symptom.HEADACHE),
            LocalDate(2024, 1, 1),
            LocalDate(2024, 2, 28)
        )
        assertTrue(headacheLogsResult.isSuccess)
        val headacheLogs = headacheLogsResult.getOrNull()!!
        
        assertEquals(2, headacheLogs.size, "Should find 2 logs with headaches")
        
        // System generates symptom pattern insights
        val symptomInsight = Insight(
            id = "symptom-pattern-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "You commonly experience cramps and bloating 3-5 days before your period",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.8,
            actionable = true
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(symptomInsight)
        assertTrue(insightResult.isSuccess)
        
        val symptomInsightsResult = mockServices.insightRepository.getInsightsByType(
            testUser.id,
            InsightType.PATTERN_RECOGNITION,
            5
        )
        assertTrue(symptomInsightsResult.isSuccess)
        val insights = symptomInsightsResult.getOrNull()!!
        
        assertEquals(1, insights.size)
        assertEquals(InsightType.PATTERN_RECOGNITION, insights.first().type)
        assertTrue(insights.first().actionable)
    }
    
    @Test
    fun `mood tracking and correlation analysis works end-to-end`() = runTest {
        // User tracks mood throughout cycle
        
        val moodLogs = listOf(
            // Follicular phase - generally positive
            DailyLog(
                id = "mood-1", userId = testUser.id, date = LocalDate(2024, 1, 8),
                mood = Mood.HAPPY, bbt = 97.2,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "mood-2", userId = testUser.id, date = LocalDate(2024, 1, 10),
                mood = Mood.ENERGETIC, bbt = 97.3,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "mood-3", userId = testUser.id, date = LocalDate(2024, 1, 12),
                mood = Mood.HAPPY, bbt = 97.4,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            
            // Ovulation - peak mood
            DailyLog(
                id = "mood-4", userId = testUser.id, date = LocalDate(2024, 1, 14),
                mood = Mood.ENERGETIC, bbt = 98.1,
                cervicalMucus = CervicalMucus.EGG_WHITE,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            
            // Luteal phase - mood changes
            DailyLog(
                id = "mood-5", userId = testUser.id, date = LocalDate(2024, 1, 20),
                mood = Mood.NEUTRAL, bbt = 98.2,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "mood-6", userId = testUser.id, date = LocalDate(2024, 1, 24),
                mood = Mood.IRRITABLE, bbt = 98.0,
                symptoms = listOf(Symptom.BLOATING),
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "mood-7", userId = testUser.id, date = LocalDate(2024, 1, 26),
                mood = Mood.SAD, bbt = 97.8,
                symptoms = listOf(Symptom.CRAMPS),
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            )
        )
        
        // User logs all mood data
        moodLogs.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result.isSuccess, "Mood log should be saved successfully")
        }
        
        // User views their mood patterns
        val allLogsResult = mockServices.logRepository.getLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(allLogsResult.isSuccess)
        val allLogs = allLogsResult.getOrNull()!!
        
        assertEquals(7, allLogs.size, "Should have 7 mood tracking entries")
        
        // Analyze mood distribution
        val moodCounts = allLogs.groupBy { it.mood }.mapValues { it.value.size }
        assertTrue(moodCounts[Mood.HAPPY]!! >= 2, "Should have multiple happy days")
        assertTrue(moodCounts[Mood.ENERGETIC]!! >= 2, "Should have multiple energetic days")
        
        // System generates mood correlation insights
        val moodInsight = Insight(
            id = "mood-correlation-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Your mood tends to be most positive around ovulation and lowest before your period",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.75,
            actionable = true
        )
        
        val insightResult = mockServices.insightRepository.saveInsight(moodInsight)
        assertTrue(insightResult.isSuccess)
        
        val moodInsightsResult = mockServices.insightRepository.getInsightsByType(
            testUser.id,
            InsightType.PATTERN_RECOGNITION,
            5
        )
        assertTrue(moodInsightsResult.isSuccess)
        val insights = moodInsightsResult.getOrNull()!!
        
        assertEquals(1, insights.size)
        assertEquals(InsightType.PATTERN_RECOGNITION, insights.first().type)
    }
    
    @Test
    fun `data editing and correction workflow works correctly`() = runTest {
        // User logs initial data
        val originalLog = DailyLog(
            id = "editable-log",
            userId = testUser.id,
            date = LocalDate(2024, 1, 15),
            bbt = 97.8,
            mood = Mood.HAPPY,
            notes = "Original entry",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val originalResult = mockServices.logRepository.saveDailyLog(originalLog)
        assertTrue(originalResult.isSuccess)
        
        // User realizes they made a mistake and updates the entry
        val correctedLog = originalLog.copy(
            bbt = 98.2, // Corrected temperature
            periodFlow = PeriodFlow.LIGHT, // Added missing period data
            symptoms = listOf(Symptom.CRAMPS), // Added symptoms
            notes = "Corrected entry - added period flow and symptoms",
            updatedAt = Clock.System.now()
        )
        
        val correctedResult = mockServices.logRepository.saveDailyLog(correctedLog)
        assertTrue(correctedResult.isSuccess)
        
        // User retrieves the updated log
        val retrievedResult = mockServices.logRepository.getDailyLog(testUser.id, LocalDate(2024, 1, 15))
        assertTrue(retrievedResult.isSuccess)
        val retrievedLog = retrievedResult.getOrNull()!!
        
        // Verify corrections were applied
        assertEquals(98.2, retrievedLog.bbt, "Temperature should be corrected")
        assertEquals(PeriodFlow.LIGHT, retrievedLog.periodFlow, "Period flow should be added")
        assertEquals(listOf(Symptom.CRAMPS), retrievedLog.symptoms, "Symptoms should be added")
        assertTrue(retrievedLog.notes!!.contains("Corrected"), "Notes should reflect correction")
        
        // User can still see it in period logs
        val periodLogsResult = mockServices.logRepository.getPeriodLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(periodLogsResult.isSuccess)
        val periodLogs = periodLogsResult.getOrNull()!!
        
        assertEquals(1, periodLogs.size, "Corrected log should appear in period logs")
        assertEquals(PeriodFlow.LIGHT, periodLogs.first().periodFlow)
    }
    
    @Test
    fun `data deletion and recovery workflow works correctly`() = runTest {
        // User logs multiple days
        val logsToDelete = (1..5).map { day ->
            DailyLog(
                id = "deletable-$day",
                userId = testUser.id,
                date = LocalDate(2024, 1, day),
                bbt = 97.0 + (day * 0.1),
                mood = Mood.HAPPY,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        logsToDelete.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result.isSuccess)
        }
        
        // Verify all logs are saved
        val initialCountResult = mockServices.logRepository.getLogCount(testUser.id)
        assertTrue(initialCountResult.isSuccess)
        assertEquals(5, initialCountResult.getOrNull())
        
        // User deletes one specific day
        val deleteResult = mockServices.logRepository.deleteDailyLog(testUser.id, LocalDate(2024, 1, 3))
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        val afterDeleteCountResult = mockServices.logRepository.getLogCount(testUser.id)
        assertTrue(afterDeleteCountResult.isSuccess)
        assertEquals(4, afterDeleteCountResult.getOrNull(), "Should have 4 logs after deletion")
        
        // Verify specific log is gone
        val deletedLogResult = mockServices.logRepository.getDailyLog(testUser.id, LocalDate(2024, 1, 3))
        assertTrue(deletedLogResult.isSuccess)
        assertNull(deletedLogResult.getOrNull(), "Deleted log should not be retrievable")
        
        // Verify other logs are still there
        val remainingLogResult = mockServices.logRepository.getDailyLog(testUser.id, LocalDate(2024, 1, 2))
        assertTrue(remainingLogResult.isSuccess)
        assertNotNull(remainingLogResult.getOrNull(), "Other logs should still exist")
        
        // User can re-add data for the deleted day
        val newLogForDeletedDay = DailyLog(
            id = "replacement-log",
            userId = testUser.id,
            date = LocalDate(2024, 1, 3),
            bbt = 98.0,
            mood = Mood.NEUTRAL,
            notes = "Re-added after deletion",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val reAddResult = mockServices.logRepository.saveDailyLog(newLogForDeletedDay)
        assertTrue(reAddResult.isSuccess)
        
        // Verify re-addition
        val finalCountResult = mockServices.logRepository.getLogCount(testUser.id)
        assertTrue(finalCountResult.isSuccess)
        assertEquals(5, finalCountResult.getOrNull(), "Should have 5 logs after re-addition")
        
        val reAddedLogResult = mockServices.logRepository.getDailyLog(testUser.id, LocalDate(2024, 1, 3))
        assertTrue(reAddedLogResult.isSuccess)
        val reAddedLog = reAddedLogResult.getOrNull()!!
        
        assertEquals(98.0, reAddedLog.bbt)
        assertTrue(reAddedLog.notes!!.contains("Re-added"))
    }
    
    @Test
    fun `bulk data import and validation works correctly`() = runTest {
        // User imports a month of historical data
        val bulkLogs = (1..31).map { day ->
            DailyLog(
                id = "bulk-$day",
                userId = testUser.id,
                date = LocalDate(2024, 1, day),
                bbt = 97.0 + ((day % 28) * 0.05), // Simulate cycle pattern
                periodFlow = if (day <= 5) {
                    when (day) {
                        1, 2 -> PeriodFlow.HEAVY
                        3, 4 -> PeriodFlow.MEDIUM
                        5 -> PeriodFlow.LIGHT
                        else -> null
                    }
                } else null,
                mood = when (day % 4) {
                    0 -> Mood.HAPPY
                    1 -> Mood.ENERGETIC
                    2 -> Mood.NEUTRAL
                    3 -> Mood.IRRITABLE
                    else -> Mood.HAPPY
                },
                symptoms = if (day in 25..27) listOf(Symptom.CRAMPS) else emptyList(),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // Import all logs
        var successCount = 0
        bulkLogs.forEach { log ->
            val result = mockServices.logRepository.saveDailyLog(log)
            if (result.isSuccess) successCount++
        }
        
        assertEquals(31, successCount, "All bulk logs should be imported successfully")
        
        // Verify total count
        val totalCountResult = mockServices.logRepository.getLogCount(testUser.id)
        assertTrue(totalCountResult.isSuccess)
        assertEquals(31, totalCountResult.getOrNull())
        
        // Verify period data
        val periodLogsResult = mockServices.logRepository.getPeriodLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(periodLogsResult.isSuccess)
        val periodLogs = periodLogsResult.getOrNull()!!
        
        assertEquals(5, periodLogs.size, "Should have 5 period days")
        
        // Verify BBT data
        val bbtLogsResult = mockServices.logRepository.getBBTLogsInRange(
            testUser.id,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 31)
        )
        assertTrue(bbtLogsResult.isSuccess)
        val bbtLogs = bbtLogsResult.getOrNull()!!
        
        assertEquals(31, bbtLogs.size, "Should have BBT data for all days")
        assertTrue(bbtLogs.all { it.bbt != null }, "All logs should have BBT data")
        
        // System can generate comprehensive insights from bulk data
        val bulkInsight = Insight(
            id = "bulk-analysis-${testUser.id}",
            userId = testUser.id,
            generatedDate = Clock.System.now(),
            insightText = "Based on your complete cycle data, your average cycle length is 28 days",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.95,
            actionable = false
        )
        
        val bulkInsightResult = mockServices.insightRepository.saveInsight(bulkInsight)
        assertTrue(bulkInsightResult.isSuccess)
        
        val highConfidenceInsightsResult = mockServices.insightRepository.getHighConfidenceInsights(
            testUser.id,
            0.9,
            5
        )
        assertTrue(highConfidenceInsightsResult.isSuccess)
        val highConfidenceInsights = highConfidenceInsightsResult.getOrNull()!!
        
        assertEquals(1, highConfidenceInsights.size)
        assertTrue(highConfidenceInsights.first().confidence >= 0.9)
    }
}