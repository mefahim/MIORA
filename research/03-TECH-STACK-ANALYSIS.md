# 03 — Tech Stack Analysis: Target App vs. Independent Modern Android Stack

## 1. Architectural Paradigms Comparison

```
Original Poweramp Stack               Proposed Modern Clean-Room Stack
┌─────────────────────────────────┐   ┌─────────────────────────────────┐
│ Java + Obfuscated Bytecode      │   │ 100% Modern Kotlin 2.x          │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Custom FastLayout / C++ OpenGL  │   │ Jetpack Compose + Material 3    │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Proprietary MsgBus / StateBus   │   │ Coroutines, StateFlow & SharedFlow│
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Custom libpowerampcore.so DSP   │   │ AndroidX Media3 / ExoPlayer     │
│ + libffmpeg_neon.so (C++)       │   │ + Oboe (AAudio/OpenSL ES) C++ DSP│
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Manual SQLiteOpenHelper (v251)  │   │ AndroidX Room DB + SQLite WAL   │
│ in folders.db                   │   │ + FTS5 Full-Text Search         │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Legacy SharedPreferences        │   │ Jetpack DataStore (Type-Safe)   │
├─────────────────────────────────┤   ├─────────────────────────────────┤
│ Android Support MediaCompat     │   │ Media3 MediaSessionService      │
└─────────────────────────────────┘   └─────────────────────────────────┘
```

---

## 2. Deep Dive: Target Application Tech Stack

### A. Audio Engine & Signal Processing
- **Engine Core**: Fully custom native C++ engine compiled into `libpowerampcore.so`.
- **Bit Depth / Precision**: Double-precision (64-bit `double`) coefficient calculation (`Coefs.java`) and floating-point PCM sample buffers in native DSP core.
- **Audio Output Plugins**:
  - `AaPluginService`: AAudio (Android NDK low-latency audio API).
  - `AtPluginService`: Standard Java `AudioTrack` output.
  - `AthdPluginService`: AudioTrack Hi-Res (Attempts to request direct output flags from device audio policy; whether direct routing is granted depends on the device HAL and vendor audio policy, requiring physical hardware testing).
  - `OslPluginService`: OpenSL ES native output.
  - `UsbxPluginService`: USB Audio host mode intended to communicate directly with external USB audio endpoints; actual bit-perfect routing requires verifying the device USB audio driver path without intervening OS resampling.
  - `CcPluginService`: Google Cast / Chromecast audio streaming.
- **Decoding Pipeline**:
  - `libffmpeg_neon.so`: Custom build of FFmpeg with ARM NEON SIMD optimizations for decoding MP3, AAC, FLAC, ALAC, APE, OPUS, OGG Vorbis, WavPack, Musepack, and DSD (DSF/DFF).
  - NDK AMediaExtractor / AMediaCodec for hardware-assisted decoding.
- **Equalizer & Effects**:
  - Configurable 10 to 32 bands graphic/parametric equalizer.
  - Bi-quad IIR filter coefficient calculations handled via native JNI calls (`Coefs.java`, `PeqNativeUtils.java`).
  - Tone controls: Independent Bass and Treble shelving filters.
  - Effects: Stereo Widening / Virtualizer, Reverb (Room Size, Damping, Width, Wet/Dry), Tempo/Pitch shifting, and Direct Volume Control (DVC).

### B. User Interface & Rendering Engine
- **Paradigm**: Custom immediate-mode/scenegraph rendering framework (`com.maxmpz.widget.FastLayout` and `SceneFastLayout`).
- **Rationale**: Built prior to Jetpack Compose to achieve solid 60/120 fps animations on legacy Android devices (Android 2.x to 8.x) where standard Android Views suffered from heavy layout measurement passes.
- **Visualization**: Embedded Milkdrop visualizer engine (fork of `projectM`) rendering via OpenGL ES 2.0/3.0 directly onto `MilkSurfaceView`.
- **Waveform Rendering**: Pre-calculates 100-sample RMS waveforms during track scanning, stored as BLOBs in SQLite and rendered via custom canvas shaders.

### C. Data Persistence & Scanning
- **Database**: SQLite database named `folders.db` with manual migrations across 251 versions in `ar.java` and `po0.java`.
- **Search**: `pref_search_fts` virtual table using SQLite FTS4 tokenizer `unicode61`.
- **Scanner**: Dual scanning mechanism:
  1. `StorageAccessFramework` (SAF) document tree scanner for Android 11+ scoped storage.
  2. POSIX direct filesystem scanner (`Dirent`) for root/unrestricted storage paths.
  3. CUE Sheet parser (`CueFileBatch`) supporting multi-track cue points inside single large audio files.
  4. Native tag readers (`FFMpegTagReader`, `ModTagReader`, `TagReader`).

