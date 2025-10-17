package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import kotlin.math.pow
import kotlin.math.round

/**
 * Interface for converting between specific temperature and weight units.
 * Works with individual unit enums rather than unit systems.
 */
interface UnitPreferencesConverter {
    
    /**
     * Converts temperature from one unit to another
     * 
     * @param value The temperature value to convert
     * @param from The source temperature unit
     * @param to The target temperature unit
     * @return The converted temperature value
     */
    fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double
    
    /**
     * Converts weight from one unit to another
     * 
     * @param value The weight value to convert
     * @param from The source weight unit
     * @param to The target weight unit
     * @return The converted weight value
     */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double
    
    /**
     * Formats temperature value for display with appropriate unit
     * 
     * @param value The temperature value to format
     * @param unit The temperature unit to use for formatting
     * @return Formatted temperature string
     */
    fun formatTemperature(value: Double, unit: TemperatureUnit): String
    
    /**
     * Formats weight value for display with appropriate unit
     * 
     * @param value The weight value to format
     * @param unit The weight unit to use for formatting
     * @return Formatted weight string
     */
    fun formatWeight(value: Double, unit: WeightUnit): String
}

/**
 * Implementation of UnitPreferencesConverter with precise conversion algorithms.
 */
class UnitPreferencesConverterImpl : UnitPreferencesConverter {
    
    override fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double {
        if (from == to) return value
        
        return when {
            from == TemperatureUnit.CELSIUS && to == TemperatureUnit.FAHRENHEIT -> 
                (value * 9.0 / 5.0) + 32.0
            from == TemperatureUnit.FAHRENHEIT && to == TemperatureUnit.CELSIUS -> 
                (value - 32.0) * 5.0 / 9.0
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double {
        if (from == to) return value
        
        return when {
            from == WeightUnit.KILOGRAMS && to == WeightUnit.POUNDS -> 
                value * 2.20462
            from == WeightUnit.POUNDS && to == WeightUnit.KILOGRAMS -> 
                value / 2.20462
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun formatTemperature(value: Double, unit: TemperatureUnit): String {
        return "${value.roundToDecimalPlaces(1)}${unit.symbol}"
    }
    
    override fun formatWeight(value: Double, unit: WeightUnit): String {
        return "${value.roundToDecimalPlaces(1)} ${unit.symbol}"
    }
    
    private fun Double.roundToDecimalPlaces(places: Int): Double {
        val factor = 10.0.pow(places.toDouble())
        return round(this * factor) / factor
    }
}