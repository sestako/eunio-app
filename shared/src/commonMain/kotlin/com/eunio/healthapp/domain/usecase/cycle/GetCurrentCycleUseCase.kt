package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * Use case for retrieving and managing the current active cycle.
 * Provides information about the user's current menstrual cycle status.
 */
class GetCurrentCycleUseCase(
    private val cycleRepository: CycleRepository
) {
    
    /**
     * Retrieves the current active cycle for the user.
     * 
     * @param userId The ID of the user
     * @return Result containing the current cycle or null if no active cycle exists
     */
    suspend fun execute(userId: String): Result<Cycle?> {
        return cycleRepository.getCurrentCycle(userId)
    }
    
    /**
     * Gets the current cycle with additional calculated information.
     * 
     * @param userId The ID of the user
     * @param referenceDate The date to use for calculations (defaults to today)
     * @return Result containing cycle info with calculated fields
     */
    suspend fun executeWithInfo(userId: String, referenceDate: LocalDate? = null): Result<CycleInfo?> {
        val cycleResult = cycleRepository.getCurrentCycle(userId)
        if (cycleResult.isError) {
            return Result.error(cycleResult.errorOrNull()!!)
        }
        
        val cycle = cycleResult.getOrNull() ?: return Result.success(null)
        
        val today = referenceDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        val cycleInfo = CycleInfo(
            cycle = cycle,
            currentDay = calculateCurrentDay(cycle, today),
            phase = calculateCurrentPhase(cycle, today),
            daysUntilNextPeriod = calculateDaysUntilNextPeriod(cycle, today),
            isActive = cycle.endDate == null
        )
        
        return Result.success(cycleInfo)
    }
    
    /**
     * Checks if the user has an active cycle.
     * 
     * @param userId The ID of the user
     * @return Result containing true if there's an active cycle, false otherwise
     */
    suspend fun hasActiveCycle(userId: String): Result<Boolean> {
        val cycleResult = execute(userId)
        if (cycleResult.isError) {
            return Result.error(cycleResult.errorOrNull()!!)
        }
        
        val cycle = cycleResult.getOrNull()
        return Result.success(cycle != null && cycle.endDate == null)
    }
    
    /**
     * Gets the length of the current cycle in days.
     * 
     * @param userId The ID of the user
     * @param referenceDate The date to use for calculations (defaults to today)
     * @return Result containing the cycle length or null if no active cycle
     */
    suspend fun getCurrentCycleLength(userId: String, referenceDate: LocalDate? = null): Result<Int?> {
        val cycleResult = execute(userId)
        if (cycleResult.isError) {
            return Result.error(cycleResult.errorOrNull()!!)
        }
        
        val cycle = cycleResult.getOrNull() ?: return Result.success(null)
        val today = referenceDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        val length = if (cycle.endDate != null) {
            cycle.cycleLength
        } else {
            // Calculate current length for active cycle
            (today.toEpochDays() - cycle.startDate.toEpochDays()).toInt() + 1
        }
        
        return Result.success(length)
    }
    
    /**
     * Calculates the current day of the cycle (1-based).
     */
    private fun calculateCurrentDay(cycle: Cycle, today: LocalDate): Int {
        return (today.toEpochDays() - cycle.startDate.toEpochDays()).toInt() + 1
    }
    
    /**
     * Determines the current phase of the cycle.
     */
    private fun calculateCurrentPhase(cycle: Cycle, today: LocalDate): CyclePhase {
        val currentDay = calculateCurrentDay(cycle, today)
        
        return when {
            currentDay <= 5 -> CyclePhase.MENSTRUAL
            currentDay <= 13 -> CyclePhase.FOLLICULAR
            currentDay <= 16 -> CyclePhase.OVULATION
            else -> CyclePhase.LUTEAL
        }
    }
    
    /**
     * Calculates estimated days until next period based on average cycle length.
     */
    private fun calculateDaysUntilNextPeriod(cycle: Cycle, today: LocalDate): Int? {
        // Use cycle length if available, otherwise use average of 28 days
        val estimatedCycleLength = cycle.cycleLength ?: 28
        val currentDay = calculateCurrentDay(cycle, today)
        
        return if (currentDay <= estimatedCycleLength) {
            estimatedCycleLength - currentDay + 1
        } else {
            // Cycle is longer than expected, can't predict accurately
            null
        }
    }
}

/**
 * Data class containing current cycle information with calculated fields.
 */
data class CycleInfo(
    val cycle: Cycle,
    val currentDay: Int,
    val phase: CyclePhase,
    val daysUntilNextPeriod: Int?,
    val isActive: Boolean
)

/**
 * Enum representing the different phases of a menstrual cycle.
 */
enum class CyclePhase {
    MENSTRUAL,    // Days 1-5: Period
    FOLLICULAR,   // Days 6-13: Pre-ovulation
    OVULATION,    // Days 14-16: Ovulation window
    LUTEAL        // Days 17+: Post-ovulation
}