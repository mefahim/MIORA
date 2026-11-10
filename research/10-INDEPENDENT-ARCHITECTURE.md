# 10 — Independent Clean-Room Architecture Specification

## 1. Architectural Principles & Design Goals
The goal of this architecture is to design a high-fidelity, production-grade, independently maintainable Android native music player. The architecture adheres to:
1. **Clean-Room Engineering**: Completely zero reliance on proprietary code, reverse-engineered binaries, or unlicensed assets.
2. **Modern Android Standards**: 100% Kotlin, Jetpack Compose, AndroidX Media3, Room, Hilt, and Coroutines/Flow.
3. **Audiophile-Grade Audio Reproduction**: 32-bit floating-point PCM audio pipeline, low-latency playback, custom Parametric EQ, gapless playback, and bit-perfect pass-through mode for external USB DACs (when DSP effects are bypassed).
4. **Offline-First & Privacy-Centric**: Zero mandatory network calls, no analytics trackers, no advertising SDKs, full local database autonomy.

---

## 2. Multi-Module System Topology

```mermaid
graph TD
    App[":app (Application Shell & Navigation)"]

    subgraph Feature Modules
        FPlayer[":feature:player (Player UI & MiniPlayer)"]
        FLib[":feature:library (Songs, Albums, Artists, Folders)"]
        FEq[":feature:equalizer (Parametric & Graphic EQ)"]
        FLyr[":feature:lyrics (LRC Parser & Synced View)"]
        FVis[":feature:visualizer (Spectrum & Waveform Canvas)"]
        FSet[":feature:settings (DataStore Preferences)"]
    end

    subgraph Core Modules
        CAudio[":core:audio (Media3 Playback & DSP Pipeline)"]
        CDB[":core:database (Room Entities, DAOs, FTS5)"]
        CModel[":core:model (Domain Entities & State)"]
        CUI[":core:ui (Design System, Knobs, Waveform Canvas)"]
        CScanner[":core:scanner (MediaStore, SAF, CUE & Tags)"]
    end

    App --> FPlayer
    App --> FLib
    App --> FEq
    App --> FLyr
    App --> FVis
    App --> FSet

    FPlayer --> CAudio
    FPlayer --> CUI
    FPlayer --> CModel

    FLib --> CDB
    FLib --> CScanner
    FLib --> CUI
    FLib --> CModel

    FEq --> CAudio
    FEq --> CUI
    FEq --> CModel

    FLyr --> CAudio
    FLyr --> CDB
    FLyr --> CUI

    CAudio --> CModel
    CDB --> CModel
    CScanner --> CDB
```

---

## 3. Module Responsibilities

| Module | Core Responsibility | Key Technologies |
| :--- | :--- | :--- |
| `:app` | Application entry point, Hilt dependency root, Compose navigation graph | Hilt, Navigation Compose |
| `:core:audio` | MediaSessionService, AudioTrack float playback, DSP filter chain, volume control | AndroidX Media3, AudioTrack Float, Biquad DSP |
| `:core:database` | Relational SQLite persistence, indexing, FTS5 full-text search, DAOs | Room 2.6+, SQLite WAL |
| `:core:scanner` | Library ingestion, MediaStore sync, SAF folder walk, CUE sheet splitting, ID3 tag extraction | WorkManager, MediaStore, SAF, JAudioTagger/TagLib |
| `:core:ui` | Design system, typography, color palette, custom rotary knobs, interactive waveform canvas | Jetpack Compose, Material 3, Canvas API |
| `:core:model` | Pure Kotlin domain models (Track, Album, Artist, Playlist, EqBand, Lyrics) | Kotlin Data Classes, Kotlinx Serialization |
| `:feature:player` | Fullscreen player screen, sliding mini-player, transport controls | Jetpack Compose, Compose Animation |
| `:feature:library` | Catalog lists and grids (Songs, Albums, Artists, Genres, Folders, Queue) | Compose LazyColumn / LazyVerticalGrid |
| `:feature:equalizer`| Graphic sliders, parametric curve canvas, tone knobs, reverb acoustic presets | Custom Compose Gestures & Path Rendering |
| `:feature:lyrics` | Synced LRC line renderer, time offset adjuster, local lyric cache | Compose LazyListState auto-scroll |
| `:feature:visualizer`| FFT audio spectrum analyzer, waveform oscilloscope | Android Visualizer API, Canvas / OpenGL ES |
| `:feature:settings`| Settings hierarchy, audio output selector, theme customizer, backup/restore | Jetpack DataStore Preferences |

