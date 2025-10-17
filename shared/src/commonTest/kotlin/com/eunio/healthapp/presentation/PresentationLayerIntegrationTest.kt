package com.eunio.healthapp.presentation

import com.eunio.healthapp.domain.model.UnitSystem
import com.eunio.healthapp.presentation.state.*
import com.eunio.healthapp.presentation.viewmodel.UiState
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests to verify the presentation layer works correctly with existing functionality.
 */
class PresentationLayerIntegrationTest {
    
    @Test
    fun `UnitSystemSettingsUiState should be compatible with UiState interface`() {
        val unitSystemState = UnitSystemSettingsUiState(
            currentUnitSystem = UnitSystem.IMPERIAL,
            isChangingUnitSystem = false,
            loadingState = LoadingState.Idle
        )
        
        // Verify it implements UiState
        assertTrue(unitSystemState is UiState)
        
        // Verify computed properties work correctly
        assertFalse(unitSystemState.isLoading)
        assertTrue(unitSystemState.isEnabled)
        assertNull(unitSystemState.errorMessage)
        
        // Test loading state
        val loadingState = unitSystemState.copy(loadingState = LoadingState.Loading)
        assertTrue(loadingState.isLoading)
        assertFalse(loadingState.isEnabled)
        
        // Test error state
        val errorState = unitSystemState.copy(
            loadingState = LoadingState.Error("Test error")
        )
        assertFalse(errorState.isLoading)
        assertEquals("Test error", errorState.errorMessage)
    }
    
    @Test
    fun `LoadingState should work consistently across all UI states`() {
        // Test with UnitSystemSettingsUiState
        val unitSystemState = UnitSystemSettingsUiState(
            loadingState = LoadingState.Success(UnitSystem.METRIC)
        )
        assertFalse(unitSystemState.isLoading)
        
        // Test with OnboardingUiState (doesn't use LoadingState directly but has isLoading)
        val onboardingState = OnboardingUiState(isLoading = true)
        assertTrue(onboardingState.isLoading)
        
        // Test with DailyLoggingUiState
        val loggingState = DailyLoggingUiState(isLoading = true)
        assertTrue(loggingState.isLoading)
        
        // Test with CalendarUiState
        val calendarState = CalendarUiState(isLoading = true)
        assertTrue(calendarState.isLoading)
        
        // Test with InsightsUiState
        val insightsState = InsightsUiState(isLoading = true)
        assertTrue(insightsState.isLoading)
    }
    
    @Test
    fun `All UI states should implement UiState interface`() {
        val states: List<UiState> = listOf(
            UnitSystemSettingsUiState(),
            OnboardingUiState(),
            DailyLoggingUiState(),
            CalendarUiState(),
            InsightsUiState()
        )
        
        // Verify all states implement UiState
        states.forEach { state ->
            assertTrue(state is UiState, "State ${state::class.simpleName} should implement UiState")
        }
    }
    
    @Test
    fun `Error handling should be consistent across UI states`() {
        // UnitSystemSettingsUiState uses LoadingState.Error
        val unitSystemError = UnitSystemSettingsUiState(
            loadingState = LoadingState.Error("Unit system error")
        )
        assertEquals("Unit system error", unitSystemError.errorMessage)
        
        // Other states use direct errorMessage property
        val onboardingError = OnboardingUiState(errorMessage = "Onboarding error")
        assertEquals("Onboarding error", onboardingError.errorMessage)
        
        val loggingError = DailyLoggingUiState(errorMessage = "Logging error")
        assertEquals("Logging error", loggingError.errorMessage)
        
        val calendarError = CalendarUiState(errorMessage = "Calendar error")
        assertEquals("Calendar error", calendarError.errorMessage)
        
        val insightsError = InsightsUiState(errorMessage = "Insights error")
        assertEquals("Insights error", insightsError.errorMessage)
    }
    
    @Test
    fun `LoadingState sealed class should handle all cases correctly`() {
        val idleState = LoadingState.Idle
        val loadingState = LoadingState.Loading
        val successState = LoadingState.Success("test data")
        val errorState = LoadingState.Error("test error")
        
        // Verify type checking works
        assertTrue(idleState is LoadingState.Idle)
        assertTrue(loadingState is LoadingState.Loading)
        assertTrue(successState is LoadingState.Success<*>)
        assertTrue(errorState is LoadingState.Error)
        
        // Verify data extraction
        assertEquals("test data", successState.data)
        assertEquals("test error", errorState.message)
    }
    
    @Test
    fun `UnitSystem enum should be accessible from presentation layer`() {
        val metricSystem = UnitSystem.METRIC
        val imperialSystem = UnitSystem.IMPERIAL
        
        assertNotEquals(metricSystem, imperialSystem)
        
        // Verify it can be used in UI states
        val state1 = UnitSystemSettingsUiState(currentUnitSystem = metricSystem)
        val state2 = UnitSystemSettingsUiState(currentUnitSystem = imperialSystem)
        
        assertEquals(metricSystem, state1.currentUnitSystem)
        assertEquals(imperialSystem, state2.currentUnitSystem)
    }
    
    @Test
    fun `Date handling should be consistent across presentation layer`() {
        val testDate = LocalDate(2024, 1, 15)
        
        // DailyLoggingUiState uses LocalDate
        val loggingState = DailyLoggingUiState(selectedDate = testDate)
        assertEquals(testDate, loggingState.selectedDate)
        
        // CalendarUiState uses LocalDate
        val calendarState = CalendarUiState(
            currentMonth = testDate,
            selectedDate = testDate
        )
        assertEquals(testDate, calendarState.currentMonth)
        assertEquals(testDate, calendarState.selectedDate)
        
        // CalendarDay uses LocalDate
        val calendarDay = CalendarDay(
            date = testDate,
            isToday = true,
            isInCurrentMonth = true
        )
        assertEquals(testDate, calendarDay.date)
    }
    
    @Test
    fun `Presentation layer should support cross-platform compilation`() {
        // This test verifies that all presentation layer components
        // use only cross-platform compatible APIs
        
        // Create instances of all main presentation components
        val states = listOf(
            UnitSystemSettingsUiState(),
            OnboardingUiState(),
            DailyLoggingUiState(),
            CalendarUiState(),
            InsightsUiState()
        )
        
        // Verify they can be instantiated (compilation test)
        assertEquals(5, states.size)
        
        // Verify they all implement the common interface
        states.forEach { state ->
            assertTrue(state is UiState)
        }
    }
}