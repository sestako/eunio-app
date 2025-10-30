# Manual Fixes Checklist - iOS Compilation Errors

**Status:** 84 errors remaining after automated fixes
**Priority:** Fix ViewModels and Core types first

---

## Category 1: Result Type Conflicts (4 errors)

**Issue:** `generic type 'Result' specialized with too many type parameters (got 2, but expected 1)`

**Root Cause:** Kotlin's `Result<T>` conflicts with Swift's `Result<T, Error>`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Settings/MockNotificationServices.swift`
- [ ] `iosApp/iosApp/ViewModels/ProfileManagementViewModelWrapper.swift`

**Fix:** Use `Swift.Result<T, Error>` explicitly instead of `Result<T, Error>`

**Status:** Partially fixed by sed script, verify remaining instances

---

## Category 2: Missing Kotlinx Types (4 errors)

**Issue:** `cannot find type 'Kotlinx_coroutines_coreFlow' in scope`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Settings/MockNotificationServices.swift`
- [ ] `iosApp/iosApp/ViewModels/NotificationPreferencesViewModelWrapper.swift`

**Fix:** These files need proper Flow handling. Options:
1. Import Shared module properly
2. Create Flow wrapper/bridge
3. Use AsyncSequence instead

**Priority:** Medium (affects notification features)

---

## Category 3: Missing Shared Module Types (9 errors)

**Issue:** Cannot find types: `Insight`, `Mood`, `Tutorial`, `HelpCategory`, `shared`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Components/ModernListStyles.swift` - Insight (2 errors)
- [ ] `iosApp/iosApp/Views/Charts/HealthCharts.swift` - Mood (2 errors)
- [ ] `iosApp/iosApp/Views/Settings/HelpSupportScreen.swift` - HelpCategory, FAQ, Tutorial (4 errors)
- [ ] `iosApp/iosApp/ViewModels/ModernCalendarViewModel.swift` - shared namespace (1 error)

**Fix:** 
1. Verify these types exist in Shared module
2. Add proper imports
3. If types don't exist, create them or use alternatives

**Priority:** Low (UI components, not core functionality)

---

## Category 4: Environment Keys Missing (3 errors)

**Issue:** `value of type 'EnvironmentValues' has no member 'settingsManager'`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Settings/CyclePreferencesScreen.swift`
- [ ] `iosApp/iosApp/Views/Settings/NotificationPreferencesScreen.swift`

**Fix:** Create environment keys:
```swift
private struct SettingsManagerKey: EnvironmentKey {
    static let defaultValue: SettingsManager? = nil
}

private struct NotificationManagerKey: EnvironmentKey {
    static let defaultValue: NotificationManager? = nil
}

extension EnvironmentValues {
    var settingsManager: SettingsManager? {
        get { self[SettingsManagerKey.self] }
        set { self[SettingsManagerKey.self] = newValue }
    }
    
    var notificationManager: NotificationManager? {
        get { self[NotificationManagerKey.self] }
        set { self[NotificationManagerKey.self] = newValue }
    }
}
```

**Priority:** Medium (affects settings screens)

---

## Category 5: Duplicate Declarations (5 errors)

**Issue:** `invalid redeclaration of 'InfoRow'`, `invalid redeclaration of 'SectionHeader'`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Calendar/CalendarView.swift` - InfoRow
- [ ] `iosApp/iosApp/Views/Settings/DataUsageInfoView.swift` - InfoRow
- [ ] `iosApp/iosApp/Views/Settings/ProfileManagementScreen.swift` - InfoRow
- [ ] `iosApp/iosApp/Views/Logging/DailyLoggingComponents.swift` - SectionHeader
- [ ] `iosApp/iosApp/Views/Settings/DisplayPreferencesScreen.swift` - SectionHeader

**Fix:** Rename one of the duplicate declarations or move to shared location

**Priority:** Low (UI components)

---

## Category 6: Protocol Conformance Issues (3 errors)

**Issue:** Various protocol conformance problems

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Charts/HealthCharts.swift` - `type 'any HealthDataService' cannot conform to 'ObservableObject'`
- [ ] `iosApp/iosApp/Views/MainTabView.swift` - `type 'any HealthDataService' cannot conform to 'ObservableObject'`
- [ ] `iosApp/iosApp/ViewModels/ModernViewModelFactory.swift` - `type 'ViewModelContainer' does not conform to protocol 'ObservableObject'`

