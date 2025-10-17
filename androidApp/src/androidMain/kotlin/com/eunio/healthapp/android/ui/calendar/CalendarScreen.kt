package com.eunio.healthapp.android.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.theme.EunioColors

// Simplified calendar day types
enum class CalendarDayType {
    NORMAL,
    PERIOD_PREDICTED,
    PERIOD_ACTUAL,
    OVULATION_PREDICTED,
    OVULATION_CONFIRMED,
    FERTILITY_WINDOW
}

// Simplified UI state
data class SimpleCalendarUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasCycle: Boolean = false,
    val cycleLength: Int? = null
)

/**
 * Smart calendar screen with cycle phase visualization.
 */
@Composable
fun CalendarScreen(
    uiState: SimpleCalendarUiState = SimpleCalendarUiState(),
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onGoToToday: () -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onNavigateToLogging: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EunioColors.Background)
            .padding(16.dp)
    ) {
        // Calendar header with navigation
        CalendarHeader(
            currentMonth = "March 2024",
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onGoToToday = onGoToToday
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calendar grid
        CalendarGrid(
            onDateSelected = { date ->
                onDateSelected(date)
                onNavigateToLogging(date)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cycle information card
        CycleInfoCard(uiState = uiState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        CalendarLegend()
        
        // Error handling
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = EunioColors.Error.copy(alpha = 0.1f)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = EunioColors.Error,
                    style = MaterialTheme.typography.body2
                )
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EunioColors.Primary
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous month button
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = EunioColors.Primary
            )
        }
        
        // Month and year display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentMonth,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnBackground
            )
            
            // Today button
            TextButton(onClick = onGoToToday) {
                Text(
                    text = "Today",
                    color = EunioColors.Primary,
                    style = MaterialTheme.typography.caption
                )
            }
        }
        
        // Next month button
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = EunioColors.Primary
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    onDateSelected: (String) -> Unit
) {
    Column {
        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption,
                    color = EunioColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar days grid - simplified with sample data
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(42) { index ->
                val dayNumber = (index % 31) + 1
                CalendarDayItem(
                    dayNumber = dayNumber,
                    dayType = when {
                        dayNumber == 1 || dayNumber == 2 -> CalendarDayType.PERIOD_ACTUAL
                        dayNumber == 15 -> CalendarDayType.OVULATION_PREDICTED
                        dayNumber in 12..17 -> CalendarDayType.FERTILITY_WINDOW
                        dayNumber == 29 -> CalendarDayType.PERIOD_PREDICTED
                        else -> CalendarDayType.NORMAL
                    },
                    isSelected = false,
                    isToday = dayNumber == 15,
                    onDateSelected = { onDateSelected(dayNumber.toString()) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayItem(
    dayNumber: Int,
    dayType: CalendarDayType,
    isSelected: Boolean,
    isToday: Boolean,
    onDateSelected: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> EunioColors.Primary
        dayType == CalendarDayType.PERIOD_ACTUAL -> EunioColors.MenstrualPhase
        dayType == CalendarDayType.PERIOD_PREDICTED -> EunioColors.MenstrualPhase.copy(alpha = 0.3f)
        dayType == CalendarDayType.OVULATION_CONFIRMED -> EunioColors.OvulatoryPhase
        dayType == CalendarDayType.OVULATION_PREDICTED -> EunioColors.OvulatoryPhase.copy(alpha = 0.3f)
        dayType == CalendarDayType.FERTILITY_WINDOW -> EunioColors.FollicularPhase.copy(alpha = 0.3f)
        isToday -> EunioColors.Primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> EunioColors.OnPrimary
        dayType == CalendarDayType.PERIOD_ACTUAL -> EunioColors.OnPrimary
        dayType == CalendarDayType.OVULATION_CONFIRMED -> EunioColors.OnPrimary
        else -> EunioColors.OnSurface
    }
    
    val borderColor = when {
        isToday && !isSelected -> EunioColors.Primary
        else -> Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (borderColor != Color.Transparent) 2.dp else 0.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { onDateSelected() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            color = textColor,
            style = MaterialTheme.typography.caption,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CycleInfoCard(uiState: SimpleCalendarUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cycle Information",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (uiState.hasCycle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Current Cycle",
                            style = MaterialTheme.typography.caption,
                            color = EunioColors.OnSurfaceVariant
                        )
                        Text(
                            text = "Day 15",
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.Medium,
                            color = EunioColors.OnSurface
                        )
                    }
                    
                    uiState.cycleLength?.let { length ->
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Avg Length",
                                style = MaterialTheme.typography.caption,
                                color = EunioColors.OnSurfaceVariant
                            )
                            Text(
                                text = "$length days",
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Medium,
                                color = EunioColors.OnSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Period",
                        style = MaterialTheme.typography.caption,
                        color = EunioColors.OnSurfaceVariant
                    )
                    Text(
                        text = "March 29, 2024",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = EunioColors.MenstrualPhase
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Ovulation",
                        style = MaterialTheme.typography.caption,
                        color = EunioColors.OnSurfaceVariant
                    )
                    Text(
                        text = "March 15, 2024",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = EunioColors.OvulatoryPhase
                    )
                }
            } else {
                Text(
                    text = "Start logging your period to see cycle predictions",
                    style = MaterialTheme.typography.body2,
                    color = EunioColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Legend",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend items
            val legendItems = listOf(
                LegendItem("Period", EunioColors.MenstrualPhase),
                LegendItem("Predicted Period", EunioColors.MenstrualPhase.copy(alpha = 0.3f)),
                LegendItem("Ovulation", EunioColors.OvulatoryPhase),
                LegendItem("Predicted Ovulation", EunioColors.OvulatoryPhase.copy(alpha = 0.3f)),
                LegendItem("Fertility Window", EunioColors.FollicularPhase.copy(alpha = 0.3f))
            )
            
            legendItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { item ->
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = item.color,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.caption,
                                color = EunioColors.OnSurfaceVariant
                            )
                        }
                    }
                    // Fill remaining space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowItems != legendItems.chunked(2).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private data class LegendItem(
    val label: String,
    val color: Color
)