# Phase 1 Final Report: Deep APK Research & Product Discovery

## 1. REA Environment Status
- **REA Engine**: Version `6.7.0` (aligned with skill version `42`).
- **Toolchain Status**:
  - Node.js `v24.19.0`, npm `11.17.0`.
  - OpenJDK `17.0.20.1 Temurin` (64-bit Full JDK) and `javac 17.0.20.1`.
  - JADX Headless MCP bridge `v0.7.1` (`jadx-headless-mcp-0.7.1-all.jar`) validated and operational.
  - JADX CLI `v1.5.6` installed at `/usr/local/bin/jadx`.
  - Apktool `v3.0.3` installed at `/usr/local/bin/apktool`.
  - Android Debug Bridge (ADB) `v1.0.39` operational on ARM64 Linux.
- **Environment Remediations**: Successfully resolved PRoot-specific file stat permission variations (`sameRegularFileState`), enabling 100% stable REA target admission and automated decompilation.

---

## 2. APK Identity & Cryptographic Integrity

| Metric | Target Specification |
| :--- | :--- |
| **File Name** | `poweramp-music-player-build-1035-uni.apk` |
| **File Path** | `/workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk` |
| **File Size** | `24,481,502` bytes (~23.35 MB) |
| **SHA-256 Digest** | `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f` |
| **Integrity State** | Unmodified, verified against original baseline |
| **Package Name** | `com.maxmpz.audioplayer` |
| **Version Name / Code** | `build-1035-uni` / `1035009` |
| **SDK Levels** | Min SDK: `21` (Android 5.0) \| Target SDK: `36` (Android 16) \| Compile SDK: `36` |
| **Shared User ID** | `com.maxmpz` (`sharedUserMaxSdkVersion="32"`) |

---

## 3. Analysis Coverage & Component Counts

```
Total Declared Android Components: 99
├── Activities: 66
├── Services: 10
├── Broadcast Receivers: 15
└── Content Providers: 8
```

- **DEX Architecture**: 2 DEX files (`classes.dex` [6.33 MB], `classes2.dex` [0.47 MB]).
- **Class Count**: `6,377` classes total:
  - Proprietary classes (`com.maxmpz.*`): `407`
  - Obfuscated core classes (`p000.*`): `1,244`
  - Third-party libraries (Billing, RuStore, AndroidX, OkHttp): `4,726`
- **Resource Count**: `3,380` decoded resources; `2,287` localized string entries across 31 languages.
- **Native Shared Libraries**:
  - `lib/arm64-v8a/libffmpeg_neon.so` (2.22 MB) & `lib/armeabi-v7a/libffmpeg_neon.so` (2.49 MB)
  - `lib/arm64-v8a/libpowerampcore.so` (4.63 MB) & `lib/armeabi-v7a/libpowerampcore.so` (3.73 MB)

---

## 4. Verified Screen Inventory
**66 distinct activities** were verified via static analysis across 7 categories:
1. **Primary Interface Containers (7)**: `StartupActivity`, `MainActivity`, `PlayerUIActivity`, `ListActivity`, `PlayListActivity`, `EqActivity`, `USBMainActivity`.
2. **Settings Hierarchy (4)**: `preference.SettingsActivity`, `SettingsActivity`, `SettingsActivityManageSpace`, `SettingsLicenseActivity`.
3. **Modal Dialogs & Sheets (22)**: `WelcomeActivity`, `InfoActivity`, `AudioInfoActivity`, `EditTagActivity`, `LyricsActivity`, `SelectAlbumArtActivity`, `EqPresetsActivity`, `AddEqPresetActivity`, `AddReverbPresetActivity`, `EqPresetAssignActivity`, `PlaylistSelectActivity`, `PlaylistShortcutActivity`, `PlaylistSortActivity`, `PlaylistsExportActivity`, `ListOptsActivity`, `AudioBufferActivity`, `EditNameActivity`, `ChangelogActivity`, `AutoFindDialogActivity`, `MusicFoldersActivity`, `GenericDialogActivity`, `AlertDialogActivity`.
4. **Homescreen Widget Configurers (6)**: `Widget4x1Configure`, `Widget4x2Configure`, `Widget2x2Configure`, `Widget4x4Configure`, `Widget4x2ConfigureCover`, `Widget4x4ConfigureCover`.
5. **Dynamic Themed Launcher Aliases (15)**: `rounded_play_inverted_mu`, `classic`, `white_round`, `black_round`, `standard_mu`, etc.
6. **Licensing & Payment Gateways (7)**: `ExpiredActivity`, `APMUnlockDialogActivity`, `HuaweiMigrationActivity`, `RstrActivity`, `PayActivity`, `ProxyBillingActivity`, `ProxyBillingActivityV2`.
7. **External Handlers & Routing (5)**: `VoiceSearchHelperActivity`, `PowerampAPIActivity`, `MediaRouteChooserActivity`, `MediaRouteControllerActivity`, `GoogleApiActivity`.

