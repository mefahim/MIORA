# MIORA Next — Baseline Workspace & Git Audit

**Audit Date:** 2026-11-12  
**Target Repository:** `https://github.com/mefahim/MIORA.git`  
**Workspace Path:** `/workspace/miora-v0-2`  
**Auditor:** MIORA Lead Android Architect & Systems Auditor  

---

## 1. Repository Topology & Branch Status

### 1.1 Working Directory & Git Roots
- **Absolute Repository Root:** `/workspace/miora-v0-2`
- **Active Git Remote:**
  ```
  origin  https://github.com/mefahim/MIORA.git (fetch)
  origin  https://github.com/mefahim/MIORA.git (push)
  ```
- **Remote Branches Detected:** `origin/main`
- **Local Branches Detected:** `main`, `v0.2-dev`, `docs/miora-next-audit-roadmap`

### 1.2 Remote Baseline (`origin/main`)
- **Commit Hash:** `03fed47c2a1d295f2b354ce7911c61b16177a19d`
- **Commit Message:** `feat(website): launch official MIORA v0.1.0 stable release landing page`
- **Author:** `Md. Faisal Hossen Fahim <62790080+mefahim@users.noreply.github.com>`
- **Date:** `Wed Nov 11 18:24:12 2026 +0000`
- **Baseline Version:** `0.1.0` (Version Code: 3)
- **Baseline License:** Apache License 2.0 (`LICENSE` file)
- **Baseline Test Results:** 164 automated unit tests across 22 test suites (100% passing).

### 1.3 Local Development Head (`v0.2-dev`)
- **Commit Hash:** `97c4cf7e761dfa9d94943f6c8ef8ca67417036d3`
- **Commit Message:** `chore(release): complete Phase M6 final certification and release bump to v0.2.0`
- **Status:** **Ahead of `origin/main` by 6 commits** (Never pushed to remote).
- **Commit Sequence Ahead of `main`:**
  1. `d66cfc5` — `feat(v0.2): complete Milestone M0 architecture audit, reference analysis and GPL-3.0 licensing transition`
  2. `a649982` — `feat(ui,domain): complete Phase M1 UI/UX redesign and decoupled online domain`
  3. `abdc162` — `feat(online,search): complete Phase M2 guest discovery, YouTube search, and playback routing`
  4. `3744d16` — `feat(auth,sync): complete Phase M3 OAuth PKCE and Phase M4 YouTube library sync`
  5. `f1e08bb` — `feat(lyrics,playback): complete Phase M5 LRCLIB synced lyrics and smart playback preview`
  6. `97c4cf7` — `chore(release): complete Phase M6 final certification and release bump to v0.2.0`
- **Working Tree Cleanliness:** Clean. All modifications on `v0.2-dev` are committed.

### 1.4 Divergence Analysis (`origin/main` vs `v0.2-dev`)
```
$ git diff origin/main..v0.2-dev --stat
 70 files changed, 12608 insertions(+), 574 deletions(-)
```
Key modifications introduced on `v0.2-dev`:
- Complete GPL-3.0 license replacement in `LICENSE` and creation of `THIRD_PARTY_NOTICES/`.
- Domain separation into `model/local/LocalModels.kt` (`LocalTrack` with `id: Long`) and `model/online/OnlineModels.kt` (`OnlineTrack` with `id: String`).
- Unified presentation layer via `model/presentation/MediaItemPresentation.kt`.
- Schema v3 in `data/db/MioraDatabaseHelper.kt` adding 5 online tables.
- Zero-dependency network & JSON engine: `data/network/MioraJson.kt` and `data/network/YouTubeDataApiClient.kt`.
- Google OAuth 2.0 PKCE and Keystore encryption: `auth/OAuthManager.kt` and `auth/EncryptedTokenManager.kt`.
- YouTube personal library sync: `data/sync/YouTubeLibrarySyncCoordinator.kt`.
- Synchronized lyrics client: `data/lyrics/LrcLibLyricsClient.kt`.
- Jetpack Compose UI additions: `ModeSelector.kt`, `ModernTrackRow.kt`, `FilterChipBar.kt`, `QuickPicksGrid.kt`, `AccountConnectScreen.kt`, `SyncedLibraryScreen.kt`, `OnlinePlaylistDetailScreen.kt`, `OnlineSearchScreen.kt`, `OnlinePlayerPreviewCard.kt`.
- Four new automated test suites (`PhaseM1ArchitectureAndUiTest`, `PhaseM2DiscoveryAndSearchTest`, `PhaseM3OAuthAndSecurityTest`, `PhaseM5LyricsAndPlaybackTest`) expanding tests from 164 to 197.

