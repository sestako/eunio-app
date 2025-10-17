package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Manager interface for unit system preferences and operations.
 * Provides reactive state management and caching for unit system preferences.
 */
interface UnitSystemManager {
    
    /**
     * Gets the current unit system preference with caching for performance.
     * Returns cached value if available, otherwise fetches from repository.
     * @return The current unit system preference
     */
    suspend fun getCurrentUnitSystem(): UnitSystem
    
    /**
     * Sets the unit system preference with validation and user model updates.
     * Updates both preferences and user model, with immediate local save and background sync.
     * @param unitSystem The unit system to set
     * @param isManuallySet Whether this was manually set by the user (default: true)
     * @return Result indicating success or failure
     */
    suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean = true): Result<Unit>
    
    /**
     * Initializes unit system from device locale with manual preference priority.
     * Only applies locale-based default if user hasn't manually set a preference.
     * @param locale The device locale string (e.g., "US", "GB", "DE")
     * @return The initialized unit system
     */
    suspend fun initializeFromLocale(locale: String): UnitSystem
    
    /**
     * Initializes unit system from current device locale using platform-specific detection.
     * Only applies locale-based default if user hasn't manually set a preference.
     * Provides fallback to metric system if locale detection fails.
     * @return The initialized unit system
     */
    suspend fun initializeFromCurrentLocale(): UnitSystem
    
    /**
     * Observes unit system changes using StateFlow for reactive updates.
     * Emits the current unit system whenever it changes.
     * @return Flow of unit system changes
     */
    fun observeUnitSystemChanges(): Flow<UnitSystem>
    
    /**
     * Clears the cached unit system and forces a refresh from repository.
     * Useful when user signs out or preferences are reset.
     */
    suspend fun clearCache()
}