package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
 * A composable that displays unit system preference setting with selection interface.
 * Provides immediate visual feedback and accessibility support.
 * 
 * @param currentUnitSystem The currently selected unit system
 * @param onUnitSystemChanged Callback when unit system selection changes
 * @param modifier Modifier for styling
 * @param enabled Whether the setting is enabled for interaction
 */
@Composable
fun UnitSystemSettingItem(
    currentUnitSystem: UnitSystem,
    onUnitSystemChanged: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Unit system preference setting"
            }
    ) {
        // Setting title and description
        Text(
            text = "Unit System",
            style = MaterialTheme.typography.h6.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Choose how measurements are displayed throughout the app",
            style = MaterialTheme.typography.body2,
            color = if (enabled) MaterialTheme.colors.onSurface.copy(alpha = 0.7f) else MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Unit system options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UnitSystemOption(
                unitSystem = UnitSystem.METRIC,
                isSelected = currentUnitSystem == UnitSystem.METRIC,
                onSelected = { onUnitSystemChanged(UnitSystem.METRIC) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            
            UnitSystemOption(
                unitSystem = UnitSystem.IMPERIAL,
                isSelected = currentUnitSystem == UnitSystem.IMPERIAL,
                onSelected = { onUnitSystemChanged(UnitSystem.IMPERIAL) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}