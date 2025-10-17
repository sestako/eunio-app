package com.eunio.healthapp.android.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.android.ui.test.ProfileTestScreen
import com.eunio.healthapp.android.ui.test.DailyLogTestScreen
import com.eunio.healthapp.android.ui.components.OfflineBanner
import com.eunio.healthapp.auth.AuthService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main app screen with sign out functionality for testing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    authService: AuthService = koinInject()
) {
    val scope = rememberCoroutineScope()
    var isSigningOut by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showProfileTest by remember { mutableStateOf(false) }
    var showDailyLogTest by remember { mutableStateOf(false) }
    var showCrashlyticsTest by remember { mutableStateOf(false) }
    var showDailyLogging by remember { mutableStateOf(false) }
    
    // Show real daily logging screen if requested
    if (showDailyLogging) {
        com.eunio.healthapp.android.ui.logging.DailyLoggingScreen(
            onNavigateBack = { showDailyLogging = false }
        )
        return
    }
    
    // Show profile test screen if requested
    if (showProfileTest) {
        ProfileTestScreen(
            onBack = { showProfileTest = false }
        )
        return
    }
    
    // Show daily log test screen if requested
    if (showDailyLogTest) {
        DailyLogTestScreen(
            onBack = { showDailyLogTest = false }
        )
        return
    }
    
    // Show crashlytics test screen if requested
    if (showCrashlyticsTest) {
        com.eunio.healthapp.android.ui.test.CrashlyticsTestScreen(
            onBack = { showCrashlyticsTest = false }
        )
        return
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Eunio Health") },
                    actions = {
                        IconButton(
                            onClick = { showSignOutDialog = true },
                            enabled = !isSigningOut
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                )
                OfflineBanner()
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Welcome to Eunio Health!",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Main app features coming soon...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Test Profile Integration Button
                Button(
                    onClick = { showProfileTest = true },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("🧪 Test User Profile")
                }
                
                // Real Daily Logging Screen Button
                Button(
                    onClick = { showDailyLogging = true },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("📝 Daily Logging")
                }
                
                // Test Daily Log Integration Button (Firebase testing)
                Button(
                    onClick = { showDailyLogTest = true },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("🧪 Test Daily Log (Firebase)")
                }
                
                // Test Crashlytics Button
                Button(
                    onClick = { showCrashlyticsTest = true },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("🧪 Test Crashlytics")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { showSignOutDialog = true },
                    enabled = !isSigningOut,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isSigningOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isSigningOut) "Signing Out..." else "Sign Out")
                }
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