---

## 5. Verified Feature Inventory (Real Counts)
Out of **70 tracked features** across 8 functional categories:
- **`VERIFIED_STATIC`**: **69 features (98.6%)**
- **`INFERRED`**: **1 feature (1.4% — gesture velocity details)**
- **`UNKNOWN`**: **0 features (0%)**
- **`NOT_FOUND`**: **0 features (0%)**
- **High Feasibility**: **64 features (91.4%)**
- **Moderate Feasibility**: **6 features (8.6%)**

### Feature Category Breakdown
- **Category A (Library Management)**: 12 features (All Songs, Albums, Artists, Multi-Artists, Genres, Folders, FTS4 Search, Multi-Criteria Sort, Favorites, History, Play Counts, Bookmarks).
- **Category B (Audio Playback)**: 14 features (Transport, Independent Queue, Shuffle/Repeat, RMS Waveform Seekbar, Gapless, Crossfade, Speed/Pitch, Sleep Timer, MediaSessionService, Audio Focus, Headset Unplug Safety, Lock Screen, MediaStyle Notification).
- **Category C (Audio Processing & EQ)**: 12 features (10-32 Band Graphic EQ, Full Parametric EQ with continuous Q/Freq, Biquad IIR math, Presets, AutoEq integration, Per-Device Output Binding, Direct Volume Control, Bass & Treble Knobs, Reverb, Balance, Hi-Res AudioTrack, USB DAC mode).
- **Category D (Lyrics System)**: 5 features (Embedded ID3/Vorbis Lyrics, Synced `.lrc` Parsing, External Intent Fallbacks, Local SQLite Caching, Millisecond Offset Adjustment).
- **Category E (Playlist System)**: 7 features (CRUD, Drag Reordering, M3U/M3U8 Export/Import, Position Memory per Playlist, Total Duration, Smart Playlists, Homescreen Shortcuts).
- **Category F (Visualizations)**: 5 features (Milkdrop OpenGL Engine, High-Res Cover Art, FFT Spectrum Analyzer, Animated Blur Background, Fullscreen Immersion).
- **Category G (UI Customization)**: 7 features (Light, Dark, OLED Black skins, Material You Accents, Homescreen Widgets, Dynamic App Icons, Touch Gestures, Rotary Knobs).
- **Category H (Advanced Features)**: 8 features (In-App Tag Editor, Audio Stream Technical Inspector, Android Auto MediaLibraryService, Quick Settings Tile, CUE Sheet virtual track splitting, Shizuku IPC, Settings FTS4 search, Space Management).

---

## 6. Premium Feature Inventory & Monetization Discovery
Poweramp employs an **all-inclusive 15-day trial model**:
- **Full Capabilities Active**: All DSP, Parametric EQ, Hi-Res output, and Milkdrop features are active during the trial.
- **Expiration Trigger**: After 15 days, playback halts, and `dialogs.ExpiredActivity` is presented.
- **Licensing Mechanisms**:
  1. Standalone Unlocker APK (`com.maxmpz.audioplayer.unlock`) via Linux `sharedUserId="com.maxmpz"`.
  2. Google Play In-App Billing Client (`com.android.billingclient.api`).
  3. Huawei AppGallery IAP (`com.maxmpz.audioplayer.huawei`).
  4. RuStore SDK Payment Gateway (`ru.rustore.sdk.pay`).
  5. Direct Website License Key validation (`APMUnlockDialogActivity`).
