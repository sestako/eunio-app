package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import kotlin.math.pow
import kotlin.math.round

/**
 * Implementation of UnitConverter that works with UnitSystem enum (Metric/Imperial).
 * This provides basic conversion functionality for the existing codebase with validation.
 */
class UnitConverterImpl : UnitConverter {
    
    companion object {
        // Conversion constants
        private const val KG_TO_LBS_FACTOR = 2.20462262185
        private const val LBS_TO_KG_FACTOR = 0.45359237
        private const val KM_TO_MILES_FACTOR = 0.621371192
        private const val MILES_TO_KM_FACTOR = 1.609344
        
        // Validation bounds
        private const val MAX_WEIGHT_KG = 1000.0
        private const val MAX_WEIGHT_LBS = 2204.62 // ~1000kg in lbs
        private const val MAX_DISTANCE_KM = 50000.0
        private const val MAX_DISTANCE_MILES = 31068.56 // ~50000km in miles
        private const val MIN_TEMP_CELSIUS = -273.15 // Absolute zero
        private const val MAX_TEMP_CELSIUS = 1000.0
        private const val MIN_TEMP_FAHRENHEIT = -459.67 // Absolute zero in F
        private const val MAX_TEMP_FAHRENHEIT = 1832.0 // ~1000°C in F
    }
    
