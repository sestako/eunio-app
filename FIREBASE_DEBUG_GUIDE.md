# Firebase Sync Debugging Guide

Tento dokument popisuje, jak používat Firebase debugging infrastrukturu pro diagnostiku sync problémů.

## Přehled

Implementovali jsme kompletní debugging a verification infrastrukturu pro Firebase sync na iOS i Android:

- ✅ Detailní Firebase logování
- ✅ Automatické diagnostic reporty
- ✅ Integrity testy (write/read)
- ✅ Network connectivity monitoring
- ✅ Auth status verification

## iOS Debugging

### 1. Zapnutí Debug Logování

Debug logování je automaticky zapnuté v Debug builds. Při spuštění app uvidíte v Xcode konzoli:

```
🔥 AppDelegate: Firebase debug logging enabled
🔥 AppDelegate: Firestore debug logging enabled via Firebase logger
🔥 AppDelegate: Firebase Project ID: eunio-c4dde
🔥 AppDelegate: Firebase API Key: AIzaSy...
```

### 2. Diagnostic Report

Při každém spuštění app v Debug módu se automaticky vygeneruje diagnostic report:

```
================================================================================
FIREBASE DIAGNOSTICS REPORT
Generated: 2025-10-23 ...
================================================================================

📱 FIREBASE INITIALIZATION
─────────────────────────────────────────────────────────────────────────────
✅ Firebase initialized: YES
   App name: __FIRAPP_DEFAULT
   Project ID: eunio-c4dde
   ...

🔐 AUTHENTICATION STATUS
─────────────────────────────────────────────────────────────────────────────
✅ User authenticated: YES
   User ID: abc123...
   ...

🌐 NETWORK CONNECTIVITY
─────────────────────────────────────────────────────────────────────────────
   Status: Connected (WiFi)

🔥 FIRESTORE CONFIGURATION
─────────────────────────────────────────────────────────────────────────────
   Persistence enabled: true
   ...

🌉 FIREBASE BRIDGE
─────────────────────────────────────────────────────────────────────────────
   Bridge connected: YES

📱 DEVICE INFORMATION
─────────────────────────────────────────────────────────────────────────────
   Device: iPhone
   OS Version: 18.0
   ...
```

Report se také ukládá do souboru v Documents directory.

### 3. Integrity Test

Pro spuštění integrity testu:

1. V Xcode, přidej do scheme environment variable:
   - Name: `DEBUG_SYNC_TEST`
   - Value: `true`

2. Nebo v kódu:
```swift
UserDefaults.standard.set(true, forKey: "DEBUG_SYNC_TEST")
```

3. Restart app

Uvidíš v konzoli:
```
🧪 Running Firebase integrity test...
✅ Integrity test - write successful (123ms)
✅ Integrity test PASSED
🧪 Write latency: 123ms
🧪 Read latency: 45ms
🧪 Total latency: 168ms
```

### 4. Sledování Sync Operací

Každá save/load operace loguje detaily:

```
[FirestoreService.iOS] SAVE_DAILY_LOG_START
  userId: abc123
  logId: 2025-10-23
  path: users/abc123/dailyLogs/2025-10-23
  dateEpochDays: 19664

[FirestoreService.iOS] SAVE_DAILY_LOG_SUCCESS
  userId: abc123
  logId: 2025-10-23
  updatedAt: 1729641600
```

Pro sledování v reálném čase:
```bash
# iOS - Xcode Console
# Filtruj: FirestoreService.iOS

# Android - Logcat
adb logcat | grep "FirestoreService.Android"
```

## Android Debugging

### 1. Zapnutí Debug Logování

Debug logování je automaticky zapnuté v Debug builds. V Logcat uvidíš:

```
D/EunioApplication: 🔥 Starting Firebase initialization...
D/EunioApplication: 🔥 Debug build - Firebase debug logging available via adb
D/EunioApplication:    Run: adb shell setprop log.tag.FirebaseFirestore DEBUG
D/EunioApplication:    Run: adb shell setprop log.tag.FirebaseAuth DEBUG
D/EunioApplication: 🔥 Firebase initialized successfully
D/EunioApplication:    Project ID: eunio-c4dde
```

### 2. Zapnutí Detailního Firebase Logování

Pro ještě více detailů, spusť v terminálu:

```bash
# Zapnout Firestore debug logging
adb shell setprop log.tag.FirebaseFirestore DEBUG

# Zapnout Auth debug logging
adb shell setprop log.tag.FirebaseAuth DEBUG

# Restart app
adb shell am force-stop com.eunio.healthapp.android
adb shell am start -n com.eunio.healthapp.android/.MainActivity
```

Pak filtruj logcat:
```bash
adb logcat | grep -E "(Firebase|Firestore|EunioApplication)"
```

### 3. Diagnostic Report

Při každém spuštění app v Debug módu se automaticky vygeneruje diagnostic report v Logcat:

```
D/FirebaseDiagnostics: ================================================================================
D/FirebaseDiagnostics: FIREBASE DIAGNOSTICS REPORT (Android)
D/FirebaseDiagnostics: Generated: ...
D/FirebaseDiagnostics: ================================================================================
D/FirebaseDiagnostics: 
D/FirebaseDiagnostics: 📱 FIREBASE INITIALIZATION
D/FirebaseDiagnostics: ─────────────────────────────────────────────────────────────────────────────
D/FirebaseDiagnostics: ✅ Firebase initialized: YES
...
```

Report se také ukládá do souboru v External Files directory.

### 4. Integrity Test

