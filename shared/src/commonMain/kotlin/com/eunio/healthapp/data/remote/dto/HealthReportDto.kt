package com.eunio.healthapp.data.remote.dto

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for HealthReport entity in Firestore
 */
@Serializable
data class HealthReportDto(
    val userId: String,
    val generatedDate: Long, // Instant as epoch milliseconds
    val reportType: String,
    val dateRange: DateRangeDto,
    val cycleSummary: CycleSummaryDto,
    val symptomAnalysis: SymptomAnalysisDto,
    val insights: List<InsightDto>,
    val pdfUrl: String? = null,
    val isShared: Boolean = false
) {
    companion object {
        /**
         * Converts domain HealthReport model to Firestore DTO
         */
        fun fromDomain(report: HealthReport): HealthReportDto {
            return HealthReportDto(
                userId = report.userId,
                generatedDate = report.generatedDate.toEpochMilliseconds(),
                reportType = report.reportType.name,
                dateRange = DateRangeDto.fromDomain(report.dateRange),
                cycleSummary = CycleSummaryDto.fromDomain(report.cycleSummary),
                symptomAnalysis = SymptomAnalysisDto.fromDomain(report.symptomAnalysis),
                insights = report.insights.map { InsightDto.fromDomain(it) },
                pdfUrl = report.pdfUrl,
                isShared = report.isShared
            )
        }
    }
    
    /**
     * Converts Firestore DTO to domain HealthReport model
     */
    fun toDomain(id: String): HealthReport {
        return HealthReport(
            id = id,
            userId = userId,
            generatedDate = Instant.fromEpochMilliseconds(generatedDate),
            reportType = ReportType.valueOf(reportType),
            dateRange = dateRange.toDomain(),
            cycleSummary = cycleSummary.toDomain(),
            symptomAnalysis = symptomAnalysis.toDomain(),
            insights = insights.mapIndexed { index, it -> it.toDomain("insight_$index", userId) },
            pdfUrl = pdfUrl,
            isShared = isShared
        )
    }
}

@Serializable
data class DateRangeDto(
    val start: Long, // LocalDate as epoch days
    val end: Long
) {
    companion object {
        fun fromDomain(dateRange: DateRange): DateRangeDto {
            return DateRangeDto(
                start = dateRange.start.toEpochDays().toLong(),
                end = dateRange.end.toEpochDays().toLong()
            )
        }
    }
    
    fun toDomain(): DateRange {
        return DateRange(
            start = LocalDate.fromEpochDays(start.toInt()),
            end = LocalDate.fromEpochDays(end.toInt())
        )
    }
}

@Serializable
data class CycleSummaryDto(
    val totalCycles: Int,
    val averageCycleLength: Double,
    val averageLutealPhase: Double,
    val cycleRegularity: String,
    val ovulationPatterns: OvulationPatternsDto
) {
    companion object {
        fun fromDomain(summary: CycleSummary): CycleSummaryDto {
            return CycleSummaryDto(
                totalCycles = summary.totalCycles,
                averageCycleLength = summary.averageCycleLength,
                averageLutealPhase = summary.averageLutealPhase,
                cycleRegularity = summary.cycleRegularity.name,
                ovulationPatterns = OvulationPatternsDto.fromDomain(summary.ovulationPatterns)
            )
        }
    }
    
    fun toDomain(): CycleSummary {
        return CycleSummary(
            totalCycles = totalCycles,
            averageCycleLength = averageCycleLength,
            averageLutealPhase = averageLutealPhase,
            cycleRegularity = CycleRegularity.valueOf(cycleRegularity),
            ovulationPatterns = ovulationPatterns.toDomain()
        )
    }
}

@Serializable
data class OvulationPatternsDto(
    val averageOvulationDay: Double,
    val ovulationConfidenceRate: Double,
    val fertilityWindowAccuracy: Double
) {
    companion object {
        fun fromDomain(patterns: OvulationPatterns): OvulationPatternsDto {
            return OvulationPatternsDto(
                averageOvulationDay = patterns.averageOvulationDay,
                ovulationConfidenceRate = patterns.ovulationConfidenceRate,
                fertilityWindowAccuracy = patterns.fertilityWindowAccuracy
            )
        }
    }
    
    fun toDomain(): OvulationPatterns {
        return OvulationPatterns(
            averageOvulationDay = averageOvulationDay,
            ovulationConfidenceRate = ovulationConfidenceRate,
            fertilityWindowAccuracy = fertilityWindowAccuracy
        )
    }
}

