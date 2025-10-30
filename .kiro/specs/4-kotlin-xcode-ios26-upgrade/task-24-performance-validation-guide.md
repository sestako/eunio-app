# Task 24: Performance Validation Guide

## Overview

This document provides comprehensive guidance for validating performance after the Kotlin 2.2.20 + iOS 26 upgrade.

## Automated Performance Validation

### Running the Script

```bash
./scripts/performance-validation.sh
```

This script automatically measures:
- ✅ Gradle build time (clean build)
- ✅ iOS framework build time
- ✅ Dependency resolution time
- ✅ Build artifact sizes
- ✅ Memory usage (build process)

### Results Location

Reports are saved to: `test-results/performance/performance-report-[timestamp].md`

## Manual Performance Testing

### 1. App Startup Time Measurement

#### Android Testing

**Method 1: Using ADB (Recommended)**
```bash
# Clear app data first
adb shell pm clear com.eunio.healthapp.android

# Launch and measure
adb shell am start -W -n com.eunio.healthapp.android/.MainActivity

# Look for "TotalTime" in output
```

**Expected Output:**
```
Starting: Intent { cmp=com.eunio.healthapp.android/.MainActivity }
Status: ok
Activity: com.eunio.healthapp.android/.MainActivity
ThisTime: 1234
TotalTime: 1234  <-- This is the startup time in ms
WaitTime: 1250
Complete
```

**Method 2: Using Android Studio Profiler**
1. Open Android Studio
2. Run app with profiler attached
3. Navigate to CPU profiler
4. Look for app startup trace
5. Measure from `Application.onCreate()` to first frame rendered

**Acceptance Criteria:**
- Cold start: < 2000ms on modern devices (Pixel 5+, Samsung S21+)
- Warm start: < 1000ms
- Hot start: < 500ms

#### iOS Testing

**Method 1: Using Xcode Instruments**
```bash
# Build and run with Instruments
# Product > Profile (Cmd+I)
# Select "Time Profiler" template
# Launch app and measure
```

**Steps:**
1. Open Xcode
2. Select Product > Profile (Cmd+I)
3. Choose "App Launch" template
4. Run the app
5. Review launch time in the timeline

**Method 2: Using Console.app**
```bash
# Open Console.app
# Filter for: process:iosApp
# Look for "PROCESS_LAUNCH" events
```

**Acceptance Criteria:**
- Cold start: < 1500ms on iPhone 12+
- Warm start: < 800ms
- Hot start: < 400ms

### 2. Firebase Sync Latency Measurement

#### Test Procedure (Both Platforms)

**Setup:**
1. Ensure device has good internet connection
2. Sign in to the app
3. Clear any pending sync queue

**Test 1: Online Save & Sync**
```
1. Navigate to Daily Logging screen
2. Start timer
3. Enter data (temperature, symptoms, etc.)
4. Tap "Save" button
5. Stop timer when sync confirmation appears
6. Record time
```

**Expected Results:**
- Save to local DB: < 100ms
- Upload to Firestore: < 1000ms
- Total operation: < 1500ms

**Test 2: Offline Save & Queue**
```
1. Enable airplane mode
2. Navigate to Daily Logging screen
3. Start timer
4. Enter data
5. Tap "Save" button
6. Stop timer when save confirmation appears
7. Record time
```

**Expected Results:**
- Save to local DB: < 100ms
- Queue for sync: < 50ms
- Total operation: < 200ms

**Test 3: Sync After Coming Online**
```
1. Save data while offline (as above)
2. Disable airplane mode
3. Start timer
4. Wait for automatic sync
5. Stop timer when sync completes
6. Record time
```

**Expected Results:**
- Sync detection: < 2000ms
- Upload queued items: < 1000ms per item
- Total operation: < 3000ms for 1-2 items

#### Measurement Tools

**Android:**
```bash
# Monitor Firebase operations in Logcat
adb logcat | grep -E "Firebase|Firestore|Sync"
```

**iOS:**
```bash
# Monitor in Console.app
# Filter: subsystem:com.eunio.healthapp
# Look for Firebase-related logs
```

### 3. Memory Usage Profiling

#### Android Memory Profiling

**Using Android Studio Profiler:**
```
1. Open Android Studio
2. Run app with profiler attached
3. Navigate to Memory profiler
4. Perform typical user actions:
   - Sign in
   - Navigate between screens
   - Save daily logs
   - View calendar
   - Check insights
5. Monitor memory allocation
6. Check for memory leaks
```

**Key Metrics to Record:**
- **Idle memory:** App running, no user interaction
- **Active memory:** During typical usage
- **Peak memory:** Maximum observed
- **Memory leaks:** Check for growing allocations

**Expected Results:**
- Idle: 60-80 MB
- Active: 80-120 MB
- Peak: < 150 MB
- Leaks: None detected

**Using ADB:**
```bash
# Get memory info
adb shell dumpsys meminfo com.eunio.healthapp.android

# Monitor continuously
watch -n 1 'adb shell dumpsys meminfo com.eunio.healthapp.android | grep "TOTAL"'
```

#### iOS Memory Profiling

**Using Xcode Instruments:**
```
1. Open Xcode
2. Product > Profile (Cmd+I)
3. Select "Leaks" template
4. Run the app
5. Perform typical user actions
6. Monitor memory graph
7. Check for leaks
```

**Key Metrics to Record:**
- **Idle memory:** App in foreground, no interaction
- **Active memory:** During typical usage
- **Peak memory:** Maximum observed
- **Leaks:** Check for memory leaks

**Expected Results:**
- Idle: 40-60 MB
- Active: 60-90 MB
- Peak: < 100 MB
- Leaks: None detected

