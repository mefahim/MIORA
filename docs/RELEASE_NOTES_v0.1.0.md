# 🎵 MIORA v0.1.0 — Stable Release

We are excited to announce the first stable release of **MIORA** (Music in Our Real Audio), the modern, audiophile-grade native Android music player built with 100% clean-room engineering.

---

### 🚀 Highlights in v0.1.0

- **Official Stable Identity (`com.fhf.miora`)**: Reached production milestone across Gradle, AndroidX Media3 playback services, FileProvider authorities, and AAPT2 badging.
- **First-Launch Permission Onboarding**: Introducing a smooth, reassuring 4-step wizard guiding users through Welcome, Local Audio Storage Permission (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`), Notification Access (`POST_NOTIFICATIONS` on Android 13+), and an automatic initial local media scan.
- **Dynamic Artwork Theming & Accent Fallback**: Real-time palette extraction now smoothly harmonizes player backgrounds, waveform scrubbers, and transport controls with album artwork. When dynamic color is disabled, surfaces instantly fall back to the user's selected accent (`Ember`, `Ocean`, `Forest`, `Rose`, `Violet`, `Gold`, `Monochrome`).
- **Dynamic Track Count — Single Source of Truth**: Song and playlist counters across Library, Settings Profile, Scanner, and Playlist details are now unified directly with the persistent repository, eliminating all hardcoded counts and sample track fallbacks in production.
- **UI & UX Refinement**:
  - Removed all obsolete Phase 2 preview, simulated playback, and sample library disclaimers across Player, Equalizer, Dialogs, and Search screens.
  - Eliminated the redundant bottom waveform strip in the player interface, focusing playback navigation on the interactive seekbar.
  - Resolved duplicate vertical overflow menu buttons on playlist track rows.
  - Redesigned the empty playlist action into a comfortable, touch-friendly primary button.
  - Enabled multi-line text wrapping for long file paths in Track Info & Details dialogs to eliminate visual clipping.
- **Exhaustive Quality Assurance**: 164 automated unit, stress, and regression tests passing 100% across 22 test suites.

---

### 📦 Release Assets & Cryptographic Checksums

| Asset | Size | SHA-256 Checksum |
|:---|:---:|:---|
| **MIORA-v0.1.0.apk** | 20.51 MB (21,508,520 bytes) | `2fe958326f49537760c47437bbaadfc3627a14fb879ae4ea52d14ddcdaa47381` |
| **MIORA-v0.1.0.apk.sha256** | 85 bytes | `2fe958326f49537760c47437bbaadfc3627a14fb879ae4ea52d14ddcdaa47381` |

---

### 📱 System Requirements

- **Minimum Android Version**: Android 8.0 Oreo (API level 26)
- **Target Android Version**: Android 14 (API level 34)
- **Architectures Supported**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

---

*MIORA is an independent clean-room project licensed under Apache 2.0.*
