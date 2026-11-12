# MIORA NEXT — Master Phased Implementation Roadmap (M0 through M9)

- **Audit & Roadmap Date:** November 12, 2026
- **Auditor & Lead Architect:** Antigravity Engineering Team
- **Milestone Scope:** M0 through M9 (From Architectural Baseline to v1.0.0 Production Release)

---

## 1. Roadmap Overview & Status Summary

| Milestone | Title | Focus Area | Status | Unit Tests | Commit Reference |
| :---: | :--- | :--- | :---: | :---: | :---: |
| **M0** | License Transition & Architecture Audit | GPL-3.0 relicensing, project baseline audit | **COMPLETE** | — | `d66cfc5` |
| **M1** | Domain Decoupling & Schema v3 | `MioraTrack` / `OnlineTrack`, ModeSelector | **COMPLETE** | 45 | `a649982` |
| **M2** | Online Search & Query Caching | YouTube Data API v3, SQLite 24h cache | **COMPLETE** | 35 | `abdc162` |
| **M3/M4**| Secure Auth & Library Sync | OAuth 2.0 PKCE, Keystore AES-GCM, Playlists | **COMPLETE** | 48 | `3744d16` |
| **M5** | Synchronized Lyrics & Ambient Glow | LRCLIB client, Dynamic Glow, Intent Handoff | **COMPLETE** | 34 | `f1e08bb` |
| **M6** | Test Hardening & v0.2.0 SemVer Bump | Full suite verification (197 tests), Docs | **COMPLETE** | 35 | `97c4cf7` |
| **M7** | Integrated In-App Stream Engine | Media3 ResolvingDataSource, Stream Resolver | **PLANNED** | ~40 | Phase Gate 1 |
| **M8** | Unified Queue & Offline Stream Cache | Mixed local/online queue, SimpleCache, DL | **PLANNED** | ~35 | Phase Gate 2 |
| **M9** | Equalizer, Polish & v1.0.0 Release | Volume normalizer, 10-band EQ, CI/CD | **PLANNED** | ~30 | Phase Gate 3 |

---

## 2. Completed Milestones Breakdown (M0 – M6)

### Milestone M0: GPL-3.0 Transition & Architecture Audit
- **Objective:** Establish the legal and structural foundation for integrating open-source streaming technologies.
- **Key Artifacts:** `LICENSE` updated to GNU General Public License v3.0, clean-room audit documentation.
- **Commit:** `d66cfc5`

### Milestone M1: Decoupled Domain Models & SQLite Schema v3
- **Objective:** Separate local and online domain entities, preventing schema corruption and model coupling.
- **Key Artifacts:** `OnlineModels.kt`, `OnlineDatabaseHelper.kt`, `ModeSelector.kt`, SQLite Schema v3 migrations.
- **Commit:** `a649982`

### Milestone M2: YouTube Search & Query Caching
- **Objective:** Fast, quota-efficient online search with offline query caching.
- **Key Artifacts:** `YouTubeSearchService.kt`, `OnlineSearchViewModel.kt`, `online_search_cache` table with 24-hour TTL eviction.
- **Commit:** `abdc162`

### Milestones M3 & M4: OAuth 2.0 PKCE, Keystore Security & Personal Sync
- **Objective:** Enterprise-grade Google account authentication and personal playlist sync.
- **Key Artifacts:** `OAuthManager.kt` (RFC 7636 PKCE), `KeystoreCryptoManager.kt` (AES-256-GCM hardware key), `YouTubeLibrarySyncService.kt`.
- **Commit:** `3744d16`

### Milestone M5: Synchronized Lyrics, Dynamic Glow & Playback Router
- **Objective:** Rich visual presentation, synchronized lyrics parsing, and initial online track routing.
- **Key Artifacts:** `LrcLibLyricsClient.kt`, `OnlinePlayerPreviewCard.kt`, `PlaybackRouter.kt` (Intent handoff).
- **Commit:** `f1e08bb`

### Milestone M6: Test Hardening, SemVer Bump & Documentation
- **Objective:** 100% test verification across all suites, bumping version to 0.2.0.
- **Key Artifacts:** 197 automated unit tests passing across 26 test suites; `CHANGELOG.md`, `README.md` updated.
- **Commit:** `97c4cf7`

---

## 3. Planned Milestones Detailed Specification (M7 – M9)

