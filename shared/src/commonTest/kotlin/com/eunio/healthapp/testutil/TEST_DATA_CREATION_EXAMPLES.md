# Test Data Creation Examples

## Overview

This document provides comprehensive examples of proper test data creation using the TestDataBuilder infrastructure. Consistent test data creation is crucial for maintainable and reliable tests.

## Basic Test Data Creation

### User Data

```kotlin
// Basic user creation
val basicUser = TestDataBuilder.createUser()
// Result: User with default values and unique ID

// User with specific properties
val specificUser = TestDataBuilder.createUser(
    id = "user-123",
    email = "test@example.com",
    profile = TestDataBuilder.createUserProfile(
        name = "Test User",
        dateOfBirth = LocalDate(1990, 1, 1)
    )
)

// Admin user
val adminUser = TestDataBuilder.createAdminUser(
    permissions = setOf(
        Permission.USER_MANAGEMENT,
        Permission.SYSTEM_CONFIGURATION
    )
)

// User with specific health profile
val healthUser = TestDataBuilder.createUser(
    healthProfile = TestDataBuilder.createHealthProfile(
        conditions = listOf(HealthCondition.DIABETES),
        medications = listOf("Metformin", "Insulin")
    )
)
```

### Preferences Data

```kotlin
// Basic user preferences
val basicPreferences = TestDataBuilder.createUserPreferences()

// Display preferences with specific theme
val displayPrefs = TestDataBuilder.createDisplayPreferences(
    theme = ThemeType.DARK,
    language = LanguageCode.EN,
    dateFormat = DateFormat.ISO,
    timeFormat = TimeFormat.TWENTY_FOUR_HOUR
)

// Notification preferences
val notificationPrefs = TestDataBuilder.createNotificationPreferences(
    dailyReminders = true,
    cycleUpdates = true,
    insightAlerts = false,
    quietHours = TestDataBuilder.createQuietHours(
        startTime = LocalTime(22, 0),
        endTime = LocalTime(7, 0)
    )
)

// Privacy preferences
val privacyPrefs = TestDataBuilder.createPrivacyPreferences(
    dataSharing = DataSharingLevel.ANONYMOUS,
    analyticsEnabled = false,
    crashReportingEnabled = true
)

// Complete user preferences
val completePreferences = TestDataBuilder.createUserPreferences(
    userId = "user-123",
    displayPreferences = displayPrefs,
    notificationPreferences = notificationPrefs,
    privacyPreferences = privacyPrefs
)
```

### Health Data

```kotlin
// Basic health data entry
val basicHealthData = TestDataBuilder.createHealthData()

// Menstrual cycle data
val cycleData = TestDataBuilder.createMenstrualCycleData(
    startDate = LocalDate(2024, 1, 1),
    flow = FlowIntensity.MEDIUM,
    symptoms = listOf(
        Symptom.CRAMPS,
        Symptom.MOOD_SWINGS,
        Symptom.FATIGUE
    )
)

// Mood tracking data
val moodData = TestDataBuilder.createMoodData(
    date = LocalDate.now(),
    mood = MoodType.HAPPY,
    intensity = IntensityLevel.MODERATE,
    notes = "Feeling good today"
)

// Symptom tracking data
val symptomData = TestDataBuilder.createSymptomData(
    date = LocalDate.now(),
    symptoms = mapOf(
        SymptomType.HEADACHE to IntensityLevel.MILD,
        SymptomType.NAUSEA to IntensityLevel.SEVERE,
        SymptomType.FATIGUE to IntensityLevel.MODERATE
    )
)

// Fertility tracking data
val fertilityData = TestDataBuilder.createFertilityData(
    date = LocalDate.now(),
    basalBodyTemperature = 98.6,
    cervicalMucus = CervicalMucusType.EGG_WHITE,
    ovulationTest = OvulationTestResult.POSITIVE
)
```

## Advanced Test Data Patterns

### Data Collections

