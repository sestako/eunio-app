package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.settings.TemperatureUnit
import com.eunio.healthapp.domain.model.settings.WeightUnit

/**
 * A composable that displays a unit option with selection state and animations.
 * Supports both temperature and weight units with visual feedback.
 */
@Composable
fun <T> UnitOption(
    unit: T,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) where T : Enum<T> {
    // Animation states
    val animationSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    val colorAnimationSpec = tween<Color>(durationMillis = 200, easing = LinearEasing)
    
    // Animated colors for smooth transitions
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = colorAnimationSpec,
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = colorAnimationSpec,
        label = "borderColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = colorAnimationSpec,
        label = "contentColor"
    )
    
    // Scale animation for selection feedback
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = animationSpec,
        label = "scale"
    )
    
    // Get unit display information
    val (displayName, symbol, description) = getUnitDisplayInfo(unit)
    
    Card(
        modifier = modifier
            .scale(scale)
            .selectable(
                selected = isSelected,
                onClick = { if (enabled) onSelected() },
                role = Role.RadioButton,
                enabled = enabled
            )
            .semantics {
                contentDescription = "$displayName unit option, ${if (isSelected) "selected" else "not selected"}"
            }
            .testTag("unit_option_${displayName.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator with animation
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                    exit = scaleOut(animationSpec = tween(150)) + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Unit symbol with emphasis
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.testTag("unit_symbol")
            )
            
            // Unit name
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.testTag("unit_name")
            )
            
            // Unit description
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.testTag("unit_description")
            )
        }
    }
}

/**
 * Gets display information for different unit types.
 */
private fun <T> getUnitDisplayInfo(unit: T): Triple<String, String, String> where T : Enum<T> {
    return when (unit) {
        is TemperatureUnit -> Triple(
            unit.displayName,
            unit.symbol,
            when (unit) {
                TemperatureUnit.CELSIUS -> "Used worldwide"
                TemperatureUnit.FAHRENHEIT -> "Used in US"
            }
        )
        is WeightUnit -> Triple(
            unit.displayName,
            unit.symbol,
            when (unit) {
                WeightUnit.KILOGRAMS -> "Metric system"
                WeightUnit.POUNDS -> "Imperial system"
            }
        )
        else -> Triple(unit.name, unit.name, "")
    }
}

/**
 * Compact version of UnitOption for smaller displays.
 */
@Composable
fun <T> CompactUnitOption(
    unit: T,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) where T : Enum<T> {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(200),
        label = "contentColor"
    )
    
    val (displayName, symbol, _) = getUnitDisplayInfo(unit)
    
    Surface(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = { if (enabled) onSelected() },
                role = Role.RadioButton,
                enabled = enabled
            )
            .semantics {
                contentDescription = "$displayName unit option, ${if (isSelected) "selected" else "not selected"}"
            }
            .testTag("compact_unit_option_${displayName.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            // Unit info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor
                )
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}