# MIORA NEXT — Master Audit & Strategic Architectural Synthesis

- **Audit Date:** November 12, 2026
- **Auditor:** Antigravity Engineering, Architecture & Security Team
- **Audited Commit Baseline:** `97c4cf7e761dfa9d94943f6c8ef8ca67417036d3` (Local `v0.2-dev`)
- **Public Stable Reference:** `03fed47c2a1d295f2b354ce7911c61b16177a19d` (`origin/main`, v0.1.0)
- **Target Product Vision:** Unified Hybrid Music Player combining MIORA's audiophile-grade local playback engine with seamless, ad-free online streaming, unified queues, synchronized lyrics, and modern dynamic design.

---

## 1. Executive Summary & Core Verdict

The comprehensive Stage 0 audit of the MIORA Next project has concluded. Over the course of this investigation, our team performed static decompilation and reverse-engineering of Metrolist 13.7.0 (`com.metrolist.music`), a forensic code review of existing local development work on `v0.2-dev` (Milestones M0 through M6), a legal analysis of open-source license compatibility, and a feasibility assessment of online audio streaming mechanisms.

### Key Audit Findings:
1. **Existing Work Quality (M0–M6):**
   - The local branch `v0.2-dev` contains high-quality, architecturally clean work comprising 6 commits ahead of `origin/main`.
   - All **197 automated unit tests across 26 test suites pass with 100% success**.
   - It successfully implements decoupled domain models (`MioraTrack` vs `OnlineTrack`), SQLite schema v3 migrations, YouTube Data API v3 search with 24-hour query caching, Google OAuth 2.0 PKCE with Android Keystore AES-256-GCM encryption, and LRCLIB synchronized lyrics client.
2. **The Real Gap in M0–M6 Work:**
   - **Online audio is NOT currently played in-app.**
   - Milestone M5 implemented `OnlinePlayerPreviewCard.kt` and `PlaybackRouter.kt`, which triggers an external Android `Intent` to launch the YouTube Music app, YouTube app, or web browser.
   - Presenting an external-app handoff as a complete music player fails the core product vision. MIORA Next requires **in-app audio streaming** with background playback, unified queue mixing, and offline caching.
3. **Metrolist 13.7.0 Reverse-Engineering Insights:**
   - Metrolist achieves in-app audio playback via AndroidX Media3 1.10.1 `MediaLibraryService` (`MusicService`) utilizing a custom `ResolvingDataSource` that resolves `youtube://video/{id}` URIs to direct audio streams (`googlevideo.com/videoplayback`).
   - Stream extraction relies on InnerTube private endpoints, a headless Android `WebView` BotGuard VM minter (`assets/po_token.html`), and an embedded QuickJS native binary (`libquickjs.so`) running an AST cipher solver (`meriyah.js`, `astring.js`, `yt.solver.core.js`).
   - **Severe Security Vulnerability Identified:** Metrolist sniffs master session cookies (`SAPISID`) via an in-app WebView, calculates `SAPISIDHASH`, and stores raw cookies unencrypted in `DataStore` and plaintext JSON backup files. **This practice is strictly rejected for MIORA Next.**
4. **Licensing & Legal Compliance:**
   - Metrolist and InnerTubeX are strictly **GPL-3.0**. Direct compilation creates a single derivative work.
   - Milestone M0 successfully transitioned the local repository to **GNU General Public License v3.0 (GPL-3.0)**, ensuring 100% legal compliance.
5. **Distribution Strategy:**
   - Distribute the full-featured hybrid MIORA Next application via **GitHub Releases, F-Droid, and Obtainium**.
   - If Google Play Store distribution is desired, maintain a compliant "MIORA Local" flavor that omits YouTube stream extraction.

---

## 2. Complete Deliverables Index in `docs/miora-next/audit/`

| Deliverable File | Focus & Subject Matter | Status |
| :--- | :--- | :---: |
| [README.md](file:///workspace/miora-v0-2/docs/miora-next/audit/README.md) | Audit index, scope, repository topology, and methodology | **VERIFIED** |
| [MIORA_NEXT_BASELINE_AUDIT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_BASELINE_AUDIT.md) | Technical baseline of MIORA v0.1.0 vs v0.2-dev; 197 test inventory | **VERIFIED** |
| [MIORA_NEXT_REA_FORENSIC_REPORT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_REA_FORENSIC_REPORT.md) | Forensic breakdown of Metrolist 13.7.0 across 11 subsystems | **VERIFIED** |
| [MIORA_NEXT_FEATURE_COMPARISON.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_FEATURE_COMPARISON.md) | Quad-layer matrix: Metrolist vs v0.1.0 vs v0.2-dev vs MIORA Next | **VERIFIED** |
| [MIORA_NEXT_ONLINE_FEASIBILITY_MATRIX.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_ONLINE_FEASIBILITY_MATRIX.md) | Methods A through F evaluation; GO/CONDITIONAL/NO-GO matrix | **VERIFIED** |
| [MIORA_NEXT_LICENSE_AUDIT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_LICENSE_AUDIT.md) | GPL-3.0 copyleft analysis, clean-room rules, dependency inventory | **VERIFIED** |
| [MIORA_NEXT_TARGET_ARCHITECTURE.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_TARGET_ARCHITECTURE.md) | Complete architectural blueprint, multi-tier cache, Media3 engine | **VERIFIED** |
| [MIORA_NEXT_PHASE_ROADMAP.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_PHASE_ROADMAP.md) | Detailed implementation plan for Milestones M0 through M9 | **VERIFIED** |
| [MIORA_NEXT_RISK_REGISTER.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_RISK_REGISTER.md) | Technical, legal, policy, and operational risk mitigation register | **VERIFIED** |
| [MIORA_NEXT_AUDIT_SUMMARY.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_AUDIT_SUMMARY.md) | Executive synthesis and strategic architectural decisions | **VERIFIED** |

---

## 3. Recommended Immediate Next Step

With Stage 0 (Audit & Roadmap Recovery) complete and all 10 deliverables written and verified:
1. **Commit and push all audit documentation** to the dedicated GitHub documentation branch `docs/miora-next-audit-roadmap`.
2. **Synchronize documentation** back into the local `v0.2-dev` development branch.
3. **Present this summary to the project owner and await review.**
4. **Immediate implementation milestone upon owner approval:** **Milestone M7 (Integrated In-App Online Audio Streaming Engine)**, replacing the external `PlaybackRouter.kt` handoff with a native AndroidX Media3 `ResolvingDataSource` stream resolver chain.
