package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.model.SyncStatus

/**
 * User profile section component with profile editing navigation.
 */
@Composable
fun UserProfileSection(
    userSettings: UserSettings,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("user_profile_section"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .clickable(
                    onClick = onEditProfile,
                    role = Role.Button
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile avatar
                ProfileAvatar(
                    userName = getUserDisplayName(userSettings),
                    modifier = Modifier.testTag("profile_avatar")
                )
                
                // Profile information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = getUserDisplayName(userSettings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = getUserSubtitle(userSettings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Sync status indicator
                    SyncStatusIndicator(
                        isSynced = userSettings.syncStatus == SyncStatus.SYNCED,
                        lastSyncTime = userSettings.lastModified,
                        modifier = Modifier.testTag("sync_status_indicator")
                    )
                }
                
                // Edit profile button
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Profile avatar component with user initials.
 */
@Composable
private fun ProfileAvatar(
    userName: String,
    modifier: Modifier = Modifier
) {
    val initials = getInitials(userName)
    
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Sync status indicator showing sync state and last sync time.
 */
@Composable
private fun SyncStatusIndicator(
    isSynced: Boolean,
    lastSyncTime: kotlinx.datetime.Instant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Refresh,
            contentDescription = if (isSynced) "Synced" else "Syncing",
            tint = if (isSynced) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(14.dp)
        )
        
        Text(
            text = if (isSynced) "Synced" else "Syncing...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Compact version of user profile section.
 */
@Composable
fun CompactUserProfileSection(
    userSettings: UserSettings,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("compact_user_profile_section"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onEditProfile,
                    role = Role.Button
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact profile avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(getUserDisplayName(userSettings)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // Compact profile information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = getUserDisplayName(userSettings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Tap to edit profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Compact edit icon
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Edit profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Profile section with additional user statistics.
 */
@Composable
fun DetailedUserProfileSection(
    userSettings: UserSettings,
    onEditProfile: () -> Unit,
    cycleCount: Int = 0,
    dataPoints: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("detailed_user_profile_section"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .clickable(
                    onClick = onEditProfile,
                    role = Role.Button
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileAvatar(
                    userName = getUserDisplayName(userSettings),
                    modifier = Modifier.size(64.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = getUserDisplayName(userSettings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = getUserSubtitle(userSettings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(onClick = onEditProfile) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // User statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UserStatItem(
                    label = "Cycles Tracked",
                    value = cycleCount.toString(),
                    icon = Icons.Default.DateRange
                )
                
                UserStatItem(
                    label = "Data Points",
                    value = dataPoints.toString(),
                    icon = Icons.Default.Star
                )
                
                UserStatItem(
                    label = "Days Active",
                    value = "30+", // Placeholder
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

/**
 * Individual user statistic item.
 */
@Composable
private fun UserStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Gets display name from user settings.
 */
private fun getUserDisplayName(userSettings: UserSettings): String {
    // This would typically come from user profile data
    // For now, return a placeholder
    return "User" // In real implementation, this would come from user profile
}

/**
 * Gets user subtitle from settings.
 */
private fun getUserSubtitle(userSettings: UserSettings): String {
    // This could show health goals, membership status, etc.
    return "Health tracking since 2024" // Placeholder
}

/**
 * Gets initials from a name.
 */
private fun getInitials(name: String): String {
    return name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "U" }
}