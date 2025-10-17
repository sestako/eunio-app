package com.eunio.healthapp.testutil

import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.data.remote.datasource.MockFirestoreService as ProperMockFirestoreService
import com.eunio.healthapp.data.security.AuthenticationManagerImpl
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock services for testing - simplified version without MockK
 */
class MockServices {
    val authService = MockAuthService()
    val userRepository = MockUserRepository()
    val logRepository = MockLogRepository()
    val cycleRepository = MockCycleRepository()
    val insightRepository = MockInsightRepository()
    val healthReportRepository = MockHealthReportRepository()
    val firestoreService = ProperMockFirestoreService()
    val databaseManager = MockDatabaseManager()
    val testDataFactory = TestDataFactory()
    
    val authManager = AuthenticationManagerImpl(authService, userRepository)
    
    val testDate = LocalDate(2024, 1, 15)
    
    fun createMockUser(userId: String): User {
        return TestDataFactory.createTestUser(userId)
    }
    
    fun createMockFirebaseUser(userId: String): User {
        return TestDataFactory.createTestUser(userId)
    }
    
    fun createUserPreferences(userId: String): UserPreferences {
        return TestDataFactory.createUserPreferences(userId)
    }
    
    fun createUser(userId: String): User {
        return TestDataFactory.createTestUser(userId)
    }
    
    val firebaseAuthService = authService
    
    // Add missing methods for tests
    fun setIdToken(token: String) {
        // Mock implementation for testing
    }
    
    fun createMockUserRepository(): MockUserRepository {
        return MockUserRepository()
    }
    
    fun createMockLogRepository(): MockLogRepository {
        return MockLogRepository()
    }
    
    fun createMockPlatformManager(): SimpleMockPlatformManager {
        return SimpleMockPlatformManager()
    }
    

    
    // Simple mock implementations without MockK
    private fun createMockFirestoreService(): ProperMockFirestoreService {
        return ProperMockFirestoreService()
    }
    
    /**
     * Reset all mock services to clean state for test isolation
     */
    fun reset() {
        userRepository.reset()
        logRepository.reset()
        cycleRepository.reset()
        insightRepository.reset()
        healthReportRepository.reset()
        // Reset other services as needed
    }
}

// Enhanced mock implementations
class MockUserRepository : UserRepository {
    private var currentUser: User? = null
    private val users = mutableMapOf<String, StoredUser>() // email -> StoredUser
    private val userSessions = mutableMapOf<String, UserSession>() // userId -> session
    private val passwordResetTokens = mutableMapOf<String, PasswordResetToken>() // email -> token
    
    // Internal data class to store user with password hash
    private data class StoredUser(
        val user: User,
        val passwordHash: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now()
    )
    
    private data class UserSession(
        val userId: String,
        val token: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        val expiresAt: kotlinx.datetime.Instant = Clock.System.now().plus(kotlin.time.Duration.parse("PT24H")),
        val lastAccessedAt: kotlinx.datetime.Instant = Clock.System.now(),
        val deviceId: String = "default-device",
        val isActive: Boolean = true
    )
    
    private data class PasswordResetToken(
        val token: String,
        val email: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        val expiresAt: kotlinx.datetime.Instant = Clock.System.now().plus(kotlin.time.Duration.parse("PT1H"))
    )
    
    override suspend fun getCurrentUser(): Result<User?> {
        // Clean up expired sessions first
        cleanupExpiredSessions()
        
        // If no current user, try to restore from session
        if (currentUser == null) {
            // In a real app, this would check persistent storage for valid sessions
            // For testing, we simulate this by checking if any active sessions exist
            val activeSessions = getAllActiveSessions()
            if (activeSessions.isNotEmpty()) {
                // Try to restore the most recently accessed session
                val mostRecentSession = activeSessions.maxByOrNull { it.lastAccessedAt }
                if (mostRecentSession != null) {
                    val restored = restoreSessionFromPersistentStorage(mostRecentSession.userId)
                    if (restored) {
                        return Result.Success(currentUser)
                    }
                }
            }
        }
        
        // If we have a current user, validate their session
        if (currentUser != null) {
            val sessionValid = isSessionValid(currentUser!!.id)
            if (!sessionValid) {
                // Session expired or invalid, clear current user
                currentUser = null
                return Result.Success(null)
            }
            
            // Update session access time
            updateSessionAccess(currentUser!!.id)
        }
        
        return Result.Success(currentUser)
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        // For testing purposes, allow user updates even without active session
        // In a real implementation, session validation would be required
        
        // Update the user in storage if they exist
        val storedUser = users.values.find { it.user.id == user.id }
        if (storedUser != null) {
            val updatedUser = user.copy(updatedAt = Clock.System.now())
            users[user.email] = storedUser.copy(user = updatedUser)
            
            // Update current user if it's the same user
            if (currentUser?.id == user.id) {
                currentUser = updatedUser
            }
            
            // Update session access time (if session exists)
            if (isSessionValid(user.id)) {
                updateSessionAccess(user.id)
                persistSessionToStorage(user.id)
            }
            
            return Result.Success(Unit)
        }
        
        // For testing purposes, create the user if it doesn't exist
        val updatedUser = user.copy(updatedAt = Clock.System.now())
        users[user.email] = StoredUser(updatedUser, simulatePasswordHash("testPassword"))
        currentUser = updatedUser
        
        return Result.Success(Unit)
    }
    
