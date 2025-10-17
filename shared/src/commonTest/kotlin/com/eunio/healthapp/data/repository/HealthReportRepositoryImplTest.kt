package com.eunio.healthapp.data.repository

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.service.PDFGenerationService
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.data.remote.FirestoreService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthReportRepositoryImplTest {

    @Test
    fun `validateReportData returns error when cycle repository fails`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository().apply { shouldFail = true }
        val mockLogRepo = MockLogRepository()
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val result = repository.validateReportData(
            "user-123",
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        
        assertTrue(result is Result.Error)
    }

    @Test
    fun `validateReportData returns validation result when data is sufficient`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository()
        val mockLogRepo = MockLogRepository()
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val result = repository.validateReportData(
            "user-123",
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        
        assertTrue(result is Result.Success)
        assertTrue(result.data.isValid)
    }

    @Test
    fun `generateReport returns error when validation fails`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository().apply { 
            cycles = listOf() // Empty list will fail validation
        }
        val mockLogRepo = MockLogRepository().apply {
            logs = listOf() // Empty list will fail validation
        }
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val result = repository.generateReport(
            "user-123",
            ReportType.MONTHLY,
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error.message?.contains("Insufficient data") == true)
    }

    @Test
    fun `generateReport creates report with valid data`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository()
        val mockLogRepo = MockLogRepository()
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val result = repository.generateReport(
            "user-123",
            ReportType.MONTHLY,
            DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31))
        )
        
        assertTrue(result is Result.Success)
        assertEquals("user-123", result.data.userId)
        assertEquals(ReportType.MONTHLY, result.data.reportType)
        assertTrue(result.data.id.isNotEmpty())
    }

    @Test
    fun `generatePDF updates report with PDF URL`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository()
        val mockLogRepo = MockLogRepository()
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val report = createTestReport()
        val result = repository.generatePDF(report)
        
        assertTrue(result is Result.Success)
        assertEquals("test-pdf-url", result.data)
    }

    @Test
    fun `shareReport creates shareable link and updates report`() = runTest {
        val mockFirestore = MockFirestoreService()
        val mockCycleRepo = MockCycleRepository()
        val mockLogRepo = MockLogRepository()
        val mockInsightRepo = MockInsightRepository()
        val mockPdfService = MockPDFGenerationService()
        
        val repository = HealthReportRepositoryImpl(
            mockFirestore, mockCycleRepo, mockLogRepo, mockInsightRepo, mockPdfService
        )
        
        val result = repository.shareReport("test-report", 7)
        
        assertTrue(result is Result.Success)
        assertEquals("test-share-url", result.data)
    }

    private fun createTestReport(): HealthReport {
        return HealthReport(
            id = "test-report",
            userId = "user-123",
            generatedDate = Clock.System.now(),
            reportType = ReportType.MONTHLY,
            dateRange = DateRange(
                start = LocalDate(2024, 1, 1),
                end = LocalDate(2024, 1, 31)
            ),
            cycleSummary = CycleSummary(
                totalCycles = 3,
                averageCycleLength = 28.0,
                averageLutealPhase = 14.0,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(14.0, 0.85, 0.90)
            ),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = emptyList(),
                symptomTrends = emptyMap(),
                moodPatterns = MoodPatterns("NEUTRAL", 0.3, emptyMap()),
                bbtAnalysis = null
            ),
            insights = emptyList()
        )
    }

    // Mock implementations
    private class MockFirestoreService : FirestoreService {
        override suspend fun saveHealthReport(report: HealthReport): Result<Unit> = Result.Success(Unit)
        override suspend fun getHealthReport(reportId: String): Result<HealthReport?> = Result.Success(createTestReport())
        override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> = Result.Success(emptyList())
        override suspend fun updateHealthReport(report: HealthReport): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteHealthReport(reportId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> = Result.Success("test-share-url")
        override suspend fun revokeShareableLink(reportId: String): Result<Unit> = Result.Success(Unit)
        
        // Other required methods (simplified)
        override suspend fun getUser(userId: String): Result<User?> = Result.Success(null)
        override suspend fun saveUser(user: User): Result<Unit> = Result.Success(Unit)
        override suspend fun updateUser(user: User): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteUser(userId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> = Result.Success(null)
        override suspend fun getCurrentCycle(userId: String): Result<Cycle?> = Result.Success(null)
        override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> = Result.Success(emptyList())
        override suspend fun saveCycle(cycle: Cycle): Result<Unit> = Result.Success(Unit)
        override suspend fun updateCycle(cycle: Cycle): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> = Result.Success(null)
        override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> = Result.Success(null)
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.Success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.Success(emptyList())
        override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> = Result.Success(Unit)
        override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> = Result.Success(null)
        override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun saveInsight(insight: Insight): Result<Unit> = Result.Success(Unit)
        override suspend fun updateInsight(insight: Insight): Result<Unit> = Result.Success(Unit)
        override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun batchSaveUsers(users: List<User>): Result<Unit> = Result.Success(Unit)
        override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> = Result.Success(Unit)
        override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> = Result.Success(Unit)
        override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> = Result.Success(Unit)
        override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> = Result.Success(null)
        override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> = Result.Success(Unit)
        override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<com.eunio.healthapp.data.remote.SyncData> = Result.Success(com.eunio.healthapp.data.remote.SyncData())
        
        // Settings operations
        override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteUserSettings(userId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit> = Result.Success(Unit)
        override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>> = Result.Success(emptyList())
        override suspend fun deleteSettingsHistory(userId: String): Result<Unit> = Result.Success(Unit)
        
        private fun createTestReport(): HealthReport {
            return HealthReport(
                id = "test-report",
                userId = "user-123",
                generatedDate = Clock.System.now(),
                reportType = ReportType.MONTHLY,
                dateRange = DateRange(LocalDate(2024, 1, 1), LocalDate(2024, 1, 31)),
                cycleSummary = CycleSummary(3, 28.0, 14.0, CycleRegularity.REGULAR, OvulationPatterns(14.0, 0.85, 0.90)),
                symptomAnalysis = SymptomAnalysis(emptyList(), emptyMap(), MoodPatterns("NEUTRAL", 0.3, emptyMap()), null),
                insights = emptyList()
            )
        }
    }

    private class MockCycleRepository : CycleRepository {
        var shouldFail = false
        var cycles = listOf(
            Cycle("cycle1", "user-123", LocalDate(2024, 1, 1), LocalDate(2024, 1, 28), null, null, 28, 14),
            Cycle("cycle2", "user-123", LocalDate(2024, 2, 1), LocalDate(2024, 2, 28), null, null, 28, 14),
            Cycle("cycle3", "user-123", LocalDate(2024, 3, 1), null, null, null, null, null)
        )

        override suspend fun getCurrentCycle(userId: String): Result<Cycle?> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(cycles.lastOrNull())
        override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(cycles)
        override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> = Result.Success(Unit)
        override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> = Result.Success(cycles.first())
        override suspend fun updateCycle(cycle: Cycle): Result<Unit> = Result.Success(Unit)
        override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> = Result.Success(Unit)
        override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> = Result.Success(28.0)
        override suspend fun getCyclesInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Cycle>> = Result.Success(cycles)
        override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> = Result.Success(LocalDate(2024, 2, 1))
    }

    private class MockLogRepository : LogRepository {
        var shouldFail = false
        var logs = listOf(
            DailyLog("log1", "user-123", LocalDate(2024, 1, 1), PeriodFlow.MEDIUM, listOf(Symptom.CRAMPS), Mood.NEUTRAL, null, 98.2, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log2", "user-123", LocalDate(2024, 1, 2), null, listOf(Symptom.BLOATING), Mood.HAPPY, null, 98.1, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log3", "user-123", LocalDate(2024, 1, 3), null, emptyList(), Mood.CALM, null, 98.3, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log4", "user-123", LocalDate(2024, 1, 4), null, listOf(Symptom.HEADACHE), Mood.NEUTRAL, null, 98.0, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log5", "user-123", LocalDate(2024, 1, 5), null, listOf(Symptom.FATIGUE), Mood.TIRED, null, 98.4, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log6", "user-123", LocalDate(2024, 1, 6), null, listOf(Symptom.MOOD_SWINGS), Mood.IRRITABLE, null, 98.2, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log7", "user-123", LocalDate(2024, 1, 7), null, listOf(Symptom.BREAST_TENDERNESS), Mood.ANXIOUS, null, 98.5, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log8", "user-123", LocalDate(2024, 1, 8), null, listOf(Symptom.ACNE), Mood.HAPPY, null, 98.1, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log9", "user-123", LocalDate(2024, 1, 9), null, listOf(Symptom.BACK_PAIN), Mood.CALM, null, 98.3, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log10", "user-123", LocalDate(2024, 1, 10), null, listOf(Symptom.NAUSEA), Mood.NEUTRAL, null, 98.2, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log11", "user-123", LocalDate(2024, 1, 11), null, listOf(Symptom.FOOD_CRAVINGS), Mood.ENERGETIC, null, 98.4, null, null, null, Clock.System.now(), Clock.System.now()),
            DailyLog("log12", "user-123", LocalDate(2024, 1, 12), null, emptyList(), Mood.HAPPY, null, 98.0, null, null, null, Clock.System.now(), Clock.System.now())
        )

        override suspend fun saveDailyLog(log: DailyLog): Result<Unit> = Result.Success(Unit)
        override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> = Result.Success(logs.firstOrNull())
        override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(logs)
        override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> = Result.Success(Unit)
        override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(logs.filter { it.bbt != null })
        override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(logs.filter { it.cervicalMucus != null || it.opkResult != null })
        override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = 
            if (shouldFail) Result.Error(AppError.UnknownError("Failed")) else Result.Success(logs.filter { it.periodFlow != null })
        override suspend fun getLogCount(userId: String): Result<Int> = Result.Success(logs.size)
        override suspend fun getLogsBySymptoms(userId: String, symptoms: List<Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> = Result.Success(emptyList())
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.Success(logs.take(limit))
    }

    private class MockInsightRepository : InsightRepository {
        override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun markInsightAsRead(insightId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun deleteInsight(insightId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getActionableInsights(userId: String): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun getHighConfidenceInsights(userId: String, minConfidence: Double, limit: Int): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun getInsightCount(userId: String, includeRead: Boolean): Result<Int> = Result.Success(0)
        override suspend fun getInsightsByType(userId: String, type: InsightType, limit: Int): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun getInsightsForLogs(userId: String, logIds: List<String>): Result<List<Insight>> = Result.Success(emptyList())
        override suspend fun markAllInsightsAsRead(userId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun saveInsight(insight: Insight): Result<Unit> = Result.Success(Unit)
    }

    private class MockPDFGenerationService : PDFGenerationService {
        override suspend fun generateReportPDF(report: HealthReport): String = "test-pdf-url"
        override suspend fun generateCustomPDF(report: HealthReport, templateType: com.eunio.healthapp.domain.service.PDFTemplate): String = "test-pdf-url"
        override fun validateReportForPDF(report: HealthReport): Boolean = true
    }
}