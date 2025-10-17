package com.eunio.healthapp.data.remote.datasource

import com.eunio.healthapp.data.remote.*
import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.model.settings.UserSettings
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Mock implementation of FirestoreService for testing settings synchronization.
 */
class MockFirestoreService : FirestoreService {
    
    // Mock data
    var mockUserSettings: UserSettings? = null
    var mockHistoryEntries: List<Pair<String, Map<String, Any>>> = emptyList()
    var isHealthy: Boolean = true
    var shouldThrowException: Boolean = false
    
    // Call tracking
    var getUserCalled = false
    var updateUserSettingsCalled = false
    var deleteUserSettingsCalled = false
    var saveSettingsHistoryCalled = false
    var getSettingsHistoryCalled = false
    var deleteSettingsHistoryCalled = false
    
    override suspend fun getUser(userId: String): Result<User?> {
        getUserCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        if (!isHealthy) {
            return Result.error(SettingsError.SyncError("Mock Firestore is not healthy"))
        }
        
        val user = if (mockUserSettings != null) {
            // Convert UserSettings to the map format that would be stored in Firestore
            val settingsMap = mapOf(
                "unitPreferences" to mapOf(
                    "temperatureUnit" to mockUserSettings!!.unitPreferences.temperatureUnit.name,
                    "weightUnit" to mockUserSettings!!.unitPreferences.weightUnit.name,
                    "isManuallySet" to mockUserSettings!!.unitPreferences.isManuallySet
                ),
                "notificationPreferences" to mapOf(
                    "dailyLoggingReminder" to mapOf(
                        "enabled" to mockUserSettings!!.notificationPreferences.dailyLoggingReminder.enabled,
                        "timeHour" to mockUserSettings!!.notificationPreferences.dailyLoggingReminder.time?.hour,
                        "timeMinute" to mockUserSettings!!.notificationPreferences.dailyLoggingReminder.time?.minute,
                        "daysInAdvance" to mockUserSettings!!.notificationPreferences.dailyLoggingReminder.daysInAdvance
                    ),
                    "periodPredictionAlert" to mapOf(
                        "enabled" to mockUserSettings!!.notificationPreferences.periodPredictionAlert.enabled,
                        "timeHour" to mockUserSettings!!.notificationPreferences.periodPredictionAlert.time?.hour,
                        "timeMinute" to mockUserSettings!!.notificationPreferences.periodPredictionAlert.time?.minute,
                        "daysInAdvance" to mockUserSettings!!.notificationPreferences.periodPredictionAlert.daysInAdvance
                    ),
                    "ovulationAlert" to mapOf(
                        "enabled" to mockUserSettings!!.notificationPreferences.ovulationAlert.enabled,
                        "timeHour" to mockUserSettings!!.notificationPreferences.ovulationAlert.time?.hour,
                        "timeMinute" to mockUserSettings!!.notificationPreferences.ovulationAlert.time?.minute,
                        "daysInAdvance" to mockUserSettings!!.notificationPreferences.ovulationAlert.daysInAdvance
                    ),
                    "insightNotifications" to mapOf(
                        "enabled" to mockUserSettings!!.notificationPreferences.insightNotifications.enabled,
                        "timeHour" to mockUserSettings!!.notificationPreferences.insightNotifications.time?.hour,
                        "timeMinute" to mockUserSettings!!.notificationPreferences.insightNotifications.time?.minute,
                        "daysInAdvance" to mockUserSettings!!.notificationPreferences.insightNotifications.daysInAdvance
                    ),
                    "globalNotificationsEnabled" to mockUserSettings!!.notificationPreferences.globalNotificationsEnabled
                ),
                "cyclePreferences" to mapOf(
                    "averageCycleLength" to mockUserSettings!!.cyclePreferences.averageCycleLength,
                    "averageLutealPhaseLength" to mockUserSettings!!.cyclePreferences.averageLutealPhaseLength,
                    "periodDuration" to mockUserSettings!!.cyclePreferences.periodDuration,
                    "isCustomized" to mockUserSettings!!.cyclePreferences.isCustomized
                ),
                "privacyPreferences" to mapOf(
                    "dataSharingEnabled" to mockUserSettings!!.privacyPreferences.dataSharingEnabled,
                    "anonymousInsightsEnabled" to mockUserSettings!!.privacyPreferences.anonymousInsightsEnabled,
                    "crashReportingEnabled" to mockUserSettings!!.privacyPreferences.crashReportingEnabled,
                    "analyticsEnabled" to mockUserSettings!!.privacyPreferences.analyticsEnabled
                ),
                "displayPreferences" to mapOf(
                    "textSizeScale" to mockUserSettings!!.displayPreferences.textSizeScale,
                    "highContrastMode" to mockUserSettings!!.displayPreferences.highContrastMode,
                    "hapticFeedbackEnabled" to mockUserSettings!!.displayPreferences.hapticFeedbackEnabled,
                    "hapticIntensity" to mockUserSettings!!.displayPreferences.hapticIntensity.name
                ),
                "syncPreferences" to mapOf(
                    "autoSyncEnabled" to mockUserSettings!!.syncPreferences.autoSyncEnabled,
                    "wifiOnlySync" to mockUserSettings!!.syncPreferences.wifiOnlySync,
                    "cloudBackupEnabled" to mockUserSettings!!.syncPreferences.cloudBackupEnabled,
                    "lastSyncTime" to mockUserSettings!!.syncPreferences.lastSyncTime?.toEpochMilliseconds()
                ),
                "lastModified" to mockUserSettings!!.lastModified.toEpochMilliseconds(),
                "syncStatus" to mockUserSettings!!.syncStatus.name,
                "version" to mockUserSettings!!.version
            )
            
            User(
                id = userId,
                email = "test@example.com",
                name = "Test User",
                onboardingComplete = true,
                primaryGoal = HealthGoal.CYCLE_TRACKING,
                unitSystem = UnitSystem.METRIC,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                settings = settingsMap
            )
        } else {
            null
        }
        
        return Result.success(user)
    }
    
