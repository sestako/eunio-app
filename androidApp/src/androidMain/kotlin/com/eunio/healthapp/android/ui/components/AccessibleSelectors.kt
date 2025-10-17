@file:OptIn(ExperimentalMaterial3Api::class)

package com.eunio.healthapp.android.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.accessibleFormField
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.accessibleSelection
import com.eunio.healthapp.android.ui.components.AccessibilityUtils.minimumTouchTarget
import com.eunio.healthapp.android.ui.components.AccessibilityPriority
import com.eunio.healthapp.domain.model.*

/**
 * Accessible date navigation section with previous/next day buttons.
 */
@Composable
fun AccessibleDateNavigationSection(
    selectedDate: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
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
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics {
                contentDescription = "Selected date: $selectedDate"
            }
        )
        
        IconButton(
            onClick = onNextDay,
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
}

/**
 * Accessible period flow selector with proper semantics and announcements.
 */
@Composable
fun AccessiblePeriodFlowSelector(
    selectedFlow: PeriodFlow?,
    onFlowSelected: (PeriodFlow?) -> Unit,
    modifier: Modifier = Modifier
) {
    val flows = listOf(
        null to "None",
        PeriodFlow.SPOTTING to "Spotting",
        PeriodFlow.LIGHT to "Light",
        PeriodFlow.MEDIUM to "Medium", 
        PeriodFlow.HEAVY to "Heavy"
    )
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Period flow selection, ${flows.size} options available"
        }
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics {
                contentDescription = "Period flow options, swipe to browse all options"
            }
        ) {
            itemsIndexed(flows) { index, (flow, label) ->
                AccessibleSelectionChip(
                    text = label,
                    isSelected = selectedFlow == flow,
                    onClick = { onFlowSelected(flow) },
                    position = index,
                    totalItems = flows.size,
                    groupDescription = "period flow"
                )
            }
        }
        
        // Announce selection changes
        val context = LocalContext.current
        val accessibilityManager = remember {
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        }
        
        LaunchedEffect(selectedFlow) {
            selectedFlow?.let { flow ->
                val selectedLabel = flows.find { it.first == flow }?.second ?: "Unknown"
                AccessibilityUtils.announceToAccessibilityManager(
                    accessibilityManager,
                    context,
                    "Period flow changed to $selectedLabel"
                )
            }
        }
    }
}

/**
 * Accessible symptom selector with multi-selection support.
 */
@Composable
fun AccessibleSymptomSelector(
    selectedSymptoms: Set<Symptom>,
    onSymptomToggled: (Symptom) -> Unit,
    modifier: Modifier = Modifier
) {
    val symptoms = listOf(
        Symptom.CRAMPS to "Cramps",
        Symptom.HEADACHE to "Headache",
        Symptom.BLOATING to "Bloating",
        Symptom.BREAST_TENDERNESS to "Breast Tenderness",
        Symptom.ACNE to "Acne",
        Symptom.MOOD_SWINGS to "Mood Swings",
        Symptom.FATIGUE to "Fatigue",
        Symptom.NAUSEA to "Nausea",
        Symptom.BACK_PAIN to "Back Pain",
        Symptom.FOOD_CRAVINGS to "Food Cravings"
    )
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Symptom selection, ${selectedSymptoms.size} of ${symptoms.size} symptoms selected"
        },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Group symptoms into rows of 2 for better layout
        val symptomRows = symptoms.chunked(2)
        
        symptomRows.forEachIndexed { rowIndex, rowSymptoms ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Symptom row ${rowIndex + 1} of ${symptomRows.size}"
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSymptoms.forEach { (symptom, label) ->
                    val isSelected = selectedSymptoms.contains(symptom)
                    AccessibleMultiSelectionChip(
                        text = label,
                        isSelected = isSelected,
                        onClick = { onSymptomToggled(symptom) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number of symptoms in row
                if (rowSymptoms.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        // Provide summary of selected symptoms
        if (selectedSymptoms.isNotEmpty()) {
            Text(
                text = "Selected: ${selectedSymptoms.size} symptoms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "${selectedSymptoms.size} symptoms currently selected"
                }
            )
        }
    }
}

/**
 * Accessible mood selector with emoji support.
 */
@Composable
fun AccessibleMoodSelector(
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf(
        Triple(null, "None", ""),
        Triple(Mood.HAPPY, "Happy", "😊"),
        Triple(Mood.SAD, "Sad", "😢"),
        Triple(Mood.ANXIOUS, "Anxious", "😰"),
        Triple(Mood.IRRITABLE, "Irritable", "😠"),
        Triple(Mood.CALM, "Calm", "😌"),
        Triple(Mood.ENERGETIC, "Energetic", "⚡"),
        Triple(Mood.TIRED, "Tired", "😴"),
        Triple(Mood.NEUTRAL, "Neutral", "😐")
    )
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Mood selection, ${moods.size} mood options available"
        }
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics {
                contentDescription = "Mood options, swipe to browse all moods"
            }
        ) {
            itemsIndexed(moods) { index, (mood, label, emoji) ->
                AccessibleSelectionChip(
                    text = if (emoji.isNotEmpty()) "$emoji $label" else label,
                    isSelected = selectedMood == mood,
                    onClick = { onMoodSelected(mood) },
                    position = index,
                    totalItems = moods.size,
                    groupDescription = "mood",
                    accessibilityLabel = label // Use label without emoji for screen readers
                )
            }
        }
    }
}

