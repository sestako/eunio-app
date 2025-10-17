# Data Flow & Synchronization Remediation Plan

**Goal:** Increase Overall Functionality from 15% → 100%  
**Current Status:** Excellent architecture, minimal implementation  
**Timeline:** 4-6 weeks (20-30 working days)  
**Last Updated:** January 9, 2025

---

## 📊 Current State Summary

| Component | Design | Implementation | Functional | Target |
|-----------|--------|----------------|------------|--------|
| ViewModels | ✅ 10/10 | ✅ Complete | ❌ 0% | 100% |
| Use Cases | ✅ 10/10 | ✅ Complete | ❌ 0% | 100% |
| Repositories | ✅ 10/10 | ✅ Complete | ❌ 0% | 100% |
| Local Storage | ✅ 10/10 | ⚠️ Schema only | ❌ 0% | 100% |
| Remote Services | ✅ 10/10 | ❌ Missing | ❌ 0% | 100% |
| Offline Sync | ✅ 10/10 | ✅ Complete | ❌ 0% | 100% |
| DI System | ✅ 10/10 | ❌ Not init | ❌ 0% | 100% |
| **OVERALL** | **✅ 10/10** | **⚠️ 60%** | **❌ 15%** | **100%** |

---

## 🎯 Remediation Phases

### Phase 1: Foundation (Week 1) - Critical Blockers
**Goal:** Unblock dependency injection and enable basic data flow  
**Effort:** 5 days  
**Functionality Gain:** 15% → 35%

### Phase 2: Local Storage (Week 2) - Data Persistence
**Goal:** Implement local database and enable offline data storage  
**Effort:** 5 days  
**Functionality Gain:** 35% → 60%

### Phase 3: Remote Services (Week 3) - Cloud Integration
**Goal:** Implement Firebase services and enable cloud sync  
**Effort:** 5 days  
**Functionality Gain:** 60% → 85%

### Phase 4: Optimization (Week 4) - Production Ready
**Goal:** Fix platform bridges, add conflict resolution, optimize performance  
**Effort:** 5 days  
**Functionality Gain:** 85% → 100%

---


# PHASE 1: FOUNDATION (Week 1)

**Timeline:** Days 1-5  
**Functionality:** 15% → 35% (+20%)  
**Priority:** 🔴 CRITICAL - Unblocks everything

---

## Day 1: Initialize Dependency Injection (iOS)

### 🎯 Objective
Enable Koin dependency injection in iOS app to allow ViewModel instantiation.

### 📋 Current State
- Koin modules defined in shared module
- iOS app entry point has DI commented out or missing
- ViewModels cannot be instantiated

### ✅ Tasks

#### Task 1.1: Verify Koin Configuration in Shared Module
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt`

```bash
# Check if Koin modules are properly defined
cat shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt
```

**Expected:** Should see module definitions for ViewModels, UseCases, Repositories

**Action if missing:** Create Koin modules (see template below)

#### Task 1.2: Initialize Koin in iOS App Entry Point
**File:** `iosApp/iosApp/iOSApp.swift`

**Current (broken):**
```swift
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

**Fix to apply:**
```swift
import shared

@main
struct iOSApp: App {
    
    init() {
        // Initialize Koin
        KoinInitializerKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

#### Task 1.3: Create Koin Initializer Helper
**File:** `shared/src/iosMain/kotlin/com/eunio/healthapp/di/KoinInitializer.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}
```

#### Task 1.4: Test ViewModel Instantiation
**File:** `iosApp/iosApp/ViewModels/ModernCalendarViewModel.swift`

**Add test code:**
```swift
// In init() method, add logging
init(calendarService: CalendarViewModelService, ...) {
    print("✅ CalendarViewModel instantiated successfully")
    self.sharedViewModel = calendarService.getViewModel()
    print("✅ Shared ViewModel accessed: \(sharedViewModel)")
}
```

**Run app and check console for success messages**

### 🧪 Verification Steps

1. Build iOS app: `cd iosApp && xcodebuild`
2. Check for Koin initialization logs
3. Verify no crash on ViewModel access
4. Check console for "✅ CalendarViewModel instantiated successfully"

### 📊 Success Criteria
- [ ] iOS app builds without errors
- [ ] Koin initializes on app launch
- [ ] ViewModels can be instantiated
- [ ] No crashes when accessing shared ViewModels

### 🚨 Troubleshooting

**Issue:** "Koin not started"
- **Solution:** Ensure `initKoin()` is called before any ViewModel access

**Issue:** "Module not found"
- **Solution:** Check that `appModule` is properly exported from shared module

**Issue:** Build errors
- **Solution:** Clean build: `cd iosApp && xcodebuild clean`

---

## Day 2: Initialize Dependency Injection (Android)

### 🎯 Objective
Enable Koin dependency injection in Android app.

### 📋 Current State
- Android app likely has similar DI issues
- Need to verify and fix Android initialization

### ✅ Tasks

#### Task 2.1: Check Android Application Class
**File:** `androidApp/src/main/java/com/eunio/healthapp/android/HealthApp.kt`

**Expected structure:**
```kotlin
class HealthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@HealthApp)
            modules(appModule)
        }
    }
}
```

#### Task 2.2: Verify AndroidManifest.xml
**File:** `androidApp/src/main/AndroidManifest.xml`

**Ensure application class is registered:**
```xml
<application
    android:name=".HealthApp"
    ...>
```

#### Task 2.3: Test Android ViewModel Access
**File:** `androidApp/src/main/java/com/eunio/healthapp/android/ui/calendar/CalendarScreen.kt`

**Add test code:**
```kotlin
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    Log.d("CalendarScreen", "✅ ViewModel injected: $viewModel")
    // ... rest of code
}
```

### 🧪 Verification Steps

1. Build Android app: `cd androidApp && ./gradlew assembleDebug`
2. Install on emulator/device
3. Check logcat for Koin initialization
4. Verify ViewModels are injected

### 📊 Success Criteria
- [ ] Android app builds without errors
- [ ] Koin initializes on app launch
- [ ] ViewModels are injected via koinViewModel()
- [ ] No crashes when navigating screens

---

## Day 3: Implement Mock Data Services

### 🎯 Objective
Create temporary mock implementations to enable testing while real services are built.

### 📋 Current State
- Repositories have no data sources
- Need temporary implementations to test data flow

### ✅ Tasks

#### Task 3.1: Create Mock Local Database Service
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/local/MockDatabaseService.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.local

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

class MockDatabaseService {
    private val logs = mutableMapOf<String, DailyLog>()
    private val cycles = mutableMapOf<String, Cycle>()
    
    suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        logs[log.id] = log
        return Result.success(Unit)
    }
    
    suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        val log = logs.values.find { it.userId == userId && it.date == date }
        return Result.success(log)
    }
    
    suspend fun getLogHistory(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        val filtered = logs.values.filter {
            it.userId == userId && it.date >= startDate && it.date <= endDate
        }
        return Result.success(filtered)
    }
    
    suspend fun saveCycle(cycle: Cycle): Result<Unit> {
        cycles[cycle.id] = cycle
        return Result.success(Unit)
    }
    
    suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        val cycle = cycles.values.find { it.userId == userId }
        return Result.success(cycle)
    }
}
```

