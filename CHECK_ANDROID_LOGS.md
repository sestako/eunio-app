# Android Firebase Sync - Debugging Checklist

## Rychlý Test

1. **Spusť test script:**
   ```bash
   ./test_android_firebase.sh
   ```

2. **Nebo manuálně:**
   ```bash
   # Install app
   ./gradlew :androidApp:installDebug
   
   # Start app
   adb shell am start -n com.eunio.healthapp.android/.MainActivity
   
   # Watch logs
   adb logcat | grep -E "(LogRepository|FirestoreService\.Android)"
   ```

## Co hledat v lozích

### 1. Firebase Initialization (při startu app)
```
D/EunioApplication: 🔥 Starting Firebase initialization...
D/EunioApplication: 🔥 Firebase initialized successfully
D/EunioApplication:    Project ID: eunio-c4dde
```

**Pokud nevidíš:** Firebase není inicializovaný - zkontroluj `google-services.json`

### 2. Diagnostic Report (při startu app)
```
D/FirebaseDiagnostics: ================================================================================
D/FirebaseDiagnostics: FIREBASE DIAGNOSTICS REPORT (Android)
D/FirebaseDiagnostics: ✅ Firebase initialized: YES
D/FirebaseDiagnostics: ✅ User authenticated: YES
D/FirebaseDiagnostics:    User ID: abc123...
```

**Pokud nevidíš:** Diagnostic není zapnutý nebo app není v debug módu

### 3. Save Operation (když uložíš daily log)
```
D/LogRepository: 💾 saveDailyLog() called - userId: abc123, logId: 2025-10-23, date: 2025-10-23
D/LogRepository: 🔥 Calling firestoreService.saveDailyLog() - userId: abc123, logId: 2025-10-23
D/FirestoreService.Android: SAVE_DAILY_LOG_START - userId: abc123, logId: 2025-10-23, path: users/abc123/dailyLogs/2025-10-23, dateEpochDays: 19664
D/FirestoreService.Android: SAVE_DAILY_LOG_DTO - logId: 2025-10-23, dateEpochDays: 19664, createdAt: 1729641600, updatedAt: 1729641600
D/FirestoreService.Android: SAVE_DAILY_LOG_SUCCESS - userId: abc123, logId: 2025-10-23, updatedAt: 1729641600
D/LogRepository: 🔥 firestoreService.saveDailyLog() returned - success: true, latency: 234ms
```

**Pokud nevidíš `saveDailyLog() called`:** Repository se nevolá - problém v UI/ViewModel
**Pokud nevidíš `SAVE_DAILY_LOG_START`:** FirestoreService se nevolá - problém v Repository
**Pokud vidíš `SAVE_DAILY_LOG_ERROR`:** Problém s Firebase - zkontroluj error message

### 4. Load Operation (když načteš daily log)
```
D/FirestoreService.Android: GET_DAILY_LOG_BY_DATE_START - userId: abc123, date: 2025-10-23, dateEpochDays: 19664
D/FirestoreService.Android: GET_DAILY_LOG_BY_DATE_SUCCESS - userId: abc123, logId: 2025-10-23, dateEpochDays: 19664
```

**Pokud vidíš `NOT_FOUND`:** Data nejsou v Firebase - možná se neuložila

## Časté Problémy

### Problém: Vidím "saveDailyLog() called" ale ne "SAVE_DAILY_LOG_START"

**Příčina:** Repository volá FirestoreService, ale metoda se nespustí

**Řešení:**
1. Zkontroluj, že FirestoreService je správně injektovaný v Koin
2. Zkontroluj, že používáš správnou implementaci (ne mock)

```bash
# Hledej v lozích
adb logcat | grep "Koin"
```

### Problém: Vidím "SAVE_DAILY_LOG_SUCCESS" ale data nejsou v Firebase Console

**Příčina:** Možná offline režim nebo persistence issue

**Řešení:**
1. Zkontroluj network connectivity v diagnostic reportu
2. Zkontroluj, že persistence není blokující flush
3. Zkus force refresh Firebase Console
4. Zkontroluj, že jsi ve správném Firebase projektu

```bash
# Zkontroluj network
adb logcat | grep -i "network\|connectivity"
```

### Problém: Vidím "SAVE_DAILY_LOG_ERROR"

**Příčina:** Firebase operace selhala

**Řešení:**
1. Přečti si error message v logu
2. Časté chyby:
   - `PERMISSION_DENIED`: Zkontroluj Firestore rules a auth
   - `UNAVAILABLE`: Network problém
   - `INVALID_ARGUMENT`: Špatná data

```bash
# Hledej error details
adb logcat | grep -A 5 "SAVE_DAILY_LOG_ERROR"
```

### Problém: Nevidím žádné logy

**Příčina:** Logcat není správně filtrovaný nebo app není v debug módu

**Řešení:**
1. Zkontroluj, že app běží:
   ```bash
   adb shell ps | grep eunio
   ```

2. Zkontroluj všechny logy:
   ```bash
   adb logcat | grep -i "eunio"
   ```

3. Zkontroluj, že je to debug build:
   ```bash
   adb shell dumpsys package com.eunio.healthapp.android | grep "versionName"
   ```

## Manuální Test Scenario

1. **Spusť app a počkej na diagnostic report**
2. **Přihlaš se** (pokud nejsi)
3. **Jdi na Daily Logging screen**
4. **Vyber dnešní datum**
5. **Vyplň nějaká data** (např. Period Flow: Medium)
6. **Klikni Save**
7. **Sleduj logy** - měl bys vidět celou sekvenci výše
8. **Otevři Firebase Console** - měl bys vidět dokument
9. **Zkus načíst** - změň datum a vrať se zpět
10. **Sleduj logy** - měl bys vidět GET_DAILY_LOG_BY_DATE

## Export Logů

Pro sdílení nebo analýzu:

```bash
# Export všech relevantních logů do souboru
adb logcat -d | grep -E "(EunioApplication|FirebaseDiagnostics|LogRepository|FirestoreService\.Android)" > android_firebase_logs.txt

# Nebo jen od určitého času
adb logcat -t '10-23 14:30:00.000' | grep -E "(LogRepository|FirestoreService\.Android)" > android_save_logs.txt
```

## Další Kroky

Pokud stále nevidíš data v Firebase:

1. **Spusť integrity test:**
   ```kotlin
   // V kódu nebo přes adb
   val prefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
   prefs.edit().putBoolean("DEBUG_SYNC_TEST", true).apply()
   // Restart app
   ```

2. **Zkontroluj Firestore rules:**
   ```bash
   firebase deploy --only firestore:rules
   ```

3. **Porovnej s iOS:**
   - Ulož log na iOS
   - Zkontroluj Firebase Console
   - Zkus načíst na Android
   - Porovnej strukturu dokumentů
