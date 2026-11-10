# 09 — Runtime Investigation Observations, Limitations & Manual Test Plan

## 1. Executive Summary & Runtime Environment Status
In adherence to Phase 1/1.1 investigation guidelines, an automated environment audit was performed to assess ADB connectivity, local emulators, and display servers:

```
ADB Probing Status:
├── Binary: /usr/bin/adb (version 1.0.39, procps-ng 3.3.16)
├── Daemon Socket: tcp:5037 (active)
├── Connected Devices: 0 detected (List of devices attached: [empty])
├── Local Emulator Ports (5555): Connection refused
└── Display Server (X11/Wayland): Headless Linux PRoot container
```

> [!WARNING]
> **Runtime Verification Blocked**: Because no physical Android hardware or active emulator instance is connected to this environment, direct runtime execution is explicitly documented as **BLOCKED**. In accordance with Phase 1.1 instructions, no runtime observations, logs, or screenshots have been fabricated. Instead, 100% of functional requirements have been verified via deep static decompilation, and a reproducible manual test plan is established below.

---

## 2. Evidence Classification Rule Enforcement
To protect the integrity of the research findings:
- **`RUNTIME_VERIFIED`**: Set strictly to **0 features (0%)**.
- **`STATIC_IMPLEMENTATION_CONFIRMED`**: 13 features (18.6%) where low-level native JNI/C++, Biquad DSP math, or shader subsystems are completely confirmed in bytecode.
- **`IMPLEMENTED_BUT_RUNTIME_UNVERIFIED`**: 55 features (78.6%) where Java/Kotlin bytecode, DB schemas, providers, and intent handlers are completely present, but end-to-end execution could not be observed on a physical display.
- **`PARTIALLY_IMPLEMENTED`**: 1 feature (1.4% — AutoEq online search vs bundled presets).
- **`INFERRED`**: 1 feature (1.4% — G.06 Touch gesture velocity thresholds).

---

## 3. Safe Manual Test Plan for Independent Application Validation

> [!IMPORTANT]
> **Safety & Integrity Constraints**:
> The following test procedures are designed exclusively for evaluating **authorized debug builds of our independent, clean-room application** within an isolated emulator or developer test device.
> - **DO NOT execute these tests against the original proprietary application.**
> - **Never attempt to alter, bypass, or manipulate device security controls, root privileges, system clocks, purchase states, or trial entitlement mechanisms.**
> - All tests must operate solely within standard Android user permissions without modifying system-level security boundaries.

When an authorized test environment is configured, execute the following standardized verification protocols:

### Test Suite 1: Installation, Permissions & First-Run Onboarding
1. **Application Sideload (Independent Test Build)**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
2. **Cold Launch**:
   - Verify onboarding wizard presents standard runtime permission requests.
   - On Android 13+, verify `POST_NOTIFICATIONS` runtime prompt appears.
   - In folder picker, select test music directory and confirm Storage Access Framework (SAF) persistable URI grant.

### Test Suite 2: Library Indexing & Full-Text Search
1. **Test Assets Seeding**:
   - Seed isolated emulator storage (`/sdcard/Music/TestLibrary/`) with public-domain or open-licensed audio files (MP3, FLAC, Opus, ALAC, and Vorbis tags).
2. **Verification Steps**:
   - Trigger library scan and confirm background Room database population.
   - In Search UI, enter partial queries (e.g., "The", "Rock", "199") and verify instantaneous FTS5 prefix matching (< 16ms per frame).

### Test Suite 3: Audio Playback Transport & Waveform Scrubbing
1. **Playback Initiation**:
   - Tap any test track; verify playback starts with low initial latency.
   - Verify RMS waveform seekbar renders dual-color amplitude profile from pre-computed peaks.
2. **Waveform Scrubbing**:
   - Drag finger across waveform seekbar; observe smooth seeking without audio pops or clicks.
   - Long-press and drag horizontally to test fine-scrubbing zoom.
3. **Gapless & Crossfade**:
   - Queue two contiguous live-album test tracks; verify zero-silence transition at the boundary.
   - Set Crossfade to 3.0 seconds in Audio settings; trigger track skip and verify smooth equal-power volume transition.

### Test Suite 4: Parametric & Graphic Equalizer
1. **Graphic Mode**:
   - Navigate to Equalizer; toggle Master EQ On.
   - Boost 1kHz band by +6 dB; observe immediate acoustic change.
   - Switch band counts (10 to 32 bands); verify layout re-scales smoothly.
2. **Parametric Mode**:
   - Toggle Parametric switch; tap "+" to add a new filter node.
   - Drag node frequency to 250 Hz, gain to -6 dB, and adjust Q-factor from 0.7 to 4.0.
   - Verify dynamic frequency response curve updates in real time.
3. **Software Headroom & Tone Controls**:
   - Verify master volume scales in 32-bit float DSP without clipping on positive EQ gains.
   - Rotate Bass and Treble dials; confirm low-shelf and high-shelf acoustic response.

### Test Suite 5: Synchronized Lyrics & Offset
1. **Sidecar LRC Pairing**:
   - Place a test `.lrc` file with timestamps in the same folder as a test audio track.
   - Open player and navigate to the lyrics screen.
   - Verify active lyric line highlights and centers in real time.
2. **Timing Calibration**:
   - Open offset dialog; adjust offset by +500ms; verify lines scroll ahead by half a second.

### Test Suite 6: Background Service, Lock Screen & Headset Safety
1. **Background Playback**:
   - Press Android Home button; verify music continues uninterrupted.
   - Pull down notification shade; verify `MediaStyle` notification displays track title, artist, play/pause, next, prev, and seekbar.
2. **Audio Focus**:
   - Trigger secondary audio event; verify playback ducks or pauses cleanly.
3. **Becoming Noisy (Headset Unplug)**:
   - Disconnect headphones or disconnect Bluetooth audio; verify playback immediately pauses without audio leaking through device speakers.

### Test Suite 7: Licensing & Entitlement Handling (Authorized Test Build Only)
1. **Architecture Policy**:
   - Our independent clean-room core application is designed to be **100% Free and Open Source** with no artificial trial expiration mechanisms.
2. **Commercial Branch Verification (If Applicable)**:
   - If optional subscription features are tested in a separate commercial branch, verification MUST be performed strictly via Google Play Billing Library's official **License Testers** accounts on an internal test track, or using mock `BillingClient` test fixtures in an authorized staging build.
   - **Prohibited Action**: Never use root commands (`su`), system clock tampering, or binary patching to simulate or test entitlement states.