    override suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit> {
        // For testing purposes, allow onboarding completion even without active session
        // In a real implementation, session validation would be required
        
        // Find the user by ID and update their onboarding status
        val userEntry = users.entries.find { it.value.user.id == userId }
        if (userEntry != null) {
            val updatedUser = userEntry.value.user.copy(
                onboardingComplete = true,
                primaryGoal = primaryGoal,
                updatedAt = Clock.System.now()
            )
            users[userEntry.key] = userEntry.value.copy(user = updatedUser)
            
            // Update current user if it's the same user
            if (currentUser?.id == userId) {
                currentUser = updatedUser
            }
            
            // Update session access time to keep session active (if session exists)
            if (isSessionValid(userId)) {
                updateSessionAccess(userId)
                persistSessionToStorage(userId)
            }
            
            return Result.Success(Unit)
        }
        
        // For testing purposes, create a mock user if it doesn't exist
        val mockUser = User(
            id = userId,
            email = "test-$userId@example.com",
            name = "Test User $userId",
            onboardingComplete = true,
            primaryGoal = primaryGoal,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        users["test-$userId@example.com"] = StoredUser(mockUser, simulatePasswordHash("testPassword"))
        currentUser = mockUser
        
        return Result.Success(Unit)
    }
    
    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        // Input validation
        if (email.isBlank()) {
            return Result.Error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        if (password.isBlank()) {
            return Result.Error(AppError.ValidationError("Password cannot be empty", "password"))
        }
        if (name.isBlank()) {
            return Result.Error(AppError.ValidationError("Name cannot be empty", "name"))
        }
        
        // Email format validation (basic)
        if (!email.contains("@") || !email.contains(".")) {
            return Result.Error(AppError.ValidationError("Invalid email format", "email"))
        }
        
        // Password strength validation
        if (password.length < 8) {
            return Result.Error(AppError.ValidationError("Password must be at least 8 characters long", "password"))
        }
        
        // Check for malicious input
        if (email.contains("DROP TABLE") || email.contains("';") || email.contains("--")) {
            return Result.Error(AppError.ValidationError("Invalid email format", "email"))
        }
        
        // Check if user already exists
        if (users.containsKey(email.lowercase())) {
            return Result.Error(AppError.AuthenticationError("User with this email already exists"))
        }
        
        // Create new user with onboardingComplete = false initially
        val userId = "user_${Clock.System.now().toEpochMilliseconds()}_${users.size}"
        val newUser = User(
            id = userId,
            email = email,
            name = name,
            onboardingComplete = false, // Key fix: new users haven't completed onboarding
            primaryGoal = HealthGoal.CYCLE_TRACKING, // Default goal, will be set during onboarding
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Simulate password hashing
        val passwordHash = simulatePasswordHash(password)
        
        // Store user
        users[email.lowercase()] = StoredUser(newUser, passwordHash)
        
        // Create session for the new user
        val session = UserSession(
            userId = userId,
            token = generateSessionToken(),
            createdAt = Clock.System.now(),
            expiresAt = Clock.System.now().plus(kotlin.time.Duration.parse("PT24H")),
            lastAccessedAt = Clock.System.now(),
            deviceId = "signup-device-${(1000..9999).random()}",
            isActive = true
        )
        userSessions[userId] = session
        
        // Set as current user
        currentUser = newUser
        
        // Persist session to simulate storage
        persistSessionToStorage(userId)
        
        return Result.Success(newUser)
    }
    
    override suspend fun signInUser(email: String, password: String): Result<User> {
        // Input validation
        if (email.isBlank()) {
            return Result.Error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        if (password.isBlank()) {
            return Result.Error(AppError.ValidationError("Password cannot be empty", "password"))
        }
        
        // Clean up expired sessions first
        cleanupExpiredSessions()
        
        // Find user by email
        val storedUser = users[email.lowercase()]
        if (storedUser == null) {
            return Result.Error(AppError.AuthenticationError("Invalid email or password"))
        }
        
        // Verify password
        if (!verifyPassword(password, storedUser.passwordHash)) {
            return Result.Error(AppError.AuthenticationError("Invalid email or password"))
        }
        
        // Check if user already has an active session
        val existingSession = userSessions[storedUser.user.id]
        if (existingSession != null && existingSession.isActive && Clock.System.now() < existingSession.expiresAt) {
            // Update existing session
            userSessions[storedUser.user.id] = existingSession.copy(
                lastAccessedAt = Clock.System.now(),
                token = generateSessionToken() // Generate new token for security
            )
        } else {
            // Create new session
            val session = UserSession(
                userId = storedUser.user.id,
                token = generateSessionToken(),
                createdAt = Clock.System.now(),
                expiresAt = Clock.System.now().plus(kotlin.time.Duration.parse("PT24H")),
                lastAccessedAt = Clock.System.now(),
                deviceId = "test-device-${(1000..9999).random()}",
                isActive = true
            )
            userSessions[storedUser.user.id] = session
        }
        
        // Set as current user
        currentUser = storedUser.user
        
        // Persist session to simulate storage
        persistSessionToStorage(storedUser.user.id)
        
        return Result.Success(storedUser.user)
    }
    
    override suspend fun signOutUser(): Result<Unit> {
        // Invalidate current user session instead of removing it
        // This allows for session history tracking
        currentUser?.let { user ->
            invalidateSession(user.id)
        }
        currentUser = null
        return Result.Success(Unit)
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        // For security reasons, user deletion should be restricted in most cases
        // This simulates a security policy where users cannot delete themselves directly
        return Result.Error(AppError.SecurityError("User deletion is restricted for security reasons"))
    }
    
    // Helper methods for password handling
    private fun simulatePasswordHash(password: String): String {
        // Simple hash simulation - in real implementation would use proper hashing
        return "hash_${password.hashCode()}_${Clock.System.now().toEpochMilliseconds()}"
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        // Simple verification simulation
        return hash.startsWith("hash_${password.hashCode()}_")
    }
    
    private fun generateSessionToken(): String {
        return "session_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    // Session Management Methods
    
    /**
     * Validates if a session is still active and not expired
     * @param userId User ID to check session for
     * @return true if session is valid and active
     */
    fun isSessionValid(userId: String): Boolean {
        val session = userSessions[userId] ?: return false
        val now = Clock.System.now()
        
        return session.isActive && now < session.expiresAt
    }
    
    /**
     * Updates the last accessed time for a session to keep it active
     * @param userId User ID to update session for
     */
    fun updateSessionAccess(userId: String) {
        val session = userSessions[userId]
        if (session != null && session.isActive) {
            userSessions[userId] = session.copy(lastAccessedAt = Clock.System.now())
        }
    }
    
    /**
     * Extends a session's expiration time
     * @param userId User ID to extend session for
     * @param extensionDuration Duration to extend the session by
     */
    fun extendSession(userId: String, extensionDuration: kotlin.time.Duration = kotlin.time.Duration.parse("PT24H")) {
        val session = userSessions[userId]
        if (session != null && session.isActive) {
            val newExpirationTime = Clock.System.now().plus(extensionDuration)
            userSessions[userId] = session.copy(
                expiresAt = newExpirationTime,
                lastAccessedAt = Clock.System.now()
            )
        }
    }
    
    /**
     * Invalidates a specific session
     * @param userId User ID to invalidate session for
     */
    fun invalidateSession(userId: String) {
        val session = userSessions[userId]
        if (session != null) {
            userSessions[userId] = session.copy(isActive = false)
        }
    }
    
    /**
     * Cleans up expired sessions
     * @return Number of sessions cleaned up
     */
    fun cleanupExpiredSessions(): Int {
        val now = Clock.System.now()
        val expiredSessions = userSessions.filter { (_, session) ->
            !session.isActive || now >= session.expiresAt
        }
        
        expiredSessions.keys.forEach { userId ->
            userSessions.remove(userId)
            // If the expired session belongs to current user, clear current user
            if (currentUser?.id == userId) {
                currentUser = null
            }
        }
        
        return expiredSessions.size
    }
    
    /**
     * Gets session information for a user
     * @param userId User ID to get session info for
     * @return Session information or null if no active session
     */
    fun getSessionInfo(userId: String): SessionInfo? {
        val session = userSessions[userId] ?: return null
        if (!session.isActive || Clock.System.now() >= session.expiresAt) {
            return null
        }
        
        return SessionInfo(
            userId = session.userId,
            token = session.token,
            createdAt = session.createdAt,
            expiresAt = session.expiresAt,
            lastAccessedAt = session.lastAccessedAt,
            deviceId = session.deviceId,
            isActive = session.isActive
        )
    }
    
    /**
     * Simulates app restart by attempting to restore session from persistent storage
     * @param userId User ID to restore session for
     * @return true if session was successfully restored
     */
    fun restoreSessionFromPersistentStorage(userId: String): Boolean {
        // In a real implementation, this would read from persistent storage
        // For testing, we simulate that sessions persist across app restarts
        val session = userSessions[userId]
        if (session != null && session.isActive && Clock.System.now() < session.expiresAt) {
            // Update last accessed time to simulate session restoration
            updateSessionAccess(userId)
            
            // Restore current user if session is valid
            val storedUser = users.values.find { it.user.id == userId }
            if (storedUser != null) {
                currentUser = storedUser.user
                return true
            }
        }
        return false
    }
    
    /**
     * Simulates persisting session to storage (for app restart simulation)
     * @param userId User ID to persist session for
     */
    fun persistSessionToStorage(userId: String) {
        // In a real implementation, this would write to persistent storage
        // For testing, we just ensure the session exists and is active
        val session = userSessions[userId]
        if (session != null && session.isActive) {
            // Session is already in memory, simulating persistence
            updateSessionAccess(userId)
        }
    }
    
    /**
     * Gets all active sessions (for testing purposes)
     * @return List of active session information
     */
    fun getAllActiveSessions(): List<SessionInfo> {
        val now = Clock.System.now()
        return userSessions.values
            .filter { it.isActive && now < it.expiresAt }
            .map { session ->
                SessionInfo(
                    userId = session.userId,
                    token = session.token,
                    createdAt = session.createdAt,
                    expiresAt = session.expiresAt,
                    lastAccessedAt = session.lastAccessedAt,
                    deviceId = session.deviceId,
                    isActive = session.isActive
                )
            }
    }
    
    // Data class for session information
    data class SessionInfo(
        val userId: String,
        val token: String,
        val createdAt: kotlinx.datetime.Instant,
        val expiresAt: kotlinx.datetime.Instant,
        val lastAccessedAt: kotlinx.datetime.Instant,
        val deviceId: String,
        val isActive: Boolean
    )
    
    // Test helper methods
    fun setCurrentUser(user: User) {
        currentUser = user
        // Also store in users map if not already there
        if (!users.containsKey(user.email.lowercase())) {
            users[user.email.lowercase()] = StoredUser(user, simulatePasswordHash("defaultPassword"))
        }
    }
    
    fun reset() {
        currentUser = null
        users.clear()
        userSessions.clear()
        passwordResetTokens.clear()
    }
    
    /**
     * Simulates app restart by clearing current user but keeping sessions
     * This tests session restoration functionality
     */
    fun simulateAppRestart() {
        currentUser = null
        // Keep sessions and user data to simulate persistence
        // Clean up expired sessions
        cleanupExpiredSessions()
    }
    
    // Additional helper methods for testing
    fun getUserCount(): Int = users.size
    
    fun hasUser(email: String): Boolean = users.containsKey(email.lowercase())
    
    fun getStoredUser(email: String): User? = users[email.lowercase()]?.user
    
    // Additional session management helper methods for testing
    
    fun getActiveSessionCount(): Int = userSessions.values.count { 
        it.isActive && Clock.System.now() < it.expiresAt 
    }
    
    fun hasActiveSession(userId: String): Boolean = isSessionValid(userId)
    
    fun getSessionToken(userId: String): String? = userSessions[userId]?.token
    
    fun forceExpireSession(userId: String) {
        val session = userSessions[userId]
        if (session != null) {
            userSessions[userId] = session.copy(
                expiresAt = Clock.System.now().minus(kotlin.time.Duration.parse("PT1H"))
            )
        }
    }
    
    fun forceExpireAllSessions() {
        val now = Clock.System.now()
        val expiredTime = now.minus(kotlin.time.Duration.parse("PT1H"))
        
        userSessions.keys.forEach { userId ->
            val session = userSessions[userId]
            if (session != null) {
                userSessions[userId] = session.copy(expiresAt = expiredTime)
            }
        }
        
        // Clear current user since all sessions are expired
        currentUser = null
    }
    
    // Password Reset Functionality
    
    /**
     * Initiates password reset by generating a reset token for the user
     * @param email User's email address
     * @return Result indicating success or error
     */
    suspend fun initiatePasswordReset(email: String): Result<Unit> {
        // Input validation
        if (email.isBlank()) {
            return Result.Error(AppError.ValidationError("Email cannot be empty", "email"))
        }
        
        // Check if user exists (for security, we return success even if user doesn't exist)
        val userExists = users.containsKey(email.lowercase())
        
        if (userExists) {
            // Generate reset token
            val resetToken = PasswordResetToken(
                token = generatePasswordResetToken(),
                email = email.lowercase()
            )
            passwordResetTokens[email.lowercase()] = resetToken
        }
        
        // Always return success for security (don't reveal if email exists)
        return Result.Success(Unit)
    }
    
    /**
     * Validates a password reset token
     * @param email User's email address
     * @param token Reset token to validate
     * @return Result indicating if token is valid
     */
    suspend fun validatePasswordResetToken(email: String, token: String): Result<Boolean> {
        // Input validation
        if (email.isBlank() || token.isBlank()) {
            return Result.Error(AppError.ValidationError("Email and token are required"))
        }
        
        val resetToken = passwordResetTokens[email.lowercase()]
        if (resetToken == null) {
            return Result.Success(false)
        }
        
        // Check if token matches and hasn't expired
        val isValid = resetToken.token == token && 
                     Clock.System.now() < resetToken.expiresAt
        
        return Result.Success(isValid)
    }
    
    /**
     * Confirms password reset by changing the user's password
     * @param email User's email address
     * @param token Reset token
     * @param newPassword New password
     * @return Result indicating success or error
     */
    suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): Result<Unit> {
        // Input validation
        if (email.isBlank() || token.isBlank() || newPassword.isBlank()) {
            return Result.Error(AppError.ValidationError("All fields are required"))
        }
        
        // Password strength validation
        if (newPassword.length < 8) {
            return Result.Error(AppError.ValidationError("Password must be at least 8 characters long", "password"))
        }
        
        // Validate reset token
        val tokenValidation = validatePasswordResetToken(email, token)
        if (tokenValidation.isError || tokenValidation.getOrNull() != true) {
            return Result.Error(AppError.AuthenticationError("Invalid or expired reset token"))
        }
        
        // Find user
        val storedUser = users[email.lowercase()]
        if (storedUser == null) {
            return Result.Error(AppError.AuthenticationError("User not found"))
        }
        
        // Update password
        val newPasswordHash = simulatePasswordHash(newPassword)
        users[email.lowercase()] = storedUser.copy(passwordHash = newPasswordHash)
        
        // Remove used reset token
        passwordResetTokens.remove(email.lowercase())
        
        // Clear any existing sessions for security
        userSessions.remove(storedUser.user.id)
        if (currentUser?.id == storedUser.user.id) {
            currentUser = null
        }
        
        return Result.Success(Unit)
    }
    
    /**
     * Gets the password reset token for testing purposes
     * @param email User's email address
     * @return Reset token if exists
     */
    fun getPasswordResetToken(email: String): String? {
        return passwordResetTokens[email.lowercase()]?.token
    }
    
    /**
     * Checks if a password reset token exists for the email
     * @param email User's email address
     * @return true if token exists and is valid
     */
    fun hasValidPasswordResetToken(email: String): Boolean {
        val resetToken = passwordResetTokens[email.lowercase()]
        return resetToken != null && Clock.System.now() < resetToken.expiresAt
    }
    
    private fun generatePasswordResetToken(): String {
        return "reset_${Clock.System.now().toEpochMilliseconds()}_${(100000..999999).random()}"
    }
}

class MockLogRepository : LogRepository {
    private val logs = mutableMapOf<String, DailyLog>()
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        logs[log.id] = log
        return Result.Success(Unit)
    }
    
    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        val log = logs.values.find { it.userId == userId && it.date == date }
        return Result.Success(log)
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        val filteredLogs = logs.values.filter { 
            it.userId == userId && it.date >= startDate && it.date <= endDate 
        }
        return Result.Success(filteredLogs)
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        val userLogs = logs.values.filter { it.userId == userId }.take(limit)
        return Result.Success(userLogs)
    }
    
    override suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit> {
        logs.values.removeAll { it.userId == userId && it.date == date }
        return Result.Success(Unit)
    }
    
    override suspend fun getPeriodLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        val filteredLogs = logs.values.filter { 
            it.userId == userId && it.date >= startDate && it.date <= endDate && it.periodFlow != null
        }
        return Result.Success(filteredLogs)
    }
    