```kotlin
// Multiple users for testing user management
val userCollection = TestDataBuilder.createUserCollection(
    count = 10,
    userType = UserType.REGULAR
)

// Users with different roles
val mixedUsers = TestDataBuilder.createMixedUserCollection(
    regularUsers = 5,
    adminUsers = 2,
    moderatorUsers = 1
)

// Health data time series
val healthTimeSeries = TestDataBuilder.createHealthDataTimeSeries(
    startDate = LocalDate(2024, 1, 1),
    endDate = LocalDate(2024, 3, 31),
    dataTypes = setOf(
        HealthDataType.MENSTRUAL_CYCLE,
        HealthDataType.MOOD,
        HealthDataType.SYMPTOMS
    )
)

// Cycle history for analysis
val cycleHistory = TestDataBuilder.createCycleHistory(
    userId = "user-123",
    cycleCount = 6,
    averageCycleLength = 28,
    lengthVariation = 2 // ±2 days variation
)
```

### Realistic Data Scenarios

```kotlin
// Typical user journey data
val userJourneyData = TestDataBuilder.createUserJourneyData(
    scenario = UserJourneyScenario.NEW_USER_ONBOARDING,
    duration = 30.days
)

// Irregular cycle pattern
val irregularCycleData = TestDataBuilder.createIrregularCycleData(
    userId = "user-123",
    irregularityPattern = IrregularityPattern.VARIABLE_LENGTH,
    cycleCount = 12
)

// Pregnancy tracking data
val pregnancyData = TestDataBuilder.createPregnancyTrackingData(
    userId = "user-123",
    conceptionDate = LocalDate(2024, 1, 15),
    currentWeek = 12
)

// Menopause transition data
val menopauseData = TestDataBuilder.createMenopauseTransitionData(
    userId = "user-123",
    transitionStage = MenopauseStage.PERIMENOPAUSE,
    symptomSeverity = SeverityLevel.MODERATE
)
```

### Test Data with Relationships

```kotlin
// User with complete health profile
val userWithHealthData = TestDataBuilder.createUserWithHealthData(
    userId = "user-123",
    healthDataRange = DateRange(
        start = LocalDate.now().minus(90.days),
        end = LocalDate.now()
    ),
    includeAllDataTypes = true
)

// Family/partner relationships
val familyData = TestDataBuilder.createFamilyData(
    primaryUser = "user-123",
    partners = listOf("partner-456"),
    sharedData = setOf(
        SharedDataType.CYCLE_PREDICTIONS,
        SharedDataType.FERTILITY_WINDOW
    )
)

// Healthcare provider relationships
val healthcareData = TestDataBuilder.createHealthcareProviderData(
    userId = "user-123",
    providers = listOf(
        TestDataBuilder.createHealthcareProvider(
            type = ProviderType.GYNECOLOGIST,
            name = "Dr. Smith"
        ),
        TestDataBuilder.createHealthcareProvider(
            type = ProviderType.PRIMARY_CARE,
            name = "Dr. Johnson"
        )
    )
)
```

## Platform-Specific Test Data

### Android-Specific Data

```kotlin
// Android notification data
val androidNotificationData = TestDataBuilder.createAndroidNotificationData(
    channelId = "cycle_reminders",
    importance = NotificationImportance.HIGH,
    actions = listOf(
        NotificationAction.LOG_PERIOD,
        NotificationAction.DISMISS
    )
)

// Android widget data
val widgetData = TestDataBuilder.createAndroidWidgetData(
    widgetType = WidgetType.CYCLE_OVERVIEW,
    size = WidgetSize.MEDIUM,
    updateFrequency = UpdateFrequency.DAILY
)

// Android backup data
val backupData = TestDataBuilder.createAndroidBackupData(
    userId = "user-123",
    backupType = BackupType.FULL,
    encryptionEnabled = true
)
```

### iOS-Specific Data

