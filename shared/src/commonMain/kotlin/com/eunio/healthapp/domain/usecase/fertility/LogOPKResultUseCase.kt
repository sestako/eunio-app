package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod

/**
 * Use case for logging Ovulation Predictor Kit (OPK) results with result tracking and analysis.
 * Handles OPK result validation, surge detection, and ovulation prediction timing.
 */
class LogOPKResultUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Logs OPK result for a specific date.
     * 
     * @param userId The ID of the user
     * @param date The date of the OPK test
     * @param result The OPK test result
     * @return Result indicating success or failure of the logging operation
     */
    suspend operator fun invoke(
        userId: String,
        date: LocalDate,
        result: OPKResult
    ): Result<Unit> {
        // Validate the date
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (date > today) {
            return Result.error(
                AppError.ValidationError("Cannot log OPK result for future dates")
            )
        }
        
        // Get existing daily log or create new one
        val existingLogResult = logRepository.getDailyLog(userId, date)
        if (existingLogResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve existing log: ${existingLogResult.error.message}")
            )
        }
        
        val existingLog = existingLogResult.getOrNull()
        val updatedLog = if (existingLog != null) {
            // Update existing log with OPK result
            existingLog.copy(
                opkResult = result,
                updatedAt = Clock.System.now()
            )
        } else {
            // Create new log with OPK result
            DailyLog(
                id = generateLogId(userId, date),
                userId = userId,
                date = date,
                opkResult = result,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // Save the updated log
        return when (val saveResult = logRepository.saveDailyLog(updatedLog)) {
            is Result.Success -> Result.success(Unit)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to save OPK result: ${saveResult.error.message}")
            )
        }
    }
    
    /**
     * Analyzes OPK results over a date range to detect LH surge patterns.
     * 
     * @param userId The ID of the user
     * @param startDate Start of the analysis period
     * @param endDate End of the analysis period
     * @return Result containing OPK pattern analysis
     */
    suspend fun analyzeOPKPattern(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<OPKPatternAnalysis> {
        val logsResult = logRepository.getFertilityLogsInRange(userId, startDate, endDate)
        if (logsResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve fertility logs: ${logsResult.error.message}")
            )
        }
        
        val opkLogs = logsResult.getOrNull()
            ?.filter { it.opkResult != null }
            ?.sortedBy { it.date } ?: emptyList()
        
        if (opkLogs.isEmpty()) {
            return Result.success(
                OPKPatternAnalysis(
                    totalTests = 0,
                    surgeDetected = false,
                    surgeDates = emptyList(),
                    predictedOvulationDates = emptyList(),
                    recommendations = listOf("Start OPK testing around day 10-12 of your cycle for best results")
                )
            )
        }
        
        val surgeDates = detectLHSurge(opkLogs)
        val predictedOvulationDates = surgeDates.map { date -> date.plus(DatePeriod(days = 1)) } // Ovulation typically occurs 12-36 hours after LH surge
        val recommendations = generateOPKRecommendations(opkLogs, surgeDates)
        
        return Result.success(
            OPKPatternAnalysis(
                totalTests = opkLogs.size,
                surgeDetected = surgeDates.isNotEmpty(),
                surgeDates = surgeDates,
                predictedOvulationDates = predictedOvulationDates,
                recommendations = recommendations
            )
        )
    }
    
    /**
     * Detects LH surge from OPK results sequence.
     * Groups consecutive positive/peak results as a single surge and prioritizes peak dates.
     * 
     * @param opkLogs List of daily logs with OPK results, sorted by date
     * @return List of dates when LH surge was detected
     */
    private fun detectLHSurge(opkLogs: List<DailyLog>): List<LocalDate> {
        val surgeDates = mutableListOf<LocalDate>()
        var currentSurgeStart: LocalDate? = null
        var peakInCurrentSurge: LocalDate? = null
        
        for (i in opkLogs.indices) {
            val log = opkLogs[i]
            val isPositiveOrPeak = log.opkResult == OPKResult.POSITIVE || log.opkResult == OPKResult.PEAK
            val isPeak = log.opkResult == OPKResult.PEAK
            
            if (isPositiveOrPeak) {
                if (currentSurgeStart == null) {
                    // Start of a new surge
                    currentSurgeStart = log.date
                }
                if (isPeak) {
                    peakInCurrentSurge = log.date
                }
            } else {
                // End of current surge (if any)
                if (currentSurgeStart != null) {
                    // Add the peak date if we found one, otherwise the first positive date (start of surge)
                    val surgeDate = peakInCurrentSurge ?: currentSurgeStart
                    surgeDates.add(surgeDate)
                    currentSurgeStart = null
                    peakInCurrentSurge = null
                }
            }
        }
        
        // Handle case where surge continues to the end
        if (currentSurgeStart != null) {
            val surgeDate = peakInCurrentSurge ?: currentSurgeStart
            surgeDates.add(surgeDate)
        }
        
        return surgeDates
    }
    
    /**
     * Generates personalized recommendations based on OPK patterns.
     */
    private fun generateOPKRecommendations(
        opkLogs: List<DailyLog>,
        surgeDates: List<LocalDate>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when {
            surgeDates.isEmpty() -> {
                recommendations.add("No LH surge detected in this period")
                recommendations.add("Continue testing daily during your fertile window")
                recommendations.add("Test at the same time each day for consistency")
                if (opkLogs.size < 5) {
                    recommendations.add("Consider testing for a longer period (5-7 days) around expected ovulation")
                }
            }
            surgeDates.size == 1 -> {
                recommendations.add("LH surge detected - ovulation likely within 12-36 hours")
                recommendations.add("This is your most fertile time for conception")
                recommendations.add("Continue tracking to confirm ovulation patterns")
            }
            surgeDates.size > 1 -> {
                val daysBetweenSurges = surgeDates.zipWithNext { first, second ->
                    second.toEpochDays() - first.toEpochDays()
                }
                
                if (daysBetweenSurges.any { it <= 3 }) {
                    recommendations.add("Multiple LH surges detected close together")
                    recommendations.add("This pattern may indicate PCOS or other hormonal conditions")
                    recommendations.add("Consider consulting healthcare provider for evaluation")
                } else {
                    recommendations.add("Multiple LH surges detected in this cycle")
                    recommendations.add("This can be normal but may indicate irregular ovulation")
                }
            }
        }
        
        // General recommendations
        recommendations.add("Combine OPK results with BBT and cervical mucus tracking for best accuracy")
        
        return recommendations
    }
    
    /**
     * Provides educational information about OPK results.
     * 
     * @param result The OPK result
     * @return Educational description of the result and its significance
     */
    fun getResultDescription(result: OPKResult): String {
        return when (result) {
            OPKResult.NEGATIVE -> "No LH surge detected. Continue testing daily during your fertile window."
            OPKResult.POSITIVE -> "LH surge detected! Ovulation is likely to occur within 12-36 hours. This is your peak fertile time."
            OPKResult.PEAK -> "Peak LH surge detected! This is the strongest positive result, indicating ovulation is imminent (within 12-24 hours)."
        }
    }
    
    /**
     * Calculates optimal testing window based on cycle length.
     * 
     * @param cycleLength Average cycle length in days
     * @return Pair of (start day, end day) for optimal OPK testing
     */
    fun calculateOptimalTestingWindow(cycleLength: Int): Pair<Int, Int> {
        // General rule: start testing 17 days before expected next period
        val startDay = maxOf(cycleLength - 17, 8) // Don't start before day 8
        val endDay = minOf(cycleLength - 10, cycleLength - 3) // Don't test too close to period
        
        return Pair(startDay, endDay)
    }
    
    /**
     * Generates a unique log ID for the user and date.
     */
    private fun generateLogId(userId: String, date: LocalDate): String {
        return "${userId}_${date}"
    }
}

/**
 * Analysis result for OPK patterns.
 */
data class OPKPatternAnalysis(
    val totalTests: Int,
    val surgeDetected: Boolean,
    val surgeDates: List<LocalDate>,
    val predictedOvulationDates: List<LocalDate>,
    val recommendations: List<String>
)