    override suspend fun getBBTLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        val filteredLogs = logs.values.filter { 
            it.userId == userId && it.date >= startDate && it.date <= endDate && it.bbt != null
        }
        return Result.Success(filteredLogs)
    }
    
    override suspend fun getFertilityLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        val filteredLogs = logs.values.filter { 
            it.userId == userId && it.date >= startDate && it.date <= endDate && 
            (it.cervicalMucus != null || it.opkResult != null)
        }
        return Result.Success(filteredLogs)
    }
    
    override suspend fun getLogCount(userId: String): Result<Int> {
        val count = logs.values.count { it.userId == userId }
        return Result.Success(count)
    }
    
    override suspend fun getLogsBySymptoms(userId: String, symptoms: List<Symptom>, startDate: LocalDate?, endDate: LocalDate?): Result<List<DailyLog>> {
        val filteredLogs = logs.values.filter { log ->
            log.userId == userId && 
            symptoms.any { symptom -> log.symptoms.contains(symptom) } &&
            (startDate == null || log.date >= startDate) &&
            (endDate == null || log.date <= endDate)
        }
        return Result.Success(filteredLogs)
    }
    
    fun reset() {
        logs.clear()
    }
}

class MockCycleRepository : CycleRepository {
    private val cycles = mutableListOf<Cycle>()
    
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        val currentCycle = cycles.find { it.userId == userId && it.endDate == null }
        return Result.Success(currentCycle)
    }
    
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        val userCycles = cycles.filter { it.userId == userId }.take(limit)
        return Result.Success(userCycles)
    }
    
    override suspend fun startNewCycle(userId: String, startDate: LocalDate): Result<Cycle> {
        val newCycle = Cycle(
            id = "cycle-${cycles.size}",
            userId = userId,
            startDate = startDate,
            endDate = null,
            predictedOvulationDate = null,
            confirmedOvulationDate = null,
            cycleLength = null,
            lutealPhaseLength = null
        )
        cycles.add(newCycle)
        return Result.Success(newCycle)
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        val index = cycles.indexOfFirst { it.id == cycle.id }
        if (index >= 0) {
            cycles[index] = cycle
        }
        return Result.Success(Unit)
    }
    
    override suspend fun endCurrentCycle(userId: String, endDate: LocalDate): Result<Unit> {
        val currentCycle = cycles.find { it.userId == userId && it.endDate == null }
        currentCycle?.let { cycle ->
            val updatedCycle = cycle.copy(endDate = endDate)
            updateCycle(updatedCycle)
        }
        return Result.Success(Unit)
    }
    
    override suspend fun getCyclesInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Cycle>> {
        val filteredCycles = cycles.filter { 
            it.userId == userId && it.startDate >= startDate && it.startDate <= endDate
        }
        return Result.Success(filteredCycles)
    }
    
    override suspend fun getAverageCycleLength(userId: String, cycleCount: Int): Result<Double?> {
        val completedCycles = cycles.filter { it.userId == userId && it.cycleLength != null }.take(cycleCount)
        if (completedCycles.isEmpty()) return Result.Success(null)
        
        val average = completedCycles.mapNotNull { it.cycleLength }.average()
        return Result.Success(average)
    }
    
    override suspend fun predictNextPeriod(userId: String): Result<LocalDate?> {
        // Simple prediction based on last cycle
        val lastCycle = cycles.filter { it.userId == userId }.maxByOrNull { it.startDate }
        // Simple mock prediction - just return null for now
        return Result.Success(null)
    }
    
    override suspend fun confirmOvulation(cycleId: String, ovulationDate: LocalDate): Result<Unit> {
        val cycle = cycles.find { it.id == cycleId }
        cycle?.let { 
            val updatedCycle = it.copy(confirmedOvulationDate = ovulationDate)
            updateCycle(updatedCycle)
        }
        return Result.Success(Unit)
    }
    
    fun reset() {
        cycles.clear()
    }
}