- **Clean-Room Recommendation**: 100% Free / Open Source Core without artificial paywalls on audio fidelity. If commercialized, standard Google Play In-App Billing Library 7.x should be used without shared-UID dependencies.

---

## 7. Key Technical Findings
1. **Audio Engine**: Powered by a custom 64-bit float native C++ DSP core (`libpowerampcore.so`) bridged via `Pipeline2.java` and JNI.
2. **Decoding Engine**: Powered by a custom ARM NEON-optimized FFmpeg build (`libffmpeg_neon.so`).
3. **Equalizer Math**: Direct-form II transposed biquad IIR filters with real-time coefficient calculations (`Coefs.java`, `Band.java`).
4. **Custom UI Pipeline**: Uses a pre-Compose immediate-mode canvas/scenegraph engine (`FastLayout`, `SceneFastLayout`) and custom buses (`MsgBus`, `StateBus`).
5. **Database Architecture**: SQLite database named `folders.db` with 251 manual schema migrations (`p000/ar.java`, `p000/po0.java`), featuring FTS4 virtual tables for instant search and relational multi-artist splitting.
6. **CUE & Waveform Integration**: Virtual track splitting via CUE sheets with start offsets (`offset_ms`) and pre-calculated RMS audio amplitude waveforms stored as BLOBs in the database.

---

## 8. Independent Architecture Recommendation
A modern, production-grade native Android stack is specified:
- **Language**: Kotlin 2.0+ with Coroutines & Flow.
- **UI Framework**: Jetpack Compose + Material 3 (Material You dynamic theming).
- **Audio Engine**: AndroidX Media3 (ExoPlayer) with custom 32-bit floating-point `AudioSink` + optional Oboe C++ DSP extension.
- **Background Service**: AndroidX Media3 `MediaSessionService` with unified `MediaStyle` notifications and auto-handling of audio focus.
- **Database**: AndroidX Room 2.6+ with SQLite WAL and FTS5 full-text search.
- **Preferences**: Jetpack DataStore (Type-Safe Preferences).
- **Dependency Injection**: Google Hilt.
- **Image Loading**: Coil (Compose extension).
- **Media Ingestion**: Dual pipeline: `MediaStore.Audio` reactive ContentObserver + Storage Access Framework (SAF) WorkManager scanner.

---

## 9. Major Risks & Mitigation Strategies
1. **OEM Audio Resampling Inconsistency**: Mitigated by adopting standard 32-bit float PCM via `AudioTrack` as baseline (broadly supported across modern Android) with vendor direct output flags as optional toggles.
2. **Android 11+ Scoped Storage Restrictions**: Mitigated by utilizing official Storage Access Framework (SAF) directory tree permissions (`ACTION_OPEN_DOCUMENT_TREE`) rather than Google Play-restricted `MANAGE_EXTERNAL_STORAGE`.
3. **Android 14+ Foreground Service Mandates**: Mitigated by declaring `android:foregroundServiceType="mediaPlayback"` on `PlaybackService` coupled with an active `MediaSession`.
4. **LGPL/GPL Licensing Compliance**: FFmpeg decoding components must be dynamically linked and compiled with `--disable-gpl` to adhere strictly to LGPL v2.1.

---

## 10. Remaining Unknowns
1. **OEM-Specific Direct PCM DAC Whitelists**: Vendor-specific internal direct output flags for legacy HTC/LG Quad-DAC devices are proprietary (mitigated by using universal Android 8.0+ AAudio/AudioTrack float).
2. **ProjectM Custom Shader Bindings**: Exact GLSL vertex/fragment shader variations used by Poweramp's custom Milkdrop fork (mitigated by adopting the standard open-source `projectM` library).

---

## 11. Complete Documentation Index

