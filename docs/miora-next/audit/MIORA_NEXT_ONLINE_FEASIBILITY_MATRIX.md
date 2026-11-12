# MIORA NEXT — Online Streaming & Integration Feasibility Matrix

- **Audit Date:** November 12, 2026
- **Auditor:** Antigravity Engineering & Architecture Team
- **Purpose:** Rigorous technical, operational, legal, and architectural evaluation of online audio streaming mechanisms for the MIORA Next unified platform.

---

## 1. Evaluation of Architectural Approaches (Methods A through F)

### Method A: Official YouTube Data API v3
- **Mechanism:** Google Cloud Platform console OAuth 2.0 and API keys calling `https://www.googleapis.com/youtube/v3/*`.
- **Capabilities Provided:** Video search, playlist retrieval, channel subscriptions, liked videos list.
- **Critical Limitations:**
  - **No Audio Stream URLs:** The API returns only metadata; it does *not* provide raw audio or video stream URLs.
  - **Quota Restrictions:** GCP projects receive a default quota of 10,000 units/day (a single search query costs 100 units; max 100 searches/day for all users combined unless an enterprise quota extension is granted).
  - **Terms of Service Restrictions:** YouTube Developer Terms explicitly prohibit separating audio from video, caching streams offline, and playing audio in the background without displaying the video player.
- **Feasibility Verdict:** **CONDITIONAL** (Usable strictly for metadata search, user playlist sync, and personal library indexing; completely incapable of driving in-app audio playback).

---

### Method B: YouTube IFrame Player / Embedded WebView
- **Mechanism:** Embedding an Android `WebView` rendering the official YouTube IFrame Player API (`iframe_api`).
- **Capabilities Provided:** Fully licensed playback of YouTube videos directly within the app.
- **Critical Limitations:**
  - **No Background Audio:** YouTube IFrame player policy strictly mandates video visibility. When the screen turns off or the app is minimized, the WebView stops playback unless the user has YouTube Premium and official web backgrounding applies.
  - **System MediaSession Conflicts:** Does not integrate cleanly with Android Media3 `MediaSessionService`, lockscreen controls, notification actions, or Android Auto.
  - **Massive Resource Overhead:** Heavy RAM and CPU consumption running full Chromium web instances for every song.
- **Feasibility Verdict:** **NO-GO** (Directly violates the core product requirement of an uncompromised, audiophile-grade background music player).

---

### Method C: Direct In-App InnerTube Client (Metrolist / InnerTubeX Pattern)
- **Mechanism:** Reverse-engineered Google InnerTube private endpoints (`/youtubei/v1/*`), dynamic AST deciphering of `base.js` throttled parameters via embedded QuickJS native binary (`libquickjs.so`), and headless WebView BotGuard `poToken` minting (`po_token.html`).
- **Capabilities Provided:** Ad-free audio stream URLs (Opus 160kbps ITAG 251 / AAC 128kbps ITAG 140), background playback, native ExoPlayer integration, direct stream caching.
- **Critical Limitations & Risks:**
  - **Continuous Breakage & High Maintenance:** YouTube frequently modifies its player JavaScript (`base.js`), changes cipher variable signatures, and updates BotGuard challenge requirements. Any change instantly breaks playback until a new AST solver is released.
  - **Technological Protection Measure (TPM) Circumvention:** Bypasses Google's cipher throttling and origin verification mechanisms, creating DMCA/copyright exposure.
  - **User Account Risk:** Using `SAPISID` session sniffing on private endpoints exposes user accounts to automated fraud flags or termination.
  - **Copyleft Contagion:** Existing mature implementations (InnerTubeX) are strictly GPL-3.0.
- **Feasibility Verdict:** **CONDITIONAL / HIGH MAINTENANCE** (Provides the target user experience, but requires dedicated maintenance infrastructure and careful clean-room abstraction to protect the core player from catastrophic upstream breakages).

---

### Method D: Self-Hosted / Community Proxy (Piped / Invidious)
- **Mechanism:** Client calls an intermediate server API (e.g. Piped API or Invidious instance) which performs stream extraction, cipher solving, and caching on the server side, returning clean direct audio stream URLs to the client.
- **Capabilities Provided:** Shields the Android app from QuickJS native dependencies, BotGuard WebView minting, and frequent client-side app updates.
- **Critical Limitations:**
  - **Server Infrastructure & Bandwidth Costs:** Video/audio proxying consumes massive server bandwidth.
  - **Public Instance Instability:** Public community instances frequently experience rate-limiting, IP bans by YouTube, downtime, and deprecation.
  - **Latency:** Extra network hop adds 300–800ms of latency to initial track buffering.
- **Feasibility Verdict:** **CONDITIONAL** (Excellent as a fallback or user-configurable backend option, but unsuitable as the sole hardcoded dependency).

