package com.eunio.healthapp.domain.manager

/**
 * iOS-specific integration tests for IOSAuthManager.
 * Extends the common AuthManagerIntegrationTest to ensure iOS implementation works correctly.
 */
class IOSAuthManagerIntegrationTest : AuthManagerIntegrationTest() {
    
    override fun createAuthManager(): AuthManager = IOSAuthManager()
}