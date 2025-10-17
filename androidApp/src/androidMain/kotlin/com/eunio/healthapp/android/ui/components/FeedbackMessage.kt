package com.eunio.healthapp.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A composable that displays feedback messages with appropriate styling and icons.
 * Can be used for success, error, or informational messages.
 */
@Composable
fun FeedbackMessage(
    message: String,
    type: FeedbackType = FeedbackType.SUCCESS,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    autoDismissDelay: Long = 3000L
) {
    val backgroundColor = when (type) {
        FeedbackType.SUCCESS -> Color(0xFF4CAF50)
        FeedbackType.ERROR -> MaterialTheme.colors.error
        FeedbackType.INFO -> MaterialTheme.colors.primary
    }
    
    val icon = when (type) {
        FeedbackType.SUCCESS -> Icons.Default.Check
        FeedbackType.ERROR -> Icons.Default.Warning
        FeedbackType.INFO -> null
    }
    
    // Auto-dismiss after delay
    LaunchedEffect(message) {
        if (onDismiss != null && autoDismissDelay > 0) {
            delay(autoDismissDelay)
            onDismiss()
        }
    }
    
    Card(
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}

/**
 * Types of feedback messages with different visual styling.
 */
enum class FeedbackType {
    SUCCESS,
    ERROR,
    INFO
}