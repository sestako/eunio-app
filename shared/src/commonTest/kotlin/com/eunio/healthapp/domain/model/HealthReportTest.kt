package com.eunio.healthapp.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthReportTest {

    @Test
    fun `HealthReportValidation validates minimum cycles requirement`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 1,
            logCount = 15,
            bbtCount = 20,
            dateRangeDays = 90
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("At least 2 complete cycles") })
    }

    @Test
    fun `HealthReportValidation validates minimum logs requirement`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 3,
            logCount = 5,
            bbtCount = 20,
            dateRangeDays = 90
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("At least 10 daily logs") })
    }

    @Test
    fun `HealthReportValidation validates maximum date range`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 3,
            logCount = 15,
            bbtCount = 20,
            dateRangeDays = 400
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("cannot exceed 365 days") })
    }

    @Test
    fun `HealthReportValidation passes with sufficient data`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 3,
            logCount = 15,
            bbtCount = 20,
            dateRangeDays = 90
        )
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `HealthReportValidation generates warnings for limited BBT data`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 3,
            logCount = 15,
            bbtCount = 10,
            dateRangeDays = 90
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Limited BBT data") })
    }

    @Test
    fun `HealthReportValidation generates warnings for limited cycle data`() {
        val result = HealthReportValidation.validateReportRequest(
            cycleCount = 2,
            logCount = 15,
            bbtCount = 20,
            dateRangeDays = 90
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("More cycle data will improve") })
    }

    @Test
    fun `HealthReport can be created with valid data`() {
        val report = createTestHealthReport()
        
        assertEquals("test-report-1", report.id)
        assertEquals("user-123", report.userId)
        assertEquals(ReportType.MONTHLY, report.reportType)
        assertEquals(3, report.cycleSummary.totalCycles)
        assertFalse(report.isShared)
    }

    @Test
    fun `DateRange calculates correctly`() {
        val start = LocalDate(2024, 1, 1)
        val end = LocalDate(2024, 1, 31)
        val dateRange = DateRange(start, end)
        
        assertEquals(start, dateRange.start)
        assertEquals(end, dateRange.end)
    }

    @Test
    fun `CycleSummary contains expected data`() {
        val ovulationPatterns = OvulationPatterns(
            averageOvulationDay = 14.0,
            ovulationConfidenceRate = 0.85,
            fertilityWindowAccuracy = 0.90
        )
        
        val cycleSummary = CycleSummary(
            totalCycles = 5,
            averageCycleLength = 28.5,
            averageLutealPhase = 14.2,
            cycleRegularity = CycleRegularity.REGULAR,
            ovulationPatterns = ovulationPatterns
        )
        
        assertEquals(5, cycleSummary.totalCycles)
        assertEquals(28.5, cycleSummary.averageCycleLength)
        assertEquals(CycleRegularity.REGULAR, cycleSummary.cycleRegularity)
        assertEquals(14.0, cycleSummary.ovulationPatterns.averageOvulationDay)
    }

    @Test
    fun `SymptomAnalysis contains expected data`() {
        val symptomFrequency = SymptomFrequency(
            symptom = "CRAMPS",
            frequency = 0.75,
            cyclePhaseDistribution = mapOf(
                CyclePhase.MENSTRUAL to 0.9,
                CyclePhase.FOLLICULAR to 0.1
            )
        )
        
        val moodPatterns = MoodPatterns(
            dominantMood = "NEUTRAL",
            moodVariability = 0.3,
            cyclePhaseCorrelations = mapOf(
                CyclePhase.MENSTRUAL to "IRRITABLE",
                CyclePhase.OVULATORY to "HAPPY"
            )
        )
        
        val symptomAnalysis = SymptomAnalysis(
            mostCommonSymptoms = listOf(symptomFrequency),
            symptomTrends = mapOf(),
            moodPatterns = moodPatterns,
            bbtAnalysis = null
        )
        
        assertEquals(1, symptomAnalysis.mostCommonSymptoms.size)
        assertEquals("CRAMPS", symptomAnalysis.mostCommonSymptoms.first().symptom)
        assertEquals("NEUTRAL", symptomAnalysis.moodPatterns.dominantMood)
    }

    @Test
    fun `BBTAnalysis contains temperature data`() {
        val temperatureRange = TemperatureRange(
            min = 97.2,
            max = 99.1,
            follicularAverage = 97.8,
            lutealAverage = 98.4
        )
        
        val temperaturePattern = TemperaturePattern(
            pattern = "BIPHASIC",
            frequency = 0.85,
            description = "Clear temperature shift after ovulation"
        )
        
        val bbtAnalysis = BBTAnalysis(
            averageTemperature = 98.1,
            temperatureRange = temperatureRange,
            ovulationDetectionRate = 0.80,
            temperaturePatterns = listOf(temperaturePattern)
        )
        
        assertEquals(98.1, bbtAnalysis.averageTemperature)
        assertEquals(97.2, bbtAnalysis.temperatureRange.min)
        assertEquals(99.1, bbtAnalysis.temperatureRange.max)
        assertEquals(0.80, bbtAnalysis.ovulationDetectionRate)
        assertEquals(1, bbtAnalysis.temperaturePatterns.size)
    }

    private fun createTestHealthReport(): HealthReport {
        return HealthReport(
            id = "test-report-1",
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
}