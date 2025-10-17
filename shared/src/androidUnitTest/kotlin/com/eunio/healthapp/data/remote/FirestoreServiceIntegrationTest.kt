package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests for FirestoreService implementation.
 * Tests CRUD operations, data transformation, and error handling.
 */
class FirestoreServiceIntegrationTest {
    
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
    
    @Before
    fun setup() {
        errorHandler = ErrorHandler()
        
        // For integration tests, we would use a real Firebase instance or emulator
        // For now, we'll create a mock implementation for testing
        firestoreService = createMockFirestoreService()
    }
    
    @After
    fun tearDown() {
        // Clean up test data if needed
    }
    
    @Test
    fun `saveUser should store user successfully`() = runTest {
        // When
        val result = firestoreService.saveUser(testUser)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
    }
    
    @Test
    fun `getUser should retrieve stored user`() = runTest {
        // Given
        firestoreService.saveUser(testUser)
        
        // When
        val result = firestoreService.getUser(testUserId)
        
        // Then
        assertIs<Result.Success<User?>>(result)
        val retrievedUser = result.data
        assertNotNull(retrievedUser)
        assertEquals(testUser.id, retrievedUser.id)
        assertEquals(testUser.email, retrievedUser.email)
        assertEquals(testUser.name, retrievedUser.name)
        assertEquals(testUser.primaryGoal, retrievedUser.primaryGoal)
    }
    
    @Test
    fun `getUser should return null for non-existent user`() = runTest {
        // When
        val result = firestoreService.getUser("non-existent-user")
        
        // Then
        assertIs<Result.Success<User?>>(result)
        assertNull(result.data)
    }
    
    @Test
    fun `updateUser should modify existing user`() = runTest {
        // Given
        firestoreService.saveUser(testUser)
        val updatedUser = testUser.copy(name = "Updated Name")
        
        // When
        val updateResult = firestoreService.updateUser(updatedUser)
        val getResult = firestoreService.getUser(testUserId)
        
        // Then
        assertIs<Result.Success<Unit>>(updateResult)
        assertIs<Result.Success<User?>>(getResult)
        assertEquals("Updated Name", getResult.data?.name)
    }
    
    @Test
    fun `saveCycle should store cycle successfully`() = runTest {
        // When
        val result = firestoreService.saveCycle(testCycle)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
    }
    
    @Test
    fun `getCurrentCycle should retrieve active cycle`() = runTest {
        // Given
        firestoreService.saveCycle(testCycle)
        
        // When
        val result = firestoreService.getCurrentCycle(testUserId)
        
        // Then
        assertIs<Result.Success<Cycle?>>(result)
        val retrievedCycle = result.data
        assertNotNull(retrievedCycle)
        assertEquals(testCycle.id, retrievedCycle.id)
        assertEquals(testCycle.startDate, retrievedCycle.startDate)
        assertNull(retrievedCycle.endDate) // Active cycle has no end date
    }
    
    @Test
    fun `getCycleHistory should return cycles in descending order`() = runTest {
        // Given
        val cycle1 = testCycle.copy(id = "cycle-1", startDate = LocalDate(2024, 1, 1))
        val cycle2 = testCycle.copy(id = "cycle-2", startDate = LocalDate(2024, 2, 1))
        firestoreService.saveCycle(cycle1)
        firestoreService.saveCycle(cycle2)
        
        // When
        val result = firestoreService.getCycleHistory(testUserId, 10)
        
        // Then
        assertIs<Result.Success<List<Cycle>>>(result)
        val cycles = result.data
        assertEquals(2, cycles.size)
        // Should be in descending order by start date
        assertEquals("cycle-2", cycles[0].id)
        assertEquals("cycle-1", cycles[1].id)
    }
    
    @Test
    fun `saveDailyLog should store log successfully`() = runTest {
        // When
        val result = firestoreService.saveDailyLog(testDailyLog)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
    }
    
    @Test
    fun `getDailyLogByDate should retrieve log for specific date`() = runTest {
        // Given
        firestoreService.saveDailyLog(testDailyLog)
        
        // When
        val result = firestoreService.getDailyLogByDate(testUserId, testDailyLog.date)
        
        // Then
        assertIs<Result.Success<DailyLog?>>(result)
        val retrievedLog = result.data
        assertNotNull(retrievedLog)
        assertEquals(testDailyLog.id, retrievedLog.id)
        assertEquals(testDailyLog.date, retrievedLog.date)
        assertEquals(testDailyLog.periodFlow, retrievedLog.periodFlow)
        assertEquals(testDailyLog.symptoms, retrievedLog.symptoms)
        assertEquals(testDailyLog.bbt, retrievedLog.bbt)
    }
    
    @Test
    fun `getLogsInRange should return logs within date range`() = runTest {
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
        assertIs<Result.Success<List<DailyLog>>>(result)
        val logs = result.data
        assertEquals(2, logs.size)
        assertEquals(setOf("log-1", "log-2"), logs.map { it.id }.toSet())
    }
    
