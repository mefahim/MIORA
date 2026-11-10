# Phase 1 & 1.1 Final Research Validation & Baseline Specification Report

## 1. Actual REA Environment Status
- **REA CLI Engine**: Version `6.7.0` (`/usr/local/lib/nodejs/lib/node_modules/rea-agents/scripts/rea.mjs`).
- **REA Skill**: Version `42` (`/root/.gemini/config/skills/reverse-engineer-anything/SKILL.md`), 207 analysis tools.
- **Node.js & npm**: Node.js `v24.19.0`, npm `11.17.0`.
- **JDK Toolchain**: OpenJDK `17.0.20.1 Temurin` (64-Bit Full JDK) with `javac 17.0.20.1`.
- **JADX Headless MCP**: Bridge `v0.7.1` (`/opt/jadx-mcp/jadx-headless-mcp-0.7.1-all.jar`, SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`).
- **JADX CLI**: Version `1.5.6` (`/usr/local/bin/jadx`).
- **Apktool**: Version `3.0.3` (`/usr/local/bin/apktool`).
- **ADB**: Version `1.0.39` (`/usr/bin/adb`), `procps-ng 3.3.16` process inspection.
- **REA MCP Connectivity**: Verified via live MCP tool invocation `inspect_android_package` (Evidence ID: `ev_eb9da26d9168eaca1b1871ff31e9bd2f0999488392b15c88c608f3b85b961246`).
- **PRoot Invariant Hardening**: Reversible patch documented in [`research/patches/rea-proot-stat.patch`](research/patches/rea-proot-stat.patch) and managed via [`research/patches/rea-patch-manager.sh`](research/patches/rea-patch-manager.sh).

---

## 2. APK Identity & Cryptographic Integrity

| Metric | Target Specification |
| :--- | :--- |
| **File Name** | `poweramp-music-player-build-1035-uni.apk` |
| **File Path** | `/workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk` |
| **File Size** | `24,481,502` bytes (~23.35 MB) |
| **SHA-256 Digest** | `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f` |
| **Integrity State** | 100% Unmodified, cryptographic baseline verified |
| **Package Name** | `com.maxmpz.audioplayer` |
| **Version Name / Code** | `build-1035-uni` / `1035009` |
| **SDK Levels** | Min SDK: `21` (Android 5.0) \| Target SDK: `36` (Android 16) \| Compile SDK: `36` |
| **Shared User ID** | `com.maxmpz` (`sharedUserMaxSdkVersion="32"`) |

---

## 3. Static Investigation Coverage

```
Declared Android Components: 99
├── Activities: 66
├── Services: 10
├── Broadcast Receivers: 15
└── Content Providers: 8
```

- **DEX Codebase**: 2 DEX files (`classes.dex` [6.33 MB], `classes2.dex` [0.47 MB]).
- **Class Analysis**: `6,377` classes total:
  - Proprietary classes (`com.maxmpz.*`): `407`
  - Core obfuscated classes (`p000.*`): `1,244`
  - Third-party SDKs (Billing, RuStore, AndroidX, OkHttp): `4,726`
- **Resource Decompilation**: `3,380` resources; `2,287` localized string entries across 31 languages.
- **Native Shared Libraries**:
  - `libffmpeg_neon.so` (arm64-v8a: 2.22 MB; armeabi-v7a: 2.49 MB)
  - `libpowerampcore.so` (arm64-v8a: 4.63 MB; armeabi-v7a: 3.73 MB)

---

## 4. Runtime Investigation Coverage & Test Plan
- **Hardware Audit**: Probed ADB socket (`tcp:5037`) and local emulator ports (`5555`). 0 devices attached (`error: no devices/emulators found`).
- **Runtime Execution**: Explicitly documented as **BLOCKED (0% Runtime Verified)** due to headless PRoot container constraints.
- **Integrity Enforcement**: In compliance with Phase 1.1 instructions, no runtime logs or screenshots were fabricated.
- **Reproducible Test Plan**: Comprehensive 7-suite manual test plan established in [`research/09-RUNTIME-OBSERVATIONS.md`](research/09-RUNTIME-OBSERVATIONS.md) covering first-run onboarding, library FTS indexing, waveform scrubbing, parametric EQ, LRC synchronization, audio focus, and licensing expiration.

---

## 5. Hardened Feature Classification Counts

Every feature was audited across four distinct dimensions (DECLARATION, IMPLEMENTATION, EXECUTION, OUTCOME) in [`research/15-FEATURE-AUDIT-VALIDATION.md`](research/15-FEATURE-AUDIT-VALIDATION.md) and [`research/14-EVIDENCE-MATRIX.md`](research/14-EVIDENCE-MATRIX.md):

| Hardened Classification | Feature Count | Percentage | Definition |
| :--- | :--- | :--- | :--- |
| **`STATIC_IMPLEMENTATION_CONFIRMED`** | **13** | **18.6%** | Low-level native JNI/C++, Biquad DSP math, or shader pipelines completely confirmed in bytecode. |
| **`IMPLEMENTED_BUT_RUNTIME_UNVERIFIED`** | **55** | **78.6%** | Declared and implemented in Java/Kotlin bytecode, providers, and schemas; runtime execution unverified on physical hardware. |
| **`PARTIALLY_IMPLEMENTED`** | **1** | **1.4%** | C.04 AutoEq online database search (string present, query API requires clarification). |
| **`INFERRED`** | **1** | **1.4%** | G.06 Touch swipe gesture velocity thresholds (deduced from view touch listener structures). |
| **`RUNTIME_VERIFIED`** | **0** | **0.0%** | Strictly enforced; no live device was connected. |
| **Total Features Audited** | **70** | **100%** | **64 High Feasibility (91.4%) / 6 Moderate Feasibility (8.6%)** |

---

## 6. Screen Classification Counts (66 Activities)

The 66 declared Activities are categorized into 6 functional classifications in [`research/05-SCREEN-INVENTORY.md`](research/05-SCREEN-INVENTORY.md):

| Component Classification | Count | Representative Activities |
| :--- | :--- | :--- |
| **`USER_FACING_SCREEN`** | **8** | `MainActivity`, `PlayerUIActivity`, `ListActivity`, `PlayListActivity`, `EqActivity`, `USBMainActivity`, `preference.SettingsActivity`, `SettingsActivity` |
| **`DIALOG_OR_SHEET`** | **22** | `WelcomeActivity`, `MusicFoldersActivity`, `InfoActivity`, `AudioInfoActivity`, `EditTagActivity`, `LyricsActivity`, `SelectAlbumArtActivity`, `EqPresetsActivity`, `AddEqPresetActivity`, `AddReverbPresetActivity`, `EqPresetAssignActivity`, `PlaylistSelectActivity`, `PlaylistShortcutActivity`, `PlaylistSortActivity`, `PlaylistsExportActivity`, `ListOptsActivity`, `AudioBufferActivity`, `EditNameActivity`, `ChangelogActivity`, `AutoFindDialogActivity`, `GenericDialogActivity`, `AlertDialogActivity` |
| **`WIDGET_CONFIGURATION`** | **6** | `Widget4x1Configure`, `Widget4x2Configure`, `Widget2x2Configure`, `Widget4x4Configure`, `Widget4x2ConfigureCover`, `Widget4x4ConfigureCover` |
| **`PAYMENT_OR_LICENSING`** | **8** | `SettingsLicenseActivity`, `ExpiredActivity`, `APMUnlockDialogActivity`, `HuaweiMigrationActivity`, `RstrActivity`, `PayActivity`, `ProxyBillingActivity`, `ProxyBillingActivityV2` |
| **`THIRD_PARTY_INTEGRATION`** | **5** | `VoiceSearchHelperActivity`, `PowerampAPIActivity`, `MediaRouteChooserActivity`, `MediaRouteControllerActivity`, `GoogleApiActivity` |
| **`INTERNAL_ACTIVITY`** | **17** | `StartupActivity` (trampoline), `SettingsActivityManageSpace` (OS hook), 15 dynamic themed launcher aliases (`white_round`, `black_round`, `classic`, etc.) |
| **Total Declared Activities** | **66** | **Every component accounted for with explicit intent mappings** |

---

## 7. Important Verified Code Paths

All 12 prioritized subsystems are reverse-engineered and documented in [`research/08-CODE-PATH-ANALYSIS.md`](research/08-CODE-PATH-ANALYSIS.md):
1. **PlayerService**: Android 14+ foreground service with `mediaPlayback` type delegating to `PlayerServiceCore`.
2. **PlayerServiceCore**: Central state machine implementing `OnAudioFocusChangeListener`, `MsgBus`, and `StateBus`.
3. **Pipeline2 JNI Bridge**: Lock-free control gateway exposing native transport methods and C++ audio callbacks.
4. **Audio Decoding**: FFmpeg NEON decoders in `libffmpeg_neon.so` converting streams into uncompressed float PCM buffers.
5. **Native DSP Invocation**: Floating-point processing loop in `libpowerampcore.so` (64-bit double filter coefficients and 32-bit float PCM buffers providing scale-invariant intermediate arithmetic to prevent digital clipping).
6. **Equalizer & Biquad Mathematics**: `Band.java` and `Coefs.java` evaluating Robert Bristow-Johnson Audio EQ Cookbook IIR transfer functions.
7. **AudioTrack / AAudio Output**: `AthdPluginService.java` routing 32-bit float PCM (`AudioFormat.ENCODING_PCM_FLOAT`) up to 384kHz.
8. **MediaSession & Notification**: `MediaSessionCompat` synchronizing transport actions, lock screen metadata, and MediaStyle notification.
9. **Database & Playlist Persistence**: SQLite WAL database `folders.db` (v251, `p000/ar.java`, `po0.java`) with FTS4 full-text search (`unicode61`) and RMS waveform BLOBs.
10. **Media Scanning & SAF**: Dual-engine scanner combining raw POSIX `dirent` directory traversal with recursive SAF tree resolution.
11. **Lyrics Processing**: Four-tier resolution hierarchy (local cache -> sidecar `.lrc` -> embedded ID3 tags -> external app intents).
12. **Crossfade & Gapless**: Codec delay sample trimming and early trigger callback (`native_set_end_event_position_ms`) mixing two PCM streams with equal-power gain ramps.

---

## 8. Audio Architecture Evaluation & Trade-Offs

Evaluated in detail in [`research/10-INDEPENDENT-ARCHITECTURE.md`](research/10-INDEPENDENT-ARCHITECTURE.md):
- **Architecture A (Media3-First)**: High compatibility, lowest development complexity, but lacks parametric EQ and scale-invariant float headroom; routes through standard audio mixing paths subject to OS resampling.
- **Architecture B (Hybrid Float DSP — RECOMMENDED)**: AndroidX Media3 decoding paired with custom 32-bit float PCM `AudioProcessor` / `AudioSink` and high-resolution `AudioTrack` output. Delivers audiophile parametric EQ, prevents internal clipping, supports gapless transitions, clean-room independence, and broad device compatibility (`AudioTrack` float supported since API 21).
- **Architecture C (Fully Custom Native C++ / Oboe)**: Low-latency target potential and direct USB host control, but fragile across vendor OEM HALs and carries extreme NDK maintenance overhead.

> [!NOTE]
> **Audiophile Validation Requirement**: Float PCM provides internal digital headroom against clipping, but is NOT automatically "bit-perfect". Bit-perfect reproduction requires establishing that the complete source-to-DAC path is unmodified and unmixed, with DSP effects disabled. The clean-room engine must query supported sample rates via `AudioDeviceInfo` and document resampling behavior when conversion is necessary.

---

## 9. Premium Feature & Licensing Discovery
- **Trial Mechanism**: 15-day all-inclusive trial period with full DSP, parametric EQ, and Hi-Res output active.
- **Enforcement**: Upon day 16, playback halts and redirects to `dialogs.ExpiredActivity`.
- **Validation Channels**: Standalone unlocker APK via Linux `sharedUserId="com.maxmpz"`, Google Play Billing Client, Huawei AppGallery IAP, RuStore SDK, and direct website license keys.
- **Clean-Room Recommendation**: Core audiophile player released 100% Free and Open Source without artificial paywalls. If commercialized, standard Google Play In-App Billing Library 7.x should be utilized without shared-UID dependencies.

---

## 10. Licensing, Privacy & Security Risks
Documented in [`research/12-RISK-AND-LICENSING.md`](research/12-RISK-AND-LICENSING.md):
1. **FFmpeg Compliance**: Must be compiled strictly under **LGPL v2.1** with dynamic linking, zero GPL/non-free dependency contamination, full corresponding source distribution, and user replacement mechanisms per LGPL Section 6.
2. **Storage Permission Policy**: Poweramp's request for `MANAGE_EXTERNAL_STORAGE` is rejected by Google Play for music players. Our independent implementation uses Android 13+ `READ_MEDIA_AUDIO` + Storage Access Framework (SAF) `ACTION_OPEN_DOCUMENT_TREE` for recursive directory access.
3. **Privacy Assurance**: 100% offline-first architecture with zero third-party telemetry, trackers, or advertising SDKs.
4. **Items for Professional Legal Review**: Jurisdictional review of audio codec patent pools and trademark clearance for the new application brand identity.

---

## 11. Unresolved Technical Questions
1. **Hardware Direct Output Routing on Android 14/15**: Evaluating OEM-specific direct output flag acceptance across diverse Android HALs (e.g. Qualcomm, MediaTek) without root, and verifying when the audio server grants non-resampled routing.
2. **Milkdrop Preset Shaders**: Feasibility of compiling complex Milkdrop `.milk` preset equation shaders into Vulkan / OpenGL ES 3.0 via projectM vs modern WebGL/Canvas compute shaders.

---

## 12. Recommended Phase 2 Implementation Tasks

When Phase 2 is authorized by the project owner:
1. **Design System & Tokens**: Compose Material 3 theme palette, OLED Black (#000000), dynamic color tokens, and custom Canvas typography.
2. **Interactive UI Wireframes & Custom Views**:
   - Fullscreen Player (Album art carousel, RMS waveform seeker, sliding mini-player).
   - Parametric Equalizer Canvas (Interactive touch curve with draggable frequency/Q nodes, rotary knobs).
   - Fast-scrolling Library (Multi-column category grid, alphabetical fast-scroller thumb).
   - Synchronized Lyrics View (Karaoke-style line highlighting, manual timing offset calibration).
3. **Multi-Module Gradle Scaffolding**: Setup modular project structure (`:core:audio`, `:core:database`, `:core:scanner`, `:core:ui`, `:feature:player`, etc.) adhering to PocketDev offline build rules.
