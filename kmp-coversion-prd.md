# Focus App — Kotlin Multiplatform Implementation Master Prompt

## Project Overview
**Focus** is an offline-first productivity, mood tracking, and journaling app. We're expanding from Android (Jetpack Compose) to iOS using **Kotlin Multiplatform (KMP)** while keeping the existing Android app functional.

### Multiplatform Strategy
- **Android**: Keep existing Jetpack Compose app as-is; will consume shared KMP modules for data/business logic
- **iOS**: Build using KMP for shared business logic; UI can be Compose Multiplatform OR native SwiftUI (decide per feature based on what works best)
- **Shared Layer**: KMP will handle all offline-first sync, persistence, business logic, and data models
- **Distribution**: iOS will be sideloadable via IPA for distribution

---

## Architecture & Tech Stack

### Shared (KMP - `commonMain`)
- **Language**: Kotlin (shared code)
- **Persistence**: SQLite with SQLCipher (AES-256 encryption)
- **Sync Engine**: Custom offline-first sync strategy (conflict resolution, bidirectional sync)
- **Business Logic**: All feature logic (mood tracking, journaling, achievements, quotes, focus guard rules)
- **Network**: Ktor Client for any cloud sync (with graceful offline fallback)
- **DI**: Koin (multiplatform compatible)
- **Serialization**: kotlinx.serialization (JSON)

