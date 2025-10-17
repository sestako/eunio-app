package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.settings.UnitPreferences
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Settings-aware unit system manager that bridges the old UnitSystem enum
 * with the new enhanced settings system. This provides backward compatibility
 * while enabling the enhanced unit preferences.
 */
interface SettingsAwareUnitSystemManager {
    
    /**
     * Gets current unit preferences from settings
     */
    suspend fun getCurrentUnitPreferences(): UnitPreferences
    
    /**
     * Gets current unit system (for backward compatibility)
     */
    suspend fun getCurrentUnitSystem(): UnitSystem
    
    /**
     * Updates unit preferences through settings manager
     */
    suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit>
    
    /**
     * Sets unit system (for backward compatibility)
     */
    suspend fun setUnitSystem(unitSystem: UnitSystem): Result<Unit>
    
    /**
     * Observes unit preferences changes
     */
    fun observeUnitPreferences(): Flow<UnitPreferences>
    
    /**
     * Observes unit system changes (for backward compatibility)
     */
    fun observeUnitSystemChanges(): Flow<UnitSystem>
    
    /**
     * Initializes unit preferences from locale
     */
    suspend fun initializeFromLocale(locale: String): UnitPreferences
}

/**
 * Implementation that integrates with the enhanced settings system
 */
class SettingsAwareUnitSystemManagerImpl(
    private val settingsManager: SettingsManager
) : SettingsAwareUnitSystemManager {
    
    override suspend fun getCurrentUnitPreferences(): UnitPreferences {
        return settingsManager.getUserSettings()
            .getOrNull()?.unitPreferences 
            ?: UnitPreferences.default()
    }
    
    override suspend fun getCurrentUnitSystem(): UnitSystem {
        val preferences = getCurrentUnitPreferences()
        return convertToUnitSystem(preferences)
    }
    
    override suspend fun updateUnitPreferences(preferences: UnitPreferences): Result<Unit> {
        return settingsManager.updateUnitPreferences(preferences)
    }
    
    override suspend fun setUnitSystem(unitSystem: UnitSystem): Result<Unit> {
        val preferences = convertFromUnitSystem(unitSystem)
        return settingsManager.updateUnitPreferences(preferences)
    }
    
    override fun observeUnitPreferences(): Flow<UnitPreferences> {
        return flow {
            // Emit default first to ensure immediate value
            emit(UnitPreferences.default())
            // Then emit from settings manager
            settingsManager.observeSettingsChanges()
                .map { it.unitPreferences }
                .collect { emit(it) }
        }.catch { 
            emit(UnitPreferences.default()) 
        }
    }
    
    override fun observeUnitSystemChanges(): Flow<UnitSystem> {
        return observeUnitPreferences()
            .map { convertToUnitSystem(it) }
    }
    
    override suspend fun initializeFromLocale(locale: String): UnitPreferences {
        val preferences = UnitPreferences.fromLocale(locale)
        updateUnitPreferences(preferences)
        return preferences
    }
    
    /**
     * Converts UnitPreferences to legacy UnitSystem for backward compatibility
     */
    private fun convertToUnitSystem(preferences: UnitPreferences): UnitSystem {
        // Use temperature unit as the primary indicator since it's more distinctive
        return when (preferences.temperatureUnit) {
            TemperatureUnit.CELSIUS -> UnitSystem.METRIC
            TemperatureUnit.FAHRENHEIT -> UnitSystem.IMPERIAL
        }
    }
    
    /**
     * Converts legacy UnitSystem to UnitPreferences
     */
    private fun convertFromUnitSystem(unitSystem: UnitSystem): UnitPreferences {
        return when (unitSystem) {
            UnitSystem.METRIC -> UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
            UnitSystem.IMPERIAL -> UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        }
    }
}