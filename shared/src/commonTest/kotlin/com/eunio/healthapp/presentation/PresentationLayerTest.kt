package com.eunio.healthapp.presentation

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.presentation.state.*
import com.eunio.healthapp.presentation.navigation.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlin.test.*

/**
 * Tests for the presentation layer components including UI states and navigation.
 */
class PresentationLayerTest {
    
    @Test
    fun `OnboardingUiState should have correct initial values`() {
        val state = OnboardingUiState()
        
        assertEquals(OnboardingStep.WELCOME, state.currentStep)
        assertNull(state.selectedGoal)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isCompleted)
    }
    
    @Test
    fun `DailyLoggingUiState should validate BBT correctly`() {
        val validState = DailyLoggingUiState(bbt = "98.6")
        assertTrue(validState.isBbtValid)
        
        val invalidHighState = DailyLoggingUiState(bbt = "110.0")
        assertFalse(invalidHighState.isBbtValid)
        
        val invalidLowState = DailyLoggingUiState(bbt = "90.0")
        assertFalse(invalidLowState.isBbtValid)
        
        val emptyState = DailyLoggingUiState(bbt = "")
        assertTrue(emptyState.isBbtValid) // Empty is valid
    }
    
    @Test
    fun `DailyLoggingUiState canSave should work correctly`() {
        val testDate = LocalDate(2024, 1, 15)
        
        val canSaveState = DailyLoggingUiState(
            selectedDate = testDate,
            hasUnsavedChanges = true,
            isSaving = false
        )
        assertTrue(canSaveState.canSave)
        
        val cannotSaveState = DailyLoggingUiState(
            selectedDate = testDate,
            hasUnsavedChanges = false,
            isSaving = false
        )
        assertFalse(cannotSaveState.canSave)
        
        val savingState = DailyLoggingUiState(
            selectedDate = testDate,
            hasUnsavedChanges = true,
            isSaving = true
        )
        assertFalse(savingState.canSave)
    }
    
    @Test
    fun `CalendarUiState should handle calendar day creation`() {
        val testDate = LocalDate(2024, 1, 15)
        val calendarDay = CalendarDay(
            date = testDate,
            isToday = true,
            isInCurrentMonth = true,
            dayType = CalendarDayType.PERIOD_ACTUAL,
            hasLog = true,
            periodFlow = PeriodFlow.MEDIUM
        )
        
        assertEquals(testDate, calendarDay.date)
        assertTrue(calendarDay.isToday)
        assertTrue(calendarDay.isInCurrentMonth)
        assertEquals(CalendarDayType.PERIOD_ACTUAL, calendarDay.dayType)
        assertTrue(calendarDay.hasLog)
        assertEquals(PeriodFlow.MEDIUM, calendarDay.periodFlow)
    }
    
    @Test
    fun `InsightsUiState should calculate properties correctly`() {
        val unreadInsight = createTestInsight("1", false)
        val readInsight = createTestInsight("2", true)
        
        val state = InsightsUiState(
            unreadInsights = listOf(unreadInsight),
            readInsights = listOf(readInsight)
        )
        
        assertEquals(2, state.allInsights.size)
        assertTrue(state.hasInsights)
        assertEquals(1, state.unreadCount)
        
        val emptyState = InsightsUiState()
        assertFalse(emptyState.hasInsights)
        assertEquals(0, emptyState.unreadCount)
    }
    
    @Test
    fun `NavigationDestination should handle different types`() {
        val onboarding = NavigationDestination.Onboarding
        val dailyLogging = NavigationDestination.DailyLogging
        val calendar = NavigationDestination.Calendar
        val insights = NavigationDestination.Insights
        
        assertNotEquals<NavigationDestination>(onboarding, dailyLogging)
        assertNotEquals<NavigationDestination>(calendar, insights)
        
        val loggingWithDate = NavigationDestination.DailyLoggingWithDate(LocalDate(2024, 1, 15))
        assertTrue(loggingWithDate is NavigationDestination.DailyLoggingWithDate)
        assertEquals(LocalDate(2024, 1, 15), loggingWithDate.date)
    }
    
    @Test
    fun `NavigationEvent should handle different actions`() {
        val navigateTo: NavigationEvent = NavigationEvent.NavigateTo(NavigationDestination.Calendar)
        val navigateBack: NavigationEvent = NavigationEvent.NavigateBack
        val navigateWithClear: NavigationEvent = NavigationEvent.NavigateToWithClearStack(NavigationDestination.Insights)
        
        assertTrue(navigateTo is NavigationEvent.NavigateTo)
        assertTrue(navigateBack is NavigationEvent.NavigateBack)
        assertTrue(navigateWithClear is NavigationEvent.NavigateToWithClearStack)
        
        assertEquals(NavigationDestination.Calendar, navigateTo.destination)
        assertEquals(NavigationDestination.Insights, navigateWithClear.destination)
    }
    
    @Test
    fun `CyclePredictions should handle nullable values`() {
        val testDate = LocalDate(2024, 1, 15)
        val predictions = CyclePredictions(
            nextPeriodDate = testDate,
            nextOvulationDate = testDate.plus(DatePeriod(days = 14)),
            fertilityWindow = testDate..testDate.plus(DatePeriod(days = 5))
        )
        
        assertEquals(testDate, predictions.nextPeriodDate)
        assertNotNull(predictions.nextOvulationDate)
        assertNotNull(predictions.fertilityWindow)
        
        val nullPredictions = CyclePredictions(
            nextPeriodDate = null,
            nextOvulationDate = null,
            fertilityWindow = null
        )
        
        assertNull(nullPredictions.nextPeriodDate)
        assertNull(nullPredictions.nextOvulationDate)
        assertNull(nullPredictions.fertilityWindow)
    }
    
    @Test
    fun `CalendarDayType enum should have all expected values`() {
        val types = CalendarDayType.values()
        
        assertTrue(types.contains(CalendarDayType.NORMAL))
        assertTrue(types.contains(CalendarDayType.PERIOD_PREDICTED))
        assertTrue(types.contains(CalendarDayType.PERIOD_ACTUAL))
        assertTrue(types.contains(CalendarDayType.OVULATION_PREDICTED))
        assertTrue(types.contains(CalendarDayType.OVULATION_CONFIRMED))
        assertTrue(types.contains(CalendarDayType.FERTILITY_WINDOW))
    }
    
    @Test
    fun `OnboardingStep enum should have correct progression`() {
        val steps = OnboardingStep.values()
        
        assertEquals(3, steps.size)
        assertTrue(steps.contains(OnboardingStep.WELCOME))
        assertTrue(steps.contains(OnboardingStep.GOAL_SELECTION))
        assertTrue(steps.contains(OnboardingStep.COMPLETION))
    }
    
    private fun createTestInsight(id: String, isRead: Boolean) = Insight(
        id = id,
        userId = "test-user",
        generatedDate = kotlinx.datetime.Clock.System.now(),
        insightText = "Test insight",
        type = InsightType.PATTERN_RECOGNITION,
        isRead = isRead,
        relatedLogIds = emptyList(),
        confidence = 0.8,
        actionable = false
    )
}