#### Task 3.2: Create Mock Firebase Service
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/MockFirebaseService.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.delay

class MockFirebaseService {
    private val remoteData = mutableMapOf<String, Any>()
    
    suspend fun syncData(userId: String, data: Any): Result<Unit> {
        // Simulate network delay
        delay(500)
        remoteData["${userId}_${data::class.simpleName}"] = data
        return Result.success(Unit)
    }
    
    suspend fun fetchData(userId: String, dataType: String): Result<Any?> {
        delay(300)
        return Result.success(remoteData["${userId}_${dataType}"])
    }
}
```

#### Task 3.3: Update Repository Implementations
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`

**Update to use mock service:**
```kotlin
class LogRepositoryImpl(
    private val localDatabase: MockDatabaseService,
    private val remoteService: MockFirebaseService
) : LogRepository {
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        // Save locally
        val localResult = localDatabase.saveDailyLog(log)
        if (localResult.isError) return localResult
        
        // Sync to remote (fire and forget for now)
        remoteService.syncData(log.userId, log)
        
        return Result.success(Unit)
    }
    
    override suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        return localDatabase.getDailyLog(userId, date)
    }
    
    // ... implement other methods
}
```

#### Task 3.4: Update Koin Modules
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt`

**Add mock services to DI:**
```kotlin
val appModule = module {
    // Mock services (temporary)
    single { MockDatabaseService() }
    single { MockFirebaseService() }
    
    // Repositories
    single<LogRepository> { 
        LogRepositoryImpl(
            localDatabase = get(),
            remoteService = get()
        ) 
    }
    
    // ... rest of modules
}
```

### 🧪 Verification Steps

1. Build project: `./gradlew build`
2. Run iOS app and try to save a daily log
3. Check that data persists in memory
4. Navigate away and back - data should still be there (until app restart)

### 📊 Success Criteria
- [ ] Mock services compile without errors
- [ ] Repositories can save and retrieve data
- [ ] Data persists in memory during app session
- [ ] No crashes when performing CRUD operations

---

## Day 4: Test End-to-End Data Flow

### 🎯 Objective
Verify that data flows from UI → ViewModel → UseCase → Repository → Mock Service.

### ✅ Tasks

#### Task 4.1: Add Logging to Track Data Flow
**Add to each layer:**

**ViewModel:**
```kotlin
fun saveLog() {
    println("📱 [ViewModel] Saving log for date: ${uiState.value.selectedDate}")
    viewModelScope.launch {
        val result = saveDailyLogUseCase(dailyLog)
        println("📱 [ViewModel] Save result: ${result.isSuccess}")
    }
}
```

**UseCase:**
```kotlin
override suspend fun invoke(log: DailyLog): Result<Unit> {
    println("🔧 [UseCase] Processing log: ${log.id}")
    val result = logRepository.saveDailyLog(log)
    println("🔧 [UseCase] Repository result: ${result.isSuccess}")
    return result
}
```

**Repository:**
```kotlin
override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
    println("💾 [Repository] Saving to local database")
    val result = localDatabase.saveDailyLog(log)
    println("💾 [Repository] Local save: ${result.isSuccess}")
    return result
}
```

**Mock Service:**
```kotlin
suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
    println("🗄️  [MockDB] Storing log: ${log.id}")
    logs[log.id] = log
    println("🗄️  [MockDB] Total logs: ${logs.size}")
    return Result.success(Unit)
}
```

#### Task 4.2: Create Test Scenario
**Test in iOS app:**

1. Launch app
2. Navigate to Daily Logging screen
3. Select today's date
4. Add period flow: MEDIUM
5. Add symptom: CRAMPS
6. Add mood: HAPPY
7. Tap Save
8. Check console logs

**Expected console output:**
```
📱 [ViewModel] Saving log for date: 2025-01-09
🔧 [UseCase] Processing log: log_2025-01-09_1234567890
💾 [Repository] Saving to local database
🗄️  [MockDB] Storing log: log_2025-01-09_1234567890
🗄️  [MockDB] Total logs: 1
💾 [Repository] Local save: true
🔧 [UseCase] Repository result: true
📱 [ViewModel] Save result: true
```

#### Task 4.3: Test Data Retrieval
**Test flow:**

1. Save a log (as above)
2. Navigate to Calendar screen
3. Check that the date shows a log indicator
4. Tap on the date
5. Verify log data is displayed

**Check console for:**
```
📱 [ViewModel] Loading logs for month: 2025-01
🔧 [UseCase] Fetching log history
💾 [Repository] Querying local database
🗄️  [MockDB] Found 1 logs
```

### 🧪 Verification Steps

1. Complete save flow test
2. Complete retrieval flow test
3. Test navigation between screens
4. Verify data persists during session

### 📊 Success Criteria
- [ ] Console shows complete data flow chain
- [ ] Data saves successfully
- [ ] Data retrieves successfully
- [ ] UI updates with saved data
- [ ] No errors in console

---

## Day 5: Phase 1 Validation & Documentation

### 🎯 Objective
Validate Phase 1 completion and document progress.

### ✅ Tasks

#### Task 5.1: Run Comprehensive Tests

**Test Checklist:**
- [ ] iOS app launches without crashes
- [ ] Android app launches without crashes
- [ ] Daily Logging: Save data
- [ ] Daily Logging: Load data
- [ ] Calendar: Display logs
- [ ] Calendar: Navigate months
- [ ] Insights: Load insights (even if empty)
- [ ] Settings: Load settings
- [ ] Data persists during app session
- [ ] No memory leaks (check Instruments/Profiler)

#### Task 5.2: Measure Functionality Improvement

**Before Phase 1:** 15%
**After Phase 1:** Target 35%

**Calculate actual:**
```
Components now functional:
- DI System: 100% ✅
- ViewModels: 100% ✅
- Use Cases: 100% ✅
- Repositories: 100% ✅
- Mock Services: 100% ✅
- Data Flow: 100% ✅

