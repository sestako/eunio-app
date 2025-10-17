package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.Validation

/**
 * Use case for user registration.
 * Handles user sign-up with email and password validation.
 */
class SignUpUseCase(
    private val authService: AuthService
) {
    
    /**
     * Signs up a new user with the provided credentials
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @return Result containing the created user or validation/authentication error
     */
    suspend operator fun invoke(email: String, password: String, name: String): Result<User> {
        // Validate email format
        if (!Validation.isValidEmail(email)) {
            return Result.error(AppError.ValidationError("Please enter a valid email address", "email"))
        }
        
        // Validate password strength
        val passwordValidation = Validation.validatePassword(password)
        if (!passwordValidation.isValid) {
            return Result.error(AppError.ValidationError(passwordValidation.message, "password"))
        }
        
        // Validate name
        if (name.isBlank() || name.length < 2) {
            return Result.error(AppError.ValidationError("Name must be at least 2 characters long", "name"))
        }
        
        // Attempt to sign up
        return authService.signUp(email.trim(), password, name.trim())
    }
}