package com.eunio.healthapp.data.remote.sync

import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * Unit tests for SettingsConflictResolver.
 * Tests conflict detection and resolution strategies.
 */
class SettingsConflictResolverTest {
    
    private lateinit var conflictResolver: SettingsConflictResolver
    
    @BeforeTest
    fun setup() {
        conflictResolver = SettingsConflictResolver()
    }
    
    @Test
    fun `detectConflicts should return empty list when settings are identical`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy()
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertTrue(conflicts.isEmpty())
    }
    
    @Test
    fun `detectConflicts should detect unit preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            unitPreferences = settings1.unitPreferences.copy(
                temperatureUnit = TemperatureUnit.FAHRENHEIT
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.UNIT_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect notification preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            notificationPreferences = settings1.notificationPreferences.copy(
                globalNotificationsEnabled = false
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.NOTIFICATION_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect cycle preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            cyclePreferences = settings1.cyclePreferences.copy(
                averageCycleLength = 30
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.CYCLE_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect privacy preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            privacyPreferences = settings1.privacyPreferences.copy(
                dataSharingEnabled = true
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.PRIVACY_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect display preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            displayPreferences = settings1.displayPreferences.copy(
                textSizeScale = 1.5f
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.DISPLAY_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect sync preferences conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            syncPreferences = settings1.syncPreferences.copy(
                autoSyncEnabled = false
            )
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(1, conflicts.size)
        assertEquals(SettingsConflictType.SYNC_PREFERENCES, conflicts[0].type)
    }
    
    @Test
    fun `detectConflicts should detect multiple conflicts`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            unitPreferences = settings1.unitPreferences.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT),
            notificationPreferences = settings1.notificationPreferences.copy(globalNotificationsEnabled = false),
            cyclePreferences = settings1.cyclePreferences.copy(averageCycleLength = 30)
        )
        
        // When
        val conflicts = conflictResolver.detectConflicts(settings1, settings2)
        
        // Then
        assertEquals(3, conflicts.size)
        assertTrue(conflicts.any { it.type == SettingsConflictType.UNIT_PREFERENCES })
        assertTrue(conflicts.any { it.type == SettingsConflictType.NOTIFICATION_PREFERENCES })
        assertTrue(conflicts.any { it.type == SettingsConflictType.CYCLE_PREFERENCES })
    }
    
    @Test
    fun `hasConflicts should return true when conflicts exist`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy(
            unitPreferences = settings1.unitPreferences.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        )
        
        // When
        val hasConflicts = conflictResolver.hasConflicts(settings1, settings2)
        
        // Then
        assertTrue(hasConflicts)
    }
    
    @Test
    fun `hasConflicts should return false when no conflicts exist`() = runTest {
        // Given
        val settings1 = createTestUserSettings("user1")
        val settings2 = settings1.copy()
        
        // When
        val hasConflicts = conflictResolver.hasConflicts(settings1, settings2)
        
        // Then
        assertFalse(hasConflicts)
    }
    
    @Test
    fun `resolveConflict with LAST_WRITE_WINS should choose more recent settings`() = runTest {
        // Given
        val now = Clock.System.now()
        val earlier = now.minus(kotlin.time.Duration.parse("1h"))
        
        val localSettings = createTestUserSettings("user1").copy(lastModified = now)
        val remoteSettings = createTestUserSettings("user1").copy(
            lastModified = earlier,
            unitPreferences = UnitPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        )
        
        // When
        val result = conflictResolver.resolveConflict(
            localSettings,
            remoteSettings,
            ConflictResolutionStrategy.LAST_WRITE_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolved = result.getOrNull()!!
        assertEquals(TemperatureUnit.CELSIUS, resolved.unitPreferences.temperatureUnit) // Local wins
    }
    
    @Test
    fun `resolveConflict with LOCAL_WINS should choose local settings`() = runTest {
        // Given
        val localSettings = createTestUserSettings("user1")
        val remoteSettings = createTestUserSettings("user1").copy(
            unitPreferences = UnitPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        )
        
        // When
        val result = conflictResolver.resolveConflict(
            localSettings,
            remoteSettings,
            ConflictResolutionStrategy.LOCAL_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolved = result.getOrNull()!!
        assertEquals(TemperatureUnit.CELSIUS, resolved.unitPreferences.temperatureUnit) // Local wins
    }
    
    @Test
    fun `resolveConflict with REMOTE_WINS should choose remote settings`() = runTest {
        // Given
        val localSettings = createTestUserSettings("user1")
        val remoteSettings = createTestUserSettings("user1").copy(
            unitPreferences = UnitPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        )
        
        // When
        val result = conflictResolver.resolveConflict(
            localSettings,
            remoteSettings,
            ConflictResolutionStrategy.REMOTE_WINS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolved = result.getOrNull()!!
        assertEquals(TemperatureUnit.FAHRENHEIT, resolved.unitPreferences.temperatureUnit) // Remote wins
    }
    
    @Test
    fun `resolveConflict with MERGE_FIELDS should merge settings intelligently`() = runTest {
        // Given
        val localSettings = createTestUserSettings("user1").copy(
            unitPreferences = UnitPreferences(temperatureUnit = TemperatureUnit.CELSIUS, isManuallySet = true),
            notificationPreferences = NotificationPreferences(globalNotificationsEnabled = false)
        )
        val remoteSettings = createTestUserSettings("user1").copy(
            unitPreferences = UnitPreferences(temperatureUnit = TemperatureUnit.FAHRENHEIT, isManuallySet = false),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(enabled = true),
                globalNotificationsEnabled = true
            )
        )
        
        // When
        val result = conflictResolver.resolveConflict(
            localSettings,
            remoteSettings,
            ConflictResolutionStrategy.MERGE_FIELDS
        )
        
        // Then
        assertTrue(result.isSuccess)
        val resolved = result.getOrNull()!!
        // Should prefer manually set unit preferences
        assertEquals(TemperatureUnit.CELSIUS, resolved.unitPreferences.temperatureUnit)
        // Should prefer enabled notifications
        assertTrue(resolved.notificationPreferences.dailyLoggingReminder.enabled)
    }
    
    @Test
    fun `getRecommendedStrategy should return MANUAL_RESOLUTION for privacy conflicts`() = runTest {
        // Given
        val conflicts = listOf(
            SettingsConflict(
                type = SettingsConflictType.PRIVACY_PREFERENCES,
                localValue = PrivacyPreferences(dataSharingEnabled = false),
                remoteValue = PrivacyPreferences(dataSharingEnabled = true),
                conflictFields = listOf("dataSharingEnabled")
            )
        )
        
        // When
        val strategy = conflictResolver.getRecommendedStrategy(conflicts)
        
        // Then
        assertEquals(ConflictResolutionStrategy.MANUAL_RESOLUTION, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should return MANUAL_RESOLUTION for cycle conflicts`() = runTest {
        // Given
        val conflicts = listOf(
            SettingsConflict(
                type = SettingsConflictType.CYCLE_PREFERENCES,
                localValue = CyclePreferences(averageCycleLength = 28),
                remoteValue = CyclePreferences(averageCycleLength = 30),
                conflictFields = listOf("averageCycleLength")
            )
        )
        
        // When
        val strategy = conflictResolver.getRecommendedStrategy(conflicts)
        
        // Then
        assertEquals(ConflictResolutionStrategy.MANUAL_RESOLUTION, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should return MERGE_FIELDS for multiple conflicts`() = runTest {
        // Given
        val conflicts = listOf(
            SettingsConflict(
                type = SettingsConflictType.UNIT_PREFERENCES,
                localValue = UnitPreferences(),
                remoteValue = UnitPreferences(),
                conflictFields = listOf("temperatureUnit")
            ),
            SettingsConflict(
                type = SettingsConflictType.DISPLAY_PREFERENCES,
                localValue = DisplayPreferences(),
                remoteValue = DisplayPreferences(),
                conflictFields = listOf("textSizeScale")
            )
        )
        
        // When
        val strategy = conflictResolver.getRecommendedStrategy(conflicts)
        
        // Then
        assertEquals(ConflictResolutionStrategy.MERGE_FIELDS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should return LAST_WRITE_WINS for single non-critical conflict`() = runTest {
        // Given
        val conflicts = listOf(
            SettingsConflict(
                type = SettingsConflictType.DISPLAY_PREFERENCES,
                localValue = DisplayPreferences(),
                remoteValue = DisplayPreferences(),
                conflictFields = listOf("textSizeScale")
            )
        )
        
        // When
        val strategy = conflictResolver.getRecommendedStrategy(conflicts)
        
        // Then
        assertEquals(ConflictResolutionStrategy.LAST_WRITE_WINS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should return LAST_WRITE_WINS for no conflicts`() = runTest {
        // Given
        val conflicts = emptyList<SettingsConflict>()
        
        // When
        val strategy = conflictResolver.getRecommendedStrategy(conflicts)
        
        // Then
        assertEquals(ConflictResolutionStrategy.LAST_WRITE_WINS, strategy)
    }
    
    private fun createTestUserSettings(userId: String): UserSettings {
        return UserSettings(
            userId = userId,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = false
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(enabled = false),
                globalNotificationsEnabled = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 28,
                averageLutealPhaseLength = 14,
                periodDuration = 5,
                isCustomized = false
            ),
            privacyPreferences = PrivacyPreferences(
                dataSharingEnabled = false,
                anonymousInsightsEnabled = true,
                crashReportingEnabled = true,
                analyticsEnabled = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.0f,
                highContrastMode = false,
                hapticFeedbackEnabled = true,
                hapticIntensity = HapticIntensity.MEDIUM
            ),
            syncPreferences = SyncPreferences(
                autoSyncEnabled = true,
                wifiOnlySync = false,
                cloudBackupEnabled = true,
                lastSyncTime = Clock.System.now()
            ),
            lastModified = Clock.System.now(),
            syncStatus = SyncStatus.PENDING,
            version = 1
        )
    }
}