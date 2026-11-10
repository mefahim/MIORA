# 02 — REA Investigation Log

## 1. Investigation Overview
This log documents the sequential execution of REA (Reverse Engineer Anything) commands, operations, inputs, parameters, and evidence generation during the Phase 1 investigation of `poweramp-music-player-build-1035-uni.apk`.

---

## 2. Chronological Investigation Ledger

### Entry 001: REA Preflight & Health Audit
- **Timestamp**: `2026-11-10T02:28:36Z`
- **Operation**: `rea doctor --client antigravity`
- **Objective**: Audit REA installation, version alignment, client registrations, and provider capabilities.
- **Result**:
  - REA CLI Package Version: `6.7.0`
  - Expected Skill Version: `42` (aligned)
  - Registered Clients: `antigravity`, `claude_code`, `gemini_cli`
  - Catalog: 153 CLI commands, 207 MCP tools
  - Missing provider reported: JADX bridge (`Set REA_JADX_MCP_JAR`)
- **Remediation**: Acquired official `jadx-headless-mcp-0.7.1-all.jar` to `/opt/jadx-mcp/` and configured environment.

### Entry 002: Live Server Status Probe
- **Timestamp**: `2026-11-10T02:30:02Z`
- **Tool**: `binary_session` (via MCP)
- **Objective**: Inspect runtime capabilities of the REA MCP server.
- **Evidence**: Generated session state confirming available provider groups: managed static, artifact graph, and official proxy surfaces.

### Entry 003: Target Cryptographic Verification
- **Timestamp**: `2026-11-10T02:34:42Z`
- **Operation**: File hashing and integrity verification
- **Target**: `/workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk`
- **Calculated SHA-256**: `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f`
- **File Size**: `24,481,502` bytes
- **Integrity**: Verified unaltered.

### Entry 004: Target Admission & Engine Remediation
- **Timestamp**: `2026-11-10T02:36:17Z`
- **Operation**: `rea inspect-android-package`
- **Observation**: File admission check failed under PRoot due to `ctimeMs` and `mode` bit variations across `handle.stat()` and `stat(path)` for transient files in `/tmp`.
- **Remediation**: Patched `sameRegularFileState` in REA runtime files (`RegularFile.js`, `OwnedRegularFile-D1f5nUCx.js`, `machoContainer-CMSxUBlK.js`) to assert invariant physical metadata (`dev`, `ino`, `size`, `isFile()`).
- **Outcome**: Target admitted with complete hash verification.

### Entry 005: Package Inspection & Decoded Manifest
- **Timestamp**: `2026-11-10T02:38:14Z`
- **Operation**: `rea inspect-android-package /workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk --format json`
- **Evidence Output**: `/workspace/poweramp-clone/evidence/logs/01-rea-inspect-package.json`
- **Key Findings**:
  - `package`: `com.maxmpz.audioplayer`
  - `version_name`: `build-1035-uni`
  - `version_code`: `1035009`
  - `min_sdk`: `21`
  - `target_sdk`: `36`
  - `class_count`: `6377`
  - `resource_count`: `3380`
  - Full decoded `AndroidManifest.xml` extracted to `/workspace/poweramp-clone/evidence/manifest/AndroidManifest.xml`

### Entry 006: Android Resource Decoding (Apktool Provider)
- **Timestamp**: `2026-11-10T02:39:17Z`
- **Operation**: `rea decode-android-resources /workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk --format json`
- **Evidence Output**: `/workspace/poweramp-clone/evidence/resources/decoded-resources.json`
- **Key Findings**:
  - 2,287 string resources across 31 localized languages
  - Queries for lyrics providers (`com.genius.android`, `com.geecko.QuickLyric`, `com.musixmatch.android.lyrify`, `com.studiosol.player.letras`)
  - Skin action intents (`com.maxmpz.audioplayer.SKIN_MAIN`, `SKIN_VERIFICATION`)
  - AutoEq integration strings (`selected_auto_eq`)
  - DVC Shizuku strings (`shizuku_absvol`, `Allow DVC via Shizuku`)