Still missing:
- Real Local Storage: 0% ❌
- Real Remote Services: 0% ❌
- Offline Sync: 0% ❌
- Conflict Resolution: 0% ❌

Actual functionality: ~35%
```

#### Task 5.3: Document Phase 1 Completion

**Create file:** `remediation-plans/phase-1-completion-report.md`

**Include:**
- What was completed
- What works now
- Known limitations
- Next steps

#### Task 5.4: Commit Changes

```bash
git add .
git commit -m "Phase 1: Initialize DI and enable basic data flow

- Initialize Koin in iOS and Android apps
- Create mock database and Firebase services
- Enable end-to-end data flow
- Add comprehensive logging
- Functionality: 15% → 35%"
```

### 📊 Phase 1 Success Criteria
- [ ] Functionality increased from 15% to 35%
- [ ] All DI issues resolved
- [ ] Data flows end-to-end (with mocks)
- [ ] No crashes in basic user flows
- [ ] Documentation updated
- [ ] Changes committed to git

---

## 🎉 Phase 1 Complete!

**Achievements:**
- ✅ Dependency injection working
- ✅ ViewModels instantiating correctly
- ✅ Data flowing through all layers
- ✅ Basic CRUD operations working (in-memory)
- ✅ Foundation ready for real implementations

**Next:** Phase 2 - Implement real local storage with SQLDelight



---

# PHASE 2: LOCAL STORAGE (Week 2)

**Timeline:** Days 6-10  
**Functionality:** 35% → 60% (+25%)  
**Priority:** 🟠 HIGH - Enable data persistence

---

## Day 6: Initialize SQLDelight Database

### 🎯 Objective
Set up SQLDelight database driver for iOS and Android platforms.

### 📋 Current State
- SQLDelight schema defined
- Database driver not initialized
- No actual data persistence

### ✅ Tasks

#### Task 6.1: Create iOS Database Driver
**File:** `shared/src/iosMain/kotlin/com/eunio/healthapp/data/local/DatabaseDriverFactory.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.eunio.healthapp.database.HealthAppDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = HealthAppDatabase.Schema,
            name = "healthapp.db"
        )
    }
}
```

#### Task 6.2: Create Android Database Driver
**File:** `shared/src/androidMain/kotlin/com/eunio/healthapp/data/local/DatabaseDriverFactory.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.eunio.healthapp.database.HealthAppDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = HealthAppDatabase.Schema,
            context = context,
            name = "healthapp.db"
        )
    }
}
```

#### Task 6.3: Create Common Database Factory Interface
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/local/DatabaseDriverFactory.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

#### Task 6.4: Initialize Database in DI
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt`

**Update module:**
```kotlin
val appModule = module {
    // Database
    single { 
        val driver = get<DatabaseDriverFactory>().createDriver()
        HealthAppDatabase(driver)
    }
    
    // Database queries
    single { get<HealthAppDatabase>().dailyLogQueries }
    single { get<HealthAppDatabase>().cycleQueries }
    single { get<HealthAppDatabase>().userQueries }
    
    // Replace mock with real database service
    single { 
        DatabaseService(
            dailyLogQueries = get(),
            cycleQueries = get(),
            userQueries = get()
        )
    }
    
    // ... rest of modules
}
```

#### Task 6.5: Platform-Specific DI Setup

**iOS:** `shared/src/iosMain/kotlin/com/eunio/healthapp/di/KoinInitializer.kt`
```kotlin
fun initKoin() {
    startKoin {
        modules(
            module {
                single { DatabaseDriverFactory() }
            },
            appModule
        )
    }
}
```

**Android:** Update `HealthApp.kt`
```kotlin
class HealthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@HealthApp)
            modules(
                module {
                    single { DatabaseDriverFactory(androidContext()) }
                },
                appModule
            )
        }
    }
}
```

### 🧪 Verification Steps

1. Build project: `./gradlew build`
2. Check for SQLDelight code generation
3. Verify database file is created on device
4. Check no initialization errors

### 📊 Success Criteria
- [ ] Database driver initializes on iOS
- [ ] Database driver initializes on Android
- [ ] Database file created in app storage
- [ ] No crashes on database access

---

## Day 7: Implement Database Service Layer

### 🎯 Objective
Create service layer that wraps SQLDelight queries with proper error handling.

### ✅ Tasks

#### Task 7.1: Create Database Service
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/local/DatabaseService.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.local

import com.eunio.healthapp.database.*
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant

class DatabaseService(
    private val dailyLogQueries: DailyLogQueries,
    private val cycleQueries: CycleQueries,
    private val userQueries: UserQueries
) {
    
    // Daily Log Operations
    suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        return try {
            dailyLogQueries.insertOrReplaceDailyLog(
                id = log.id,
                userId = log.userId,
                date = log.date.toString(),
                periodFlow = log.periodFlow?.name,
                symptoms = log.symptoms.joinToString(",") { it.name },
                mood = log.mood?.name,
                sexualActivity = log.sexualActivity?.name,
                bbt = log.bbt,
                cervicalMucus = log.cervicalMucus?.name,
                opkResult = log.opkResult?.name,
                notes = log.notes,
                createdAt = log.createdAt.toString(),
                updatedAt = log.updatedAt.toString()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError(
                message = "Failed to save daily log: ${e.message}",
                cause = e
            ))
        }
    }
    
    suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?> {
        return try {
            val dbLog = dailyLogQueries.getDailyLogByDate(
                userId = userId,
                date = date.toString()
            ).executeAsOneOrNull()
            
            val log = dbLog?.toDomainModel()
            Result.success(log)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError(
                message = "Failed to get daily log: ${e.message}",
                cause = e
            ))
        }
    }
    
    suspend fun getLogHistory(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return try {
            val dbLogs = dailyLogQueries.getLogHistory(
                userId = userId,
                startDate = startDate.toString(),
                endDate = endDate.toString()
            ).executeAsList()
            
            val logs = dbLogs.map { it.toDomainModel() }
            Result.success(logs)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError(
                message = "Failed to get log history: ${e.message}",
                cause = e
            ))
        }
    }
    
    // Cycle Operations
    suspend fun saveCycle(cycle: Cycle): Result<Unit> {
        return try {
            cycleQueries.insertOrReplaceCycle(
                id = cycle.id,
                userId = cycle.userId,
                startDate = cycle.startDate.toString(),
                endDate = cycle.endDate?.toString(),
                cycleLength = cycle.cycleLength?.toLong(),
                periodLength = cycle.periodLength?.toLong(),
                confirmedOvulationDate = cycle.confirmedOvulationDate?.toString(),
                createdAt = cycle.createdAt.toString(),
                updatedAt = cycle.updatedAt.toString()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError(
                message = "Failed to save cycle: ${e.message}",
                cause = e
            ))
        }
    }
    
    suspend fun getCurrentCycle(userId: String): Result<Cycle?> {
        return try {
            val dbCycle = cycleQueries.getCurrentCycle(userId)
                .executeAsOneOrNull()
            
            val cycle = dbCycle?.toDomainModel()
            Result.success(cycle)
        } catch (e: Exception) {
            Result.error(AppError.DatabaseError(
                message = "Failed to get current cycle: ${e.message}",
                cause = e
            ))
        }
    }
}