/**
 * Accessible BBT input with validation and conversion display.
 */
@Composable
fun AccessibleBBTInput(
    bbt: String,
    onBBTChanged: (String) -> Unit,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = bbt,
            onValueChange = onBBTChanged,
            modifier = Modifier
                .fillMaxWidth()
                .accessibleFormField(
                    label = "Basal Body Temperature in Fahrenheit",
                    value = bbt,
                    isError = !isValid,
                    errorMessage = if (!isValid) "Temperature must be between 95.0 and 105.0°F" else null,
                    helperText = "Enter your temperature in Fahrenheit (e.g., 98.2)"
                ),
            label = { Text("Temperature (°F)") },
            placeholder = { Text("e.g., 98.2") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = !isValid,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                unfocusedBorderColor = if (isValid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error
            ),
            trailingIcon = {
                if (bbt.isNotEmpty() && isValid) {
                    ReactiveTemperatureDisplay(
                        temperatureInCelsius = bbt.toDoubleOrNull()?.let { (it - 32) * 5/9 } ?: 0.0,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
        
        if (!isValid && bbt.isNotEmpty()) {
            Text(
                text = "Temperature must be between 95.0 and 105.0°F",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .semantics {
                        contentDescription = "Error: Temperature must be between 95.0 and 105.0 degrees Fahrenheit"
                        liveRegion = LiveRegionMode.Assertive
                    }
            )
        }
        
        Text(
            text = "Best taken first thing in the morning before getting out of bed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 16.dp, top = 4.dp)
                .semantics {
                    contentDescription = "Tip: Best taken first thing in the morning before getting out of bed"
                }
        )
    }
}

/**
 * Accessible cervical mucus selector.
 */
@Composable
fun AccessibleCervicalMucusSelector(
    selectedMucus: CervicalMucus?,
    onMucusSelected: (CervicalMucus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val mucusTypes = listOf(
        null to "None",
        CervicalMucus.DRY to "Dry",
        CervicalMucus.STICKY to "Sticky",
        CervicalMucus.CREAMY to "Creamy",
        CervicalMucus.WATERY to "Watery",
        CervicalMucus.EGG_WHITE to "Egg White"
    )
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Cervical mucus selection, ${mucusTypes.size} options available"
        }
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(mucusTypes) { index, (mucus, label) ->
                AccessibleSelectionChip(
                    text = label,
                    isSelected = selectedMucus == mucus,
                    onClick = { onMucusSelected(mucus) },
                    position = index,
                    totalItems = mucusTypes.size,
                    groupDescription = "cervical mucus type"
                )
            }
        }
    }
}

/**
 * Accessible OPK result selector.
 */
@Composable
fun AccessibleOPKResultSelector(
    selectedResult: OPKResult?,
    onResultSelected: (OPKResult?) -> Unit,
    modifier: Modifier = Modifier
) {
    val results = listOf(
        null to "Not Tested",
        OPKResult.NEGATIVE to "Negative",
        OPKResult.POSITIVE to "Positive",
        OPKResult.PEAK to "Peak"
    )
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Ovulation test result selection, ${results.size} options available"
        }
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(results) { index, (result, label) ->
                AccessibleSelectionChip(
                    text = label,
                    isSelected = selectedResult == result,
                    onClick = { onResultSelected(result) },
                    position = index,
                    totalItems = results.size,
                    groupDescription = "ovulation test result"
                )
            }
        }
    }
}

