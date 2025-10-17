package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.UnitSystem

/**
 * A composable that displays an individual unit system option with selection state.
 * Provides visual feedback, animations, and accessibility support.
 * 
 * @param unitSystem The unit system this option represents
 * @param isSelected Whether this option is currently selected
 * @param onSelected Callback when this option is selected
 * @param modifier Modifier for styling
 * @param enabled Whether this option is enabled for interaction
 */
@Composable
fun UnitSystemOption(
    unitSystem: UnitSystem,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Animated colors for smooth transitions
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    
    val contentColor = if (enabled) {
        if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
    }
    
    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = { if (enabled) onSelected() },
                role = Role.RadioButton,
                enabled = enabled
            )
            .semantics {
                contentDescription = "${unitSystem.displayName} unit system option, ${if (isSelected) "selected" else "not selected"}"
            },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        elevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colors.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Unit system name
            Text(
                text = unitSystem.displayName,
                style = MaterialTheme.typography.subtitle1.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Unit system description
            Text(
                text = getUnitSystemDescription(unitSystem),
                style = MaterialTheme.typography.caption,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Returns a description of what units are included in each system.
 */
private fun getUnitSystemDescription(unitSystem: UnitSystem): String {
    return when (unitSystem) {
        UnitSystem.METRIC -> "kg, km, °C"
        UnitSystem.IMPERIAL -> "lbs, miles, °F"
    }
}