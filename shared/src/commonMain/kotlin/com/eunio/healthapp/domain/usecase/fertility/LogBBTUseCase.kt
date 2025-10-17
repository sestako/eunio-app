package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Use case for logging Basal Body Temperature (BBT) with comprehensive validation.
 * Handles temperature validation, measurement timing, and data integrity checks.
 */
class LogBBTUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Logs BBT for a specific date with validation.
     * 
     * @param userId The ID of the user
     * @param date The date for the BBT measurement
     * @param temperature The BBT value in Celsius
     * @return Result indicating success or failure of the logging operation
     */
    suspend operator fun invoke(
        userId: String,
        date: LocalDate,
        temperature: Double
    ): Result<Unit> {
        // Validate BBT value
        val validationResult = validateBBT(temperature)
        if (validationResult is ValidationResult.Error) {
            return Result.error(
                AppError.ValidationError("BBT validation failed: ${validationResult.errors.joinToString(", ")}")
            )
        }
        
        // Check if the date is not in the future
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (date > today) {
            return Result.error(
                AppError.ValidationError("Cannot log BBT for future dates")
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
            // Update existing log with BBT
            existingLog.copy(
                bbt = temperature,
                updatedAt = Clock.System.now()
            )
        } else {
            // Create new log with BBT
            DailyLog(
                id = generateLogId(userId, date),
                userId = userId,
                date = date,
                bbt = temperature,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // Save the updated log
        return when (val result = logRepository.saveDailyLog(updatedLog)) {
            is Result.Success -> Result.success(Unit)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to save BBT log: ${result.error.message}")
            )
        }
    }
    
    /**
     * Validates BBT value with detailed temperature checks.
     * 
     * @param temperature The BBT value to validate in Celsius
     * @return ValidationResult with specific BBT validation errors
     */
    fun validateBBT(temperature: Double): ValidationResult {
        val errors = mutableListOf<String>()
        
        when {
            temperature < 35.0 -> errors.add("BBT cannot be below 35.0°C (95.0°F) - this may indicate measurement error")
            temperature > 40.0 -> errors.add("BBT cannot be above 40.0°C (104.0°F) - this may indicate fever or measurement error")
            temperature < 36.0 -> errors.add("BBT below 36.0°C (96.8°F) is unusually low - please verify measurement")
            temperature > 38.0 -> errors.add("BBT above 38.0°C (100.4°F) may indicate fever - consider if you're feeling unwell")
            // Normal range validation
            temperature < 36.1 || temperature > 37.8 -> {
                // This is a warning, not an error - BBT can vary outside typical range
                // We don't add this as an error to allow for individual variation
            }
        }
        
        // Check for reasonable precision (BBT should be measured to 0.1°C precision)
        val decimalPlaces = temperature.toString().substringAfter('.', "").length
        if (decimalPlaces > 2) {
            errors.add("BBT should be measured to 0.1°C precision (one decimal place)")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Converts Fahrenheit to Celsius for BBT logging.
     * 
     * @param fahrenheit Temperature in Fahrenheit
     * @return Temperature in Celsius
     */
    fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return (fahrenheit - 32) * 5 / 9
    }
    
    /**
     * Validates BBT measurement timing and provides guidance.
     * 
     * @param measurementHour Hour of measurement (0-23)
     * @return ValidationResult with timing guidance
     */
    fun validateMeasurementTiming(measurementHour: Int): ValidationResult {
        val warnings = mutableListOf<String>()
        
        when {
            measurementHour > 10 -> {
                warnings.add("BBT measured after 10:00 AM may not be accurate for fertility tracking")
            }
            measurementHour < 5 || measurementHour > 9 -> {
                warnings.add("BBT is most accurate when measured between 5:00-9:00 AM upon waking")
            }
        }
        
        return if (warnings.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(warnings)
        }
    }
    
    /**
     * Generates a unique log ID for the user and date.
     */
    private fun generateLogId(userId: String, date: LocalDate): String {
        return "${userId}_${date}"
    }
}