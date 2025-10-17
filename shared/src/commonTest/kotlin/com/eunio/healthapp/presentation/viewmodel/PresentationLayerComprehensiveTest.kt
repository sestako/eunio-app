package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.presentation.navigation.*
import com.eunio.healthapp.presentation.state.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.*

/**
 * Comprehensive tests for the presentation layer components.
 * Tests UI states, navigation, and presentation logic without complex ViewModel runtime dependencies.
 */
class PresentationLayerComprehensiveTest {
    
    private val testDate = LocalDate(2024, 1, 15)
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    @Test
    fun `OnboardingUiState should handle all onboarding steps correctly`() {
        // Test initial state
        val initialState = OnboardingUiState()
        assertEquals(OnboardingStep.WELCOME, initialState.currentStep)
        assertNull(initialState.selectedGoal)
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isCompleted)
        assertNull(initialState.errorMessage)
        
        // Test goal selection
        val goalSelectedState = initialState.copy(selectedGoal = HealthGoal.CONCEPTION)
        assertEquals(HealthGoal.CONCEPTION, goalSelectedState.selectedGoal)
        
        // Test loading state
        val loadingState = goalSelectedState.copy(isLoading = true)
        assertTrue(loadingState.isLoading)
        
        // Test completion
        val completedState = loadingState.copy(
            isLoading = false,
            isCompleted = true,
            currentStep = OnboardingStep.COMPLETION
        )
        assertTrue(completedState.isCompleted)
        assertFalse(completedState.isLoading)
        assertEquals(OnboardingStep.COMPLETION, completedState.currentStep)
        
