package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Use case for retrieving daily log history with various filtering and date range options.
 * Handles complex queries for historical health data analysis.
 */
class GetLogHistoryUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Retrieves daily logs within a specific date range.
     * 
     * @param userId The ID of the user
     * @param startDate The start of the date range (inclusive)
     * @param endDate The end of the date range (inclusive)
     * @return Result containing the list of daily logs in the specified range
     */
    suspend operator fun invoke(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        // Validate input parameters
        val validationResult = validateDateRange(userId, startDate, endDate)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        // Retrieve logs in the specified range
        return when (val result = logRepository.getLogsInRange(userId, startDate, endDate)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve log history: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves recent daily logs for a specified number of days.
     * 
     * @param userId The ID of the user
     * @param days Number of recent days to retrieve (default: 30)
     * @return Result containing the list of recent daily logs
     */
    suspend fun getRecentLogs(userId: String, days: Int = 30): Result<List<DailyLog>> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        if (days <= 0) {
            return Result.error(AppError.ValidationError("Number of days must be positive"))
        }
        
        if (days > 365) {
            return Result.error(AppError.ValidationError("Cannot retrieve more than 365 days of history"))
        }
        
        return when (val result = logRepository.getRecentLogs(userId, days)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve recent logs: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves logs for the current month.
     * 
     * @param userId The ID of the user
     * @param referenceDate The reference date to determine the current month (default: today)
     * @return Result containing logs for the current month
     */
    suspend fun getCurrentMonthLogs(
        userId: String, 
        referenceDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Result<List<DailyLog>> {
        val startOfMonth = LocalDate(referenceDate.year, referenceDate.month, 1)
        val endOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        
        return invoke(userId, startOfMonth, endOfMonth)
    }
    
    /**
     * Retrieves logs for the previous month.
     * 
     * @param userId The ID of the user
     * @param referenceDate The reference date to determine the previous month (default: today)
     * @return Result containing logs for the previous month
     */
    suspend fun getPreviousMonthLogs(
        userId: String, 
        referenceDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): Result<List<DailyLog>> {
        val previousMonth = referenceDate.minus(DatePeriod(months = 1))
        val startOfMonth = LocalDate(previousMonth.year, previousMonth.month, 1)
        val endOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        
        return invoke(userId, startOfMonth, endOfMonth)
    }
    
    /**
     * Retrieves logs that contain period flow data within a date range.
     * 
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Result containing logs with period flow data
     */
    suspend fun getPeriodLogs(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        val validationResult = validateDateRange(userId, startDate, endDate)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        return when (val result = logRepository.getPeriodLogsInRange(userId, startDate, endDate)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve period logs: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves logs that contain BBT (Basal Body Temperature) data.
     * 
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Result containing logs with BBT data
     */
    suspend fun getBBTLogs(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        val validationResult = validateDateRange(userId, startDate, endDate)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        return when (val result = logRepository.getBBTLogsInRange(userId, startDate, endDate)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve BBT logs: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves logs that contain fertility indicator data (cervical mucus, OPK results).
     * 
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return Result containing logs with fertility data
     */
    suspend fun getFertilityLogs(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        val validationResult = validateDateRange(userId, startDate, endDate)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        return when (val result = logRepository.getFertilityLogsInRange(userId, startDate, endDate)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve fertility logs: ${result.error.message}")
            )
        }
    }
    
    /**
     * Retrieves logs that contain specific symptoms.
     * 
     * @param userId The ID of the user
     * @param symptoms List of symptoms to search for
     * @param startDate Optional start date for the search range
     * @param endDate Optional end date for the search range
     * @return Result containing logs that match the specified symptoms
     */
    suspend fun getLogsBySymptoms(
        userId: String, 
        symptoms: List<Symptom>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<List<DailyLog>> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        if (symptoms.isEmpty()) {
            return Result.error(AppError.ValidationError("At least one symptom must be specified"))
        }
        
        // Validate date range if provided
        if (startDate != null && endDate != null) {
            val validationResult = validateDateRange(userId, startDate, endDate)
            if (validationResult is Result.Error) {
                return validationResult
            }
        }
        
        return when (val result = logRepository.getLogsBySymptoms(userId, symptoms, startDate, endDate)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to retrieve logs by symptoms: ${result.error.message}")
            )
        }
    }
    
    /**
     * Gets the total count of logs for a user.
     * 
     * @param userId The ID of the user
     * @return Result containing the total number of logs
     */
    suspend fun getLogCount(userId: String): Result<Int> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        return when (val result = logRepository.getLogCount(userId)) {
            is Result.Success -> Result.success(result.data)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to get log count: ${result.error.message}")
            )
        }
    }
    
    /**
     * Validates the date range parameters.
     * 
     * @param userId The user ID to validate
     * @param startDate The start date
     * @param endDate The end date
     * @return Result indicating validation success or specific error
     */
    private fun validateDateRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<Unit> {
        if (userId.isBlank()) {
            return Result.error(AppError.ValidationError("User ID cannot be blank"))
        }
        
        if (startDate > endDate) {
            return Result.error(AppError.ValidationError("Start date cannot be after end date"))
        }
        
        // Check for reasonable date range (not more than 2 years)
        val daysDifference = endDate.toEpochDays() - startDate.toEpochDays()
        if (daysDifference > 730) { // 2 years
            return Result.error(AppError.ValidationError("Date range cannot exceed 2 years"))
        }
        
        return Result.success(Unit)
    }
}