**Using Xcode Debug Navigator:**
```
1. Run app in debug mode
2. Open Debug Navigator (Cmd+7)
3. Select "Memory" gauge
4. Monitor during usage
```

### 4. Build Time Comparison

#### Pre-Upgrade Baseline (Estimated)

**Kotlin 1.9.21 + iOS 17:**
- Gradle clean build: ~120-180s
- iOS framework build: ~60-90s
- Full clean build (both): ~180-270s

#### Post-Upgrade Measurements

Run the automated script:
```bash
./scripts/performance-validation.sh
```

**Expected Results:**
- Gradle build: Similar or improved (Kotlin 2.2.20 has compiler optimizations)
- iOS framework: Similar (Kotlin/Native improvements)
- Overall: Within 10% of baseline

### 5. UI Responsiveness Testing

#### Test Procedure

**Scroll Performance:**
1. Navigate to Calendar screen
2. Scroll through months rapidly
3. Observe frame rate and smoothness
4. Check for jank or stuttering

**Expected:** Smooth 60fps scrolling, no dropped frames

**Navigation Performance:**
1. Rapidly switch between tabs
2. Navigate back and forth between screens
3. Measure transition smoothness

**Expected:** Instant transitions, no lag

**Input Responsiveness:**
1. Type in text fields
2. Tap buttons
3. Interact with pickers/selectors

**Expected:** Immediate feedback, no input lag

## Performance Comparison Template

### Build Performance

| Metric | Pre-Upgrade | Post-Upgrade | Change | Status |
|--------|-------------|--------------|--------|--------|
| Gradle clean build | ~150s | [MEASURE] | [CALC] | [✅/⚠️] |
| iOS framework build | ~75s | [MEASURE] | [CALC] | [✅/⚠️] |
| Dependency resolution | ~10s | [MEASURE] | [CALC] | [✅/⚠️] |

### Runtime Performance

| Metric | Pre-Upgrade | Post-Upgrade | Change | Status |
|--------|-------------|--------------|--------|--------|
| Android cold start | ~1500ms | [MEASURE] | [CALC] | [✅/⚠️] |
| iOS cold start | ~1000ms | [MEASURE] | [CALC] | [✅/⚠️] |
| Firebase sync (online) | ~800ms | [MEASURE] | [CALC] | [✅/⚠️] |
| Firebase sync (offline) | ~150ms | [MEASURE] | [CALC] | [✅/⚠️] |

### Memory Usage

| Metric | Pre-Upgrade | Post-Upgrade | Change | Status |
|--------|-------------|--------------|--------|--------|
| Android idle | ~70MB | [MEASURE] | [CALC] | [✅/⚠️] |
| Android active | ~100MB | [MEASURE] | [CALC] | [✅/⚠️] |
| iOS idle | ~50MB | [MEASURE] | [CALC] | [✅/⚠️] |
| iOS active | ~75MB | [MEASURE] | [CALC] | [✅/⚠️] |

## Acceptance Criteria

### ✅ Pass Criteria

1. **Build times:** Within 10% of baseline
2. **App startup:** Within 10% of baseline or improved
3. **Firebase sync:** Within 10% of baseline or improved
4. **Memory usage:** Within 10% of baseline or improved
5. **No memory leaks:** Zero leaks detected
6. **UI responsiveness:** Smooth 60fps, no jank

### ⚠️ Warning Criteria

1. **Build times:** 10-20% slower than baseline
2. **App startup:** 10-20% slower than baseline
3. **Memory usage:** 10-20% higher than baseline

**Action:** Investigate and optimize if possible, but may proceed

### ❌ Fail Criteria

1. **Build times:** >20% slower than baseline
2. **App startup:** >20% slower than baseline
3. **Memory leaks:** Any leaks detected
4. **Memory usage:** >20% higher than baseline
5. **UI jank:** Visible stuttering or dropped frames

**Action:** Must investigate and fix before proceeding

## Troubleshooting

### Slow Build Times

**Possible causes:**
1. Gradle cache not warmed up
2. Insufficient memory allocation
3. Dependency resolution issues

**Solutions:**
```bash
# Increase Gradle memory
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g" >> gradle.properties

# Enable configuration cache
echo "org.gradle.configuration-cache=true" >> gradle.properties

# Run build again to warm cache
./gradlew clean build
```

### Slow App Startup

**Possible causes:**
1. Koin initialization overhead
2. Database migration
3. Firebase initialization

**Solutions:**
- Profile with Instruments/Profiler to identify bottleneck
- Consider lazy initialization for non-critical services
- Optimize database queries

### High Memory Usage

**Possible causes:**
1. Memory leaks in new code
2. Larger dependency footprint
3. Retained objects

**Solutions:**
- Use memory profiler to identify leaks
- Review recent code changes
- Check for retained listeners/observers

### Slow Firebase Sync

**Possible causes:**
1. Network latency
2. Large data payloads
3. Firestore indexing

**Solutions:**
- Check network conditions
- Optimize data structure
- Review Firestore indexes

## Documentation

After completing all measurements, document results in:
```
.kiro/specs/4-kotlin-xcode-ios26-upgrade/task-24-performance-results.md
```

Include:
1. All measured metrics
2. Comparison with baseline
3. Analysis of any regressions
4. Recommendations for optimization
5. Sign-off for production readiness

## Next Steps

After performance validation:
1. ✅ Review all metrics
2. ✅ Document any issues found
3. ✅ Implement optimizations if needed
4. ✅ Get team sign-off
5. ✅ Proceed to task 25 (Toolchain verification script)

---

*Last updated: $(date)*
