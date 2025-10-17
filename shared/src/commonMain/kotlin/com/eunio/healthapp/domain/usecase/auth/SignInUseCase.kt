package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.Validation

/**
 * Use case for user authentication.
 * Handles user sign-in with email and password validation.
 */
class SignInUseCase(
    private val authService: AuthService
) {
    
    /**
     * Signs in a user with the provided credentials
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated user or validation/authentication error
     */
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validate email format
        if (!Validation.isValidEmail(email)) {
            return Result.error(AppError.ValidationError("Please enter a valid email address", "email"))
        }
        
        // Validate password is not empty
        if (password.isBlank()) {
            return Result.error(AppError.ValidationError("Password cannot be empty", "password"))
        }
        
        // Attempt to sign in
        return authService.signIn(email.trim(), password)
    }
}