class MockInsightRepository : InsightRepository {
    private val insights = mutableListOf<Insight>()
    
    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
        val unreadInsights = insights.filter { it.userId == userId && !it.isRead }
        return Result.Success(unreadInsights)
    }
    
    override suspend fun markInsightAsRead(insightId: String): Result<Unit> {
        val index = insights.indexOfFirst { it.id == insightId }
        if (index >= 0) {
            insights[index] = insights[index].copy(isRead = true)
        }
        return Result.Success(Unit)
    }
    
    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
        val userInsights = insights.filter { it.userId == userId }.take(limit)
        return Result.Success(userInsights)
    }
    
    override suspend fun saveInsight(insight: Insight): Result<Unit> {
        insights.add(insight)
        return Result.Success(Unit)
    }
    
    override suspend fun getInsightsByType(userId: String, type: InsightType, limit: Int): Result<List<Insight>> {
        val filteredInsights = insights.filter { it.userId == userId && it.type == type }.take(limit)
        return Result.Success(filteredInsights)
    }
    
    override suspend fun getActionableInsights(userId: String): Result<List<Insight>> {
        val actionableInsights = insights.filter { it.userId == userId && it.actionable }
        return Result.Success(actionableInsights)
    }
    
    override suspend fun deleteInsight(insightId: String): Result<Unit> {
        insights.removeAll { it.id == insightId }
        return Result.Success(Unit)
    }
    
    override suspend fun getInsightsForLogs(userId: String, logIds: List<String>): Result<List<Insight>> {
        val relatedInsights = insights.filter { insight ->
            insight.userId == userId && insight.relatedLogIds.any { logIds.contains(it) }
        }
        return Result.Success(relatedInsights)
    }
    
    override suspend fun getInsightCount(userId: String, includeRead: Boolean): Result<Int> {
        val count = if (includeRead) {
            insights.count { it.userId == userId }
        } else {
            insights.count { it.userId == userId && !it.isRead }
        }
        return Result.Success(count)
    }
    
    override suspend fun getHighConfidenceInsights(userId: String, minConfidence: Double, limit: Int): Result<List<Insight>> {
        val highConfidenceInsights = insights.filter { 
            it.userId == userId && it.confidence >= minConfidence 
        }.take(limit)
        return Result.Success(highConfidenceInsights)
    }
    
    override suspend fun markAllInsightsAsRead(userId: String): Result<Unit> {
        insights.forEachIndexed { index, insight ->
            if (insight.userId == userId && !insight.isRead) {
                insights[index] = insight.copy(isRead = true)
            }
        }
        return Result.Success(Unit)
    }
    
    fun reset() {
        insights.clear()
    }
}

