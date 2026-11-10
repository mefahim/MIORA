# Independent Android Native Music Player

An audiophile-grade, clean-room native Android music player project inspired by Poweramp's advanced audio engine and user experience. 

This repository contains the complete **Phase 1 Deep Reverse-Engineering Research, Product Discovery, and Clean-Room Architecture Specifications** produced using REA (Reverse Engineer Anything), JADX, Apktool, and static analysis tools.

---

## 📌 Phase 1 Research Overview

The goal of Phase 1 was to investigate the technical implementation details of advanced Android music players (specifically Poweramp build-1035), audit all 70 core features, and design a modern, clean-room native Android architecture using Kotlin, Jetpack Compose, AndroidX Media3, and custom DSP pipelines.

- **Executive Summary**: [PHASE-1-FINAL-REPORT.md](PHASE-1-FINAL-REPORT.md)
- **Investigation Matrix**: 70 Features Audited (13 `STATIC_IMPLEMENTATION_CONFIRMED`, 55 `IMPLEMENTED_BUT_RUNTIME_UNVERIFIED`, 1 `PARTIALLY_IMPLEMENTED`, 1 `INFERRED`, 0 `RUNTIME_VERIFIED`)
- **Compliance**: 100% Clean-Room design without reusing proprietary binary code or copyrighted assets.

---

## 📚 Complete Research & Architecture Reports

All detailed engineering and forensic reports are located in the [`research/`](research/) directory:

| Document | Title | Description |
| :--- | :--- | :--- |
| **[00](research/00-ENVIRONMENT-READINESS.md)** | **Environment Readiness** | REA 6.7.0, JADX 1.5.6, Apktool 3.0.3, ADB 1.0.39, JDK 17 setup audit and PRoot compatibility. |
| **[01](research/01-APK-FORENSIC-REPORT.md)** | **APK Forensic Report** | Cryptographic verification (SHA-256), manifest analysis, permissions, and native library audit. |
| **[02](research/02-REA-INVESTIGATION-LOG.md)** | **REA Investigation Log** | Chronological record of all REA tool calls, parameters, and decompilation steps. |
| **[03](research/03-TECH-STACK-ANALYSIS.md)** | **Tech Stack Analysis** | Side-by-side comparison of target stack vs. modern Android architecture (Jetpack Compose, Media3). |
| **[04](research/04-FEATURE-INVENTORY.md)** | **Feature Inventory** | Exhaustive catalog of 70 features across 8 categories (A through H) with evidence links. |
| **[05](research/05-SCREEN-INVENTORY.md)** | **Screen Inventory** | Complete mapping of 66 declared Activities, dialogs, preferences, and widgets. |
| **[06](research/06-NAVIGATION-FLOW.md)** | **Navigation Flow** | Mermaid state diagrams detailing player, library, equalizer, and modal transitions. |
| **[07](research/07-PREMIUM-FEATURE-INVENTORY.md)** | **Monetization & Licensing** | Analysis of the 15-day trial engine, unlocker IPC (`sharedUserId`), and multi-store billing. |
| **[08](research/08-CODE-PATH-ANALYSIS.md)** | **Code Path Deep Dive** | Reverse-engineered paths for `PlayerServiceCore`, `Pipeline2` JNI, Biquad filter math, and SQLite FTS4. |
| **[09](research/09-RUNTIME-OBSERVATIONS.md)** | **Runtime Observations** | Hardware/emulator environment constraints, ADB findings, and headless static mitigations. |
| **[10](research/10-INDEPENDENT-ARCHITECTURE.md)** | **Clean-Room Architecture** | Multi-module architecture proposal, Room schema, Biquad IIR math formulas, and AudioTrack pipeline. |
| **[11](research/11-PRODUCT-REQUIREMENTS.md)** | **Product Requirements (PRD)** | User personas, MoSCoW prioritization, functional specs, and non-functional audio benchmarks. |
| **[12](research/12-RISK-AND-LICENSING.md)** | **Risk & Licensing Analysis** | Clean-room copyright boundaries, LGPL 2.1 compliance (FFmpeg), and Play Store policies. |
| **[13](research/13-IMPLEMENTATION-ROADMAP.md)** | **Implementation Roadmap** | Six-phase execution roadmap from UI scaffolding to production store release. |
| **[14](research/14-EVIDENCE-MATRIX.md)** | **Evidence Traceability Matrix** | Traceability matrix linking every feature to evidence logs, confidence ratings, and test cases. |
| **[15](research/15-FEATURE-AUDIT-VALIDATION.md)** | **Feature Audit & Hardening** | Four-dimensional audit (Declaration, Implementation, Execution, Outcome) across all 70 features. |
| **[16](research/16-QA-CORRECTION-LOG.md)** | **QA & Consistency Log** | Final Phase 1.2 QA corrections, audio claim qualifications, and FFmpeg compliance audit. |
| **[17](research/17-PHASE-1.2.1-CORRECTION-REPORT.md)** | **Phase 1.2.1 Correction Report** | Residual technical evidence corrections, DSP precision bounds, HAL routing, and LGPL analysis. |

---

## 🏗️ Target Independent Architecture

The planned production implementation uses a modular, offline-first native Android architecture:

```
app (Application entry, Hilt DI root)
├── core
│   ├── audio        # AudioTrack / Oboe 32-bit float DSP, Biquad EQ, Crossfade, Gapless
│   ├── database     # Room 2.6+ with SQLite WAL & FTS5 full-text search
│   ├── scanner      # MediaStore + SAF fast parallel recursive audio metadata extractor
│   ├── model        # Domain models (Track, Album, Artist, EqualizerBand, Preset)
│   └── ui           # Material 3 design tokens, rotary knobs, OLED dark themes
└── feature
    ├── player       # Media3 MediaSessionService, RMS waveform seekbar, gestures
    ├── library      # Fast-scrolling track lists, album grid, folder tree
    ├── equalizer    # 10–32 band graphic/parametric EQ canvas with interactive Q-factor
    ├── lyrics       # Synchronized LRC parser & karaoke-style smooth scrolling
    └── settings     # DataStore preferences & audio output configuration
```

### Key Engineering Decisions
- **Audio Pipeline**: Custom Float `AudioTrack` output pipeline utilizing 32-bit floating-point PCM (`AudioFormat.ENCODING_PCM_FLOAT`) to provide scale-invariant DSP headroom and avoid 16-bit fixed-point quantization distortion prior to output.
- **Parametric Equalizer**: Digital Biquad IIR filtering (Peaking, Low-Shelf, High-Shelf, High-Pass, Low-Pass) calculated with Robert Bristow-Johnson Audio EQ Cookbook formulas.
- **Library Scanning**: Dual-engine scanner combining Android `MediaStore` with direct filesystem indexing and Storage Access Framework (SAF) for SD cards and USB DAC/OTG storage.
- **UI Engine**: 100% Jetpack Compose with custom hardware-accelerated Canvas renderers for audio waveforms and spectrum visualizers.

---

## ⚖️ Clean-Room & Legal Compliance

To strictly adhere to intellectual property laws and clean-room engineering standards:
1. **Zero Code Copying**: No decompiled Smali, Java, or C++ code is copied or incorporated into this repository.
2. **Proprietary Assets Excluded**: The commercial APK and copyrighted graphic assets are strictly excluded via `.gitignore`.
3. **Independent Implementation**: All code and algorithms are reconstructed independently based on publicly documented audio DSP engineering standards (RFCs, Audio EQ Cookbook, Android open-source documentation).

---

## 📄 License

This research and specification documentation is licensed under the [Apache License 2.0](LICENSE).
