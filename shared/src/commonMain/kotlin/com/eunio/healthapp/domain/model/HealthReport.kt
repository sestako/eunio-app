package com.eunio.healthapp.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class HealthReport(
    val id: String,
    val userId: String,
    val generatedDate: Instant,
    val reportType: ReportType,
    val dateRange: DateRange,
    val cycleSummary: CycleSummary,
    val symptomAnalysis: SymptomAnalysis,
    val insights: List<Insight>,
    val pdfUrl: String? = null,
    val isShared: Boolean = false
)

@Serializable
data class DateRange(
    val start: LocalDate,
    val end: LocalDate
)

@Serializable
data class CycleSummary(
    val totalCycles: Int,
    val averageCycleLength: Double,
    val averageLutealPhase: Double,
    val cycleRegularity: CycleRegularity,
    val ovulationPatterns: OvulationPatterns
)

@Serializable
data class OvulationPatterns(
    val averageOvulationDay: Double,
    val ovulationConfidenceRate: Double,
    val fertilityWindowAccuracy: Double
)

@Serializable
data class SymptomAnalysis(
    val mostCommonSymptoms: List<SymptomFrequency>,
    val symptomTrends: Map<String, TrendData>,
    val moodPatterns: MoodPatterns,
    val bbtAnalysis: BBTAnalysis?
)

@Serializable
data class SymptomFrequency(
    val symptom: String,
    val frequency: Double,
    val cyclePhaseDistribution: Map<CyclePhase, Double>
)

@Serializable
data class TrendData(
    val trend: TrendDirection,
    val changePercentage: Double,
    val significance: Double
)

@Serializable
data class MoodPatterns(
    val dominantMood: String,
    val moodVariability: Double,
    val cyclePhaseCorrelations: Map<CyclePhase, String>
)

@Serializable
data class BBTAnalysis(
    val averageTemperature: Double,
    val temperatureRange: TemperatureRange,
    val ovulationDetectionRate: Double,
    val temperaturePatterns: List<TemperaturePattern>
)

@Serializable
data class TemperatureRange(
    val min: Double,
    val max: Double,
    val follicularAverage: Double,
    val lutealAverage: Double
)

@Serializable
data class TemperaturePattern(
    val pattern: String,
    val frequency: Double,
    val description: String
)

@Serializable
enum class ReportType {
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

@Serializable
enum class CycleRegularity {
    VERY_REGULAR,
    REGULAR,
    SOMEWHAT_IRREGULAR,
    IRREGULAR,
    VERY_IRREGULAR
}

@Serializable
enum class CyclePhase {
    MENSTRUAL,
    FOLLICULAR,
    OVULATORY,
    LUTEAL
}

@Serializable
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    FLUCTUATING
}

/**
 * Validation rules for health report generation
 */
object HealthReportValidation {
    const val MIN_CYCLES_FOR_MEANINGFUL_REPORT = 2
    const val MIN_LOGS_FOR_SYMPTOM_ANALYSIS = 10
    const val MIN_BBT_READINGS_FOR_ANALYSIS = 15
    const val MAX_REPORT_DATE_RANGE_DAYS = 365
    
    fun validateReportRequest(
        cycleCount: Int,
        logCount: Int,
        bbtCount: Int,
        dateRangeDays: Int
    ): ReportValidationResult {
        val errors = mutableListOf<String>()
        
        if (cycleCount < MIN_CYCLES_FOR_MEANINGFUL_REPORT) {
            errors.add("At least $MIN_CYCLES_FOR_MEANINGFUL_REPORT complete cycles are required for a meaningful report")
        }
        
        if (logCount < MIN_LOGS_FOR_SYMPTOM_ANALYSIS) {
            errors.add("At least $MIN_LOGS_FOR_SYMPTOM_ANALYSIS daily logs are required for symptom analysis")
        }
        
        if (dateRangeDays > MAX_REPORT_DATE_RANGE_DAYS) {
            errors.add("Report date range cannot exceed $MAX_REPORT_DATE_RANGE_DAYS days")
        }
        
        return ReportValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = generateWarnings(cycleCount, logCount, bbtCount)
        )
    }
    
    private fun generateWarnings(cycleCount: Int, logCount: Int, bbtCount: Int): List<String> {
        val warnings = mutableListOf<String>()
        
        if (bbtCount < MIN_BBT_READINGS_FOR_ANALYSIS) {
            warnings.add("Limited BBT data available. At least $MIN_BBT_READINGS_FOR_ANALYSIS readings recommended for temperature analysis")
        }
        
        if (cycleCount < 3) {
            warnings.add("More cycle data will improve report accuracy and insights")
        }
        
        return warnings
    }
}

@Serializable
data class ReportValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)