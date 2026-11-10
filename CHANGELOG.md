# Changelog

All notable changes to **MIORA** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
  - `.github/workflows/release.yml`: Automated build, test, checksum generation, and GitHub release publication on `v*` tag push.
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
