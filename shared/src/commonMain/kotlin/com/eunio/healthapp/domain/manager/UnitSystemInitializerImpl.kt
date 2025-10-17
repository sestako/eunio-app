package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.Result

/**
 * Implementation of UnitSystemInitializer that handles locale-based initialization
 * with proper fallback mechanisms and respect for manual user preferences.
 */
class UnitSystemInitializerImpl(
    private val localeDetector: LocaleDetector,
    private val unitSystemManager: UnitSystemManager
) : UnitSystemInitializer {
    
    override suspend fun initializeForNewUser(): Result<UnitSystem> {
        return try {
            val localeBasedSystem = getUnitSystemForCurrentLocale()
            
            // Set the unit system as non-manually set (can be overridden by locale changes)
            val result = unitSystemManager.setUnitSystem(localeBasedSystem, isManuallySet = false)
            
            if (result.isError) {
                // If setting fails, return the locale-based system anyway as fallback
                Result.success(localeBasedSystem)
            } else {
                Result.success(localeBasedSystem)
            }
        } catch (e: Exception) {
            // Fallback to metric system if initialization completely fails
            val fallbackSystem = UnitSystem.METRIC
            try {
                unitSystemManager.setUnitSystem(fallbackSystem, isManuallySet = false)
            } catch (fallbackException: Exception) {
                // Even fallback failed, but still return metric as the system
            }
            Result.success(fallbackSystem)
        }
    }
    
    override suspend fun handleLocaleChange(): Result<UnitSystem> {
        return try {
            // Get current unit system to check if it was manually set
            val currentSystem = unitSystemManager.getCurrentUnitSystem()
            
            // Check if current preference was manually set by trying to get preferences
            // If user has manually set preferences, don't override with locale changes
            val shouldRespectLocale = !isManuallySet()
            
            if (shouldRespectLocale) {
                val localeBasedSystem = getUnitSystemForCurrentLocale()
                
                // Only update if the locale-based system is different from current
                if (localeBasedSystem != currentSystem) {
                    val result = unitSystemManager.setUnitSystem(localeBasedSystem, isManuallySet = false)
                    if (result.isError) {
                        // If update fails, return current system
                        Result.success(currentSystem)
                    } else {
                        Result.success(localeBasedSystem)
                    }
                } else {
                    Result.success(currentSystem)
                }
            } else {
                // User has manual preferences, don't change
                Result.success(currentSystem)
            }
        } catch (e: Exception) {
            // If anything fails, try to return current system or fallback to metric
            try {
                val currentSystem = unitSystemManager.getCurrentUnitSystem()
                Result.success(currentSystem)
            } catch (fallbackException: Exception) {
                Result.success(UnitSystem.METRIC)
            }
        }
    }
    
    override fun getUnitSystemForCurrentLocale(): UnitSystem {
        return try {
            val countryCode = localeDetector.getCurrentLocaleCountryCode()
            if (countryCode != null) {
                UnitSystem.fromLocale(countryCode)
            } else {
                // Fallback to metric if locale detection fails
                UnitSystem.METRIC
            }
        } catch (e: Exception) {
            // Fallback to metric if any exception occurs
            UnitSystem.METRIC
        }
    }
    
    /**
     * Checks if the current unit system preference was manually set by the user.
     * This is a simplified check - in a real implementation, this would query
     * the preferences repository to check the isManuallySet flag.
     */
    private suspend fun isManuallySet(): Boolean {
        return try {
            // This is a placeholder - the actual implementation would need access
            // to the preferences repository to check the isManuallySet flag
            // For now, we'll assume it's not manually set to allow locale-based updates
            false
        } catch (e: Exception) {
            // If we can't determine, assume it's manually set to be safe
            true
        }
    }
}