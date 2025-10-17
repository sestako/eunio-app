package com.eunio.healthapp.domain.manager

/**
 * Android-specific integration tests for AndroidAuthManager.
 * Extends the common AuthManagerIntegrationTest to ensure Android implementation works correctly.
 */
class AndroidAuthManagerIntegrationTest : AuthManagerIntegrationTest() {
    
    override fun createAuthManager(): AuthManager = AndroidAuthManager()
}