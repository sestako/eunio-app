package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.Validation

/**
 * Use case for sending password reset emails.
 * Handles password reset functionality with email validation.
 */
class SendPasswordResetUseCase(
    private val authService: AuthService
) {
    
    /**
     * Sends a password reset email to the specified email address
     * @param email User's email address
     * @return Result indicating success or validation/network error
     */
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        
        // Validate email format
        if (!Validation.isValidEmail(trimmedEmail)) {
            return Result.Error(AppError.ValidationError("Please enter a valid email address", "email"))
        }
        
        // Send password reset email
        return authService.sendPasswordResetEmail(trimmedEmail)
    }
}