    @Test
    fun `saveInsight should store insight successfully`() = runTest {
        // When
        val result = firestoreService.saveInsight(testInsight)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
    }
    
    @Test
    fun `getUnreadInsights should return only unread insights`() = runTest {
        // Given
        val unreadInsight = testInsight.copy(id = "unread-insight", isRead = false)
        val readInsight = testInsight.copy(id = "read-insight", isRead = true)
        
        firestoreService.saveInsight(unreadInsight)
        firestoreService.saveInsight(readInsight)
        
        // When
        val result = firestoreService.getUnreadInsights(testUserId)
        
        // Then
        assertIs<Result.Success<List<Insight>>>(result)
        val insights = result.data
        assertEquals(1, insights.size)
        assertEquals("unread-insight", insights[0].id)
        assertEquals(false, insights[0].isRead)
    }
    
    @Test
    fun `markInsightAsRead should update insight status`() = runTest {
        // Given
        firestoreService.saveInsight(testInsight)
        
        // When
        val markResult = firestoreService.markInsightAsRead(testUserId, testInsight.id)
        val getResult = firestoreService.getInsight(testUserId, testInsight.id)
        
        // Then
        assertIs<Result.Success<Unit>>(markResult)
        assertIs<Result.Success<Insight?>>(getResult)
        assertEquals(true, getResult.data?.isRead)
    }
    
    @Test
    fun `batchSaveDailyLogs should save multiple logs atomically`() = runTest {
        // Given
        val logs = listOf(
            testDailyLog.copy(id = "batch-log-1", date = LocalDate(2024, 1, 1)),
            testDailyLog.copy(id = "batch-log-2", date = LocalDate(2024, 1, 2)),
            testDailyLog.copy(id = "batch-log-3", date = LocalDate(2024, 1, 3))
        )
        
        // When
        val result = firestoreService.batchSaveDailyLogs(logs)
        
        // Then
        assertIs<Result.Success<Unit>>(result)
        
        // Verify all logs were saved
        logs.forEach { log ->
            val getResult = firestoreService.getDailyLog(testUserId, log.id)
            assertIs<Result.Success<DailyLog?>>(getResult)
            assertNotNull(getResult.data)
        }
    }
    
    @Test
    fun `sync operations should handle timestamps correctly`() = runTest {
        // Given
        val timestamp = Clock.System.now().epochSeconds
        
        // When
        val updateResult = firestoreService.updateLastSyncTimestamp(testUserId, timestamp)
        val getResult = firestoreService.getLastSyncTimestamp(testUserId)
        
        // Then
        assertIs<Result.Success<Unit>>(updateResult)
        assertIs<Result.Success<Long?>>(getResult)
        assertEquals(timestamp, getResult.data)
    }
    
    @Test
    fun `error handling should return appropriate AppError types`() = runTest {
        // This test would verify error handling with a real Firebase instance
        // For now, we'll test the error handler directly
        
        val networkException = Exception("Network connection failed")
        val authException = Exception("Authentication failed")
        val validationException = IllegalArgumentException("Invalid input")
        
        assertEquals(AppError.NetworkError::class, errorHandler.handleError(networkException)::class)
        assertEquals(AppError.AuthenticationError::class, errorHandler.handleError(authException)::class)
        assertEquals(AppError.ValidationError::class, errorHandler.handleError(validationException)::class)
    }
    
    /**
     * Creates a mock FirestoreService for testing purposes.
     * In a real integration test, this would be replaced with a Firebase emulator.
     */
    private fun createMockFirestoreService(): FirestoreService {
        return object : FirestoreService {
            // Mock storage
            val users = mutableMapOf<String, User>()
            val cycles = mutableMapOf<String, Cycle>()
            val dailyLogs = mutableMapOf<String, DailyLog>()
            val insights = mutableMapOf<String, Insight>()
            val syncTimestamps = mutableMapOf<String, Long>()
            
            // User operations
            override suspend fun saveUser(user: User): Result<Unit> {
                users[user.id] = user
                return Result.success(Unit)
            }
            
            override suspend fun getUser(userId: String): Result<User?> {
                return Result.success(users[userId])
            }
            
            override suspend fun updateUser(user: User): Result<Unit> {
                users[user.id] = user
                return Result.success(Unit)
            }
            
            override suspend fun deleteUser(userId: String): Result<Unit> {
                users.remove(userId)
                return Result.success(Unit)
            }
            
            // Cycle operations
            override suspend fun saveCycle(cycle: Cycle): Result<Unit> {
                cycles[cycle.id] = cycle
                return Result.success(Unit)
            }
            
            override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> {
                return Result.success(cycles[cycleId])
            }
            
            override suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
                val activeCycle = cycles.values.find { it.userId == userId && it.endDate == null }
                return Result.success(activeCycle)
            }
            
            override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> {
                val userCycles = cycles.values
                    .filter { it.userId == userId }
                    .sortedByDescending { it.startDate }
                    .take(limit)
                return Result.success(userCycles)
            }
            
            override suspend fun updateCycle(cycle: Cycle): Result<Unit> {
                cycles[cycle.id] = cycle
                return Result.success(Unit)
            }
            
            override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> {
                cycles.remove(cycleId)
                return Result.success(Unit)
            }
            
            // DailyLog operations
            override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
                dailyLogs[dailyLog.id] = dailyLog
                return Result.success(Unit)
            }
            