### Milestone M7: Integrated In-App Online Audio Streaming Engine
- **Objective:** Eliminate the external app Intent handoff (`PlaybackRouter.kt`). Implement direct in-app audio streaming inside MIORA's Media3 `PlaybackService`.
- **Scope & Architecture:**
  1. Subclass `androidx.media3.session.MediaLibraryService` to replace legacy session wiring.
  2. Implement `ResolvingDataSource.Factory` wrapping ExoPlayer's HTTP / Cache data source.
  3. Build `StreamResolverChain`:
     - Primary: Clean-room InnerTube stream extractor with QuickJS AST solver.
     - Secondary: Piped / Invidious proxy resolver fallback.
     - Fallback: Graceful prompt to launch external app if both resolvers fail.
  4. Dynamic stream URL caching with TTL awareness (`StreamUrlCache`).
- **Files Affected:**
  - Create: `app/src/main/java/com/fhf/miora/player/online/StreamResolverChain.kt`
  - Create: `app/src/main/java/com/fhf/miora/player/online/InnerTubeStreamResolver.kt`
  - Create: `app/src/main/java/com/fhf/miora/player/online/ProxyStreamResolver.kt`
  - Modify: `app/src/main/java/com/fhf/miora/service/MioraMediaService.kt`
  - Modify: `app/src/main/java/com/fhf/miora/ui/components/OnlinePlayerPreviewCard.kt`
- **Identified Risks & Mitigations:**
  - *Risk:* YouTube cipher changes break playback.
  - *Mitigation:* Secondary proxy fallback activates automatically without application crash; user notification informs of stream degradation.
- **Test Strategy:** Mock stream resolver suites testing 200 OK resolution, expired URL renewal, and graceful proxy failover.
- **Exit Gate:** Online songs play continuously inside the MIORA player with screen turned off.

---

### Milestone M8: Unified Hybrid Queue, Offline Stream Caching & Downloads
- **Objective:** Unify local and online tracks into a single continuous playback queue, backed by a multi-tier cache and background download service.
- **Scope & Architecture:**
  1. Create polymorphic `UnifiedTrack` and `UnifiedQueueManager` allowing mixed lists of local files, cached tracks, and online streams.
  2. Implement Media3 `SimpleCache` with LRU eviction (configurable limit, default 512MB) for transient streaming cache.
  3. Implement `MioraDownloadService` extending `androidx.media3.exoplayer.offline.DownloadService` for user-requested permanent downloads.
  4. Room SQLite schema update: `download_records` table tracking download status, file paths, and audio format.
- **Files Affected:**
  - Create: `app/src/main/java/com/fhf/miora/domain/model/UnifiedTrack.kt`
  - Create: `app/src/main/java/com/fhf/miora/player/queue/UnifiedQueueManager.kt`
  - Create: `app/src/main/java/com/fhf/miora/player/cache/MediaCacheManager.kt`
  - Create: `app/src/main/java/com/fhf/miora/service/MioraDownloadService.kt`
  - Modify: `app/src/main/java/com/fhf/miora/ui/screens/queue/QueueScreen.kt`
- **Identified Risks & Mitigations:**
  - *Risk:* Disk space exhaustion from uncontrolled caching.
  - *Mitigation:* Strict LRU evictor capped at user-defined threshold in Settings; low storage warnings.
- **Exit Gate:** Seamless queue containing Local FLAC -> Online Stream -> Offline Download with continuous, uninterrupted playback.

---

### Milestone M9: Equalizer, Volume Normalization, UI Polish & v1.0.0 Release
- **Objective:** Finalize audiophile acoustic features, comprehensive UI polish, CI/CD automated build pipeline, and produce the stable v1.0.0 release.
- **Scope & Architecture:**
  1. **Volume Normalization:** Integrate `VolumeNormalizationAudioProcessor` into ExoPlayer audio sink, honoring ReplayGain tags on local tracks and YouTube `loudnessDb` on online streams.
  2. **10-Band Graphic Equalizer:** Build custom Compose EQ screen with pre-gain, 10 frequency bands, bass boost, and audio virtualizer.
  3. **UI / UX Polish:** Smooth spring-animated transitions between mini-player and full-screen player; interactive synced lyrics seeking; dynamic ambient glow shaders.
  4. **Release Engineering:** Bump SemVer to `v1.0.0`, generate production APK, verify ProGuard/R8 obfuscation, and publish release notes.
- **Files Affected:**
  - Create: `app/src/main/java/com/fhf/miora/player/audio/VolumeNormalizationAudioProcessor.kt`
  - Create: `app/src/main/java/com/fhf/miora/player/audio/EqualizerManager.kt`
  - Create: `app/src/main/java/com/fhf/miora/ui/screens/equalizer/EqualizerScreen.kt`
  - Modify: `app/src/main/java/com/fhf/miora/ui/screens/player/PlayerScreen.kt`
  - Modify: `build.gradle.kts` (versionName = "1.0.0", versionCode = 100)
- **Exit Gate:** 100% automated test pass (>250 tests), zero memory leaks, release APK verified on Android 8.0 (API 26) through Android 15 (API 35).
