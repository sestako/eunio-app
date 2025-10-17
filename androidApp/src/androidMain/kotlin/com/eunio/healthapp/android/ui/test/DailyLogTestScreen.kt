package com.eunio.healthapp.android.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.services.AndroidDailyLogService
import com.eunio.healthapp.services.DailyLogService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogTestScreen(
    onBack: () -> Unit,
    viewModel: DailyLogTestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Log Test") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current User
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Current User", style = MaterialTheme.typography.titleMedium)
                    Text("User ID: ${uiState.currentUserId ?: "Not signed in"}")
                }
            }
            
            // Test Actions
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Test Actions", style = MaterialTheme.typography.titleMedium)
                    
                    Button(
                        onClick = { viewModel.createTestLog() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentUserId != null && !uiState.isLoading
                    ) {
                        Text("Create Test Daily Log")
                    }
                    
                    Button(
                        onClick = { viewModel.loadTodayLogs() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentUserId != null && !uiState.isLoading
                    ) {
                        Text("Load Today's Logs")
                    }
                    
                    Button(
                        onClick = { viewModel.loadThisMonthLogs() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentUserId != null && !uiState.isLoading
                    ) {
                        Text("Load This Month's Logs")
                    }
                    
                    Button(
                        onClick = { viewModel.updateFirstLog() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.logs.isNotEmpty() && !uiState.isLoading
                    ) {
                        Text("Update First Log")
                    }
                    
                    Button(
                        onClick = { viewModel.deleteFirstLog() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.logs.isNotEmpty() && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete First Log")
                    }
                }
            }
            
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Logs Display
            if (uiState.logs.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✅ Found ${uiState.logs.size} Log(s)", style = MaterialTheme.typography.titleMedium)
                        uiState.logs.forEach { log ->
                            Divider()
                            Text("Date: ${log.date}")
                            log.periodFlow?.let { Text("Period: $it") }
                            if (log.symptoms.isNotEmpty()) {
                                Text("Symptoms: ${log.symptoms.joinToString()}")
                            }
                            log.mood?.let { Text("Mood: $it") }
                            log.sexualActivity?.let { Text("Sexual Activity: ${if (it.occurred) "Yes (${it.protection ?: "No protection"})" else "No"}") }
                            log.bbt?.let { Text("BBT: $it°C") }
                            log.cervicalMucus?.let { Text("Cervical Mucus: $it") }
                            log.opkResult?.let { Text("OPK: $it") }
                            log.notes?.let { if (it.isNotEmpty()) Text("Notes: $it") }
                        }
                    }
                }
            }
            
            // Error
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "❌ ${uiState.error}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Success
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        "✅ ${uiState.successMessage}",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Instructions
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium)
                    Text("1. Click 'Create Test Daily Log' to add a log")
                    Text("2. Click 'Load Today's Logs' to see today's data")
                    Text("3. Check Firebase Console → daily_logs collection")
                    Text("4. Test on iOS to verify cross-platform sync")
                }
            }
        }
    }
}

data class DailyLogTestUiState(
    val currentUserId: String? = null,
    val logs: List<DailyLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Test ViewModel for direct Firebase testing.
 * 
 * ⚠️ NOTE: This ViewModel directly uses AndroidDailyLogService for testing purposes only.
 * Production code uses DailyLoggingViewModel which delegates to shared Kotlin UseCases.
 * 
 * Production architecture:
 * DailyLoggingViewModel → GetDailyLogUseCase/SaveDailyLogUseCase → LogRepository → Firebase
 * 
 * This test architecture:
 * DailyLogTestViewModel → AndroidDailyLogService → Firebase (bypasses repository layer)
 */
class DailyLogTestViewModel : ViewModel() {
    
    private val logService: DailyLogService = AndroidDailyLogService()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(DailyLogTestUiState())
    val uiState: StateFlow<DailyLogTestUiState> = _uiState
    
    init {
        _uiState.value = _uiState.value.copy(
            currentUserId = auth.currentUser?.uid
        )
    }
    
    fun createTestLog() {
        val userId = _uiState.value.currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            val testLog = DailyLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                date = today,
                periodFlow = PeriodFlow.MEDIUM,
                symptoms = listOf(Symptom.CRAMPS, Symptom.FATIGUE),
                mood = Mood.HAPPY,
                sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
                bbt = 36.7,
                cervicalMucus = CervicalMucus.EGG_WHITE,
                opkResult = OPKResult.POSITIVE,
                notes = "Test log from Android - All fields",
                createdAt = now,
                updatedAt = now
            )
            
            logService.createLog(testLog)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Daily log created successfully!"
                    )
                    loadTodayLogs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create log: ${error.message}"
                    )
                }
        }
    }
    
    fun loadTodayLogs() {
        val userId = _uiState.value.currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            logService.getLogsByDateRange(userId, today, today)
                .onSuccess { logs ->
                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        isLoading = false,
                        successMessage = if (logs.isEmpty()) "No logs found for today" else "Loaded ${logs.size} log(s)"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load logs: ${error.message}"
                    )
                }
        }
    }
    
    fun loadThisMonthLogs() {
        val userId = _uiState.value.currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val startOfMonth = kotlinx.datetime.LocalDate(now.year, now.month, 1)
            val endOfMonth = kotlinx.datetime.LocalDate(now.year, now.month, now.month.length(false))
            
            logService.getLogsByDateRange(userId, startOfMonth, endOfMonth)
                .onSuccess { logs ->
                    _uiState.value = _uiState.value.copy(
                        logs = logs,
                        isLoading = false,
                        successMessage = if (logs.isEmpty()) "No logs found this month" else "Loaded ${logs.size} log(s)"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load logs: ${error.message}"
                    )
                }
        }
    }
    
    fun updateFirstLog() {
        val firstLog = _uiState.value.logs.firstOrNull() ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val updatedLog = firstLog.copy(
                notes = "Updated at ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}",
                updatedAt = Clock.System.now()
            )
            
            logService.updateLog(updatedLog)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Log updated successfully!"
                    )
                    loadTodayLogs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update log: ${error.message}"
                    )
                }
        }
    }
    
    fun deleteFirstLog() {
        val userId = _uiState.value.currentUserId ?: return
        val firstLog = _uiState.value.logs.firstOrNull() ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            logService.deleteLog(userId, firstLog.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Log deleted successfully!"
                    )
                    loadTodayLogs()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete log: ${error.message}"
                    )
                }
        }
    }
}
