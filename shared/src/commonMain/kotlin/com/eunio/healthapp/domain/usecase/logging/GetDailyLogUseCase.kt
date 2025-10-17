package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

/**
 * Use case for retrieving daily health log entries for specific dates.
 * Handles validation and retrieval of daily log data.
 */
class GetDailyLogUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Retrieves the daily log for a specific date.
     * 
     * @param userId The ID of the user
     * @param date The date to retrieve the log for
     * @return Result containing the daily log or null if no log exists for that date
     */
    suspend operator fun invoke(userId: String, date: LocalDate): Result<DailyLog?> {
        // Validate input parameters
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        // Retrieve the daily log
        return when (val result = logRepository.getDailyLog(userId, date)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve daily log: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves the daily log for a specific date with additional validation.
     * Returns a more detailed error if the log doesn't exist.
     * 
     * @param userId The ID of the user
     * @param date The date to retrieve the log for
     * @return Result containing the daily log, with specific error if not found
     */
    suspend fun getDailyLogOrError(userId: String, date: LocalDate): Result<DailyLog> {
        val result = invoke(userId, date)
        
        return when (result) {
            is Result.Success -> {
                if (result.data != null) {
                    Result.success(result.data)
                } else {
                    Result.error(AppError.ValidationError("No log found for date: $date"))
                }
            }
            is Result.Error -> result
        }
    }
    
    /**
     * Checks if a daily log exists for a specific date.
     * 
     * @param userId The ID of the user
     * @param date The date to check
     * @return Result containing true if a log exists, false otherwise
     */
    suspend fun hasLogForDate(userId: String, date: LocalDate): Result<Boolean> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        return when (val result = logRepository.getDailyLog(userId, date)) {
            is Result.Success -> Result.success(result.data != null)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to check log existence: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves multiple daily logs for specific dates.
     * 
     * @param userId The ID of the user
     * @param dates List of dates to retrieve logs for
     * @return Result containing a map of dates to their corresponding logs (null if no log exists)
     */
    suspend fun getDailyLogsForDates(
        userId: String, 
        dates: List<LocalDate>
    ): Result<Map<LocalDate, DailyLog?>> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        if (dates.isEmpty()) {
            return Result.success(emptyMap())
        }
        
        if (dates.size > 100) {
            return Result.error(AppError.ValidationError("Cannot retrieve more than 100 logs at once"))
        }
        
        val logsMap = mutableMapOf<LocalDate, DailyLog?>()
        
        // Retrieve logs for each date
        for (date in dates) {
            when (val result = logRepository.getDailyLog(userId, date)) {
                is Result.Success -> {
                    logsMap[date] = result.data
                }
                is Result.Error -> {
                    return Result.error(
                        AppError.DataSyncError("Failed to retrieve log for date $date: ${result.error.message}")
                    )
                }
            }
        }
        
        return Result.success(logsMap)
    }
    
    /**
     * Retrieves the most recent daily log for a user.
     * 
     * @param userId The ID of the user
     * @return Result containing the most recent daily log or null if no logs exist
     */
    suspend fun getMostRecentLog(userId: String): Result<DailyLog?> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        return when (val result = logRepository.getRecentLogs(userId, 1)) {
            is Result.Success -> {
                val mostRecent = result.data.firstOrNull()
                Result.success(mostRecent)
            }
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve most recent log: ${result.error.message}")
            )
        }
    }
}