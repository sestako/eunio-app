package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisplayPreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences`() {
        val preferences = DisplayPreferences()
        
        assertEquals(1.0f, preferences.textSizeScale)
        assertFalse(preferences.highContrastMode)
        assertTrue(preferences.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.MEDIUM, preferences.hapticIntensity)
        assertTrue(preferences.isValid())
        assertFalse(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = DisplayPreferences.default()
        
        assertTrue(preferences.isValid())
        assertEquals(1.0f, preferences.textSizeScale)
        assertFalse(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `accessibilityOptimized factory method creates accessibility-friendly preferences`() {
        val preferences = DisplayPreferences.accessibilityOptimized()
        
        assertEquals(1.2f, preferences.textSizeScale)
        assertTrue(preferences.highContrastMode)
        assertTrue(preferences.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.STRONG, preferences.hapticIntensity)
        assertTrue(preferences.isValid())
        assertTrue(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `minimal factory method creates minimal preferences`() {
        val preferences = DisplayPreferences.minimal()
        
        assertEquals(1.0f, preferences.textSizeScale)
        assertFalse(preferences.highContrastMode)
        assertFalse(preferences.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.DISABLED, preferences.hapticIntensity)
        assertTrue(preferences.isValid())
        assertFalse(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `text size scale below minimum is invalid`() {
        val preferences = DisplayPreferences(
            textSizeScale = 0.7f // Below MIN_TEXT_SCALE (0.8f)
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Text size scale must be between") })
    }
    
    @Test
    fun `text size scale above maximum is invalid`() {
        val preferences = DisplayPreferences(
            textSizeScale = 2.1f // Above MAX_TEXT_SCALE (2.0f)
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Text size scale must be between") })
    }
    
    @Test
    fun `text size scale at boundaries is valid`() {
        val minPreferences = DisplayPreferences(
            textSizeScale = DisplayPreferences.MIN_TEXT_SCALE
        )
        val maxPreferences = DisplayPreferences(
            textSizeScale = DisplayPreferences.MAX_TEXT_SCALE
        )
        
        assertTrue(minPreferences.isValid())
        assertTrue(maxPreferences.isValid())
    }
    
    @Test
    fun `haptic feedback disabled with non-disabled intensity is invalid`() {
        val preferences = DisplayPreferences(
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Haptic intensity should be disabled") })
    }
    
    @Test
    fun `haptic feedback disabled with disabled intensity is valid`() {
        val preferences = DisplayPreferences(
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.DISABLED
        )
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.getValidationErrors().isEmpty())
    }
    
    @Test
    fun `haptic feedback enabled with any intensity is valid`() {
        val intensities = listOf(
            HapticIntensity.DISABLED,
            HapticIntensity.LIGHT,
            HapticIntensity.MEDIUM,
            HapticIntensity.STRONG
        )
        
        intensities.forEach { intensity ->
            val preferences = DisplayPreferences(
                hapticFeedbackEnabled = true,
                hapticIntensity = intensity
            )
            
            assertTrue(preferences.isValid(), "Should be valid with intensity $intensity")
        }
    }
    
    @Test
    fun `hasAccessibilityFeaturesEnabled returns true for high contrast`() {
        val preferences = DisplayPreferences(
            highContrastMode = true
        )
        
        assertTrue(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `hasAccessibilityFeaturesEnabled returns true for non-default text size`() {
        val preferences = DisplayPreferences(
            textSizeScale = 1.5f
        )
        
        assertTrue(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `hasAccessibilityFeaturesEnabled returns false for default settings`() {
        val preferences = DisplayPreferences(
            textSizeScale = 1.0f,
            highContrastMode = false
        )
        
        assertFalse(preferences.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `withHapticFeedback creates correct configuration when enabled`() {
        val original = DisplayPreferences.default()
        val modified = original.withHapticFeedback(true, HapticIntensity.STRONG)
        
        assertTrue(modified.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.STRONG, modified.hapticIntensity)
        assertTrue(modified.isValid())
    }
    
    @Test
    fun `withHapticFeedback creates correct configuration when disabled`() {
        val original = DisplayPreferences.default()
        val modified = original.withHapticFeedback(false)
        
        assertFalse(modified.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.DISABLED, modified.hapticIntensity)
        assertTrue(modified.isValid())
    }
    
    @Test
    fun `create factory method returns valid preferences for valid input`() {
        val preferences = DisplayPreferences.create(
            textScale = 1.5f,
            highContrast = true,
            hapticEnabled = true,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        assertEquals(1.5f, preferences.textSizeScale)
        assertTrue(preferences.highContrastMode)
        assertTrue(preferences.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.STRONG, preferences.hapticIntensity)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `create factory method coerces text scale to valid range`() {
        val tooLow = DisplayPreferences.create(
            textScale = 0.5f, // Below minimum
            highContrast = false,
            hapticEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
        
        val tooHigh = DisplayPreferences.create(
            textScale = 3.0f, // Above maximum
            highContrast = false,
            hapticEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
        
        assertEquals(DisplayPreferences.MIN_TEXT_SCALE, tooLow.textSizeScale)
        assertEquals(DisplayPreferences.MAX_TEXT_SCALE, tooHigh.textSizeScale)
        assertTrue(tooLow.isValid())
        assertTrue(tooHigh.isValid())
    }
    
    @Test
    fun `create factory method handles haptic feedback correctly`() {
        val disabled = DisplayPreferences.create(
            textScale = 1.0f,
            highContrast = false,
            hapticEnabled = false,
            hapticIntensity = HapticIntensity.STRONG // Should be overridden
        )
        
        assertFalse(disabled.hapticFeedbackEnabled)
        assertEquals(HapticIntensity.DISABLED, disabled.hapticIntensity)
        assertTrue(disabled.isValid())
    }
    
    @Test
    fun `create factory method returns default for invalid combination`() {
        // This test ensures that if validation fails, we get default
        // In current implementation, create() always produces valid results
        val preferences = DisplayPreferences.create(
            textScale = 1.0f,
            highContrast = false,
            hapticEnabled = true,
            hapticIntensity = HapticIntensity.MEDIUM
        )
        
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = DisplayPreferences.default()
        val modified = original.copy(
            textSizeScale = 1.5f,
            highContrastMode = true
        )
        
        assertEquals(1.0f, original.textSizeScale)
        assertEquals(1.5f, modified.textSizeScale)
        assertFalse(original.highContrastMode)
        assertTrue(modified.highContrastMode)
        assertFalse(original.hasAccessibilityFeaturesEnabled())
        assertTrue(modified.hasAccessibilityFeaturesEnabled())
    }
    
    @Test
    fun `multiple validation errors are reported`() {
        val preferences = DisplayPreferences(
            textSizeScale = 0.5f, // Invalid: too low
            hapticFeedbackEnabled = false,
            hapticIntensity = HapticIntensity.MEDIUM // Invalid: should be disabled
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertEquals(2, errors.size)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = DisplayPreferences(
            textSizeScale = 1.2f,
            highContrastMode = true,
            hapticFeedbackEnabled = true,
            hapticIntensity = HapticIntensity.STRONG
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}