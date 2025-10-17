package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.presentation.state.SettingSection as SettingSectionState

/**
 * Settings section component that displays a group of related settings.
 */
@Composable
fun SettingsSection(
    section: SettingSectionState,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = section.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_section_${section.id}"),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section header
                SettingsSectionHeader(
                    title = section.title,
                    subtitle = section.subtitle,
                    icon = getSettingIcon(section.icon),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Section items
                if (section.items.isNotEmpty()) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        section.items.forEach { item ->
                            SettingItem(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header for a settings section.
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("settings_section_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact settings section for smaller displays or specific layouts.
 */
@Composable
fun CompactSettingsSection(
    section: SettingSectionState,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = section.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_settings_section_${section.id}"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Compact section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getSettingIcon(section.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Compact section items
            section.items.forEach { item ->
                CompactSettingItem(
                    item = item,
                    onClick = { onItemClick(item.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp)
                )
            }
        }
    }
}

/**
 * Expandable settings section that can be collapsed/expanded.
 */
@Composable
fun ExpandableSettingsSection(
    section: SettingSectionState,
    onItemClick: (String) -> Unit,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    AnimatedVisibility(
        visible = section.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expandable_settings_section_${section.id}"),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Expandable header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expandable_section_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = getSettingIcon(section.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (section.subtitle.isNotEmpty()) {
                            Text(
                                text = section.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.testTag("expand_collapse_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Expandable content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        if (section.items.isNotEmpty()) {
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                section.items.forEach { item ->
                                    SettingItem(
                                        item = item,
                                        onClick = { onItemClick(item.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps setting icon strings to Material Icons.
 */
private fun getSettingIcon(iconName: String): ImageVector {
    return when (iconName) {
        "health" -> Icons.Default.Favorite
        "privacy" -> Icons.Default.Lock
        "accessibility" -> Icons.Default.Settings
        "support" -> Icons.Default.Info
        "account" -> Icons.Default.Person
        else -> Icons.Default.Settings
    }
}