# MIORA Next — Master Audit & Verified Roadmap Index

**Project:** MIORA (Music in Our Real Audio)  
**Classification:** Pre-Implementation Architectural Audit, REA Forensic Analysis & Engineering Roadmap  
**Audit Date:** 2026-11-12  
**Public Baseline (GitHub `main`):** `03fed47c2a1d295f2b354ce7911c61b16177a19d` (MIORA v0.1.0 Stable Release)  
**Inspected Local Development Head (`v0.2-dev`):** `97c4cf7e761dfa9d94943f6c8ef8ca67417036d3` (M0–M6 Experimental Milestone)  
**Preservation Branch:** `docs/miora-next-audit-roadmap` (Branched safely from `origin/main`)  

---

## 1. Executive Purpose & Scope

The purpose of this comprehensive audit and roadmap recovery is to define the exact engineering path for **MIORA Next**: an uncompromised hybrid music ecosystem that seamlessly unifies:
1. **MIORA's High-Fidelity Local Music Player & Library**: Gapless MediaStore playback, Media3 background audio, 10-band equalizer with AutoEQ presets, lockscreen controls, and zero-latency SQLite playlists.
2. **A Full-Featured Online Music Experience Inspired by Metrolist**: Catalog browsing, online search, personal cloud library synchronization (playlists and liked songs), synchronized lyrics via open community providers, dynamic artwork aesthetics, and offline caching.
3. **A Single Unified Android Application**: One robust application with top-bar mode switching (`[ Local Music | Online Music ]`), shared audio processing, common navigation paradigms, and unified queue architecture.

This audit rejects superficial mockups, static metadata-only shells, and external-app handoffs presented as completed player functionality. It establishes a rigorous, legally compliant, and technically maintainable engineering architecture.

---

## 2. Master Deliverables Directory

| File Name | Title | Primary Scope |
|---|---|---|
| **[MIORA_NEXT_BASELINE_AUDIT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_BASELINE_AUDIT.md)** | Baseline Workspace & Git Audit | Rigorous inspection of working directory, Git branch topology, commits, build system versions, database schemas (v1/v2/v3), test suites (197 tests), and v0.1.0 recovery path. |
| **[MIORA_NEXT_REA_FORENSIC_REPORT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_REA_FORENSIC_REPORT.md)** | Metrolist REA Forensic Re-Analysis | Forensic breakdown of `Metrolist-izzy.apk` (v13.7.0, build 153) and `metrolistgroup/metrolist` source. Separates static decompiled facts from runtime-unverified inferences. |
| **[MIORA_NEXT_FEATURE_COMPARISON.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_FEATURE_COMPARISON.md)** | Quad-Layer Feature Comparison Matrix | Granular feature-by-feature matrix comparing Metrolist 13.7.0, MIORA v0.1.0, MIORA v0.2.0 local dev (`v0.2-dev`), and the target MIORA Next vision. |
| **[MIORA_NEXT_ONLINE_FEASIBILITY_MATRIX.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_ONLINE_FEASIBILITY_MATRIX.md)** | Online Service Feasibility & Strategy | Deep evaluation of online mechanisms (YouTube Data API v3, IFrame visual embeds, InnerTube private endpoints, Piped/Invidious proxies, and open federated backends). Defensible GO/NO-GO classifications. |
| **[MIORA_NEXT_LICENSE_AUDIT.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_LICENSE_AUDIT.md)** | License Audit & Copyleft Analysis | In-depth legal analysis of Apache-2.0 vs GPL-3.0, clean-room development boundaries, InnerTube copyleft contagion, and concrete licensing recommendations. |
| **[MIORA_NEXT_TARGET_ARCHITECTURE.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_TARGET_ARCHITECTURE.md)** | MIORA Next Target Architecture | Complete system blueprint: Dual-domain providers, shared domain models, ResolvingMediaSourceFactory, Media3 MediaLibraryService, Room/SQLite persistence, and Keystore crypto. |
| **[MIORA_NEXT_PHASE_ROADMAP.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_PHASE_ROADMAP.md)** | Phased Implementation Roadmap (M0–M9) | Ten-phase milestone plan mapping all existing work, new streaming/caching engines, automated tests, device testing, acceptance criteria, and exit gates. |
| **[MIORA_NEXT_RISK_REGISTER.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_RISK_REGISTER.md)** | Master Risk Register & Mitigations | Comprehensive evaluation of technical, legal, security, platform policy, and operational risks with concrete mitigation strategies and contingencies. |
| **[MIORA_NEXT_AUDIT_SUMMARY.md](file:///workspace/miora-v0-2/docs/miora-next/audit/MIORA_NEXT_AUDIT_SUMMARY.md)** | Executive Audit Summary & Decision Gates | High-level synthesis for project owners: true completion status of M0–M6, stale/inconsistent reports, feasibility conclusions, and recommended immediate milestone. |

---

## 3. Inspected Workspaces & Git Context

- **Remote Baseline (`origin/main`):** `03fed47`  
  - Contains production-verified MIORA v0.1.0 stable release under the Apache License 2.0.
  - 164 unit tests passing 100%. Single-source-of-truth song count derivation, 4-step permission onboarding wizard, and pure local MediaStore playback.
- **Local Development Branch (`v0.2-dev`):** `97c4cf7` (ahead by 6 commits)  
  - Contains experimental implementation of Milestones M0 through M6:
    - `d66cfc5`: Milestone M0 (GPL-3.0 license migration and baseline research).
    - `a649982`: Milestone M1 (Decoupled domain models, SQLite schema v3, ModeSelector).
    - `abdc162`: Milestone M2 (YouTube Data API v3 search, 24h SQLite query cache).
    - `3744d16`: Milestone M3 & M4 (Google OAuth 2.0 PKCE, Keystore AES-256-GCM token storage, personal YouTube library sync).
    - `f1e08bb`: Milestone M5 (LRCLIB synchronized lyrics client, dynamic ambient glow, PlaybackRouter fallback).
    - `97c4cf7`: Milestone M6 (Unit test expansion to 197 tests, v0.2.0 SemVer bump).
  - *Audit Finding:* While existing M0–M6 work provides robust domain decoupling, security encryption, and UI foundations, online playback was implemented via external Intent handoff (`PlaybackRouter.kt` launching the YouTube Music or YouTube Android app) rather than an integrated in-app streaming player.
- **Preservation Branch (`docs/miora-next-audit-roadmap`):**  
  - Dedicated branch created directly from `origin/main` (`03fed47`).
  - Contains exclusively documentation files within `docs/miora-next/audit/`. Zero unreviewed source code from `v0.2-dev` is leaked into this branch.