// Extension functions to convert DB models to domain models
private fun DailyLogEntity.toDomainModel(): DailyLog {
    return DailyLog(
        id = id,
        userId = userId,
        date = LocalDate.parse(date),
        periodFlow = periodFlow?.let { PeriodFlow.valueOf(it) },
        symptoms = symptoms?.split(",")?.mapNotNull { 
            try { Symptom.valueOf(it) } catch (e: Exception) { null }
        } ?: emptyList(),
        mood = mood?.let { Mood.valueOf(it) },
        sexualActivity = sexualActivity?.let { SexualActivity.valueOf(it) },
        bbt = bbt,
        cervicalMucus = cervicalMucus?.let { CervicalMucus.valueOf(it) },
        opkResult = opkResult?.let { OPKResult.valueOf(it) },
        notes = notes,
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )
}

private fun CycleEntity.toDomainModel(): Cycle {
    return Cycle(
        id = id,
        userId = userId,
        startDate = LocalDate.parse(startDate),
        endDate = endDate?.let { LocalDate.parse(it) },
        cycleLength = cycleLength?.toInt(),
        periodLength = periodLength?.toInt(),
        confirmedOvulationDate = confirmedOvulationDate?.let { LocalDate.parse(it) },
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt)
    )
}
```

### 🧪 Verification Steps

1. Build project
2. Test save operation
3. Query database to verify data
4. Test retrieval operation

### 📊 Success Criteria
- [ ] DatabaseService compiles without errors
- [ ] Save operations work
- [ ] Retrieve operations work
- [ ] Data persists after app restart

---

## Day 8: Update Repositories to Use Real Database

### 🎯 Objective
Replace mock database service with real SQLDelight implementation.

### ✅ Tasks

#### Task 8.1: Update LogRepositoryImpl
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`

**Update implementation:**
```kotlin
class LogRepositoryImpl(
    private val databaseService: DatabaseService,
    private val remoteService: MockFirebaseService // Still mock for now
) : LogRepository {
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        // Save to local database
        val localResult = databaseService.saveDailyLog(log)
        if (localResult.isError) return localResult
        
        // Queue for remote sync (fire and forget)
        try {
            remoteService.syncData(log.userId, log)
        } catch (e: Exception) {
            // Log error but don't fail the operation
            println("⚠️  Remote sync failed: ${e.message}")
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun getDailyLog(
        userId: String,
        date: LocalDate
    ): Result<DailyLog?> {
        return databaseService.getDailyLog(userId, date)
    }
    
    override suspend fun getLogHistory(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        return databaseService.getLogHistory(userId, startDate, endDate)
    }
    
    override suspend fun deleteDailyLog(logId: String): Result<Unit> {
        return databaseService.deleteDailyLog(logId)
    }
    
    override suspend fun getPeriodLogs(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>> {
        val logsResult = databaseService.getLogHistory(userId, startDate, endDate)
        return logsResult.map { logs ->
            logs.filter { it.periodFlow != null }
        }
    }
}
```

#### Task 8.2: Update CycleRepositoryImpl
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/CycleRepositoryImpl.kt`

**Similar updates for cycle operations**

#### Task 8.3: Update DI Module
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt`

**Update repositories:**
```kotlin
val appModule = module {
    // Database (already added in Day 6)
    single { 
        val driver = get<DatabaseDriverFactory>().createDriver()
        HealthAppDatabase(driver)
    }
    single { DatabaseService(get(), get(), get()) }
    
    // Remote service (still mock)
    single { MockFirebaseService() }
    
    // Repositories with real database
    single<LogRepository> { 
        LogRepositoryImpl(
            databaseService = get(),
            remoteService = get()
        ) 
    }
    
    single<CycleRepository> {
        CycleRepositoryImpl(
            databaseService = get(),
            remoteService = get()
        )
    }
    
    // ... rest of repositories
}
```

### 🧪 Verification Steps

1. Clean and rebuild project
2. Uninstall app from device (to clear old data)
3. Install fresh build
4. Test save operation
5. Force quit app
6. Relaunch app
7. Verify data persists

### 📊 Success Criteria
- [ ] Data saves to SQLite database
- [ ] Data persists after app restart
- [ ] Data persists after device reboot
- [ ] Query performance is acceptable

---

## Day 9: Test Data Persistence End-to-End

### 🎯 Objective
Comprehensive testing of data persistence across app lifecycle.

### ✅ Tasks

#### Task 9.1: Test Daily Logging Persistence

**Test Scenario 1: Basic Save and Retrieve**
1. Launch app
2. Navigate to Daily Logging
3. Enter data:
   - Date: Today
   - Period Flow: MEDIUM
   - Symptoms: CRAMPS, BLOATING
   - Mood: HAPPY
   - Notes: "Test entry"
4. Save
5. Force quit app
6. Relaunch app
7. Navigate to Daily Logging
8. Select same date
9. Verify all data is present

**Expected:** All data persists correctly

#### Task 9.2: Test Calendar Data Display

**Test Scenario 2: Calendar Integration**
1. Create logs for multiple dates (past 7 days)
2. Navigate to Calendar
3. Verify all dates show log indicators
4. Tap on each date
5. Verify correct data displays

**Expected:** Calendar shows all logged dates

#### Task 9.3: Test Data Updates

**Test Scenario 3: Update Existing Log**
1. Open existing log
2. Modify data (change mood, add symptom)
3. Save
4. Force quit app
5. Relaunch and verify changes persisted

**Expected:** Updates save correctly

#### Task 9.4: Test Data Deletion

**Test Scenario 4: Delete Log**
1. Open existing log
2. Delete log
3. Verify log removed from calendar
4. Force quit app
5. Relaunch and verify log still deleted

**Expected:** Deletion persists

#### Task 9.5: Test Large Dataset

**Test Scenario 5: Performance with Many Logs**
1. Create script to generate 90 days of logs
2. Run script
3. Navigate calendar through 3 months
4. Measure load times
5. Check for UI lag

