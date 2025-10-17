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
import com.eunio.healthapp.models.UserProfile
import com.eunio.healthapp.services.AndroidUserProfileService
import com.eunio.healthapp.services.UserProfileService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTestScreen(
    onBack: () -> Unit,
    viewModel: ProfileTestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile Test") },
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
            // Current User Info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Current User", style = MaterialTheme.typography.titleMedium)
                    Text("User ID: ${uiState.currentUserId ?: "Not signed in"}")
                    Text("Email: ${uiState.currentEmail ?: "N/A"}")
                }
            }
            
            // Test Actions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Test Actions", style = MaterialTheme.typography.titleMedium)
                    
                    Button(
                        onClick = { viewModel.loadProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentUserId != null && !uiState.isLoading
                    ) {
                        Text("Load Profile from Firestore")
                    }
                    
                    Button(
                        onClick = { viewModel.createTestProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.currentUserId != null && !uiState.isLoading
                    ) {
                        Text("Create/Update Profile")
                    }
                    
                    Button(
                        onClick = { viewModel.updateDisplayName("Updated Name ${(kotlinx.datetime.Clock.System.now().toEpochMilliseconds() % 1000)}") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.profile != null && !uiState.isLoading
                    ) {
                        Text("Update Display Name")
                    }
                }
            }
            
            // Loading Indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Profile Data
            if (uiState.profile != null) {
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
                        Text("✅ Profile Found", style = MaterialTheme.typography.titleMedium)
                        Text("User ID: ${uiState.profile?.userId}")
                        Text("Email: ${uiState.profile?.email}")
                        Text("Display Name: ${uiState.profile?.displayName}")
                        Text("Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(uiState.profile?.createdAt ?: 0))}")
                        Text("Updated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(uiState.profile?.updatedAt ?: 0))}")
                    }
                }
            }
            
            // Error Message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌ Error", style = MaterialTheme.typography.titleMedium)
                        Text(uiState.error ?: "")
                    }
                }
            }
            
            // Success Message
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("✅ ${uiState.successMessage}")
                    }
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium)
                    Text("1. Make sure you're signed in")
                    Text("2. Click 'Load Profile' to check if profile exists")
                    Text("3. If no profile found, click 'Create/Update Profile'")
                    Text("4. Check Firebase Console → Firestore → users collection")
                    Text("5. Try 'Update Display Name' to test updates")
                }
            }
        }
    }
}

data class ProfileTestUiState(
    val currentUserId: String? = null,
    val currentEmail: String? = null,
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileTestViewModel : ViewModel() {
    
    private val profileService: UserProfileService = AndroidUserProfileService()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(ProfileTestUiState())
    val uiState: StateFlow<ProfileTestUiState> = _uiState
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        val user = auth.currentUser
        _uiState.value = _uiState.value.copy(
            currentUserId = user?.uid,
            currentEmail = user?.email
        )
    }
    
    fun loadProfile() {
        val userId = _uiState.value.currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            profileService.getProfile(userId)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false,
                        error = if (profile == null) "No profile found in Firestore" else null,
                        successMessage = if (profile != null) "Profile loaded successfully!" else null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load profile: ${error.message}"
                    )
                }
        }
    }
    
    fun createTestProfile() {
        val userId = _uiState.value.currentUserId ?: return
        val email = _uiState.value.currentEmail ?: "test@example.com"
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val profile = UserProfile.create(
                userId = userId,
                email = email,
                displayName = email.substringBefore("@")
            )
            
            profileService.createProfile(profile)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false,
                        successMessage = "Profile created/updated successfully!"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to create profile: ${error.message}"
                    )
                }
        }
    }
    
    fun updateDisplayName(newName: String) {
        val currentProfile = _uiState.value.profile ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            val updatedProfile = currentProfile.copy(
                displayName = newName,
                updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
            
            profileService.updateProfile(updatedProfile)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        profile = updatedProfile,
                        isLoading = false,
                        successMessage = "Display name updated successfully!"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to update profile: ${error.message}"
                    )
                }
        }
    }
}