---

## 4. Evaluation of Three Independent Audio Architectures

In compliance with Phase 1.1 architectural validation, we rigorously evaluate three candidate architectures:
- **Architecture A**: Media3-First (Standard AndroidX Media3 / ExoPlayer with default AudioSink and framework `AudioEffect`).
- **Architecture B (Recommended)**: Hybrid — Media3 Decoding with Custom 32-bit Float DSP Pipeline and High-Res AudioTrack Output.
- **Architecture C**: Fully Custom Native Audio Pipeline (C++ / Oboe / AAudio / FFmpeg via JNI).

### Deep Trade-Off Comparison Matrix

> [!NOTE]
> Every metric and capability below is labeled with its evidentiary and engineering status:
> - `[DOCUMENTED]`: Supported by official Android / API documentation.
> - `[MATHEMATICAL PROPERTY]`: Inherent mathematical characteristic of the numerical format.
> - `[DESIGN TARGET]`: Engineering objective planned for clean-room implementation.
> - `[ENGINEERING ESTIMATE]`: Engineering estimate based on architectural analysis (unmeasured in this environment).
> - `[UNVERIFIED]`: Unverified on physical hardware.

| Evaluation Dimension | Architecture A (Media3-First) | Architecture B (Hybrid Float DSP — Recommended Design Target) | Architecture C (Native C++ / Oboe) |
| :--- | :--- | :--- | :--- |
| **Codec Support** | `[DOCUMENTED]` Standard Android platform codecs (MP3, AAC, FLAC, Opus) | `[DESIGN TARGET]` System codecs + optional software extensions (APE, ALAC, DSD, WavPack) | `[DESIGN TARGET]` Custom-compiled FFmpeg NDK decoders |
| **PCM Formats** | `[DOCUMENTED]` 16-bit signed integer PCM | `[DOCUMENTED]` **32-bit float PCM (`ENCODING_PCM_FLOAT`)** | `[DESIGN TARGET]` 32/64-bit float native audio ring buffers |
| **Sample-Rate Handling** | `[DOCUMENTED]` Standard OS mixing path (resampling dependent on device HAL / policy) | `[DESIGN TARGET]` Preserves source rate where accepted by device audio policy / HAL | `[DESIGN TARGET]` Native rate selection where supported by AAudio exclusive HAL |
| **AudioTrack Behavior** | `[DOCUMENTED]` Standard `DefaultAudioSink` | `[DESIGN TARGET]` Custom `AudioSink` with float output & direct flags | `[DESIGN TARGET]` Bypasses Java AudioTrack; direct AAudio/OpenSL |
| **USB DAC Routing** | `[DOCUMENTED]` Standard OS USB class driver | `[DESIGN TARGET / UNVERIFIED]` Direct routing via `AudioDeviceInfo.TYPE_USB_DEVICE` (unverified on physical hardware) | `[UNVERIFIED]` Custom USB Host asynchronous packet streaming |
| **Audio Focus** | `[DOCUMENTED]` Built-in, fully automated by Media3 | `[DOCUMENTED]` Built-in, integrated with Media3 session | `[DESIGN TARGET]` Manual implementation via JNI callbacks |
| **Gapless Playback** | `[DOCUMENTED]` Built-in ExoPlayer gapless for MP3/FLAC/Opus | `[DESIGN TARGET]` Built-in with custom padding/delay trim | `[DESIGN TARGET]` Manual dual-decoder buffer pre-roll in C++ |
| **Crossfade** | `[DOCUMENTED]` Not supported out-of-the-box | `[DESIGN TARGET]` Dual-buffer mixing via custom `AudioProcessor` | `[DESIGN TARGET]` Custom C++ mixing loop with linear/equal-power curve |
| **EQ Processing** | `[DOCUMENTED]` Limited framework `AudioEffect` (5-10 fixed bands) | `[DESIGN TARGET]` **Full 10-32 band graphic & parametric Biquad IIR chain** | `[DESIGN TARGET]` Custom C++ SIMD NEON biquad filter chain |
| **Internal Arithmetic Dynamic Span** | `[MATHEMATICAL PROPERTY]` None (16-bit fixed integer clips at 0 dBFS) | `[MATHEMATICAL PROPERTY]` **Scale-invariant float (mathematical exponent range >1500 dB; instantaneous mantissa SNR ~144 dB)** | `[THEORETICAL ESTIMATE]` Scale-invariant float/double buffer math |
| **Audio Latency** | `[ENGINEERING ESTIMATE]` Estimated Standard: 50–100ms (buffer-dependent) | `[DESIGN TARGET]` **Low (estimated 20–50ms target)** | `[THEORETICAL ESTIMATE / UNVERIFIED]` Theoretical Minimum (<10ms via AAudio exclusive mode, hardware-dependent; unverified) |
| **Battery Impact** | `[ENGINEERING ESTIMATE]` Low (standard OS path; hardware DSP offloading when available) | `[ENGINEERING ESTIMATE]` Low to Moderate (software float DSP calculations on CPU) | `[ENGINEERING ESTIMATE]` Moderate to High (continuous native CPU wake during low-latency polling) |
| **Device Compatibility** | `[DOCUMENTED]` High (Android API 21 through 35 framework standard) | `[DOCUMENTED]` **High platform availability (`AudioTrack` float supported in Android framework since API 21; subject to vendor HAL quirks)** | `[ENGINEERING ESTIMATE]` Variable / fragile across vendor Android forks (OEM HAL quirks) |
| **Development Complexity** | `[ENGINEERING ESTIMATE]` Lowest | `[ENGINEERING ESTIMATE]` **Moderate (Clean separation of concerns, testable)** | `[ENGINEERING ESTIMATE]` Extremely High (NDK, C++ ABI builds, memory hazards) |
| **Licensing Risks** | `[DOCUMENTED]` Permissive (Apache 2.0 framework dependencies) | `[ENGINEERING ESTIMATE]` **Clean-room open-source architecture (Apache 2.0 / MIT dependencies; external FFmpeg requires LGPL review)** | `[ENGINEERING ESTIMATE]` High compliance burden (LGPL 2.1 compliance management for custom FFmpeg/C++ builds) |