**Expected:** Smooth performance with 90+ logs

### 🧪 Verification Steps

**Create test data generator:**
```kotlin
// In a test file or debug menu
fun generateTestData() {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    repeat(90) { daysAgo ->
        val date = today.minus(DatePeriod(days = daysAgo))
        val log = DailyLog(
            id = "test_log_$daysAgo",
            userId = "current_user",
            date = date,
            periodFlow = if (daysAgo % 28 < 5) PeriodFlow.MEDIUM else null,
            symptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING).take((daysAgo % 3)),
            mood = Mood.values()[(daysAgo % Mood.values().size)],
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        
        // Save log
        saveDailyLogUseCase(log)
    }
}
```

### 📊 Success Criteria
- [ ] All test scenarios pass
- [ ] Data persists across app restarts
- [ ] Updates and deletes work correctly
- [ ] Performance acceptable with 90+ logs
- [ ] No data corruption
- [ ] No crashes

---

## Day 10: Phase 2 Validation & Documentation

### 🎯 Objective
Validate Phase 2 completion and measure progress.

### ✅ Tasks

#### Task 10.1: Run Full Test Suite

**Comprehensive Test Checklist:**
- [ ] Save daily log - persists after restart
- [ ] Update daily log - changes persist
- [ ] Delete daily log - deletion persists
- [ ] Load log history - correct data returned
- [ ] Calendar displays all logs correctly
- [ ] Navigate between months - no lag
- [ ] Large dataset (90+ logs) - good performance
- [ ] Database file size reasonable
- [ ] No memory leaks
- [ ] No crashes

#### Task 10.2: Measure Functionality Improvement

**Before Phase 2:** 35%
**After Phase 2:** Target 60%

**Calculate actual:**
```
Now functional:
- DI System: 100% ✅
- ViewModels: 100% ✅
- Use Cases: 100% ✅
- Repositories: 100% ✅
- Local Storage: 100% ✅
- Data Persistence: 100% ✅
- Data Flow: 100% ✅

Still missing:
- Real Remote Services: 0% ❌
- Cloud Sync: 0% ❌
- Offline Sync: 50% ⚠️ (detection works, sync doesn't)
- Conflict Resolution: 0% ❌

Actual functionality: ~60%
```

#### Task 10.3: Performance Benchmarks

**Measure and document:**
- Database initialization time: < 100ms
- Save operation time: < 50ms
- Query operation time: < 30ms
- Load 30 days of logs: < 100ms
- Calendar month render: < 200ms

#### Task 10.4: Document Phase 2 Completion

**Create file:** `remediation-plans/phase-2-completion-report.md`

**Include:**
- SQLDelight implementation details
- Performance benchmarks
- Test results
- Known issues
- Next steps

#### Task 10.5: Commit Changes

```bash
git add .
git commit -m "Phase 2: Implement SQLDelight local storage

- Initialize SQLDelight database drivers for iOS/Android
- Create DatabaseService layer with error handling
- Update repositories to use real database
- Add comprehensive data persistence tests
- Functionality: 35% → 60%

Performance:
- Save: <50ms
- Query: <30ms
- 90 days of data: smooth performance"
```

### 📊 Phase 2 Success Criteria
- [ ] Functionality increased from 35% to 60%
- [ ] Data persists across app restarts
- [ ] All CRUD operations working
- [ ] Performance benchmarks met
- [ ] No data corruption issues
- [ ] Documentation updated
- [ ] Changes committed to git

---

## 🎉 Phase 2 Complete!

**Achievements:**
- ✅ SQLDelight database fully functional
- ✅ Data persists across app lifecycle
- ✅ All CRUD operations working
- ✅ Good performance with large datasets
- ✅ Offline-first capability (local storage)

**Next:** Phase 3 - Implement Firebase services and cloud sync



---

# PHASE 3: REMOTE SERVICES (Week 3)

**Timeline:** Days 11-15  
**Functionality:** 60% → 85% (+25%)  
**Priority:** 🟡 MEDIUM - Enable cloud sync

---

## Day 11: Set Up Firebase Project

### 🎯 Objective
Configure Firebase project and integrate SDKs for iOS and Android.

### 📋 Current State
- No Firebase project configured
- Mock Firebase service in use
- No cloud data storage

### ✅ Tasks

#### Task 11.1: Create Firebase Project
**Platform:** Firebase Console (https://console.firebase.google.com)

**Steps:**
1. Go to Firebase Console
2. Click "Add project"
3. Project name: "Eunio Health App"
4. Enable Google Analytics (optional)
5. Create project

#### Task 11.2: Add iOS App to Firebase

**Steps:**
1. In Firebase Console, click "Add app" → iOS
2. iOS bundle ID: `com.eunio.healthapp.ios` (check in Xcode)
3. Download `GoogleService-Info.plist`
4. Add to `iosApp/iosApp/` directory
5. Add to Xcode project (drag and drop)

**Update iOS app:**
```swift
// iosApp/iosApp/iOSApp.swift
import Firebase

@main
struct iOSApp: App {
    
    init() {
        // Initialize Firebase
        FirebaseApp.configure()
        
        // Initialize Koin
        KoinInitializerKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

#### Task 11.3: Add Android App to Firebase

**Steps:**
1. In Firebase Console, click "Add app" → Android
2. Android package name: `com.eunio.healthapp.android`
3. Download `google-services.json`
4. Add to `androidApp/` directory

**Update Android build.gradle:**
```gradle
// androidApp/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services") // Add this
}

dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
}
```

**Update project build.gradle:**
```gradle
// build.gradle.kts (project level)
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
```

#### Task 11.4: Enable Firestore Database

**In Firebase Console:**
1. Go to Firestore Database
2. Click "Create database"
3. Start in **test mode** (for development)
4. Choose location (closest to users)
5. Enable database

**Set up security rules (temporary - for development):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### Task 11.5: Enable Firebase Authentication

**In Firebase Console:**
1. Go to Authentication
2. Click "Get started"
3. Enable sign-in methods:
   - Email/Password
   - Google (optional)
   - Apple (for iOS, required by App Store)

### 🧪 Verification Steps

1. Build iOS app - should compile without errors
2. Build Android app - should compile without errors
3. Launch apps - check for Firebase initialization logs
4. Check Firebase Console - should show app connections

### 📊 Success Criteria
- [ ] Firebase project created
- [ ] iOS app connected to Firebase
- [ ] Android app connected to Firebase
- [ ] Firestore database enabled
- [ ] Authentication enabled
- [ ] No build errors

---

## Day 12: Implement Firebase Service Layer

### 🎯 Objective
Create service layer for Firebase operations with proper error handling.

### ✅ Tasks

#### Task 12.1: Create Firebase Service Interface
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/FirebaseService.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

interface FirebaseService {
    suspend fun saveDailyLog(log: DailyLog): Result<Unit>
    suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?>
    suspend fun getLogHistory(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyLog>>
    suspend fun deleteDailyLog(logId: String): Result<Unit>
    
    suspend fun saveCycle(cycle: Cycle): Result<Unit>
    suspend fun getCurrentCycle(userId: String): Result<Cycle?>
    suspend fun getCycleHistory(userId: String, limit: Int): Result<List<Cycle>>
    
    suspend fun saveUserSettings(settings: UserSettings): Result<Unit>
    suspend fun getUserSettings(userId: String): Result<UserSettings?>
    
    suspend fun syncData(userId: String): Result<SyncResult>
}

data class SyncResult(
    val logssynced: Int,
    val cyclesSynced: Int,
    val settingsSynced: Boolean,
    val conflicts: List<SyncConflict>
)

data class SyncConflict(
    val dataType: String,
    val localVersion: Any,
    val remoteVersion: Any
)
```

#### Task 12.2: Implement iOS Firebase Service
**File:** `shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/FirebaseServiceImpl.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.remote

import cocoapods.FirebaseFirestore.*
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FirebaseServiceImpl : FirebaseService {
    
    private val firestore = FIRFirestore.firestore()
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val docRef = firestore
                .collectionWithPath("users/${log.userId}/dailyLogs")
                .documentWithPath(log.id)
            
            val data = mapOf(
                "id" to log.id,
                "userId" to log.userId,
                "date" to log.date.toString(),
                "periodFlow" to log.periodFlow?.name,
                "symptoms" to log.symptoms.map { it.name },
                "mood" to log.mood?.name,
                "sexualActivity" to log.sexualActivity?.name,
                "bbt" to log.bbt,
                "cervicalMucus" to log.cervicalMucus?.name,
                "opkResult" to log.opkResult?.name,
                "notes" to log.notes,
                "createdAt" to log.createdAt.toString(),
                "updatedAt" to log.updatedAt.toString()
            )
            
            docRef.setData(data) { error ->
                if (error != null) {
                    continuation.resume(Result.error(
                        AppError.NetworkError(
                            message = "Failed to save log: ${error.localizedDescription}",
                            cause = null
                        )
                    ))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }
        }
    }
    
    override suspend fun getDailyLog(
        userId: String,
        date: LocalDate
    ): Result<DailyLog?> {
        return suspendCancellableCoroutine { continuation ->
            firestore
                .collectionWithPath("users/$userId/dailyLogs")
                .whereField("date", isEqualTo = date.toString())
                .getDocumentsWithCompletion { snapshot, error ->
                    if (error != null) {
                        continuation.resume(Result.error(
                            AppError.NetworkError(
                                message = "Failed to get log: ${error.localizedDescription}",
                                cause = null
                            )
                        ))
                    } else {
                        val doc = snapshot?.documents?.firstOrNull()
                        val log = doc?.data()?.toDailyLog()
                        continuation.resume(Result.success(log))
                    }
                }
        }
    }
    
    // ... implement other methods similarly
}

// Extension to convert Firestore data to domain model
private fun Map<Any?, *>.toDailyLog(): DailyLog {
    return DailyLog(
        id = this["id"] as String,
        userId = this["userId"] as String,
        date = LocalDate.parse(this["date"] as String),
        periodFlow = (this["periodFlow"] as? String)?.let { PeriodFlow.valueOf(it) },
        symptoms = (this["symptoms"] as? List<String>)?.mapNotNull {
            try { Symptom.valueOf(it) } catch (e: Exception) { null }
        } ?: emptyList(),
        mood = (this["mood"] as? String)?.let { Mood.valueOf(it) },
        sexualActivity = (this["sexualActivity"] as? String)?.let { SexualActivity.valueOf(it) },
        bbt = this["bbt"] as? Double,
        cervicalMucus = (this["cervicalMucus"] as? String)?.let { CervicalMucus.valueOf(it) },
        opkResult = (this["opkResult"] as? String)?.let { OPKResult.valueOf(it) },
        notes = this["notes"] as? String,
        createdAt = Instant.parse(this["createdAt"] as String),
        updatedAt = Instant.parse(this["updatedAt"] as String)
    )
}
```

#### Task 12.3: Implement Android Firebase Service
**File:** `shared/src/androidMain/kotlin/com/eunio/healthapp/data/remote/FirebaseServiceImpl.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.tasks.await

actual class FirebaseServiceImpl : FirebaseService {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        return try {
            val data = hashMapOf(
                "id" to log.id,
                "userId" to log.userId,
                "date" to log.date.toString(),
                "periodFlow" to log.periodFlow?.name,
                "symptoms" to log.symptoms.map { it.name },
                "mood" to log.mood?.name,
                "sexualActivity" to log.sexualActivity?.name,
                "bbt" to log.bbt,
                "cervicalMucus" to log.cervicalMucus?.name,
                "opkResult" to log.opkResult?.name,
                "notes" to log.notes,
                "createdAt" to log.createdAt.toString(),
                "updatedAt" to log.updatedAt.toString()
            )
            
            firestore
                .collection("users/${log.userId}/dailyLogs")
                .document(log.id)
                .set(data)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(AppError.NetworkError(
                message = "Failed to save log: ${e.message}",
                cause = e
            ))
        }
    }
    
    override suspend fun getDailyLog(
        userId: String,
        date: LocalDate
    ): Result<DailyLog?> {
        return try {
            val snapshot = firestore
                .collection("users/$userId/dailyLogs")
                .whereEqualTo("date", date.toString())
                .get()
                .await()
            
            val doc = snapshot.documents.firstOrNull()
            val log = doc?.data?.toDailyLog()
            
            Result.success(log)
        } catch (e: Exception) {
            Result.error(AppError.NetworkError(
                message = "Failed to get log: ${e.message}",
                cause = e
            ))
        }
    }
    
    // ... implement other methods similarly
}

// Extension to convert Firestore data to domain model
private fun Map<String, Any>.toDailyLog(): DailyLog {
    return DailyLog(
        id = this["id"] as String,
        userId = this["userId"] as String,
        date = LocalDate.parse(this["date"] as String),
        periodFlow = (this["periodFlow"] as? String)?.let { PeriodFlow.valueOf(it) },
        symptoms = (this["symptoms"] as? List<String>)?.mapNotNull {
            try { Symptom.valueOf(it) } catch (e: Exception) { null }
        } ?: emptyList(),
        mood = (this["mood"] as? String)?.let { Mood.valueOf(it) },
        sexualActivity = (this["sexualActivity"] as? String)?.let { SexualActivity.valueOf(it) },
        bbt = this["bbt"] as? Double,
        cervicalMucus = (this["cervicalMucus"] as? String)?.let { CervicalMucus.valueOf(it) },
        opkResult = (this["opkResult"] as? String)?.let { OPKResult.valueOf(it) },
        notes = this["notes"] as? String,
        createdAt = Instant.parse(this["createdAt"] as String),
        updatedAt = Instant.parse(this["updatedAt"] as String)
    )
}
```