### Android Platform (`androidMain`)
- **UI Framework**: Jetpack Compose (existing app — don't change)
- **Runtime**: Android Runtime (existing setup)
- **Platform Services**: Biometric API, Play Integrity API, Work Scheduler, Notifications, deep linking
- **Use from KMP**: Data models, SQLite repos, sync engine, business logic classes

### iOS Platform (`iosMain`)
- **UI Framework**: Flexible approach
  - **Option A (Preferred if possible)**: Compose Multiplatform (share UI code with Android)
  - **Option B (Fallback)**: SwiftUI with native iOS UI + KMP for logic/data
  - **Decision Trigger**: If Compose Multiplatform works smoothly on iOS, use it; otherwise, use SwiftUI
- **Runtime**: iOS 14+ (minimum for sideloading)
- **Platform Services**: LocalAuthentication (biometrics), UserNotifications, URLSession for deep linking
- **Build**: XCFramework from KMP shared module; Xcode project for iOS app
- **Sideloading**: Generate unsigned IPA via Xcode build pipeline (or use development provisioning profile)

---

## Features to Implement (In Priority Order)

### Tier 1: Core Offline-First Foundation
1. **Data Models (KMP)**
   - Mood entries (timestamp, mood, intensity, notes)
   - Journal entries (timestamp, title, content, tags, encryption flag)
   - Quote storage (local SQLite, bundled data)
   - Achievements/gamification state
   - Focus Guard rules (blocklist, DND schedules)

2. **SQLite Persistence (KMP)**
   - SQLCipher integration for encrypted storage
   - Repository pattern (MoodRepo, JournalRepo, QuoteRepo, AchievementRepo, FocusGuardRepo)
   - Migration strategy
   - Test helpers for in-memory SQLite

3. **Sync Engine (KMP)**
   - Offline-first conflict resolution
   - Dirty flags + sync queue
   - Bidirectional sync for cloud (optional later)
   - Graceful offline handling

### Tier 2: Authentication & Security (KMP + Platform)
4. **Biometric Authentication**
   - KMP interface: `BiometricProvider`
   - Android: Biometric API + PIN fallback
   - iOS: LocalAuthentication + PIN fallback
   - Private Journal protection

5. **Encryption & Root Detection**
   - SQLCipher (KMP level)
   - Android: Play Integrity, root/Frida detection (androidMain)
   - iOS: device jailbreak detection (iosMain)

### Tier 3: Feature Layer (KMP Business Logic)
6. **Mood Tracking**
   - Quick mood log (emoji/scale input)
   - Mood history & trends (analytics-ready)

7. **Journaling**
   - Rich text editing (handle per-platform)
   - Private mode with biometric lock
   - Tags & search

8. **Quote System**
   - Bundled quote dataset (CSV or JSON in assets)
   - Daily quote delivery
   - Quote rotation logic (round-robin or smart selection)

9. **Gamification & Achievements**
   - Achievement definitions & unlock logic
   - Streak tracking (daily mood logs, journaling)
   - XP/points system

10. **Focus Guard (App Blocker + DND)**
    - Blocklist management (KMP)
    - DND schedule rules (KMP)
    - Android: Work Scheduler + Notification Policy Manager
    - iOS: App Clips or local scheduler (limited by iOS sandbox)

### Tier 4: UI & Platform-Specific (Per Platform)
11. **Theme Switching**
    - Light/Dark mode toggle
    - Circular reveal animation (Android: Canvas; iOS: CABasicAnimation or SwiftUI transitions)

12. **Deep Linking**
    - URI scheme setup (Android & iOS)
    - Route handlers for feature campaigns

13. **Notifications**
    - Scheduled daily reminders
    - Android: NotificationCompat
    - iOS: UserNotifications

14. **Onboarding & Guided Flow**
    - First-launch walkthrough
    - Platform-specific UI

---

## Implementation Phases

### Phase 1: KMP Setup & Shared Modules (Weeks 1–2)
- [ ] Initialize KMP project structure (shared, android, ios)
- [ ] Set up Gradle for multiplatform builds
- [ ] Configure SQLCipher + SQLite integration
- [ ] Create data models & repositories (KMP)
- [ ] Set up Koin DI (multiplatform)
- [ ] Write unit tests for data layer

### Phase 2: Offline-First Sync Engine (Weeks 3–4)
- [ ] Implement sync queue & dirty tracking
- [ ] Build conflict resolution logic
- [ ] Create sync orchestrator
- [ ] Test offline scenarios thoroughly

### Phase 3: Authentication & Security (Weeks 5–6)
- [ ] Define `BiometricProvider` interface (KMP)
- [ ] Implement Android biometric + PIN
- [ ] Implement iOS LocalAuthentication + PIN
- [ ] Integrate SQLCipher encryption
- [ ] Add root/jailbreak detection

### Phase 4: Feature Business Logic (Weeks 7–9)
- [ ] Mood tracking engine (KMP)
- [ ] Journal service (KMP)
- [ ] Quote system & rotation logic (KMP)
- [ ] Achievement unlock logic (KMP)
- [ ] Focus Guard rule engine (KMP)

### Phase 5: iOS UI & Build (Weeks 10–12)
- [ ] Choose UI framework (Compose Multiplatform vs SwiftUI)
- [ ] Build iOS UI screens (mood log, journal, quote display, settings)
- [ ] Implement theme switching & animations (platform-specific)
- [ ] Set up notifications (iOS UserNotifications)
- [ ] Deep linking support
- [ ] Generate XCFramework from KMP shared module
- [ ] Create Xcode project for iOS app

### Phase 6: Android Integration & Testing (Weeks 13–14)
- [ ] Hook existing Jetpack Compose UI to new KMP repos & logic
- [ ] Test bidirectional data flow
- [ ] Verify offline sync works on Android
- [ ] Run full integration tests

### Phase 7: Build & Sideloading (Week 15)
- [ ] Configure iOS development provisioning profile
- [ ] Generate unsigned IPA via Xcode
- [ ] Test sideloading on physical device
- [ ] Document sideloading process

### Phase 8: Polish & Rollout (Week 16+)
- [ ] Performance profiling (both platforms)
- [ ] Bug fixes & edge cases
- [ ] Beta testing with real users
- [ ] Deploy to TestFlight (if moving to official distribution later)

---

## KMP Project Structure

```
focus-kmp/
├── shared/                    # KMP shared module
│   ├── src/commonMain/kotlin/
│   │   ├── data/             # Models, repos, DB
│   │   ├── domain/           # Business logic, use cases
│   │   ├── sync/             # Offline sync engine
│   │   ├── util/             # Common utilities
│   │   └── platform/         # Expect/actual interfaces
│   ├── src/androidMain/kotlin/  # Android-specific (biometric, etc.)
│   ├── src/iosMain/kotlin/      # iOS-specific (LocalAuth, etc.)
│   └── build.gradle.kts
├── android/
│   ├── app/
│   │   ├── src/main/kotlin/   # Existing Compose app (minimal changes)
│   │   └── build.gradle.kts   # Add dependency on shared module
│   └── build.gradle.kts
├── ios/
│   ├── Xcode project (created from XCFramework)
│   ├── Focus/
│   │   ├── ContentView.swift / ComposableView.kt (if using Compose MP)
│   │   └── ...
│   └── Focus.xcodeproj
└── build.gradle.kts           # Root build file
```

---

## Key Implementation Guidelines

### KMP Best Practices
1. **Expect/Actual Pattern**: Use for biometrics, notifications, platform-specific services
   ```kotlin
   // commonMain
   expect interface BiometricProvider { ... }
   
   // androidMain
   actual class AndroidBiometricProvider : BiometricProvider { ... }
   
   // iosMain
   actual class IOSBiometricProvider : BiometricProvider { ... }
   ```

2. **Repository Pattern**: All data access through repos (KMP)
   ```kotlin
   interface MoodRepository {
       suspend fun getMoods(limit: Int): List<Mood>
       suspend fun saveMood(mood: Mood)
       fun observeMoods(): Flow<List<Mood>>
   }
   ```

3. **Coroutines & Flow**: Use for async operations and reactive state (KMP)
   - Share `Flow` objects across platforms
   - Use `StateFlow` for UI state

4. **Testing**: Unit test all KMP logic
   - Mock platform implementations in tests
   - Test offline scenarios explicitly

### Android-Specific
- Keep existing Jetpack Compose UI
- Add dependency on shared KMP module in `build.gradle.kts`
- Use `CombinedContext` or `Dispatchers.Main` for thread switching

### iOS-Specific
- If using **Compose Multiplatform**: Use `UIApplicationDelegateAdaptor` for lifecycle
- If using **SwiftUI**: Map KMP data models to SwiftUI `ObservableObject` wrappers
- For notifications: Use `UserNotificationsManager` to bridge with KMP
- For biometrics: Wrap `LocalAuthentication` in KMP interface
- Test heavily on physical device (simulator can have quirks with encryption)

---

## Testing & QA Checklist

### Unit Tests (KMP)
- [ ] Data model serialization/deserialization
- [ ] Sync conflict resolution logic
- [ ] Achievement unlock conditions
- [ ] Mood analytics calculations

### Integration Tests
- [ ] Offline → Online → Offline transitions
- [ ] Biometric auth flow (mock on tests)
- [ ] SQLCipher encryption/decryption

### Platform Tests
- [ ] Android: Jetpack Compose UI + KMP integration
- [ ] iOS: UI rendering (Compose MP or SwiftUI) + KMP data flow
- [ ] Sideloading: IPA generation & installation

### Manual Testing
- [ ] Create 10+ moods, verify sync
- [ ] Lock journal entry, verify biometric gate
- [ ] Toggle theme → verify animation smoothness
- [ ] Kill app → reopen → verify data persistence
- [ ] Test on both platforms simultaneously

---

## Deliverables

1. ✅ KMP project with shared modules (data, domain, sync)
2. ✅ SQLCipher persistence working on both platforms
3. ✅ Biometric authentication (Android + iOS)
4. ✅ Mood tracking & journaling business logic (KMP)
5. ✅ iOS UI (Compose MP or SwiftUI, your choice)
6. ✅ iOS sideloadable IPA
7. ✅ Android integration with existing Jetpack Compose app
8. ✅ Full test coverage for data/sync layers
9. ✅ Documentation: KMP architecture, build instructions, sideloading guide

---

## Questions for Antigravity (as you progress)

- Do you want Compose Multiplatform or native SwiftUI for iOS? (We'll decide based on feasibility)
- Should we build cloud sync from day one, or keep it offline-only for now?
- Any existing quote dataset, or should we create a bundled CSV?
- Do you want to start with iOS build first, or integrate Android simultaneously?

---

## Notes
- Keep the existing Android app **unchanged** initially; it will consume the shared KMP modules
- iOS will be **sideloadable** (unsigned IPA or dev-signed)
- All **offline-first principles** from the current Android app carry over
- The **quote generation engine** (Ollama-based) stays as a separate tool; we'll integrate quotes into the app
- Use the existing **getfocus.online** website as reference for branding/copy

---

**Status**: Ready for Antigravity to start Phase 1 (KMP Setup & Shared Modules)