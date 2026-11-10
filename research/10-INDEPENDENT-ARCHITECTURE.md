# 10 — Independent Clean-Room Architecture Specification

## 1. Architectural Principles & Design Goals
The goal of this architecture is to design a high-fidelity, production-grade, independently maintainable Android native music player. The architecture adheres to:
1. **Clean-Room Engineering**: Completely zero reliance on proprietary code, reverse-engineered binaries, or unlicensed assets.
2. **Modern Android Standards**: 100% Kotlin, Jetpack Compose, AndroidX Media3, Room, Hilt, and Coroutines/Flow.
3. **Audiophile-Grade Audio Reproduction**: 32-bit floating-point PCM audio pipeline, low-latency playback, custom Parametric EQ, gapless playback, and bit-perfect DAC output.
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

## 4. Audio Playback Pipeline Architecture

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
    Limiter --> OutputRoute{Output Routing}
    OutputRoute -->|Internal DAC| AT[AudioTrack PCM Float 32-bit]
    OutputRoute -->|External USB| USB[Direct USB DAC Host Mode]
    OutputRoute -->|Chromecast| Cast[Media3 Cast Extension]
```

### Digital Filter Mathematics (Parametric EQ)
Each band of the parametric equalizer is evaluated as a standard second-order direct-form II transposed biquad IIR digital filter:

$$H(z) = \frac{b_0 + b_1 z^{-1} + b_2 z^{-2}}{a_0 + a_1 z^{-1} + a_2 z^{-2}}$$

The filter coefficients ($b_0, b_1, b_2, a_0, a_1, a_2$) are computed in clean-room Kotlin using the standard Robert Bristow-Johnson Audio EQ Cookbook algorithms parameterized by:
- $F_s$: Current audio sample rate (e.g. 44,100 Hz, 48,000 Hz, 96,000 Hz, 192,000 Hz).
- $f_0$: Center frequency in Hz (20 Hz to 20,000 Hz).
- $Q$: Quality factor (0.1 to 12.0).
- $dBgain$: Gain in decibels (-15.0 dB to +15.0 dB).

---

## 5. Relational Database Schema Proposal (Room)

```mermaid
erDiagram
    TRACK ||--o{ PLAYLIST_ENTRY : contains
    PLAYLIST ||--o{ PLAYLIST_ENTRY : holds
    ALBUM ||--o{ TRACK : groups
    ARTIST ||--o{ TRACK_ARTIST : associates
    TRACK ||--o{ TRACK_ARTIST : associates
    FOLDER ||--o{ TRACK : stores
    TRACK ||--o| LYRICS : has
    TRACK ||--o{ BOOKMARK : marks
    OUTPUT_DEVICE ||--o{ EQ_PRESET_BINDING : binds
    EQ_PRESET ||--o{ EQ_PRESET_BINDING : provides
    EQ_PRESET ||--o{ EQ_BAND : contains

    TRACK {
        long id PK
        string uri
        string title
        string titleSort
        long albumId FK
        int trackNumber
        int discNumber
        long durationMs
        long sizeBytes
        long dateAddedSec
        long dateModifiedSec
        string mimeType
        int sampleRate
        int bitDepth
        int bitrateKbps
        string codec
        long playCount
        long lastPlayedMs
        boolean isFavorite
        long bookmarkOffsetMs
        string waveformData
    }

    ALBUM {
        long id PK
        string title
        string titleSort
        string artistName
        int year
        int trackCount
        string artworkUri
    }

    ARTIST {
        long id PK
        string name
        string nameSort
        int albumCount
        int trackCount
    }

    FOLDER {
        long id PK
        string path
        string name
        long parentFolderId
        int songCount
    }

    PLAYLIST {
        long id PK
        string name
        long createdAt
        long updatedAt
        int songCount
        long totalDurationMs
    }

    EQ_PRESET {
        long id PK
        string name
        boolean isParametric
        float preampGainDb
        boolean isBuiltIn
    }

    EQ_BAND {
        long id PK
        long presetId FK
        int filterType
        float frequencyHz
        float qFactor
        float gainDb
        int channelMode
    }
```

---

## 6. Background Playback & Lifecycle Service

```mermaid
sequenceDiagram
    autonumber
    participant System as Android OS / System
    participant Service as PlaybackService (MediaSessionService)
    participant Session as MediaSession
    participant Player as ExoPlayer Instance
    participant Notif as MediaNotificationManager

    System->>Service: onCreate()
    Service->>Player: Instantiate ExoPlayer with custom AudioSink
    Service->>Session: MediaSession.Builder(this, Player).build()
    Service->>Notif: Initialize MediaStyle Notification Provider

    Note over Service,Player: User initiates playback
    Service->>Service: startForeground(NOTIFICATION_ID, notification)
    Player->>Player: Transition to STATE_READY & playWhenReady = true
    Service->>System: Register HeadsetPlugReceiver & AudioDeviceCallback

    Note over Service,System: Headset unplugged / BT disconnected
    System->>Service: onAudioDevicesRemoved() / ACTION_AUDIO_BECOMING_NOISY
    Service->>Player: pause()
    Service->>Notif: Update notification to Paused state
```

---

## 7. Media Ingestion & Scanner Architecture

1. **MediaStore Observer**:
   - `ContentObserver` registers on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
   - Incremental diffing: Detects newly added or deleted files instantly without walking the storage tree.
2. **SAF / WorkManager Direct Scanner**:
   - For user-selected folders on SD cards or internal directories, a periodic or on-demand `CoroutineWorker` scans files.
   - Extracts embedded tags via lightweight ID3/Vorbis reader.
   - Discovers adjacent `.lrc` and `.cue` files.
   - Generates and stores 100-point RMS waveform arrays for the visual seekbar.

---

## 8. Battery & Performance Optimization Strategy

1. **Audio Decoding Offload**: Utilize platform hardware decoders via MediaCodec where supported to minimize CPU wake time.
2. **Zero Polling in Service Loop**: All state changes propagate reactively via Kotlin `StateFlow` and `SharedFlow`.
3. **Lazy UI Rendering**: Jetpack Compose `LazyColumn` and `LazyVerticalGrid` recycling memory for libraries with 10,000+ tracks.
4. **Artwork Memory & Disk Caching**: Coil 3-tier cache (Bitmap Memory Cache -> Disk File Cache -> Raw Audio Extraction) prevents repetitive decoding of embedded JPEG/PNG tags.
5. **WakeLock Discipline**: Acquire `PARTIAL_WAKE_LOCK` strictly when `playWhenReady == true` and release immediately when paused.
