package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

/**
 * Repository interface for menstrual cycle-related data operations.
 * Handles cycle tracking, predictions, and historical data.
 */
interface CycleRepository {
    
    /**
     * Retrieves the current active cycle for the user.
     * Returns null if no active cycle exists.
     * @param userId The ID of the user
     */
    suspend fun getCurrentCycle(userId: String): Result<Cycle?>
    
    /**
     * Retrieves the user's cycle history, ordered by start date (most recent first).
     * @param userId The ID of the user
     * @param limit Maximum number of cycles to retrieve
     */
    suspend fun getCycleHistory(userId: String, limit: Int = 12): Result<List<Cycle>>
    
    /**
     * Starts a new menstrual cycle for the user.
     * Automatically closes any existing active cycle.
     * @param userId The ID of the user
     * @param startDate The start date of the new cycle
     */
    suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle>
    
    /**
     * Updates an existing cycle with new information.
     * @param cycle The updated cycle data
     */
    suspend fun updateCycle(cycle: Cycle): Result<Unit>
    
    /**
     * Ends the current active cycle by setting the end date.
     * @param userId The ID of the user
     * @param endDate The end date of the cycle
     */
    suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit>
    
    /**
     * Retrieves cycles within a specific date range.
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     */
    suspend fun getCyclesInRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<Cycle>>
    
    /**
     * Calculates the average cycle length based on historical data.
     * @param userId The ID of the user
     * @param cycleCount Number of recent cycles to include in calculation
     */
    suspend fun getAverageCycleLength(userId: String, cycleCount: Int = 6): Result<Double?>
    
    /**
     * Predicts the next period start date based on cycle history.
     * @param userId The ID of the user
     */
    suspend fun predictNextPeriod(userId: String): Result<LocalDate?>
    
    /**
     * Confirms ovulation for a specific cycle.
     * @param cycleId The ID of the cycle
     * @param ovulationDate The confirmed ovulation date
     */
    suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit>
}