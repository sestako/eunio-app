package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.CervicalMucus
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.math.abs

/**
 * Use case for confirming ovulation by combining multiple fertility indicators.
 * Analyzes BBT patterns, cervical mucus, and OPK results to determine ovulation confirmation.
 */
class ConfirmOvulationUseCase(
    private val logRepository: LogRepository,
    private val cycleRepository: CycleRepository
) {
    
    /**
     * Confirms ovulation for a specific cycle by analyzing multiple fertility indicators.
     * 
     * @param userId The ID of the user
     * @param cycleId The ID of the cycle to analyze
     * @param analysisStartDate Start date for fertility indicator analysis
     * @param analysisEndDate End date for fertility indicator analysis
     * @return Result containing ovulation confirmation analysis
     */
    suspend operator fun invoke(
        userId: String,
        cycleId: String,
        analysisStartDate: LocalDate,
        analysisEndDate: LocalDate
    ): Result<OvulationConfirmation> {
        // Get fertility logs for the analysis period
        val logsResult = logRepository.getFertilityLogsInRange(userId, analysisStartDate, analysisEndDate)
        if (logsResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve fertility logs: ${logsResult.error.message}")
            )
        }
        
        val logs = logsResult.getOrNull()?.sortedBy { it.date } ?: emptyList()
        
        if (logs.isEmpty()) {
            return Result.success(
                OvulationConfirmation(
                    isConfirmed = false,
                    confidence = 0.0,
                    ovulationDate = null,
                    supportingIndicators = emptyList(),
                    recommendations = listOf("Insufficient data for ovulation confirmation. Continue tracking BBT, cervical mucus, and OPK results.")
                )
            )
        }
        
        // Analyze each fertility indicator
        val bbtAnalysis = analyzeBBTPattern(logs)
        val mucusAnalysis = analyzeCervicalMucusPattern(logs)
        val opkAnalysis = analyzeOPKPattern(logs)
        
        // Combine indicators to determine ovulation
        val ovulationConfirmation = combineIndicators(bbtAnalysis, mucusAnalysis, opkAnalysis)
        
        // Update cycle with confirmed ovulation if confidence is high enough
        if (ovulationConfirmation.isConfirmed && ovulationConfirmation.ovulationDate != null) {
            val updateResult = cycleRepository.confirmOvulation(cycleId, ovulationConfirmation.ovulationDate)
            if (updateResult is Result.Error) {
                return Result.error(
                    AppError.DataSyncError("Failed to update cycle with ovulation confirmation: ${updateResult.error.message}")
                )
            }
        }
        
        return Result.success(ovulationConfirmation)
    }
    
    /**
     * Analyzes BBT pattern to detect ovulation.
     * Looks for sustained temperature rise indicating ovulation.
     */
    private fun analyzeBBTPattern(logs: List<DailyLog>): IndicatorAnalysis {
        val bbtLogs = logs.filter { it.bbt != null }.sortedBy { it.date }
        
        if (bbtLogs.size < 6) {
            return IndicatorAnalysis(
                indicatorType = "BBT",
                ovulationDate = null,
                confidence = 0.0,
                evidence = "Insufficient BBT data (need at least 6 days)"
            )
        }
        
        // Look for temperature shift pattern
        for (i in 3 until bbtLogs.size - 2) {
            val preOvulationTemps = bbtLogs.subList(i - 3, i).mapNotNull { it.bbt }
            val postOvulationTemps = bbtLogs.subList(i, i + 3).mapNotNull { it.bbt }
            
            if (preOvulationTemps.size >= 3 && postOvulationTemps.size >= 3) {
                val preAvg = preOvulationTemps.average()
                val postAvg = postOvulationTemps.average()
                val tempRise = postAvg - preAvg
                
                // Look for sustained rise of at least 0.2°C
                if (tempRise >= 0.2 && postOvulationTemps.all { it >= preAvg + 0.1 }) {
                    val confidence = minOf(1.0, tempRise / 0.4) // Higher rise = higher confidence
                    return IndicatorAnalysis(
                        indicatorType = "BBT",
                        ovulationDate = bbtLogs[i - 1].date, // Ovulation likely day before temp rise
                        confidence = confidence,
                        evidence = "Sustained temperature rise of ${(tempRise * 100).toInt() / 100.0}°C detected"
                    )
                }
            }
        }
        
        return IndicatorAnalysis(
            indicatorType = "BBT",
            ovulationDate = null,
            confidence = 0.0,
            evidence = "No clear temperature shift pattern detected"
        )
    }
    
    /**
     * Analyzes cervical mucus pattern to detect ovulation.
     * Looks for peak fertile mucus followed by drying pattern.
     */
    private fun analyzeCervicalMucusPattern(logs: List<DailyLog>): IndicatorAnalysis {
        val mucusLogs = logs.filter { it.cervicalMucus != null }.sortedBy { it.date }
        
        if (mucusLogs.size < 4) {
            return IndicatorAnalysis(
                indicatorType = "Cervical Mucus",
                ovulationDate = null,
                confidence = 0.0,
                evidence = "Insufficient cervical mucus data"
            )
        }
        
        // Look for peak mucus followed by drying pattern
        var peakMucusDate: LocalDate? = null
        var peakFound = false
        
        for (i in mucusLogs.indices) {
            val mucus = mucusLogs[i].cervicalMucus!!
            
            if (mucus == CervicalMucus.EGG_WHITE) {
                peakMucusDate = mucusLogs[i].date
                peakFound = true
            } else if (peakFound && (mucus == CervicalMucus.DRY || mucus == CervicalMucus.STICKY)) {
                // Found drying pattern after peak - ovulation likely occurred
                val confidence = if (peakMucusDate != null) 0.7 else 0.4
                return IndicatorAnalysis(
                    indicatorType = "Cervical Mucus",
                    ovulationDate = peakMucusDate ?: mucusLogs[maxOf(0, i - 1)].date,
                    confidence = confidence,
                    evidence = "Peak fertile mucus followed by drying pattern"
                )
            }
        }
        
        if (peakMucusDate != null) {
            return IndicatorAnalysis(
                indicatorType = "Cervical Mucus",
                ovulationDate = peakMucusDate,
                confidence = 0.5,
                evidence = "Peak fertile mucus detected but no clear drying pattern"
            )
        }
        
        return IndicatorAnalysis(
            indicatorType = "Cervical Mucus",
            ovulationDate = null,
            confidence = 0.0,
            evidence = "No peak fertile mucus pattern detected"
        )
    }
    
    /**
     * Analyzes OPK pattern to detect ovulation.
     * Looks for LH surge indicating imminent ovulation.
     */
    private fun analyzeOPKPattern(logs: List<DailyLog>): IndicatorAnalysis {
        val opkLogs = logs.filter { it.opkResult != null }.sortedBy { it.date }
        
        if (opkLogs.isEmpty()) {
            return IndicatorAnalysis(
                indicatorType = "OPK",
                ovulationDate = null,
                confidence = 0.0,
                evidence = "No OPK data available"
            )
        }
        
        // Look for LH surge
        for (i in opkLogs.indices) {
            val result = opkLogs[i].opkResult!!
            
            when (result) {
                OPKResult.PEAK -> {
                    return IndicatorAnalysis(
                        indicatorType = "OPK",
                        ovulationDate = opkLogs[i].date.plus(DatePeriod(days = 1)), // Ovulation typically 12-36 hours after peak
                        confidence = 0.8,
                        evidence = "Peak LH surge detected"
                    )
                }
                OPKResult.POSITIVE -> {
                    val confidence = if (i < opkLogs.size - 1 && 
                        opkLogs[i + 1].opkResult == OPKResult.NEGATIVE) 0.6 else 0.4
                    return IndicatorAnalysis(
                        indicatorType = "OPK",
                        ovulationDate = opkLogs[i].date.plus(DatePeriod(days = 1)),
                        confidence = confidence,
                        evidence = "Positive LH surge detected"
                    )
                }
                OPKResult.NEGATIVE -> {
                    // Continue looking
                }
            }
        }
        
        return IndicatorAnalysis(
            indicatorType = "OPK",
            ovulationDate = null,
            confidence = 0.0,
            evidence = "No LH surge detected in OPK results"
        )
    }
    
    /**
     * Combines multiple fertility indicators to determine ovulation confirmation.
     */
    private fun combineIndicators(
        bbtAnalysis: IndicatorAnalysis,
        mucusAnalysis: IndicatorAnalysis,
        opkAnalysis: IndicatorAnalysis
    ): OvulationConfirmation {
        val analyses = listOf(bbtAnalysis, mucusAnalysis, opkAnalysis)
        val validAnalyses = analyses.filter { it.ovulationDate != null && it.confidence > 0.3 }
        
        if (validAnalyses.isEmpty()) {
            return OvulationConfirmation(
                isConfirmed = false,
                confidence = 0.0,
                ovulationDate = null,
                supportingIndicators = analyses.map { "${it.indicatorType}: ${it.evidence}" },
                recommendations = listOf(
                    "Continue tracking all fertility indicators for better ovulation detection",
                    "Ensure consistent daily tracking of BBT, cervical mucus, and OPK results",
                    "Consider consulting healthcare provider if ovulation is not detected after 3 cycles"
                )
            )
        }
        
        // Find consensus ovulation date
        val ovulationDate = findConsensusOvulationDate(validAnalyses)
        
        // Calculate combined confidence
        val combinedConfidence = calculateCombinedConfidence(validAnalyses, ovulationDate)
        
        // Generate supporting evidence
        val supportingIndicators = validAnalyses.map { "${it.indicatorType}: ${it.evidence}" }
        
        // Generate recommendations
        val recommendations = generateOvulationRecommendations(combinedConfidence, validAnalyses.size)
        
        return OvulationConfirmation(
            isConfirmed = combinedConfidence >= 0.6,
            confidence = combinedConfidence,
            ovulationDate = ovulationDate,
            supportingIndicators = supportingIndicators,
            recommendations = recommendations
        )
    }
    
    /**
     * Finds consensus ovulation date from multiple indicators.
     */
    private fun findConsensusOvulationDate(analyses: List<IndicatorAnalysis>): LocalDate? {
        if (analyses.isEmpty()) return null
        
        val dates = analyses.mapNotNull { it.ovulationDate }
        if (dates.isEmpty()) return null
        
        // Find the date with the highest weighted confidence
        val weightedDates = dates.groupBy { it }.mapValues { (date, occurrences) ->
            analyses.filter { it.ovulationDate == date }.sumOf { it.confidence }
        }
        
        return weightedDates.maxByOrNull { it.value }?.key
    }
    
    /**
     * Calculates combined confidence from multiple indicators.
     */
    private fun calculateCombinedConfidence(analyses: List<IndicatorAnalysis>, consensusDate: LocalDate?): Double {
        if (consensusDate == null) return 0.0
        
        val relevantAnalyses = analyses.filter { analysis ->
            analysis.ovulationDate?.let { date ->
                abs(date.toEpochDays() - consensusDate.toEpochDays()) <= 2 // Within 2 days
            } ?: false
        }
        
        if (relevantAnalyses.isEmpty()) return 0.0
        
        // Weighted average with bonus for multiple indicators
        val baseConfidence = relevantAnalyses.map { it.confidence }.average()
        val multiIndicatorBonus = if (relevantAnalyses.size > 1) 0.1 * (relevantAnalyses.size - 1) else 0.0
        
        return minOf(1.0, baseConfidence + multiIndicatorBonus)
    }
    
    /**
     * Generates recommendations based on ovulation confirmation results.
     */
    private fun generateOvulationRecommendations(confidence: Double, indicatorCount: Int): List<String> {
        val recommendations = mutableListOf<String>()
        
        when {
            confidence >= 0.8 -> {
                recommendations.add("Strong ovulation confirmation with multiple supporting indicators")
                recommendations.add("Continue current tracking methods for consistent results")
            }
            confidence >= 0.6 -> {
                recommendations.add("Moderate ovulation confirmation detected")
                recommendations.add("Consider tracking additional indicators for higher confidence")
            }
            confidence >= 0.3 -> {
                recommendations.add("Weak ovulation signals detected")
                recommendations.add("Increase tracking consistency and duration")
                recommendations.add("Consider consulting healthcare provider if patterns remain unclear")
            }
            else -> {
                recommendations.add("No clear ovulation confirmation")
                recommendations.add("Continue daily tracking of all fertility indicators")
                recommendations.add("Ensure proper timing and technique for all measurements")
            }
        }
        
        if (indicatorCount < 2) {
            recommendations.add("Track multiple fertility indicators (BBT, cervical mucus, OPK) for better accuracy")
        }
        
        return recommendations
    }
}

/**
 * Analysis result for a single fertility indicator.
 */
data class IndicatorAnalysis(
    val indicatorType: String,
    val ovulationDate: LocalDate?,
    val confidence: Double,
    val evidence: String
)

/**
 * Comprehensive ovulation confirmation result.
 */
data class OvulationConfirmation(
    val isConfirmed: Boolean,
    val confidence: Double,
    val ovulationDate: LocalDate?,
    val supportingIndicators: List<String>,
    val recommendations: List<String>
)