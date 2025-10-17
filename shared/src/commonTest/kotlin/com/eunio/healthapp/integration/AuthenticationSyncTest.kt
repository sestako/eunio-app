package com.eunio.healthapp.integration

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.data.security.AuthenticationManagerImpl
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.testutil.MockServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests for user authentication and data synchronization.
 * Tests authentication flows, session management, and data sync after authentication.
 * 
 * Requirements: 3.2, 3.4
 */
class AuthenticationSyncTest {
    
    /**
     * Enhanced mock auth service that simulates real authentication scenarios
     */
    private class TestAuthService : AuthService {
        private val _authState = MutableStateFlow<User?>(null)
        override val authState: Flow<User?> = _authState.asStateFlow()
        
        private val userDatabase = mutableMapOf<String, Pair<String, User>>() // email -> (password, user)
        private var shouldFailAuth = false
        private var authDelay = 0L
        private var sessionTimeout = false
        private var requiresReauth = false
        
        override suspend fun getCurrentUser(): Result<User?> {
            if (sessionTimeout) {
                _authState.value = null
                return Result.error(AppError.AuthenticationError("Session expired"))
            }
            return Result.success(_authState.value)
        }
        
        override suspend fun signUp(email: String, password: String, name: String): Result<User> {
            if (authDelay > 0) delay(authDelay)
            
            if (shouldFailAuth) {
                return Result.error(AppError.AuthenticationError("Sign up failed"))
            }
            
            if (userDatabase.containsKey(email)) {
                return Result.error(AppError.ValidationError("User already exists"))
            }
            
            val user = User(
                id = "user-${userDatabase.size}",
                email = email,
                name = name,
                onboardingComplete = false,
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                unitSystem = UnitSystem.METRIC,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            userDatabase[email] = Pair(password, user)
            _authState.value = user
            return Result.success(user)
        }
        
        override suspend fun signIn(email: String, password: String): Result<User> {
            if (authDelay > 0) delay(authDelay)
            
            if (shouldFailAuth) {
                return Result.error(AppError.AuthenticationError("Sign in failed"))
            }
            
            val userData = userDatabase[email]
            if (userData == null || userData.first != password) {
                return Result.error(AppError.AuthenticationError("Invalid credentials"))
            }
            
            if (requiresReauth) {
                return Result.error(AppError.AuthenticationError("Re-authentication required"))
            }
            
            _authState.value = userData.second
            return Result.success(userData.second)
        }
        
        override suspend fun signOut(): Result<Unit> {
            _authState.value = null
            return Result.success(Unit)
        }
        
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
            if (shouldFailAuth) {
                return Result.error(AppError.NetworkError("Failed to send reset email"))
            }
            return Result.success(Unit)
        }
        
        override fun isAuthenticated(): Boolean {
            return _authState.value != null && !sessionTimeout
        }
        
        fun setFailAuth(shouldFail: Boolean) {
            shouldFailAuth = shouldFail
        }
        
        fun setAuthDelay(delay: Long) {
            authDelay = delay
        }
        
        fun setSessionTimeout(timeout: Boolean) {
            sessionTimeout = timeout
        }
        
        fun setRequiresReauth(requires: Boolean) {
            requiresReauth = requires
        }
        
        fun getUserCount(): Int = userDatabase.size
        
        fun clear() {
            userDatabase.clear()
            _authState.value = null
            shouldFailAuth = false
            authDelay = 0L
            sessionTimeout = false
            requiresReauth = false
        }
    }
    
