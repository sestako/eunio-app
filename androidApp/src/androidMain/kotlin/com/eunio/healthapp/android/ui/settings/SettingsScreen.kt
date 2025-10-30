package com.eunio.healthapp.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.auth.AuthService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Settings screen with user preferences and account management.
 * TODO: Implement full settings functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    authService: AuthService = koinInject()
) {
    val scope = rememberCoroutineScope()
    var isSigningOut by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Account Section
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile",
                    subtitle = "Manage your profile information",
                    onClick = { /* TODO: Navigate to profile */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    onClick = { showSignOutDialog = true },
                    enabled = !isSigningOut
                )
            }
            
            Divider()
            
            // Preferences Section
            SettingsSection(title = "Preferences") {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notification preferences",
                    onClick = { /* TODO: Navigate to notifications */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    subtitle = "Theme and display settings",
                    onClick = { /* TODO: Navigate to appearance */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "App language and region",
                    onClick = { /* TODO: Navigate to language */ }
                )
            }
            
            Divider()
            
            // Health Data Section
            SettingsSection(title = "Health Data") {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Sync Settings",
                    subtitle = "Manage data synchronization",
                    onClick = { /* TODO: Navigate to sync settings */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy",
                    subtitle = "Data privacy and security",
                    onClick = { /* TODO: Navigate to privacy */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Export Data",
                    subtitle = "Download your health data",
                    onClick = { /* TODO: Export data */ }
                )
            }
            
            Divider()
            
            // Support Section
            SettingsSection(title = "Support") {
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    onClick = { /* TODO: Navigate to help */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App version and information",
                    onClick = { /* TODO: Navigate to about */ }
                )
            }
        }
    }
    
    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        isSigningOut = true
                        scope.launch {
                            try {
                                authService.signOut()
                                onSignOut()
                            } catch (e: Exception) {
                                // Handle error
                                isSigningOut = false
                            }
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
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