```kotlin
// iOS HealthKit data
val healthKitData = TestDataBuilder.createHealthKitData(
    dataTypes = setOf(
        HealthKitDataType.MENSTRUAL_FLOW,
        HealthKitDataType.BASAL_BODY_TEMPERATURE,
        HealthKitDataType.HEART_RATE
    ),
    permissionStatus = HealthKitPermissionStatus.AUTHORIZED
)

// iOS Shortcuts data
val shortcutsData = TestDataBuilder.createIOSShortcutsData(
    shortcuts = listOf(
        Shortcut("Log Period", ShortcutType.QUICK_LOG),
        Shortcut("Check Fertility", ShortcutType.PREDICTION)
    )
)

// iOS CloudKit sync data
val cloudKitData = TestDataBuilder.createCloudKitSyncData(
    userId = "user-123",
    syncStatus = CloudKitSyncStatus.UP_TO_DATE,
    lastSyncDate = Instant.now().minus(1.hours)
)
```

## Error and Edge Case Data

### Invalid Data Scenarios

```kotlin
// User with missing required fields (for validation testing)
val invalidUser = TestDataBuilder.createInvalidUser(
    missingFields = setOf(
        UserField.EMAIL,
        UserField.DATE_OF_BIRTH
    )
)

// Corrupted health data (for error handling testing)
val corruptedHealthData = TestDataBuilder.createCorruptedHealthData(
    corruptionType = DataCorruptionType.INVALID_DATES,
    severity = CorruptionSeverity.RECOVERABLE
)

// Conflicting preferences (for conflict resolution testing)
val conflictingPreferences = TestDataBuilder.createConflictingPreferences(
    localPreferences = TestDataBuilder.createUserPreferences(theme = ThemeType.DARK),
    remotePreferences = TestDataBuilder.createUserPreferences(theme = ThemeType.LIGHT),
    conflictTimestamp = Instant.now()
)
```

### Boundary Value Data

```kotlin
// Minimum valid cycle data
val minimumCycleData = TestDataBuilder.createMinimumCycleData(
    cycleLength = 21, // Minimum valid cycle length
    flowDays = 1      // Minimum flow duration
)

// Maximum valid cycle data
val maximumCycleData = TestDataBuilder.createMaximumCycleData(
    cycleLength = 35, // Maximum typical cycle length
    flowDays = 7      // Maximum typical flow duration
)

// Edge case dates
val edgeCaseDates = TestDataBuilder.createEdgeCaseDateData(
    scenarios = setOf(
        DateScenario.LEAP_YEAR,
        DateScenario.YEAR_BOUNDARY,
        DateScenario.DAYLIGHT_SAVING_TRANSITION
    )
)
```

### Performance Test Data

```kotlin
// Large dataset for performance testing
val largeDataset = TestDataBuilder.createLargeHealthDataset(
    userCount = 1000,
    dataPointsPerUser = 365 * 2, // 2 years of daily data
    dataTypes = HealthDataType.values().toSet()
)

// Memory-intensive data
val memoryIntensiveData = TestDataBuilder.createMemoryIntensiveData(
    dataSize = DataSize.LARGE,
    complexity = DataComplexity.HIGH,
    relationships = RelationshipComplexity.DEEP
)

// Concurrent access data
val concurrentAccessData = TestDataBuilder.createConcurrentAccessData(
    userCount = 50,
    simultaneousOperations = 100,
    operationTypes = setOf(
        OperationType.READ,
        OperationType.WRITE,
        OperationType.UPDATE
    )
)
```

## Test Data Builders Usage Patterns

### Builder Pattern Usage

```kotlin
// Using builder pattern for complex data
val complexUser = TestDataBuilder.userBuilder()
    .withId("user-123")
    .withEmail("test@example.com")
    .withProfile(
        TestDataBuilder.profileBuilder()
            .withName("Test User")
            .withAge(25)
            .withHealthConditions(listOf(HealthCondition.PCOS))
            .build()
    )
    .withPreferences(
        TestDataBuilder.preferencesBuilder()
            .withTheme(ThemeType.DARK)
            .withNotifications(enabled = true)
            .withPrivacy(DataSharingLevel.ANONYMOUS)
            .build()
    )
    .build()

// Chaining multiple builders
val userWithData = TestDataBuilder.userBuilder()
    .withBasicInfo("user-123", "test@example.com")
    .withHealthData(
        TestDataBuilder.healthDataBuilder()
            .withCycleHistory(months = 6)
            .withMoodTracking(enabled = true)
            .withSymptomTracking(enabled = true)
            .build()
    )
    .build()
```

