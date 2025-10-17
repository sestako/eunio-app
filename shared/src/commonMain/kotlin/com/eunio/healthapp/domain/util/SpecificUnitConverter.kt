package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit

/**
 * Interface for converting between different units of measurement
 * and formatting values for display with proper units and decimal places.
 * This interface specifically handles TemperatureUnit and WeightUnit enums.
 */
interface SpecificUnitConverter {
    
    /**
     * Converts temperature from one unit to another
     * 
     * @param value The temperature value to convert
     * @param from The source temperature unit
     * @param to The target temperature unit
     * @return The converted temperature value
     * @throws IllegalArgumentException if the value is invalid
     */
    fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double
    
    /**
     * Converts weight from one unit to another
     * 
     * @param value The weight value to convert
     * @param from The source weight unit
     * @param to The target weight unit
     * @return The converted weight value
     * @throws IllegalArgumentException if the value is invalid
     */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double
    
    /**
     * Formats temperature value for display with appropriate decimal places and unit symbol
     * 
     * @param value The temperature value to format
     * @param unit The temperature unit
     * @param decimalPlaces Number of decimal places to show (default: 1)
     * @return Formatted temperature string (e.g., "36.5°C", "97.7°F")
     */
    fun formatTemperature(value: Double, unit: TemperatureUnit, decimalPlaces: Int = 1): String
    
    /**
     * Formats weight value for display with appropriate decimal places and unit symbol
     * 
     * @param value The weight value to format
     * @param unit The weight unit
     * @param decimalPlaces Number of decimal places to show (default: 1)
     * @return Formatted weight string (e.g., "65.5 kg", "144.3 lbs")
     */
    fun formatWeight(value: Double, unit: WeightUnit, decimalPlaces: Int = 1): String
    
    /**
     * Validates that a temperature value is within reasonable bounds
     * 
     * @param value The temperature value to validate
     * @param unit The temperature unit
     * @return true if the temperature is valid, false otherwise
     */
    fun isValidTemperature(value: Double, unit: TemperatureUnit): Boolean
    
    /**
     * Validates that a weight value is within reasonable bounds
     * 
     * @param value The weight value to validate
     * @param unit The weight unit
     * @return true if the weight is valid, false otherwise
     */
    fun isValidWeight(value: Double, unit: WeightUnit): Boolean
}