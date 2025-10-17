package com.eunio.healthapp.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.presentation.state.ErrorState

/**
 * Error boundary component that catches and displays errors in a user-friendly way.
 * Provides retry mechanisms and helpful error messages.
 */
@Composable
fun ErrorBoundary(
    errorState: ErrorState?,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (errorState != null) {
        ErrorDisplay(
            errorState = errorState,
            onRetry = onRetry,
            onDismiss = onDismiss,
            modifier = modifier
        )
    } else {
        content()
    }
}

/**
 * Displays error information with appropriate actions
 */
@Composable
private fun ErrorDisplay(
    errorState: ErrorState,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error icon
            Icon(
                imageVector = when (errorState) {
                    is ErrorState.NetworkError -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )
            
            // Error title
            Text(
                text = getErrorTitle(errorState),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // User-friendly error message
            Text(
                text = errorState.userFriendlyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            // Action suggestions
            val suggestions = getActionSuggestions(errorState)
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What you can do:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                suggestions.forEach { suggestion ->
                    Text(
                        text = "• $suggestion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                // Retry button
                if (errorState.isRetryable && onRetry != null) {
                    FilledTonalButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
                
                // Dismiss button
                if (onDismiss != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

/**
 * Error dialog for critical errors that need immediate attention
 */
@Composable
fun ErrorDialog(
    errorState: ErrorState,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = getErrorTitle(errorState),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = errorState.userFriendlyMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val suggestions = getActionSuggestions(errorState)
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Suggestions:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    suggestions.take(2).forEach { suggestion ->
                        Text(
                            text = "• $suggestion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (errorState.isRetryable && onRetry != null) {
                FilledTonalButton(onClick = onRetry) {
                    Text("Try Again")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        modifier = modifier
    )
}

/**
 * Inline error message for form fields and smaller components
 */
@Composable
fun InlineErrorMessage(
    errorState: ErrorState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = errorState.userFriendlyMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Loading state with error fallback
 */
@Composable
fun LoadingWithErrorFallback(
    isLoading: Boolean,
    errorState: ErrorState?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    },
    content: @Composable () -> Unit
) {
    when {
        isLoading -> loadingContent()
        errorState != null -> ErrorBoundary(
            errorState = errorState,
            onRetry = onRetry,
            modifier = modifier,
            content = content
        )
        else -> content()
    }
}

private fun getErrorTitle(errorState: ErrorState): String {
    return when (errorState) {
        is ErrorState.NetworkError -> "Connection Issue"
        is ErrorState.ValidationError -> "Invalid Input"
        is ErrorState.SyncError -> "Sync Failed"
        is ErrorState.NotificationError -> "Notification Issue"
        is ErrorState.ExportError -> "Export Failed"
        is ErrorState.BackupError -> if (errorState.isRestoreError) "Restore Failed" else "Backup Failed"
        is ErrorState.GenericError -> "Something Went Wrong"
    }
}

private fun getActionSuggestions(errorState: ErrorState): List<String> {
    return when (errorState) {
        is ErrorState.NetworkError -> listOf(
            "Check your internet connection",
            "Try again in a few moments"
        )
        is ErrorState.ValidationError -> listOf(
            "Review your input",
            "Check required fields"
        )
        is ErrorState.SyncError -> listOf(
            "Your changes are saved locally",
            "Sync will resume when connection is restored"
        )
        is ErrorState.NotificationError -> if (errorState.requiresPermission) {
            listOf("Enable notification permissions in device settings")
        } else {
            listOf("Try again", "Check notification settings")
        }
        is ErrorState.ExportError -> listOf(
            "Ensure you have enough storage space",
            "Try exporting to a different location"
        )
        is ErrorState.BackupError -> if (errorState.isRestoreError) {
            listOf("Try importing settings manually", "Check if backup file is valid")
        } else {
            listOf("Your data is safe locally", "Try backup again later")
        }
        is ErrorState.GenericError -> listOf(
            "Try again",
            "Restart the app if issue persists"
        )
    }
}