    override suspend fun updateUserSettings(userId: String, settings: Map<String, Any>): Result<Unit> {
        updateUserSettingsCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun deleteUserSettings(userId: String): Result<Unit> {
        deleteUserSettingsCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun saveSettingsHistory(
        userId: String,
        historyId: String,
        historyData: Map<String, Any>
    ): Result<Unit> {
        saveSettingsHistoryCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun getSettingsHistory(
        userId: String,
        limit: Int
    ): Result<List<Pair<String, Map<String, Any>>>> {
        getSettingsHistoryCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        return Result.success(mockHistoryEntries)
    }
    
    override suspend fun deleteSettingsHistory(userId: String): Result<Unit> {
        deleteSettingsHistoryCalled = true
        
        if (shouldThrowException) {
            return Result.error(SettingsError.SyncError("Mock Firestore exception"))
        }
        
        return Result.success(Unit)
    }
    
    // Other FirestoreService methods (not used in settings tests)
    
    override suspend fun saveUser(user: User): Result<Unit> = Result.success(Unit)
    override suspend fun updateUser(user: User): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUser(userId: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun getCycle(userId: String, cycleId: String): Result<Cycle?> = Result.success(null)
    override suspend fun getCurrentCycle(userId: String): Result<Cycle?> = Result.success(null)
    override suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>> = Result.success(emptyList())
    override suspend fun saveCycle(cycle: Cycle): Result<Unit> = Result.success(Unit)
    override suspend fun updateCycle(cycle: Cycle): Result<Unit> = Result.success(Unit)
    override suspend fun deleteCycle(userId: String, cycleId: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun getDailyLog(userId: String, logId: String): Result<DailyLog?> = Result.success(null)
    override suspend fun getDailyLogByDate(userId: String, date: LocalDate): Result<DailyLog?> = Result.success(null)
    override suspend fun getLogsInRange(userId: String, startDate: LocalDate, endDate: LocalDate): Result<List<DailyLog>> = Result.success(emptyList())
    override suspend fun getRecentLogs(userId: String, limit: Int): Result<List<DailyLog>> = Result.success(emptyList())
    override suspend fun saveDailyLog(dailyLog: DailyLog): Result<Unit> = Result.success(Unit)
    override suspend fun updateDailyLog(dailyLog: DailyLog): Result<Unit> = Result.success(Unit)
    override suspend fun deleteDailyLog(userId: String, logId: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun getInsight(userId: String, insightId: String): Result<Insight?> = Result.success(null)
    override suspend fun getUnreadInsights(userId: String): Result<List<Insight>> = Result.success(emptyList())
    override suspend fun getInsightHistory(userId: String, limit: Int): Result<List<Insight>> = Result.success(emptyList())
    override suspend fun saveInsight(insight: Insight): Result<Unit> = Result.success(Unit)
    override suspend fun updateInsight(insight: Insight): Result<Unit> = Result.success(Unit)
    override suspend fun markInsightAsRead(userId: String, insightId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteInsight(userId: String, insightId: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun batchSaveUsers(users: List<User>): Result<Unit> = Result.success(Unit)
    override suspend fun batchSaveCycles(cycles: List<Cycle>): Result<Unit> = Result.success(Unit)
    override suspend fun batchSaveDailyLogs(logs: List<DailyLog>): Result<Unit> = Result.success(Unit)
    override suspend fun batchSaveInsights(insights: List<Insight>): Result<Unit> = Result.success(Unit)
    
    override suspend fun getHealthReport(reportId: String): Result<HealthReport?> = Result.success(null)
    override suspend fun getUserHealthReports(userId: String): Result<List<HealthReport>> = Result.success(emptyList())
    override suspend fun saveHealthReport(report: HealthReport): Result<Unit> = Result.success(Unit)
    override suspend fun updateHealthReport(report: HealthReport): Result<Unit> = Result.success(Unit)
    override suspend fun deleteHealthReport(reportId: String): Result<Unit> = Result.success(Unit)
    override suspend fun createShareableLink(reportId: String, expirationDays: Int): Result<String> = Result.success("mock-link")
    override suspend fun revokeShareableLink(reportId: String): Result<Unit> = Result.success(Unit)
    
    override suspend fun getLastSyncTimestamp(userId: String): Result<Long?> = Result.success(Clock.System.now().toEpochMilliseconds())
    override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long): Result<Unit> = Result.success(Unit)
    override suspend fun getChangedDocumentsSince(userId: String, timestamp: Long): Result<SyncData> = Result.success(SyncData())
}