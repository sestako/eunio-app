package com.eunio.healthapp.domain.usecase.auth

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.util.Result

/**
 * Use case for user sign out.
 * Handles user logout and session cleanup.
 */
class SignOutUseCase(
    private val authService: AuthService
) {
    
    /**
     * Signs out the current user
     * @return Result indicating success or error
     */
    suspend operator fun invoke(): Result<Unit> {
        return authService.signOut()
    }
}