package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.OPKResult
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.abs

/**
 * Use case for predicting ovulation dates based on cycle history and fertility indicators.
 * Uses multiple data sources including cycle patterns, BBT, and OPK results.
 */
class PredictOvulationUseCase(
    private val cycleRepository: CycleRepository,
    private val logRepository: LogRepository
) {
    
    /**
     * Predicts ovulation date for the current cycle using historical data and fertility indicators.
     * 
     * @param userId The ID of the user
     * @return Result containing the predicted ovulation date or null if insufficient data
     */
    suspend fun execute(userId: String): Result<OvulationPrediction?> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.success(null) // No active cycle
        
        // Get cycle history for pattern analysis
        val historyResult = cycleRepository.getCycleHistory(userId, 6)
        if (historyResult.isError) {
            return Result.error(historyResult.errorOrNull()!!)
        }
        
        val cycleHistory = historyResult.getOrNull() ?: emptyList()
        
        // Calculate prediction based on available data
        val prediction = calculateOvulationPrediction(currentCycle, cycleHistory, userId)
        
        return Result.success(prediction)
    }
    
    /**
     * Predicts ovulation for a specific cycle using historical patterns.
     * 
     * @param userId The ID of the user
     * @param cycleStartDate The start date of the cycle to predict for
     * @return Result containing the predicted ovulation date
     */
    suspend fun predictForCycle(userId: String, cycleStartDate: LocalDate): Result<LocalDate?> {
        val historyResult = cycleRepository.getCycleHistory(userId, 6)
        if (historyResult.isError) {
            return Result.error(historyResult.errorOrNull()!!)
        }
        
        val cycleHistory = historyResult.getOrNull() ?: emptyList()
        
        if (cycleHistory.isEmpty()) {
            // No history available, use standard 14-day rule
            return Result.success(cycleStartDate.plus(DatePeriod(days = 13))) // Day 14 of cycle
        }
        
        // Calculate average days to ovulation from historical data
        val avgDaysToOvulation = calculateAverageDaysToOvulation(cycleHistory)
        val predictedDate = cycleStartDate.plus(DatePeriod(days = avgDaysToOvulation - 1)) // -1 because day 1 is start date
        
        return Result.success(predictedDate)
    }
    
    /**
     * Updates ovulation prediction based on current cycle fertility indicators.
     * 
     * @param userId The ID of the user
     * @return Result containing updated prediction with confidence level
     */
    suspend fun updatePredictionWithCurrentData(userId: String, referenceDate: LocalDate? = null): Result<OvulationPrediction?> {
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.success(null)
        
        // Get fertility logs for current cycle
        val endDate = referenceDate ?: kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val logsResult = logRepository.getFertilityLogsInRange(
            userId, 
            currentCycle.startDate, 
            endDate
        )
        if (logsResult.isError) {
            return Result.error(logsResult.errorOrNull()!!)
        }
        
        val fertilityLogs = logsResult.getOrNull() ?: emptyList()
        
        // Analyze current cycle data
        val bbtShift = detectBBTShift(fertilityLogs)
        val opkPeak = detectOPKPeak(fertilityLogs)
        
        // Get cycle history for base prediction
        val historyResult = cycleRepository.getCycleHistory(userId, 6)
        if (historyResult.isError) {
            return Result.error(historyResult.errorOrNull()!!)
        }
        
        val cycleHistory = historyResult.getOrNull() ?: emptyList()
        
        // Calculate base prediction without recursive call
        val basePrediction = calculateOvulationPrediction(currentCycle, cycleHistory, userId)
        
        // Refine prediction based on current indicators
        val refinedPrediction = refinePredictionWithIndicators(
            basePrediction, 
            bbtShift, 
            opkPeak, 
            currentCycle
        )
        
        return Result.success(refinedPrediction)
    }
    
    /**
     * Calculates ovulation prediction using multiple data sources.
     */
    private suspend fun calculateOvulationPrediction(
        currentCycle: Cycle, 
        cycleHistory: List<Cycle>, 
        userId: String
    ): OvulationPrediction? {
        
        if (cycleHistory.isEmpty()) {
            // No history, use standard prediction
            return OvulationPrediction(
                predictedDate = currentCycle.startDate.plus(DatePeriod(days = 13)), // Day 14
                confidence = PredictionConfidence.LOW,
                method = PredictionMethod.STANDARD_CALCULATION,
                fertilityWindow = calculateFertilityWindow(currentCycle.startDate.plus(DatePeriod(days = 13)))
            )
        }
        
        // Calculate based on historical patterns
        val avgDaysToOvulation = calculateAverageDaysToOvulation(cycleHistory)
        val predictedDate = currentCycle.startDate.plus(DatePeriod(days = avgDaysToOvulation - 1))
        
        // Determine confidence based on cycle regularity
        val confidence = calculateConfidenceLevel(cycleHistory)
        
        return OvulationPrediction(
            predictedDate = predictedDate,
            confidence = confidence,
            method = PredictionMethod.HISTORICAL_PATTERN,
            fertilityWindow = calculateFertilityWindow(predictedDate)
        )
    }
    
    /**
     * Calculates average days from cycle start to ovulation.
     */
    private fun calculateAverageDaysToOvulation(cycles: List<Cycle>): Int {
        val ovulationDays = cycles.mapNotNull { cycle ->
            cycle.confirmedOvulationDate?.let { ovulationDate ->
                (ovulationDate.toEpochDays() - cycle.startDate.toEpochDays()).toInt() + 1
            }
        }
        
        return if (ovulationDays.isNotEmpty()) {
            ovulationDays.average().toInt()
        } else {
            // Use luteal phase length if available (typically 12-16 days)
            val avgLutealLength = cycles.mapNotNull { it.lutealPhaseLength }.average().toInt()
            val avgCycleLength = cycles.mapNotNull { it.cycleLength }.average().toInt()
            if (avgLutealLength > 0 && avgCycleLength > 0) {
                avgCycleLength - avgLutealLength + 1
            } else {
                14 // Standard day 14 ovulation
            }
        }
    }
    
    /**
     * Calculates prediction confidence based on cycle regularity.
     */
    private fun calculateConfidenceLevel(cycles: List<Cycle>): PredictionConfidence {
        if (cycles.size < 3) return PredictionConfidence.LOW
        
        val cycleLengths = cycles.mapNotNull { it.cycleLength }
        if (cycleLengths.size < 3) return PredictionConfidence.LOW
        
        // Calculate standard deviation of cycle lengths
        val avgLength = cycleLengths.average()
        val variance = cycleLengths.map { (it - avgLength) * (it - avgLength) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        
        return when {
            stdDev <= 2.0 -> PredictionConfidence.HIGH
            stdDev <= 4.0 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }
    }
    
    /**
     * Detects BBT shift indicating ovulation has occurred.
     */
    private fun detectBBTShift(logs: List<DailyLog>): LocalDate? {
        val bbtLogs = logs.filter { it.bbt != null }.sortedBy { it.date }
        if (bbtLogs.size < 6) return null // Need at least 6 days of data
        
        // Look for sustained temperature rise (0.2°F or more for 3+ days)
        for (i in 3 until bbtLogs.size) {
            val currentTemp = bbtLogs[i].bbt!!
            val previousTemps = bbtLogs.subList(i - 3, i).map { it.bbt!! }
            val avgPreviousTemp = previousTemps.average()
            
            if (currentTemp - avgPreviousTemp >= 0.2) {
                // Check if temperature stays elevated for at least 2 more days
                val remainingLogs = bbtLogs.subList(i, bbtLogs.size)
                if (remainingLogs.size >= 3) { // Need at least 3 days of elevated temps
                    val elevatedTemps = remainingLogs.take(3)
                    if (elevatedTemps.all { it.bbt!! >= avgPreviousTemp + 0.1 }) {
                        return bbtLogs[i - 1].date // Ovulation likely occurred the day before temp rise
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Detects OPK peak indicating imminent ovulation.
     */
    private fun detectOPKPeak(logs: List<DailyLog>): LocalDate? {
        return logs
            .filter { it.opkResult == OPKResult.PEAK || it.opkResult == OPKResult.POSITIVE }
            .maxByOrNull { it.date }
            ?.date
    }
    
    /**
     * Refines prediction based on current cycle indicators.
     */
    private fun refinePredictionWithIndicators(
        basePrediction: OvulationPrediction?,
        bbtShift: LocalDate?,
        opkPeak: LocalDate?,
        currentCycle: Cycle
    ): OvulationPrediction? {
        
        val baseDate = basePrediction?.predictedDate ?: currentCycle.startDate.plus(DatePeriod(days = 13))
        
        // If BBT shift detected, ovulation likely occurred 1-2 days before
        if (bbtShift != null) {
            val ovulationDate = bbtShift.minus(DatePeriod(days = 1))
            return OvulationPrediction(
                predictedDate = ovulationDate,
                confidence = PredictionConfidence.HIGH,
                method = PredictionMethod.BBT_CONFIRMED,
                fertilityWindow = calculateFertilityWindow(ovulationDate)
            )
        }
        
        // If OPK peak detected, ovulation likely within 12-36 hours
        if (opkPeak != null) {
            val ovulationDate = opkPeak.plus(DatePeriod(days = 1))
            return OvulationPrediction(
                predictedDate = ovulationDate,
                confidence = PredictionConfidence.HIGH,
                method = PredictionMethod.OPK_CONFIRMED,
                fertilityWindow = calculateFertilityWindow(ovulationDate)
            )
        }
        
        return basePrediction
    }
    
    /**
     * Calculates the fertility window (5 days before to 1 day after ovulation).
     */
    private fun calculateFertilityWindow(ovulationDate: LocalDate): FertilityWindow {
        return FertilityWindow(
            startDate = ovulationDate.minus(DatePeriod(days = 5)),
            endDate = ovulationDate.plus(DatePeriod(days = 1)),
            peakDate = ovulationDate
        )
    }
}

/**
 * Data class representing an ovulation prediction with confidence and method.
 */
data class OvulationPrediction(
    val predictedDate: LocalDate,
    val confidence: PredictionConfidence,
    val method: PredictionMethod,
    val fertilityWindow: FertilityWindow
)

/**
 * Data class representing the fertility window around ovulation.
 */
data class FertilityWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val peakDate: LocalDate
)

/**
 * Enum representing the confidence level of ovulation prediction.
 */
enum class PredictionConfidence {
    LOW,      // Based on limited data or irregular cycles
    MEDIUM,   // Based on some historical data with moderate regularity
    HIGH      // Based on confirmed indicators or very regular cycles
}

/**
 * Enum representing the method used for ovulation prediction.
 */
enum class PredictionMethod {
    STANDARD_CALCULATION,  // Day 14 rule
    HISTORICAL_PATTERN,    // Based on cycle history
    BBT_CONFIRMED,         // Confirmed by BBT shift
    OPK_CONFIRMED         // Confirmed by OPK results
}