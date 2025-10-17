package com.eunio.healthapp.android.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.presentation.state.SettingItem as SettingItemState

/**
 * Individual setting item component with click handling and navigation.
 */
@Composable
fun SettingItem(
    item: SettingItemState,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button
                )
                .testTag("setting_item_${item.id}"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Setting icon (if available)
                getSettingItemIcon(item.id)?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Setting content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Navigation chevron
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Navigate to ${item.title}",
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Maps setting item IDs to appropriate icons.
 */
private fun getSettingItemIcon(itemId: String): ImageVector? {
    return when (itemId) {
        "units" -> Icons.Default.Settings
        "notifications" -> Icons.Default.Notifications
        "cycle" -> Icons.Default.DateRange
        "privacy" -> Icons.Default.Lock
        "sync" -> Icons.Default.Refresh
        "display" -> Icons.Default.Settings
        "help" -> Icons.Default.Info
        "contact" -> Icons.Default.Email
        "profile" -> Icons.Default.Person
        else -> null
    }
}
/**
 
* Compact version of setting item for smaller displays.
 */
@Composable
fun CompactSettingItem(
    item: SettingItemState,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button
                )
                .testTag("compact_setting_item_${item.id}"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact setting content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Compact navigation chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate to ${item.title}",
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Setting item with toggle switch functionality.
 */
@Composable
fun ToggleSettingItem(
    item: SettingItemState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    enabled = enabled,
                    onClick = { onCheckedChange(!checked) },
                    role = Role.Switch
                )
                .testTag("toggle_setting_item_${item.id}"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Setting icon (if available)
                getSettingItemIcon(item.id)?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Setting content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Toggle switch
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                    modifier = Modifier.testTag("setting_toggle_${item.id}")
                )
            }
        }
    }
}

/**
 * Setting item with value display (e.g., current selection).
 */
@Composable
fun ValueSettingItem(
    item: SettingItemState,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = item.isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button
                )
                .testTag("value_setting_item_${item.id}"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Setting icon (if available)
                getSettingItemIcon(item.id)?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Setting content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (item.subtitle.isNotEmpty()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Current value
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    fontWeight = FontWeight.Medium
                )
                
                // Navigation chevron
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate to ${item.title}",
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}