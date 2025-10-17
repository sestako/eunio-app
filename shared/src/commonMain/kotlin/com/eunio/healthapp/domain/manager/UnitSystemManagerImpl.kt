package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/**
 * Implementation of UnitSystemManager with reactive state management and caching.
 * Provides efficient unit system preference management with offline-first strategy.
 * Includes comprehensive error handling and recovery mechanisms.
 */
class UnitSystemManagerImpl(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository,
    private val localeDetector: com.eunio.healthapp.domain.util.LocaleDetector,
    private val errorHandler: UnitSystemErrorHandler = UnitSystemErrorHandler()
) : UnitSystemManager {
    
    private val _unitSystemFlow = MutableStateFlow<UnitSystem?>(null)
    private var cachedUnitSystem: UnitSystem? = null
    
    override suspend fun getCurrentUnitSystem(): UnitSystem {
        return try {
            // Return cached value if available
            cachedUnitSystem?.let { return it }
            
            // Fetch from repository with error handling
            val preferencesResult = preferencesRepository.getUserPreferences()
            val unitSystem = if (preferencesResult.isSuccess) {
                preferencesResult.getOrNull()?.unitSystem ?: UnitSystem.METRIC
            } else {
                // Log error and fallback to metric
                UnitSystem.METRIC
            }
            
            // Cache the result
            cachedUnitSystem = unitSystem
            _unitSystemFlow.value = unitSystem
            
            unitSystem
        } catch (e: Exception) {
            // Fallback to metric system on any error
            val fallbackSystem = UnitSystem.METRIC
            cachedUnitSystem = fallbackSystem
            _unitSystemFlow.value = fallbackSystem
            fallbackSystem
        }
    }
    
    override suspend fun setUnitSystem(unitSystem: UnitSystem, isManuallySet: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.error(errorHandler.createValidationError(
                    message = "No authenticated user found",
                    field = "userId"
                ))
            
            val preferences = UserPreferences(
                userId = userId,
                unitSystem = unitSystem,
                isManuallySet = isManuallySet,
                lastModified = Clock.System.now()
            )
            
            // Save preferences with retry mechanism
            val saveResult = savePreferencesWithRetry(preferences)
            if (saveResult.isError) {
                return Result.error(saveResult.errorOrNull() ?: errorHandler.createSyncError(
                    message = "Failed to save unit system preferences",
                    operation = "setUnitSystem",
                    userId = userId
                ))
            }
            
            // Update cache and notify observers
            cachedUnitSystem = unitSystem
            _unitSystemFlow.value = unitSystem
            
            // Update user model (non-blocking)
            updateUserModelSafely(unitSystem)
            
            Result.success(Unit)
        } catch (e: Exception) {
            val unitSystemError = errorHandler.handleUnitSystemError(e, "setUnitSystem")
            Result.error(unitSystemError)
        }
    }
    
    override suspend fun initializeFromLocale(locale: String): UnitSystem {
        return try {
            // Validate locale input
            if (locale.isBlank()) {
                throw errorHandler.createLocaleError(
                    message = "Locale cannot be empty",
                    locale = locale
                )
            }
            
            val prefsResult = preferencesRepository.getUserPreferences()
            if (prefsResult.isError) {
                // If we can't get preferences due to an error, fall back to METRIC
                throw (prefsResult as Result.Error).error
            }
            
            val existingPrefs = prefsResult.getOrNull()
            
            // Only apply locale-based default if user hasn't manually set a preference
            if (existingPrefs?.isManuallySet == true) {
                val unitSystem = existingPrefs.unitSystem
                cachedUnitSystem = unitSystem
                _unitSystemFlow.value = unitSystem
                return unitSystem
            }
            
            // Apply locale-based default with error handling
            val localeBasedSystem = try {
                UnitSystem.fromLocale(locale)
            } catch (e: Exception) {
                throw errorHandler.createLocaleError(
                    message = "Failed to determine unit system from locale: $locale",
                    locale = locale,
                    cause = e
                )
            }
            
            val setResult = setUnitSystem(localeBasedSystem, isManuallySet = false)
            if (setResult.isError) {
                // If setting fails, still return the locale-based system but don't persist it
                cachedUnitSystem = localeBasedSystem
                _unitSystemFlow.value = localeBasedSystem
            }
            
            localeBasedSystem
        } catch (e: Exception) {
            val unitSystemError = when (e) {
                is UnitSystemError -> e
                else -> errorHandler.handleUnitSystemError(e, "initializeFromLocale")
            }
            
            // Fallback to metric if initialization fails
            val fallbackSystem = UnitSystem.METRIC
            cachedUnitSystem = fallbackSystem
            _unitSystemFlow.value = fallbackSystem
            fallbackSystem
        }
    }
    
    override suspend fun initializeFromCurrentLocale(): UnitSystem {
        return try {
            val existingPrefs = preferencesRepository.getUserPreferences().getOrNull()
            
            // Only apply locale-based default if user hasn't manually set a preference
            if (existingPrefs?.isManuallySet == true) {
                val unitSystem = existingPrefs.unitSystem
                cachedUnitSystem = unitSystem
                _unitSystemFlow.value = unitSystem
                return unitSystem
            }
            
            // Detect current locale and apply appropriate unit system with error handling
            val countryCode = try {
                localeDetector.getCurrentLocaleCountryCode()
            } catch (e: Exception) {
                throw errorHandler.createLocaleError(
                    message = "Failed to detect current locale",
                    cause = e
                )
            }
            
            val localeBasedSystem = if (countryCode != null) {
                try {
                    UnitSystem.fromLocale(countryCode)
                } catch (e: Exception) {
                    throw errorHandler.createLocaleError(
                        message = "Failed to determine unit system from detected locale: $countryCode",
                        locale = countryCode,
                        cause = e
                    )
                }
            } else {
                // Fallback to metric if locale detection returns null
                UnitSystem.METRIC
            }
            
            val setResult = setUnitSystem(localeBasedSystem, isManuallySet = false)
            if (setResult.isError) {
                // If setting fails, still return the locale-based system but don't persist it
                cachedUnitSystem = localeBasedSystem
                _unitSystemFlow.value = localeBasedSystem
            }
            
            localeBasedSystem
        } catch (e: Exception) {
            val unitSystemError = when (e) {
                is UnitSystemError -> e
                else -> errorHandler.handleUnitSystemError(e, "initializeFromCurrentLocale")
            }
            
            // Fallback to metric if initialization fails
            val fallbackSystem = UnitSystem.METRIC
            cachedUnitSystem = fallbackSystem
            _unitSystemFlow.value = fallbackSystem
            fallbackSystem
        }
    }
    
    override fun observeUnitSystemChanges(): Flow<UnitSystem> {
        return _unitSystemFlow.asStateFlow()
            .let { flow ->
                // Ensure we emit current value if available
                kotlinx.coroutines.flow.flow {
                    val current = cachedUnitSystem ?: getCurrentUnitSystem()
                    emit(current)
                    flow.collect { value ->
                        value?.let { emit(it) }
                    }
                }
            }
    }
    
    override suspend fun clearCache() {
        cachedUnitSystem = null
        _unitSystemFlow.value = null
    }
    
    private suspend fun getCurrentUserId(): String? {
        return userRepository.getCurrentUser().getOrNull()?.id
    }
    
    /**
     * Saves preferences with retry mechanism for network failures
     */
    private suspend fun savePreferencesWithRetry(
        preferences: UserPreferences,
        maxRetries: Int = 3
    ): Result<Unit> {
        var lastError: Throwable? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = preferencesRepository.saveUserPreferences(preferences)
                if (result.isSuccess) {
                    return result
                }
                
                lastError = result.errorOrNull()
                val error = lastError
                
                // Check if error is retryable
                if (error is UnitSystemError && errorHandler.isRetryable(error)) {
                    val delaySeconds = errorHandler.getRetryDelay(error, attempt + 1)
                    if (delaySeconds > 0 && attempt < maxRetries - 1) {
                        delay(delaySeconds * 1000L)
                    } else {
                        // If not retryable or max retries reached, return the error
                        return result
                    }
                } else {
                    // If not retryable, return the error
                    return result
                }
            } catch (e: Exception) {
                lastError = e
                val unitSystemError = errorHandler.handleUnitSystemError(e, "savePreferences")
                
                if (errorHandler.isRetryable(unitSystemError) && attempt < maxRetries - 1) {
                    val delaySeconds = errorHandler.getRetryDelay(unitSystemError, attempt + 1)
                    if (delaySeconds > 0) {
                        delay(delaySeconds * 1000L)
                    } else {
                        // If not retryable or max retries reached, return error
                        return Result.error(unitSystemError)
                    }
                } else {
                    // If not retryable or max retries reached, return error
                    return Result.error(unitSystemError)
                }
            }
        }
        
        // Should not reach here, but return error if it does
        return Result.error(lastError?.let { 
            errorHandler.handleUnitSystemError(it, "savePreferences") 
        } ?: errorHandler.createSyncError(
            message = "Failed to save preferences after $maxRetries attempts",
            operation = "savePreferences",
            userId = preferences.userId
        ))
    }
    
    /**
     * Updates user model safely without failing the main operation
     */
    private suspend fun updateUserModelSafely(unitSystem: UnitSystem) {
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
            // User model update is secondary to preference update
            val unitSystemError = errorHandler.handleUnitSystemError(e, "updateUserModel")
            // In a real app, this would be logged to analytics/monitoring
        }
    }
    
    /**
     * Recovers from preference operation failures by attempting to restore from cache or defaults
     */
    suspend fun recoverFromFailure(): UnitSystem {
        return try {
            // Try to get from cache first
            cachedUnitSystem?.let { return it }
            
            // Try to initialize from current locale
            initializeFromCurrentLocale()
        } catch (e: Exception) {
            // Ultimate fallback to metric system
            val fallbackSystem = UnitSystem.METRIC
            cachedUnitSystem = fallbackSystem
            _unitSystemFlow.value = fallbackSystem
            fallbackSystem
        }
    }
    
    /**
     * Validates unit system operations and provides recovery suggestions
     */
    fun validateOperation(operation: String, parameters: Map<String, Any?>): Result<Unit> {
        return try {
            when (operation) {
                "setUnitSystem" -> {
                    val unitSystem = parameters["unitSystem"] as? UnitSystem
                        ?: return Result.error(errorHandler.createValidationError(
                            message = "Unit system parameter is required",
                            field = "unitSystem"
                        ))
                    
                    // Validate unit system is supported
                    if (unitSystem !in UnitSystem.values()) {
                        return Result.error(errorHandler.createValidationError(
                            message = "Unsupported unit system: $unitSystem",
                            field = "unitSystem",
                            inputValue = unitSystem
                        ))
                    }
                }
                
                "initializeFromLocale" -> {
                    val locale = parameters["locale"] as? String
                    if (locale.isNullOrBlank()) {
                        return Result.error(errorHandler.createValidationError(
                            message = "Locale parameter is required and cannot be empty",
                            field = "locale",
                            inputValue = locale
                        ))
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleUnitSystemError(e, "validateOperation"))
        }
    }
}