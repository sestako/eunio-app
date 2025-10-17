package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.createLocaleDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Cross-platform UnitSystemManager that ensures consistent behavior across Android and iOS.
 * Provides platform-agnostic unit system management with consistent data handling.
 */
class CrossPlatformUnitSystemManager(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository,
    private val localeDetector: LocaleDetector = createLocaleDetector()
) : UnitSystemManager {
    
    companion object {
        // Standardized imperial countries across platforms
        private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")
        
        // Default unit system when detection fails
        private val DEFAULT_UNIT_SYSTEM = UnitSystem.METRIC
    }
    
    private val _unitSystemFlow = MutableStateFlow(DEFAULT_UNIT_SYSTEM)
    
    override suspend fun getCurrentUnitSystem(): UnitSystem {
        return try {
            val preferences = preferencesRepository.getUserPreferences().getOrNull()
            val unitSystem = preferences?.unitSystem ?: DEFAULT_UNIT_SYSTEM
            _unitSystemFlow.value = unitSystem
            unitSystem
        } catch (e: Exception) {
            DEFAULT_UNIT_SYSTEM
        }
    }
    
    override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            val preferences = UserPreferences(
                userId = userId,
                unitSystem = unitSystem,
                isManuallySet = isManuallySet,
                lastModified = Clock.System.now()
            )
            
            preferencesRepository.saveUserPreferences(preferences)
                .onSuccess { 
                    _unitSystemFlow.value = unitSystem
                    updateUserModel(unitSystem)
                }
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.ValidationError("Failed to set unit system: ${e.message}"))
        }
    }
    
    override suspend fun initializeFromLocale(locale: String): UnitSystem {
        return try {
            val existingPrefs = preferencesRepository.getUserPreferences().getOrNull()
            
            // Only apply locale-based default if user hasn't manually set a preference
            if (existingPrefs?.isManuallySet == true) {
                return existingPrefs.unitSystem
            }
            
            // Standardized locale-based detection across platforms
            val localeBasedSystem = determineUnitSystemFromLocale(locale)
            setUnitSystem(localeBasedSystem, isManuallySet = false)
            localeBasedSystem
        } catch (e: Exception) {
            DEFAULT_UNIT_SYSTEM
        }
    }
    
    override suspend fun initializeFromCurrentLocale(): UnitSystem {
        return initializeFromPlatformLocale()
    }
    
    override suspend fun clearCache() {
        // Reset the state flow to default
        _unitSystemFlow.value = DEFAULT_UNIT_SYSTEM
    }
    
    override fun observeUnitSystemChanges(): Flow<UnitSystem> = _unitSystemFlow.asStateFlow()
    
    /**
     * Cross-platform method to initialize unit system with enhanced locale detection.
     * Uses platform-specific locale detection while maintaining consistent logic.
     */
    suspend fun initializeFromPlatformLocale(): UnitSystem {
        return try {
            val countryCode = localeDetector.getCurrentLocaleCountryCode()
            val localeString = localeDetector.getCurrentLocaleString()
            
            // Use country code if available, fallback to full locale string
            val locale = countryCode ?: localeString?.substringAfter("_") ?: ""
            
            initializeFromLocale(locale)
        } catch (e: Exception) {
            // Fallback to default and save it
            setUnitSystem(DEFAULT_UNIT_SYSTEM, isManuallySet = false)
            DEFAULT_UNIT_SYSTEM
        }
    }
    
    /**
     * Cross-platform method to validate unit system consistency.
     * Ensures data integrity across platform boundaries.
     */
    suspend fun validateConsistency(): Result<Boolean> {
        return try {
            val preferences = preferencesRepository.getUserPreferences().getOrNull()
            val user = userRepository.getCurrentUser().getOrNull()
            
            if (preferences == null || user == null) {
                return Result.success(true) // No data to validate
            }
            
            // Check if user model and preferences are in sync
            val isConsistent = preferences.unitSystem == user.unitSystem
            
            if (!isConsistent) {
                // Auto-repair: preferences take precedence
                updateUserModel(preferences.unitSystem)
            }
            
            Result.success(isConsistent)
        } catch (e: Exception) {
            Result.error(com.eunio.healthapp.domain.error.AppError.ValidationError("Consistency validation failed: ${e.message}"))
        }
    }
    
    /**
     * Cross-platform method to get detailed unit system information.
     * Provides consistent metadata across platforms.
     */
    suspend fun getUnitSystemInfo(): Map<String, Any> {
        return try {
            val preferences = preferencesRepository.getUserPreferences().getOrNull()
            val currentSystem = getCurrentUnitSystem()
            
            mapOf(
                "currentUnitSystem" to currentSystem.name,
                "isManuallySet" to (preferences?.isManuallySet ?: false),
                "lastModified" to (preferences?.lastModified?.toString() ?: "unknown"),
                "syncStatus" to (preferences?.syncStatus?.name ?: "unknown"),
                "detectedLocale" to (localeDetector.getCurrentLocaleString() ?: "unknown"),
                "detectedCountry" to (localeDetector.getCurrentLocaleCountryCode() ?: "unknown"),
                "platform" to getPlatformName()
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "unknown error"),
                "platform" to getPlatformName()
            )
        }
    }
    
    private suspend fun getCurrentUserId(): String {
        return userRepository.getCurrentUser().getOrNull()?.id 
            ?: throw IllegalStateException("No authenticated user found")
    }
    
    private suspend fun updateUserModel(unitSystem: UnitSystem) {
        try {
            userRepository.getCurrentUser().getOrNull()?.let { user ->
                val updatedUser = user.copy(
                    unitSystem = unitSystem,
                    updatedAt = Clock.System.now()
                )
                userRepository.updateUser(updatedUser)
            }
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }
    
    /**
     * Standardized locale-to-unit-system mapping across platforms.
     */
    private fun determineUnitSystemFromLocale(locale: String): UnitSystem {
        val normalizedLocale = locale.uppercase().trim()
        
        return if (IMPERIAL_COUNTRIES.any { country -> 
            normalizedLocale == country || normalizedLocale.endsWith("_$country") 
        }) {
            UnitSystem.IMPERIAL
        } else {
            UnitSystem.METRIC
        }
    }
    
    /**
     * Platform identification for debugging and analytics.
     */
    private fun getPlatformName(): String {
        return "CrossPlatform" // Simplified for multiplatform compatibility
    }
}