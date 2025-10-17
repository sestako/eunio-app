package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A composable that displays a preference section with title, subtitle, and content.
 * Used for organizing preference groups in settings screens.
 */
@Composable
fun PreferenceSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preference_section_${title.lowercase().replace(" ", "_")}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section header
                PreferenceSectionHeader(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Section content
                content()
            }
        }
    }
}

/**
 * Header for a preference section with optional icon.
 */
@Composable
private fun PreferenceSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("preference_section_header"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optional icon
        icon?.let { iconVector ->
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp) // Align with text baseline
            )
        }
        
        // Title and subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("section_title")
            )
            
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("section_subtitle")
                )
            }
        }
    }
}

/**
 * Compact version of PreferenceSection for smaller displays.
 */
@Composable
fun CompactPreferenceSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compact_preference_section_${title.lowercase().replace(" ", "_")}"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let { iconVector ->
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
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
            
            // Compact content
            content()
        }
    }
}

/**
 * Expandable preference section that can be collapsed/expanded.
 */
@Composable
fun ExpandablePreferenceSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    initiallyExpanded: Boolean = true,
    isVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expandable_preference_section_${title.lowercase().replace(" ", "_")}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expandable header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expandable_section_header"),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    icon?.let { iconVector ->
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Expand/collapse button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.testTag("expand_collapse_button")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) {
                                androidx.compose.material.icons.Icons.Default.KeyboardArrowLeft
                            } else {
                                androidx.compose.material.icons.Icons.Default.KeyboardArrowRight
                            },
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Simple preference section without card styling.
 */
@Composable
fun SimplePreferenceSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isVisible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("simple_preference_section_${title.lowercase().replace(" ", "_")}"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simple header
            PreferenceSectionHeader(
                title = title,
                subtitle = subtitle,
                icon = icon,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Content
            content()
        }
    }
}