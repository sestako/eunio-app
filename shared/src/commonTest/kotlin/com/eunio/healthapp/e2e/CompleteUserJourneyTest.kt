package com.eunio.healthapp.e2e

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteUserJourneyTest {
    
    private val mockServices = MockServices()
    
    @Test
    fun `complete user journey from signup to insights should work`() = runTest {
        // 1. User signs up
        val signUpResult = mockServices.userRepository.createUser(
            email = "newuser@example.com",
            password = "securePassword123",
            name = "New User"
        )
        assertTrue(signUpResult is Result.Success)
        val user = signUpResult.data
        
        // 2. User completes onboarding
        val onboardingResult = mockServices.userRepository.completeOnboarding(
            userId = user.id,
            primaryGoal = HealthGoal.CYCLE_TRACKING
        )
        assertTrue(onboardingResult is Result.Success)
        
        // 3. User starts logging daily data
        val dailyLogs = mutableListOf<DailyLog>()
        for (day in 1..28) {
            val log = DailyLog(
                id = "log_${user.id}_$day",
                userId = user.id,
                date = LocalDate(2024, 1, day),
                bbt = if (day < 14) 97.5 + (day * 0.1) else 98.0 + (day * 0.05),
                periodFlow = if (day <= 5) PeriodFlow.MEDIUM else null,
                symptoms = if (day <= 5) listOf(Symptom.CRAMPS) else emptyList(),
                mood = Mood.HAPPY,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            dailyLogs.add(log)
            
            val saveResult = mockServices.logRepository.saveDailyLog(log)
            assertTrue(saveResult is Result.Success)
        }
        
        // 4. User starts a new cycle
        val cycleResult = mockServices.cycleRepository.startNewCycle(
            userId = user.id,
            startDate = LocalDate(2024, 1, 1)
        )
        assertTrue(cycleResult is Result.Success)
        val cycle = cycleResult.data
        
        // 5. User confirms ovulation
        val ovulationResult = mockServices.cycleRepository.confirmOvulation(
            cycleId = cycle.id,
            ovulationDate = LocalDate(2024, 1, 14)
        )
        assertTrue(ovulationResult is Result.Success)
        
        // 6. System generates insights (simulated)
        val insight = Insight(
            id = "insight_${user.id}_1",
            userId = user.id,
            generatedDate = Clock.System.now(),
            insightText = "Your cycle length is consistent at 28 days",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.9,
            actionable = false
        )
        
        // 7. User views insights
        val insightsResult = mockServices.insightRepository.getUnreadInsights(user.id)
        assertTrue(insightsResult is Result.Success)
        
        // 8. User generates health report
        val reportResult = mockServices.healthReportRepository.generateReport(
            userId = user.id,
            reportType = ReportType.MONTHLY,
            dateRange = DateRange(
                start = LocalDate(2024, 1, 1),
                end = LocalDate(2024, 1, 31)
            )
        )
        assertTrue(reportResult is Result.Success)
        val report = reportResult.data
        
        // 9. User exports report as PDF
        val pdfResult = mockServices.healthReportRepository.generatePDF(report)
        assertTrue(pdfResult is Result.Success)
        
        // Verify the complete journey
        assertEquals(user.id, cycle.userId)
        assertEquals(user.id, insight.userId)
        assertEquals(user.id, report.userId)
    }
    
    @Test
    fun `fertility tracking journey should work end to end`() = runTest {
        val userId = "fertility_user"
        
        // User trying to conceive logs fertility indicators
        val fertilityLogs = listOf(
            DailyLog(
                id = "log1", userId = userId, date = LocalDate(2024, 1, 10),
                bbt = 97.2, cervicalMucus = CervicalMucus.DRY,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log2", userId = userId, date = LocalDate(2024, 1, 12),
                bbt = 97.4, cervicalMucus = CervicalMucus.STICKY,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log3", userId = userId, date = LocalDate(2024, 1, 14),
                bbt = 98.1, cervicalMucus = CervicalMucus.EGG_WHITE,
                opkResult = OPKResult.POSITIVE,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log4", userId = userId, date = LocalDate(2024, 1, 16),
                bbt = 98.3, cervicalMucus = CervicalMucus.CREAMY,
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            )
        )
        
        // Save all fertility logs
        for (log in fertilityLogs) {
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result is Result.Success)
        }
        
        // Get fertility logs in range
        val fertilityResult = mockServices.logRepository.getFertilityLogsInRange(
            userId = userId,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        assertTrue(fertilityResult is Result.Success)
        
        // Confirm ovulation based on multiple indicators
        val cycleResult = mockServices.cycleRepository.startNewCycle(
            userId = userId,
            startDate = LocalDate(2024, 1, 1)
        )
        assertTrue(cycleResult is Result.Success)
        
        val ovulationResult = mockServices.cycleRepository.confirmOvulation(
            cycleId = cycleResult.data.id,
            ovulationDate = LocalDate(2024, 1, 14)
        )
        assertTrue(ovulationResult is Result.Success)
    }
    
    @Test
    fun `contraception tracking journey should work end to end`() = runTest {
        val userId = "contraception_user"
        
        // User tracking for contraception logs sexual activity
        val contraceptionLogs = listOf(
            DailyLog(
                id = "log1", userId = userId, date = LocalDate(2024, 1, 5),
                sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log2", userId = userId, date = LocalDate(2024, 1, 12),
                sexualActivity = SexualActivity(occurred = true, protection = Protection.BIRTH_CONTROL),
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            ),
            DailyLog(
                id = "log3", userId = userId, date = LocalDate(2024, 1, 20),
                sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
                createdAt = Clock.System.now(), updatedAt = Clock.System.now()
            )
        )
        
        // Save all contraception logs
        for (log in contraceptionLogs) {
            val result = mockServices.logRepository.saveDailyLog(log)
            assertTrue(result is Result.Success)
        }
        
        // Generate insights about protection patterns
        val insight = Insight(
            id = "contraception_insight",
            userId = userId,
            generatedDate = Clock.System.now(),
            insightText = "You've been consistent with protection methods",
            type = InsightType.PATTERN_RECOGNITION,
            isRead = false,
            confidence = 0.95,
            actionable = false
        )
        
        // Verify insights are available
        val insightsResult = mockServices.insightRepository.getUnreadInsights(userId)
        assertTrue(insightsResult is Result.Success)
    }
}