**Fix:** 
1. Remove `@StateObject` or `@ObservedObject` from protocol types
2. Use concrete types instead of protocols
3. Add `ObservableObject` conformance where appropriate

**Priority:** High (affects ViewModels)

---

## Category 7: Initializer/KeyPath Issues (14 errors)

**Issue:** `no exact matches in call to initializer` and `cannot infer key path type from context`

**Files Affected:**
- [ ] `iosApp/iosApp/Views/Settings/CyclePreferencesScreen.swift`
- [ ] `iosApp/iosApp/Views/Settings/NotificationPreferencesScreen.swift`
- [ ] `iosApp/iosApp/ViewModels/ModernViewModelFactory.swift`

**Fix:** These are likely related to environment keys and need case-by-case analysis

**Priority:** Medium

---

## Category 8: Missing Properties/Methods (4 errors)

**Issue:** Type members that don't exist

**Files Affected:**
- [ ] `iosApp/iosApp/ViewModels/SyncPreferencesViewModelWrapper.swift` - `type 'SyncStatus' has no member 'idle'`
- [ ] `iosApp/iosApp/Views/Logging/DailyLoggingComponents.swift` - `type 'Symptom' has no member 'foodCravings'`
- [ ] `iosApp/iosApp/Views/Settings/MockNotificationServices.swift` - `type 'MockSettingsRepository' does not conform to protocol 'SettingsRepository'`

**Fix:** 
1. Check if these properties exist in Shared module
2. Update to use correct property names
3. Fix protocol conformance

**Priority:** Medium

---

## Category 9: Other Issues (remaining errors)

**Files Affected:**
- [ ] `iosApp/iosApp/ViewModels/ProfileManagementViewModelWrapper.swift` - override keyword, inheritance issues
- [ ] `iosApp/iosApp/ViewModels/NotificationPreferencesViewModelWrapper.swift` - redeclaration
- [ ] `iosApp/iosApp/ViewModels/ObservableDailyLoggingViewModel.swift` - redeclaration
- [ ] `iosApp/iosApp/Views/Authentication/SignInView.swift` - redeclaration
- [ ] `iosApp/iosApp/Views/Authentication/SignUpView.swift` - redeclaration
- [ ] `iosApp/iosApp/Views/Settings/SettingsTabView.swift` - redeclaration, init issues
- [ ] `iosApp/iosApp/Views/Settings/NotificationSettingItem.swift` - missing arguments
- [ ] `iosApp/iosApp/Views/Settings/DisplayPreferencesScreen.swift` - cannot find type
- [ ] `iosApp/iosApp/Views/Settings/SupportRequestFormView.swift` - missing types
- [ ] `iosApp/iosApp/Views/Examples/ModernViewModelUsageExample.swift` - redeclaration

**Priority:** Low to Medium (case-by-case)

---

## Recommended Fix Order

### Phase 1: Core Types (High Priority)
1. ✅ Protocol conformance (AppError, KotlinThrowable) - DONE
2. [ ] ViewModelFactory ObservableObject issue
3. [ ] HealthDataService ObservableObject issues

### Phase 2: ViewModels (High Priority)
4. [ ] ProfileManagementViewModelWrapper
5. [ ] NotificationPreferencesViewModelWrapper
6. [ ] SyncPreferencesViewModelWrapper
7. [ ] ModernViewModelFactory

### Phase 3: Environment & Settings (Medium Priority)
8. [ ] Create environment keys (settingsManager, notificationManager)
9. [ ] Fix CyclePreferencesScreen
10. [ ] Fix NotificationPreferencesScreen
11. [ ] Fix MockNotificationServices

### Phase 4: UI Components (Low Priority)
12. [ ] Fix duplicate declarations (InfoRow, SectionHeader)
13. [ ] Fix missing Shared types (Insight, Mood, Tutorial, etc.)
14. [ ] Fix HealthCharts
15. [ ] Fix other view files

---

## Progress Tracking

- **Total Errors:** 84
- **Fixed:** 0
- **Remaining:** 84
- **Completion:** 0%

**Last Updated:** 2025-10-20
