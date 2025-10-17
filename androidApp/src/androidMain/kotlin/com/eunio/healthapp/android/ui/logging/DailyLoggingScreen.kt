package com.eunio.healthapp.android.ui.logging

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.components.*
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.accessibleClickable
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.accessibleFormField
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.accessibleHeading
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.liveRegion
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.minimumTouchTarget
import com.eunio.healthapp.android.ui.components.AccessibilityPriority
import com.eunio.healthapp.presentation.viewmodel.DailyLoggingViewModel
import com.eunio.healthapp.presentation.state.DailyLoggingUiState
import com.eunio.healthapp.domain.model.*
import org.koin.compose.koinInject

/**
 * Daily logging screen with proper MVVM architecture and comprehensive accessibility support.
 * Allows users to input and edit their daily health data with full screen reader support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyLoggingScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: DailyLoggingViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle messages from ViewModel
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            // Handle success messages if needed
        }
    }
    
    // Show snackbar for success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }
    
    // Show snackbar for error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }
    
    AccessibilityProvider {
        // Announce state changes
        uiState.errorMessage?.let { error ->
            AccessibilityUtils.announceForAccessibility(
                "Error: $error",
                AccessibilityPriority.HIGH
            )
        }
        
        uiState.successMessage?.let { message ->
            AccessibilityUtils.announceForAccessibility(
                message,
                AccessibilityPriority.NORMAL
            )
        }
        
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Daily Log",
                            modifier = Modifier.accessibleHeading(1)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .minimumTouchTarget()
                                .semantics {
                                    contentDescription = "Navigate back to previous screen"
                                }
                        ) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = null // Description is on the button
                            )
                        }
                    },
                    actions = {
                        // Always show Save button
                        TextButton(
                            onClick = viewModel::saveLog,
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .minimumTouchTarget()
                                .semantics {
                                    contentDescription = if (uiState.isSaving) {
                                        "Saving your daily log, please wait"
                                    } else {
                                        "Save daily log"
                                    }
                                    stateDescription = if (uiState.isSaving) "Saving" else "Ready to save"
                                }
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .semantics {
                                            contentDescription = "Saving in progress"
                                        },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Date picker section
                AccessibleDateNavigationSection(
                    selectedDate = uiState.selectedDate.toString(),
                    onPreviousDay = { 
                        uiState.selectedDate?.let { currentDate ->
                            viewModel.selectDate(currentDate.minus(1, DateTimeUnit.DAY))
                        }
                    },
                    onNextDay = { 
                        uiState.selectedDate?.let { currentDate ->
                            viewModel.selectDate(currentDate.plus(1, DateTimeUnit.DAY))
                        }
                    },
                    onDateSelected = { date ->
                        viewModel.selectDate(date)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Divider()
                
                // Main form content
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = "Loading your daily log data"
                                liveRegion = LiveRegionMode.Polite
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics {
                                contentDescription = "Loading daily log data, please wait"
                            }
                        )
                    }
                } else {
                    AccessibleDailyLogForm(
                        uiState = uiState,
                        onPeriodFlowChanged = viewModel::updatePeriodFlow,
                        onSymptomToggled = viewModel::toggleSymptom,
                        onMoodChanged = viewModel::updateMood,
                        onSexualActivityChanged = viewModel::updateSexualActivity,
                        onBBTChanged = viewModel::updateBBT,
                        onCervicalMucusChanged = viewModel::updateCervicalMucus,
                        onOPKResultChanged = viewModel::updateOPKResult,
                        onNotesChanged = viewModel::updateNotes,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
                
                // Error/Success messages with proper accessibility
                uiState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .liveRegion(LiveRegionMode.Assertive)
                            .semantics {
                                contentDescription = "Error message: $error"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearMessages,
                                modifier = Modifier
                                    .minimumTouchTarget()
                                    .semantics {
                                        contentDescription = "Dismiss error message"
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
                
                uiState.successMessage?.let { success ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .liveRegion(LiveRegionMode.Polite)
                            .semantics {
                                contentDescription = "Success message: $success"
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = success,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::clearMessages,
                                modifier = Modifier
                                    .minimumTouchTarget()
                                    .semantics {
                                        contentDescription = "Dismiss success message"
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibleDateNavigationSection(
    selectedDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Parse the selected date with error handling
    val currentDate = remember(selectedDate) {
        try {
            LocalDate.parse(selectedDate)
        } catch (e: Exception) {
            // Fallback to today's date if parsing fails
            Clock.System.todayIn(TimeZone.currentSystemDefault())
        }
    }
    
    // Generate date range (3 days before to 3 days after)
    val dateRange = remember(currentDate) {
        (-3..3).map { offset ->
            val date = currentDate.plus(offset, DateTimeUnit.DAY)
            DateDisplay(
                date = date,
                dayNumber = date.dayOfMonth,
                monthAbbreviation = date.month.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                isSelected = offset == 0
            )
        }
    }
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Date navigation section"
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Date navigation controls, current date: $selectedDate"
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onPreviousDay()
                    // Announce the change will be handled by the parent component
                },
                modifier = Modifier
                    .minimumTouchTarget()
                    .semantics {
                        contentDescription = "Go to previous day"
                    }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft, 
                    contentDescription = null
                )
            }
            
            Text(
                text = selectedDate,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .accessibleHeading(2)
                    .semantics {
                        contentDescription = "Selected date: $selectedDate"
                    }
            )
            
            IconButton(
                onClick = {
                    onNextDay()
                    // Announce the change will be handled by the parent component
                },
                modifier = Modifier
                    .minimumTouchTarget()
                    .semantics {
                        contentDescription = "Go to next day"
                    }
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight, 
                    contentDescription = null
                )
            }
        }
        
        // Quick date selection with accessibility
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Quick date selection, swipe to browse recent dates"
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(dateRange) { dateDisplay ->
                val dateDescription = "${dateDisplay.monthAbbreviation} ${dateDisplay.dayNumber}"
                
                Card(
                    modifier = Modifier
                        .accessibleClickable(
                            onClick = { onDateSelected(dateDisplay.date) },
                            contentDescription = if (dateDisplay.isSelected) {
                                "$dateDescription, currently selected"
                            } else {
                                "Select $dateDescription"
                            },
                            role = Role.Button
                        )
                        .padding(2.dp)
                        .minimumTouchTarget(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (dateDisplay.isSelected) 8.dp else 2.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dateDisplay.isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dateDisplay.dayNumber.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (dateDisplay.isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = dateDisplay.monthAbbreviation,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dateDisplay.isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Internal data class to encapsulate date display information
 */