Pro spuštění integrity testu:

1. V kódu nebo přes SharedPreferences:
```kotlin
val prefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
prefs.edit().putBoolean("DEBUG_SYNC_TEST", true).apply()
```

2. Restart app

Uvidíš v Logcat:
```
D/FirebaseDiagnostics: 🧪 Running Firebase integrity test...
D/FirebaseDiagnostics: ✅ Integrity test - write successful (156ms)
D/FirebaseDiagnostics: ✅ Integrity test PASSED
D/FirebaseDiagnostics: 🧪 Write latency: 156ms
D/FirebaseDiagnostics: 🧪 Read latency: 67ms
D/FirebaseDiagnostics: 🧪 Total latency: 223ms
```

## Ověření Cross-Platform Sync

### Test iOS → Android

1. Spusť iOS app
2. Ulož daily log pro dnešní datum
3. Zkontroluj Firebase Console - měl by se objevit dokument v `users/{userId}/dailyLogs/`
4. Spusť Android app se stejným uživatelem
5. Načti dnešní datum - měl by se zobrazit stejný log

### Test Android → iOS

1. Spusť Android app
2. Ulož daily log pro dnešní datum
3. Zkontroluj Firebase Console
4. Spusť iOS app se stejným uživatelem
5. Načti dnešní datum - měl by se zobrazit stejný log

## Firebase Console Verification

1. Otevři [Firebase Console](https://console.firebase.google.com/)
2. Vyber projekt `eunio-c4dde`
3. Jdi na Firestore Database
4. Naviguj na `users/{userId}/dailyLogs/`
5. Měl bys vidět dokumenty s touto strukturou:

```json
{
  "logId": "2025-10-23",
  "dateEpochDays": 19664,
  "createdAt": 1729641600,
  "updatedAt": 1729641600,
  "periodFlow": "MEDIUM",
  "symptoms": ["CRAMPS", "FATIGUE"],
  "mood": "TIRED",
  "notes": "...",
  "v": 1
}
```

## Troubleshooting

### iOS: "Bridge connection test failed"

**Příčina:** Firebase bridge není správně inicializovaný.

**Řešení:**
1. Zkontroluj, že `FirebaseBridgeSetup.initialize()` je volaný v `AppDelegate`
2. Rebuild shared framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
3. Clean build iOS app

### Android: "Firebase initialization failed"

**Příčina:** Chybí nebo je špatný `google-services.json`.

**Řešení:**
1. Zkontroluj, že `androidApp/google-services.json` existuje
2. Ověř, že `package_name` odpovídá `applicationId` v `build.gradle`
3. Sync Gradle files

### "Permission Denied" při zápisu

**Příčina:** Firestore security rules nebo chybějící autentizace.

**Řešení:**
1. Zkontroluj diagnostic report - je uživatel přihlášený?
2. Zkontroluj Firestore rules v `firestore.rules`
3. Ověř, že rules jsou nasazené: `firebase deploy --only firestore:rules`

### Data se neobjevují v Firebase Console

**Příčina:** Možná offline režim nebo network issues.

**Řešení:**
1. Zkontroluj network status v diagnostic reportu
2. Zkontroluj logy - hledej "SAVE_DAILY_LOG_SUCCESS"
3. Zkus force refresh Firebase Console
4. Spusť integrity test

### Cross-platform sync nefunguje

**Příčina:** Různé field names nebo paths.

**Řešení:**
1. Zkontroluj, že obě platformy používají `dateEpochDays` (ne `date`)
2. Zkontroluj, že obě platformy používají path `users/{userId}/dailyLogs/{logId}`
3. Porovnej dokumenty v Firebase Console - měly by mít identickou strukturu

## Užitečné Příkazy

### iOS

```bash
# Rebuild shared framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Clean build iOS
cd iosApp && xcodebuild clean

# Build iOS
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -arch arm64 build
```

### Android

```bash
# Build Android
./gradlew :androidApp:assembleDebug

# Install na zařízení
./gradlew :androidApp:installDebug

# Sledovat logy
adb logcat | grep -E "(Firebase|Firestore|EunioApplication|FirebaseDiagnostics)"

# Clear app data
adb shell pm clear com.eunio.healthapp.android
```

## Soubory

### iOS
- `iosApp/iosApp/iOSApp.swift` - Firebase initialization + debug logging
- `iosApp/iosApp/Services/FirebaseDiagnostics.swift` - Diagnostic utility
- `iosApp/iosApp/Services/FirebaseBridgeSetup.swift` - Bridge setup + integrity test
- `iosApp/iosApp/Services/FirebaseIOSBridge.swift` - Firebase operations

### Android
- `androidApp/src/androidMain/kotlin/.../EunioApplication.kt` - Firebase initialization + debug logging
- `androidApp/src/androidMain/kotlin/.../FirebaseDiagnostics.kt` - Diagnostic utility

### Shared
- `shared/src/iosMain/kotlin/.../FirestoreServiceImpl.ios.kt` - iOS Firestore implementation
- `shared/src/androidMain/kotlin/.../FirestoreServiceImpl.android.kt` - Android Firestore implementation
- `firestore.rules` - Security rules
- `firestore.indexes.json` - Firestore indexes

## Další Kroky

Po ověření, že sync funguje:

1. ✅ Dokončit cross-platform sync testy (spec 1 & 2)
2. ✅ Implementovat offline-first architecture s conflict resolution
3. ✅ Přidat automated monitoring pro sync latency
4. ✅ Optimalizovat batch operations
