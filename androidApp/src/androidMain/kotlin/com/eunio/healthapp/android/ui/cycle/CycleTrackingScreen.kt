package com.eunio.healthapp.android.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.theme.EunioColors

// Simplified UI state for cycle tracking
data class SimpleCycleUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasCycle: Boolean = false,
    val cycleLength: Int? = null,
    val cycleHistory: List<String> = emptyList()
)

/**
 * Cycle tracking screen with detailed cycle information and predictions.
 */
@Composable
fun CycleTrackingScreen(
    uiState: SimpleCycleUiState = SimpleCycleUiState()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EunioColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Cycle Tracking",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = EunioColors.OnBackground
            )
        }
        
        // Current cycle overview
        item {
            CurrentCycleCard(uiState = uiState)
        }
        
        // Cycle predictions
        item {
            CyclePredictionsCard(uiState = uiState)
        }
        
        // Cycle phase information
        item {
            CyclePhaseCard(uiState = uiState)
        }
        
        // Cycle history
        item {
            CycleHistoryCard(cycles = uiState.cycleHistory)
        }
        
        // Error handling
        uiState.errorMessage?.let { error ->
            item {
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
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            item {
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
}

@Composable
private fun CurrentCycleCard(uiState: SimpleCycleUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = EunioColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Current Cycle",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.SemiBold,
                    color = EunioColors.OnSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.hasCycle) {
                val cycleDay = 15 // Simplified for now
                
                // Cycle day progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Day $cycleDay",
                            style = MaterialTheme.typography.h4,
                            fontWeight = FontWeight.Bold,
                            color = EunioColors.Primary
                        )
                        Text(
                            text = "of current cycle",
                            style = MaterialTheme.typography.body2,
                            color = EunioColors.OnSurfaceVariant
                        )
                    }
                    
                    uiState.cycleLength?.let { length ->
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "$length days",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.SemiBold,
                                color = EunioColors.OnSurface
                            )
                            Text(
                                text = "average length",
                                style = MaterialTheme.typography.caption,
                                color = EunioColors.OnSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cycle progress bar
                uiState.cycleLength?.let { length ->
                    val progress = (cycleDay.toFloat() / length.toFloat()).coerceAtMost(1f)
                    
                    Column {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = EunioColors.Primary,
                            backgroundColor = EunioColors.Primary.copy(alpha = 0.2f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Started March 1, 2024",
                                style = MaterialTheme.typography.caption,
                                color = EunioColors.OnSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% complete",
                                style = MaterialTheme.typography.caption,
                                color = EunioColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No active cycle",
                        style = MaterialTheme.typography.h6,
                        color = EunioColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start logging your period to begin cycle tracking",
                        style = MaterialTheme.typography.body2,
                        color = EunioColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CyclePredictionsCard(uiState: SimpleCycleUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Predictions",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.hasCycle) {
                PredictionItem(
                    title = "Next Period",
                    date = "March 29, 2024",
                    color = EunioColors.MenstrualPhase,
                    description = "Expected start date"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PredictionItem(
                    title = "Next Ovulation",
                    date = "March 15, 2024",
                    color = EunioColors.OvulatoryPhase,
                    description = "Predicted ovulation"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fertility Window",
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium,
                            color = EunioColors.OnSurface
                        )
                        Text(
                            text = "March 10 - March 16",
                            style = MaterialTheme.typography.body2,
                            color = EunioColors.FollicularPhase
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = EunioColors.FollicularPhase,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            } else {
                Text(
                    text = "Log more cycles to see predictions",
                    style = MaterialTheme.typography.body2,
                    color = EunioColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PredictionItem(
    title: String,
    date: String,
    color: Color,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = EunioColors.OnSurface
            )
            Text(
                text = date,
                style = MaterialTheme.typography.body2,
                color = color
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = EunioColors.OnSurfaceVariant
            )
        }
        
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun CyclePhaseCard(uiState: SimpleCycleUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Current Phase",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.hasCycle) {
                val cycleDay = 15 // Simplified for now
                
                val (phaseName, phaseColor, phaseDescription) = when {
                    cycleDay <= 5 -> Triple("Menstrual", EunioColors.MenstrualPhase, "Period phase - rest and self-care")
                    cycleDay <= 13 -> Triple("Follicular", EunioColors.FollicularPhase, "Energy building phase")
                    cycleDay <= 16 -> Triple("Ovulatory", EunioColors.OvulatoryPhase, "Peak fertility window")
                    else -> Triple("Luteal", EunioColors.LutealPhase, "Pre-menstrual phase")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = phaseName,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.SemiBold,
                            color = phaseColor
                        )
                        Text(
                            text = phaseDescription,
                            style = MaterialTheme.typography.body2,
                            color = EunioColors.OnSurfaceVariant
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = phaseColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            } else {
                Text(
                    text = "Start tracking to see your current cycle phase",
                    style = MaterialTheme.typography.body2,
                    color = EunioColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CycleHistoryCard(cycles: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = EunioColors.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Recent Cycles",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = EunioColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (cycles.isNotEmpty()) {
                cycles.take(5).forEach { cycle ->
                    CycleHistoryItem(cycle = cycle)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "No cycle history available yet",
                    style = MaterialTheme.typography.body2,
                    color = EunioColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CycleHistoryItem(cycle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "March 1, 2024",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = EunioColors.OnSurface
            )
            Text(
                text = "to March 28, 2024",
                style = MaterialTheme.typography.caption,
                color = EunioColors.OnSurfaceVariant
            )
        }
        
        Text(
            text = "28 days",
            style = MaterialTheme.typography.body2,
            color = EunioColors.Primary
        )
    }
}