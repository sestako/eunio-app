package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.settings.CyclePreferences
import com.eunio.healthapp.presentation.state.CyclePreferencesUiState
import com.eunio.healthapp.presentation.state.LoadingState
import com.eunio.healthapp.presentation.state.PredictionPreview
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Unit tests for CyclePreferencesViewModel.
 * Tests validation, prediction updates, and state management.
 */
class CyclePreferencesViewModelTest {
    
    @Test
    fun uiState_initialValues_areCorrect() {
        val uiState = CyclePreferencesUiState()
        
        assertEquals(CyclePreferences.default(), uiState.preferences)
        assertEquals(LoadingState.Idle, uiState.loadingState)
        assertFalse(uiState.isUpdating)
        assertEquals(emptyMap(), uiState.validationErrors)
        assertFalse(uiState.showPredictionPreview)
        assertNull(uiState.predictionPreview)
        assertFalse(uiState.isRecalculatingPredictions)
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isEnabled)
        assertNull(uiState.errorMessage)
        assertFalse(uiState.hasPreferences)
        assertTrue(uiState.isValid)
        assertFalse(uiState.hasValidationErrors)
        assertFalse(uiState.hasCustomizations)
    }
    
    @Test
    fun uiState_isLoading_trueWhenLoadingState() {
        val uiState = CyclePreferencesUiState(
            loadingState = LoadingState.Loading
        )
        
        assertTrue(uiState.isLoading)
        assertFalse(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenUpdating() {
        val uiState = CyclePreferencesUiState(
            isUpdating = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_isEnabled_falseWhenRecalculatingPredictions() {
        val uiState = CyclePreferencesUiState(
            isRecalculatingPredictions = true
        )
        
        assertFalse(uiState.isEnabled)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_errorMessage_extractedFromLoadingState() {
        val errorMessage = "Test error"
        val uiState = CyclePreferencesUiState(
            loadingState = LoadingState.Error(errorMessage)
        )
        
        assertEquals(errorMessage, uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun uiState_hasPreferences_trueWhenSuccessful() {
        val preferences = CyclePreferences.default()
        val uiState = CyclePreferencesUiState(
            preferences = preferences,
            loadingState = LoadingState.Success(preferences)
        )
        
        assertTrue(uiState.hasPreferences)
        assertTrue(uiState.isEnabled)
    }
    
    @Test
    fun uiState_isValid_falseWhenValidationErrors() {
        val validationErrors = mapOf("cycleLength" to "Invalid cycle length")
        val uiState = CyclePreferencesUiState(
            validationErrors = validationErrors
        )
        
        assertFalse(uiState.isValid)
        assertTrue(uiState.hasValidationErrors)
    }
    
    @Test
    fun uiState_getValidationError_worksCorrectly() {
        val validationErrors = mapOf(
            "cycleLength" to "Cycle length error",
            "lutealPhaseLength" to "Luteal phase error"
        )
        val uiState = CyclePreferencesUiState(
            validationErrors = validationErrors
        )
        
        assertEquals("Cycle length error", uiState.getValidationError("cycleLength"))
        assertEquals("Luteal phase error", uiState.getValidationError("lutealPhaseLength"))
        assertNull(uiState.getValidationError("periodDuration"))
    }
    
    @Test
    fun uiState_hasValidationError_worksCorrectly() {
        val validationErrors = mapOf("cycleLength" to "Error")
        val uiState = CyclePreferencesUiState(
            validationErrors = validationErrors
        )
        
        assertTrue(uiState.hasValidationError("cycleLength"))
        assertFalse(uiState.hasValidationError("lutealPhaseLength"))
    }
    
    @Test
    fun uiState_hasCustomizations_trueWhenCustomized() {
        val customizedPreferences = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 12,
            periodDuration = 6,
            isCustomized = true
        )
        val uiState = CyclePreferencesUiState(
            preferences = customizedPreferences
        )
        
        assertTrue(uiState.hasCustomizations)
    }
    
    @Test
    fun predictionPreview_properties_workCorrectly() {
        val preview = PredictionPreview(
            nextPeriodDate = LocalDate(2024, 1, 15),
            nextOvulationDate = LocalDate(2024, 1, 1),
            fertileWindowStart = LocalDate(2023, 12, 27),
            fertileWindowEnd = LocalDate(2024, 1, 2),
            cycleDay = 15,
            daysUntilPeriod = 3,
            daysUntilOvulation = 2
        )
        
        assertTrue(preview.isPeriodSoon)
        assertTrue(preview.isOvulationSoon)
        // Note: isInFertileWindow calculation is simplified in the model
    }
    
    @Test
    fun cyclePreferences_default_valuesAreCorrect() {
        val defaultPrefs = CyclePreferences.default()
        
        assertEquals(28, defaultPrefs.averageCycleLength)
        assertEquals(14, defaultPrefs.averageLutealPhaseLength)
        assertEquals(5, defaultPrefs.periodDuration)
        assertFalse(defaultPrefs.isCustomized)
    }
    
    @Test
    fun cyclePreferences_isValid_worksCorrectly() {
        val validPrefs = CyclePreferences.default()
        assertTrue(validPrefs.isValid())
        
        val invalidCycleLength = CyclePreferences(
            averageCycleLength = 50, // Too long
            averageLutealPhaseLength = 14,
            periodDuration = 5
        )
        assertFalse(invalidCycleLength.isValid())
        
        val invalidLutealPhase = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 30, // Longer than cycle
            periodDuration = 5
        )
        assertFalse(invalidLutealPhase.isValid())
        
        val invalidPeriodDuration = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14,
            periodDuration = 15 // Too long
        )
        assertFalse(invalidPeriodDuration.isValid())
    }
    
    @Test
    fun cyclePreferences_getValidationErrors_worksCorrectly() {
        val invalidPrefs = CyclePreferences(
            averageCycleLength = 50, // Too long
            averageLutealPhaseLength = 30, // Too long and longer than cycle
            periodDuration = 15 // Too long
        )
        
        val errors = invalidPrefs.getValidationErrors()
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Cycle length") })
        assertTrue(errors.any { it.contains("Luteal phase") })
        assertTrue(errors.any { it.contains("Period duration") })
    }
    
    @Test
    fun cyclePreferences_follicularPhaseLength_calculatedCorrectly() {
        val prefs = CyclePreferences(
            averageCycleLength = 28,
            averageLutealPhaseLength = 14
        )
        
        assertEquals(14, prefs.follicularPhaseLength)
        
        val customPrefs = CyclePreferences(
            averageCycleLength = 30,
            averageLutealPhaseLength = 12
        )
        
        assertEquals(18, customPrefs.follicularPhaseLength)
    }
    
    @Test
    fun cyclePreferences_create_worksCorrectly() {
        // Test valid creation
        val validPrefs = CyclePreferences.create(
            cycleLength = 30,
            lutealPhase = 12,
            periodDuration = 6
        )
        
        assertEquals(30, validPrefs.averageCycleLength)
        assertEquals(12, validPrefs.averageLutealPhaseLength)
        assertEquals(6, validPrefs.periodDuration)
        assertTrue(validPrefs.isCustomized)
        
        // Test invalid creation returns default
        val invalidPrefs = CyclePreferences.create(
            cycleLength = 50, // Too long
            lutealPhase = 12,
            periodDuration = 6
        )
        
        assertEquals(CyclePreferences.default(), invalidPrefs)
    }
    
    @Test
    fun cyclePreferences_constants_areCorrect() {
        assertEquals(21, CyclePreferences.MIN_CYCLE_LENGTH)
        assertEquals(45, CyclePreferences.MAX_CYCLE_LENGTH)
        assertEquals(10, CyclePreferences.MIN_LUTEAL_PHASE)
        assertEquals(16, CyclePreferences.MAX_LUTEAL_PHASE)
        assertEquals(2, CyclePreferences.MIN_PERIOD_DURATION)
        assertEquals(8, CyclePreferences.MAX_PERIOD_DURATION)
    }
    
    @Test
    fun cyclePreferences_edgeCases_workCorrectly() {
        // Test minimum valid values
        val minValidPrefs = CyclePreferences(
            averageCycleLength = CyclePreferences.MIN_CYCLE_LENGTH,
            averageLutealPhaseLength = CyclePreferences.MIN_LUTEAL_PHASE,
            periodDuration = CyclePreferences.MIN_PERIOD_DURATION
        )
        assertTrue(minValidPrefs.isValid())
        
        // Test maximum valid values
        val maxValidPrefs = CyclePreferences(
            averageCycleLength = CyclePreferences.MAX_CYCLE_LENGTH,
            averageLutealPhaseLength = CyclePreferences.MAX_LUTEAL_PHASE,
            periodDuration = CyclePreferences.MAX_PERIOD_DURATION
        )
        assertTrue(maxValidPrefs.isValid())
        
        // Test boundary conditions
        val boundaryPrefs = CyclePreferences(
            averageCycleLength = 21,
            averageLutealPhaseLength = 20, // Equal to cycle length - should be invalid
            periodDuration = 5
        )
        assertFalse(boundaryPrefs.isValid())
    }
}