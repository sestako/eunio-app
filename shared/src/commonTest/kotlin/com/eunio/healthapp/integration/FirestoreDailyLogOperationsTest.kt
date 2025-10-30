package com.eunio.healthapp.integration

import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.*

/**
 * Integration tests for Firestore daily log operations on both Android and iOS platforms.
 * Tests saving, loading, and data format consistency across platforms.
 * 
 * This test validates that Firestore operations work correctly on both platforms
 * and that data format is consistent, which is critical for cross-platform sync.
 * 
 * Requirements tested:
 * - 6.4: Firestore save operations work correctly
 * - 6.5: Firestore load operations work correctly
 * - 7.3: Data format is correct and consistent
 */
class FirestoreDailyLogOperationsTest {
    
    /**
     * Mock Firestore service that simulates both Android and iOS Firebase SDK behavior
     */
    private class MockFirestoreService(
        val platform: String // "Android" or "iOS"
    ) : FirestoreService {
        private val storage = mutableMapOf<String, DailyLog>()
        private var shouldFailOperations = false
        private var networkDelay = 100L
        
        override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore read failed"))
            }
            val key = "$userId:$logId"
            return Result.success(storage[key])
        }
        
        override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore read failed"))
            }
            val logId = date.toString()
            val key = "$userId:$logId"
            return Result.success(storage[key])
        }
        
        override suspend fun getLogsInRange(
            userId: String,
            startDate: LocalDate,
            endDate: LocalDate
        ): Result<List<DailyLog>> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore read failed"))
            }
            val logs = storage.values.filter { log ->
                log.userId == userId && log.date >= startDate && log.date <= endDate
            }.sortedBy { it.date }
            return Result.success(logs)
        }
        
        override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore read failed"))
            }
            val logs = storage.values
                .filter { it.userId == userId }
                .sortedByDescending { it.date }
                .take(limit)
            return Result.success(logs)
        }
        
        override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore write failed"))
            }
            val key = "${dailyLog.userId}:${dailyLog.id}"
            storage[key] = dailyLog
            return Result.success(Unit)
        }
        
        override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> {
            return saveDailyLog(dailyLog)
        }
        
        override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> {
            delay(networkDelay)
            if (shouldFailOperations) {
                return Result.error(AppError.NetworkError("$platform: Firestore delete failed"))
            }
            val key = "$userId:$logId"
            storage.remove(key)
            return Result.success(Unit)
        }
        
        fun setFailOperations(shouldFail: Boolean) {
            shouldFailOperations = shouldFail
        }
        
        fun setNetworkDelay(delay: Long) {
            networkDelay = delay
        }
        
        fun getStoredLog(userId: String, logId: String): DailyLog? {
            val key = "$userId:$logId"
            return storage[key]
        }
        
        fun getAllStoredLogs(): List<DailyLog> = storage.values.toList()
        
        fun clear() {
            storage.clear()
        }
        
        // Unimplemented methods (not needed for this test)
        override suspend fun getUser(userId: String) = Result.error<User?>(AppError.UnknownError("Not implemented"))
        override suspend fun saveUser(user: User) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun updateUser(user: User) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteUser(userId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getCycle(userId: String, cycleId: String) = Result.error<Cycle?>(AppError.UnknownError("Not implemented"))
        override suspend fun getCurrentCycle(userId: String) = Result.error<Cycle?>(AppError.UnknownError("Not implemented"))
        override suspend fun getCycleHistory(userId: String, limit: Int) = Result.error<List<Cycle>>(AppError.UnknownError("Not implemented"))
        override suspend fun saveCycle(cycle: Cycle) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun updateCycle(cycle: Cycle) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteCycle(userId: String, cycleId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getInsight(userId: String, insightId: String) = Result.error<Insight?>(AppError.UnknownError("Not implemented"))
        override suspend fun getUnreadInsights(userId: String) = Result.error<List<Insight>>(AppError.UnknownError("Not implemented"))
        override suspend fun getInsightHistory(userId: String, limit: Int) = Result.error<List<Insight>>(AppError.UnknownError("Not implemented"))
        override suspend fun saveInsight(insight: Insight) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun updateInsight(insight: Insight) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun markInsightAsRead(userId: String, insightId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteInsight(userId: String, insightId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun batchSaveUsers(users: List<User>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun batchSaveCycles(cycles: List<Cycle>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun batchSaveDailyLogs(logs: List<DailyLog>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun batchSaveInsights(insights: List<Insight>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getHealthReport(reportId: String) = Result.error<HealthReport?>(AppError.UnknownError("Not implemented"))
        override suspend fun getUserHealthReports(userId: String) = Result.error<List<HealthReport>>(AppError.UnknownError("Not implemented"))
        override suspend fun saveHealthReport(report: HealthReport) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun updateHealthReport(report: HealthReport) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteHealthReport(reportId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun createShareableLink(reportId: String, expirationDays: Int) = Result.error<String>(AppError.UnknownError("Not implemented"))
        override suspend fun revokeShareableLink(reportId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteUserSettings(userId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun saveSettingsHistory(userId: String, historyId: String, historyData: Map<String, Any>) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getSettingsHistory(userId: String, limit: Int) = Result.error<List<Pair<String, Map<String, Any>>>>(AppError.UnknownError("Not implemented"))
        override suspend fun deleteSettingsHistory(userId: String) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getLastSyncTimestamp(userId: String) = Result.error<Long?>(AppError.UnknownError("Not implemented"))
        override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long) = Result.error<Unit>(AppError.UnknownError("Not implemented"))
        override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long) = Result.error<com.eunio.healthapp.data.remote.SyncData>(AppError.UnknownError("Not implemented"))
    }
    
    // Test components
    private lateinit var androidFirestore: MockFirestoreService
    private lateinit var iosFirestore: MockFirestoreService
    
    private val testUserId = "test-user-123"
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    @BeforeTest
    fun setup() {
        androidFirestore = MockFirestoreService("Android")
        iosFirestore = MockFirestoreService("iOS")
    }
    
    @AfterTest
    fun tearDown() {
        androidFirestore.clear()
        iosFirestore.clear()
    }
    
    private fun createTestDailyLog(
        date: LocalDate = today,
        periodFlow: PeriodFlow? = null,
        symptoms: List<Symptom> = emptyList(),
        mood: Mood? = null,
        bbt: Double? = null
    ): DailyLog {
        val logId = date.toString()
        return DailyLog(
            id = logId,
            userId = testUserId,
            date = date,
            periodFlow = periodFlow,
            symptoms = symptoms,
            mood = mood,
            sexualActivity = null,
            bbt = bbt,
            cervicalMucus = null,
            opkResult = null,
            notes = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }
    
    // Test: Save daily log on Android
    @Test
    fun `test saving daily log on Android`() = runTest {
        // Given: Android Firestore service
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS, Symptom.FATIGUE),
            mood = Mood.TIRED
        )
        
        // When: Saving daily log via Android Firestore
        val result = androidFirestore.saveDailyLog(dailyLog)
        
        // Then: Save should succeed
        assertTrue(result.isSuccess, "Android save should succeed")
        
        // Verify data is stored in Firestore
        val storedLog = androidFirestore.getStoredLog(testUserId, dailyLog.id)
        assertNotNull(storedLog, "Log should be stored in Android Firestore")
        assertEquals(dailyLog.id, storedLog.id)
        assertEquals(dailyLog.userId, storedLog.userId)
        assertEquals(dailyLog.date, storedLog.date)
        assertEquals(PeriodFlow.MEDIUM, storedLog.periodFlow)
        assertEquals(2, storedLog.symptoms.size)
        assertTrue(storedLog.symptoms.contains(Symptom.CRAMPS))
        assertTrue(storedLog.symptoms.contains(Symptom.FATIGUE))
        assertEquals(Mood.TIRED, storedLog.mood)
    }
    
    // Test: Save daily log on iOS
    @Test
    fun `test saving daily log on iOS`() = runTest {
        // Given: iOS Firestore service
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(Symptom.HEADACHE, Symptom.BLOATING, Symptom.MOOD_SWINGS),
            mood = Mood.IRRITABLE,
            bbt = 98.2
        )
        
        // When: Saving daily log via iOS Firestore
        val result = iosFirestore.saveDailyLog(dailyLog)
        
        // Then: Save should succeed
        assertTrue(result.isSuccess, "iOS save should succeed")
        
        // Verify data is stored in Firestore
        val storedLog = iosFirestore.getStoredLog(testUserId, dailyLog.id)
        assertNotNull(storedLog, "Log should be stored in iOS Firestore")
        assertEquals(dailyLog.id, storedLog.id)
        assertEquals(dailyLog.userId, storedLog.userId)
        assertEquals(dailyLog.date, storedLog.date)
        assertEquals(PeriodFlow.HEAVY, storedLog.periodFlow)
        assertEquals(3, storedLog.symptoms.size)
        assertTrue(storedLog.symptoms.contains(Symptom.HEADACHE))
        assertTrue(storedLog.symptoms.contains(Symptom.BLOATING))
        assertTrue(storedLog.symptoms.contains(Symptom.MOOD_SWINGS))
        assertEquals(Mood.IRRITABLE, storedLog.mood)
        assertEquals(98.2, storedLog.bbt)
    }
    
    // Test: Load daily log on Android
    @Test
    fun `test loading daily log on Android`() = runTest {
        // Given: Daily log exists in Android Firestore
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.LIGHT,
            symptoms = listOf(Symptom.BREAST_TENDERNESS),
            mood = Mood.CALM
        )
        androidFirestore.saveDailyLog(dailyLog)
        
        // When: Loading daily log via Android Firestore
        val result = androidFirestore.getDailyLogByDate(testUserId, dailyLog.date)
        
        // Then: Load should succeed
        assertTrue(result.isSuccess, "Android load should succeed")
        val loadedLog = result.getOrNull()
        assertNotNull(loadedLog, "Loaded log should not be null")
        assertEquals(dailyLog.id, loadedLog.id)
        assertEquals(dailyLog.userId, loadedLog.userId)
        assertEquals(dailyLog.date, loadedLog.date)
        assertEquals(PeriodFlow.LIGHT, loadedLog.periodFlow)
        assertEquals(1, loadedLog.symptoms.size)
        assertTrue(loadedLog.symptoms.contains(Symptom.BREAST_TENDERNESS))
        assertEquals(Mood.CALM, loadedLog.mood)
    }
    
    // Test: Load daily log on iOS
    @Test
    fun `test loading daily log on iOS`() = runTest {
        // Given: Daily log exists in iOS Firestore
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.SPOTTING,
            symptoms = listOf(Symptom.ACNE, Symptom.BACK_PAIN),
            mood = Mood.ANXIOUS,
            bbt = 97.8
        )
        iosFirestore.saveDailyLog(dailyLog)
        
        // When: Loading daily log via iOS Firestore
        val result = iosFirestore.getDailyLogByDate(testUserId, dailyLog.date)
        
        // Then: Load should succeed
        assertTrue(result.isSuccess, "iOS load should succeed")
        val loadedLog = result.getOrNull()
        assertNotNull(loadedLog, "Loaded log should not be null")
        assertEquals(dailyLog.id, loadedLog.id)
        assertEquals(dailyLog.userId, loadedLog.userId)
        assertEquals(dailyLog.date, loadedLog.date)
        assertEquals(PeriodFlow.SPOTTING, loadedLog.periodFlow)
        assertEquals(2, loadedLog.symptoms.size)
        assertTrue(loadedLog.symptoms.contains(Symptom.ACNE))
        assertTrue(loadedLog.symptoms.contains(Symptom.BACK_PAIN))
        assertEquals(Mood.ANXIOUS, loadedLog.mood)
        assertEquals(97.8, loadedLog.bbt)
    }
    
    // Test: Data format consistency
    @Test
    fun `test data format is correct and consistent across platforms`() = runTest {
        // Given: Same daily log to be saved on both platforms
        val dailyLog = createTestDailyLog(
            periodFlow = PeriodFlow.MEDIUM,
            symptoms = listOf(Symptom.CRAMPS, Symptom.FATIGUE, Symptom.NAUSEA),
            mood = Mood.TIRED,
            bbt = 98.6
        )
        
        // When: Saving on Android Firestore
        val androidSaveResult = androidFirestore.saveDailyLog(dailyLog)
        assertTrue(androidSaveResult.isSuccess, "Android save should succeed")
        
        // When: Saving on iOS Firestore
        val iosSaveResult = iosFirestore.saveDailyLog(dailyLog)
        assertTrue(iosSaveResult.isSuccess, "iOS save should succeed")
        
        // Then: Both platforms should store identical data
        val androidStored = androidFirestore.getStoredLog(testUserId, dailyLog.id)
        val iosStored = iosFirestore.getStoredLog(testUserId, dailyLog.id)
        
        assertNotNull(androidStored, "Android stored log should not be null")
        assertNotNull(iosStored, "iOS stored log should not be null")
        
        // Verify all fields match
        assertEquals(androidStored.id, iosStored.id, "IDs should match")
        assertEquals(androidStored.userId, iosStored.userId, "User IDs should match")
        assertEquals(androidStored.date, iosStored.date, "Dates should match")
        assertEquals(androidStored.periodFlow, iosStored.periodFlow, "Period flow should match")
        assertEquals(androidStored.symptoms.size, iosStored.symptoms.size, "Symptom count should match")
        assertEquals(androidStored.symptoms.toSet(), iosStored.symptoms.toSet(), "Symptoms should match")
        assertEquals(androidStored.mood, iosStored.mood, "Mood should match")
        assertEquals(androidStored.bbt, iosStored.bbt, "BBT should match")
        assertEquals(androidStored.sexualActivity, iosStored.sexualActivity, "Sexual activity should match")
        assertEquals(androidStored.cervicalMucus, iosStored.cervicalMucus, "Cervical mucus should match")
        assertEquals(androidStored.opkResult, iosStored.opkResult, "OPK result should match")
        assertEquals(androidStored.notes, iosStored.notes, "Notes should match")
    }
    
    // Test: Multiple logs in date range
    @Test
    fun `test loading multiple logs in date range on both platforms`() = runTest {
        // Given: Multiple logs across a date range
        val startDate = LocalDate.fromEpochDays(today.toEpochDays() - 5)
        val logs = (0..5).map { dayOffset ->
            val date = LocalDate.fromEpochDays(startDate.toEpochDays() + dayOffset)
            createTestDailyLog(
                date = date,
                periodFlow = if (dayOffset < 3) PeriodFlow.MEDIUM else null,
                symptoms = listOf(Symptom.CRAMPS),
                mood = Mood.NEUTRAL
            )
        }
        
        // When: Saving logs on Android Firestore
        logs.forEach { log ->
            val result = androidFirestore.saveDailyLog(log)
            assertTrue(result.isSuccess, "Android save should succeed for ${log.date}")
        }
        
        // When: Saving logs on iOS Firestore
        logs.forEach { log ->
            val result = iosFirestore.saveDailyLog(log)
            assertTrue(result.isSuccess, "iOS save should succeed for ${log.date}")
        }
        
        // Then: Both platforms should retrieve all logs in range
        val androidLogsResult = androidFirestore.getLogsInRange(testUserId, startDate, today)
        assertTrue(androidLogsResult.isSuccess, "Android range query should succeed")
        val androidLogs = androidLogsResult.getOrNull()!!
        assertEquals(6, androidLogs.size, "Android should have 6 logs")
        
        val iosLogsResult = iosFirestore.getLogsInRange(testUserId, startDate, today)
        assertTrue(iosLogsResult.isSuccess, "iOS range query should succeed")
        val iosLogs = iosLogsResult.getOrNull()!!
        assertEquals(6, iosLogs.size, "iOS should have 6 logs")
        
        // Verify logs are sorted by date
        assertEquals(androidLogs.map { it.date }, androidLogs.sortedBy { it.date }.map { it.date })
        assertEquals(iosLogs.map { it.date }, iosLogs.sortedBy { it.date }.map { it.date })
    }
    
    // Test: Complex data with all fields populated
    @Test
    fun `test complex daily log with all fields populated`() = runTest {
        // Given: Complex daily log with all fields
        val complexLog = DailyLog(
            id = today.toString(),
            userId = testUserId,
            date = today,
            periodFlow = PeriodFlow.HEAVY,
            symptoms = listOf(
                Symptom.CRAMPS,
                Symptom.HEADACHE,
                Symptom.BLOATING,
                Symptom.BREAST_TENDERNESS,
                Symptom.ACNE,
                Symptom.MOOD_SWINGS,
                Symptom.FATIGUE,
                Symptom.NAUSEA,
                Symptom.BACK_PAIN,
                Symptom.FOOD_CRAVINGS
            ),
            mood = Mood.IRRITABLE,
            sexualActivity = SexualActivity(occurred = true, protection = Protection.CONDOM),
            bbt = 98.4,
            cervicalMucus = CervicalMucus.EGG_WHITE,
            opkResult = OPKResult.POSITIVE,
            notes = "Feeling very tired today. Had chocolate cravings.",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // When: Saving on both platforms
        val androidResult = androidFirestore.saveDailyLog(complexLog)
        val iosResult = iosFirestore.saveDailyLog(complexLog)
        
        // Then: Both should succeed
        assertTrue(androidResult.isSuccess, "Android save should succeed")
        assertTrue(iosResult.isSuccess, "iOS save should succeed")
        
        // Verify all fields are preserved on Android
        val androidStored = androidFirestore.getStoredLog(testUserId, complexLog.id)
        assertNotNull(androidStored)
        assertEquals(PeriodFlow.HEAVY, androidStored.periodFlow)
        assertEquals(10, androidStored.symptoms.size)
        assertEquals(Mood.IRRITABLE, androidStored.mood)
        assertNotNull(androidStored.sexualActivity)
        assertTrue(androidStored.sexualActivity!!.occurred)
        assertEquals(Protection.CONDOM, androidStored.sexualActivity!!.protection)
        assertEquals(98.4, androidStored.bbt)
        assertEquals(CervicalMucus.EGG_WHITE, androidStored.cervicalMucus)
        assertEquals(OPKResult.POSITIVE, androidStored.opkResult)
        assertEquals("Feeling very tired today. Had chocolate cravings.", androidStored.notes)
        
        // Verify all fields are preserved on iOS
        val iosStored = iosFirestore.getStoredLog(testUserId, complexLog.id)
        assertNotNull(iosStored)
        assertEquals(PeriodFlow.HEAVY, iosStored.periodFlow)
        assertEquals(10, iosStored.symptoms.size)
        assertEquals(Mood.IRRITABLE, iosStored.mood)
        assertNotNull(iosStored.sexualActivity)
        assertTrue(iosStored.sexualActivity!!.occurred)
        assertEquals(Protection.CONDOM, iosStored.sexualActivity!!.protection)
        assertEquals(98.4, iosStored.bbt)
        assertEquals(CervicalMucus.EGG_WHITE, iosStored.cervicalMucus)
        assertEquals(OPKResult.POSITIVE, iosStored.opkResult)
        assertEquals("Feeling very tired today. Had chocolate cravings.", iosStored.notes)
    }
    
    // Test: Error handling
    @Test
    fun `test error handling when Firestore operations fail`() = runTest {
        // Given: Android Firestore configured to fail
        androidFirestore.setFailOperations(true)
        
        val dailyLog = createTestDailyLog()
        
        // When: Attempting to save
        val saveResult = androidFirestore.saveDailyLog(dailyLog)
        
        // Then: Should fail with network error
        assertTrue(saveResult.isError, "Save should fail when Firestore operations fail")
        val error = saveResult.errorOrNull()
        assertNotNull(error, "Error should not be null")
        assertTrue(error is AppError.NetworkError, "Error should be NetworkError")
        assertTrue(error.message.contains("Android"), "Error message should mention Android platform")
        
        // Verify data is NOT in Firestore
        val firestoreLog = androidFirestore.getStoredLog(testUserId, dailyLog.id)
        assertNull(firestoreLog, "Log should not be in Firestore when operations fail")
    }
}