### Factory Method Patterns

```kotlin
// Quick factory methods for common scenarios
val newUser = TestDataBuilder.newUser()
val existingUser = TestDataBuilder.existingUser()
val premiumUser = TestDataBuilder.premiumUser()

// Scenario-based factories
val onboardingUser = TestDataBuilder.onboardingUser(step = OnboardingStep.HEALTH_PROFILE)
val activeUser = TestDataBuilder.activeUser(lastActivity = 1.days.ago)
val inactiveUser = TestDataBuilder.inactiveUser(lastActivity = 30.days.ago)

// Data type factories
val cycleData = TestDataBuilder.typicalCycleData()
val irregularCycleData = TestDataBuilder.irregularCycleData()
val pregnancyData = TestDataBuilder.pregnancyData()
```

### Template-Based Creation

```kotlin
// Using templates for consistent data
val userTemplate = TestDataBuilder.createUserTemplate(
    baseProfile = UserProfile.STANDARD,
    healthConditions = listOf(HealthCondition.ENDOMETRIOSIS),
    preferences = PreferenceProfile.PRIVACY_FOCUSED
)

// Create multiple users from template
val users = (1..10).map { index ->
    TestDataBuilder.createUserFromTemplate(
        template = userTemplate,
        uniqueId = "user-$index"
    )
}

// Template variations
val variations = TestDataBuilder.createTemplateVariations(
    baseTemplate = userTemplate,
    variations = mapOf(
        "age" to listOf(18, 25, 35, 45),
        "cycleLength" to listOf(24, 28, 32, 35)
    )
)
```

## Data Validation and Consistency

### Validation Helpers

```kotlin
// Validate created data
val user = TestDataBuilder.createUser()
TestDataBuilder.validateUser(user) // Throws if invalid

// Validate data relationships
val userWithData = TestDataBuilder.createUserWithHealthData()
TestDataBuilder.validateDataConsistency(userWithData) // Checks relationships

// Validate data constraints
val cycleData = TestDataBuilder.createCycleHistory()
TestDataBuilder.validateCycleConstraints(cycleData) // Checks medical validity
```

### Consistency Checks

```kotlin
// Ensure data consistency across related objects
val userId = "user-123"
val user = TestDataBuilder.createUser(id = userId)
val preferences = TestDataBuilder.createUserPreferences(userId = userId)
val healthData = TestDataBuilder.createHealthData(userId = userId)

// Verify all data references the same user
TestDataBuilder.verifyDataConsistency(user, preferences, healthData)

// Check temporal consistency
val timeSeriesData = TestDataBuilder.createHealthDataTimeSeries(
    startDate = LocalDate(2024, 1, 1),
    endDate = LocalDate(2024, 3, 31)
)
TestDataBuilder.verifyTemporalConsistency(timeSeriesData)
```

## Best Practices for Test Data Creation

### 1. Use Descriptive Names

```kotlin
// ✅ Good: Descriptive variable names
val userWithIrregularCycles = TestDataBuilder.createUser(
    healthProfile = TestDataBuilder.createHealthProfile(
        cyclePattern = CyclePattern.IRREGULAR
    )
)

// ❌ Bad: Generic names
val user1 = TestDataBuilder.createUser()
val data = TestDataBuilder.createHealthData()
```

### 2. Create Data Close to Usage

```kotlin
// ✅ Good: Create data in test method
@Test
fun `test handles irregular cycles`() {
    val userWithIrregularCycles = TestDataBuilder.createUser(
        cyclePattern = CyclePattern.IRREGULAR
    )
    
    val result = cycleAnalyzer.analyzeCycle(userWithIrregularCycles)
    // Test logic
}

// ❌ Bad: Shared test data
class CycleAnalyzerTest {
    private val testUser = TestDataBuilder.createUser() // Shared across tests
}
```

