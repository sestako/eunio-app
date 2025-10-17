package com.eunio.healthapp.presentation.state

import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.usecase.profile.UserStatistics
import com.eunio.healthapp.presentation.viewmodel.UiState

/**
 * UI state for the profile management screen.
 */
data class ProfileManagementUiState(
    val user: User? = null,
    val statistics: UserStatistics? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Edit form state
    val isEditing: Boolean = false,
    val editedName: String = "",
    val editedEmail: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val hasUnsavedChanges: Boolean = false,
    
    // Health goal selection state
    val isSelectingGoal: Boolean = false,
    val selectedGoal: HealthGoal? = null,
    val goalChangeImpact: String = "",
    val showGoalConfirmation: Boolean = false,
    
    // Profile picture state
    val isUploadingPicture: Boolean = false,
    val profilePictureUrl: String? = null,
    val showPictureOptions: Boolean = false,
    
    // Email verification state
    val emailVerificationRequired: Boolean = false,
    val emailVerificationSent: Boolean = false,
    val emailVerificationError: String? = null
) : UiState {
    
    /**
     * Checks if the form has validation errors
     */
    fun hasValidationErrors(): Boolean {
        return nameError != null || emailError != null
    }
    
    /**
     * Checks if the save button should be enabled
     */
    fun canSave(): Boolean {
        return hasUnsavedChanges && !hasValidationErrors() && !isSaving
    }
    
    /**
     * Gets the display name for the current user
     */
    fun getDisplayName(): String {
        return if (isEditing) editedName else (user?.name ?: "")
    }
    
    /**
     * Gets the display email for the current user
     */
    fun getDisplayEmail(): String {
        return if (isEditing) editedEmail else (user?.email ?: "")
    }
    
    /**
     * Gets the current health goal display name
     */
    fun getCurrentGoalDisplayName(): String {
        val goal = selectedGoal ?: user?.primaryGoal
        return when (goal) {
            HealthGoal.CONCEPTION -> "Conception"
            HealthGoal.CONTRACEPTION -> "Contraception"
            HealthGoal.CYCLE_TRACKING -> "Cycle Tracking"
            HealthGoal.GENERAL_HEALTH -> "General Health"
            null -> "Not Set"
        }
    }
    
    /**
     * Gets the current health goal description
     */
    fun getCurrentGoalDescription(): String {
        val goal = selectedGoal ?: user?.primaryGoal
        return when (goal) {
            HealthGoal.CONCEPTION -> "Track fertility and optimize for conception"
            HealthGoal.CONTRACEPTION -> "Monitor fertility for natural contraception"
            HealthGoal.CYCLE_TRACKING -> "Understand and track menstrual cycles"
            HealthGoal.GENERAL_HEALTH -> "Monitor overall reproductive and general health"
            null -> "No health goal selected"
        }
    }
    
    /**
     * Gets account creation date formatted for display
     */
    fun getAccountCreationDate(): String {
        return statistics?.getAccountAgeString() ?: "Unknown"
    }
    
    /**
     * Gets last activity formatted for display
     */
    fun getLastActivity(): String {
        return statistics?.getLastActivityString() ?: "Unknown"
    }
    
    /**
     * Gets engagement level description
     */
    fun getEngagementDescription(): String {
        return when (statistics?.getEngagementLevel()) {
            com.eunio.healthapp.domain.usecase.profile.EngagementLevel.HIGH -> "Very Active User"
            com.eunio.healthapp.domain.usecase.profile.EngagementLevel.MEDIUM -> "Regular User"
            com.eunio.healthapp.domain.usecase.profile.EngagementLevel.LOW -> "Occasional User"
            com.eunio.healthapp.domain.usecase.profile.EngagementLevel.NEW -> "New User"
            null -> "Unknown"
        }
    }
    
    /**
     * Checks if email verification is needed for the current email change
     */
    fun needsEmailVerification(): Boolean {
        return emailVerificationRequired && editedEmail != user?.email
    }
    
    /**
     * Gets the appropriate success message based on the action
     */
    fun getDisplaySuccessMessage(): String? {
        return successMessage ?: when {
            emailVerificationSent -> "Verification email sent to ${editedEmail}"
            selectedGoal != null && selectedGoal != user?.primaryGoal -> "Health goal updated successfully"
            hasUnsavedChanges -> "Profile updated successfully"
            else -> null
        }
    }
}