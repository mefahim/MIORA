# 12 — Risk Assessment, Licensing Compliance & Clean-Room Boundaries

## 1. Clean-Room Reverse Engineering Legal Framework
In accordance with international software copyright laws (including Section 1201(f) of the DMCA and EU Directive 2009/24/EC on the legal protection of computer programs), reverse engineering for the purpose of achieving interoperability and independent functional specification is legally recognized when strict clean-room protocols are maintained.

### Strict Boundaries for the Independent Project
1. **Zero Proprietary Code Reuse**: Absolutely no decompiled smali, Java, C++, or assembly instructions from `com.maxmpz.audioplayer` or `p000.*` may be copied, pasted, or directly translated into the new project codebase.
2. **Zero Proprietary Asset Reuse**: No icons, vector drawables, audio sound effects, shaders, or visual themes from the APK may be included. All UI assets must be independently designed or sourced from permissive libraries (e.g. Google Material Icons).
3. **Trademark & Branding Non-Infringement**: The application must not use the trademarks "Poweramp", "Max MPZ", or confusingly similar naming, iconography, or trade dress. The independent project will use a distinct brand identity (e.g., *Resonate Music Player* / *Pulse Audio Player*).
4. **Independent Functional Specification**: All algorithms (such as biquad filter calculation and LRC parsing) must be implemented strictly from publicly available scientific literature and standards (e.g., the Robert Bristow-Johnson Audio EQ Cookbook).

---

## 2. Third-Party Open Source Licensing Audit

| Component / Library | Intended Role in Project | License | Compliance Obligations & Strategy |
| :--- | :--- | :--- | :--- |
| **AndroidX Media3 (ExoPlayer)** | Playback engine, MediaSessionService | **Apache 2.0** | Permissive. Requires retaining Apache copyright notice in source code and licenses dialog. |
| **AndroidX Jetpack & Room** | Architecture components, Compose, DB | **Apache 2.0** | Permissive. Retain notices. |
| **Google Hilt / Dagger** | Dependency Injection | **Apache 2.0** | Permissive. |
| **Coil** | Image loading & artwork cache | **Apache 2.0** | Permissive. |
| **FFmpeg (via Media3 FFmpeg Extension)** | Software decoding of FLAC, ALAC, OPUS, APE, DSD | **LGPL v2.1+ / GPL v2+** | **Strict Compliance**: Compile with `--disable-gpl` to remain under LGPL v2.1. Build as dynamically linked `.so` libraries (`libffmpeg.so`) so users can replace the binary. Provide source code and build scripts in compliance with LGPL Section 6. |
| **TagLib / JAudioTagger** | Audio metadata tag parsing & editing | **LGPL v2.1 / LGPL v3** | Dynamically link or consume as external modular library; retain open license. |
| **AutoEq Database** | Headphone frequency response presets | **MIT License** | Fully permissive. Attribute Jaakkopasanen/AutoEq project in app settings. |
| **projectM** | Optional OpenGL Milkdrop visualizer | **LGPL v2.1+** | Build as dynamic shared library; publish modification scripts if altered. |

---

## 3. Platform & Google Play Policy Compliance Risks

### A. Android 11+ Scoped Storage & `MANAGE_EXTERNAL_STORAGE`
- **Risk**: Google Play enforces strict gating on `android.permission.MANAGE_EXTERNAL_STORAGE` (All Files Access). Google routinely rejects apps requesting this permission unless they are file managers or backup utilities.
- **Mitigation Strategy**:
  - Rely primarily on `android.permission.READ_MEDIA_AUDIO` on Android 13+ and `android.permission.READ_EXTERNAL_STORAGE` on Android 12 and below.
  - For folder browsing, CUE sheets, and `.lrc` external files, use the official **Storage Access Framework (SAF)** `ACTION_OPEN_DOCUMENT_TREE`. When the user explicitly designates their `/Music/` directory via SAF, the application gains persistent read/write access to that specific directory tree without needing the high-risk `MANAGE_EXTERNAL_STORAGE` permission.

### B. Android 14+ Foreground Service Mandates
- **Risk**: Android 14 (API 34) and Android 15 (API 35) require all foreground services to declare a specific `android:foregroundServiceType` and provide justification during Play Console review.
- **Mitigation Strategy**:
  - Declare `android:foregroundServiceType="mediaPlayback"` on `PlaybackService`.
  - Maintain an active `MediaSession` and ongoing playback notification; Android OS automatically validates this type for media players.

### C. Android Auto Approval Guidelines
- **Risk**: Automotive applications on Google Play are subject to Driver Distraction Guidelines and automotive quality reviews.
- **Mitigation Strategy**:
  - Implement standard Media3 `MediaLibraryService`.
  - Avoid deeply nested hierarchies (limit browse depth to 4 levels).
  - Provide distinct browse categories (Recent, Albums, Artists, Playlists) with properly sized album artwork.

---

## 4. Technical Feasibility & Architectural Risk Matrix

| Risk Factor | Probability | Severity | Mitigation Action |
| :--- | :--- | :--- | :--- |
| **Hi-Res Audio Inconsistency across OEMs** | High | Medium | Implement standard 32-bit float PCM via `AudioTrack` as baseline (works uniformly across 99% of devices); offer vendor-specific direct output flags only as an optional advanced toggle. |
| **Large Library (50k+ tracks) Scanning Latency** | Medium | High | Utilize SQLite Write-Ahead Logging (WAL), indexed columns, batch transactions (`insertAll`), and asynchronous `CoroutineWorker`. |
| **Audio Glitches during Background Transitions** | Low | High | Elevate audio decoding thread priority to `THREAD_PRIORITY_URGENT_AUDIO`; maintain dedicated buffer queues. |
| **Battery Drain from Active Equalizer** | Low | Medium | Vectorize biquad filter loops; bypass DSP blocks when equalizer is toggled Off or set to Flat. |