@Serializable
data class SymptomAnalysisDto(
    val mostCommonSymptoms: List<SymptomFrequencyDto>,
    val symptomTrends: Map<String, TrendDataDto>,
    val moodPatterns: MoodPatternsDto,
    val bbtAnalysis: BBTAnalysisDto?
) {
    companion object {
        fun fromDomain(analysis: SymptomAnalysis): SymptomAnalysisDto {
            return SymptomAnalysisDto(
                mostCommonSymptoms = analysis.mostCommonSymptoms.map { SymptomFrequencyDto.fromDomain(it) },
                symptomTrends = analysis.symptomTrends.mapValues { TrendDataDto.fromDomain(it.value) },
                moodPatterns = MoodPatternsDto.fromDomain(analysis.moodPatterns),
                bbtAnalysis = analysis.bbtAnalysis?.let { BBTAnalysisDto.fromDomain(it) }
            )
        }
    }
    
    fun toDomain(): SymptomAnalysis {
        return SymptomAnalysis(
            mostCommonSymptoms = mostCommonSymptoms.map { it.toDomain() },
            symptomTrends = symptomTrends.mapValues { it.value.toDomain() },
            moodPatterns = moodPatterns.toDomain(),
            bbtAnalysis = bbtAnalysis?.toDomain()
        )
    }
}

@Serializable
data class SymptomFrequencyDto(
    val symptom: String,
    val frequency: Double,
    val cyclePhaseDistribution: Map<String, Double>
) {
    companion object {
        fun fromDomain(frequency: SymptomFrequency): SymptomFrequencyDto {
            return SymptomFrequencyDto(
                symptom = frequency.symptom,
                frequency = frequency.frequency,
                cyclePhaseDistribution = frequency.cyclePhaseDistribution.mapKeys { it.key.name }
            )
        }
    }
    
    fun toDomain(): SymptomFrequency {
        return SymptomFrequency(
            symptom = symptom,
            frequency = frequency,
            cyclePhaseDistribution = cyclePhaseDistribution.mapKeys { CyclePhase.valueOf(it.key) }
        )
    }
}

@Serializable
data class TrendDataDto(
    val trend: String,
    val changePercentage: Double,
    val significance: Double
) {
    companion object {
        fun fromDomain(trend: TrendData): TrendDataDto {
            return TrendDataDto(
                trend = trend.trend.name,
                changePercentage = trend.changePercentage,
                significance = trend.significance
            )
        }
    }
    
    fun toDomain(): TrendData {
        return TrendData(
            trend = TrendDirection.valueOf(trend),
            changePercentage = changePercentage,
            significance = significance
        )
    }
}

@Serializable
data class MoodPatternsDto(
    val dominantMood: String,
    val moodVariability: Double,
    val cyclePhaseCorrelations: Map<String, String>
) {
    companion object {
        fun fromDomain(patterns: MoodPatterns): MoodPatternsDto {
            return MoodPatternsDto(
                dominantMood = patterns.dominantMood,
                moodVariability = patterns.moodVariability,
                cyclePhaseCorrelations = patterns.cyclePhaseCorrelations.mapKeys { it.key.name }
            )
        }
    }
    
    fun toDomain(): MoodPatterns {
        return MoodPatterns(
            dominantMood = dominantMood,
            moodVariability = moodVariability,
            cyclePhaseCorrelations = cyclePhaseCorrelations.mapKeys { CyclePhase.valueOf(it.key) }
        )
    }
}

@Serializable
data class BBTAnalysisDto(
    val averageTemperature: Double,
    val temperatureRange: TemperatureRangeDto,
    val ovulationDetectionRate: Double,
    val temperaturePatterns: List<TemperaturePatternDto>
) {
    companion object {
        fun fromDomain(analysis: BBTAnalysis): BBTAnalysisDto {
            return BBTAnalysisDto(
                averageTemperature = analysis.averageTemperature,
                temperatureRange = TemperatureRangeDto.fromDomain(analysis.temperatureRange),
                ovulationDetectionRate = analysis.ovulationDetectionRate,
                temperaturePatterns = analysis.temperaturePatterns.map { TemperaturePatternDto.fromDomain(it) }
            )
        }
    }
    
    fun toDomain(): BBTAnalysis {
        return BBTAnalysis(
            averageTemperature = averageTemperature,
            temperatureRange = temperatureRange.toDomain(),
            ovulationDetectionRate = ovulationDetectionRate,
            temperaturePatterns = temperaturePatterns.map { it.toDomain() }
        )
    }
}

@Serializable
data class TemperatureRangeDto(
    val min: Double,
    val max: Double,
    val follicularAverage: Double,
    val lutealAverage: Double
) {
    companion object {
        fun fromDomain(range: TemperatureRange): TemperatureRangeDto {
            return TemperatureRangeDto(
                min = range.min,
                max = range.max,
                follicularAverage = range.follicularAverage,
                lutealAverage = range.lutealAverage
            )
        }
    }
    
    fun toDomain(): TemperatureRange {
        return TemperatureRange(
            min = min,
            max = max,
            follicularAverage = follicularAverage,
            lutealAverage = lutealAverage
        )
    }
}

@Serializable
data class TemperaturePatternDto(
    val pattern: String,
    val frequency: Double,
    val description: String
) {
    companion object {
        fun fromDomain(pattern: TemperaturePattern): TemperaturePatternDto {
            return TemperaturePatternDto(
                pattern = pattern.pattern,
                frequency = pattern.frequency,
                description = pattern.description
            )
        }
    }
    
    fun toDomain(): TemperaturePattern {
        return TemperaturePattern(
            pattern = pattern,
            frequency = frequency,
            description = description
        )
    }
}