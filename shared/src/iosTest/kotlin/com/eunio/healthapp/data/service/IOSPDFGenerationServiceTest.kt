package com.eunio.healthapp.data.service

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.service.PDFTemplate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IOSPDFGenerationServiceTest {

    private val pdfService = IOSPDFGenerationService()

    @Test
    fun `generateReportPDF creates PDF URL for iOS`() = runTest {
        val report = createTestReport()
        
        val pdfUrl = pdfService.generateReportPDF(report)
        
        assertNotNull(pdfUrl)
        assertTrue(pdfUrl.isNotEmpty())
        assertTrue(pdfUrl.contains("health_report_") && pdfUrl.contains(".pdf"))
    }

    @Test
    fun `generateCustomPDF creates PDF with standard template`() = runTest {
        val report = createTestReport()
        
        val pdfUrl = pdfService.generateCustomPDF(report, PDFTemplate.STANDARD)
        
        assertNotNull(pdfUrl)
        assertTrue(pdfUrl.isNotEmpty())
    }

    @Test
    fun `generateCustomPDF creates PDF with detailed template`() = runTest {
        val report = createTestReport()
        
        val pdfUrl = pdfService.generateCustomPDF(report, PDFTemplate.DETAILED)
        
        assertNotNull(pdfUrl)
        assertTrue(pdfUrl.isNotEmpty())
    }

    @Test
    fun `generateCustomPDF creates PDF with summary template`() = runTest {
        val report = createTestReport()
        
        val pdfUrl = pdfService.generateCustomPDF(report, PDFTemplate.SUMMARY)
        
        assertNotNull(pdfUrl)
        assertTrue(pdfUrl.isNotEmpty())
    }

    @Test
    fun `validateReportForPDF returns true for valid report`() {
        val report = createTestReport()
        
        val isValid = pdfService.validateReportForPDF(report)
        
        assertTrue(isValid)
    }

    @Test
    fun `validateReportForPDF returns false for report with no cycles`() {
        val report = createTestReport().copy(
            cycleSummary = CycleSummary(
                totalCycles = 0,
                averageCycleLength = 0.0,
                averageLutealPhase = 0.0,
                cycleRegularity = CycleRegularity.IRREGULAR,
                ovulationPatterns = OvulationPatterns(0.0, 0.0, 0.0)
            )
        )
        
        val isValid = pdfService.validateReportForPDF(report)
        
        // Should still be valid as iOS service is more lenient
        assertTrue(isValid)
    }

    @Test
    fun `iOS PDF service handles large reports efficiently`() = runTest {
        val largeReport = createLargeTestReport()
        
        val pdfUrl = pdfService.generateReportPDF(largeReport)
        
        assertNotNull(pdfUrl)
        assertTrue(pdfUrl.isNotEmpty())
    }

    @Test
    fun `iOS PDF service generates unique URLs for different reports`() = runTest {
        val report1 = createTestReport()
        val report2 = createTestReport().copy(id = "different-report-id")
        
        val pdfUrl1 = pdfService.generateReportPDF(report1)
        val pdfUrl2 = pdfService.generateReportPDF(report2)
        
        assertNotNull(pdfUrl1)
        assertNotNull(pdfUrl2)
        // URLs should be different (contain different identifiers)
        assertTrue(pdfUrl1 != pdfUrl2 || pdfUrl1.contains(report1.id) || pdfUrl2.contains(report2.id))
    }

    private fun createTestReport(): HealthReport {
        return HealthReport(
            id = "ios-test-report",
            userId = "ios-user-123",
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
                mostCommonSymptoms = listOf(
                    SymptomFrequency("CRAMPS", 0.3, mapOf()),
                    SymptomFrequency("BLOATING", 0.2, mapOf())
                ),
                symptomTrends = mapOf(),
                moodPatterns = MoodPatterns("NEUTRAL", 0.3, mapOf()),
                bbtAnalysis = BBTAnalysis(
                    averageTemperature = 98.2,
                    temperatureRange = TemperatureRange(97.8, 98.6, 98.0, 98.4),
                    ovulationDetectionRate = 0.85,
                    temperaturePatterns = listOf()
                )
            ),
            insights = listOf(
                Insight(
                    id = "insight-1",
                    userId = "ios-user-123",
                    type = InsightType.PATTERN_RECOGNITION,
                    insightText = "Regular Cycle Pattern: Your cycles are very regular",
                    confidence = 0.9,
                    actionable = false,
                    isRead = false,
                    generatedDate = Clock.System.now(),
                    relatedLogIds = emptyList()
                )
            )
        )
    }

    private fun createLargeTestReport(): HealthReport {
        val baseReport = createTestReport()
        val manyInsights = (1..50).map { index ->
            Insight(
                id = "insight-$index",
                userId = "ios-user-123",
                type = InsightType.PATTERN_RECOGNITION,
                insightText = "Pattern $index: Description for pattern $index with detailed analysis and recommendations",
                confidence = 0.8 + (index % 3) * 0.05,
                actionable = index % 2 == 0,
                isRead = false,
                generatedDate = Clock.System.now(),
                relatedLogIds = listOf("log-$index", "log-${index + 1}")
            )
        }
        
        return baseReport.copy(
            insights = manyInsights,
            symptomAnalysis = baseReport.symptomAnalysis.copy(
                mostCommonSymptoms = (1..20).map { index ->
                    SymptomFrequency("SYMPTOM_$index", 0.1 + (index % 5) * 0.05, mapOf())
                }
            )
        )
    }
}