/**
 * Accessible sexual activity selector with protection options.
 */
@Composable
fun AccessibleSexualActivitySelector(
    selectedActivity: SexualActivity?,
    onActivityChanged: (SexualActivity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProtectionOptions by remember { mutableStateOf(selectedActivity?.occurred == true) }
    
    Column(
        modifier = modifier.semantics {
            contentDescription = "Sexual activity tracking section"
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Yes/No selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Sexual activity occurred selection"
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccessibleSelectionChip(
                text = "No",
                isSelected = selectedActivity?.occurred == false || selectedActivity == null,
                onClick = { 
                    onActivityChanged(SexualActivity(occurred = false))
                    showProtectionOptions = false
                },
                modifier = Modifier.weight(1f),
                groupDescription = "sexual activity occurrence"
            )
            AccessibleSelectionChip(
                text = "Yes",
                isSelected = selectedActivity?.occurred == true,
                onClick = { 
                    onActivityChanged(SexualActivity(occurred = true))
                    showProtectionOptions = true
                },
                modifier = Modifier.weight(1f),
                groupDescription = "sexual activity occurrence"
            )
        }
        
        // Protection options (shown when "Yes" is selected)
        if (selectedActivity?.occurred == true || showProtectionOptions) {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = "Protection method selection"
                }
            ) {
                Text(
                    text = "Protection Used:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .semantics {
                            contentDescription = "Protection method selection heading"
                        }
                )
                
                val protectionTypes = listOf(
                    Protection.NONE to "None",
                    Protection.CONDOM to "Condom",
                    Protection.BIRTH_CONTROL to "Birth Control",
                    Protection.WITHDRAWAL to "Withdrawal"
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(protectionTypes) { index, (protection, label) ->
                        AccessibleSelectionChip(
                            text = label,
                            isSelected = selectedActivity?.protection == protection,
                            onClick = { 
                                onActivityChanged(SexualActivity(occurred = true, protection = protection))
                            },
                            position = index,
                            totalItems = protectionTypes.size,
                            groupDescription = "protection method"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable accessible selection chip for single selection.
 */
@Composable
private fun AccessibleSelectionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: Int = 0,
    totalItems: Int = 1,
    groupDescription: String = "option",
    accessibilityLabel: String = text
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
            .minimumTouchTarget()
            .accessibleSelection(
                isSelected = isSelected,
                role = Role.RadioButton,
                selectionDescription = if (isSelected) {
                    "$accessibilityLabel selected"
                } else {
                    "Select $accessibilityLabel"
                }
            )
            .semantics {
                contentDescription = "$accessibilityLabel, $groupDescription option ${position + 1} of $totalItems, ${if (isSelected) "selected" else "not selected"}"
            }
    )
}

/**
 * Reusable accessible selection chip for multi-selection.
 */
@Composable
private fun AccessibleMultiSelectionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
            .minimumTouchTarget()
            .accessibleSelection(
                isSelected = isSelected,
                role = Role.Checkbox,
                selectionDescription = if (isSelected) {
                    "$text selected"
                } else {
                    "$text not selected"
                }
            )
            .semantics {
                contentDescription = "$text, ${if (isSelected) "selected" else "not selected"}"
            }
    )
}