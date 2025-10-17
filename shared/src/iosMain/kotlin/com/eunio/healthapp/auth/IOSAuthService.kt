package com.eunio.healthapp.auth

/**
 * iOS implementation of AuthService using Firebase Auth
 * Note: This is a stub implementation. The actual Firebase Auth calls
 * should be made from Swift code in the iOS app layer.
 */
class IOSAuthService : AuthService {
    
    override suspend fun signIn(email: String, password: String): Result<String> {
        // This will be implemented in Swift layer
        return Result.failure(Exception("Not implemented - use Swift implementation"))
    }
    
    override suspend fun signUp(email: String, password: String): Result<String> {
        // This will be implemented in Swift layer
        return Result.failure(Exception("Not implemented - use Swift implementation"))
    }
    
    override suspend fun signOut(): Result<Unit> {
        // This will be implemented in Swift layer
        return Result.failure(Exception("Not implemented - use Swift implementation"))
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> {
        // This will be implemented in Swift layer
        return Result.failure(Exception("Not implemented - use Swift implementation"))
    }
    
    override fun getCurrentUserId(): String? {
        // This will be implemented in Swift layer
        return null
    }
    
    override fun isSignedIn(): Boolean {
        // This will be implemented in Swift layer
        return false
    }
    
    override fun observeAuthState(callback: (String?) -> Unit) {
        // This will be implemented in Swift layer
    }
}
