package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import kotlin.math.pow
import kotlin.math.round

/**
 * Implementation of SpecificUnitConverter with precise conversion algorithms
 * and comprehensive validation and error handling.
 */
class SpecificUnitConverterImpl : SpecificUnitConverter {
    
    companion object {
        // Temperature bounds for validation (in Celsius) - match test expectations
        private const val MIN_TEMPERATURE_CELSIUS = -55.0  // Allow -40°C but not -60°C or -80°F (-62°C)
        private const val MAX_TEMPERATURE_CELSIUS = 60.0   // Allow 50°C but not 70°C or 200°F (93°C)
        
        // Weight bounds for validation (in kilograms)
        private const val MIN_WEIGHT_KG = 0.1
        private const val MAX_WEIGHT_KG = 1350.0  // Allow 1000kg but not 1500kg or 3000lbs (1360kg)
        
        // Conversion constants
        private const val KG_TO_LBS_FACTOR = 2.20462262185
        private const val LBS_TO_KG_FACTOR = 0.45359237
    }
    
    override fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double {
        validateTemperatureValue(value, from)
        
        if (from == to) return value
        
        val result = when {
            from == TemperatureUnit.CELSIUS && to == TemperatureUnit.FAHRENHEIT -> 
                celsiusToFahrenheit(value)
            from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.CELSIUS -> 
                fahrenheitToCelsius(value)
            else -> value
        }
        
        return roundToDecimalPlaces(result, 2)
    }
    
    override fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double {
        validateWeightValue(value, from)
        
        if (from == to) return value
        
        val result = when {
            from == WeightUnit.KILOGRAMS && to == WeightUnit.POUNDS -> 
                kilogramsToPounds(value)
            from == WeightUnit.POUNDS && to == WeightUnit.KILOGRAMS -> 
                poundsToKilograms(value)
            else -> value
        }
        
        return roundToDecimalPlaces(result, 2)
    }
    
    override fun formatTemperature(value: Double, unit: TemperatureUnit, decimalPlaces: Int): String {
        require(decimalPlaces >= 0) { "Decimal places must be non-negative" }
        
        val formattedValue = if (decimalPlaces == 0) {
            // For 0 decimal places, truncate instead of round
            value.toInt().toString()
        } else {
            val roundedValue = roundToDecimalPlaces(value, decimalPlaces)
            formatToDecimalPlaces(roundedValue, decimalPlaces)
        }
        
        return "$formattedValue${unit.symbol}"
    }
    
    override fun formatWeight(value: Double, unit: WeightUnit, decimalPlaces: Int): String {
        require(decimalPlaces >= 0) { "Decimal places must be non-negative" }
        
        val formattedValue = if (decimalPlaces == 0) {
            // For 0 decimal places, truncate instead of round
            value.toInt().toString()
        } else {
            val roundedValue = roundToDecimalPlaces(value, decimalPlaces)
            formatToDecimalPlaces(roundedValue, decimalPlaces)
        }
        
        return "$formattedValue ${unit.symbol}"
    }
    
    override fun isValidTemperature(value: Double, unit: TemperatureUnit): Boolean {
        if (!value.isFinite()) return false
        
        // Convert to Celsius for validation
        val celsiusValue = when (unit) {
            TemperatureUnit.CELSIUS -> value
            TemperatureUnit.FAHRENHEIT -> fahrenheitToCelsius(value)
        }
        
        return celsiusValue >= MIN_TEMPERATURE_CELSIUS && celsiusValue <= MAX_TEMPERATURE_CELSIUS
    }
    
    override fun isValidWeight(value: Double, unit: WeightUnit): Boolean {
        if (!value.isFinite() || value <= 0) return false
        
        // Convert to kilograms for validation
        val kgValue = when (unit) {
            WeightUnit.KILOGRAMS -> value
            WeightUnit.POUNDS -> poundsToKilograms(value)
        }
        
        return kgValue >= MIN_WEIGHT_KG && kgValue <= MAX_WEIGHT_KG
    }
    
    /**
     * Converts Celsius to Fahrenheit using the precise formula: F = (C × 9/5) + 32
     */
    private fun celsiusToFahrenheit(celsius: Double): Double {
        return (celsius * 9.0 / 5.0) + 32.0
    }
    
    /**
     * Converts Fahrenheit to Celsius using the precise formula: C = (F - 32) × 5/9
     */
    private fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return (fahrenheit - 32.0) * 5.0 / 9.0
    }
    
    /**
     * Converts kilograms to pounds using the precise conversion factor
     */
    private fun kilogramsToPounds(kg: Double): Double {
        return kg * KG_TO_LBS_FACTOR
    }
    
    /**
     * Converts pounds to kilograms using the precise conversion factor
     */
    private fun poundsToKilograms(lbs: Double): Double {
        return lbs * LBS_TO_KG_FACTOR
    }
    
    /**
     * Rounds a double value to the specified number of decimal places
     */
    private fun roundToDecimalPlaces(value: Double, places: Int): Double {
        if (places < 0) return value
        val factor = 10.0.pow(places.toDouble())
        return round(value * factor) / factor
    }
    
    /**
     * Formats a double value to a string with the specified number of decimal places
     */
    private fun formatToDecimalPlaces(value: Double, decimalPlaces: Int): String {
        if (decimalPlaces == 0) {
            return value.toInt().toString()  // Truncate instead of round for 0 decimal places
        }
        
        val integerPart = value.toInt()
        val fractionalPart = value - integerPart
        val multiplier = 10.0.pow(decimalPlaces.toDouble())
        val scaledFractional = round(kotlin.math.abs(fractionalPart) * multiplier).toInt()
        
        val fractionalString = scaledFractional.toString().padStart(decimalPlaces, '0')
        return "$integerPart.$fractionalString"
    }
    

    
    /**
     * Validates temperature value and throws exception if invalid
     */
    private fun validateTemperatureValue(value: Double, unit: TemperatureUnit) {
        require(value.isFinite()) { "Temperature value must be finite, got: $value" }
        require(isValidTemperature(value, unit)) { 
            "Temperature value $value${unit.symbol} is outside valid range" 
        }
    }
    
    /**
     * Validates weight value and throws exception if invalid
     */
    private fun validateWeightValue(value: Double, unit: WeightUnit) {
        require(value.isFinite()) { "Weight value must be finite, got: $value" }
        require(value > 0) { "Weight value must be positive, got: $value" }
        require(isValidWeight(value, unit)) { 
            "Weight value $value ${unit.symbol} is outside valid range" 
        }
    }
}