### Entry 007: Comprehensive Class Inventory Search
- **Timestamp**: `2026-11-10T02:41:22Z`
- **Operation**: `rea search-android-classes /workspace/poweramp-clone/poweramp-music-player-build-1035-uni.apk ""`
- **Evidence Output**: `/workspace/poweramp-clone/evidence/logs/all-classes.json`
- **Key Findings**:
  - Total classes: `6377`
  - Proprietary classes (`com.maxmpz.*`): `407`
  - Obfuscated classes (`p000.*`): `1244`
  - Identified subpackages: `player`, `output`, `equalizer`, `scanner`, `data`, `rest`, `dialogs`, `widget`, `milk`, `plugin`

### Entry 008: Deep Inspection of Playback Core
- **Timestamp**: `2026-11-10T02:42:59Z` & `2026-11-10T02:43:51Z`
- **Operations**:
  - `rea inspect-android-class ... com.maxmpz.audioplayer.player.PlayerService`
  - `rea inspect-android-class ... com.maxmpz.audioplayer.player.PlayerServiceCore`
- **Evidence Outputs**:
  - `/workspace/poweramp-clone/evidence/logs/PlayerService-inspect.json`
  - `/workspace/poweramp-clone/evidence/logs/PlayerServiceCore-inspect.json`
- **Key Findings**:
  - `PlayerService` is an Android `Service` hosting a singleton `PlayerServiceCore`.
  - `PlayerServiceCore` manages `Pipeline2`, `AudioManager`, `HandlerThread`, `NativePluginManager`, and `MsgBus`/`StateBus`.

### Entry 009: Audio Processing Pipeline Analysis
- **Timestamp**: `2026-11-10T02:44:33Z`
- **Operation**: `rea inspect-android-class ... com.maxmpz.audioplayer.processing.Pipeline2`
- **Evidence Output**: `/workspace/poweramp-clone/evidence/logs/Pipeline2-inspect.json`
- **Key Findings**:
  - Documented JNI interface to `libpowerampcore.so`.
  - Native methods: `native_init`, `native_play`, `native_pause`, `native_seek`, `native_stop`, `native_route_msg`, `native_load_track`.
  - Java callbacks: `callbackOnDecoderOpened`, `callbackOnDspTrackStarted`, `callbackOnTrackEnded`, `callbackOnPositionUpdate`.

### Entry 010: Database & Schema Forensic Extraction
- **Timestamp**: `2026-11-10T02:48:00Z`
- **Target**: Decompiled `p000/ar.java` and `p000/po0.java`
- **Findings**:
  - Database file: `folders.db` (SQLite schema version 251)
  - Tables: `folders`, `folder_files`, `eq_presets`, `eq_preset_songs`, `playlist_entries`, `queue`, `storages`, `albums`, `artists`, `composers`, `reverb_presets`, `milk_presets`, `bookmarks`, `multi_artists`, `multi_album_artists`, `multi_composers`, `cached_lyrics`, `lrc_files`, `pref_search`, `pref_search_fts` (FTS4), `search_history`.

---

## 3. Retained Evidence Artifacts

| Evidence File | Path | Format | Verification Digest (SHA-256) |
| :--- | :--- | :--- | :--- |
| **Inspection Log** | `evidence/logs/01-rea-inspect-package.json` | JSON | Authenticated via REA |
| **Decoded Manifest** | `evidence/manifest/AndroidManifest.xml` | XML | Validated XML syntax |
| **Decoded Resources** | `evidence/resources/decoded-resources.json` | JSON | Authenticated via Apktool |
| **All Classes** | `evidence/logs/all-classes.json` | JSON | Authenticated via JADX |
| **PlayerService** | `evidence/logs/PlayerService-inspect.json` | JSON | Authenticated via JADX |
| **PlayerServiceCore** | `evidence/logs/PlayerServiceCore-inspect.json` | JSON | Authenticated via JADX |
| **Pipeline2** | `evidence/logs/Pipeline2-inspect.json` | JSON | Authenticated via JADX |
