# 12 — Risk Assessment, Licensing Compliance & Clean-Room Boundaries

## 1. Clean-Room Reverse Engineering Legal Framework
In accordance with international software copyright laws (including Section 1201(f) of the US Digital Millennium Copyright Act (DMCA) and EU Directive 2009/24/EC on the legal protection of computer programs), reverse engineering for the purpose of achieving interoperability and independent functional specification is legally recognized when strict clean-room protocols are maintained.

### Strict Boundaries for the Independent Project
1. **Zero Proprietary Code Reuse**: Absolutely no decompiled smali, Java, C++, or assembly instructions from `com.maxmpz.audioplayer` or obfuscated `p000.*` may be copied, pasted, or directly translated into the new project codebase.
2. **Zero Proprietary Asset Reuse**: No icons, vector drawables, audio sound effects, shaders, or visual themes from the APK may be included. All UI assets must be independently designed or sourced from permissive libraries (e.g. Google Material Symbols under Apache 2.0).
3. **Trademark & Branding Non-Infringement**: The application must not use the trademarks "Poweramp", "Max MPZ", or confusingly similar naming, iconography, or trade dress. The independent project will use a distinct brand identity (e.g., *Resonate Music Player*).
4. **Independent Functional Specification**: All algorithms (such as biquad filter calculation and LRC parsing) must be implemented strictly from publicly available scientific literature and standards (e.g., the Robert Bristow-Johnson Audio EQ Cookbook).

---

## 2. Open Source Licensing & Build Configuration Audit

### A. FFmpeg Licensing & Build Strategy
- **Role in Project**: Decoding audiophile formats (FLAC, ALAC, Opus, APE, WavPack, DSD).
- **License**: **LGPL v2.1+** (or GPL v2+/v3+ depending on build flags).
- **Required Build Configuration**:
  To prevent "GPL viral infection" of the application codebase, FFmpeg MUST be compiled strictly under LGPL v2.1:
  ```bash
  ./configure \
    --enable-cross-compile \
    --target-os=android \
    --arch=arm64 \
    --disable-gpl \
    --disable-nonfree \
    --disable-version3 \
    --enable-shared \
    --disable-static \
    --enable-small \
    --disable-programs \
    --disable-doc \
    --disable-everything \
    --enable-decoder=flac,alac,ape,opus,vorbis,wavpack,mp3,aac \
    --enable-demuxer=flac,mov,ape,ogg,wavpack,mp3,aac
  ```
- **Compliance Obligations**:
  - Build FFmpeg as dynamically linked shared libraries (`libffmpeg.so`, `libavcodec.so`, etc.).
  - Comply with LGPL v2.1 Section 6 by providing build scripts and source tarballs enabling end-users to recompile and replace the `.so` binaries.
  - Do NOT statically link FFmpeg into the application executable.

### B. projectM Visualizer Licensing
- **Role in Project**: OpenGL Milkdrop visualizer engine.
- **License**: **LGPL v2.1+**.
- **Compliance Obligations**:
  - Build projectM as a dynamic shared library (`libprojectM.so`).
  - Provide source code for any modifications to the core library.
  - Preset scripts (`.milk` files) are individually authored; bundled presets must be sourced from open-source repositories with permissive licenses.

### C. Permissive Dependencies (Apache 2.0 / MIT)
- **AndroidX Media3 (ExoPlayer)**: Apache 2.0 (Google). Retain copyright notices.
- **AndroidX Jetpack & Room**: Apache 2.0 (Google). Retain copyright notices.
- **Google Hilt / Dagger**: Apache 2.0 (Google).
- **Coil Image Loader**: Apache 2.0.
- **AutoEq Database**: MIT License (Jaakkopasanen). Attribution required in application settings.

---

## 3. Android Security, Scoped Storage & Permission Audit

### A. Android Scoped Storage & `MANAGE_EXTERNAL_STORAGE`
- **Audit Finding**: Poweramp requests `android.permission.MANAGE_EXTERNAL_STORAGE` (All Files Access).
- **Policy Risk**: Google Play policy strictly restricts `MANAGE_EXTERNAL_STORAGE` to file managers, antivirus, and backup utilities. Music players requesting this permission face immediate rejection or removal.
- **Independent Compliance Strategy**:
  1. Primary: Use Android 13+ `android.permission.READ_MEDIA_AUDIO` and legacy `READ_EXTERNAL_STORAGE` (API < 33).
  2. Extended Storage (CUE sheets, `.lrc` files, custom folders): Utilize the **Storage Access Framework (SAF)** via `Intent.ACTION_OPEN_DOCUMENT_TREE`. When the user explicitly picks their music directory, the app takes a persistable URI permission (`takePersistableUriPermission`), gaining persistent recursive read/write access without requiring `MANAGE_EXTERNAL_STORAGE`.

### B. Android 14+ Foreground Service Mandates
- **Audit Finding**: Background playback requires strict foreground service declarations.
- **Policy Compliance**:
  - Declare `<service android:name=".playback.PlaybackService" android:foregroundServiceType="mediaPlayback" />`.
  - Maintain an ongoing `MediaStyle` notification coupled with an active `MediaSession`.
  - Android OS automatically validates this type for audio playback applications.

### C. High-Risk / Specialized Permissions Review

| Permission | Declared in APK | Independent Project Strategy | Risk / Justification |
| :--- | :--- | :--- | :--- |
| `POST_NOTIFICATIONS` | Yes | **Required** (Android 13+) | Required to display playback controls in notification shade |
| `WAKE_LOCK` | Yes | **Required** | Prevents CPU sleep during background playback when screen turns off |
| `BLUETOOTH_CONNECT` | Yes | **Required** (Android 12+) | Queries Bluetooth device names to auto-restore per-device EQ presets |
| `MODIFY_AUDIO_SETTINGS` | Yes | **Required** | Manages volume levels and audio focus transitions |
| `SYSTEM_ALERT_WINDOW` | Yes | **Omit** | Poweramp uses for floating lyrics; omit to prevent Play Store security flags |
| `WRITE_SETTINGS` | Yes | **Omit** | Poweramp uses for ringtone setting; omit to maintain zero system modification |
| `moe.shizuku.manager` | Yes | **Optional / Future** | Optional plugin for advanced users; zero core dependence |

---

## 4. Privacy & Offline-First Guarantees
- **Zero Telemetry**: No third-party tracking, analytics, crash reporting SDKs (e.g., Firebase, AppLovin, adjust) that transmit user listening history or device identifiers.
- **Local Autonomy**: All database queries, full-text searches, and preset management run locally inside the device SQLite/Room database.
- **Network Access**: Restricted strictly to optional user-requested actions (e.g., downloading high-res album art or lyrics from user-configured endpoints).

---

## 5. Items Requiring Professional Legal Review

Before publishing or commercializing the application, professional legal counsel should review:
1. **Audio Codec Patent Licensing**: Certain legacy audio codecs (e.g., MP3/AAC) have expired patents in most jurisdictions, but specialized formats (e.g., Dolby AC-4, MPEG-H) may carry patent pool obligations (Via Licensing / MPEG LA) if custom software decoders are bundled instead of using Android OS system decoders.
2. **LGPL Dynamic Linking Verification**: Independent verification that dynamically linking `libffmpeg.so` on Android (via JNI) fully satisfies LGPL v2.1 requirements in target distribution markets.
3. **Trademark & Trade Dress Clearing**: Formal trademark search for the selected application name and logo icon.
