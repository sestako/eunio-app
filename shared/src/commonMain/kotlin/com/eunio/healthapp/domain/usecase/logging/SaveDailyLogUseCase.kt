package com.eunio.healthapp.domain.usecase.logging

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.Validation
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Use case for saving daily health log entries with comprehensive validation.
 * Handles data validation for all health tracking fields and ensures data integrity.
 */
class SaveDailyLogUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Saves a daily health log entry after validation.
     * 
     * @param dailyLog The daily log data to save
     * @return Result indicating success or failure of the save operation
     */
    suspend operator fun invoke(dailyLog: DailyLog): Result<Unit> {
        // Validate the daily log data
        val validationResult = validateDailyLogData(dailyLog)
        if (validationResult is ValidationResult.Error) {
            return Result.Error(
                AppError.ValidationError("Validation failed: ${validationResult.errors.joinToString(", ")}")
            )
        }
        
        // Check if the log date is not in the future
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (dailyLog.date > today) {
            return Result.Error(
                AppError.ValidationError("Cannot log data for future dates")
            )
        }
        
        // Save the daily log
        return when (val result = logRepository.saveDailyLog(dailyLog)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(
                AppError.DataSyncError("Failed to save daily log: ${result.error.message}")
            )
        }
    }
    
    /**
     * Validates daily log data with comprehensive field validation.
     * 
     * @param dailyLog The daily log to validate
     * @return ValidationResult indicating success or specific validation errors
     */
    private fun validateDailyLogData(dailyLog: DailyLog): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Basic validation (excluding BBT which we handle separately)
        if (dailyLog.id.isBlank()) {
            errors.add("Daily log ID cannot be blank")
        }
        
        if (dailyLog.userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        // Notes validation
        dailyLog.notes?.let { notes ->
            if (notes.length > 1000) {
                errors.add("Notes cannot exceed 1000 characters")
            }
        }
        
        // Symptoms validation
        if (dailyLog.symptoms.size > 10) {
            errors.add("Cannot log more than 10 symptoms per day")
        }
        
        // Additional comprehensive validation for health tracking fields
        
        // BBT (Basal Body Temperature) validation - support both Celsius and Fahrenheit ranges
        dailyLog.bbt?.let { temp ->
            when {
                // Assume Fahrenheit if temp > 50 (since normal body temp in Celsius is ~37)
                temp > 50 -> {
                    // Fahrenheit validation
                    when {
                        temp < 95.0 -> errors.add("BBT cannot be below 95.0°F")
                        temp > 104.0 -> errors.add("BBT cannot be above 104.0°F")
                        else -> { /* Temperature is in normal Fahrenheit range */ }
                    }
                }
                else -> {
                    // Celsius validation
                    when {
                        temp < 35.0 -> errors.add("BBT cannot be below 35.0°C")
                        temp > 40.0 -> errors.add("BBT cannot be above 40.0°C")
                        else -> { /* Temperature is in normal Celsius range */ }
                    }
                }
            }
        }
        

        
        // Sexual activity validation
        dailyLog.sexualActivity?.let { activity ->
            if (activity.occurred && activity.protection == null) {
                // This is just a warning, not an error - user might choose not to specify
                // We don't add this as an error to allow user flexibility
            }
        }
        
        // Date validation - ensure it's not too far in the past (more than 2 years)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val twoYearsAgo = LocalDate(today.year - 2, today.month, today.dayOfMonth)
        if (dailyLog.date < twoYearsAgo) {
            errors.add("Cannot log data more than 2 years in the past")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validates BBT value with detailed temperature checks.
     * 
     * @param bbt The BBT value to validate
     * @return ValidationResult with specific BBT validation errors
     */
    fun validateBBT(bbt: Double?): ValidationResult {
        if (bbt == null) return ValidationResult.Success
        
        val errors = mutableListOf<String>()
        
        when {
            // Assume Fahrenheit if temp > 50
            bbt > 50 -> {
                when {
                    bbt < 95.0 -> errors.add("BBT cannot be below 95.0°F")
                    bbt > 104.0 -> errors.add("BBT cannot be above 104.0°F")
                    else -> { /* Temperature is in normal Fahrenheit range */ }
                }
            }
            else -> {
                when {
                    bbt < 35.0 -> errors.add("BBT cannot be below 35.0°C")
                    bbt > 40.0 -> errors.add("BBT cannot be above 40.0°C")
                    else -> { /* Temperature is in normal Celsius range */ }
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Validates notes content.
     * 
     * @param notes The notes to validate
     * @return ValidationResult with notes validation errors
     */
    fun validateNotes(notes: String?): ValidationResult {
        if (notes == null) return ValidationResult.Success
        
        val errors = mutableListOf<String>()
        
        if (notes.length > 1000) {
            errors.add("Notes cannot exceed 1000 characters")
        }
        
        // Check for potentially sensitive information patterns (basic check)
        val sensitivePatterns = listOf(
            "password", "ssn", "social security", "credit card", "bank account"
        )
        
        val lowerNotes = notes.lowercase()
        sensitivePatterns.forEach { pattern ->
            if (lowerNotes.contains(pattern)) {
                errors.add("Notes should not contain sensitive information like passwords or financial data")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}