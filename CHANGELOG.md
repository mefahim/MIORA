# Changelog

All notable changes to **MIORA** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-11-11

### 🚀 Highlights
- **First Stable Release (`v0.1.0`)**: Production-ready official release of MIORA (Music in Our Real Audio), target application ID `com.fhf.miora`.
- **First-Launch Permission Onboarding**: Introduced a native 4-step wizard guiding new users through Welcome, Audio Library Permission (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`), Notification Access (`POST_NOTIFICATIONS` for Android 13+), and an automatic initial local media scan.
- **Dynamic Artwork Theming & Accent Fallback**: Implemented real-time dynamic palette extraction from album art with automatic, contrast-tested fallback to user-selected accent themes (`Ember`, `Ocean`, `Forest`, `Rose`, `Violet`, `Gold`, `Monochrome`) whenever dynamic color is disabled.
- **Dynamic Song Count — Single Source of Truth**: Harmonized song and playlist counts across the main Library, Settings Profile, Music Scanner, and Playlist details, completely removing hardcoded counts and sample track fallbacks in production.
- **UI/UX Polish & Layout Refinements**:
  - Removed all obsolete Phase 2 preview, simulated playback, and sample library disclaimers across Player, Equalizer, Dialogs, and Search screens.
  - Eliminated the redundant bottom waveform strip in the player interface, focusing playback navigation on the interactive seekbar.
  - Fixed duplicate vertical overflow menu buttons on playlist track rows.
  - Replaced cramped three-column buttons on empty playlists with a clean, touch-friendly "Add Tracks" primary action.
  - Enabled multi-line text wrapping for long file paths in Track Info & Details dialogs to eliminate visual clipping.
- **Comprehensive Quality Assurance**: 164 automated unit, stress, and regression tests passing 100% across 22 test suites.

### Added
- `OnboardingScreen.kt`: 4-step onboarding flow for permission granting and library initialization.
- `FinalPolishRegressionTest.kt`: Unit tests verifying dynamic count consistency, mock decoupling, and accent palette fallbacks.
- Documentation:
  - `docs/FINAL_UI_UX_AUDIT.md`: Pre-release audit of real-device findings and visual defects.
  - `docs/FINAL_UI_UX_FIX_REPORT.md`: Comprehensive report of UI/UX corrections.
  - `docs/FINAL_PERMISSION_ONBOARDING_REPORT.md`: Architecture and implementation details of permission onboarding.
  - `docs/FINAL_DATA_CONSISTENCY_REPORT.md`: Single source of truth count derivation report.
  - `docs/FINAL_DYNAMIC_COLOR_VERIFICATION.md`: Verification of dynamic palette extraction and fallback.
  - `docs/FINAL_RELEASE_VERIFICATION.md`: End-to-end verification and gate validation for `v0.1.0`.

---

## [0.1.0-beta.2] - 2026-11-11

### 🚀 Highlights
- **Application ID Migration**: Successfully migrated official application identity to `com.fhf.miora` across Gradle config, Service intent actions, FileProvider authorities, and AAPT2 badging.
- **Phase 6 Master QA, Stress & Security Verification**: 159 automated unit and regression tests passing 100% across 21 test suites.
- **High-Volume 50k Track Scalability**: Validated sub-millisecond lookups and rapid search across 50,000 synthetic tracks, alongside 10,000 item playback queue navigation.
- **Security & R8 Hardening**: Enforced least-privilege Android 14 permissions, cryptographic update fingerprint verification, restricted FileProvider cache paths, and comprehensive ProGuard rules.

### Added
- `Phase6FullRegressionTest.kt`: 16 cross-subsystem regression tests covering identity, playback state transitions, queue edge cases, lyrics offsets, and hardware DSP clamps.
- `Phase6StressAndPerformanceTest.kt`: 5 high-volume performance benchmarks validating 50k track operations, 10k queue transitions, 2k LRC line parsing, and 1,000 DSP parameter mutations.
- `app/proguard-rules.pro`: Hardened keep rules for Coroutines, Media3, Domain Models, FileProvider, and SQLite.
- Documentation:
  - `docs/PHASE_6_0_BASELINE_AUDIT.md`: Baseline identity and action namespace migration report.
  - `docs/PHASE_6_1_REGRESSION_QA_REPORT.md`: Full regression QA validation report (154 tests).
  - `docs/PHASE_6_2_STRESS_PERFORMANCE_REPORT.md`: High-volume stress & scalability benchmark report.
  - `docs/PHASE_6_3_SECURITY_AUDIT_REPORT.md`: Security, permissions, and manifest audit report.
  - `docs/PHASE_6_COMPLETION_REPORT.md`: Master Phase 6 completion and release acceptance report.

---

## [0.1.0-beta.1] - 2026-11-10

### 🚀 Highlights
- **Initial Public Beta Release**: Official launch of the MIORA native music player ecosystem.
- **Native In-App Update Engine**: Automatic update checks, stable/beta channel selection, background downloads, SHA-256 integrity verification, and system package installation via `FileProvider`.
- **MediaStore Audio Engine & Background Service**: Complete AndroidX foreground service playback with audio focus, lockscreen media controls, and status bar transport controls.
- **Dynamic Palette & Artwork Engine**: Real-time color extraction from album art or wallpaper with contrast-compliant fallback palettes.
- **Persistent SQLite Playlist System**: High-performance SQLite database managing custom playlist creation, track ordering, deduplication, and bulk batch operations.
- **72 Automated Unit Tests**: Comprehensive test suite achieving 100% pass rate.

### Added
- **In-App Updater**:
  - `UpdateModels.kt`: SemVer 2.0.0 parsing with precedence rules and channel configuration.
  - `UpdateChecker.kt`: GitHub REST API client with channel filtering (Stable vs. Beta) and rate-limit handling.
  - `UpdateDownloadManager.kt`: Streaming file downloader with real-time SHA-256 calculation and verification.
  - `UpdateInstaller.kt`: Preflight APK validation (`packageManager.getPackageArchiveInfo`), Android 8.0+ unknown sources verification, and `FileProvider` intent generator.
  - `UpdateDialog.kt`: Material 3 modal dialog showing release notes, download progress bar, and installation trigger.
  - `SettingsScreen.kt`: Dedicated "App Updates" section with version display, channel selector, and manual update trigger.
- **Automated CI/CD**:
  - `docs/WORKFLOW_RELEASE.md`: Automated build, test, checksum generation, and GitHub release pipeline specification.
- **Documentation**:
  - `docs/ARCHITECTURE.md`: Complete system architecture and subsystem relationships.
  - `docs/IN_APP_UPDATES.md`: Technical deep dive into the update verification flow.
  - `docs/RELEASING.md`: Step-by-step procedures for building and publishing future releases.
  - `docs/SECURITY.md`: Security policies and private vulnerability reporting guidelines.
  - `CONTRIBUTING.md`: Contributor guidelines and clean-room development rules.

### Changed
- Repository renamed to `mefahim/MIORA`.
- Application version updated to `versionCode = 2`, `versionName = "0.1.0-beta.1"`.
- Configured distribution signing with reproducible fallback and debug-to-release certificate migration path.
