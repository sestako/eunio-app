package com.eunio.healthapp.domain.usecase.reports

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.HealthReportRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateHealthReportUseCaseTest {

    @Test
    fun `invoke returns error when start date is after end date`() = runTest {
        val mockRepository = MockHealthReportRepository()
        val useCase = GenerateHealthReportUseCase(mockRepository)
        
        val result = useCase(
            userId = "user-123",
            reportType = ReportType.MONTHLY,
            startDate = LocalDate(2024, 2, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error.message?.contains("Start date must be before end date") == true)
    }

    @Test
    fun `invoke returns error when validation fails`() = runTest {
        val mockRepository = MockHealthReportRepository().apply {
            validationResult = ReportValidationResult(
                isValid = false,
                errors = listOf("Insufficient data"),
                warnings = emptyList()
            )
        }
        val useCase = GenerateHealthReportUseCase(mockRepository)
        
        val result = useCase(
            userId = "user-123",
            reportType = ReportType.MONTHLY,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Error)
        assertTrue(result.error.message?.contains("Insufficient data") == true)
    }

    @Test
    fun `invoke returns error when report generation fails`() = runTest {
        val mockRepository = MockHealthReportRepository().apply {
            shouldFailGeneration = true
        }
        val useCase = GenerateHealthReportUseCase(mockRepository)
        
        val result = useCase(
            userId = "user-123",
            reportType = ReportType.MONTHLY,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Error)
    }

    @Test
    fun `invoke returns error when save fails`() = runTest {
        val mockRepository = MockHealthReportRepository().apply {
            shouldFailSave = true
        }
        val useCase = GenerateHealthReportUseCase(mockRepository)
        
        val result = useCase(
            userId = "user-123",
            reportType = ReportType.MONTHLY,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Error)
    }

    @Test
    fun `invoke returns success when all operations succeed`() = runTest {
        val mockRepository = MockHealthReportRepository()
        val useCase = GenerateHealthReportUseCase(mockRepository)
        
        val result = useCase(
            userId = "user-123",
            reportType = ReportType.MONTHLY,
            startDate = LocalDate(2024, 1, 1),
            endDate = LocalDate(2024, 1, 31)
        )
        
        assertTrue(result is Result.Success)
        assertEquals("test-report", result.data.id)
        assertEquals("user-123", result.data.userId)
        assertEquals(ReportType.MONTHLY, result.data.reportType)
    }

    private class MockHealthReportRepository : HealthReportRepository {
        var validationResult = ReportValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = emptyList()
        )
        var shouldFailGeneration = false
        var shouldFailSave = false

        override suspend fun validateReportData(userId: String, dateRange: DateRange): Result<ReportValidationResult> {
            return Result.Success(validationResult)
        }

        override suspend fun generateReport(userId: String, reportType: ReportType, dateRange: DateRange): Result<HealthReport> {
            return if (shouldFailGeneration) {
                Result.Error(AppError.UnknownError("Generation failed"))
            } else {
                Result.Success(createTestReport(userId, reportType, dateRange))
            }
        }

        override suspend fun saveReport(report: HealthReport): Result<Unit> {
            return if (shouldFailSave) {
                Result.Error(AppError.UnknownError("Save failed"))
            } else {
                Result.Success(Unit)
            }
        }

        override suspend fun getReport(reportId: String): Result<HealthReport?> = Result.Success(null)
        override suspend fun getUserReports(userId: String): Result<List<HealthReport>> = Result.Success(emptyList())
        override suspend fun generatePDF(report: HealthReport): Result<String> = Result.Success("pdf-url")
        override suspend fun shareReport(reportId: String, expirationDays: Int): Result<String> = Result.Success("share-url")
        override suspend fun revokeSharedAccess(reportId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun deleteReport(reportId: String): Result<Unit> = Result.Success(Unit)

        private fun createTestReport(userId: String, reportType: ReportType, dateRange: DateRange): HealthReport {
            return HealthReport(
                id = "test-report",
                userId = userId,
                generatedDate = Clock.System.now(),
                reportType = reportType,
                dateRange = dateRange,
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
    }
}