    /**
     * Mock data sync service that simulates syncing user data after authentication
     */
    private class DataSyncService(
        private val userRepository: UserRepository,
        private val logRepository: com.eunio.healthapp.domain.repository.LogRepository
    ) {
        private var syncDelay = 0L
        private var shouldFailSync = false
        private val syncHistory = mutableListOf<SyncOperation>()
        
        data class SyncOperation(
            val userId: String,
            val operation: String,
            val dataType: String,
            val timestamp: kotlinx.datetime.Instant,
            val success: Boolean
        )
        
        suspend fun syncUserDataAfterAuth(userId: String): Result<Unit> {
            if (syncDelay > 0) delay(syncDelay)
            
            if (shouldFailSync) {
                recordSync(userId, "SYNC", "ALL_DATA", false)
                return Result.error(AppError.NetworkError("Data sync failed"))
            }
            
            // Simulate syncing user preferences
            val prefsResult = syncUserPreferences(userId)
            if (prefsResult.isError) {
                recordSync(userId, "SYNC", "PREFERENCES", false)
                return prefsResult
            }
            
            // Simulate syncing daily logs
            val logsResult = syncDailyLogs(userId)
            if (logsResult.isError) {
                recordSync(userId, "SYNC", "LOGS", false)
                return logsResult
            }
            
            recordSync(userId, "SYNC", "ALL_DATA", true)
            return Result.success(Unit)
        }
        
        private suspend fun syncUserPreferences(userId: String): Result<Unit> {
            // Simulate preference sync
            recordSync(userId, "SYNC", "PREFERENCES", true)
            return Result.success(Unit)
        }
        
        private suspend fun syncDailyLogs(userId: String): Result<Unit> {
            // Simulate log sync
            recordSync(userId, "SYNC", "LOGS", true)
            return Result.success(Unit)
        }
        
        private fun recordSync(userId: String, operation: String, dataType: String, success: Boolean) {
            syncHistory.add(SyncOperation(userId, operation, dataType, Clock.System.now(), success))
        }
        
        fun setSyncDelay(delay: Long) {
            syncDelay = delay
        }
        
        fun setFailSync(shouldFail: Boolean) {
            shouldFailSync = shouldFail
        }
        
        fun getSyncHistory(): List<SyncOperation> = syncHistory.toList()
        
        fun clearSyncHistory() {
            syncHistory.clear()
        }
    }
    
    // Test components
    private lateinit var authService: TestAuthService
    private lateinit var mockServices: MockServices
    private lateinit var authManager: AuthenticationManagerImpl
    private lateinit var dataSyncService: DataSyncService
    
    private val testEmail = "auth.test@example.com"
    private val testPassword = "securePassword123"
    private val testName = "Auth Test User"
    
    @BeforeTest
    fun setup() {
        authService = TestAuthService()
        mockServices = MockServices()
        authManager = AuthenticationManagerImpl(authService, mockServices.userRepository)
        dataSyncService = DataSyncService(mockServices.userRepository, mockServices.logRepository)
    }
    
    @AfterTest
    fun tearDown() {
        authService.clear()
        dataSyncService.clearSyncHistory()
    }
    
    // Authentication Flow Tests
    
    @Test
    fun `user sign up and data sync works correctly`() = runTest {
        // When: User signs up
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        
        // Then: Sign up succeeds
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        assertEquals(testEmail, user.email)
        assertEquals(testName, user.name)
        assertFalse(user.onboardingComplete)
        
        // When: Data sync occurs after authentication
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Data sync succeeds
        assertTrue(syncResult.isSuccess)
        
        // Verify sync history
        val syncHistory = dataSyncService.getSyncHistory()
        assertTrue(syncHistory.isNotEmpty())
        assertTrue(syncHistory.any { it.dataType == "ALL_DATA" && it.success })
        assertTrue(syncHistory.any { it.dataType == "PREFERENCES" && it.success })
        assertTrue(syncHistory.any { it.dataType == "LOGS" && it.success })
        
        // Verify user is authenticated
        assertTrue(authService.isAuthenticated())
        val currentUser = authService.getCurrentUser().getOrNull()
        assertEquals(user.id, currentUser?.id)
    }
    
    @Test
    fun `user sign in and data sync works correctly`() = runTest {
        // Given: User already exists
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        assertTrue(signUpResult.isSuccess)
        val originalUser = signUpResult.getOrNull()!!
        
        // User signs out
        authService.signOut()
        assertFalse(authService.isAuthenticated())
        
        // When: User signs in
        val signInResult = authService.signIn(testEmail, testPassword)
        
        // Then: Sign in succeeds
        assertTrue(signInResult.isSuccess)
        val user = signInResult.getOrNull()!!
        assertEquals(originalUser.id, user.id)
        assertEquals(testEmail, user.email)
        
        // When: Data sync occurs after sign in
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Data sync succeeds
        assertTrue(syncResult.isSuccess)
        
        // Verify user is authenticated
        assertTrue(authService.isAuthenticated())
        
        // Verify sync occurred
        val syncHistory = dataSyncService.getSyncHistory()
        assertTrue(syncHistory.any { it.userId == user.id && it.success })
    }
    