class MockHealthReportRepository : HealthReportRepository {
    private val reports = mutableListOf<HealthReport>()
    
    override suspend fun validateReportData(userId: String, dateRange: DateRange): Result<ReportValidationResult> {
        val validationResult = ReportValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = emptyList()
        )
        return Result.Success(validationResult)
    }
    
    override suspend fun generateReport(userId: String, reportType: ReportType, dateRange: DateRange): Result<HealthReport> {
        val report = HealthReport(
            id = "report-${reports.size}",
            userId = userId,
            generatedDate = Clock.System.now(),
            reportType = reportType,
            dateRange = dateRange,
            cycleSummary = CycleSummary(
                totalCycles = 3,
                averageCycleLength = 28.0,
                averageLutealPhase = 14.0,
                cycleRegularity = CycleRegularity.REGULAR,
                ovulationPatterns = OvulationPatterns(
                    averageOvulationDay = 14.0,
                    ovulationConfidenceRate = 0.8,
                    fertilityWindowAccuracy = 0.9
                )
            ),
            symptomAnalysis = SymptomAnalysis(
                mostCommonSymptoms = emptyList(),
                symptomTrends = emptyMap(),
                moodPatterns = MoodPatterns(
                    dominantMood = "Happy",
                    moodVariability = 0.3,
                    cyclePhaseCorrelations = emptyMap()
                ),
                bbtAnalysis = null
            ),
            insights = emptyList()
        )
        reports.add(report)
        return Result.Success(report)
    }
    
    override suspend fun saveReport(report: HealthReport): Result<Unit> {
        reports.add(report)
        return Result.Success(Unit)
    }
    
    override suspend fun getReport(reportId: String): Result<HealthReport?> {
        val report = reports.find { it.id == reportId }
        return Result.Success(report)
    }
    
    override suspend fun getUserReports(userId: String): Result<List<HealthReport>> {
        val userReports = reports.filter { it.userId == userId }
        return Result.Success(userReports)
    }
    
    override suspend fun generatePDF(report: HealthReport): Result<String> {
        return Result.Success("mock-pdf-url")
    }
    
    override suspend fun shareReport(reportId: String, expirationDays: Int): Result<String> {
        return Result.Success("mock-share-url")
    }
    
    override suspend fun revokeSharedAccess(reportId: String): Result<Unit> {
        return Result.Success(Unit)
    }
    
    override suspend fun deleteReport(reportId: String): Result<Unit> {
        reports.removeAll { it.id == reportId }
        return Result.Success(Unit)
    }
    
    fun reset() {
        reports.clear()
    }
}

