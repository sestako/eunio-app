package com.eunio.healthapp.domain.manager

import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.model.UserPreferences
import com.eunio.healthapp.domain.repository.PreferencesRepository
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.LocaleDetector
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.UnitSystemErrorHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitSystemManagerErrorHandlingTest {
    
    private val preferencesRepository = mockk<PreferencesRepository>()
    private val userRepository = mockk<UserRepository>()
    private val localeDetector = mockk<LocaleDetector>()
    private val errorHandler = UnitSystemErrorHandler()
    
    private val manager = UnitSystemManagerImpl(
        preferencesRepository = preferencesRepository,
        userRepository = userRepository,
        localeDetector = localeDetector,
        errorHandler = errorHandler
    )
    
    private val testUser = User(
        id = "user123",
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = com.eunio.healthapp.domain.model.HealthGoal.GENERAL_HEALTH,
        unitSystem = UnitSystem.METRIC,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @Test
    fun `getCurrentUnitSystem should handle repository errors gracefully`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } returns Result.error(
            UnitSystemError.PreferencesSyncError("Database error")
        )
        
        // When
        val result = manager.getCurrentUnitSystem()
        
        // Then
        assertEquals(UnitSystem.METRIC, result) // Should fallback to metric
    }
    
    @Test
    fun `setUnitSystem should handle missing user gracefully`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then
        assertTrue(result.isError)
        val error = result.errorOrNull()
        assertTrue(error is UnitSystemError.UnitValidationError)
        assertTrue(error.message.contains("No authenticated user found"))
    }
    
    @Test
    fun `setUnitSystem should retry on network failures`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(testUser)
        coEvery { preferencesRepository.saveUserPreferences(any()) } returnsMany listOf(
            Result.error(UnitSystemError.UnitSystemNetworkError("Network timeout")),
            Result.error(UnitSystemError.UnitSystemNetworkError("Network timeout")),
            Result.success(Unit)
        )
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { preferencesRepository.saveUserPreferences(any()) }
    }
    
    @Test
    fun `setUnitSystem should handle non-retryable errors immediately`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns Result.success(testUser)
        coEvery { preferencesRepository.saveUserPreferences(any()) } returns Result.error(
            UnitSystemError.UnitValidationError("Invalid data")
        )
        
        // When
        val result = manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // Then
        assertTrue(result.isError)
        coVerify(exactly = 1) { preferencesRepository.saveUserPreferences(any()) }
    }
    
    @Test
    fun `initializeFromLocale should handle empty locale`() = runTest {
        // When
        val result = manager.initializeFromLocale("")
        
        // Then
        assertEquals(UnitSystem.METRIC, result) // Should fallback to metric
    }
    
    @Test
    fun `initializeFromLocale should handle invalid locale`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } returns Result.success(null)
        
        // When
        val result = manager.initializeFromLocale("invalid_locale_format")
        
        // Then
        assertEquals(UnitSystem.METRIC, result) // Should fallback to metric even with invalid locale
    }
    
    @Test
    fun `initializeFromLocale should respect existing manual preferences`() = runTest {
        // Given
        val existingPrefs = UserPreferences(
            userId = "user123",
            unitSystem = UnitSystem.IMPERIAL,
            isManuallySet = true,
            lastModified = Clock.System.now()
        )
        coEvery { preferencesRepository.getUserPreferences() } returns Result.success(existingPrefs)
        
        // When
        val result = manager.initializeFromLocale("US")
        
        // Then
        assertEquals(UnitSystem.IMPERIAL, result)
        coVerify(exactly = 0) { preferencesRepository.saveUserPreferences(any()) }
    }
    
    @Test
    fun `initializeFromCurrentLocale should handle locale detection failure`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } returns Result.success(null)
        coEvery { localeDetector.getCurrentLocaleCountryCode() } throws RuntimeException("Locale detection failed")
        
        // When
        val result = manager.initializeFromCurrentLocale()
        
        // Then
        assertEquals(UnitSystem.METRIC, result) // Should fallback to metric
    }
    
    @Test
    fun `initializeFromCurrentLocale should handle null locale gracefully`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } returns Result.success(null)
        coEvery { localeDetector.getCurrentLocaleCountryCode() } returns null
        coEvery { userRepository.getCurrentUser() } returns Result.success(testUser)
        coEvery { preferencesRepository.saveUserPreferences(any()) } returns Result.success(Unit)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = manager.initializeFromCurrentLocale()
        
        // Then
        assertEquals(UnitSystem.METRIC, result) // Should default to metric when locale is null
    }
    
    @Test
    fun `recoverFromFailure should return cached value if available`() = runTest {
        // Given - set up a cached value first
        coEvery { userRepository.getCurrentUser() } returns Result.success(testUser)
        coEvery { preferencesRepository.saveUserPreferences(any()) } returns Result.success(Unit)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        manager.setUnitSystem(UnitSystem.IMPERIAL)
        
        // When
        val result = manager.recoverFromFailure()
        
        // Then
        assertEquals(UnitSystem.IMPERIAL, result)
    }
    
    @Test
    fun `recoverFromFailure should initialize from locale when no cache`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } returns Result.success(null)
        coEvery { localeDetector.getCurrentLocaleCountryCode() } returns "US"
        coEvery { userRepository.getCurrentUser() } returns Result.success(testUser)
        coEvery { preferencesRepository.saveUserPreferences(any()) } returns Result.success(Unit)
        coEvery { userRepository.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = manager.recoverFromFailure()
        
        // Then
        assertEquals(UnitSystem.IMPERIAL, result) // US should default to Imperial
    }
    
    @Test
    fun `recoverFromFailure should fallback to metric on all failures`() = runTest {
        // Given
        coEvery { preferencesRepository.getUserPreferences() } throws RuntimeException("Database error")
        coEvery { localeDetector.getCurrentLocaleCountryCode() } throws RuntimeException("Locale error")
        
        // When
        val result = manager.recoverFromFailure()
        
        // Then
        assertEquals(UnitSystem.METRIC, result)
    }
    
    @Test
    fun `validateOperation should validate setUnitSystem parameters`() {
        // Test missing unit system
        val result1 = manager.validateOperation("setUnitSystem", mapOf())
        assertTrue(result1.isError)
        val error1 = result1.errorOrNull()
        assertTrue(error1 is UnitSystemError.UnitValidationError)
        assertTrue(error1.message.contains("Unit system parameter is required"))
        
        // Test valid unit system
        val result2 = manager.validateOperation("setUnitSystem", mapOf("unitSystem" to UnitSystem.METRIC))
        assertTrue(result2.isSuccess)
    }
    
    @Test
    fun `validateOperation should validate initializeFromLocale parameters`() {
        // Test missing locale
        val result1 = manager.validateOperation("initializeFromLocale", mapOf())
        assertTrue(result1.isError)
        val error1 = result1.errorOrNull()
        assertTrue(error1 is UnitSystemError.UnitValidationError)
        assertTrue(error1.message.contains("Locale parameter is required"))
        
        // Test empty locale
        val result2 = manager.validateOperation("initializeFromLocale", mapOf("locale" to ""))
        assertTrue(result2.isError)
        val error2 = result2.errorOrNull()
        assertTrue(error2 is UnitSystemError.UnitValidationError)
        assertTrue(error2.message.contains("cannot be empty"))
        
        // Test valid locale
        val result3 = manager.validateOperation("initializeFromLocale", mapOf("locale" to "US"))
        assertTrue(result3.isSuccess)
    }
}