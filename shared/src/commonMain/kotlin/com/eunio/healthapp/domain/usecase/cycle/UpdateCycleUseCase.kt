package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * Use case for updating cycle information including completion, ovulation confirmation,
 * and cycle length calculations.
 */
class UpdateCycleUseCase(
    private val cycleRepository: CycleRepository,
    private val logRepository: LogRepository
) {
    
    /**
     * Completes the current cycle by setting the end date and calculating cycle length.
     * 
     * @param userId The ID of the user
     * @param endDate The end date of the cycle (last day of period)
     * @return Result containing the updated cycle
     */
    suspend fun completeCycle(userId: String, endDate: LocalDate): Result<Cycle> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.error(AppError.ValidationError("No active cycle to complete"))
        
        // Validate end date
        if (endDate < currentCycle.startDate) {
            return Result.error(
                AppError.ValidationError("End date cannot be before cycle start date")
            )
        }
        
        // Calculate cycle length
        val cycleLength = (endDate.toEpochDays() - currentCycle.startDate.toEpochDays()).toInt() + 1
        
        // Validate cycle length
        if (cycleLength < MIN_CYCLE_LENGTH || cycleLength > MAX_CYCLE_LENGTH) {
            return Result.error(
                AppError.ValidationError(
                    "Invalid cycle length: $cycleLength days. Must be between $MIN_CYCLE_LENGTH and $MAX_CYCLE_LENGTH days."
                )
            )
        }
        
        // Calculate luteal phase length if ovulation is confirmed
        val lutealPhaseLength = currentCycle.confirmedOvulationDate?.let { ovulationDate ->
            (endDate.toEpochDays() - ovulationDate.toEpochDays()).toInt()
        }
        
        // Update cycle with completion data
        val updatedCycle = currentCycle.copy(
            endDate = endDate,
            cycleLength = cycleLength,
            lutealPhaseLength = lutealPhaseLength
        )
        
        val updateResult = cycleRepository.updateCycle(updatedCycle)
        return if (updateResult.isError) {
            Result.error(updateResult.errorOrNull()!!)
        } else {
            Result.success(updatedCycle)
        }
    }
    
    /**
     * Confirms ovulation for the current cycle and updates the cycle record.
     * 
     * @param userId The ID of the user
     * @param ovulationDate The confirmed ovulation date
     * @return Result containing the updated cycle
     */
    suspend fun confirmOvulation(userId: String, ovulationDate: LocalDate, referenceDate: LocalDate? = null): Result<Cycle> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.error(AppError.ValidationError("No active cycle to update"))
        
        // Validate ovulation date
        if (ovulationDate < currentCycle.startDate) {
            return Result.error(
                AppError.ValidationError("Ovulation date cannot be before cycle start date")
            )
        }
        
        val today = referenceDate ?: kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        if (ovulationDate > today) {
            return Result.error(
                AppError.ValidationError("Ovulation date cannot be in the future")
            )
        }
        
        // Calculate luteal phase length if cycle is completed
        val lutealPhaseLength = currentCycle.endDate?.let { endDate ->
            (endDate.toEpochDays() - ovulationDate.toEpochDays()).toInt()
        }
        
        // Update cycle with ovulation confirmation
        val updatedCycle = currentCycle.copy(
            confirmedOvulationDate = ovulationDate,
            lutealPhaseLength = lutealPhaseLength
        )
        
        val updateResult = cycleRepository.updateCycle(updatedCycle)
        return if (updateResult.isError) {
            Result.error(updateResult.errorOrNull()!!)
        } else {
            Result.success(updatedCycle)
        }
    }
    
    /**
     * Updates the predicted ovulation date for the current cycle.
     * 
     * @param userId The ID of the user
     * @param predictedDate The new predicted ovulation date
     * @return Result containing the updated cycle
     */
    suspend fun updatePredictedOvulation(userId: String, predictedDate: LocalDate): Result<Cycle> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.error(AppError.ValidationError("No active cycle to update"))
        
        // Validate predicted date
        if (predictedDate < currentCycle.startDate) {
            return Result.error(
                AppError.ValidationError("Predicted ovulation date cannot be before cycle start date")
            )
        }
        
        // Update cycle with new prediction
        val updatedCycle = currentCycle.copy(predictedOvulationDate = predictedDate)
        
        val updateResult = cycleRepository.updateCycle(updatedCycle)
        return if (updateResult.isError) {
            Result.error(updateResult.errorOrNull()!!)
        } else {
            Result.success(updatedCycle)
        }
    }
    
    /**
     * Automatically calculates and updates cycle metrics based on daily log data.
     * This includes detecting ovulation based on fertility indicators.
     * 
     * @param userId The ID of the user
     * @return Result containing the updated cycle with calculated metrics
     */
    suspend fun recalculateCycleMetrics(userId: String): Result<Cycle> {
        // Get current cycle
        val currentCycleResult = cycleRepository.getCurrentCycle(userId)
        if (currentCycleResult.isError) {
            return Result.error(currentCycleResult.errorOrNull()!!)
        }
        
        val currentCycle = currentCycleResult.getOrNull()
            ?: return Result.error(AppError.ValidationError("No active cycle to update"))
        
        // Get fertility logs for the cycle
        val endDate = currentCycle.endDate 
            ?: kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        
        val logsResult = logRepository.getFertilityLogsInRange(
            userId, 
            currentCycle.startDate, 
            endDate
        )
        if (logsResult.isError) {
            return Result.error(logsResult.errorOrNull()!!)
        }
        
        val fertilityLogs = logsResult.getOrNull() ?: emptyList()
        
        // Analyze logs for ovulation indicators
        val detectedOvulation = analyzeOvulationIndicators(fertilityLogs)
        
        // Update cycle with detected ovulation if found and not already confirmed
        val updatedCycle = if (detectedOvulation != null && currentCycle.confirmedOvulationDate == null) {
            val lutealPhaseLength = currentCycle.endDate?.let { end ->
                (end.toEpochDays() - detectedOvulation.toEpochDays()).toInt()
            }
            
            currentCycle.copy(
                confirmedOvulationDate = detectedOvulation,
                lutealPhaseLength = lutealPhaseLength
            )
        } else {
            currentCycle
        }
        
        val updateResult = cycleRepository.updateCycle(updatedCycle)
        return if (updateResult.isError) {
            Result.error(updateResult.errorOrNull()!!)
        } else {
            Result.success(updatedCycle)
        }
    }
    
    /**
     * Analyzes fertility indicators to detect ovulation.
     * Uses BBT patterns and OPK results to determine ovulation date.
     */
    private fun analyzeOvulationIndicators(logs: List<com.eunio.healthapp.domain.model.DailyLog>): LocalDate? {
        // Look for BBT shift pattern
        val bbtLogs = logs.filter { it.bbt != null }.sortedBy { it.date }
        
        if (bbtLogs.size >= 6) {
            // Detect sustained temperature rise
            for (i in 3 until bbtLogs.size) {
                val currentTemp = bbtLogs[i].bbt!!
                val previousTemps = bbtLogs.subList(i - 3, i).map { it.bbt!! }
                val avgPreviousTemp = previousTemps.average()
                
                if (currentTemp - avgPreviousTemp >= 0.2) {
                    // Check if temperature stays elevated for at least 3 days
                    val subsequentTemps = bbtLogs.subList(i, minOf(i + 3, bbtLogs.size))
                    if (subsequentTemps.size >= 2 && subsequentTemps.all { it.bbt!! >= avgPreviousTemp + 0.1 }) {
                        return bbtLogs[i - 1].date // Ovulation likely occurred day before temp rise
                    }
                }
            }
        }
        
        // Look for OPK peak as secondary indicator
        val opkPeakLog = logs
            .filter { it.opkResult == com.eunio.healthapp.domain.model.OPKResult.PEAK }
            .maxByOrNull { it.date }
        
        return opkPeakLog?.date?.plus(DatePeriod(days = 1)) // Ovulation typically 12-36 hours after LH surge
    }
    
    companion object {
        private const val MIN_CYCLE_LENGTH = 15
        private const val MAX_CYCLE_LENGTH = 45
    }
}