package com.eunio.healthapp.domain.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UnitSystemErrorTest {
    
    @Test
    fun `ConversionError should contain conversion context`() {
        val error = UnitSystemError.ConversionError(
            message = "Invalid conversion",
            inputValue = 100.0,
            fromUnit = "METRIC",
            toUnit = "IMPERIAL"
        )
        
        assertEquals("Invalid conversion", error.message)
        assertEquals(100.0, error.inputValue)
        assertEquals("METRIC", error.fromUnit)
        assertEquals("IMPERIAL", error.toUnit)
    }
    
    @Test
    fun `PreferencesSyncError should contain operation context`() {
        val error = UnitSystemError.PreferencesSyncError(
            message = "Sync failed",
            operation = "savePreferences",
            userId = "user123"
        )
        
        assertEquals("Sync failed", error.message)
        assertEquals("savePreferences", error.operation)
        assertEquals("user123", error.userId)
    }
    
    @Test
    fun `LocaleDetectionError should contain locale context`() {
        val error = UnitSystemError.LocaleDetectionError(
            message = "Invalid locale",
            locale = "invalid_locale"
        )
        
        assertEquals("Invalid locale", error.message)
        assertEquals("invalid_locale", error.locale)
    }
    
    @Test
    fun `UnitValidationError should contain field context`() {
        val error = UnitSystemError.UnitValidationError(
            message = "Invalid input",
            field = "weight",
            inputValue = -10.0
        )
        
        assertEquals("Invalid input", error.message)
        assertEquals("weight", error.field)
        assertEquals(-10.0, error.inputValue)
    }
    
    @Test
    fun `PreferenceOperationError should have retryable flag`() {
        val retryableError = UnitSystemError.PreferenceOperationError(
            message = "Network timeout",
            operation = "save",
            retryable = true
        )
        
        val nonRetryableError = UnitSystemError.PreferenceOperationError(
            message = "Invalid data",
            operation = "validate",
            retryable = false
        )
        
        assertTrue(retryableError.retryable)
        assertTrue(!nonRetryableError.retryable)
    }
    
    @Test
    fun `UnitSystemNetworkError should contain retry information`() {
        val error = UnitSystemError.UnitSystemNetworkError(
            message = "Network error",
            operation = "sync",
            retryAfterSeconds = 30
        )
        
        assertEquals("Network error", error.message)
        assertEquals("sync", error.operation)
        assertEquals(30, error.retryAfterSeconds)
    }
    
    @Test
    fun `all errors should extend AppError`() {
        val errors = listOf(
            UnitSystemError.ConversionError("test"),
            UnitSystemError.PreferencesSyncError("test"),
            UnitSystemError.LocaleDetectionError("test"),
            UnitSystemError.UnitValidationError("test"),
            UnitSystemError.PreferenceOperationError("test", "op"),
            UnitSystemError.UnitSystemNetworkError("test")
        )
        
        errors.forEach { error ->
            assertTrue(error is AppError, "Error ${error::class.simpleName} should extend AppError")
        }
    }
    
    @Test
    fun `errors should preserve cause information`() {
        val cause = RuntimeException("Original cause")
        
        val error = UnitSystemError.ConversionError(
            message = "Conversion failed",
            cause = cause
        )
        
        assertEquals(cause, error.cause)
        assertNotNull(error.cause)
    }
}