---

## 2. Build Toolchain & System Environment

| Component | Target Version | Installed Environment Path / Verification | Notes |
|---|---|---|---|
| **Gradle** | `8.14.3` | `/opt/gradle/gradle-8.14.3/bin/gradle` | Bundled in PocketDev container |
| **JDK** | `17.0.20.1+1` | `/usr/local/bin/java`, `/opt/jdk-17.0.20.1+1` | Java 17 toolchain required by AGP |
| **Android SDK Build Tools** | `35.0.0` | `/root/android-sdk/build-tools/35.0.0` | AAPT2, apksigner, zipalign |
| **Android SDK Platforms** | `android-35` / `android-34` | `/root/android-sdk/platforms` | Min SDK: 26, Compile: 35, Target: 34 |
| **Android Gradle Plugin (AGP)**| `8.5.2` | `gradle/libs.versions.toml` (`agp = "8.5.2"`) | Defined in catalog |
| **Kotlin** | `2.0.21` | `gradle/libs.versions.toml` (`kotlin = "2.0.21"`) | Uses Compose Compiler plugin |
| **Compose BOM** | `2024.09.00` | `libs.androidx.compose.bom` | Material 3 1.3.0, Foundation 1.7.0 |
| **Media3 (ExoPlayer)** | `1.4.1` | `libs.androidx.media3.*` | `exoplayer`, `session`, `common` |
| **Security Crypto** | `1.1.0-alpha06` | Handled via Android Keystore cipher in `auth/` | Hardware-backed keystore integration |

---

## 3. Application Identity & Security Configuration

### 3.1 Application Identity
- **Application ID:** `com.fhf.miora` (Verified across `app/build.gradle.kts`, `AndroidManifest.xml`, AAPT2 dumps).
- **App Name:** `MIORA`
- **Version Code (Baseline `main`):** `3`
- **Version Name (Baseline `main`):** `0.1.0`
- **Version Code (`v0.2-dev`):** `4`
- **Version Name (`v0.2-dev`):** `0.2.0`

### 3.2 Signing Configuration (`app/build.gradle.kts`)
- **Release Signing:** Configured to read from environment variables:
  `MIORA_KEYSTORE_PATH`, `MIORA_KEYSTORE_PASSWORD`, `MIORA_KEY_ALIAS`, `MIORA_KEY_PASSWORD`.
- **Reproducible Fallback:** If environment variables are absent, falls back to `~/.android/debug.keystore` with credentials `android` / `androiddebugkey`.
- **Debug Signing:** Uses standard debug keystore.
- **Minification:** `isMinifyEnabled = false` for debug and standard builds; ProGuard rules defined in `proguard-rules.pro`.

### 3.3 Android Manifest & Permissions (`AndroidManifest.xml`)
- **Network Security:** `android:usesCleartextTraffic="false"` strictly enforced. Cleartext HTTP is prohibited.
- **Declared Permissions:**
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.FOREGROUND_SERVICE`
  - `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `android.permission.POST_NOTIFICATIONS` (Android 13+)
  - `android.permission.READ_MEDIA_AUDIO` (Android 13+)
  - `android.permission.READ_EXTERNAL_STORAGE` (Max SDK: 32)
  - `android.permission.WAKE_LOCK`
- **Intent Filters:**
  - Main Launcher: `android.intent.action.MAIN` / `android.intent.category.LAUNCHER`
  - Deep Link OAuth Redirects:
    - Scheme: `miora://oauth2redirect`
    - Scheme: `com.fhf.miora:/oauth2redirect`

