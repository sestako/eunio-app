package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

/**
 * Implementation of CycleRepository that manages menstrual cycle data.
 * Handles cycle tracking, predictions, and historical analysis.
 */
class CycleRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val errorHandler: ErrorHandler
) : CycleRepository {

    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return try {
            firestoreService.getCurrentCycle(userId)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        return try {
            if (limit <= 0) {
                return Result.error(errorHandler.createValidationError("Limit must be positive", "limit"))
            }
            
            firestoreService.getCycleHistory(userId, limit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> {
        return try {
            // Validate that start date is not in the future
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            if (startDate > today) {
                return Result.error(errorHandler.createValidationError("Cycle start date cannot be in the future", "startDate"))
            }
            
            // End current cycle if it exists
            val currentCycleResult = getCurrentCycle(userId)
            if (currentCycleResult.isSuccess) {
                val currentCycle = currentCycleResult.getOrNull()
                if (currentCycle != null && currentCycle.endDate == null) {
                    // Calculate cycle length and end the current cycle
                    val cycleLength = startDate.toEpochDays() - currentCycle.startDate.toEpochDays()
                    if (cycleLength > 0) {
                        val endedCycle = currentCycle.copy(
                            endDate = startDate.plus(-1, DateTimeUnit.DAY),
                            cycleLength = cycleLength.toInt()
                        )
                        firestoreService.updateCycle(endedCycle)
                    }
                }
            }
            
            // Create new cycle
            val newCycle = Cycle(
                id = "cycle_${userId}_${startDate.toEpochDays()}",
                userId = userId,
                startDate = startDate
            )
            
            val saveResult = firestoreService.saveCycle(newCycle)
            if (saveResult.isError) {
                return saveResult.map { newCycle }
            }
            
            Result.success(newCycle)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        return try {
            // Validate cycle data
            if (cycle.endDate != null && cycle.endDate < cycle.startDate) {
                return Result.error(errorHandler.createValidationError("End date cannot be before start date", "endDate"))
            }
            
            if (cycle.cycleLength != null && cycle.cycleLength <= 0) {
                return Result.error(errorHandler.createValidationError("Cycle length must be positive", "cycleLength"))
            }
            
            if (cycle.lutealPhaseLength != null && cycle.lutealPhaseLength <= 0) {
                return Result.error(errorHandler.createValidationError("Luteal phase length must be positive", "lutealPhaseLength"))
            }
            
            firestoreService.updateCycle(cycle)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> {
        return try {
            val currentCycleResult = getCurrentCycle(userId)
            if (currentCycleResult.isError) {
                return currentCycleResult.map { }
            }
            
            val currentCycle = currentCycleResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("No active cycle found"))
            
            if (endDate < currentCycle.startDate) {
                return Result.error(errorHandler.createValidationError("End date cannot be before start date", "endDate"))
            }
            
            val cycleLength = endDate.toEpochDays() - currentCycle.startDate.toEpochDays() + 1
            val updatedCycle = currentCycle.copy(
                endDate = endDate,
                cycleLength = cycleLength.toInt()
            )
            
            updateCycle(updatedCycle)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getCyclesInRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Cycle>> {
        return try {
            if (endDate < startDate) {
                return Result.error(errorHandler.createValidationError("End date cannot be before start date", "dateRange"))
            }
            
            val historyResult = getCycleHistory(userId, 100) // Get more cycles to filter
            if (historyResult.isError) {
                return historyResult
            }
            
            val allCycles = historyResult.getOrThrow()
            val filteredCycles = allCycles.filter { cycle ->
                cycle.startDate <= endDate && (cycle.endDate == null || cycle.endDate >= startDate)
            }
            
            Result.success(filteredCycles)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> {
        return try {
            if (cycleCount <= 0) {
                return Result.error(errorHandler.createValidationError("Cycle count must be positive", "cycleCount"))
            }
            
            val historyResult = getCycleHistory(userId, cycleCount)
            if (historyResult.isError) {
                return historyResult.map { null }
            }
            
            val cycles = historyResult.getOrThrow()
            val completedCycles = cycles.filter { it.cycleLength != null }
            
            if (completedCycles.isEmpty()) {
                return Result.success(null)
            }
            
            val averageLength = completedCycles.map { it.cycleLength!! }.average()
            Result.success(averageLength)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> {
        return try {
            val currentCycleResult = getCurrentCycle(userId)
            if (currentCycleResult.isError) {
                return currentCycleResult.map { null }
            }
            
            val currentCycle = currentCycleResult.getOrNull()
            if (currentCycle == null) {
                return Result.success(null)
            }
            
            // Get average cycle length
            val averageLengthResult = getAverageCycleLength(userId, 6)
            if (averageLengthResult.isError) {
                return averageLengthResult.map { null }
            }
            
            val averageLength = averageLengthResult.getOrNull()
            if (averageLength == null) {
                // Use default cycle length if no history
                val predictedDate = currentCycle.startDate.plus(28, DateTimeUnit.DAY)
                return Result.success(predictedDate)
            }
            
            val predictedDate = currentCycle.startDate.plus(averageLength.toInt(), DateTimeUnit.DAY)
            Result.success(predictedDate)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> {
        return try {
            // Get the cycle to update
            val cycleResult = firestoreService.getCycle("", cycleId) // userId not needed for this operation
            if (cycleResult.isError) {
                return cycleResult.map { }
            }
            
            val cycle = cycleResult.getOrNull()
                ?: return Result.error(AppError.ValidationError("Cycle not found"))
            
            // Validate ovulation date is within cycle bounds
            if (ovulationDate < cycle.startDate) {
                return Result.error(errorHandler.createValidationError("Ovulation date cannot be before cycle start", "ovulationDate"))
            }
            
            if (cycle.endDate != null && ovulationDate > cycle.endDate) {
                return Result.error(errorHandler.createValidationError("Ovulation date cannot be after cycle end", "ovulationDate"))
            }
            
            // Calculate luteal phase length if cycle is complete
            val lutealPhaseLength = if (cycle.endDate != null) {
                cycle.endDate.toEpochDays() - ovulationDate.toEpochDays()
            } else null
            
            val updatedCycle = cycle.copy(
                confirmedOvulationDate = ovulationDate,
                lutealPhaseLength = lutealPhaseLength?.toInt()
            )
            
            updateCycle(updatedCycle)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
}