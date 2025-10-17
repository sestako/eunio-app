package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.Result

/**
 * Service responsible for initializing unit system preferences for new users
 * based on device locale while respecting manual user preferences.
 */
interface UnitSystemInitializer {
    /**
     * Initializes unit system for a new user based on device locale.
     * 
     * @return Result containing the initialized UnitSystem or error
     */
    suspend fun initializeForNewUser(): Result<UnitSystem>
    
    /**
     * Checks if unit system should be updated based on locale change.
     * Only updates if user hasn't manually set a preference.
     * 
     * @return Result containing the unit system (updated or existing) or error
     */
    suspend fun handleLocaleChange(): Result<UnitSystem>
    
    /**
     * Gets the appropriate unit system for the current device locale.
     * 
     * @return UnitSystem based on locale or METRIC as fallback
     */
    fun getUnitSystemForCurrentLocale(): UnitSystem
}