### 3. Use Realistic Data

```kotlin
// ✅ Good: Realistic data
val realisticCycleData = TestDataBuilder.createCycleHistory(
    averageCycleLength = 28,
    lengthVariation = 3, // Realistic variation
    flowDuration = 5
)

// ❌ Bad: Unrealistic data
val unrealisticCycleData = TestDataBuilder.createCycleHistory(
    averageCycleLength = 100, // Medically impossible
    flowDuration = 20
)
```

### 4. Isolate Test Data

```kotlin
// ✅ Good: Unique data per test
@Test
fun `test user creation`() {
    val uniqueUserId = TestDataBuilder.generateUniqueUserId()
    val user = TestDataBuilder.createUser(id = uniqueUserId)
    // Test logic
}

// ❌ Bad: Reused IDs
@Test
fun `test user creation`() {
    val user = TestDataBuilder.createUser(id = "test-user") // Same ID in all tests
}
```

### 5. Document Complex Data

```kotlin
// ✅ Good: Document complex test scenarios
@Test
fun `handles user with PCOS and irregular cycles`() {
    // Create user with PCOS condition which typically causes irregular cycles
    val userWithPCOS = TestDataBuilder.createUser(
        healthConditions = listOf(HealthCondition.PCOS),
        cyclePattern = CyclePattern.IRREGULAR_LONG // 35-45 day cycles
    )
    
    val analysis = cycleAnalyzer.analyze(userWithPCOS)
    // Verify PCOS-specific analysis logic
}
```

## Common Anti-Patterns to Avoid

### ❌ Don't Do This

```kotlin
// Hard-coded test data
val user = User(
    id = "123",
    email = "test@test.com",
    profile = UserProfile(...)
)

// Shared mutable test data
class MyTest {
    companion object {
        val sharedUser = TestDataBuilder.createUser()
    }
}

// Overly complex test data
val overlyComplexUser = TestDataBuilder.createUser(
    // 50+ parameters with complex nested objects
)

// Unrealistic test data
val impossibleCycleData = CycleData(
    cycleLength = -5, // Negative cycle length
    flowIntensity = FlowIntensity.IMPOSSIBLE
)
```

### ✅ Do This Instead

```kotlin
// Use TestDataBuilder
val user = TestDataBuilder.createUser(
    email = "test@example.com"
)

// Create fresh data per test
@Test
fun `my test`() {
    val user = TestDataBuilder.createUser()
    // Test logic
}

// Use appropriate complexity
val simpleUser = TestDataBuilder.createUser() // For basic tests
val complexUser = TestDataBuilder.createUserWithHealthData() // When needed

// Use realistic data
val realisticCycleData = TestDataBuilder.createTypicalCycleData()
```

## Integration with Test Infrastructure

### Using with Mock Services

```kotlin
@Test
fun `test with mock data and services`() {
    // Create test data
    val user = TestDataBuilder.createUser()
    val preferences = TestDataBuilder.createUserPreferences(userId = user.id)
    
    // Configure mock services
    val mockRepository = get<UserRepository>() as MockUserRepository
    mockRepository.userData = user
    mockRepository.userPreferences = preferences
    
    // Test service behavior
    val service = get<UserService>()
    val result = service.getUserWithPreferences(user.id)
    
    assertEquals(user, result.user)
    assertEquals(preferences, result.preferences)
}
```

### Performance Considerations

```kotlin
@Test
fun `performance test with large dataset`() {
    // Use performance-optimized data creation
    val largeDataset = TestDataBuilder.createLargeDatasetOptimized(
        size = 10000,
        useCache = true, // Reuse common objects
        lazyLoading = true // Create objects on demand
    )
    
    val startTime = TimeSource.Monotonic.markNow()
    
    val result = dataProcessor.processLargeDataset(largeDataset)
    
    val duration = startTime.elapsedNow()
    assertTrue(duration < 5.seconds, "Processing took too long: $duration")
}
```

This comprehensive guide should help you create consistent, realistic, and maintainable test data for all your testing needs.