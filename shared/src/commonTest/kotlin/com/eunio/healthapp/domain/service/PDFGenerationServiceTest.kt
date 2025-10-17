package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PDFGenerationServiceTest {

    @Test
    fun `validateReportForPDF returns true for report with cycles`() {
        val service = TestPDFGenerationService()
        val report = createTestReport().copy(
            cycleSummary = CycleSummary(
                totalCycles = 3,
                averageCycleLength = 28.0,
                averageLutealPhase = 14.0,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(14.0, 0.85, 0.90)
            )
        )
        
        assertTrue(service.validateReportForPDF(report))
    }

    @Test
    fun `validateReportForPDF returns true for report with symptoms`() {
        val service = TestPDFGenerationService()
        val report = createTestReport().copy(
            cycleSummary = CycleSummary(0, 0.0, 0.0, CycleRegularity.IRREGULAR, OvulationPatterns(0.0, 0.0, 0.0)),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = listOf(
                    SymptomFrequency("CRAMPS", 0.75, emptyMap())
                ),
                symptomTrends = emptyMap(),
                moodPatterns = MoodPatterns("NEUTRAL", 0.3, emptyMap()),
                bbtAnalysis = null
            )
        )
        
        assertTrue(service.validateReportForPDF(report))
    }

    @Test
    fun `validateReportForPDF returns true for report with insights`() {
        val service = TestPDFGenerationService()
        val report = createTestReport().copy(
            cycleSummary = CycleSummary(0, 0.0, 0.0, CycleRegularity.IRREGULAR, OvulationPatterns(0.0, 0.0, 0.0)),
            symptomAnalysis = SymptomAnalysis(emptyList(), emptyMap(), MoodPatterns("NEUTRAL", 0.3, emptyMap()), null),
            insights = listOf(
                Insight(
                    id = "insight1",
                    userId = "user-123",
                    generatedDate = Clock.System.now(),
                    insightText = "Test insight",
                    type = InsightType.PATTERN_RECOGNITION,
                    isRead = false,
                    relatedLogIds = emptyList(),
                    confidence = 0.85,
                    actionable = false
                )
            )
        )
        
        assertTrue(service.validateReportForPDF(report))
    }

    @Test
    fun `validateReportForPDF returns false for empty report`() {
        val service = TestPDFGenerationService()
        val report = createTestReport().copy(
            cycleSummary = CycleSummary(0, 0.0, 0.0, CycleRegularity.IRREGULAR, OvulationPatterns(0.0, 0.0, 0.0)),
            symptomAnalysis = SymptomAnalysis(emptyList(), emptyMap(), MoodPatterns("NEUTRAL", 0.3, emptyMap()), null),
            insights = emptyList()
        )
        
        assertFalse(service.validateReportForPDF(report))
    }

    @Test
    fun `generateReportPDF returns URL for valid report`() = runTest {
        val service = TestPDFGenerationService()
        val report = createTestReport()
        
        val result = service.generateReportPDF(report)
        
        assertEquals("test-pdf-url", result)
    }

    @Test
    fun `generateCustomPDF returns URL for different templates`() = runTest {
        val service = TestPDFGenerationService()
        val report = createTestReport()
        
        val standardResult = service.generateCustomPDF(report, PDFTemplate.STANDARD)
        val detailedResult = service.generateCustomPDF(report, PDFTemplate.DETAILED)
        val summaryResult = service.generateCustomPDF(report, PDFTemplate.SUMMARY)
        val medicalResult = service.generateCustomPDF(report, PDFTemplate.MEDICAL_PROFESSIONAL)
        
        assertEquals("test-pdf-url-STANDARD", standardResult)
        assertEquals("test-pdf-url-DETAILED", detailedResult)
        assertEquals("test-pdf-url-SUMMARY", summaryResult)
        assertEquals("test-pdf-url-MEDICAL_PROFESSIONAL", medicalResult)
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

    private class TestPDFGenerationService : PDFGenerationService {
        override suspend fun generateReportPDF(report: HealthReport): String {
            return "test-pdf-url"
        }

        override suspend fun generateCustomPDF(report: HealthReport, templateType: PDFTemplate): String {
            return "test-pdf-url-${templateType.name}"
        }

        override fun validateReportForPDF(report: HealthReport): Boolean {
            return report.cycleSummary.totalCycles > 0 || 
                   report.symptomAnalysis.mostCommonSymptoms.isNotEmpty() ||
                   report.insights.isNotEmpty()
        }
    }
}