---

### Critical Technical Distinctions

> [!WARNING]
> **Float PCM vs. Bit-Perfect Output**:
> 32-bit floating-point PCM (`AudioFormat.ENCODING_PCM_FLOAT`) provides scale-invariant numerical dynamic range that prevents internal clipping and accumulation distortion during DSP equalization. However, **Float PCM is NOT identical to bit-perfect output**. Bit-perfect reproduction means the digital audio samples delivered to the hardware DAC are mathematically identical to the source file without gain changes, equalization, or sample-rate conversion.
> - Active equalization, volume scaling, or crossfading inherently modifies sample values and precludes bit-perfect reproduction.
> - Even with DSP disabled, bit-perfect reproduction requires establishing that the complete source-to-DAC path bypassed system mixing and resampling. Output format, sample rate, mixing, and resampling behavior depend on the Android version, device audio policy, HAL implementation, and output path.

> [!IMPORTANT]
> **DAC Hardware Sample Rate & Negotiation Realities**:
> Not all USB DACs or smartphone DACs support every arbitrary sample rate. Furthermore, integer-multiple sample rate conversion is not inherently distortion-free. An audiophile audio player implementation must:
> 1. Query the device's supported sample rates using standard Android APIs (`AudioDeviceInfo.getSampleRates()` on API 23+).
> 2. Negotiate an actually supported output configuration on the target hardware.
> 3. Preserve the source sample rate where natively accepted by the hardware route.
> 4. Use an explicitly documented, high-quality bandlimited sinc resampling strategy when sample rate conversion is unavoidable.
> 5. Verify the entire physical audio route before claiming bit-perfect playback on any specific device.

