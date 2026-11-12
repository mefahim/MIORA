# MIORA NEXT — Comprehensive Engineering & Legal Risk Register

- **Audit Date:** November 12, 2026
- **Auditor:** Antigravity Risk & Compliance Engineering Team
- **Classification:** Formal Technical, Legal, Security & Operational Risk Assessment

---

## 1. Risk Evaluation Matrix

```
       IMPACT
       +-------------+-------------+-------------+
HIGH   |   RSK-03    |   RSK-01    |   RSK-06    |
       |   RSK-05    |   RSK-02    |   RSK-08    |
       +-------------+-------------+-------------+
MED    |   RSK-07    |   RSK-04    |   RSK-09    |
       +-------------+-------------+-------------+
LOW    |   RSK-10    |   RSK-11    |   RSK-12    |
       +-------------+-------------+-------------+
            LOW           MED          HIGH
                     LIKELIHOOD
```

---

## 2. In-Depth Risk Register & Mitigation Controls

### Category A: Technical Risks

#### RSK-01: YouTube Cipher Signature & Throttling Parameter Mutation
- **Category:** Technical / Upstream Dependency
- **Likelihood:** HIGH | **Impact:** HIGH | **Inherent Risk:** CRITICAL
- **Failure Mode:** YouTube deploys a change to `base.js` syntax, altering the signature deciphering algorithm or the `n`-parameter throttling function. Streaming speeds drop to 40 kbps (causing perpetual buffering) or return HTTP 403 Forbidden.
- **Mitigation Strategy:**
  1. Implement a pluggable `StreamResolverChain` that decouples cipher parsing from the player core.
  2. Maintain automated fallback to secondary Piped / Invidious proxy endpoints when direct extraction fails.
  3. Support remote dynamic cipher rules downloaded from a secure GitHub release asset, avoiding full APK rebuilds for minor cipher updates.
  4. Gracefully fall back to `PlaybackRouter.kt` (external YouTube Music app intent) if all resolvers fail.
- **Residual Risk:** MEDIUM
- **Trigger / Indicator:** Player reports consecutive `StreamResolveException` or HTTP 403 responses during stream resolution.

#### RSK-02: BotGuard / Proof of Origin (poToken) Invalidation
- **Category:** Technical / Origin Verification
- **Likelihood:** MEDIUM | **Impact:** HIGH | **Inherent Risk:** HIGH
- **Failure Mode:** Google updates the BotGuard challenge VM, breaking the headless `po_token.html` harness. Requests to `/youtubei/v1/player` fail with bot detection challenge errors.
- **Mitigation Strategy:**
  1. Abstract `TokenProvider` behind an interface with configurable token endpoints.
  2. Cache valid minted tokens with TTL monitoring to minimize unnecessary WebView invocations.
  3. Implement automated test harness detecting BotGuard failure modes in continuous integration.
- **Residual Risk:** MEDIUM
- **Trigger / Indicator:** `PoTokenException` thrown during player pre-warm or token minting flow.

#### RSK-03: QuickJS Native Library ABI Incompatibility
- **Category:** Technical / Native Toolchain
- **Likelihood:** LOW | **Impact:** HIGH | **Inherent Risk:** MEDIUM
- **Failure Mode:** `libquickjs.so` fails to load on emerging Android architectures (e.g. 16KB page sizes on Android 15+, RISC-V), crashing the application process with `UnsatisfiedLinkError`.
- **Mitigation Strategy:**
  1. Compile native binaries with 16KB ELF alignment (`-Wl,-z,max-page-size=16384`) in Android NDK r27+.
  2. Include fallback pure-Kotlin JavaScript interpreter (such as Rhino or Duktape fallback) for environments where native shared libraries cannot load.
- **Residual Risk:** LOW
- **Trigger / Indicator:** Fatal SIGSEGV or `UnsatisfiedLinkError` in crash reports on Android 15 devices.

---

### Category B: Legal, Intellectual Property & Policy Risks