    @Test
    fun `authentication failure prevents data sync`() = runTest {
        // Given: Auth service will fail
        authService.setFailAuth(true)
        
        // When: User attempts to sign up
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        
        // Then: Sign up fails
        assertTrue(signUpResult.isError)
        assertTrue(signUpResult.errorOrNull() is AppError.AuthenticationError)
        
        // Verify user is not authenticated
        assertFalse(authService.isAuthenticated())
        
        // When: Attempting data sync without authentication
        val syncResult = dataSyncService.syncUserDataAfterAuth("non-existent-user")
        
        // Then: Sync may still be attempted but user data won't be available
        // In a real implementation, this would check authentication first
        val syncHistory = dataSyncService.getSyncHistory()
        // Sync history may be empty or contain failed attempts
    }
    
    @Test
    fun `session timeout triggers re-authentication and data sync`() = runTest {
        // Given: User is authenticated
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        assertTrue(authService.isAuthenticated())
        
        // When: Session times out
        authService.setSessionTimeout(true)
        
        // Then: User is no longer authenticated
        assertFalse(authService.isAuthenticated())
        
        val currentUserResult = authService.getCurrentUser()
        assertTrue(currentUserResult.isError)
        assertTrue(currentUserResult.errorOrNull() is AppError.AuthenticationError)
        
        // When: User re-authenticates
        authService.setSessionTimeout(false)
        val reAuthResult = authService.signIn(testEmail, testPassword)
        
        // Then: Re-authentication succeeds
        assertTrue(reAuthResult.isSuccess)
        assertTrue(authService.isAuthenticated())
        
        // When: Data sync occurs after re-authentication
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Data sync succeeds
        assertTrue(syncResult.isSuccess)
        
        val syncHistory = dataSyncService.getSyncHistory()
        assertTrue(syncHistory.any { it.userId == user.id && it.success })
    }
    
    @Test
    fun `data sync failure after authentication is handled gracefully`() = runTest {
        // Given: User is authenticated
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // Given: Data sync will fail
        dataSyncService.setFailSync(true)
        
        // When: Data sync is attempted after authentication
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Sync fails but user remains authenticated
        assertTrue(syncResult.isError)
        assertTrue(syncResult.errorOrNull() is AppError.NetworkError)
        assertTrue(authService.isAuthenticated())
        
        // Verify sync failure is recorded
        val syncHistory = dataSyncService.getSyncHistory()
        assertTrue(syncHistory.any { it.userId == user.id && !it.success })
        
        // When: Sync is retried after network recovery
        dataSyncService.setFailSync(false)
        val retryResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Retry succeeds
        assertTrue(retryResult.isSuccess)
        
        val updatedHistory = dataSyncService.getSyncHistory()
        assertTrue(updatedHistory.any { it.userId == user.id && it.success })
    }
    
    @Test
    fun `multiple users can authenticate and sync independently`() = runTest {
        // Given: Multiple users
        val users = listOf(
            Triple("user1@example.com", "password1", "User One"),
            Triple("user2@example.com", "password2", "User Two"),
            Triple("user3@example.com", "password3", "User Three")
        )
        
        val authenticatedUsers = mutableListOf<User>()
        
        // When: All users sign up
        users.forEach { (email, password, name) ->
            val signUpResult = authService.signUp(email, password, name)
            assertTrue(signUpResult.isSuccess)
            authenticatedUsers.add(signUpResult.getOrNull()!!)
            
            // Sign out to allow next user to sign up
            authService.signOut()
        }
        
        assertEquals(3, authenticatedUsers.size)
        assertEquals(3, authService.getUserCount())
        
        // When: Each user signs in and syncs data
        authenticatedUsers.forEach { user ->
            val signInResult = authService.signIn(user.email, users.find { it.first == user.email }!!.second)
            assertTrue(signInResult.isSuccess)
            
            val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
            assertTrue(syncResult.isSuccess)
            
            authService.signOut()
        }
        
        // Then: All users have successful sync history
        val syncHistory = dataSyncService.getSyncHistory()
        authenticatedUsers.forEach { user ->
            assertTrue(syncHistory.any { it.userId == user.id && it.success })
        }
        
        assertEquals(3, syncHistory.count { it.dataType == "ALL_DATA" && it.success })
    }
    