### Recommended Architectural Strategy: Architecture B (Design Target)
Architecture B is our recommended clean-room design target, striking an optimal balance between audiophile audio reproduction, maintainability, and clean-room independence:
1. Leverage AndroidX Media3 for rock-solid decoding, media session management, audio focus, and background notification controls.
2. Intercept the audio pipeline at the `AudioProcessor` / `AudioSink` boundary to convert samples to 32-bit float PCM.
3. Apply our independent Robert Bristow-Johnson Biquad parametric equalizer, bass/treble tone controls, stereo balance, and soft limiter.
4. Output to `AudioTrack` configured with `AudioFormat.ENCODING_PCM_FLOAT`, querying `AudioDeviceInfo` to match native DAC hardware clock rates.

> [!NOTE]
> **Implementation Qualification**: Architecture B represents an engineering design recommendation and architectural specification. Actual production latency figures, power consumption curves, and OEM-specific HAL compatibility are engineering targets that must be empirically measured and validated on physical hardware during Phase 3 implementation.

---

## 5. Audio Playback Pipeline Architecture (Architecture B)

```mermaid
flowchart LR
    File[Audio File / Storage URI] --> Extractor[Media3 Audio Extractors / Decoders]
    Extractor -->|Decoded PCM Data| AudioSink[Custom Float AudioSink]
    
    subgraph DSP["Clean-Room 32-bit Float DSP Chain"]
        Preamp[Digital Preamp Gain] --> EQ[Bi-quad Parametric EQ Chain]
        EQ --> Tone[Bass & Treble Shelving Filters]
        Tone --> Balance[Stereo Pan & Balance]
        Balance --> Reverb[Algorithmic Reverb / Delay]
        Reverb --> Limiter[Soft Peak Limiter]
    end

    AudioSink --> Preamp
    Limiter --> SinkOut[AudioTrack PCM Float Output]
    SinkOut --> DAC[Device DAC / USB Interface]
```

### Digital Biquad Filter Mathematics
Every band in our 10-32 band graphic and parametric equalizer uses standard second-order IIR biquad transfer functions:

$$H(z) = \frac{b_0 + b_1 z^{-1} + b_2 z^{-2}}{a_0 + a_1 z^{-1} + a_2 z^{-2}}$$

#### Peaking EQ Filter Equations (Robert Bristow-Johnson Cookbook)
Given center frequency $f_0$, sample rate $f_s$, gain in dB $A = 10^{\text{gain}/40}$, and quality factor $Q$:

$$\omega_0 = 2\pi \frac{f_0}{f_s}, \quad \alpha = \frac{\sin(\omega_0)}{2Q}$$

$$b_0 = 1 + \alpha A, \quad b_1 = -2\cos(\omega_0), \quad b_2 = 1 - \alpha A$$
$$a_0 = 1 + \frac{\alpha}{A}, \quad a_1 = -2\cos(\omega_0), \quad a_2 = 1 - \frac{\alpha}{A}$$

All coefficients are normalized by dividing by $a_0$. Filtering is computed in Direct Form II Transposed for numerical stability.

---

## 6. Relational Database Schema (Room)

```mermaid
erDiagram
    TRACK ||--o{ PLAYLIST_ENTRY : "contains"
    PLAYLIST ||--o{ PLAYLIST_ENTRY : "has"
    ALBUM ||--o{ TRACK : "contains"
    ARTIST ||--o{ TRACK : "performs"
    FOLDER ||--o{ TRACK : "stores"
    TRACK ||--o{ BOOKMARK : "has"
    TRACK ||--o{ CACHED_LYRICS : "matches"

    TRACK {
        long id PK
        string title
        string path
        long durationMs
        int trackNumber
        int year
        long albumId FK
        long artistId FK
        long folderId FK
        int playCount
        long lastPlayedTimestamp
        boolean isFavorite
        byte[] waveformPeaks
    }

    ALBUM {
        long id PK
        string title
        string artistName
        int year
        int trackCount
        string artworkUri
    }

    ARTIST {
        long id PK
        string name
        int trackCount
        int albumCount
    }

    PLAYLIST {
        long id PK
        string name
        long createdAt
        long updatedAt
        int trackCount
    }

    PLAYLIST_ENTRY {
        long id PK
        long playlistId FK
        long trackId FK
        int sortOrder
    }

    BOOKMARK {
        long id PK
        long trackId FK
        long offsetMs
        string note
        long createdAt
    }

    CACHED_LYRICS {
        long id PK
        long trackId FK
        string content
        string source
        long updatedAt
    }
```