class MockAuthService : AuthService {
    private var currentUser: User? = null
    private val users = mutableMapOf<String, StoredAuthUser>() // email -> StoredAuthUser
    private val passwordResetTokens = mutableMapOf<String, PasswordResetToken>() // email -> token
    
    private data class StoredAuthUser(
        val user: User,
        val passwordHash: String
    )
    
    private data class PasswordResetToken(
        val token: String,
        val email: String,
        val createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        val expiresAt: kotlinx.datetime.Instant = Clock.System.now().plus(kotlin.time.Duration.parse("PT1H"))
    )
    
    override val authState: Flow<User?> = flowOf(currentUser)
    
    override suspend fun getCurrentUser(): Result<User?> {
        return Result.Success(currentUser)
    }
    
    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        // Input validation
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.Error(AppError.ValidationError("All fields are required"))
        }
        
        // Check if user already exists
        if (users.containsKey(email.lowercase())) {
            return Result.Error(AppError.AuthenticationError("User with this email already exists"))
        }
        
        // Create new user with onboardingComplete = false
        val userId = "auth_user_${Clock.System.now().toEpochMilliseconds()}"
        val newUser = User(
            id = userId,
            email = email,
            name = name,
            onboardingComplete = false, // New users haven't completed onboarding
            primaryGoal = HealthGoal.CYCLE_TRACKING,
            unitSystem = UnitSystem.METRIC,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Store user with password hash
        users[email.lowercase()] = StoredAuthUser(newUser, simulatePasswordHash(password))
        currentUser = newUser
        
        return Result.Success(newUser)
    }
    
    override suspend fun signIn(email: String, password: String): Result<User> {
        // Input validation
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(AppError.ValidationError("Email and password are required"))
        }
        
        // Find user
        val storedUser = users[email.lowercase()]
        if (storedUser == null) {
            return Result.Error(AppError.AuthenticationError("Invalid email or password"))
        }
        
        // Verify password
        if (!verifyPassword(password, storedUser.passwordHash)) {
            return Result.Error(AppError.AuthenticationError("Invalid email or password"))
        }
        
        currentUser = storedUser.user
        return Result.Success(storedUser.user)
    }
    
    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        return Result.Success(Unit)
    }
    
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        // Input validation
        if (email.isBlank()) {
            return Result.Error(AppError.ValidationError("Email is required"))
        }
        
        // Check if user exists
        if (!users.containsKey(email.lowercase())) {
            // For security, don't reveal if email exists or not
            return Result.Success(Unit)
        }
        
        // Generate reset token
        val resetToken = PasswordResetToken(
            token = generateResetToken(),
            email = email.lowercase()
        )
        passwordResetTokens[email.lowercase()] = resetToken
        
        return Result.Success(Unit)
    }
    
    override fun isAuthenticated(): Boolean {
        return currentUser != null
    }
    
    // Helper methods
    private fun simulatePasswordHash(password: String): String {
        return "auth_hash_${password.hashCode()}_${Clock.System.now().toEpochMilliseconds()}"
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hash.startsWith("auth_hash_${password.hashCode()}_")
    }
    
    private fun generateResetToken(): String {
        return "reset_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
    
    // Test helper methods
    fun reset() {
        currentUser = null
        users.clear()
        passwordResetTokens.clear()
    }
    
    fun hasUser(email: String): Boolean = users.containsKey(email.lowercase())
    
    fun getPasswordResetToken(email: String): String? = passwordResetTokens[email.lowercase()]?.token
    
    /**
     * Validates a password reset token
     * @param email User's email address
     * @param token Reset token to validate
     * @return Result indicating if token is valid
     */
    suspend fun validatePasswordResetToken(email: String, token: String): Result<Boolean> {
        // Input validation
        if (email.isBlank() || token.isBlank()) {
            return Result.Error(AppError.ValidationError("Email and token are required"))
        }
        
        val resetToken = passwordResetTokens[email.lowercase()]
        if (resetToken == null) {
            return Result.Success(false)
        }
        
        // Check if token matches and hasn't expired
        val isValid = resetToken.token == token && 
                     Clock.System.now() < resetToken.expiresAt
        
        return Result.Success(isValid)
    }
    
    /**
     * Confirms password reset by changing the user's password
     * @param email User's email address
     * @param token Reset token
     * @param newPassword New password
     * @return Result indicating success or error
     */
    suspend fun confirmPasswordReset(email: String, token: String, newPassword: String): Result<Unit> {
        // Input validation
        if (email.isBlank() || token.isBlank() || newPassword.isBlank()) {
            return Result.Error(AppError.ValidationError("All fields are required"))
        }
        
        // Password strength validation
        if (newPassword.length < 8) {
            return Result.Error(AppError.ValidationError("Password must be at least 8 characters long", "password"))
        }
        
        // Validate reset token
        val tokenValidation = validatePasswordResetToken(email, token)
        if (tokenValidation.isError || tokenValidation.getOrNull() != true) {
            return Result.Error(AppError.AuthenticationError("Invalid or expired reset token"))
        }
        
        // Find user
        val storedUser = users[email.lowercase()]
        if (storedUser == null) {
            return Result.Error(AppError.AuthenticationError("User not found"))
        }
        
        // Update password
        val newPasswordHash = simulatePasswordHash(newPassword)
        users[email.lowercase()] = storedUser.copy(passwordHash = newPasswordHash)
        
        // Remove used reset token
        passwordResetTokens.remove(email.lowercase())
        
        // Sign out user for security
        if (currentUser?.id == storedUser.user.id) {
            currentUser = null
        }
        
        return Result.Success(Unit)
    }
    
    /**
     * Checks if a password reset token exists and is valid for the email
     * @param email User's email address
     * @return true if token exists and is valid
     */
    fun hasValidPasswordResetToken(email: String): Boolean {
        val resetToken = passwordResetTokens[email.lowercase()]
        return resetToken != null && Clock.System.now() < resetToken.expiresAt
    }
}

