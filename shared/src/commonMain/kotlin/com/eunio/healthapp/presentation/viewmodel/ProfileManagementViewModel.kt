package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.usecase.auth.GetCurrentUserUseCase
import com.eunio.healthapp.domain.usecase.profile.GetUserStatisticsUseCase
import com.eunio.healthapp.domain.usecase.profile.UpdateHealthGoalUseCase
import com.eunio.healthapp.domain.usecase.profile.UpdateUserProfileUseCase
import com.eunio.healthapp.presentation.state.ProfileManagementUiState
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profile information and settings.
 */
class ProfileManagementViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val updateHealthGoalUseCase: UpdateHealthGoalUseCase,
    private val getUserStatisticsUseCase: GetUserStatisticsUseCase
) : BaseViewModel<ProfileManagementUiState>() {
    
    override val initialState = ProfileManagementUiState()
    
    /**
     * Loads user profile and statistics
     */
    fun loadProfile() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Load user data
                val userResult = getCurrentUserUseCase()
                if (userResult.isError) {
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load profile: ${userResult.errorOrNull()?.message}"
                        )
                    }
                    return@launch
                }
                
                val user = userResult.getOrNull()
                if (user == null) {
                    updateState { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "User not found"
                        )
                    }
                    return@launch
                }
                
                // Load statistics
                val statisticsResult = getUserStatisticsUseCase()
                val statistics = statisticsResult.getOrNull()
                
                updateState { 
                    it.copy(
                        isLoading = false,
                        user = user,
                        statistics = statistics,
                        editedName = user.name,
                        editedEmail = user.email,
                        selectedGoal = user.primaryGoal
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load profile: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Starts editing the profile
     */
    fun startEditing() {
        val currentUser = uiState.value.user ?: return
        
        updateState { 
            it.copy(
                isEditing = true,
                editedName = currentUser.name,
                editedEmail = currentUser.email,
                nameError = null,
                emailError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }
    
    /**
     * Cancels editing and reverts changes
     */
    fun cancelEditing() {
        val currentUser = uiState.value.user ?: return
        
        updateState { 
            it.copy(
                isEditing = false,
                editedName = currentUser.name,
                editedEmail = currentUser.email,
                nameError = null,
                emailError = null,
                hasUnsavedChanges = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }
    
    /**
     * Updates the edited name
     */
    fun updateName(name: String) {
        val currentUser = uiState.value.user ?: return
        val nameError = validateName(name)
        
        updateState { 
            it.copy(
                editedName = name,
                nameError = nameError,
                hasUnsavedChanges = name != currentUser.name || it.editedEmail != currentUser.email
            )
        }
    }
    
    /**
     * Updates the edited email
     */
    fun updateEmail(email: String) {
        val currentUser = uiState.value.user ?: return
        val emailError = validateEmail(email)
        
        updateState { 
            it.copy(
                editedEmail = email,
                emailError = emailError,
                hasUnsavedChanges = it.editedName != currentUser.name || email != currentUser.email,
                emailVerificationRequired = email != currentUser.email
            )
        }
    }
    
    /**
     * Saves the profile changes
     */
    fun saveProfile() {
        val currentState = uiState.value
        val currentUser = currentState.user ?: return
        
        if (currentState.hasValidationErrors()) return
        
        viewModelScope.launch {
            updateState { it.copy(isSaving = true, errorMessage = null) }
            
            try {
                val result = updateUserProfileUseCase(
                    userId = currentUser.id,
                    name = if (currentState.editedName != currentUser.name) currentState.editedName else null,
                    email = if (currentState.editedEmail != currentUser.email) currentState.editedEmail else null
                )
                
                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()!!
                    
                    updateState { 
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            user = updatedUser,
                            hasUnsavedChanges = false,
                            successMessage = "Profile updated successfully",
                            emailVerificationSent = currentState.needsEmailVerification()
                        )
                    }
                } else {
                    updateState { 
                        it.copy(
                            isSaving = false,
                            errorMessage = "Failed to update profile: ${result.errorOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to update profile: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Starts health goal selection
     */
    fun startGoalSelection() {
        updateState { 
            it.copy(
                isSelectingGoal = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }
    
    /**
     * Cancels health goal selection
     */
    fun cancelGoalSelection() {
        updateState { 
            it.copy(
                isSelectingGoal = false,
                selectedGoal = it.user?.primaryGoal,
                goalChangeImpact = "",
                showGoalConfirmation = false
            )
        }
    }
    
    /**
     * Selects a new health goal
     */
    fun selectHealthGoal(goal: HealthGoal) {
        val currentUser = uiState.value.user ?: return
        val impact = updateHealthGoalUseCase.getGoalChangeImpact(currentUser.primaryGoal, goal)
        
        updateState { 
            it.copy(
                selectedGoal = goal,
                goalChangeImpact = impact,
                showGoalConfirmation = goal != currentUser.primaryGoal
            )
        }
    }
    
    /**
     * Confirms the health goal change
     */
    fun confirmGoalChange() {
        val currentState = uiState.value
        val currentUser = currentState.user ?: return
        val newGoal = currentState.selectedGoal ?: return
        
        viewModelScope.launch {
            updateState { it.copy(isSaving = true, errorMessage = null) }
            
            try {
                val result = updateHealthGoalUseCase(currentUser.id, newGoal)
                
                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()!!
                    
                    updateState { 
                        it.copy(
                            isSaving = false,
                            isSelectingGoal = false,
                            showGoalConfirmation = false,
                            user = updatedUser,
                            successMessage = "Health goal updated successfully"
                        )
                    }
                } else {
                    updateState { 
                        it.copy(
                            isSaving = false,
                            errorMessage = "Failed to update health goal: ${result.errorOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to update health goal: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Gets available health goals with descriptions
     */
    fun getAvailableGoals(): List<Pair<HealthGoal, String>> {
        return updateHealthGoalUseCase.getAvailableGoals()
    }
    
    /**
     * Shows profile picture options
     */
    fun showPictureOptions() {
        updateState { it.copy(showPictureOptions = true) }
    }
    
    /**
     * Hides profile picture options
     */
    fun hidePictureOptions() {
        updateState { it.copy(showPictureOptions = false) }
    }
    
    /**
     * Uploads a new profile picture (placeholder implementation)
     */
    fun uploadProfilePicture(imageData: ByteArray) {
        viewModelScope.launch {
            updateState { it.copy(isUploadingPicture = true, showPictureOptions = false) }
            
            try {
                // TODO: Implement actual image upload to Firebase Storage
                // For now, just simulate the upload
                kotlinx.coroutines.delay(2000)
                
                updateState { 
                    it.copy(
                        isUploadingPicture = false,
                        profilePictureUrl = "https://example.com/profile.jpg", // Placeholder
                        successMessage = "Profile picture updated successfully"
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isUploadingPicture = false,
                        errorMessage = "Failed to upload profile picture: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Removes the profile picture
     */
    fun removeProfilePicture() {
        updateState { 
            it.copy(
                profilePictureUrl = null,
                showPictureOptions = false,
                successMessage = "Profile picture removed"
            )
        }
    }
    
    /**
     * Clears any error or success messages
     */
    fun clearMessages() {
        updateState { 
            it.copy(
                errorMessage = null,
                successMessage = null,
                emailVerificationError = null
            )
        }
    }
    
    /**
     * Validates name input
     */
    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name cannot be empty"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name cannot exceed 50 characters"
            else -> null
        }
    }
    
    /**
     * Validates email input
     */
    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email cannot be empty"
        
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return if (emailRegex.matches(email)) null else "Please enter a valid email address"
    }
}