---

## 4. Database Architecture & Schema Evolution

Managed by unified open helper: `com.fahim.powerampclone.data.db.MioraDatabaseHelper`.  
Database File: `miora_playlists.db`.

```
========================================================================================
                          MIORA DATABASE SCHEMA EVOLUTION
========================================================================================

  [SCHEMA V1] Baseline Playlist System (MIORA v0.1.0-beta.1)
  --------------------------------------------------------------------------------------
  - playlists            (id INTEGER PK AUTOINCREMENT, name TEXT UNIQUE, created_at)
  - playlist_tracks     (id INTEGER PK, playlist_id FK, track_id INTEGER, position)
  - favorites           (track_id INTEGER PK, created_at)
  - playback_history    (id INTEGER PK, track_id INTEGER, played_at)

  [SCHEMA V2] High-Performance Local MediaStore Cache (MIORA v0.1.0 Stable)
  --------------------------------------------------------------------------------------
  - tracks              (id INTEGER PK, media_store_id, stable_hash TEXT UNIQUE,
                         uri, data_path, title, artist, album, duration_ms,
                         year, genre, bitrate_kbps, sample_rate_hz, lyrics_raw)
  - scan_metadata       (key TEXT PK, value TEXT)
  - Indexes:            idx_tracks_path, idx_tracks_artist, idx_tracks_hash

  [SCHEMA V3] Isolated Online Domain & Cache Architecture (MIORA v0.2.0 Dev)
  --------------------------------------------------------------------------------------
  - online_search_cache        (query TEXT, category TEXT, result_json TEXT,
                                expires_at INTEGER, PRIMARY KEY(query, category))
  - online_synced_playlists    (id TEXT PK, title TEXT, description TEXT, track_count)
  - online_synced_tracks       (id TEXT PK, playlist_id FK, title TEXT, artist TEXT,
                                video_id TEXT, position INTEGER, synced_at INTEGER)
  - online_account_metadata    (key TEXT PK, value TEXT, updated_at INTEGER)
  - online_lyrics_cache        (lookup_key TEXT PK, track_title, artist_name,
                                plain_lyrics, synced_lyrics, cached_at)
========================================================================================
```

**Migration Integrity:**
- All migrations (`onUpgrade`) use `IF NOT EXISTS` constructs.
- Migrating from v1 -> v2 or v2 -> v3 preserves 100% of user data with zero data loss.
- Foreign keys enabled via `PRAGMA foreign_keys = ON;`.

---

## 5. Playback Service & Audio Engine Architecture

### 5.1 Local Audio Engine
- **Service:** `com.fahim.powerampclone.playback.PlaybackService` extending Android `Service` with `ForegroundServiceType.MEDIA_PLAYBACK`.
- **Engine Core:** `Media3AudioEngine` wrapping AndroidX Media3 `ExoPlayer 1.4.1`.
- **Audio Attributes:** `C.USAGE_MEDIA` and `C.CONTENT_TYPE_MUSIC` with automatic audio focus handling (ducking, transient pauses on phone calls).
- **Becoming Noisy:** Broadcast receiver unregisters and pauses playback upon headphone disconnection.
- **DSP & Equalizer:** `EqualizerController` applying 10-band software biquad filters or Android system `Equalizer` effect, with AutoEQ presets.

### 5.2 Online Playback State (Current v0.2 Dev Status)
- In the current `v0.2-dev` workspace, online tracks resolve through `PlaybackRouter.kt`.
- `PlaybackRouter` creates an Android Intent targeting the official YouTube Music application (`vnd.youtube.music://watch?v=...`) or standard YouTube (`vnd.youtube:...`), with a web browser fallback.
- **Architectural Gap:** While compliant, this is an **external handoff**, NOT an integrated in-app streaming player. In-app background playback, continuous queue mixing between local and online tracks, and in-app offline caching for online audio do not yet exist in the codebase.

---

## 6. Automated Test Suite Inventory

