package com.eunio.healthapp.android.ui.insights

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.theme.EunioColors
import com.eunio.healthapp.domain.model.Insight
import com.eunio.healthapp.domain.model.InsightType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Dismissible insight card component that displays health insights with appropriate styling
 * based on insight type and confidence level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightCard(
    insight: Insight,
    isRead: Boolean,
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(false) }
    
    AnimatedVisibility(
        visible = !isDismissed,
        exit = slideOutHorizontally() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = if (isRead) {
                    EunioColors.SurfaceVariant
                } else {
                    EunioColors.Surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isRead) 2.dp else 4.dp
            ),
            shape = MaterialTheme.shapes.medium,
            onClick = {
                if (!isRead) {
                    onMarkAsRead()
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with type, confidence, and dismiss button
                InsightCardHeader(
                    insight = insight,
                    isRead = isRead,
                    onDismiss = {
                        isDismissed = true
                        onDismiss()
                    }
                )
                
                // Main insight text
                Text(
                    text = insight.insightText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRead) {
                        EunioColors.OnSurfaceVariant
                    } else {
                        EunioColors.OnSurface
                    }
                )
                
                // Footer with metadata
                InsightCardFooter(
                    insight = insight,
                    isRead = isRead
                )
            }
        }
    }
}

@Composable
private fun InsightCardHeader(
    insight: Insight,
    isRead: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Type icon
            InsightTypeIcon(
                type = insight.type,
                isRead = isRead
            )
            
            // Type and confidence
            Column {
                Text(
                    text = insight.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = insight.type.color(isRead)
                )
                
                ConfidenceBadge(
                    confidence = insight.confidence,
                    isRead = isRead
                )
            }
        }
        
        // Dismiss button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss insight",
                tint = if (isRead) {
                    EunioColors.OnSurfaceVariant
                } else {
                    EunioColors.OnSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun InsightTypeIcon(
    type: InsightType,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor) = when (type) {
        InsightType.PATTERN_RECOGNITION -> Icons.Default.Star to EunioColors.Info
        InsightType.EARLY_WARNING -> Icons.Default.Warning to EunioColors.Warning
        InsightType.CYCLE_PREDICTION -> Icons.Default.Favorite to EunioColors.Primary
        InsightType.FERTILITY_WINDOW -> Icons.Default.Info to EunioColors.Secondary
    }
    
    Surface(
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = backgroundColor.copy(
            alpha = if (isRead) 0.3f else 0.6f
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isRead) {
                    EunioColors.OnSurfaceVariant
                } else {
                    Color.White
                },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ConfidenceBadge(
    confidence: Double,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val confidenceText = "${(confidence * 100).toInt()}% confidence"
    val confidenceColor = when {
        confidence >= 0.8 -> EunioColors.Success
        confidence >= 0.6 -> EunioColors.Warning
        else -> EunioColors.Error
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = confidenceColor.copy(
            alpha = if (isRead) 0.2f else 0.3f
        )
    ) {
        Text(
            text = confidenceText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isRead) {
                EunioColors.OnSurfaceVariant
            } else {
                confidenceColor
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InsightCardFooter(
    insight: Insight,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Generated date
        Text(
            text = formatInsightDate(insight.generatedDate),
            style = MaterialTheme.typography.labelSmall,
            color = if (isRead) {
                EunioColors.OnSurfaceVariant.copy(alpha = 0.7f)
            } else {
                EunioColors.OnSurfaceVariant
            }
        )
        
        // Actionable badge
        if (insight.actionable) {
            ActionableBadge(isRead = isRead)
        }
    }
}

@Composable
private fun ActionableBadge(
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = EunioColors.Secondary.copy(
            alpha = if (isRead) 0.2f else 0.3f
        )
    ) {
        Text(
            text = "Actionable",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isRead) {
                EunioColors.OnSurfaceVariant
            } else {
                EunioColors.Secondary
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Extension properties for InsightType to provide display information.
 */
private val InsightType.displayName: String
    get() = when (this) {
        InsightType.PATTERN_RECOGNITION -> "Pattern Recognition"
        InsightType.EARLY_WARNING -> "Health Alert"
        InsightType.CYCLE_PREDICTION -> "Cycle Prediction"
        InsightType.FERTILITY_WINDOW -> "Fertility Window"
    }

private fun InsightType.color(isRead: Boolean): Color {
    val baseColor = when (this) {
        InsightType.PATTERN_RECOGNITION -> EunioColors.Info
        InsightType.EARLY_WARNING -> EunioColors.Warning
        InsightType.CYCLE_PREDICTION -> EunioColors.Primary
        InsightType.FERTILITY_WINDOW -> EunioColors.Secondary
    }
    return if (isRead) {
        EunioColors.OnSurfaceVariant
    } else {
        baseColor
    }
}

/**
 * Formats the insight generation date for display.
 */
private fun formatInsightDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    
    return when {
        localDateTime.date == now.date -> "Today"
        localDateTime.date.dayOfYear == now.date.dayOfYear - 1 && localDateTime.date.year == now.date.year -> "Yesterday"
        else -> {
            val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            "${month} ${localDateTime.dayOfMonth}"
        }
    }
}