package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import kotlin.math.pow
import kotlin.math.round

/**
 * Enhanced unit converter that works with the new settings system.
 * Provides conversion between specific temperature and weight units.
 * This converter works with the enhanced settings system and provides
 * more granular control over unit conversions.
 */
interface EnhancedUnitConverter {
    
    /**
     * Converts temperature between specific temperature units
     */
    fun convertTemperature(value: Double, from: TemperatureUnit, to: TemperatureUnit): Double
    
    /**
     * Converts weight between specific weight units
     */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double
    
    /**
     * Formats temperature with the appropriate unit symbol
     */
    fun formatTemperature(value: Double, unit: TemperatureUnit): String
    
    /**
     * Formats weight with the appropriate unit symbol
     */
    fun formatWeight(value: Double, unit: WeightUnit): String
    
    /**
     * Converts temperature from Celsius (storage format) to user preference
     */
    fun convertTemperatureFromCelsius(celsiusValue: Double, targetUnit: TemperatureUnit): Double
    
    /**
     * Converts temperature to Celsius (storage format) from user input
     */
    fun convertTemperatureToCelsius(value: Double, sourceUnit: TemperatureUnit): Double
    
    /**
     * Converts weight from Kilograms (storage format) to user preference
     */
    fun convertWeightFromKilograms(kgValue: Double, targetUnit: WeightUnit): Double
    
    /**
     * Converts weight to Kilograms (storage format) from user input
     */
    fun convertWeightToKilograms(value: Double, sourceUnit: WeightUnit): Double
}

/**
 * Implementation of EnhancedUnitConverter with precise conversion algorithms
 */
class EnhancedUnitConverterImpl : EnhancedUnitConverter {
    
    companion object {
        private const val KG_TO_LBS_FACTOR = 2.20462262185
        private const val LBS_TO_KG_FACTOR = 0.45359237
    }
    
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
                value * KG_TO_LBS_FACTOR
            from == WeightUnit.POUNDS && to == WeightUnit.KILOGRAMS -> 
                value * LBS_TO_KG_FACTOR
            else -> value
        }.roundToDecimalPlaces(2)
    }
    
    override fun formatTemperature(value: Double, unit: TemperatureUnit): String {
        return "${value.formatForDisplay()}${unit.symbol}"
    }
    
    override fun formatWeight(value: Double, unit: WeightUnit): String {
        return "${value.formatForDisplay()} ${unit.symbol}"
    }
    
    override fun convertTemperatureFromCelsius(celsiusValue: Double, targetUnit: TemperatureUnit): Double {
        return convertTemperature(celsiusValue, TemperatureUnit.CELSIUS, targetUnit)
    }
    
    override fun convertTemperatureToCelsius(value: Double, sourceUnit: TemperatureUnit): Double {
        return convertTemperature(value, sourceUnit, TemperatureUnit.CELSIUS)
    }
    
    override fun convertWeightFromKilograms(kgValue: Double, targetUnit: WeightUnit): Double {
        return convertWeight(kgValue, WeightUnit.KILOGRAMS, targetUnit)
    }
    
    override fun convertWeightToKilograms(value: Double, sourceUnit: WeightUnit): Double {
        return convertWeight(value, sourceUnit, WeightUnit.KILOGRAMS)
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
}