        // Test error handling
        val errorState = initialState.copy(errorMessage = "Test error")
        assertEquals("Test error", errorState.errorMessage)
    }
    
    @Test
    fun `DailyLoggingUiState should validate form data correctly`() {
        // Test initial state
        val initialState = DailyLoggingUiState(selectedDate = testDate)
        assertEquals(testDate, initialState.selectedDate)
        assertFalse(initialState.hasUnsavedChanges)
        assertFalse(initialState.canSave)
        assertTrue(initialState.isBbtValid) // Empty BBT is valid
        
        // Test form changes
        val changedState = initialState.copy(
            periodFlow = PeriodFlow.HEAVY,
            hasUnsavedChanges = true
        )
        assertTrue(changedState.hasUnsavedChanges)
        assertTrue(changedState.canSave)
        
        // Test BBT validation
        val validBbtState = changedState.copy(bbt = "98.6")
        assertTrue(validBbtState.isBbtValid)
        
        val invalidBbtState = changedState.copy(bbt = "110.0")
        assertFalse(invalidBbtState.isBbtValid)
        
        val lowBbtState = changedState.copy(bbt = "90.0")
        assertFalse(lowBbtState.isBbtValid)
        
        // Test saving state
        val savingState = changedState.copy(isSaving = true)
        assertFalse(savingState.canSave) // Cannot save while saving
        
        // Test symptoms selection
        val symptomsState = initialState.copy(
            selectedSymptoms = setOf(Symptom.CRAMPS, Symptom.BLOATING),
            hasUnsavedChanges = true
        )
        assertEquals(2, symptomsState.selectedSymptoms.size)
        assertTrue(symptomsState.selectedSymptoms.contains(Symptom.CRAMPS))
        assertTrue(symptomsState.selectedSymptoms.contains(Symptom.BLOATING))
    }
    
    @Test
    fun `CalendarUiState should handle calendar data correctly`() {
        // Test initial state
        val initialState = CalendarUiState(currentMonth = testDate)
        assertEquals(testDate, initialState.currentMonth)
        assertNull(initialState.selectedDate)
        assertNull(initialState.currentCycle)
        assertTrue(initialState.logsInMonth.isEmpty())
        assertFalse(initialState.isLoading)
        
        // Test with cycle data
        val testCycle = createTestCycle()
        val cycleState = initialState.copy(currentCycle = testCycle)
        assertEquals(testCycle, cycleState.currentCycle)
        
        // Test with logs
        val testLog = createTestDailyLog(testDate)
        val logsMap = mapOf(testDate to testLog)
        val logsState = cycleState.copy(logsInMonth = logsMap)
        assertTrue(logsState.logsInMonth.containsKey(testDate))
        assertEquals(testLog, logsState.logsInMonth[testDate])
        
        // Test with predictions
        val nextPeriodDate = LocalDate(2024, 2, 12)
        val nextOvulationDate = LocalDate(2024, 1, 29)
        val fertilityStart = LocalDate(2024, 1, 25)
        val fertilityEnd = LocalDate(2024, 1, 31)
        
        val predictions = CyclePredictions(
            nextPeriodDate = nextPeriodDate,
            nextOvulationDate = nextOvulationDate,
            fertilityWindow = fertilityStart..fertilityEnd
        )
        val predictionsState = logsState.copy(predictions = predictions)
        assertNotNull(predictionsState.predictions)
        assertEquals(nextPeriodDate, predictionsState.predictions!!.nextPeriodDate)
        
        // Test date selection
        val selectedState = predictionsState.copy(selectedDate = testDate)
        assertEquals(testDate, selectedState.selectedDate)
    }
    
    @Test
    fun `CalendarDay should represent calendar information correctly`() {
        // Test normal day
        val normalDay = CalendarDay(
            date = testDate,
            isToday = false,
            isInCurrentMonth = true,
            dayType = CalendarDayType.NORMAL,
            hasLog = false
        )
        assertEquals(testDate, normalDay.date)
        assertFalse(normalDay.isToday)
        assertTrue(normalDay.isInCurrentMonth)
        assertEquals(CalendarDayType.NORMAL, normalDay.dayType)
        assertFalse(normalDay.hasLog)
        
        // Test today
        val todayDay = normalDay.copy(
            date = today,
            isToday = true,
            hasLog = true,
            dayType = CalendarDayType.PERIOD_ACTUAL,
            periodFlow = PeriodFlow.MEDIUM
        )
        assertTrue(todayDay.isToday)
        assertTrue(todayDay.hasLog)
        assertEquals(CalendarDayType.PERIOD_ACTUAL, todayDay.dayType)
        assertEquals(PeriodFlow.MEDIUM, todayDay.periodFlow)
        
        // Test different day types
        val ovulationDay = normalDay.copy(dayType = CalendarDayType.OVULATION_CONFIRMED)
        assertEquals(CalendarDayType.OVULATION_CONFIRMED, ovulationDay.dayType)
        
        val fertilityDay = normalDay.copy(dayType = CalendarDayType.FERTILITY_WINDOW)
        assertEquals(CalendarDayType.FERTILITY_WINDOW, fertilityDay.dayType)
    }
    
    @Test
    fun `InsightsUiState should manage insights correctly`() {
        // Test initial state
        val initialState = InsightsUiState()
        assertTrue(initialState.unreadInsights.isEmpty())
        assertTrue(initialState.readInsights.isEmpty())
        assertFalse(initialState.isLoading)
        assertFalse(initialState.hasInsights)
        assertEquals(0, initialState.unreadCount)
        assertTrue(initialState.allInsights.isEmpty())
        
        // Test with insights
        val unreadInsights = listOf(
            createTestInsight("1", false, InsightType.PATTERN_RECOGNITION),
            createTestInsight("2", false, InsightType.EARLY_WARNING)
        )
        val readInsights = listOf(
            createTestInsight("3", true, InsightType.CYCLE_PREDICTION)
        )
        
        val populatedState = initialState.copy(
            unreadInsights = unreadInsights,
            readInsights = readInsights
        )
        
        assertEquals(2, populatedState.unreadInsights.size)
        assertEquals(1, populatedState.readInsights.size)
        assertTrue(populatedState.hasInsights)
        assertEquals(2, populatedState.unreadCount)
        assertEquals(3, populatedState.allInsights.size)
        
        // Test dismissing insights
        val dismissedState = populatedState.copy(
            dismissedInsightIds = setOf("1")
        )
        assertTrue(dismissedState.dismissedInsightIds.contains("1"))
        
        // Test loading states
        val loadingState = populatedState.copy(isLoading = true)
        assertTrue(loadingState.isLoading)
        
        val refreshingState = populatedState.copy(isRefreshing = true)
        assertTrue(refreshingState.isRefreshing)
    }
    
    @Test
    fun `NavigationDestination should handle all navigation types`() {
        // Test simple destinations
        val onboarding = NavigationDestination.Onboarding
        val calendar = NavigationDestination.Calendar
        val insights = NavigationDestination.Insights
        val settings = NavigationDestination.Settings
        
        assertNotEquals<NavigationDestination>(onboarding, calendar)
        assertNotEquals<NavigationDestination>(insights, settings)
        
        // Test parameterized destinations
        val dailyLogging = NavigationDestination.DailyLogging
        val dailyLoggingWithDate = NavigationDestination.DailyLoggingWithDate(testDate)
        val bbtChart = NavigationDestination.BBTChart("cycle-123")
        val bbtChartDefault = NavigationDestination.BBTChart()
        
        assertTrue(dailyLoggingWithDate is NavigationDestination.DailyLoggingWithDate)
        assertEquals(testDate, dailyLoggingWithDate.date)
        
        assertTrue(bbtChart is NavigationDestination.BBTChart)
        assertEquals("cycle-123", bbtChart.cycleId)
        assertNull(bbtChartDefault.cycleId)
    }
    
    @Test
    fun `NavigationEvent should handle different navigation actions`() {
        // Test navigation events
        val navigateTo = NavigationEvent.NavigateTo(NavigationDestination.Calendar)
        val navigateBack = NavigationEvent.NavigateBack
        val navigateWithClear = NavigationEvent.NavigateToWithClearStack(NavigationDestination.Insights)
        
        assertTrue(navigateTo is NavigationEvent.NavigateTo)
        assertTrue(navigateBack is NavigationEvent.NavigateBack)
        assertTrue(navigateWithClear is NavigationEvent.NavigateToWithClearStack)
        
        assertEquals(NavigationDestination.Calendar, navigateTo.destination)
        assertEquals(NavigationDestination.Insights, navigateWithClear.destination)
    }
    
    @Test
    fun `NavigationState should track navigation stack correctly`() {
        // Test initial navigation state
        val initialState = NavigationState()
        assertEquals(NavigationDestination.Onboarding, initialState.currentDestination)
        assertFalse(initialState.canNavigateBack)
        assertTrue(initialState.navigationStack.isEmpty())
        
        // Test navigation with stack
        val navigatedState = initialState.copy(
            currentDestination = NavigationDestination.Calendar,
            canNavigateBack = true,
            navigationStack = listOf(NavigationDestination.Onboarding, NavigationDestination.Calendar)
        )
        
        assertEquals(NavigationDestination.Calendar, navigatedState.currentDestination)
        assertTrue(navigatedState.canNavigateBack)
        assertEquals(2, navigatedState.navigationStack.size)
        assertTrue(navigatedState.navigationStack.contains(NavigationDestination.Onboarding))
    }
    
    @Test
    fun `LoadingState should handle all loading scenarios`() {
        // Test all loading states
        val idle = LoadingState.Idle
        val loading = LoadingState.Loading
        val success = LoadingState.Success("test data")
        val error = LoadingState.Error("test error")
        
        assertTrue(idle is LoadingState.Idle)
        assertTrue(loading is LoadingState.Loading)
        assertTrue(success is LoadingState.Success<*>)
        assertTrue(error is LoadingState.Error)
        
        // Test data extraction
        assertEquals("test data", success.data)
        assertEquals("test error", error.message)
        
        // Test type safety
        val stringSuccess = LoadingState.Success("string")
        val intSuccess = LoadingState.Success(42)
        
        assertTrue(stringSuccess.data is String)
        assertTrue(intSuccess.data is Int)
    }
    
    @Test
    fun `UiState interface should be implemented by all state classes`() {
        // Test that all UI state classes implement UiState
        val states: List<UiState> = listOf(
            OnboardingUiState(),
            DailyLoggingUiState(),
            CalendarUiState(),
            InsightsUiState(),
            UnitSystemSettingsUiState()
        )
        
        // Verify all states implement UiState
        states.forEach { state ->
            assertTrue(state is UiState, "State ${state::class.simpleName} should implement UiState")
        }
    }
    
    @Test
    fun `Presentation layer should support cross-platform date handling`() {
        // Test date handling across different scenarios
        val dates = listOf(
            LocalDate(2024, 1, 1),   // New Year
            LocalDate(2024, 2, 29),  // Leap year
            LocalDate(2024, 12, 31), // End of year
            today                     // Current date
        )
        
        dates.forEach { date ->
            // Test in DailyLoggingUiState
            val loggingState = DailyLoggingUiState(selectedDate = date)
            assertEquals(date, loggingState.selectedDate)
            
            // Test in CalendarUiState
            val calendarState = CalendarUiState(currentMonth = date, selectedDate = date)
            assertEquals(date, calendarState.currentMonth)
            assertEquals(date, calendarState.selectedDate)
            
            // Test in CalendarDay
            val calendarDay = CalendarDay(date = date)
            assertEquals(date, calendarDay.date)
        }
    }
    
    // Helper methods
    private fun createTestCycle() = Cycle(
        id = "test-cycle",
        userId = "test-user",
        startDate = LocalDate(testDate.year, testDate.month, testDate.dayOfMonth - 10),
        endDate = null,
        predictedOvulationDate = LocalDate(testDate.year, testDate.month, testDate.dayOfMonth + 4),
        confirmedOvulationDate = null,
        cycleLength = 28,
        lutealPhaseLength = 14
    )
    
    private fun createTestDailyLog(date: LocalDate) = DailyLog(
        id = "test-log-$date",
        userId = "test-user",
        date = date,
        periodFlow = PeriodFlow.MEDIUM,
        symptoms = listOf(Symptom.CRAMPS),
        mood = Mood.NEUTRAL,
        sexualActivity = null,
        bbt = 98.2,
        cervicalMucus = CervicalMucus.CREAMY,
        opkResult = OPKResult.NEGATIVE,
        notes = "Test notes",
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    private fun createTestInsight(
        id: String,
        isRead: Boolean,
        type: InsightType = InsightType.PATTERN_RECOGNITION
    ) = Insight(
        id = id,
        userId = "test-user",
        generatedDate = Clock.System.now(),
        insightText = "Test insight $id",
        type = type,
        isRead = isRead,
        relatedLogIds = emptyList(),
        confidence = 0.8,
        actionable = false
    )
}