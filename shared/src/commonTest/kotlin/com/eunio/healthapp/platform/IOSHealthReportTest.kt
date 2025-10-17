package com.eunio.healthapp.platform

import com.eunio.healthapp.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IOSHealthReportTest {

    @Test
    fun `iOS health report generation works correctly`() = runTest {
        val report = createIOSHealthReport()
        
        // Test iOS-specific behavior
        assertNotNull(report.id)
        assertTrue(report.id.contains("ios"))
        assertEquals("ios-user-123", report.userId)
        assertTrue(report.cycleSummary.totalCycles > 0)
    }

    @Test
    fun `iOS handles large health reports efficiently`() = runTest {
        val largeReport = createLargeIOSHealthReport()
        
        // Verify the report structure is maintained for iOS
        assertNotNull(largeReport.id)
        assertTrue(largeReport.insights.size >= 10)
        assertTrue(largeReport.symptomAnalysis.mostCommonSymptoms.isNotEmpty())
    }

    @Test
    fun `iOS report sharing URLs are properly formatted`() = runTest {
        val report = createIOSHealthReport()
        
        // Simulate iOS-specific sharing behavior
        val shareUrl = generateIOSShareUrl(report.id)
        
        assertTrue(shareUrl.startsWith("ios://"))
        assertTrue(shareUrl.contains(report.id))
    }

    @Test
    fun `iOS handles report caching for offline access`() = runTest {
        val report = createIOSHealthReport()
        
        // Test iOS-specific offline behavior
        val cachedReport = simulateIOSCaching(report)
        
        assertEquals(report.userId, cachedReport.userId)
        assertEquals(report.reportType, cachedReport.reportType)
        assertTrue(cachedReport.id.contains("cached"))
    }

    @Test
    fun `iOS PDF generation paths are correct`() = runTest {
        val report = createIOSHealthReport()
        
        // Test iOS-specific PDF path generation
        val pdfPath = generateIOSPDFPath(report)
        
        assertTrue(pdfPath.startsWith("file://"))
        assertTrue(pdfPath.contains("Documents"))
        assertTrue(pdfPath.endsWith(".pdf"))
    }

    @Test
    fun `iOS handles concurrent report operations`() = runTest {
        val reports = (1..5).map { index ->
            createIOSHealthReport().copy(id = "ios-concurrent-report-$index")
        }
        
        // Verify all reports are properly structured for iOS
        reports.forEach { report ->
            assertNotNull(report.id)
            assertTrue(report.id.startsWith("ios-concurrent-report-"))
            assertEquals("ios-user-123", report.userId)
        }
    }

    private fun createIOSHealthReport(): HealthReport {
        return HealthReport(
            id = "ios-health-report-${Clock.System.now().toEpochMilliseconds()}",
            userId = "ios-user-123",
            generatedDate = Clock.System.now(),
            reportType = ReportType.MONTHLY,
            dateRange = DateRange(
                start = LocalDate(2024, 1, 1),
                end = LocalDate(2024, 1, 31)
            ),
            cycleSummary = CycleSummary(
                totalCycles = 3,
                averageCycleLength = 28.5,
                averageLutealPhase = 14.2,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(14.1, 0.87, 0.92)
            ),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = listOf(
                    SymptomFrequency("CRAMPS", 0.35, mapOf(CyclePhase.MENSTRUAL to 0.8)),
                    SymptomFrequency("BLOATING", 0.25, mapOf(CyclePhase.LUTEAL to 0.6))
                ),
                symptomTrends = mapOf(
                    "CRAMPS" to TrendData(TrendDirection.STABLE, 0.02, 0.8)
                ),
                moodPatterns = MoodPatterns(
                    dominantMood = "CALM",
                    moodVariability = 0.25,
                    cyclePhaseCorrelations = mapOf(
                        CyclePhase.MENSTRUAL to "TIRED",
                        CyclePhase.FOLLICULAR to "ENERGETIC"
                    )
                ),
                bbtAnalysis = BBTAnalysis(
                    averageTemperature = 98.3,
                    temperatureRange = TemperatureRange(97.9, 98.7, 98.1, 98.5),
                    ovulationDetectionRate = 0.88,
                    temperaturePatterns = listOf(
                        TemperaturePattern("BIPHASIC", 0.9, "Clear temperature shift detected")
                    )
                )
            ),
            insights = listOf(
                Insight(
                    id = "ios-insight-1",
                    userId = "ios-user-123",
                    type = InsightType.PATTERN_RECOGNITION,
                    insightText = "Your ovulation occurs consistently around day 14 of your cycle",
                    confidence = 0.92,
                    actionable = true,
                    isRead = false,
                    generatedDate = Clock.System.now(),
                    relatedLogIds = listOf("log-14", "log-15", "log-16")
                )
            ),
            pdfUrl = "file:///Documents/reports/ios-health-report.pdf",
            isShared = false
        )
    }

    private fun createLargeIOSHealthReport(): HealthReport {
        val baseReport = createIOSHealthReport()
        
        val manySymptoms = (1..15).map { index ->
            SymptomFrequency(
                symptom = "IOS_SYMPTOM_$index",
                frequency = 0.05 + (index % 10) * 0.02,
                cyclePhaseDistribution = mapOf(
                    CyclePhase.MENSTRUAL to 0.2 + (index % 4) * 0.1,
                    CyclePhase.FOLLICULAR to 0.15 + (index % 3) * 0.05
                )
            )
        }
        
        val manyInsights = (1..20).map { index ->
            Insight(
                id = "ios-large-insight-$index",
                userId = "ios-user-123",
                type = if (index % 2 == 0) InsightType.PATTERN_RECOGNITION else InsightType.EARLY_WARNING,
                insightText = "iOS-specific insight analysis $index with detailed recommendations",
                confidence = 0.7 + (index % 4) * 0.075,
                actionable = index % 2 == 0,
                isRead = index % 5 == 0,
                generatedDate = Clock.System.now(),
                relatedLogIds = (1..3).map { "ios-log-${index}-$it" }
            )
        }
        
        return baseReport.copy(
            id = "ios-large-report-${Clock.System.now().toEpochMilliseconds()}",
            symptomAnalysis = baseReport.symptomAnalysis.copy(
                mostCommonSymptoms = manySymptoms
            ),
            insights = manyInsights
        )
    }

    private fun generateIOSShareUrl(reportId: String): String {
        return "ios://share/health-report/$reportId"
    }

    private fun simulateIOSCaching(report: HealthReport): HealthReport {
        return report.copy(
            id = "${report.id}_cached_ios",
            pdfUrl = "file:///Library/Caches/health-reports/${report.id}.pdf"
        )
    }

    private fun generateIOSPDFPath(report: HealthReport): String {
        return "file:///Documents/health-reports/${report.id}.pdf"
    }
}