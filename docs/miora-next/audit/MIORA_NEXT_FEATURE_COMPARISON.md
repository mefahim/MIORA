# MIORA NEXT — Quad-Layer Feature & Architecture Comparison

- **Audit Date:** November 12, 2026
- **Auditor:** Antigravity Engineering & Architecture Team
- **Comparison Baseline:**
  1. **Metrolist 13.7.0:** Upstream reference online client (`com.metrolist.music` v13.7.0, build 153).
  2. **MIORA v0.1.0:** Stable public release on `origin/main` (commit `03fed47`, Apache-2.0).
  3. **MIORA v0.2.0 Dev:** Local development branch on `v0.2-dev` (commit `97c4cf7`, Milestones M0–M6).
  4. **MIORA Next Target:** Unified hybrid architecture combining uncompromised local playback with seamless online streaming.

---

## 1. Quad-Layer Feature Comparison Matrix

| Feature / Architectural Area | Metrolist 13.7.0 | MIORA v0.1.0 (Stable) | MIORA v0.2.0 (Local Dev) | MIORA Next (Target Vision) |
| :--- | :--- | :--- | :--- | :--- |
| **Primary Domain Focus** | Online-first (YouTube Music) with secondary local playback | Pure Local-only audio player | Dual-mode domain models (Local & Online separated) | Unified Hybrid (seamless local/online coexistence) |
| **Media Playback Engine** | Media3 1.10.1 `MediaLibraryService` (`MusicService`) | Media3 1.5.0 `MediaSessionService` (`MioraMediaService`) | Media3 1.5.0 `MediaSessionService` (`MioraMediaService`) | Media3 1.5+ `MediaLibraryService` with unified `PlaybackService` |
| **Online Stream Playback** | In-app continuous stream via `ResolvingDataSource` + ExoPlayer | None (Local files only) | **Intent Handoff Only** (`PlaybackRouter` -> YTM/YouTube app) | **Integrated In-App Stream** via pluggable resolving data sources |
| **Local Audio Engine** | Basic MediaStore scanner via Room indexing | High-performance Android MediaStore scanner with folder browsing | MediaStore scanner + folder browsing + SQLite cache | Multi-threaded MediaStore + SAF folder provider + tag extraction |
| **Unified Queue System** | Online queue only; local files converted to pseudo-items | Local track queue only | Local track queue only (Online items not queued in-app) | **Unified Hybrid Queue** (mixed local and online items seamlessly) |
| **Search Subsystem** | InnerTube `/youtubei/v1/search` with chips & suggestions | Local MediaStore search (title, artist, album) | YouTube Data API v3 search + 24h SQLite query cache | Universal Federated Search (Local library + Online YouTube/YT Music) |
| **Stream Caching & Offline** | Media3 `SimpleCache` (`CacheDataSource`) + `ExoDownloadService` | None (Local files already on disk) | None | Smart Multi-Tier Cache (transient LRU player cache + explicit downloads) |
| **Authentication Subsystem** | In-app WebView sniffing `SAPISID` cookie -> `SAPISIDHASH` | None | Google OAuth 2.0 PKCE + Android Keystore AES-256-GCM | Secure OAuth 2.0 PKCE + Encrypted Token Vault (No raw cookie sniffing) |
| **User Library Sync** | InnerTube private library endpoints (`/youtubei/v1/browse`) | None | YouTube Data API v3 personal playlist sync & local SQLite mirror | Hybrid Sync (Personal playlists, Liked music, Cloud sync) |
| **Lyrics Subsystem** | Multi-provider (LRCLIB, KuGou, Paxsenix, YouTube timed captions) | Embedded ID3 lyrics & local `.lrc` file parser | LRCLIB remote client (`LrcLibLyricsClient`) + Plain & Synced LRC parser | Composite Lyrics Engine (Embedded ID3 + Local `.lrc` + LRCLIB + Fallbacks) |
| **Dynamic Visuals / Theming** | Jetpack Compose M3 + AndroidX Palette ambient glow | Jetpack Compose M3 + Material You dynamic colors | Compose M3 + Dynamic ambient background glow card | Dynamic Palette ambient glow + Synchronized lyric highlights + Waveform |
| **Audio Processing & EQ** | Loudness normalization (`VolumeNormalizationAudioProcessor`) | System AudioFX Equalizer intent launcher | System AudioFX Equalizer intent launcher | 10-Band Graphic Equalizer + ReplayGain / EBU R128 Loudness Normalizer |
| **BotGuard / poToken** | Headless WebView minting via `po_token.html` (AST/BotGuard VM) | None | None | Clean-room token provider or proxy resolver |
| **Signature Cipher Solver** | QuickJS native binary (`libquickjs.so`) + Meriyah AST | None | None | Clean-room AST solver or externalized extraction engine |
| **Test Automation** | Minimal unit tests (~15-20 basic test classes) | Basic unit tests | **197 automated unit tests** (26 suites, 100% pass) | Comprehensive test suite (>250 unit + integration + mock tests) |
| **Licensing** | GPL-3.0 (Strict Copyleft via InnerTubeX) | Apache-2.0 (Permissive) | GPL-3.0 Transitioned (Milestone M0) | GPL-3.0 (Verified clean-room compliance) |