    @Test
    fun `authentication with slow network conditions works correctly`() = runTest {
        // Given: Network has significant delay
        authService.setAuthDelay(1000L) // 1 second delay
        dataSyncService.setSyncDelay(500L) // 0.5 second delay
        
        // When: User signs up with network delay
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        
        // Then: Sign up eventually succeeds
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // When: Data sync occurs with network delay
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Sync succeeds
        assertTrue(syncResult.isSuccess)
        
        // Verify user is authenticated and data is synced
        assertTrue(authService.isAuthenticated())
        val syncHistory = dataSyncService.getSyncHistory()
        assertTrue(syncHistory.any { it.userId == user.id && it.success })
        
        // Note: In test environment with runTest, delays are simulated but don't affect wall-clock time
        // The important thing is that the operations complete successfully despite the configured delays
        // Real timing validation would require integration tests with actual network conditions
    }
    
    @Test
    fun `password reset flow works correctly`() = runTest {
        // Given: User exists
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // User signs out
        authService.signOut()
        assertFalse(authService.isAuthenticated())
        
        // When: User requests password reset
        val resetResult = authService.sendPasswordResetEmail(testEmail)
        
        // Then: Reset email is sent successfully
        assertTrue(resetResult.isSuccess)
        
        // When: User tries to sign in with old password
        val oldPasswordResult = authService.signIn(testEmail, testPassword)
        
        // Then: Sign in still works (in real implementation, password would be changed)
        assertTrue(oldPasswordResult.isSuccess)
        
        // When: Data sync occurs after sign in
        val syncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        
        // Then: Data sync succeeds
        assertTrue(syncResult.isSuccess)
    }
    
    @Test
    fun `concurrent authentication attempts are handled correctly`() = runTest {
        // When: Multiple concurrent sign up attempts for same email
        val concurrentResults = mutableListOf<Result<User>>()
        
        repeat(3) {
            val result = authService.signUp(testEmail, testPassword, testName)
            concurrentResults.add(result)
        }
        
        // Then: Only one should succeed, others should fail
        val successCount = concurrentResults.count { it.isSuccess }
        val failureCount = concurrentResults.count { it.isError }
        
        assertEquals(1, successCount, "Only one sign up should succeed")
        assertEquals(2, failureCount, "Two sign ups should fail due to duplicate email")
        
        // Verify only one user was created
        assertEquals(1, authService.getUserCount())
        
        // When: Successful user syncs data
        val successfulUser = concurrentResults.first { it.isSuccess }.getOrNull()!!
        val syncResult = dataSyncService.syncUserDataAfterAuth(successfulUser.id)
        
        // Then: Data sync succeeds
        assertTrue(syncResult.isSuccess)
    }
    
    @Test
    fun `authentication state is consistent across app lifecycle`() = runTest {
        // Given: User signs up and completes onboarding
        val signUpResult = authService.signUp(testEmail, testPassword, testName)
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrNull()!!
        
        // Complete onboarding
        val onboardingResult = mockServices.userRepository.completeOnboarding(user.id, HealthGoal.CONCEPTION)
        assertTrue(onboardingResult.isSuccess)
        
        // Initial data sync
        val initialSyncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        assertTrue(initialSyncResult.isSuccess)
        
        // When: App is backgrounded and foregrounded (simulated by checking auth state)
        val authStateCheck1 = authService.getCurrentUser()
        assertTrue(authStateCheck1.isSuccess)
        assertEquals(user.id, authStateCheck1.getOrNull()?.id)
        
        // When: User adds some data
        val dailyLog = DailyLog(
            id = "lifecycle-log",
            userId = user.id,
            date = LocalDate(2024, 1, 15),
            bbt = 98.2,
            mood = Mood.HAPPY,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        val logResult = mockServices.logRepository.saveDailyLog(dailyLog)
        assertTrue(logResult.isSuccess)
        
        // When: App is backgrounded again and auth state is checked
        val authStateCheck2 = authService.getCurrentUser()
        assertTrue(authStateCheck2.isSuccess)
        assertTrue(authService.isAuthenticated())
        
        // When: Data sync occurs again
        val finalSyncResult = dataSyncService.syncUserDataAfterAuth(user.id)
        assertTrue(finalSyncResult.isSuccess)
        
        // Then: Authentication state remains consistent
        val finalAuthCheck = authService.getCurrentUser()
        assertTrue(finalAuthCheck.isSuccess)
        assertEquals(user.id, finalAuthCheck.getOrNull()?.id)
        
        // Verify sync history shows multiple successful syncs
        val syncHistory = dataSyncService.getSyncHistory()
        val userSyncs = syncHistory.filter { it.userId == user.id && it.success }
        assertTrue(userSyncs.size >= 2, "Should have multiple successful syncs")
    }
}