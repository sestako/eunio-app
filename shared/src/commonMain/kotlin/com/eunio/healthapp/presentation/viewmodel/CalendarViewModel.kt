package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.usecase.cycle.GetCurrentCycleUseCase
import com.eunio.healthapp.domain.usecase.cycle.PredictOvulationUseCase
import com.eunio.healthapp.domain.usecase.logging.GetLogHistoryUseCase
import com.eunio.healthapp.presentation.navigation.NavigationDestination
import com.eunio.healthapp.presentation.navigation.NavigationEvent
import com.eunio.healthapp.presentation.state.CalendarDay
import com.eunio.healthapp.presentation.state.CalendarDayType
import com.eunio.healthapp.presentation.state.CalendarUiState
import com.eunio.healthapp.presentation.state.CyclePredictions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

/**
 * ViewModel for managing calendar functionality.
 */
class CalendarViewModel(
    private val getCurrentCycleUseCase: GetCurrentCycleUseCase,
    private val predictOvulationUseCase: PredictOvulationUseCase,
    private val getLogHistoryUseCase: GetLogHistoryUseCase,
    dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
) : BaseViewModel<CalendarUiState>(dispatcher) {
    
    override val initialState = CalendarUiState(
        currentMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
    )
    
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()
    
    init {
        loadCalendarData()
    }
    
    /**
     * Loads calendar data for the current month.
     */
    private fun loadCalendarData() {
        val currentMonth = uiState.value.currentMonth ?: return
        
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Load current cycle
                val currentCycle = getCurrentCycleUseCase.execute("current_user").getOrNull()
                
                // Load logs for the month
                val startOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
                val endOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                val logs = getLogHistoryUseCase("current_user", startOfMonth, endOfMonth).getOrNull() ?: emptyList()
                val logsMap = logs.associateBy { it.date }
                
                // Generate predictions
                val predictions = currentCycle?.let { cycle ->
                    val ovulationPrediction = predictOvulationUseCase.execute("current_user").getOrNull()
                    val nextOvulation = ovulationPrediction?.predictedDate
                    CyclePredictions(
                        nextPeriodDate = cycle.startDate.plus(DatePeriod(days = cycle.cycleLength ?: 28)),
                        nextOvulationDate = nextOvulation,
                        fertilityWindow = nextOvulation?.let { ovulation ->
                            ovulation.minus(DatePeriod(days = 5))..ovulation.plus(DatePeriod(days = 1))
                        }
                    )
                }
                
                updateState { 
                    it.copy(
                        isLoading = false,
                        currentCycle = currentCycle,
                        logsInMonth = logsMap,
                        predictions = predictions
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load calendar data: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Navigates to the previous month.
     */
    fun previousMonth() {
        val currentMonth = uiState.value.currentMonth ?: return
        val previousMonth = currentMonth.minus(DatePeriod(months = 1))
        
        updateState { it.copy(currentMonth = previousMonth) }
        loadCalendarData()
    }
    
    /**
     * Navigates to the next month.
     */
    fun nextMonth() {
        val currentMonth = uiState.value.currentMonth ?: return
        val nextMonth = currentMonth.plus(DatePeriod(months = 1))
        
        updateState { it.copy(currentMonth = nextMonth) }
        loadCalendarData()
    }
    
    /**
     * Navigates to today's month.
     */
    fun goToToday() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        updateState { it.copy(currentMonth = today) }
        loadCalendarData()
    }
    
    /**
     * Selects a date on the calendar.
     */
    fun selectDate(date: LocalDate) {
        updateState { it.copy(selectedDate = date) }
    }
    
    /**
     * Navigates to daily logging for the selected date.
     */
    fun navigateToLogging(date: LocalDate? = null) {
        val targetDate = date ?: uiState.value.selectedDate
        if (targetDate != null) {
            viewModelScope.launch {
                _navigationEvents.emit(
                    NavigationEvent.NavigateTo(NavigationDestination.DailyLoggingWithDate(targetDate))
                )
            }
        }
    }
    
    /**
     * Generates calendar days for the current month view.
     */
    fun getCalendarDays(): List<CalendarDay> {
        val currentMonth = uiState.value.currentMonth ?: return emptyList()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val state = uiState.value
        
        val startOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
        val endOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        
        // Get first day of week for the month
        val firstDayOfWeek = startOfMonth.dayOfWeek.ordinal
        val startDate = startOfMonth.minus(DatePeriod(days = firstDayOfWeek))
        
        // Generate 42 days (6 weeks) for calendar grid
        val days = mutableListOf<CalendarDay>()
        var currentDate = startDate
        
        repeat(42) {
            val isInCurrentMonth = currentDate.month == currentMonth.month
            val isToday = currentDate == today
            val hasLog = state.logsInMonth.containsKey(currentDate)
            val periodFlow = state.logsInMonth[currentDate]?.periodFlow
            
            val dayType = when {
                // Check for actual period data
                periodFlow != null -> CalendarDayType.PERIOD_ACTUAL
                
                // Check predictions
                state.predictions?.nextPeriodDate == currentDate -> CalendarDayType.PERIOD_PREDICTED
                state.predictions?.nextOvulationDate == currentDate -> CalendarDayType.OVULATION_PREDICTED
                state.predictions?.fertilityWindow?.contains(currentDate) == true -> CalendarDayType.FERTILITY_WINDOW
                
                // Check current cycle data
                state.currentCycle?.confirmedOvulationDate == currentDate -> CalendarDayType.OVULATION_CONFIRMED
                
                else -> CalendarDayType.NORMAL
            }
            
            days.add(
                CalendarDay(
                    date = currentDate,
                    isToday = isToday,
                    isInCurrentMonth = isInCurrentMonth,
                    dayType = dayType,
                    hasLog = hasLog,
                    periodFlow = periodFlow
                )
            )
            
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return days
    }
    
    /**
     * Refreshes calendar data.
     */
    fun refresh() {
        loadCalendarData()
    }
    
    /**
     * Clears any error messages.
     */
    fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }
}