All research artifacts have been generated in the project workspace:

- [00-ENVIRONMENT-READINESS.md](file:///workspace/poweramp-clone/research/00-ENVIRONMENT-READINESS.md) — Preflight audit, engine readiness, and PRoot stat patch.
- [01-APK-FORENSIC-REPORT.md](file:///workspace/poweramp-clone/research/01-APK-FORENSIC-REPORT.md) — SHA-256 verification, manifest analysis, native libraries, and permissions.
- [02-REA-INVESTIGATION-LOG.md](file:///workspace/poweramp-clone/research/02-REA-INVESTIGATION-LOG.md) — Sequential REA tool call ledger and parameters.
- [03-TECH-STACK-ANALYSIS.md](file:///workspace/poweramp-clone/research/03-TECH-STACK-ANALYSIS.md) — Deep comparison of original vs independent modern stack.
- [04-FEATURE-INVENTORY.md](file:///workspace/poweramp-clone/research/04-FEATURE-INVENTORY.md) — 70-feature catalog across categories A through H with evidence.
- [05-SCREEN-INVENTORY.md](file:///workspace/poweramp-clone/research/05-SCREEN-INVENTORY.md) — Complete 66-activity component catalog and intent filters.
- [06-NAVIGATION-FLOW.md](file:///workspace/poweramp-clone/research/06-NAVIGATION-FLOW.md) — Mermaid state diagrams, player-library transitions, and gestures.
- [07-PREMIUM-FEATURE-INVENTORY.md](file:///workspace/poweramp-clone/research/07-PREMIUM-FEATURE-INVENTORY.md) — 15-day trial mechanism, unlocker IPC, and billing channels.
- [08-CODE-PATH-ANALYSIS.md](file:///workspace/poweramp-clone/research/08-CODE-PATH-ANALYSIS.md) — Playback service, Pipeline2 JNI, biquad EQ, scanner, and DB paths.
- [09-RUNTIME-OBSERVATIONS.md](file:///workspace/poweramp-clone/research/09-RUNTIME-OBSERVATIONS.md) — ADB device probe findings and headless environment limits.
- [10-INDEPENDENT-ARCHITECTURE.md](file:///workspace/poweramp-clone/research/10-INDEPENDENT-ARCHITECTURE.md) — Clean-room multi-module architecture, DSP math, and Room ERD.
- [11-PRODUCT-REQUIREMENTS.md](file:///workspace/poweramp-clone/research/11-PRODUCT-REQUIREMENTS.md) — Product requirements document (PRD) with MoSCoW prioritization.
- [12-RISK-AND-LICENSING.md](file:///workspace/poweramp-clone/research/12-RISK-AND-LICENSING.md) — Clean-room IP boundaries, LGPL compliance, and Play Store policies.
- [13-IMPLEMENTATION-ROADMAP.md](file:///workspace/poweramp-clone/research/13-IMPLEMENTATION-ROADMAP.md) — Six-phase execution roadmap from UI/UX to release.
- [14-EVIDENCE-MATRIX.md](file:///workspace/poweramp-clone/research/14-EVIDENCE-MATRIX.md) — Comprehensive 70-feature traceability matrix.

---

## 12. Recommended Phase 2 Tasks
Upon explicit user approval, Phase 2 (UI/UX Design & Scaffolding) will begin:
1. Define the Jetpack Compose Material 3 Design System (typography, icons, dark/OLED color schemes).
2. Wireframe the Fullscreen Player Screen (album art, waveform seekbar, controls, sheet transitions).
3. Wireframe the Parametric Equalizer Canvas (frequency response curve, interactive filter nodes, tone knobs).
4. Wireframe the Synchronized Lyrics View (karaoke-style active line highlighting, manual timing offset).
5. Scaffolding the multi-module Android Gradle project structure (`:core:audio`, `:core:database`, `:core:ui`, `:feature:player`, etc.).

---

## 13. Phase Boundary Enforcement
**Phase 1 is complete.** No production code or builds have been initiated. Standing by for user authorization to proceed to **Phase 2: UI/UX Design**.