---

### Method E: External App Intent Handoff (Current MIORA v0.2.0 Approach)
- **Mechanism:** When a user taps an online search result, MIORA triggers an explicit Android `Intent` targeting the official YouTube Music app, YouTube app, or browser (`PlaybackRouter.kt`).
- **Capabilities Provided:** 100% legal, 0% maintenance, never breaks, zero server cost.
- **Critical Limitations:**
  - **Fails Product Vision:** Does not provide an in-app player. The user is kicked out of MIORA into an external third-party application.
  - **No Unified Queue:** Cannot mix local FLAC files and online tracks in the same playlist or playback queue.
  - **Subject to External Ads:** Plays ads unless the user has an active YouTube Music Premium subscription.
- **Feasibility Verdict:** **NO-GO as Primary / ACCEPTABLE as Fallback** (Currently implemented in v0.2-dev, but explicitly rejected as the final product state by the MIORA Next specification).

---

### Method F: Federated / Pluggable Multi-Source Architecture (Recommended)
- **Mechanism:** Designing a decoupled, provider-agnostic `OnlineMediaProvider` interface within MIORA. The core `PlaybackService` interacts exclusively with an abstract `MediaSourceResolver`. Individual providers (Local MediaStore, InnerTube Direct, Piped/Invidious Proxy, Subsonic/Jellyfin, and Fallback Intent Handoff) are isolated plugins.
- **Capabilities Provided:**
  - Insulates the core player engine and local playback from online breakage.
  - Allows seamless fallback: if direct InnerTube extraction fails (e.g., due to an unpatched cipher change), the player gracefully falls back to a proxy or user-prompted fallback without crashing.
  - Preserves clean-room architectural boundaries and isolates copyleft/licensing obligations.
- **Feasibility Verdict:** **GO (STRATEGIC CHOICE)**.

---

## 2. Capability Feasibility & Decision Matrix

| Capability | Approach Evaluated | Status | Technical Risk | Legal / Policy Risk | Maintenance Overhead | Longevity |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Local Audio Playback** | Android MediaStore + File Provider | **GO** | Very Low | None | Low | Indefinite |
| **Online Metadata Search** | YouTube Data API v3 | **GO** | Low (Quota) | Low (Compliant) | Low | High |
| **Personal Library Sync** | OAuth 2.0 PKCE + Data API v3 | **GO** | Low | Low (Compliant) | Low | High |
| **In-App Online Streaming** | Decoupled InnerTube MediaSource | **CONDITIONAL** | High (Cipher) | Medium (ToS) | High | Medium (Updates req.) |
| **Online Stream Fallback** | Piped / Invidious Proxy Resolver | **GO** | Medium (Uptime)| Low | Medium | High |
| **Emergency Fallback** | `PlaybackRouter` Intent Handoff | **GO (Fallback)**| None | None | None | Indefinite |
| **Transient Stream Cache** | Media3 `SimpleCache` + LRU | **GO** | Low | Low | Low | High |
| **Permanent Downloads** | Media3 `DownloadManager` | **CONDITIONAL** | Medium | Medium (ToS) | Medium | Medium |
| **Synchronized Lyrics** | LRCLIB REST API | **GO** | Low | Low (Open data) | Low | High |
| **Account Authentication** | OAuth 2.0 PKCE (Keystore) | **GO** | Low | Compliant | Low | High |
| **Raw Cookie Sniffing** | Headless WebView `SAPISID` | **NO-GO** | High | High (Accnt ban)| High | Low |
| **Embedded Video WebViews**| YouTube IFrame API | **NO-GO** | High (UX/RAM) | ToS violation bg| Low | Low |

---

## 3. Concrete Architectural Recommendation for MIORA Next

1. **Adopt Method F (Federated Multi-Source Architecture):**
   - Core player (`MioraPlaybackService`) remains completely decoupled from specific streaming implementations.
   - It delegates URL resolution to an abstract `StreamResolver` chain:
     1. Local File Resolver (instantaneous, 0ms).
     2. Cached Stream Resolver (Media3 `SimpleCache`).
     3. Primary Online Resolver (InnerTube direct stream extractor with clean-room QuickJS cipher solver).
     4. Secondary Online Resolver (Piped / Invidious proxy fallback when YouTube pushes unbroken cipher updates).
     5. Graceful Degrade / Intent Handoff (`PlaybackRouter.kt` retained as the ultimate safety net).
2. **Never Store Plaintext Session Cookies:**
   - Reject Metrolist's insecure `SAPISID` sniffing pattern. Retain MIORA's secure OAuth 2.0 PKCE with Android Keystore AES-256-GCM encryption for legitimate library sync, and use anonymous visitor sessions for stream fetching.