Total Suites: **26** | Total Tests: **197** | Pass Rate: **100%** (0 Failures, 0 Errors).

| Category | Suite Name | Test Count | Scope |
|---|---|:---:|---|
| **Audio & DSP** | `AdvancedDspPipelineTest` | 7 | Biquad band gains, clamps, AutoEQ curve matching |
| | `EqualizerSystemTest` | 7 | 10-band equalizer initialization, preset switching |
| | `Media3AudioEngineTest` | 8 | ExoPlayer initialization, state transitions, audio focus |
| **Local Playback** | `FullPlaybackRegressionTest` | 17 | Queue navigation, shuffle/repeat, gapless progression |
| | `PlaybackManagerTest` | 9 | Audio engine coordination, track completion callbacks |
| | `PlayerViewModelTest` | 9 | StateFlow emissions, seekbar updates, lyric offset |
| | `ForegroundServiceStartupRegressionTest` | 8 | Android 14 foreground service compliance |
| **Media & Scan** | `LocalMediaAggregatorTest` | 5 | MediaStore scanning, album/artist grouping |
| | `TrackStoreTest` | 6 | Persistent track storage and cache validation |
| | `DatabaseArchitectureAndMigrationTest` | 3 | SQLite schema v1/v2 constants and migration |
| | `PlaylistSystemTest` | 14 | SQLite playlist CRUD, track reordering, batch delete |
| **UI & Lyrics** | `ArtworkAndPaletteTest` | 5 | DynamicPalette extraction and fallback accents |
| | `InteractivePlayerUiTest` | 4 | MiniPlayer gestures, playback controls |
| | `LibrarySearchTest` | 6 | Local track filtering, debounce search |
| | `LibraryDataIntegrationTest` | 4 | Single source of truth song count derivation |
| | `SyncedLyricsAndOffsetTest` | 6 | Local LRC parsing, offset storage |
| | `LrcParserTest` | 6 | LRC timestamp precision and sibling file discovery |
| **Phase 6 Master**| `Phase6FullRegressionTest` | 16 | Cross-subsystem integration, identity verification |
| | `Phase6StressAndPerformanceTest` | 5 | 50,000 synthetic track lookups, 10,000 queue items |
| | `FinalPolishRegressionTest` | 5 | UI/UX consistency, mock data elimination |
| | `UpdateSystemTest` | 8 | GitHub release SemVer parsing, SHA-256 APK check |
| | `WidgetAndMediaLibraryTest` | 6 | Home widget intents and MediaBrowser service |
| **Online v0.2** | `PhaseM1ArchitectureAndUiTest` | 7 | Decoupled domain models, ModeSelector state, Schema v3 |
| | `PhaseM2DiscoveryAndSearchTest` | 10 | YouTube Data API JSON, 24h search cache, debouncing |
| | `PhaseM3OAuthAndSecurityTest` | 8 | RFC 7636 PKCE S256 challenge, AES-256-GCM Keystore |
| | `PhaseM5LyricsAndPlaybackTest` | 8 | LRCLIB client parsing, fallback actions, state flows |

---

## 7. Stable v0.1.0 Recovery Path

The stable v0.1.0 production release is completely recoverable at any time:
1. **Git Tag:** `v0.1.0` (pointed at commit `83ea7c2`, ancestor of `origin/main` at `03fed47`).
2. **Recovery Procedure:**
   ```bash
   git checkout v0.1.0
   # Verify clean build:
   gradle assembleDebug
   ```
3. **Distribution Assets:**
   - Pre-built binary: `MIORA-v0.1.0.apk`
   - Checksum: `MIORA-v0.1.0.apk.sha256` (`2fe958326f49537760c47437bbaadfc3627a14fb879ae4ea52d14ddcdaa47381`)
   - Documentation in `docs/` (`FINAL_RELEASE_VERIFICATION.md`, `FINAL_UI_UX_AUDIT.md`, `PHASE_6_COMPLETION_REPORT.md`).
4. **Conclusion:** Zero risk to v0.1.0 stability. The stable release remains immutable on GitHub `main` and can be built reproducibly.
