# Focus App — Kotlin Multiplatform (KMP) Architecture & Build Guide

## 1. Overview
The Focus app is engineered with an **offline-first Kotlin Multiplatform (KMP)** architecture. The shared layer houses all core domain models, persistence (SQLDelight/SQLite with encryption), offline-first sync engine, biometric authentication, and business logic. Both Android and iOS consume this shared engine.

```
┌────────────────────────────────────────────────────────┐
│                   Focus Client Apps                    │
├───────────────────────────┬────────────────────────────┤
│    Android Platform       │        iOS Platform        │
│  (Jetpack Compose UI)     │    (SwiftUI / Compose MP)  │
├───────────────────────────┴────────────────────────────┤
│              Shared KMP Layer (:shared)                │
│ ┌────────────────────────────────────────────────────┐ │
│ │  Domain: StreakCalculator, MoodAnalytics, Quotes   │ │
│ │  Data: Repositories, SQLDelight DB, Models         │ │
│ │  Sync: SyncQueue, ConflictResolver, Orchestrator   │ │
│ │  Security: BiometricProvider, SecurityValidator    │ │
│ │  DI: Koin Multiplatform                            │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 2. Directory Structure

- `shared/`
  - `src/commonMain/kotlin/com/focus/`
    - `data/local/`: Database driver interfaces & database factory.
    - `data/repository/`: `MoodRepository`, `JournalRepository`, `PrivateJournalRepository`, `QuoteRepository`, `AchievementRepository`, `FocusGuardRepository`.
    - `domain/`: `StreakCalculator`, `MoodAnalyticsEngine`, `QuoteRotationEngine`, `FocusGuardRuleEngine`.
    - `model/`: `MoodEntry`, `JournalEntry`, `PrivateJournalEntry`, `Quote`, `Achievement`, `FocusSession`, `FocusGuardRule`, `DndSchedule`, `BackupModel`.
    - `sync/`: `SyncQueue`, `SyncItem`, `ConflictResolver`, `SyncOrchestrator`.
    - `security/`: `BiometricProvider`, `SecurityValidator`.
    - `di/`: `Koin.kt` dependency injection.
  - `src/commonMain/sqldelight/com/focus/database/FocusDatabase.sq`: All SQLite schemas and queries.
  - `src/androidMain/`: Android-specific drivers, Android BiometricManager, root detection, and Koin platform module.
  - `src/iosMain/`: iOS native drivers, LocalAuthentication, jailbreak detection, and iOS Koin module.
  - `src/commonTest/`: Comprehensive unit and integration test suites.
- `app/`: Existing Android Jetpack Compose application depending on `:shared`.
- `ios/`: iOS project containing SwiftUI views, `Info.plist`, and sideloadable IPA build script.

---

## 3. Build & Test Commands

### Run Shared KMP Tests:
```bash
./gradlew :shared:testDebugUnitTest
```

### Build Android App:
```bash
./gradlew :app:assembleDebug
```

### Build Shared iOS XCFramework:
```bash
./gradlew :shared:assembleSharedFocusXCFramework
```

### Build Sideloadable iOS IPA (on macOS with Xcode):
```bash
chmod +x ios/build_ipa.sh
./ios/build_ipa.sh
```

---

## 4. Sideloading Guide for iOS

1. **Prerequisites**:
   - Transfer `Focus_sideload.ipa` from `ios/build/Focus_sideload.ipa` to your computer or iPhone.
   - Sideloading tool installed: **AltStore**, **Sideloadly**, or **TrollStore**.

2. **Installation via AltStore / Sideloadly**:
   - Connect iPhone to computer or connect over Wi-Fi.
   - Open Sideloadly or AltServer, select `Focus_sideload.ipa`, enter your Apple ID.
   - Click **Start** to install onto your device.

3. **Trusting Developer Profile on iPhone**:
   - Go to **Settings > General > VPN & Device Management**.
   - Select your Apple ID under Developer App.
   - Tap **Trust "[Your Apple ID]"** to launch the Focus app.