class LocalMockFirestoreService {
    private val users = mutableMapOf<String, User>()
    private val logs = mutableMapOf<String, DailyLog>()
    
    suspend fun saveUser(user: User): Result<Unit> {
        users[user.id] = user
        return Result.Success(Unit)
    }
    
    suspend fun getUser(userId: String): Result<User?> {
        return Result.Success(users[userId])
    }
    
    suspend fun updateUser(user: User): Result<Unit> {
        users[user.id] = user
        return Result.Success(Unit)
    }
    
    suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        logs[log.id] = log
        return Result.Success(Unit)
    }
    
    suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
        val log = logs.values.find { it.userId == userId && it.date == date }
        return Result.Success(log)
    }
}

class MockDatabaseManager {
    fun clearAllData() {
        // Mock implementation
    }
    
    suspend fun performMigration(): Result<Unit> {
        return Result.Success(Unit)
    }
    
    fun getUserDao(): MockUserDao {
        return MockUserDao()
    }
    
    fun getDailyLogDao(): MockDailyLogDao {
        return MockDailyLogDao()
    }
}

class MockUserDao {
    suspend fun insertUser(user: User) {
        // Mock implementation
    }
    
    suspend fun getUserById(id: String): User? {
        return null
    }
    
    suspend fun updateUser(user: User) {
        // Mock implementation
    }
    
