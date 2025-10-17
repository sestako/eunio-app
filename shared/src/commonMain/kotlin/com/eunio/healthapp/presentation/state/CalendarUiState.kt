package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.presentation.viewmodel.UiState
import kotlinx.datetime.LocalDate

/**
 * UI state for calendar screen.
 */
data class CalendarUiState(
    val currentMonth: LocalDate? = null,
    val selectedDate: LocalDate? = null,
    val currentCycle: Cycle? = null,
    val cycleHistory: List<Cycle> = emptyList(),
    val logsInMonth: Map<LocalDate, DailyLog> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val predictions: CyclePredictions? = null
) : UiState

/**
 * Cycle predictions for calendar display.
 */
data class CyclePredictions(
    val nextPeriodDate: LocalDate?,
    val nextOvulationDate: LocalDate?,
    val fertilityWindow: ClosedRange<LocalDate>?
)

/**
 * Calendar day information for UI display.
 */
data class CalendarDay(
    val date: LocalDate,
    val isToday: Boolean = false,
    val isInCurrentMonth: Boolean = true,
    val dayType: CalendarDayType = CalendarDayType.NORMAL,
    val hasLog: Boolean = false,
    val periodFlow: com.eunio.healthapp.domain.model.PeriodFlow? = null
)

enum class CalendarDayType {
    NORMAL,
    PERIOD_PREDICTED,
    PERIOD_ACTUAL,
    OVULATION_PREDICTED,
    OVULATION_CONFIRMED,
    FERTILITY_WINDOW
}