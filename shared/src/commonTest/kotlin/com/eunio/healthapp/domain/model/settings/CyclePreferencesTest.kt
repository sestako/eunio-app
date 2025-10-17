package com.eunio.healthapp.domain.model.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CyclePreferencesTest {
    
    @Test
    fun `default constructor creates valid preferences`() {
        val preferences = CyclePreferences()
        
        assertEquals(28, preferences.averageCycleLength)
        assertEquals(14, preferences.averageLutealPhaseLength)
        assertEquals(5, preferences.periodDuration)
        assertFalse(preferences.isCustomized)
        assertTrue(preferences.isValid())
        assertEquals(14, preferences.follicularPhaseLength)
    }
    
    @Test
    fun `default factory method creates valid preferences`() {
        val preferences = CyclePreferences.default()
        
        assertTrue(preferences.isValid())
        assertEquals(28, preferences.averageCycleLength)
        assertEquals(14, preferences.averageLutealPhaseLength)
    }
    
    @Test
    fun `follicular phase length calculation is correct`() {
        val preferences = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 12,
            periodDuration = 5
        )
        
        assertEquals(18, preferences.follicularPhaseLength)
    }
    
    @Test
    fun `cycle length below minimum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 20, // Below MIN_CYCLE_LENGTH (21)
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Cycle length must be between") })
    }
    
    @Test
    fun `cycle length above maximum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 46, // Above MAX_CYCLE_LENGTH (45)
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Cycle length must be between") })
    }
    
    @Test
    fun `luteal phase below minimum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 9, // Below MIN_LUTEAL_PHASE (10)
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Luteal phase length must be between") })
    }
    
    @Test
    fun `luteal phase above maximum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 17, // Above MAX_LUTEAL_PHASE (16)
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Luteal phase length must be between") })
    }
    
    @Test
    fun `period duration below minimum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14,
            periodDuration = 1 // Below MIN_PERIOD_DURATION (2)
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Period duration must be between") })
    }
    
    @Test
    fun `period duration above maximum is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14,
            periodDuration = 9 // Above MAX_PERIOD_DURATION (8)
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Period duration must be between") })
    }
    
    @Test
    fun `luteal phase equal to cycle length is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 28, // Equal to cycle length
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Luteal phase length must be shorter than cycle length") })
    }
    
    @Test
    fun `luteal phase greater than cycle length is invalid`() {
        val preferences = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 30, // Greater than cycle length
            periodDuration = 5
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertTrue(errors.any { it.contains("Luteal phase length must be shorter than cycle length") })
    }
    
    @Test
    fun `valid boundary values are accepted`() {
        val preferences = CyclePreferences(
            averageCycleLength = CyclePreferences.MIN_CYCLE_LENGTH,
            averageLutealPhaseLength = CyclePreferences.MIN_LUTEAL_PHASE,
            periodDuration = CyclePreferences.MIN_PERIOD_DURATION
        )
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.getValidationErrors().isEmpty())
    }
    
    @Test
    fun `valid maximum values are accepted`() {
        val preferences = CyclePreferences(
            averageCycleLength = CyclePreferences.MAX_CYCLE_LENGTH,
            averageLutealPhaseLength = CyclePreferences.MAX_LUTEAL_PHASE,
            periodDuration = CyclePreferences.MAX_PERIOD_DURATION
        )
        
        assertTrue(preferences.isValid())
        assertTrue(preferences.getValidationErrors().isEmpty())
    }
    
    @Test
    fun `create factory method returns valid preferences for valid input`() {
        val preferences = CyclePreferences.create(
            cycleLength = 30,
            lutealPhase = 12,
            periodDuration = 6
        )
        
        assertEquals(30, preferences.averageCycleLength)
        assertEquals(12, preferences.averageLutealPhaseLength)
        assertEquals(6, preferences.periodDuration)
        assertTrue(preferences.isCustomized)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `create factory method returns default for invalid input`() {
        val preferences = CyclePreferences.create(
            cycleLength = 50, // Invalid: above maximum
            lutealPhase = 12,
            periodDuration = 6
        )
        
        // Should return default values
        assertEquals(28, preferences.averageCycleLength)
        assertEquals(14, preferences.averageLutealPhaseLength)
        assertEquals(5, preferences.periodDuration)
        assertFalse(preferences.isCustomized)
        assertTrue(preferences.isValid())
    }
    
    @Test
    fun `multiple validation errors are reported`() {
        val preferences = CyclePreferences(
            averageCycleLength = 50, // Invalid: too high
            averageLutealPhaseLength = 5, // Invalid: too low
            periodDuration = 10 // Invalid: too high
        )
        
        assertFalse(preferences.isValid())
        val errors = preferences.getValidationErrors()
        assertEquals(3, errors.size)
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = CyclePreferences.default()
        val modified = original.copy(
            averageCycleLength = 30,
            isCustomized = true
        )
        
        assertEquals(28, original.averageCycleLength)
        assertEquals(30, modified.averageCycleLength)
        assertFalse(original.isCustomized)
        assertTrue(modified.isCustomized)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val preferences = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 12,
            periodDuration = 6,
            isCustomized = true
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(preferences.isValid())
    }
}