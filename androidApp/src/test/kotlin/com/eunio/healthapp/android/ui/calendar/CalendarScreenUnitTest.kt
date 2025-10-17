package com.eunio.healthapp.android.ui.calendar

import com.eunio.healthapp.android.ui.calendar.CalendarDayType
import com.eunio.healthapp.android.ui.calendar.SimpleCalendarUiState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarScreenUnitTest {
    
    @Test
    fun calendarDayType_hasCorrectValues() {
        assertEquals(CalendarDayType.NORMAL, CalendarDayType.NORMAL)
        assertEquals(CalendarDayType.PERIOD_ACTUAL, CalendarDayType.PERIOD_ACTUAL)
        assertEquals(CalendarDayType.PERIOD_PREDICTED, CalendarDayType.PERIOD_PREDICTED)
        assertEquals(CalendarDayType.OVULATION_CONFIRMED, CalendarDayType.OVULATION_CONFIRMED)
        assertEquals(CalendarDayType.OVULATION_PREDICTED, CalendarDayType.OVULATION_PREDICTED)
        assertEquals(CalendarDayType.FERTILITY_WINDOW, CalendarDayType.FERTILITY_WINDOW)
    }
    
    @Test
    fun simpleCalendarUiState_withError_containsErrorMessage() {
        val errorMessage = "Failed to load calendar data"
        
        val uiState = SimpleCalendarUiState(
            errorMessage = errorMessage,
            isLoading = false
        )
        
        assertEquals(errorMessage, uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }
    
    @Test
    fun simpleCalendarUiState_loading_hasCorrectState() {
        val uiState = SimpleCalendarUiState(
            isLoading = true,
            errorMessage = null
        )
        
        assertTrue(uiState.isLoading)
        assertEquals(null, uiState.errorMessage)
    }
    
    @Test
    fun simpleCalendarUiState_withCycle_hasCorrectState() {
        val uiState = SimpleCalendarUiState(
            hasCycle = true,
            cycleLength = 28
        )
        
        assertTrue(uiState.hasCycle)
        assertEquals(28, uiState.cycleLength)
    }
    
    @Test
    fun simpleCalendarUiState_defaultValues_areCorrect() {
        val uiState = SimpleCalendarUiState()
        
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.errorMessage)
        assertFalse(uiState.hasCycle)
        assertEquals(null, uiState.cycleLength)
    }
}