private data class DateDisplay(
    val date: LocalDate,
    val dayNumber: Int,
    val monthAbbreviation: String,
    val isSelected: Boolean
)

@Composable
private fun AccessibleDailyLogForm(
    uiState: DailyLoggingUiState,
    onPeriodFlowChanged: (PeriodFlow?) -> Unit,
    onSymptomToggled: (Symptom) -> Unit,
    onMoodChanged: (Mood?) -> Unit,
    onSexualActivityChanged: (SexualActivity?) -> Unit,
    onBBTChanged: (String) -> Unit,
    onCervicalMucusChanged: (CervicalMucus?) -> Unit,
    onOPKResultChanged: (OPKResult?) -> Unit,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = "Daily health log form"
        },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Period Flow Section
        AccessibleFormSection(title = "Period Flow") {
            AccessiblePeriodFlowSelector(
                selectedFlow = uiState.periodFlow,
                onFlowSelected = onPeriodFlowChanged
            )
        }
        
        // Symptoms Section
        AccessibleFormSection(title = "Symptoms") {
            AccessibleSymptomSelector(
                selectedSymptoms = uiState.selectedSymptoms,
                onSymptomToggled = onSymptomToggled
            )
        }
        
        // Mood Section
        AccessibleFormSection(title = "Mood") {
            AccessibleMoodSelector(
                selectedMood = uiState.mood,
                onMoodSelected = onMoodChanged
            )
        }
        
        // BBT Section
        AccessibleFormSection(title = "Basal Body Temperature") {
            AccessibleBBTInput(
                bbt = uiState.bbt,
                onBBTChanged = onBBTChanged,
                isValid = uiState.isBbtValid
            )
        }
        
        // Cervical Mucus Section
        AccessibleFormSection(title = "Cervical Mucus") {
            AccessibleCervicalMucusSelector(
                selectedMucus = uiState.cervicalMucus,
                onMucusSelected = onCervicalMucusChanged
            )
        }
        
        // OPK Result Section
        AccessibleFormSection(title = "Ovulation Test (OPK)") {
            AccessibleOPKResultSelector(
                selectedResult = uiState.opkResult,
                onResultSelected = onOPKResultChanged
            )
        }
        
        // Sexual Activity Section
        AccessibleFormSection(title = "Sexual Activity") {
            AccessibleSexualActivitySelector(
                selectedActivity = uiState.sexualActivity,
                onActivityChanged = onSexualActivityChanged
            )
        }
        
        // Notes Section
        AccessibleFormSection(title = "Notes") {
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .accessibleFormField(
                        label = "Additional notes",
                        value = uiState.notes,
                        helperText = "Optional: Add any additional observations or notes about your day"
                    ),
                placeholder = { Text("Add any additional notes...") },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun AccessibleFormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = "$title section"
        }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .accessibleHeading(3)
                .semantics {
                    contentDescription = "$title section heading"
                }
        )
        content()
    }
}