    override fun convertWeight(value: Double, from: UnitSystem, to: UnitSystem): Double {
        validateWeight(value, from)
        
        if (from == to) return value
        
        return when {
            from == UnitSystem.METRIC && to == UnitSystem.IMPERIAL -> value * KG_TO_LBS_FACTOR
            from == UnitSystem.IMPERIAL && to == UnitSystem.METRIC -> value * LBS_TO_KG_FACTOR
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun convertDistance(value: Double, from: UnitSystem, to: UnitSystem): Double {
        validateDistance(value, from)
        
        if (from == to) return value
        
        return when {
            from == UnitSystem.METRIC && to == UnitSystem.IMPERIAL -> value * KM_TO_MILES_FACTOR
            from == UnitSystem.IMPERIAL && to == UnitSystem.METRIC -> value * MILES_TO_KM_FACTOR
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun convertTemperature(value: Double, from: UnitSystem, to: UnitSystem): Double {
        validateTemperature(value, from)
        
        if (from == to) return value
        
        return when {
            from == UnitSystem.METRIC && to == UnitSystem.IMPERIAL -> (value * 9.0 / 5.0) + 32.0
            from == UnitSystem.IMPERIAL && to == UnitSystem.METRIC -> (value - 32.0) * 5.0 / 9.0
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun formatWeight(value: Double, unitSystem: UnitSystem): String {
        if (!isValidWeightForFormatting(value, unitSystem)) {
            throw UnitSystemError.UnitValidationError(
                message = "Invalid weight value for formatting: $value",
                field = "weight",
                inputValue = value
            )
        }
        
        val unit = when (unitSystem) {
            UnitSystem.METRIC -> "kg"
            UnitSystem.IMPERIAL -> "lbs"
        }
        return "${value.formatForDisplay()} $unit"
    }
    
    override fun formatDistance(value: Double, unitSystem: UnitSystem): String {
        if (!isValidDistanceForFormatting(value, unitSystem)) {
            throw UnitSystemError.UnitValidationError(
                message = "Invalid distance value for formatting: $value",
                field = "distance",
                inputValue = value
            )
        }
        
        val unit = when (unitSystem) {
            UnitSystem.METRIC -> "km"
            UnitSystem.IMPERIAL -> "miles"
        }
        return "${value.formatForDisplay()} $unit"
    }
    
    override fun formatTemperature(value: Double, unitSystem: UnitSystem): String {
        if (!isValidTemperatureForFormatting(value, unitSystem)) {
            throw UnitSystemError.UnitValidationError(
                message = "Invalid temperature value for formatting: $value",
                field = "temperature",
                inputValue = value
            )
        }
        
        val unit = when (unitSystem) {
            UnitSystem.METRIC -> "°C"
            UnitSystem.IMPERIAL -> "°F"
        }
        return "${value.formatForDisplay()}$unit"
    }
    
    private fun Double.roundToDecimalPlaces(places: Int): Double {
        if (places < 0) return this
        val factor = 10.0.pow(places.toDouble())
        return round(this * factor) / factor
    }
    
    private fun Double.formatForDisplay(): String {
        val rounded = this.roundToDecimalPlaces(2)
        return when {
            rounded == rounded.toInt().toDouble() -> rounded.toInt().toString()
            rounded * 10 == (rounded * 10).toInt().toDouble() -> {
                val oneDecimal = this.roundToDecimalPlaces(1)
                oneDecimal.toString()
            }
            else -> rounded.toString()
        }
    }
    
    /**
     * Validates weight values for conversion
     */
    private fun validateWeight(value: Double, unitSystem: UnitSystem) {
        if (!value.isFinite()) {
            throw UnitSystemError.UnitValidationError(
                message = "Weight value must be a finite number",
                field = "weight",
                inputValue = value
            )
        }
        
        if (value < 0) {
            throw UnitSystemError.UnitValidationError(
                message = "Weight cannot be negative",
                field = "weight",
                inputValue = value
            )
        }
        
        val maxWeight = when (unitSystem) {
            UnitSystem.METRIC -> MAX_WEIGHT_KG
            UnitSystem.IMPERIAL -> MAX_WEIGHT_LBS
        }
        
        val unit = when (unitSystem) {
            UnitSystem.METRIC -> "kg"
            UnitSystem.IMPERIAL -> "lbs"
        }
        
        if (value > maxWeight) {
            throw UnitSystemError.UnitValidationError(
                message = "Weight must be between 0 and $maxWeight $unit",
                field = "weight",
                inputValue = value
            )
        }
    }
    
    /**
     * Validates distance values for conversion
     */
    private fun validateDistance(value: Double, unitSystem: UnitSystem) {
        if (!value.isFinite()) {
            throw UnitSystemError.UnitValidationError(
                message = "Distance value must be a finite number",
                field = "distance",
                inputValue = value
            )
        }
        
        if (value < 0) {
            throw UnitSystemError.UnitValidationError(
                message = "Distance cannot be negative",
                field = "distance",
                inputValue = value
            )
        }
        
        val maxDistance = when (unitSystem) {
            UnitSystem.METRIC -> MAX_DISTANCE_KM
            UnitSystem.IMPERIAL -> MAX_DISTANCE_MILES
        }
        
        val unit = when (unitSystem) {
            UnitSystem.METRIC -> "km"
            UnitSystem.IMPERIAL -> "mi"
        }
        
        if (value > maxDistance) {
            throw UnitSystemError.UnitValidationError(
                message = "Distance must be between 0 and $maxDistance $unit",
                field = "distance",
                inputValue = value
            )
        }
    }
    
    /**
     * Validates temperature values for conversion
     */
    private fun validateTemperature(value: Double, unitSystem: UnitSystem) {
        if (!value.isFinite()) {
            throw UnitSystemError.UnitValidationError(
                message = "Temperature value must be a finite number",
                field = "temperature",
                inputValue = value
            )
        }
        
        val (minTemp, maxTemp, unit) = when (unitSystem) {
            UnitSystem.METRIC -> Triple(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS, "°C")
            UnitSystem.IMPERIAL -> Triple(MIN_TEMP_FAHRENHEIT, MAX_TEMP_FAHRENHEIT, "°F")
        }
        
        if (value < minTemp || value > maxTemp) {
            throw UnitSystemError.UnitValidationError(
                message = "Temperature must be between $minTemp and $maxTemp $unit",
                field = "temperature",
                inputValue = value
            )
        }
    }
    
    /**
     * Validates weight values for formatting
     */
    private fun isValidWeightForFormatting(value: Double, unitSystem: UnitSystem): Boolean {
        if (!value.isFinite() || value < 0) return false
        
        val maxWeight = when (unitSystem) {
            UnitSystem.METRIC -> MAX_WEIGHT_KG
            UnitSystem.IMPERIAL -> MAX_WEIGHT_LBS
        }
        
        return value <= maxWeight
    }
    
    /**
     * Validates distance values for formatting
     */
    private fun isValidDistanceForFormatting(value: Double, unitSystem: UnitSystem): Boolean {
        if (!value.isFinite() || value < 0) return false
        
        val maxDistance = when (unitSystem) {
            UnitSystem.METRIC -> MAX_DISTANCE_KM
            UnitSystem.IMPERIAL -> MAX_DISTANCE_MILES
        }
        
        return value <= maxDistance
    }
    
    /**
     * Validates temperature values for formatting
     */
    private fun isValidTemperatureForFormatting(value: Double, unitSystem: UnitSystem): Boolean {
        if (!value.isFinite()) return false
        
        val (minTemp, maxTemp) = when (unitSystem) {
            UnitSystem.METRIC -> Pair(MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS)
            UnitSystem.IMPERIAL -> Pair(MIN_TEMP_FAHRENHEIT, MAX_TEMP_FAHRENHEIT)
        }
        
        return value >= minTemp && value <= maxTemp
    }
}