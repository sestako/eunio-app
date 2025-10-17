package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Multiplatform tests for FirestoreService implementation.
 * Tests CRUD operations, data transformation, and error handling across all platforms.
 */
class FirestoreServiceTest {
    
    private lateinit var firestoreService: FirestoreService
    private lateinit var errorHandler: ErrorHandler
    
    // Test data
    private val testUserId = "test-user-123"
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    private val testCycle = Cycle(
        id = "test-cycle-123",
        userId = testUserId,
        startDate = LocalDate(2024, 1, 1),
        endDate = null,
        predictedOvulationDate = LocalDate(2024, 1, 14),
        confirmedOvulationDate = null,
        cycleLength = null,
        lutealPhaseLength = null
    )
    
    private val testDailyLog = DailyLog(
        id = "test-log-123",
        userId = testUserId,
        date = LocalDate(2024, 1, 5),
        periodFlow = PeriodFlow.MEDIUM,
        symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
        mood = Mood.NEUTRAL,
        sexualActivity = SexualActivity(occurred = false),
        bbt = 98.2,
        cervicalMucus = CervicalMucus.CREAMY,
        opkResult = OPKResult.NEGATIVE,
        notes = "Test notes",
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    private val testInsight = Insight(
        id = "test-insight-123",
        userId = testUserId,
        generatedDate = Clock.System.now(),
        insightText = "Your cycle appears to be regular",
        type = InsightType.PATTERN_RECOGNITION,
        isRead = false,
        relatedLogIds = listOf("test-log-123"),
        confidence = 0.85,
        actionable = false
    )
    
    @BeforeTest
    fun setup() {
        errorHandler = ErrorHandler()
        firestoreService = createTestFirestoreService()
    }
    
    @AfterTest
    fun tearDown() {
        // Clean up test data if needed
    }
    
    // User CRUD Tests
    @Test
    fun saveUser_shouldStoreUserSuccessfully() = runTest {
        // When
        val result = firestoreService.saveUser(testUser)
        
        // Then
        assertTrue(result.isSuccess, "Save user should succeed")
    }
    
    @Test
    fun getUser_shouldRetrieveStoredUser() = runTest {
        // Given
        firestoreService.saveUser(testUser)
        
        // When
        val result = firestoreService.getUser(testUserId)
        
        // Then
        assertTrue(result.isSuccess, "Get user should succeed")
        val retrievedUser = result.getOrNull()
        assertNotNull(retrievedUser, "Retrieved user should not be null")
        assertEquals(testUser.id, retrievedUser.id)
        assertEquals(testUser.email, retrievedUser.email)
        assertEquals(testUser.name, retrievedUser.name)
        assertEquals(testUser.primaryGoal, retrievedUser.primaryGoal)
    }
    
    @Test
    fun getUser_shouldReturnNullForNonExistentUser() = runTest {
        // When
        val result = firestoreService.getUser("non-existent-user")
        
        // Then
        assertTrue(result.isSuccess, "Get non-existent user should succeed")
        assertNull(result.getOrNull(), "Non-existent user should return null")
    }
    
    @Test
    fun updateUser_shouldModifyExistingUser() = runTest {
        // Given
        firestoreService.saveUser(testUser)
        val updatedUser = testUser.copy(name = "Updated Name")
        
        // When
        val updateResult = firestoreService.updateUser(updatedUser)
        val getResult = firestoreService.getUser(testUserId)
        
        // Then
        assertTrue(updateResult.isSuccess, "Update user should succeed")
        assertTrue(getResult.isSuccess, "Get updated user should succeed")
        assertEquals("Updated Name", getResult.getOrNull()?.name)
    }
    
    @Test
    fun deleteUser_shouldRemoveUser() = runTest {
        // Given
        firestoreService.saveUser(testUser)
        
        // When
        val deleteResult = firestoreService.deleteUser(testUserId)
        val getResult = firestoreService.getUser(testUserId)
        
        // Then
        assertTrue(deleteResult.isSuccess, "Delete user should succeed")
        assertTrue(getResult.isSuccess, "Get deleted user should succeed")
        assertNull(getResult.getOrNull(), "Deleted user should return null")
    }
    
    // Cycle CRUD Tests
    @Test
    fun saveCycle_shouldStoreCycleSuccessfully() = runTest {
        // When
        val result = firestoreService.saveCycle(testCycle)
        
        // Then
        assertTrue(result.isSuccess, "Save cycle should succeed")
    }
    
    @Test
    fun getCurrentCycle_shouldRetrieveActiveCycle() = runTest {
        // Given
        firestoreService.saveCycle(testCycle)
        
        // When
        val result = firestoreService.getCurrentCycle(testUserId)
        
        // Then
        assertTrue(result.isSuccess, "Get current cycle should succeed")
        val retrievedCycle = result.getOrNull()
        assertNotNull(retrievedCycle, "Retrieved cycle should not be null")
        assertEquals(testCycle.id, retrievedCycle.id)
        assertEquals(testCycle.startDate, retrievedCycle.startDate)
        assertNull(retrievedCycle.endDate, "Active cycle should have no end date")
    }
    
    @Test
    fun getCycleHistory_shouldReturnCyclesInDescendingOrder() = runTest {
        // Given
        val cycle1 = testCycle.copy(id = "cycle-1", startDate = LocalDate(2024, 1, 1))
        val cycle2 = testCycle.copy(id = "cycle-2", startDate = LocalDate(2024, 2, 1))
        firestoreService.saveCycle(cycle1)
        firestoreService.saveCycle(cycle2)
        
        // When
        val result = firestoreService.getCycleHistory(testUserId, 10)
        
        // Then
        assertTrue(result.isSuccess, "Get cycle history should succeed")
        val cycles = result.getOrNull()
        assertNotNull(cycles, "Cycles should not be null")
        assertEquals(2, cycles.size, "Should return 2 cycles")
        // Should be in descending order by start date
        assertEquals("cycle-2", cycles[0].id)
        assertEquals("cycle-1", cycles[1].id)
    }
    
    // DailyLog CRUD Tests
    @Test
    fun saveDailyLog_shouldStoreLogSuccessfully() = runTest {
        // When
        val result = firestoreService.saveDailyLog(testDailyLog)
        
        // Then
        assertTrue(result.isSuccess, "Save daily log should succeed")
    }
    
    @Test
    fun getDailyLogByDate_shouldRetrieveLogForSpecificDate() = runTest {
        // Given
        firestoreService.saveDailyLog(testDailyLog)
        
        // When
        val result = firestoreService.getDailyLogByDate(testUserId, testDailyLog.date)
        
        // Then
        assertTrue(result.isSuccess, "Get daily log by date should succeed")
        val retrievedLog = result.getOrNull()
        assertNotNull(retrievedLog, "Retrieved log should not be null")
        assertEquals(testDailyLog.id, retrievedLog.id)
        assertEquals(testDailyLog.date, retrievedLog.date)
        assertEquals(testDailyLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(testDailyLog.symptoms, retrievedLog.symptoms)
        assertEquals(testDailyLog.bbt, retrievedLog.bbt)
    }
    
    @Test
    fun getLogsInRange_shouldReturnLogsWithinDateRange() = runTest {
        // Given
        val log1 = testDailyLog.copy(id = "log-1", date = LocalDate(2024, 1, 5))
        val log2 = testDailyLog.copy(id = "log-2", date = LocalDate(2024, 1, 10))
        val log3 = testDailyLog.copy(id = "log-3", date = LocalDate(2024, 1, 20))
        
        firestoreService.saveDailyLog(log1)
        firestoreService.saveDailyLog(log2)
        firestoreService.saveDailyLog(log3)
        
        // When
        val result = firestoreService.getLogsInRange(
            testUserId,
            LocalDate(2024, 1, 1),
            LocalDate(2024, 1, 15)
        )
        
        // Then
        assertTrue(result.isSuccess, "Get logs in range should succeed")
        val logs = result.getOrNull()
        assertNotNull(logs, "Logs should not be null")
        assertEquals(2, logs.size, "Should return 2 logs within range")
        assertEquals(setOf("log-1", "log-2"), logs.map { it.id }.toSet())
    }
    
    // Insight CRUD Tests
    @Test
    fun saveInsight_shouldStoreInsightSuccessfully() = runTest {
        // When
        val result = firestoreService.saveInsight(testInsight)
        
        // Then
        assertTrue(result.isSuccess, "Save insight should succeed")
    }
    
    @Test
    fun getUnreadInsights_shouldReturnOnlyUnreadInsights() = runTest {
        // Given
        val unreadInsight = testInsight.copy(id = "unread-insight", isRead = false)
        val readInsight = testInsight.copy(id = "read-insight", isRead = true)
        
        firestoreService.saveInsight(unreadInsight)
        firestoreService.saveInsight(readInsight)
        
        // When
        val result = firestoreService.getUnreadInsights(testUserId)
        
        // Then
        assertTrue(result.isSuccess, "Get unread insights should succeed")
        val insights = result.getOrNull()
        assertNotNull(insights, "Insights should not be null")
        assertEquals(1, insights.size, "Should return 1 unread insight")
        assertEquals("unread-insight", insights[0].id)
        assertFalse(insights[0].isRead, "Insight should be unread")
    }
    
    @Test
    fun markInsightAsRead_shouldUpdateInsightStatus() = runTest {
        // Given
        firestoreService.saveInsight(testInsight)
        
        // When
        val markResult = firestoreService.markInsightAsRead(testUserId, testInsight.id)
        val getResult = firestoreService.getInsight(testUserId, testInsight.id)
        
        // Then
        assertTrue(markResult.isSuccess, "Mark insight as read should succeed")
        assertTrue(getResult.isSuccess, "Get insight should succeed")
        assertTrue(getResult.getOrNull()?.isRead == true, "Insight should be marked as read")
    }
    
    // Batch Operations Tests
    @Test
    fun batchSaveDailyLogs_shouldSaveMultipleLogsAtomically() = runTest {
        // Given
        val logs = listOf(
            testDailyLog.copy(id = "batch-log-1", date = LocalDate(2024, 1, 1)),
            testDailyLog.copy(id = "batch-log-2", date = LocalDate(2024, 1, 2)),
            testDailyLog.copy(id = "batch-log-3", date = LocalDate(2024, 1, 3))
        )
        
        // When
        val result = firestoreService.batchSaveDailyLogs(logs)
        
        // Then
        assertTrue(result.isSuccess, "Batch save daily logs should succeed")
        
        // Verify all logs were saved
        logs.forEach { log ->
            val getResult = firestoreService.getDailyLog(testUserId, log.id)
            assertTrue(getResult.isSuccess, "Get saved log should succeed")
            assertNotNull(getResult.getOrNull(), "Saved log should exist")
        }
    }
    
    @Test
    fun batchSaveUsers_shouldSaveMultipleUsersAtomically() = runTest {
        // Given
        val users = listOf(
            testUser.copy(id = "batch-user-1", email = "user1@test.com"),
            testUser.copy(id = "batch-user-2", email = "user2@test.com"),
            testUser.copy(id = "batch-user-3", email = "user3@test.com")
        )
        
        // When
        val result = firestoreService.batchSaveUsers(users)
        
        // Then
        assertTrue(result.isSuccess, "Batch save users should succeed")
        
        // Verify all users were saved
        users.forEach { user ->
            val getResult = firestoreService.getUser(user.id)
            assertTrue(getResult.isSuccess, "Get saved user should succeed")
            assertNotNull(getResult.getOrNull(), "Saved user should exist")
        }
    }
    
    // Sync Operations Tests
    @Test
    fun syncOperations_shouldHandleTimestampsCorrectly() = runTest {
        // Given
        val timestamp = Clock.System.now().epochSeconds
        
        // When
        val updateResult = firestoreService.updateLastSyncTimestamp(testUserId, timestamp)
        val getResult = firestoreService.getLastSyncTimestamp(testUserId)
        
        // Then
        assertTrue(updateResult.isSuccess, "Update sync timestamp should succeed")
        assertTrue(getResult.isSuccess, "Get sync timestamp should succeed")
        assertEquals(timestamp, getResult.getOrNull(), "Timestamps should match")
    }
    
    @Test
    fun getChangedDocumentsSince_shouldReturnRecentChanges() = runTest {
        // Given
        val oldTimestamp = Clock.System.now().epochSeconds - 3600 // 1 hour ago
        firestoreService.saveDailyLog(testDailyLog)
        firestoreService.saveInsight(testInsight)
        
        // When
        val result = firestoreService.getChangedDocumentsSince(testUserId, oldTimestamp)
        
        // Then
        assertTrue(result.isSuccess, "Get changed documents should succeed")
        val syncData = result.getOrNull()
        assertNotNull(syncData, "Sync data should not be null")
        // Note: The exact assertions depend on the mock implementation
    }
    
    // Error Handling Tests
    @Test
    fun saveUser_withBlankId_shouldReturnValidationError() = runTest {
        // Given
        val invalidUser = testUser.copy(id = "")
        
        // When
        val result = firestoreService.saveUser(invalidUser)
        
        // Then
        assertTrue(result.isError, "Save user with blank ID should fail")
        val error = result.errorOrNull()
        assertTrue(error is AppError.ValidationError, "Should return validation error")
    }
    
    @Test
    fun saveUser_withBlankEmail_shouldReturnValidationError() = runTest {
        // Given
        val invalidUser = testUser.copy(email = "")
        
        // When
        val result = firestoreService.saveUser(invalidUser)
        
        // Then
        assertTrue(result.isError, "Save user with blank email should fail")
        val error = result.errorOrNull()
        assertTrue(error is AppError.ValidationError, "Should return validation error")
    }
    
    @Test
    fun getUser_withBlankId_shouldReturnValidationError() = runTest {
        // When
        val result = firestoreService.getUser("")
        
        // Then
        assertTrue(result.isError, "Get user with blank ID should fail")
        val error = result.errorOrNull()
        assertTrue(error is AppError.ValidationError, "Should return validation error")
    }
    
    /**
     * Creates a test FirestoreService implementation for multiplatform testing.
     * This uses a mock implementation that works across all platforms.
     */
    private fun createTestFirestoreService(): FirestoreService {
        return MockFirestoreService(errorHandler)
    }
}

/**
 * Mock implementation of FirestoreService for testing purposes.
 * Provides in-memory storage that works across all platforms.
 */
class MockFirestoreService(
    private val errorHandler: ErrorHandler
) : FirestoreService {
    
    // Mock storage
    private val users = mutableMapOf<String, User>()
    private val cycles = mutableMapOf<String, Cycle>()
    private val dailyLogs = mutableMapOf<String, DailyLog>()
    private val insights = mutableMapOf<String, Insight>()
    private val syncTimestamps = mutableMapOf<String, Long>()
    
    // User operations
    override suspend fun saveUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            if (user.id.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            if (user.email.isBlank()) {
                throw IllegalArgumentException("User email cannot be blank")
            }
            users[user.id] = user
        }
    }
    
    override suspend fun getUser(userId: String): Result<User?> {
        return Result.catching(errorHandler) {
            if (userId.isBlank()) {
                throw IllegalArgumentException("User ID cannot be blank")
            }
            users[userId]
        }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        return Result.catching(errorHandler) {
            users[user.id] = user
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            users.remove(userId)
            // Also remove related data
            cycles.entries.removeAll { it.value.userId == userId }
            dailyLogs.entries.removeAll { it.value.userId == userId }
            insights.entries.removeAll { it.value.userId == userId }
            syncTimestamps.remove(userId)
        }
    }
    
    // Cycle operations
    override suspend fun saveCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            cycles[cycle.id] = cycle
        }
    }
    
    override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            cycles[cycleId]?.takeIf { it.userId == userId }
        }
    }
    
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return Result.catching(errorHandler) {
            cycles.values.find { it.userId == userId && it.endDate == null }
        }
    }
    
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
        return Result.catching(errorHandler) {
            cycles.values
                .filter { it.userId == userId }
                .sortedByDescending { it.startDate }
                .take(limit)
        }
    }
    
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
        return Result.catching(errorHandler) {
            cycles[cycle.id] = cycle
        }
    }
    
    override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            cycles.remove(cycleId)
        }
    }
    
    // DailyLog operations
    override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
        return Result.catching(errorHandler) {
            dailyLogs[dailyLog.id] = dailyLog
        }
    }
    
    override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> {
        return Result.catching(errorHandler) {
            dailyLogs[logId]?.takeIf { it.userId == userId }
        }
    }
    
    override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
        return Result.catching(errorHandler) {
            dailyLogs.values.find { it.userId == userId && it.date == date }
        }
    }
    
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
        return Result.catching(errorHandler) {
            dailyLogs.values
                .filter { it.userId == userId && it.date >= startDate && it.date <= endDate }
                .sortedByDescending { it.date }
        }
    }
    
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
        return Result.catching(errorHandler) {
            dailyLogs.values
                .filter { it.userId == userId }
                .sortedByDescending { it.date }
                .take(limit)
        }
    }
    
    override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> {
        return Result.catching(errorHandler) {
            dailyLogs[dailyLog.id] = dailyLog
        }
    }
    
    override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            dailyLogs.remove(logId)
        }
    }
    
    // Insight operations
    override suspend fun saveInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            insights[insight.id] = insight
        }
    }
    
    override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> {
        return Result.catching(errorHandler) {
            insights[insightId]?.takeIf { it.userId == userId }
        }
    }
    
    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            insights.values
                .filter { it.userId == userId && !it.isRead }
                .sortedByDescending { it.generatedDate }
        }
    }
    
    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
        return Result.catching(errorHandler) {
            insights.values
                .filter { it.userId == userId }
                .sortedByDescending { it.generatedDate }
                .take(limit)
        }
    }
    
    override suspend fun updateInsight(insight: Insight): Result<Unit> {
        return Result.catching(errorHandler) {
            insights[insight.id] = insight
        }
    }
    
    override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            insights[insightId]?.let { insight ->
                if (insight.userId == userId) {
                    insights[insightId] = insight.copy(isRead = true)
                }
            }
        }
    }
    
    override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            insights.remove(insightId)
        }
    }
    
    // Batch operations
    override suspend fun batchSaveUsers(users: List<User>): Result<Unit> {
        return Result.catching(errorHandler) {
            users.forEach { user ->
                this.users[user.id] = user
            }
        }
    }
    
    override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> {
        return Result.catching(errorHandler) {
            cycles.forEach { cycle ->
                this.cycles[cycle.id] = cycle
            }
        }
    }
    
    override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> {
        return Result.catching(errorHandler) {
            logs.forEach { log ->
                dailyLogs[log.id] = log
            }
        }
    }
    
    override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> {
        return Result.catching(errorHandler) {
            insights.forEach { insight ->
                this.insights[insight.id] = insight
            }
        }
    }
    
    // Sync operations
    override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> {
        return Result.catching(errorHandler) {
            syncTimestamps[userId]
        }
    }
    
    override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> {
        return Result.catching(errorHandler) {
            syncTimestamps[userId] = timestamp
        }
    }
    
    override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData> {
        return Result.catching(errorHandler) {
            // Mock implementation - return recent changes
            val users = this.users.values.filter { it.id == userId && it.updatedAt.epochSeconds > timestamp }
            val cycles = this.cycles.values.filter { it.userId == userId }
            val dailyLogs = this.dailyLogs.values.filter { it.userId == userId && it.updatedAt.epochSeconds > timestamp }
            val insights = this.insights.values.filter { it.userId == userId && it.generatedDate.epochSeconds > timestamp }
            
            SyncData(
                users = users,
                cycles = cycles,
                dailyLogs = dailyLogs,
                insights = insights,
                deletedDocuments = emptyList()
            )
        }
    }

    // Health Report operations - Mock implementations
    private val healthReports = mutableMapOf<String, HealthReport>()
    private val sharedReports = mutableMapOf<String, String>()

    override suspend fun getHealthReport(reportId: String): Result<HealthReport?> {
        return Result.catching(errorHandler) {
            healthReports[reportId]
        }
    }

    override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> {
        return Result.catching(errorHandler) {
            healthReports.values.filter { it.userId == userId }
        }
    }

    override suspend fun saveHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            healthReports[report.id] = report
        }
    }

    override suspend fun updateHealthReport(report: HealthReport): Result<Unit> {
        return Result.catching(errorHandler) {
            healthReports[report.id] = report
        }
    }

    override suspend fun deleteHealthReport(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            healthReports.remove(reportId)
            sharedReports.remove(reportId)
        }
    }

    override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> {
        return Result.catching(errorHandler) {
            val shareId = "share_${reportId}_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
            sharedReports[reportId] = shareId
            "https://eunio.app/shared/$shareId"
        }
    }

    override suspend fun revokeShareableLink(reportId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            sharedReports.remove(reportId)
        }
    }

    // Settings operations
    override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - in real scenario this would update user document
        }
    }

    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - in real scenario this would remove settings from user document
        }
    }

    override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - in real scenario this would save to settingsHistory subcollection
        }
    }

    override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>> {
        return Result.catching(errorHandler) {
            // Mock implementation - return empty list
            emptyList<Pair<String, Map<String, Any>>>()
        }
    }

    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        return Result.catching(errorHandler) {
            // Mock implementation - in real scenario this would delete settingsHistory subcollection
        }
    }
}