#### Task 12.4: Create Common Expect/Actual Declaration
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/remote/FirebaseServiceImpl.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.remote

expect class FirebaseServiceImpl() : FirebaseService
```

### 🧪 Verification Steps

1. Build project
2. Test save to Firestore
3. Check Firebase Console - verify data appears
4. Test retrieve from Firestore
5. Verify data matches what was saved

### 📊 Success Criteria
- [ ] Firebase service compiles for iOS
- [ ] Firebase service compiles for Android
- [ ] Can save data to Firestore
- [ ] Can retrieve data from Firestore
- [ ] Data visible in Firebase Console

---

## Day 13: Implement Sync Logic

### 🎯 Objective
Create bidirectional sync between local database and Firebase.

### ✅ Tasks

#### Task 13.1: Create Sync Service
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/sync/SyncService.kt`

**Create new file:**
```kotlin
package com.eunio.healthapp.data.sync

import com.eunio.healthapp.data.local.DatabaseService
import com.eunio.healthapp.data.remote.FirebaseService
import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.NetworkConnectivity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SyncService(
    private val databaseService: DatabaseService,
    private val firebaseService: FirebaseService,
    private val networkConnectivity: NetworkConnectivity
) {
    
    suspend fun syncUserData(userId: String): Result<SyncResult> {
        if (!networkConnectivity.isConnected()) {
            return Result.error(AppError.NetworkError(
                message = "No internet connection",
                cause = null
            ))
        }
        
        var logsSynced = 0
        var cyclesSynced = 0
        val conflicts = mutableListOf<SyncConflict>()
        
        try {
            // Sync daily logs
            val localLogs = databaseService.getAllLogs(userId).getOrNull() ?: emptyList()
            
            for (log in localLogs) {
                // Check if log needs sync (modified since last sync)
                if (needsSync(log)) {
                    // Get remote version
                    val remoteLog = firebaseService.getDailyLog(userId, log.date).getOrNull()
                    
                    if (remoteLog == null) {
                        // No remote version, upload local
                        firebaseService.saveDailyLog(log)
                        logsSynced++
                    } else {
                        // Both versions exist, check for conflicts
                        val resolved = resolveConflict(log, remoteLog)
                        
                        if (resolved != null) {
                            // Save resolved version to both
                            databaseService.saveDailyLog(resolved)
                            firebaseService.saveDailyLog(resolved)
                            logsSynced++
                        } else {
                            // Conflict needs user resolution
                            conflicts.add(SyncConflict(
                                dataType = "DailyLog",
                                localVersion = log,
                                remoteVersion = remoteLog
                            ))
                        }
                    }
                }
            }
            
            // Download remote logs not in local database
            // (Implementation depends on Firestore query capabilities)
            
            // Sync cycles (similar logic)
            // ...
            
            // Sync settings
            // ...
            
            return Result.success(SyncResult(
                logsSynced = logsSynced,
                cyclesSynced = cyclesSynced,
                settingsSynced = true,
                conflicts = conflicts
            ))
            
        } catch (e: Exception) {
            return Result.error(AppError.DataSyncError(
                message = "Sync failed: ${e.message}",
                cause = e
            ))
        }
    }
    
    private fun needsSync(log: DailyLog): Boolean {
        // Check if log was modified since last sync
        // This requires tracking last sync time
        return true // For now, always sync
    }
    
    private fun resolveConflict(local: DailyLog, remote: DailyLog): DailyLog? {
        // Simple strategy: most recent update wins
        return if (local.updatedAt > remote.updatedAt) {
            local
        } else {
            remote
        }
    }
}
```

#### Task 13.2: Update Repositories to Use Sync
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/repository/LogRepositoryImpl.kt`

**Update implementation:**
```kotlin
class LogRepositoryImpl(
    private val databaseService: DatabaseService,
    private val firebaseService: FirebaseService,
    private val syncService: SyncService
) : LogRepository {
    
    override suspend fun saveDailyLog(log: DailyLog): Result<Unit> {
        // Save locally first (offline-first)
        val localResult = databaseService.saveDailyLog(log)
        if (localResult.isError) return localResult
        
        // Try to sync to cloud
        try {
            firebaseService.saveDailyLog(log)
        } catch (e: Exception) {
            // Log error but don't fail - will sync later
            println("⚠️  Cloud sync failed, will retry: ${e.message}")
        }
        
        return Result.success(Unit)
    }
    
    override suspend fun getDailyLog(
        userId: String,
        date: LocalDate
    ): Result<DailyLog?> {
        // Try local first (offline-first)
        val localResult = databaseService.getDailyLog(userId, date)
        
        if (localResult.isSuccess) {
            return localResult
        }
        
        // If not found locally and online, try remote
        if (networkConnectivity.isConnected()) {
            val remoteResult = firebaseService.getDailyLog(userId, date)
            
            // Cache remote data locally
            if (remoteResult.isSuccess && remoteResult.data != null) {
                databaseService.saveDailyLog(remoteResult.data)
            }
            
            return remoteResult
        }
        
        return localResult
    }
    
    // ... other methods
}
```

#### Task 13.3: Add Sync to DI Module
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/di/AppModule.kt`

**Update module:**
```kotlin
val appModule = module {
    // Services
    single { DatabaseService(get(), get(), get()) }
    single<FirebaseService> { FirebaseServiceImpl() }
    single { SyncService(get(), get(), get()) }
    
    // Repositories with sync
    single<LogRepository> { 
        LogRepositoryImpl(
            databaseService = get(),
            firebaseService = get(),
            syncService = get()
        ) 
    }
    
    // ... rest of modules
}
```

### 🧪 Verification Steps

1. Save data while online - should save to both local and cloud
2. Check Firebase Console - verify data appears
3. Turn off internet
4. Save data - should save locally
5. Turn on internet
6. Trigger sync - data should upload
7. Delete local database
8. Relaunch app - data should download from cloud