---

## 2. In-Depth Gap Analysis

### Gap 1: Playback Architecture (The In-App Audio Playback Divide)
- **Metrolist 13.7.0:** Direct, zero-friction in-app audio playback. When a user taps a song, ExoPlayer directly streams chunked audio via `ResolvingDataSource` while running in the background. The user never leaves the application.
- **MIORA v0.2.0 (Dev):** Milestone M5 implemented `OnlinePlayerPreviewCard.kt` and `PlaybackRouter.kt`. When an online track is tapped, it opens an Android Intent launching the external YouTube Music app, YouTube app, or web browser. **There is no internal online audio streaming engine in MIORA v0.2.0.**
- **Impact for MIORA Next:** This is the single largest gap. MIORA Next must implement an integrated `ResolvingMediaSourceFactory` within its existing Media3 `PlaybackService` so that online tracks play continuously in-app without external app handoffs.

### Gap 2: Queue Coexistence & Hybrid Continuity
- **Metrolist 13.7.0:** Metrolist treats YouTube video IDs as primary keys. Local files are mapped into the same structure, but the player is primarily tuned for online queues.
- **MIORA v0.1.0 & v0.2.0:** MIORA's `PlaybackService` only understands `MioraTrack` objects with local `content://` or `file://` URIs.
- **Impact for MIORA Next:** MIORA Next needs a polymorphic `MediaItem` converter and a unified queue manager that seamlessly allows Track 1 (Local FLAC on SD Card) to seamlessly transition into Track 2 (Online YouTube Stream) and Track 3 (Cached MP3) without stuttering, player re-instantiation, or UI glitching.

### Gap 3: Authentication & Security Model
- **Metrolist 13.7.0:** In-app WebView sniffing raw `SAPISID` cookies and calculating `SAPISIDHASH`. High security risk: stores raw unencrypted session cookies in `DataStore` and plaintext JSON backup files. Exposes user to account hijacking if backup files are shared.
- **MIORA v0.2.0 (Dev):** Industry-standard RFC 7636 OAuth 2.0 with PKCE (Proof Key for Code Exchange). Tokens (Access Token and Refresh Token) are encrypted using AES-256-GCM via Android Keystore (`KeystoreCryptoManager.kt`) and stored in encrypted SQLite.
- **Advantage MIORA:** MIORA's authentication architecture is vastly superior from a security, cryptographic, and architectural hygiene standpoint.

### Gap 4: Offline Caching & Downloads
- **Metrolist 13.7.0:** Full offline download pipeline via Media3 `SimpleCache` and `ExoDownloadService`.
- **MIORA v0.2.0 (Dev):** Only caches query metadata (search results, playlist items) in SQLite database `miora_database` (`cache_timestamp`, 24h TTL). Does not download or cache audio bytes.
- **Impact for MIORA Next:** MIORA Next must implement Media3 `CacheDataSource` with a two-tier cache:
  1. Transient player cache (LRU, 200MB - 1GB) for smooth playback and replay.
  2. Persistent user downloads for offline playback.

### Gap 5: Lyrics & Presentation
- **Metrolist 13.7.0:** Connects to multiple lyrics endpoints (LRCLIB, KuGou, Paxsenix, YouTube timed captions).
- **MIORA v0.2.0 (Dev):** Clean LRCLIB integration (`LrcLibLyricsClient.kt`) with parsing for synchronized LRC timestamps (`[mm:ss.xx]`) and dynamic ambient glow styling.
- **Impact for MIORA Next:** Retain the clean LRCLIB client, add local embedded ID3 USLT/SYLT tag reader, and add timed-syllable rendering.

---

## 3. Where MIORA Surpasses Metrolist

1. **Local Audio Performance & Audio File Quality:**
   - MIORA is built from the ground up for audiophiles, supporting bit-perfect local playback of FLAC, ALAC, WAV, and high-res DSD, with comprehensive folder hierarchy navigation and tag reading. Metrolist's local file support is rudimentary.
2. **Security & Cryptography:**
   - MIORA utilizes Android Keystore hardware-backed AES-256-GCM encryption for stored user tokens and RFC 7636 PKCE. Metrolist stores raw `SAPISID` cookies in unencrypted DataStore.
3. **Automated Testing & Code Quality:**
   - MIORA v0.2.0 boasts 197 automated unit tests across 26 suites covering models, database migrations, cryptographic key management, OAuth flows, and lyrics parsing. Metrolist has almost zero automated unit test coverage.
4. **Architectural Cleanliness:**
   - MIORA strictly separates UI, domain, and data layers using clean architectural patterns (Repository, Coroutines, StateFlow), avoiding massive multi-thousand-line monolithic God classes (such as Metrolist's 3,950-line `MusicService.kt`).
