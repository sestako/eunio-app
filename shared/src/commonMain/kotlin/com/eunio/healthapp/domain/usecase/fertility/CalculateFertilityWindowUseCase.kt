package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod

/**
 * Use case for calculating fertility windows based on cycle history and fertility indicators.
 * Provides personalized fertile window predictions and conception probability estimates.
 */
class CalculateFertilityWindowUseCase(
    private val cycleRepository: CycleRepository,
    private val logRepository: LogRepository
) {
    
    /**
     * Calculates the fertility window for the current cycle.
     * 
     * @param userId The ID of the user
     * @return Result containing fertility window calculation
     */
    suspend operator fun invoke(userId: String): Result<FertilityWindow> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve current cycle: ${currentCycleResult.error.message}")
            )
        }
        
        val currentCycle = currentCycleResult.getOrNull()
        if (currentCycle == null) {
            return Result.error(
                AppError.ValidationError("No active cycle found. Please start tracking your cycle first.")
            )
        }
        
        // Get cycle history for pattern analysis
        val historyResult = cycleRepository.getCycleHistory(userId, 6)
        if (historyResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve cycle history: ${historyResult.error.message}")
            )
        }
        
        val cycleHistory = historyResult.getOrNull() ?: emptyList()
        
        // Calculate fertility window
        val fertilityWindow = calculateFertilityWindow(currentCycle, cycleHistory)
        
        return Result.success(fertilityWindow)
    }
    
    /**
     * Calculates fertility window for a specific future cycle.
     * 
     * @param userId The ID of the user
     * @param cycleStartDate The start date of the cycle to calculate for
     * @return Result containing fertility window calculation
     */
    suspend fun calculateForFutureCycle(
        userId: String,
        cycleStartDate: LocalDate
    ): Result<FertilityWindow> {
        // Get cycle history for pattern analysis
        val historyResult = cycleRepository.getCycleHistory(userId, 6)
        if (historyResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve cycle history: ${historyResult.error.message}")
            )
        }
        
        val cycleHistory = historyResult.getOrNull() ?: emptyList()
        
        if (cycleHistory.isEmpty()) {
            return Result.error(
                AppError.ValidationError("Insufficient cycle history for fertility window calculation")
            )
        }
        
        // Create a hypothetical cycle for calculation
        val averageCycleLength = cycleHistory.mapNotNull { cycle -> cycle.cycleLength }.average().toInt()
        val hypotheticalCycle = Cycle(
            id = "hypothetical",
            userId = userId,
            startDate = cycleStartDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = averageCycleLength,
            lutealPhaseLength = null
        )
        
        val fertilityWindow = calculateFertilityWindow(hypotheticalCycle, cycleHistory)
        
        return Result.success(fertilityWindow)
    }
    
    /**
     * Calculates fertility window based on current cycle and historical data.
     */
    private fun calculateFertilityWindow(
        currentCycle: Cycle,
        cycleHistory: List<Cycle>
    ): FertilityWindow {
        val cycleStartDate = currentCycle.startDate
        
        // Calculate average cycle length and ovulation timing
        val averageCycleLength = calculateAverageCycleLength(cycleHistory)
        val averageOvulationDay = calculateAverageOvulationDay(cycleHistory, averageCycleLength)
        
        // Calculate predicted ovulation date
        val predictedOvulationDate = cycleStartDate.plus(DatePeriod(days = averageOvulationDay - 1))
        
        // Calculate fertility window (typically 6 days: 5 days before ovulation + ovulation day)
        val fertilityWindowStart = predictedOvulationDate.plus(DatePeriod(days = -5))
        val fertilityWindowEnd = predictedOvulationDate
        
        // Calculate daily conception probabilities
        val dailyProbabilities = calculateDailyConceptionProbabilities(
            fertilityWindowStart,
            fertilityWindowEnd,
            predictedOvulationDate
        )
        
        // Calculate confidence based on cycle regularity
        val confidence = calculatePredictionConfidence(cycleHistory)
        
        // Generate recommendations
        val recommendations = generateFertilityRecommendations(
            currentCycle,
            cycleHistory,
            confidence,
            predictedOvulationDate
        )
        
        return FertilityWindow(
            cycleStartDate = cycleStartDate,
            predictedOvulationDate = predictedOvulationDate,
            fertilityWindowStart = fertilityWindowStart,
            fertilityWindowEnd = fertilityWindowEnd,
            peakFertilityDate = predictedOvulationDate.plus(DatePeriod(days = -1)), // Day before ovulation
            dailyConceptionProbabilities = dailyProbabilities,
            confidence = confidence,
            averageCycleLength = averageCycleLength,
            recommendations = recommendations
        )
    }
    
    /**
     * Calculates average cycle length from historical data.
     */
    private fun calculateAverageCycleLength(cycleHistory: List<Cycle>): Int {
        val completedCycles = cycleHistory.filter { it.cycleLength != null }
        
        return if (completedCycles.isNotEmpty()) {
            completedCycles.mapNotNull { it.cycleLength }.average().toInt()
        } else {
            28 // Default cycle length
        }
    }
    
    /**
     * Calculates average ovulation day from historical data.
     */
    private fun calculateAverageOvulationDay(cycleHistory: List<Cycle>, averageCycleLength: Int): Int {
        val cyclesWithOvulation = cycleHistory.filter { 
            it.confirmedOvulationDate != null || it.predictedOvulationDate != null 
        }
        
        if (cyclesWithOvulation.isNotEmpty()) {
            val ovulationDays = cyclesWithOvulation.map { cycle ->
                val ovulationDate = cycle.confirmedOvulationDate ?: cycle.predictedOvulationDate!!
                (ovulationDate.toEpochDays() - cycle.startDate.toEpochDays()).toInt() + 1
            }
            return ovulationDays.average().toInt()
        }
        
        // Default: ovulation typically occurs 14 days before next period
        return maxOf(1, averageCycleLength - 14)
    }
    
    /**
     * Calculates daily conception probabilities within the fertility window.
     */
    private fun calculateDailyConceptionProbabilities(
        windowStart: LocalDate,
        windowEnd: LocalDate,
        ovulationDate: LocalDate
    ): Map<LocalDate, Double> {
        val probabilities = mutableMapOf<LocalDate, Double>()
        
        var currentDate = windowStart
        while (currentDate <= windowEnd) {
            val daysFromOvulation = currentDate.toEpochDays() - ovulationDate.toEpochDays()
            
            val probability = when (daysFromOvulation.toInt()) {
                -5 -> 0.10 // 5 days before ovulation
                -4 -> 0.16 // 4 days before ovulation
                -3 -> 0.14 // 3 days before ovulation
                -2 -> 0.20 // 2 days before ovulation
                -1 -> 0.25 // 1 day before ovulation (peak)
                0 -> 0.15  // Ovulation day
                else -> 0.05 // Outside optimal window
            }
            
            probabilities[currentDate] = probability
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return probabilities
    }
    
    /**
     * Calculates prediction confidence based on cycle regularity.
     */
    private fun calculatePredictionConfidence(cycleHistory: List<Cycle>): Double {
        if (cycleHistory.size < 3) {
            return 0.3 // Low confidence with insufficient data
        }
        
        val cycleLengths = cycleHistory.mapNotNull { it.cycleLength }
        if (cycleLengths.isEmpty()) {
            return 0.4 // Low confidence without completed cycles
        }
        
        // Calculate cycle length variability
        val averageLength = cycleLengths.average()
        val variance = cycleLengths.map { (it - averageLength) * (it - averageLength) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        // Higher confidence for more regular cycles
        val regularityScore = when {
            standardDeviation <= 2.0 -> 0.9 // Very regular
            standardDeviation <= 4.0 -> 0.7 // Moderately regular
            standardDeviation <= 7.0 -> 0.5 // Somewhat irregular
            else -> 0.3 // Very irregular
        }
        
        // Bonus for more data points
        val dataBonus = minOf(0.2, (cycleHistory.size - 3) * 0.05)
        
        return minOf(1.0, regularityScore + dataBonus)
    }
    
    /**
     * Generates personalized fertility recommendations.
     */
    private fun generateFertilityRecommendations(
        currentCycle: Cycle,
        cycleHistory: List<Cycle>,
        confidence: Double,
        predictedOvulationDate: LocalDate
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Confidence-based recommendations
        when {
            confidence >= 0.8 -> {
                recommendations.add("Your cycles are very regular - fertility predictions are highly reliable")
                recommendations.add("Focus intercourse on the 2-3 days before predicted ovulation for best conception chances")
            }
            confidence >= 0.6 -> {
                recommendations.add("Your cycles show good regularity - predictions are moderately reliable")
                recommendations.add("Track fertility signs (BBT, cervical mucus, OPK) to confirm ovulation timing")
            }
            confidence >= 0.4 -> {
                recommendations.add("Your cycles show some variability - use fertility tracking for better accuracy")
                recommendations.add("Consider daily fertility sign tracking throughout your cycle")
            }
            else -> {
                recommendations.add("Your cycles are irregular - fertility predictions may be less accurate")
                recommendations.add("Track multiple fertility indicators and consider consulting a healthcare provider")
            }
        }
        
        // Cycle length specific recommendations
        val averageLength = calculateAverageCycleLength(cycleHistory)
        when {
            averageLength < 21 -> {
                recommendations.add("Your cycles are shorter than average - consider consulting a healthcare provider")
            }
            averageLength > 35 -> {
                recommendations.add("Your cycles are longer than average - ovulation may be irregular")
                recommendations.add("Consider tracking ovulation signs more carefully")
            }
        }
        
        // General fertility recommendations
        recommendations.add("The fertile window typically lasts 6 days ending on ovulation day")
        recommendations.add("Sperm can survive up to 5 days, while the egg survives 12-24 hours after ovulation")
        
        return recommendations
    }
}

/**
 * Comprehensive fertility window calculation result.
 */
data class FertilityWindow(
    val cycleStartDate: LocalDate,
    val predictedOvulationDate: LocalDate,
    val fertilityWindowStart: LocalDate,
    val fertilityWindowEnd: LocalDate,
    val peakFertilityDate: LocalDate,
    val dailyConceptionProbabilities: Map<LocalDate, Double>,
    val confidence: Double,
    val averageCycleLength: Int,
    val recommendations: List<String>
)