### 📊 Success Criteria
- [ ] Data syncs to cloud when online
- [ ] Data saves locally when offline
- [ ] Offline data syncs when connection restored
- [ ] No data loss in any scenario
- [ ] Conflicts detected and resolved

---

## Day 14: Implement Background Sync

### 🎯 Objective
Add automatic background sync when app returns to foreground or connectivity restored.

### ✅ Tasks

#### Task 14.1: Create Sync Manager
**File:** `shared/src/commonMain/kotlin/com/eunio/healthapp/data/sync/SyncManager.kt`

**Update existing file:**
```kotlin
class SyncManager(
    private val syncService: SyncService,
    private val networkConnectivity: NetworkConnectivity,
    private val coroutineScope: CoroutineScope
) {
    
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private var syncJob: Job? = null
    
    fun startAutoSync() {
        // Monitor connectivity changes
        coroutineScope.launch {
            networkConnectivity.observeConnectivity()
                .collect { isConnected ->
                    if (isConnected) {
                        triggerSync()
                    }
                }
        }
    }
    
    fun triggerSync(userId: String = "current_user") {
        if (syncJob?.isActive == true) {
            return // Already syncing
        }
        
        syncJob = coroutineScope.launch {
            _syncState.value = SyncState.SYNCING
            
            val result = syncService.syncUserData(userId)
            
            _syncState.value = if (result.isSuccess) {
                SyncState.SYNCED(result.data!!)
            } else {
                SyncState.FAILED(result.errorOrNull()!!)
            }
        }
    }
}

sealed class SyncState {
    object IDLE : SyncState()
    object SYNCING : SyncState()
    data class SYNCED(val result: SyncResult) : SyncState()
    data class FAILED(val error: AppError) : SyncState()
}
```

#### Task 14.2: Integrate with iOS App Lifecycle
**File:** `iosApp/iosApp/iOSApp.swift`

**Update app:**
```swift
@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    
    init() {
        FirebaseApp.configure()
        KoinInitializerKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                // App became active, trigger sync
                triggerBackgroundSync()
            }
        }
    }
    
    private func triggerBackgroundSync() {
        // Get sync manager from DI and trigger sync
        // Implementation depends on how you expose Koin to Swift
    }
}
```

#### Task 14.3: Integrate with Android App Lifecycle
**File:** `androidApp/src/main/java/com/eunio/healthapp/android/HealthApp.kt`

**Update app:**
```kotlin
class HealthApp : Application() {
    
    private lateinit var syncManager: SyncManager
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@HealthApp)
            modules(appModule)
        }
        
        syncManager = get()
        syncManager.startAutoSync()
        
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver(syncManager)
        )
    }
}

class AppLifecycleObserver(
    private val syncManager: SyncManager
) : DefaultLifecycleObserver {
    
    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        syncManager.triggerSync()
    }
}
```

### 🧪 Verification Steps

1. Save data while online
2. Turn off internet
3. Save more data
4. Turn on internet
5. Wait for auto-sync (or trigger manually)
6. Verify all data synced to cloud

### 📊 Success Criteria
- [ ] Auto-sync triggers on app foreground
- [ ] Auto-sync triggers on connectivity restored
- [ ] Sync doesn't block UI
- [ ] Sync status visible to user
- [ ] Failed syncs retry automatically

---

## Day 15: Phase 3 Validation & Documentation

### 🎯 Objective
Validate Phase 3 completion and measure progress.

### ✅ Tasks

#### Task 15.1: Run Full Sync Test Suite

**Test Scenarios:**

**Scenario 1: Online Sync**
- [ ] Save data while online
- [ ] Verify data in Firebase Console
- [ ] Delete local database
- [ ] Relaunch app
- [ ] Verify data downloads from cloud

**Scenario 2: Offline then Online**
- [ ] Turn off internet
- [ ] Save multiple logs
- [ ] Turn on internet
- [ ] Verify auto-sync occurs
- [ ] Check Firebase Console for all data

**Scenario 3: Multi-Device Sync**
- [ ] Save data on Device A
- [ ] Wait for sync
- [ ] Open app on Device B
- [ ] Verify data appears
- [ ] Modify data on Device B
- [ ] Check Device A receives update

**Scenario 4: Conflict Resolution**
- [ ] Turn off internet on both devices
- [ ] Modify same log on both devices
- [ ] Turn on internet
- [ ] Verify conflict detected
- [ ] Verify conflict resolved (most recent wins)

#### Task 15.2: Measure Functionality Improvement

**Before Phase 3:** 60%
**After Phase 3:** Target 85%

**Calculate actual:**
```
Now functional:
- DI System: 100% ✅
- ViewModels: 100% ✅
- Use Cases: 100% ✅
- Repositories: 100% ✅
- Local Storage: 100% ✅
- Remote Services: 100% ✅
- Cloud Sync: 100% ✅
- Auto Sync: 100% ✅
- Data Flow: 100% ✅

Still missing:
- iOS StateFlow optimization: 0% ❌
- Advanced conflict resolution: 50% ⚠️
- Performance optimization: 70% ⚠️

Actual functionality: ~85%
```

#### Task 15.3: Performance Benchmarks

**Measure and document:**
- Sync 10 logs: < 2 seconds
- Sync 100 logs: < 10 seconds
- Download all data: < 5 seconds
- Background sync: < 3 seconds
- Conflict resolution: < 1 second

#### Task 15.4: Document Phase 3 Completion

**Create file:** `remediation-plans/phase-3-completion-report.md`

#### Task 15.5: Commit Changes

```bash
git add .
git commit -m "Phase 3: Implement Firebase and cloud sync

- Set up Firebase project for iOS and Android
- Implement FirebaseService for Firestore operations
- Create bidirectional sync logic
- Add automatic background sync
- Implement basic conflict resolution
- Functionality: 60% → 85%

Performance:
- Sync 10 logs: <2s
- Sync 100 logs: <10s
- Multi-device sync working"
```

### 📊 Phase 3 Success Criteria
- [ ] Functionality increased from 60% to 85%
- [ ] Cloud sync fully functional
- [ ] Multi-device sync working
- [ ] Offline-first architecture working
- [ ] Auto-sync on connectivity/foreground
- [ ] Performance benchmarks met
- [ ] Documentation updated

---

## 🎉 Phase 3 Complete!

**Achievements:**
- ✅ Firebase fully integrated
- ✅ Cloud sync working
- ✅ Multi-device sync functional
- ✅ Offline-first with auto-sync
- ✅ Basic conflict resolution

**Next:** Phase 4 - Optimize and polish to 100%

