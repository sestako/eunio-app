package com.eunio.healthapp.android.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eunio.healthapp.services.CrashlyticsService
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashlyticsTestScreen(
    onBack: () -> Unit
) {
    val crashlyticsService: CrashlyticsService = koinInject()
    var logMessage by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crashlytics Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Test Crashlytics Integration",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                "⚠️ Warning: The 'Test Crash' button will crash the app!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            
            Divider()
            
            // Log Message
            Text("Log Message", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = logMessage,
                onValueChange = { logMessage = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    if (logMessage.isNotBlank()) {
                        crashlyticsService.log(logMessage)
                        logMessage = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Message")
            }
            
            Divider()
            
            // Non-Fatal Exception
            Text("Non-Fatal Exception", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    val exception = RuntimeException("Test non-fatal exception")
                    crashlyticsService.recordException(exception)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Record Non-Fatal Exception")
            }
            
            Divider()
            
            // Custom Keys
            Text("Custom Keys", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    crashlyticsService.setCustomKey("test_key", "test_value")
                    crashlyticsService.setCustomKey("test_number", 42)
                    crashlyticsService.setCustomKey("test_boolean", true)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Custom Keys")
            }
            
            Divider()
            
            // Test Crash
            Text("⚠️ Fatal Crash Test", style = MaterialTheme.typography.titleMedium)
            Text(
                "This will crash the app. Reopen the app after crash to send the report.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    crashlyticsService.testCrash()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("⚠️ TEST CRASH (Will Crash App!)")
            }
            
            Divider()
            
            Text(
                "How to verify:\n" +
                "1. Perform actions above\n" +
                "2. Go to Firebase Console → Crashlytics\n" +
                "3. Check for crashes and non-fatal exceptions\n" +
                "4. For fatal crashes, reopen the app to send the report",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
