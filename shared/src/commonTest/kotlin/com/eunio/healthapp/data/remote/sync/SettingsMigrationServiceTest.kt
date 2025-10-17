package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.error.SettingsError
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Unit tests for SettingsMigrationService.
 * Tests settings schema migration and versioning.
 */
class SettingsMigrationServiceTest {
    
    private lateinit var migrationService: SettingsMigrationService
    
    @BeforeTest
    fun setup() {
        migrationService = SettingsMigrationService()
    }
    
    @Test
    fun `migrateSettings should return same settings when versions are equal`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 1)
        
        // When
        val result = migrationService.migrateSettings(settings, fromVersion = 1, toVersion = 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()!!
        assertEquals(settings.userId, migratedSettings.userId)
        assertEquals(1, migratedSettings.version)
    }
    
    @Test
    fun `migrateSettings should fail for unsupported old version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 0)
        
        // When
        val result = migrationService.migrateSettings(settings, fromVersion = -1, toVersion = 1)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.MigrationError)
        val error = result.errorOrNull() as SettingsError.MigrationError
        assertEquals(-1, error.fromVersion)
        assertEquals(1, error.toVersion)
    }
    
    @Test
    fun `migrateSettings should successfully migrate from v0 to v1`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 0)
        
        // When
        val result = migrationService.migrateSettings(settings, fromVersion = 0, toVersion = 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()!!
        assertEquals(1, migratedSettings.version)
        assertEquals(settings.userId, migratedSettings.userId)
        // Should have proper defaults for all preferences
        assertNotNull(migratedSettings.unitPreferences)
        assertNotNull(migratedSettings.notificationPreferences)
        assertNotNull(migratedSettings.cyclePreferences)
        assertNotNull(migratedSettings.privacyPreferences)
        assertNotNull(migratedSettings.displayPreferences)
        assertNotNull(migratedSettings.syncPreferences)
    }
    
    @Test
    fun `migrateSettings should update lastModified timestamp`() = runTest {
        // Given
        val oldTimestamp = Clock.System.now().minus(kotlin.time.Duration.parse("1h"))
        val settings = createTestUserSettings("user1", version = 0).copy(lastModified = oldTimestamp)
        
        // When
        val result = migrationService.migrateSettings(settings, fromVersion = 0, toVersion = 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()!!
        assertTrue(migratedSettings.lastModified > oldTimestamp)
    }
    
    @Test
    fun `needsMigration should return true for older version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 0)
        
        // When
        val needsMigration = migrationService.needsMigration(settings)
        
        // Then
        assertTrue(needsMigration)
    }
    
    @Test
    fun `needsMigration should return false for current version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = migrationService.getCurrentVersion())
        
        // When
        val needsMigration = migrationService.needsMigration(settings)
        
        // Then
        assertFalse(needsMigration)
    }
    
    @Test
    fun `getCurrentVersion should return positive version number`() = runTest {
        // When
        val currentVersion = migrationService.getCurrentVersion()
        
        // Then
        assertTrue(currentVersion > 0)
    }
    
    @Test
    fun `getMinSupportedVersion should return valid version number`() = runTest {
        // When
        val minVersion = migrationService.getMinSupportedVersion()
        val currentVersion = migrationService.getCurrentVersion()
        
        // Then
        assertTrue(minVersion > 0)
        assertTrue(minVersion <= currentVersion)
    }
    
    @Test
    fun `validateSettingsVersion should succeed for current version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = migrationService.getCurrentVersion())
        
        // When
        val result = migrationService.validateSettingsVersion(settings)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateSettingsVersion should fail for unsupported old version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = migrationService.getMinSupportedVersion() - 1)
        
        // When
        val result = migrationService.validateSettingsVersion(settings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.MigrationError)
    }
    
    @Test
    fun `validateSettingsVersion should fail for future version`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = migrationService.getCurrentVersion() + 1)
        
        // When
        val result = migrationService.validateSettingsVersion(settings)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.MigrationError)
    }
    
    @Test
    fun `createDefaultSettingsForVersion should create valid settings`() = runTest {
        // Given
        val userId = "test-user-123"
        val version = 1
        
        // When
        val settings = migrationService.createDefaultSettingsForVersion(userId, version)
        
        // Then
        assertEquals(userId, settings.userId)
        assertEquals(version, settings.version)
        assertTrue(settings.isValid())
    }
    
    @Test
    fun `getMigrationInfo should return valid migration information`() = runTest {
        // Given
        val fromVersion = 0
        val toVersion = 1
        
        // When
        val migrationInfo = migrationService.getMigrationInfo(fromVersion, toVersion)
        
        // Then
        assertEquals(fromVersion, migrationInfo.fromVersion)
        assertEquals(toVersion, migrationInfo.toVersion)
        assertTrue(migrationInfo.changes.isNotEmpty())
        // For v0 to v1, there should be no breaking changes
        assertTrue(migrationInfo.breakingChanges.isEmpty())
        assertFalse(migrationInfo.requiresUserInput)
    }
    
    @Test
    fun `getMigrationInfo should indicate no changes for same version`() = runTest {
        // Given
        val version = 1
        
        // When
        val migrationInfo = migrationService.getMigrationInfo(version, version)
        
        // Then
        assertEquals(version, migrationInfo.fromVersion)
        assertEquals(version, migrationInfo.toVersion)
        assertTrue(migrationInfo.changes.isEmpty())
        assertTrue(migrationInfo.breakingChanges.isEmpty())
        assertFalse(migrationInfo.requiresUserInput)
    }
    
    @Test
    fun `migrateSettings should handle exception gracefully`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 0)
        
        // When - simulate an exception during migration by using invalid version
        val result = migrationService.migrateSettings(settings, fromVersion = -999, toVersion = 1)
        
        // Then
        assertTrue(result.isError)
        assertTrue(result.errorOrNull() is SettingsError.MigrationError)
    }
    
    @Test
    fun `migrateSettings should preserve user customizations during migration`() = runTest {
        // Given
        val customSettings = createTestUserSettings("user1", version = 0).copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 30,
                averageLutealPhaseLength = 16,
                periodDuration = 6,
                isCustomized = true
            )
        )
        
        // When
        val result = migrationService.migrateSettings(customSettings, fromVersion = 0, toVersion = 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()!!
        
        // Should preserve custom unit preferences
        assertEquals(TemperatureUnit.FAHRENHEIT, migratedSettings.unitPreferences.temperatureUnit)
        assertEquals(WeightUnit.POUNDS, migratedSettings.unitPreferences.weightUnit)
        assertTrue(migratedSettings.unitPreferences.isManuallySet)
        
        // Should preserve custom cycle preferences
        assertEquals(30, migratedSettings.cyclePreferences.averageCycleLength)
        assertEquals(16, migratedSettings.cyclePreferences.averageLutealPhaseLength)
        assertEquals(6, migratedSettings.cyclePreferences.periodDuration)
        assertTrue(migratedSettings.cyclePreferences.isCustomized)
    }
    
    @Test
    fun `migrateSettings should set proper defaults for new preferences`() = runTest {
        // Given
        val settings = createTestUserSettings("user1", version = 0)
        
        // When
        val result = migrationService.migrateSettings(settings, fromVersion = 0, toVersion = 1)
        
        // Then
        assertTrue(result.isSuccess)
        val migratedSettings = result.getOrNull()!!
        
        // Should have proper defaults for notification preferences
        assertFalse(migratedSettings.notificationPreferences.dailyLoggingReminder.enabled)
        assertTrue(migratedSettings.notificationPreferences.globalNotificationsEnabled)
        
        // Should have proper defaults for privacy preferences
        assertFalse(migratedSettings.privacyPreferences.dataSharingEnabled)
        assertTrue(migratedSettings.privacyPreferences.anonymousInsightsEnabled)
        
        // Should have proper defaults for display preferences
        assertEquals(1.0f, migratedSettings.displayPreferences.textSizeScale)
        assertFalse(migratedSettings.displayPreferences.highContrastMode)
        assertTrue(migratedSettings.displayPreferences.hapticFeedbackEnabled)
        
        // Should have proper defaults for sync preferences
        assertTrue(migratedSettings.syncPreferences.autoSyncEnabled)
        assertFalse(migratedSettings.syncPreferences.wifiOnlySync)
        assertTrue(migratedSettings.syncPreferences.cloudBackupEnabled)
    }
    
    private fun createTestUserSettings(userId: String, version: Int): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = UnitPreferences.default(),
            notificationPreferences = NotificationPreferences.default(),
            cyclePreferences = CyclePreferences.default(),
            privacyPreferences = PrivacyPreferences.default(),
            displayPreferences = DisplayPreferences.default(),
            syncPreferences = SyncPreferences.default(),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING,
            version = version
        )
    }
}