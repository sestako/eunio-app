package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.UnitSystem

/**
 * Interface for converting between different unit systems (Metric/Imperial)
 * and formatting values for display. This interface works with the UnitSystem enum.
 */
interface UnitConverter {
    
    /**
     * Converts weight from one unit system to another
     * 
     * @param value The weight value to convert
     * @param from The source unit system
     * @param to The target unit system
     * @return The converted weight value
     */
    fun convertWeight(value: Double, from: UnitSystem, to: UnitSystem): Double
    
    /**
     * Converts distance from one unit system to another
     * 
     * @param value The distance value to convert
     * @param from The source unit system
     * @param to The target unit system
     * @return The converted distance value
     */
    fun convertDistance(value: Double, from: UnitSystem, to: UnitSystem): Double
    
    /**
     * Converts temperature from one unit system to another
     * 
     * @param value The temperature value to convert
     * @param from The source unit system
     * @param to The target unit system
     * @return The converted temperature value
     */
    fun convertTemperature(value: Double, from: UnitSystem, to: UnitSystem): Double
    
    /**
     * Formats weight value for display with appropriate unit
     * 
     * @param value The weight value to format
     * @param unitSystem The unit system to use for formatting
     * @return Formatted weight string
     */
    fun formatWeight(value: Double, unitSystem: UnitSystem): String
    
    /**
     * Formats distance value for display with appropriate unit
     * 
     * @param value The distance value to format
     * @param unitSystem The unit system to use for formatting
     * @return Formatted distance string
     */
    fun formatDistance(value: Double, unitSystem: UnitSystem): String
    
    /**
     * Formats temperature value for display with appropriate unit
     * 
     * @param value The temperature value to format
     * @param unitSystem The unit system to use for formatting
     * @return Formatted temperature string
     */
    fun formatTemperature(value: Double, unitSystem: UnitSystem): String
}