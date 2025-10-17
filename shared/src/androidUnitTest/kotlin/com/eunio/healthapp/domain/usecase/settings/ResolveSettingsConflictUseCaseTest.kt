package com.eunio.healthapp.domain.usecase.settings

import com.eunio.healthapp.domain.manager.ConflictResolutionStrategy
import com.eunio.healthapp.domain.manager.SettingsBackupManager
import com.eunio.healthapp.domain.model.SyncStatus
import com.eunio.healthapp.domain.model.settings.*
import com.eunio.healthapp.domain.repository.SettingsRepository
import com.eunio.healthapp.domain.util.Result
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

class ResolveSettingsConflictUseCaseTest {
    
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsBackupManager: SettingsBackupManager
    private lateinit var useCase: ResolveSettingsConflictUseCase
    
    private val testUserId = "test-user-123"
    
    @BeforeTest
    fun setup() {
        settingsRepository = mockk()
        settingsBackupManager = mockk()
        
        useCase = ResolveSettingsConflictUseCase(
            settingsRepository = settingsRepository,
            settingsBackupManager = settingsBackupManager
        )
    }
    
    @Test
    fun `detectConflict should return null when no conflict exists`() = runTest {
        // Given
        val settings = createTestUserSettings().copy(syncStatus = SyncStatus.SYNCED)
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(settings)
        
        // When
        val result = useCase.detectConflict(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
        
        coVerify { settingsRepository.getUserSettings(testUserId) }
    }
    
    @Test
    fun `detectConflict should return null when settings need sync but no remote comparison available`() = runTest {
        // Given
        val settings = createTestUserSettings().copy(syncStatus = SyncStatus.PENDING)
        coEvery { settingsRepository.getUserSettings(testUserId) } returns Result.success(settings)
        
        // When
        val result = useCase.detectConflict(testUserId)
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow()) // No conflict detected without remote data
        
        coVerify { settingsRepository.getUserSettings(testUserId) }
    }
    
    @Test
    fun `resolveConflict should delegate to backup manager`() = runTest {
        // Given
        val localSettings = createTestUserSettings()
        val remoteSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.CONCURRENT_MODIFICATIONS,
            detectedAt = Clock.System.now()
        )
        val resolvedSettings = localSettings
        
        coEvery { 
            settingsBackupManager.resolveConflictWithUserChoice(
                testUserId, 
                localSettings, 
                remoteSettings, 
                ConflictResolutionStrategy.LOCAL_WINS
            ) 
        } returns Result.success(resolvedSettings)
        
        // When
        val result = useCase.resolveConflict(conflict, ConflictResolutionStrategy.LOCAL_WINS)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(resolvedSettings, result.getOrThrow())
        