---

## 3. Recommended Independent Modern Android Stack

To build a clean-room, maintainable, modern, high-performance music player without proprietary lock-in, the following modern Android stack is recommended:

### A. Core Architecture & Language
- **Language**: **Kotlin 2.0+** with Coroutines, Flow, and Kotlin Serialization.
- **Architecture**: **Clean Architecture + Unidirectional Data Flow (MVI/MVVM)**.
- **Dependency Injection**: **Google Hilt / Dagger** for clean dependency graphs and testability.
- **Asynchronous Execution**: Coroutines with structured concurrency (`Dispatchers.IO` for file/db, `Dispatchers.Default` for audio math, `Dispatchers.Main.immediate` for UI).

### B. Audio Playback & Pipeline Architecture
- **Framework Core**: **AndroidX Media3 (ExoPlayer)**:
  - Modern replacement for `MediaSessionCompat` and legacy ExoPlayer.
  - Standardized `MediaSessionService` handling background lifecycle, notifications, media buttons, Bluetooth disconnects, and lock screen controls out of the box.
- **High-Resolution / Low-Latency Option**:
  - **Tier 1 (Universal Native)**: AndroidX Media3 with custom `AudioSink` utilizing Java `AudioTrack` in float PCM mode (supporting 24-bit/32-bit float up to 192 kHz).
  - **Tier 2 (Pro Audiophile Extension)**: **Google Oboe C++ Library**:
    - High-performance, open-source C++ audio library wrapping AAudio and OpenSL ES.
    - Low-latency callback model with NEON-accelerated floating-point DSP pipeline.
- **Audio Effects**:
  - Android standard `android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`, `EnvironmentalReverb`, and `LoudnessEnhancer`.
  - Independent Custom Software Equalizer: Clean-room 10-band / 30-band Biquad IIR filter implementation (using Robert Bristow-Johnson Audio EQ Cookbook equations) running in pure Kotlin / C++ for high-precision floating-point equalization without Android system audio effect limitations.

### C. UI & Visual Design System
- **Framework**: **Jetpack Compose + Material 3 (Material You)**:
  - Declarative UI with state hoisting.
  - Native dynamic color adaptation, smooth fluid animations, and gesture support.
- **Waveform Seekbar**: Custom Jetpack Compose Canvas component sampling audio amplitude data.
- **Visualizer**: OpenGL ES 2.0/3.0 SurfaceView using open-source visualizer algorithms (FFT frequency spectrum and waveform oscilloscopes via `Visualizer` API).
- **Image Loading**: **Coil (Compose Extension)** for asynchronous, memory-cached album art rendering.

### D. Data Storage & Library Scanning
- **Database**: **AndroidX Room 2.6+**:
  - Compile-time SQL query validation and type-safe migrations.
  - SQLite Write-Ahead Logging (WAL) for concurrent background scanning and UI reads.
  - Room **FTS4 / FTS5** integration for instant, fuzzy library and settings search.
- **Settings / Preferences**: **Jetpack DataStore (Preferences)**:
  - Asynchronous, transactional, coroutine-based replacement for legacy `SharedPreferences`.
- **Media Ingestion**:
  - `MediaStore.Audio` content observer for rapid Android library synchronization.
  - Storage Access Framework (SAF) `DocumentFile` scanner for direct user-selected folders and `.lrc` external subtitle detection.

---

## 4. Side-by-Side Architectural Evaluation

| Evaluation Dimension | Poweramp Original Implementation | Proposed Independent Clean-Room Implementation | Feasibility Assessment |
| :--- | :--- | :--- | :--- |
| **Development Complexity** | Extremely High (15+ years of custom C++ DSP, custom UI engine, manual JNI) | Moderate to High (Standardized Jetpack & Media3 components + focused audio engine) | High |
| **Maintainability** | Low (Large obfuscated C++ codebase, proprietary scene graph) | Very High (Standard idiomatic Kotlin, Compose, Room, Hilt) | High |
| **Android Version Support** | Android 5.0 to 16 | Android 8.0 (API 26) to Android 16 (API 36) | Ideal |
| **Audio Processing Precision** | Floating-point DSP (64-bit double coefficient calculation, float PCM sample buffers) | 32-bit Float PCM, Media3 AudioTrack Float / Oboe Hi-Res | Minimizes internal quantization distortion and provides scale-invariant intermediate arithmetic |
| **UI Fluidity** | 60-120 fps via custom OpenGL/Canvas | 60-120 fps via Jetpack Compose Hardware Acceleration | Matches |
| **Licensing Risks** | Proprietary C++ engine | Clean-room open-source architecture (Apache 2.0 / MIT dependencies; external FFmpeg requires LGPL review) | Low IP Risk (clean-room developed) |