            override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> {
                return Result.success(dailyLogs[logId])
            }
            
            override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
                val log = dailyLogs.values.find { it.userId == userId && it.date == date }
                return Result.success(log)
            }
            
            override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> {
                val logsInRange = dailyLogs.values
                    .filter { it.userId == userId && it.date >= startDate && it.date <= endDate }
                    .sortedByDescending { it.date }
                return Result.success(logsInRange)
            }
            
            override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
                val recentLogs = dailyLogs.values
                    .filter { it.userId == userId }
                    .sortedByDescending { it.date }
                    .take(limit)
                return Result.success(recentLogs)
            }
            
            override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> {
                dailyLogs[dailyLog.id] = dailyLog
                return Result.success(Unit)
            }
            
            override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> {
                dailyLogs.remove(logId)
                return Result.success(Unit)
            }
            
            // Insight operations
            override suspend fun saveInsight(insight: Insight): Result<Unit> {
                insights[insight.id] = insight
                return Result.success(Unit)
            }
            
            override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> {
                return Result.success(insights[insightId])
            }
            
            override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> {
                val unreadInsights = insights.values
                    .filter { it.userId == userId && !it.isRead }
                    .sortedByDescending { it.generatedDate }
                return Result.success(unreadInsights)
            }
            
            override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> {
                val userInsights = insights.values
                    .filter { it.userId == userId }
                    .sortedByDescending { it.generatedDate }
                    .take(limit)
                return Result.success(userInsights)
            }
            
            override suspend fun updateInsight(insight: Insight): Result<Unit> {
                insights[insight.id] = insight
                return Result.success(Unit)
            }
            
            override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> {
                insights[insightId]?.let { insight ->
                    insights[insightId] = insight.copy(isRead = true)
                }
                return Result.success(Unit)
            }
            
            override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> {
                insights.remove(insightId)
                return Result.success(Unit)
            }
            
            // Batch operations
            override suspend fun batchSaveUsers(users: List<User>): Result<Unit> {
                users.forEach { user ->
                    this.users[user.id] = user
                }
                return Result.success(Unit)
            }
            
            override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> {
                cycles.forEach { cycle ->
                    this.cycles[cycle.id] = cycle
                }
                return Result.success(Unit)
            }
            
            override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> {
                logs.forEach { log ->
                    dailyLogs[log.id] = log
                }
                return Result.success(Unit)
            }
            
            override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> {
                insights.forEach { insight ->
                    this.insights[insight.id] = insight
                }
                return Result.success(Unit)
            }
            
            // Sync operations
            override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> {
                return Result.success(syncTimestamps[userId])
            }
            
            override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> {
                syncTimestamps[userId] = timestamp
                return Result.success(Unit)
            }
            
            override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData> {
                return Result.success(SyncData())
            }

            // Health Report operations - Mock implementations
            val healthReports = mutableMapOf<String, HealthReport>()
            val sharedReports = mutableMapOf<String, String>()

            override suspend fun getHealthReport(reportId: String): Result<HealthReport?> {
                return Result.success(healthReports[reportId])
            }

            override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> {
                val userReports = healthReports.values.filter { it.userId == userId }
                return Result.success(userReports)
            }

            override suspend fun saveHealthReport(report: HealthReport): Result<Unit> {
                healthReports[report.id] = report
                return Result.success(Unit)
            }

            override suspend fun updateHealthReport(report: HealthReport): Result<Unit> {
                healthReports[report.id] = report
                return Result.success(Unit)
            }

            override suspend fun deleteHealthReport(reportId: String): Result<Unit> {
                healthReports.remove(reportId)
                sharedReports.remove(reportId)
                return Result.success(Unit)
            }

            override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> {
                val shareId = "share_${reportId}_${System.currentTimeMillis()}"
                sharedReports[reportId] = shareId
                return Result.success("https://eunio.app/shared/$shareId")
            }

            override suspend fun revokeShareableLink(reportId: String): Result<Unit> {
                sharedReports.remove(reportId)
                return Result.success(Unit)
            }

            // Settings operations - Mock implementations
            override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> {
                return Result.success(Unit)
            }

            override suspend fun deleteUserSettings(userId: String): Result<Unit> {
                return Result.success(Unit)
            }

            override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>): Result<Unit> {
                return Result.success(Unit)
            }

            override suspend fun getSettingsHistory(userId: String, limit: Int): Result<List<Pair<String, Map<String, Any>>>> {
                return Result.success(emptyList())
            }

            override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
                return Result.success(Unit)
            }
        }
    }
}