        coVerify { 
            settingsBackupManager.resolveConflictWithUserChoice(
                testUserId, 
                localSettings, 
                remoteSettings, 
                ConflictResolutionStrategy.LOCAL_WINS
            ) 
        }
    }
    
    @Test
    fun `getRecommendedStrategy should prefer LOCAL_WINS when local is significantly newer`() {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(lastModified = now)
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now.minus(kotlin.time.Duration.parse("PT2H")) // 2 hours older
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.LOCAL_NEWER_REMOTE_DIFFERENT,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.LOCAL_WINS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should prefer REMOTE_WINS when remote is significantly newer`() {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(
            lastModified = now.minus(kotlin.time.Duration.parse("PT2H")) // 2 hours older
        )
        val remoteSettings = createTestUserSettings().copy(lastModified = now)
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.REMOTE_NEWER_LOCAL_DIFFERENT,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.REMOTE_WINS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should prefer LOCAL_WINS when local has customizations and remote doesn't`() {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true // Has customizations
            )
        )
        val remoteSettings = UserSettings.createDefault(testUserId).copy(
            lastModified = now
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.SAME_TIMESTAMP_DIFFERENT_CONTENT,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.LOCAL_WINS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should prefer REMOTE_WINS when remote has customizations and local doesn't`() {
        // Given
        val now = Clock.System.now()
        val localSettings = UserSettings.createDefault(testUserId).copy(
            lastModified = now
        )
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true // Has customizations
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.SAME_TIMESTAMP_DIFFERENT_CONTENT,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.REMOTE_WINS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should prefer MERGE_FIELDS when both have customizations`() {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true // Has customizations
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 30,
                averageLutealPhaseLength = 12,
                periodDuration = 6,
                isCustomized = true // Has customizations
            )
        )
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true // Has customizations
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 25,
                averageLutealPhaseLength = 15,
                periodDuration = 4,
                isCustomized = true // Has customizations
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.CONCURRENT_MODIFICATIONS,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.MERGE_FIELDS, strategy)
    }
    
    @Test
    fun `getRecommendedStrategy should default to LAST_WRITE_WINS when no clear preference`() {
        // Given
        val now = Clock.System.now()
        val localSettings = UserSettings.createDefault(testUserId).copy(lastModified = now)
        val remoteSettings = UserSettings.createDefault(testUserId).copy(lastModified = now)
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.SAME_TIMESTAMP_DIFFERENT_CONTENT,
            detectedAt = now
        )
        
        // When
        val strategy = useCase.getRecommendedStrategy(conflict)
        
        // Then
        assertEquals(ConflictResolutionStrategy.LAST_WRITE_WINS, strategy)
    }
    
    @Test
    fun `previewResolution should show local settings for LOCAL_WINS strategy`() = runTest {
        // Given
        val localSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
        )
        val remoteSettings = createTestUserSettings().copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.CONCURRENT_MODIFICATIONS,
            detectedAt = Clock.System.now()
        )
        
        // When
        val result = useCase.previewResolution(conflict, ConflictResolutionStrategy.LOCAL_WINS)
        
        // Then
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals(localSettings, preview.resolvedSettings)
        assertEquals(ConflictResolutionStrategy.LOCAL_WINS, preview.strategy)
        assertTrue(preview.changes.isEmpty()) // No changes from local
    }
    
    @Test
    fun `previewResolution should show remote settings for REMOTE_WINS strategy`() = runTest {
        // Given
        val baseSettings = createTestUserSettings()
        val localSettings = baseSettings.copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
        )
        val remoteSettings = baseSettings.copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.CONCURRENT_MODIFICATIONS,
            detectedAt = Clock.System.now()
        )
        
        // When
        val result = useCase.previewResolution(conflict, ConflictResolutionStrategy.REMOTE_WINS)
        
        // Then
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals(remoteSettings, preview.resolvedSettings)
        assertEquals(ConflictResolutionStrategy.REMOTE_WINS, preview.strategy)
        assertEquals(1, preview.changes.size) // Unit preferences changed
        assertEquals("Unit Preferences", preview.changes[0].section)
    }
    
    @Test
    fun `previewResolution should show newer settings for LAST_WRITE_WINS strategy`() = runTest {
        // Given
        val now = Clock.System.now()
        val localSettings = createTestUserSettings().copy(
            lastModified = now,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = true
            )
        )
        val remoteSettings = createTestUserSettings().copy(
            lastModified = now.minus(kotlin.time.Duration.parse("PT1H")), // 1 hour older
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.LOCAL_NEWER_REMOTE_DIFFERENT,
            detectedAt = now
        )
        
        // When
        val result = useCase.previewResolution(conflict, ConflictResolutionStrategy.LAST_WRITE_WINS)
        
        // Then
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals(localSettings, preview.resolvedSettings) // Local is newer
        assertEquals(ConflictResolutionStrategy.LAST_WRITE_WINS, preview.strategy)
    }
    
    @Test
    fun `previewResolution should detect changes in multiple settings sections`() = runTest {
        // Given
        val localSettings = UserSettings.createDefault(testUserId)
        val remoteSettings = localSettings.copy(
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                weightUnit = WeightUnit.POUNDS,
                isManuallySet = true
            ),
            cyclePreferences = CyclePreferences(
                averageCycleLength = 30,
                averageLutealPhaseLength = 12,
                periodDuration = 6,
                isCustomized = true
            ),
            displayPreferences = DisplayPreferences(
                textSizeScale = 1.5f,
                highContrastMode = true,
                hapticFeedbackEnabled = false,
                hapticIntensity = HapticIntensity.LIGHT
            )
        )
        val conflict = SettingsConflict(
            userId = testUserId,
            localSettings = localSettings,
            remoteSettings = remoteSettings,
            conflictType = ConflictType.CONCURRENT_MODIFICATIONS,
            detectedAt = Clock.System.now()
        )
        
        // When
        val result = useCase.previewResolution(conflict, ConflictResolutionStrategy.REMOTE_WINS)
        
        // Then
        assertTrue(result.isSuccess)
        val preview = result.getOrThrow()
        assertEquals(3, preview.changes.size) // Unit, Cycle, and Display preferences changed
        
        val sections = preview.changes.map { it.section }
        assertTrue(sections.contains("Unit Preferences"))
        assertTrue(sections.contains("Cycle Settings"))
        assertTrue(sections.contains("Display"))
    }
    
    private fun createTestUserSettings(): UserSettings {
        return UserSettings(
            userId = testUserId,
            unitPreferences = UnitPreferences(
                temperatureUnit = TemperatureUnit.CELSIUS,
                weightUnit = WeightUnit.KILOGRAMS,
                isManuallySet = false
            ),
            notificationPreferences = NotificationPreferences(
                dailyLoggingReminder = NotificationSetting(enabled = true),
                periodPredictionAlert = NotificationSetting(enabled = true),
                ovulationAlert = NotificationSetting(enabled = false),
                insightNotifications = NotificationSetting(enabled = true),
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
            syncStatus = SyncStatus.SYNCED,
            version = 1
        )
    }
}