    suspend fun deleteUser(id: String) {
        // Mock implementation
    }
    
    suspend fun insertOrUpdate(user: User) {
        // Mock implementation
    }
    
    suspend fun markAsSynced(userId: String) {
        // Mock implementation
    }
    
    suspend fun getPendingSyncUsers(): List<User> {
        return emptyList()
    }
}

class MockDailyLogDao {
    suspend fun insertOrUpdate(log: DailyLog) {
        // Mock implementation
    }
    
    suspend fun getDailyLogByUserIdAndDate(userId: String, date: kotlinx.datetime.LocalDate): DailyLog? {
        return null
    }
    
    suspend fun getDailyLogsByDateRange(userId: String, startDate: kotlinx.datetime.LocalDate, endDate: kotlinx.datetime.LocalDate): List<DailyLog> {
        return emptyList()
    }
    
    suspend fun getDailyLogsByUserId(userId: String): List<DailyLog> {
        return emptyList()
    }
    
    suspend fun deleteDailyLog(id: String) {
        // Mock implementation
    }
    
    suspend fun markAsSynced(logId: String) {
        // Mock implementation
    }
    
    suspend fun getPendingSync(): List<DailyLog> {
        return emptyList()
    }
}

class MockNetworkConnectivity : com.eunio.healthapp.domain.util.NetworkConnectivity {
    private var connected = true
    private var networkType = com.eunio.healthapp.domain.util.NetworkType.WIFI
    
    override fun isConnected(): Boolean = connected
    
    override fun observeConnectivity(): Flow<Boolean> = flowOf(connected)
    
    override suspend fun hasStableConnection(): Boolean = connected
    
    override fun getNetworkType(): com.eunio.healthapp.domain.util.NetworkType = networkType
    
    fun setConnected(isConnected: Boolean) {
        connected = isConnected
    }
    
    fun setNetworkType(type: com.eunio.healthapp.domain.util.NetworkType) {
        networkType = type
    }
    
    fun reset() {
        connected = true
        networkType = com.eunio.healthapp.domain.util.NetworkType.WIFI
    }
}


class SimpleMockPlatformManager : com.eunio.healthapp.platform.PlatformManager {
    override fun optimizePerformance() {}
    override fun getPerformanceMetrics(): com.eunio.healthapp.platform.PlatformPerformanceMetrics = MockPlatformPerformanceMetrics()
    override fun configureSecurityFeatures() {}
    override fun handleDeepLink(url: String): Boolean = true
    override fun shareContent(content: String, title: String) {}
    override fun openDocumentPicker() {}
    override fun getPlatformName(): String = "Test"
    override fun getOSVersion(): String = "Test 1.0"
    override fun getDeviceModel(): String = "Test Device"
    override fun getScreenSize(): String = "1920x1080"
    override fun getLocale(): String = "en_US"
    override fun getAppVersion(): String = "1.0.0"
    override fun getBuildNumber(): String = "1"
    override fun getInstallDate(): kotlinx.datetime.Instant? = null
    override fun getLastUpdateDate(): kotlinx.datetime.Instant? = null
}

class MockPlatformPerformanceMetrics : com.eunio.healthapp.platform.PlatformPerformanceMetrics {
    override val deviceModel: String = "Test Device"
    override val systemVersion: String = "Test 1.0"
    override val availableMemory: Long = 1024L * 1024L * 1024L // 1GB
    override val totalMemory: Long = 2048L * 1024L * 1024L // 2GB
    override val processorCount: Int = 4
    fun getBuildNumber(): String = "1"
    fun getInstallDate(): kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now()
    fun getLastUpdateDate(): kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now()
    fun configureSecurityFeatures() {
        // Mock implementation
    }
}

