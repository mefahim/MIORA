# MIORA NEXT — M7 Provider Capability & Feasibility Matrix
## Multi-Dimensional Evaluation of Streaming Providers & The Authorized Provider Contract

- **Document Version:** 1.0.0
- **Gate:** Pre-Implementation Architectural Freeze (Milestone M7)
- **Target Branch:** `docs/miora-next-audit-roadmap`

---

## 1. The 6-Dimension Evaluation Framework

To provide absolute legal, operational, and architectural clarity, every potential streaming provider is evaluated across six rigorous criteria:

1. **Architecture Feasibility:** Does the provider integrate cleanly with MIORA's decoupled, provider-agnostic domain interfaces?
2. **Provider Availability:** Are endpoints stable, active, maintained, and publicly reachable without extreme IP throttling?
3. **Technical Feasibility:** Can the provider supply raw, playable audio stream URLs (e.g. Opus/AAC/MP3) suitable for ExoPlayer background streaming?
4. **Service-Policy Permission:** Does the implementation comply with the content platform's official Terms of Service, API licenses, and developer guidelines?
5. **Distribution Permission:** Can an application incorporating this provider be legally and safely distributed across GitHub, F-Droid, or the Google Play Store without triggering DMCA takedowns or developer account bans?
6. **Long-Term Maintenance Feasibility:** What is the engineering maintenance burden? How frequently do upstream changes break playback?

---

## 2. Multi-Provider Evaluation Matrix

| Provider / Method | Architecture Feasibility | Provider Availability | Technical Feasibility | Service-Policy Permission | Distribution Permission | Maintenance Feasibility | Overall Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Provider A: YouTube Data API v3** | **HIGH** | **HIGH** | **FAIL (No Audio)** | **PASS (Compliant)** | **PASS (Play Store & F-Droid)** | **HIGH (Zero Breakage)** | **AUTHORIZED (Metadata Only)** |
| **Provider B: YouTube IFrame Web Player** | **LOW** | **HIGH** | **FAIL (No Background)** | **CONDITIONAL** | **PASS (Play Store)** | **MEDIUM** | **REJECTED (No Background Audio)** |
| **Provider C: Direct InnerTube Client** | **MEDIUM** | **HIGH** | **PASS (Opus 160k)** | **FAIL (Violates ToS)** | **FAIL (Play Store Ban / DMCA)** | **VERY LOW (Frequent Breakage)** | **UNAUTHORIZED / PROHIBITED** |
| **Provider D: Piped / Invidious Proxy** | **HIGH** | **MEDIUM** | **PASS (Direct HTTP)** | **NEUTRAL (Server-Side)** | **PASS (GitHub / F-Droid)** | **MEDIUM (Server Uptime Dep.)** | **CONDITIONAL (Self-Hosted/FOSS)** |
| **Provider E: Self-Hosted Cloud (Subsonic)** | **VERY HIGH**| **USER-OWNED** | **PASS (Bit-Perfect)** | **PASS (Fully Authorized)** | **PASS (All Channels)** | **HIGH (Stable Open Standard)** | **AUTHORIZED (Fully Approved)** |
| **Provider F: Open Audio (Internet Archive)**| **HIGH** | **HIGH** | **PASS (Direct MP3/OGG)** | **PASS (Public Domain/CC)** | **PASS (All Channels)** | **HIGH (Zero Breakage)** | **AUTHORIZED (Fully Approved)** |
| **Provider G: Intent Handoff (M5 Router)** | **HIGH** | **HIGH** | **FAIL (Leaves App)** | **PASS (100% Compliant)** | **PASS (All Channels)** | **HIGH (Zero Breakage)** | **SAFETY NET / FALLBACK ONLY** |

---

## 3. In-Depth Analysis by Provider

### Provider A: Official YouTube Data API v3
- **Strengths:** Fully authorized, official Google Cloud integration, stable JSON schemas, zero legal risk.
- **Deficiencies:** Delivers metadata, search results, and playlists only. Google strictly refuses to provide direct audio stream URLs. Terms of Service explicitly forbid audio-only background streaming.
- **Architectural Role in MIORA:** **Primary Authorized Discovery & Metadata Provider.** Drives search chips, autocomplete, album tracklists, and personal library playlist synchronization.

### Provider B: YouTube IFrame / Web Player
- **Strengths:** Officially allowed method to play YouTube videos within apps.
- **Deficiencies:** Requires visible web video render surface. Playback halts immediately when screen turns off or app is backgrounded. Severe CPU/RAM memory footprint.
- **Architectural Role in MIORA:** **REJECTED.** Completely incompatible with an audiophile background music player.

### Provider C: Direct InnerTube Private Client (Metrolist / InnerTubeX Pattern)
- **Strengths:** Delivers 160kbps Opus stream URLs directly from `googlevideo.com`.
- **Deficiencies & Critical Risks:**
  - Circumvents Google's technological protection measures (TPMs) using embedded QuickJS native binaries (`libquickjs.so`) and AST parsers to solve signature cipher scrambling and throttling algorithms.
  - Requires headless Android `WebView` running `assets/po_token.html` to mint BotGuard Proof of Origin tokens (`poToken`).
  - Constantly breaks whenever YouTube modifies `base.js` syntax, requiring emergency app updates.
  - Violates YouTube Terms of Service Section III.E.4.a and Google Play Developer Policies.