#### RSK-06: Google Play Store Policy Prohibition (Stream Downloader / Backgrounding)
- **Category:** Store Distribution / Policy
- **Likelihood:** HIGH | **Impact:** CRITICAL | **Inherent Risk:** CRITICAL
- **Failure Mode:** Submitting an app that downloads YouTube audio or bypasses YouTube background playback restrictions to the Google Play Store violates Google Play Developer Distribution Agreement Section 4.4 and Developer Program Policies, resulting in immediate app suspension and developer account termination.
- **Mitigation Strategy:**
  1. **Dual Distribution Model:**
     - **F-Droid / GitHub Releases:** Distribute the full-featured MIORA Next (with in-app online streaming, downloads, and LRCLIB lyrics) via GitHub Releases, F-Droid, and Obtainium.
     - **Google Play Store (Optional):** If a Google Play release is desired, distribute a compliant "MIORA Local" build that includes only local audio playback, AudioFX equalizer, and metadata lookup, omitting direct YouTube stream extraction.
- **Residual Risk:** LOW (Zero risk for GitHub/F-Droid; strictly segmented for Play Store).
- **Trigger / Indicator:** Automated Google Play Console pre-launch report policy flags.

#### RSK-07: GPL-3.0 Copyleft License Contagion
- **Category:** Intellectual Property / Legal
- **Likelihood:** LOW | **Impact:** MEDIUM | **Inherent Risk:** MEDIUM
- **Failure Mode:** Inadvertently distributing closed-source binaries or violating GPL-3.0 terms when linking against `innertubex` or Metrolist derivative code.
- **Mitigation Strategy:**
  1. Formally relicense MIORA Next under GNU General Public License v3.0 (completed in Milestone M0, commit `d66cfc5`).
  2. Maintain complete public source code repository on GitHub with all build instructions and source dependencies clearly documented.
  3. Preserve original copyright headers from Apache-2.0 authors.
- **Residual Risk:** VERY LOW
- **Trigger / Indicator:** Open-source license scanning tools (e.g. FOSSology, ScanCode) detecting license mismatches.

---

### Category C: Security & Privacy Risks

#### RSK-08: Master Session Cookie Compromise (SAPISID Sniffing Insecurity)
- **Category:** Information Security / Privacy
- **Likelihood:** HIGH | **Impact:** CRITICAL | **Inherent Risk:** CRITICAL
- **Failure Mode:** Adopting Metrolist's pattern of sniffing master `SAPISID` cookies exposes user Google accounts. If stored in plaintext or exported in unencrypted backup files, any third-party app or attacker can hijack the user's entire Google account.
- **Mitigation Strategy:**
  1. **Strictly reject `SAPISID` cookie sniffing for MIORA Next.**
  2. Implement official RFC 7636 OAuth 2.0 with PKCE for user library synchronization.
  3. Encrypt all tokens using Android Keystore hardware-backed AES-256-GCM (`KeystoreCryptoManager.kt`).
  4. Never export credentials or authentication tokens in plain JSON backup files.
- **Residual Risk:** VERY LOW
- **Trigger / Indicator:** Security audits or code reviews detecting unencrypted credential storage.

---

### Category D: Operational & Maintenance Risks

#### RSK-09: YouTube Data API v3 Quota Exhaustion
- **Category:** Operational / API Limits
- **Likelihood:** HIGH | **Impact:** MEDIUM | **Inherent Risk:** HIGH
- **Failure Mode:** 10,000 units/day default Google Cloud quota is quickly depleted by user search queries (100 units/search = 100 queries/day max for all users combined), causing HTTP 403 `quotaExceeded` errors.
- **Mitigation Strategy:**
  1. Allow users to provide their own personal Google Cloud YouTube API Key in Settings.
  2. Implement 24-hour aggressive SQLite search query caching (`online_search_cache` table) to eliminate redundant queries.
  3. Support Piped / Invidious proxy search fallback when API quota is exhausted.
- **Residual Risk:** LOW
- **Trigger / Indicator:** API responses returning HTTP 403 with `reasons: ["quotaExceeded"]`.

#### RSK-10: Upstream Single Developer Maintenance Burnout
- **Category:** Operational / Ecosystem
- **Likelihood:** MEDIUM | **Impact:** MEDIUM | **Inherent Risk:** MEDIUM
- **Failure Mode:** Upstream maintainers of InnerTubeX or cipher solver repositories cease active development, causing stream resolution to permanently fail after the next YouTube update.
- **Mitigation Strategy:**
  1. Maintain a clean-room, independently maintainable parser architecture within the MIORA codebase.
  2. Design provider interfaces allowing rapid hot-swapping to alternative extractors (e.g., NewPipe Extractor, yt-dlp backend, or community proxy instances).
- **Residual Risk:** LOW
- **Trigger / Indicator:** Unresolved GitHub issues on upstream repository for >30 days.
