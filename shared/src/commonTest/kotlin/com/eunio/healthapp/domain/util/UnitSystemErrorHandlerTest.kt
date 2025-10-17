package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.error.UnitSystemError
import com.eunio.healthapp.domain.model.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnitSystemErrorHandlerTest {
    
    private val errorHandler = UnitSystemErrorHandler()
    
    @Test
    fun `handleUnitSystemError should map NumberFormatException to ConversionError`() {
        val exception = NumberFormatException("Invalid number")
        val result = errorHandler.handleUnitSystemError(exception)
        
        assertTrue(result is UnitSystemError.ConversionError)
        assertTrue(result.message.contains("Invalid number format"))
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleUnitSystemError should map IllegalArgumentException to UnitValidationError`() {
        val exception = IllegalArgumentException("Invalid unit parameter")
        val result = errorHandler.handleUnitSystemError(exception)
        
        assertTrue(result is UnitSystemError.UnitValidationError)
        assertTrue(result.message.contains("Invalid unit system parameter"))
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleUnitSystemError should map IllegalStateException with locale to LocaleDetectionError`() {
        val exception = IllegalStateException("Failed to detect locale")
        val result = errorHandler.handleUnitSystemError(exception)
        
        assertTrue(result is UnitSystemError.LocaleDetectionError)
        assertTrue(result.message.contains("Failed to detect locale"))
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `handleUnitSystemError should map network errors to UnitSystemNetworkError`() {
        val exception = RuntimeException("Network connection failed")
        val result = errorHandler.handleUnitSystemError(exception)
        
        assertTrue(result is UnitSystemError.UnitSystemNetworkError)
        assertTrue(result.message.contains("Network error"))
        assertEquals(exception, result.cause)
    }
    
    @Test
    fun `createConversionError should include all context`() {
        val error = errorHandler.createConversionError(
            message = "Conversion failed",
            inputValue = 100.0,
            fromUnit = UnitSystem.METRIC,
            toUnit = UnitSystem.IMPERIAL
        )
        
        assertEquals("Conversion failed", error.message)
        assertEquals(100.0, error.inputValue)
        assertEquals("METRIC", error.fromUnit)
        assertEquals("IMPERIAL", error.toUnit)
    }
    
    @Test
    fun `createValidationError should include field context`() {
        val error = errorHandler.createValidationError(
            message = "Invalid weight",
            field = "weight",
            inputValue = -10.0
        )
        
        assertEquals("Invalid weight", error.message)
        assertEquals("weight", error.field)
        assertEquals(-10.0, error.inputValue)
    }
    
    @Test
    fun `createSyncError should include operation context`() {
        val error = errorHandler.createSyncError(
            message = "Sync failed",
            operation = "savePreferences",
            userId = "user123"
        )
        
        assertEquals("Sync failed", error.message)
        assertEquals("savePreferences", error.operation)
        assertEquals("user123", error.userId)
    }
    
    @Test
    fun `createLocaleError should include locale context`() {
        val error = errorHandler.createLocaleError(
            message = "Invalid locale",
            locale = "en_US"
        )
        
        assertEquals("Invalid locale", error.message)
        assertEquals("en_US", error.locale)
    }
    
    @Test
    fun `createNetworkError should include retry information`() {
        val error = errorHandler.createNetworkError(
            message = "Network timeout",
            operation = "sync",
            retryAfterSeconds = 30
        )
        
        assertEquals("Network timeout", error.message)
        assertEquals("sync", error.operation)
        assertEquals(30, error.retryAfterSeconds)
    }
    
    @Test
    fun `isRetryable should return correct values for different error types`() {
        val networkError = UnitSystemError.UnitSystemNetworkError("Network error")
        val syncError = UnitSystemError.PreferencesSyncError("Sync error")
        val retryableOpError = UnitSystemError.PreferenceOperationError("Op error", "op", retryable = true)
        val nonRetryableOpError = UnitSystemError.PreferenceOperationError("Op error", "op", retryable = false)
        val conversionError = UnitSystemError.ConversionError("Conversion error")
        val validationError = UnitSystemError.UnitValidationError("Validation error")
        val localeError = UnitSystemError.LocaleDetectionError("Locale error")
        
        assertTrue(errorHandler.isRetryable(networkError))
        assertTrue(errorHandler.isRetryable(syncError))
        assertTrue(errorHandler.isRetryable(retryableOpError))
        assertFalse(errorHandler.isRetryable(nonRetryableOpError))
        assertFalse(errorHandler.isRetryable(conversionError))
        assertFalse(errorHandler.isRetryable(validationError))
        assertFalse(errorHandler.isRetryable(localeError))
    }
    
    @Test
    fun `getRetryDelay should return appropriate delays`() {
        val networkError = UnitSystemError.UnitSystemNetworkError("Network error", retryAfterSeconds = 10)
        val syncError = UnitSystemError.PreferencesSyncError("Sync error")
        val conversionError = UnitSystemError.ConversionError("Conversion error")
        
        assertEquals(10, errorHandler.getRetryDelay(networkError, 1))
        assertTrue(errorHandler.getRetryDelay(syncError, 1) > 0)
        assertEquals(0, errorHandler.getRetryDelay(conversionError, 1))
    }
    
    @Test
    fun `getRetryDelay should implement exponential backoff`() {
        val syncError = UnitSystemError.PreferencesSyncError("Sync error")
        
        val delay1 = errorHandler.getRetryDelay(syncError, 1)
        val delay2 = errorHandler.getRetryDelay(syncError, 2)
        val delay3 = errorHandler.getRetryDelay(syncError, 3)
        
        // Should increase with attempt number (allowing for jitter)
        assertTrue(delay2 >= delay1)
        assertTrue(delay3 >= delay2)
        
        // Should not exceed maximum
        assertTrue(delay3 <= 60)
    }
    
    @Test
    fun `toAppError should convert UnitSystemError to AppError`() {
        val conversionError = UnitSystemError.ConversionError("Conversion failed")
        val validationError = UnitSystemError.UnitValidationError("Validation failed")
        val syncError = UnitSystemError.PreferencesSyncError("Sync failed")
        val networkError = UnitSystemError.UnitSystemNetworkError("Network failed")
        val localeError = UnitSystemError.LocaleDetectionError("Locale failed")
        
        assertTrue(errorHandler.toAppError(conversionError) is AppError.ValidationError)
        assertTrue(errorHandler.toAppError(validationError) is AppError.ValidationError)
        assertTrue(errorHandler.toAppError(syncError) is AppError.DataSyncError)
        assertTrue(errorHandler.toAppError(networkError) is AppError.NetworkError)
        assertTrue(errorHandler.toAppError(localeError) is AppError.UnknownError)
    }
}