- **Architectural Role in MIORA:** **UNAUTHORIZED / PROHIBITED FOR PLAY STORE & DEFAULT BUILDS.** Must not be bundled as the primary or mandatory audio resolver in compliant MIORA builds.

### Provider D: Community / Self-Hosted Proxies (Piped / Invidious)
- **Strengths:** Offloads cipher solving and token minting to server infrastructure. Returns clean HTTP/HTTPS audio stream URLs to the client without requiring native QuickJS libraries or WebView BotGuard harnesses in the Android APK.
- **Deficiencies:** Public community instances frequently experience rate-limiting, IP blocking by YouTube, and downtime.
- **Architectural Role in MIORA:** **CONDITIONAL SECONDARY RESOLVER.** Supported as an optional, user-configurable proxy provider for F-Droid and GitHub releases where users can point to custom or private proxy instances.

### Provider E: Self-Hosted Personal Audio Cloud (Subsonic / Jellyfin / Navidrome)
- **Strengths:** 100% legally authorized, user-owned audio libraries. Provides bit-perfect FLAC/Opus/MP3 streaming, full search, album artwork, and playlist synchronization over the well-documented Subsonic open REST standard.
- **Deficiencies:** Requires the user to run a personal music server (e.g. Navidrome, Jellyfin, Subsonic).
- **Architectural Role in MIORA:** **FULLY APPROVED AUTHORIZED STREAMING PROVIDER.** Serves as the premier reference implementation of in-app streaming in MIORA Next.

### Provider F: Open / Creative Commons Music Repositories (Internet Archive / Jamendo)
- **Strengths:** Millions of legal, public-domain, live concert recordings, and Creative Commons music tracks with direct, unthrottled audio stream URLs.
- **Architectural Role in MIORA:** **FULLY APPROVED AUTHORIZED STREAMING PROVIDER.** Provides guaranteed out-of-the-box working online audio streaming for all users without API keys, subscriptions, or legal risk.

### Provider G: External Intent Handoff (`PlaybackRouter.kt`)
- **Strengths:** 100% compliant, zero maintenance, never breaks.
- **Deficiencies:** Exits the application; no unified queue or in-app audio controls.
- **Architectural Role in MIORA:** **SAFETY NET FALLBACK.** Triggered only when a user explicitly requests playback of a proprietary YouTube track for which no authorized in-app audio stream can be provided.

---

## 4. The Authorized Provider Contract Specification

To ensure total architectural separation between the playback engine and content sources, all online providers must implement the `OnlineMediaProvider` interface:

```kotlin
package com.fahim.powerampclone.playback.provider

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Declares the discrete capabilities an online provider may optionally support.
 */
enum class ProviderCapability {
    SEARCH,
    METADATA,
    ARTWORK,
    STREAM_AUDIO,
    USER_LIBRARY,
    LYRICS,
}

/**
 * Result of stream resolution containing audio URL and required HTTP request headers.
 */
data class ResolvedStream(
    val streamUri: Uri,
    val mimeType: String,
    val bitrateKbps: Int,
    val httpHeaders: Map<String, String> = emptyMap(),
    val expiryTimestampMs: Long? = null,
    val loudnessDb: Float? = null,
    val isDirectAudio: Boolean = true,
)

/**
 * Core interface contract for authorized online media providers.
 */
interface OnlineMediaProvider {
    val providerId: String
    val displayName: String
    val supportedCapabilities: Set<ProviderCapability>

    /**
     * Searches the provider for matching tracks, albums, or artists.
     */
    suspend fun search(query: String, filter: String? = null): Result<List<OnlineMediaItem>>

    /**
     * Resolves complete metadata for a given track identifier.
     */
    suspend fun getMetadata(trackId: String): Result<OnlineMediaItem>

    /**
     * Resolves a direct, playable audio stream for the given track identifier.
     * If the provider does NOT support STREAM_AUDIO, returns Failure(UnsupportedOperationException).
     */
    suspend fun resolveStream(trackId: String, quality: StreamQuality): Result<ResolvedStream>

    /**
     * Refreshes an expired stream URL proactively before playback underruns occur.
     */
    suspend fun refreshStream(trackId: String, expiredStream: ResolvedStream): Result<ResolvedStream>
}
```

---

## 5. Confirmed Provider Implementation Strategy

Based on confirmed owner decisions:
1. **Genuine In-App Streaming Mandated:** MIORA will implement direct in-app online audio streaming inside MIORA's own Media3 ExoPlayer, eliminating external-app handoffs as the primary playback mode.
2. **Multi-Source Resolver Implementation:**
   - **Primary Streaming Resolver:** Clean-room direct stream extractor inspired by Metrolist's InnerTube architecture, resolving direct 160kbps Opus / 128kbps AAC stream URLs with dynamic cipher and throttling handling.
   - **Secondary / Proxy Resolver:** Piped / Invidious proxy fallback when YouTube cipher modifications require upstream resolver updates.
   - **Discovery & Library Sync:** Official YouTube Data API v3 retained for quota-efficient search, auto-complete, and user playlist synchronization.
   - **Personal Cloud / Open Audio:** Subsonic/Navidrome and open audio repositories supported as complementary authorized providers.
   - **Safety Net Fallback:** `PlaybackRouter.kt` retained as emergency fallback only.
