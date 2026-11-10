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

## 2. Open Source Licensing & FFmpeg Compliance Audit

### A. FFmpeg Licensing, Evidence & Distribution Obligations
*(References: Official FFmpeg Legal Guidelines — https://www.ffmpeg.org/legal.html)*

#### 1. Forensic Evidence in Target APK
- **Analyzed Artifact**: `lib/arm64-v8a/libffmpeg_neon.so` (2,220,424 bytes, ELF 64-bit LSB dynamic shared object).
- **Dynamic Library Dependencies (`readelf -d`)**: `libm.so`, `liblog.so`, `libz.so`, `libc.so`, `libdl.so`.
- **Exported Core Symbols**: `avcodec_alloc_context3`, `avformat_open_input`, along with specialized demuxers (`ff_aac_demuxer`, `ff_dsf_demuxer`, `ff_mpc_demuxer`, `ff_matroska_demuxer`, `ff_pcm_*_demuxer`).
- **Packaging Topology**: Monolithic shared object combining stripped audio-focused demuxers and decoders (`libavcodec`, `libavformat`, `libavutil`) into a single binary.

#### 2. Legal Realities: Why Build Flags & Dynamic Linking Alone Do Not Guarantee Compliance
> [!CAUTION]
> It is an engineering misconception that merely specifying `--disable-gpl` and `--disable-nonfree` during compilation, and loading a shared `.so` library via `System.loadLibrary("ffmpeg")`, automatically guarantees full legal compliance.
> Under official FFmpeg licensing guidelines (`https://www.ffmpeg.org/legal.html`) and the LGPL v2.1 text, dynamic linking is only one architectural element of a potential compliance strategy. Full compliance requires fulfilling continuous source distribution obligations, auditing all enabled components, and providing a legally and technically valid library replacement mechanism suitable for the specific distribution channel.

#### 3. Specific Compliance Obligations & Engineering Risk Factors

| Obligation / Risk Area | Specific Requirement under LGPL v2.1 | Implementation & Verification Action |
| :--- | :--- | :--- |
| **GPL Component Contamination** | Must not include GPL-licensed FFmpeg modules (e.g., `postproc`, `swscale` filters using GPL code, `libx264`, `libx265`, `frei0r`). | Configure with `--disable-gpl --disable-version3`. Audit all enabled decoders/demuxers against FFmpeg's GPL component catalog. |
| **Non-Free Library Exclusion** | Must not link against unredistributable or non-free libraries (e.g., Fraunhofer `fdk-aac`, non-free crypto). | Audit dynamic symbols with `readelf -d` and `nm` to confirm zero non-free external symbols. Rely on LGPL-permissive internal decoders (e.g., native FFmpeg AAC decoder). |
| **External Dependency Chaining** | Dynamic dependencies (`libz.so`, `libc.so`, `libm.so`) must remain strictly under permissive or system licenses. | Dynamic section audit confirms target APK links only against Android system Bionic libc/libm/libdl/libz. |
| **Corresponding Source Code Delivery** | LGPL v2.1 Section 6 requires distributors to preserve and provide the exact matching source code, including all modifications, patches, configuration scripts, and build environments used. | Host the complete corresponding source tree and exact compilation container/Dockerfile on the project repository's public release page for every distributed build. |
| **User Library Replacement Mechanism** | LGPL v2.1 Section 6 requires that recipients have the technical and legal capability to modify the library and run the application with the modified library. | **Unresolved Legal & Architectural Question**: On Android, APK signature verification, dm-verity, and scoped application sandboxing restrict users from easily swapping out shared libraries. Presenting root access (`adb root`) or manual APK deconstruction/resigning as a universal legal solution is legally untested and controversial across different jurisdictions. The complete application packaging, dynamic loader architecture, and target distribution model (e.g. sideloaded APK vs Google Play) must be evaluated with professional legal counsel. |
| **Patent Pool Licensing (MPEG LA / Via)** | Copyright licenses (LGPL/GPL) grant permissions under copyright law, but **do NOT grant patent indemnification**. Standard audio codecs (AAC, MP3) involve patent pools in certain jurisdictions. | **Item requiring qualified legal review**: Software distribution of patent-encumbered decoders in certain territories (such as the US) carries patent exposure regardless of LGPL compliance. While delegating decoding to the Android platform's built-in `MediaCodec` framework decoders avoids bundling third-party patented software routines, relying on platform codecs does not automatically indemnify against all potential system-level claims. Patent exposure in target distribution markets must be formally assessed by intellectual property counsel. |

#### 4. Baseline LGPL v2.1 Build Configuration Blueprint
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
  --disable-programs \
  --disable-doc \
  --disable-everything \
  --enable-decoder=flac,alac,ape,opus,vorbis,wavpack,mp3,aac \
  --enable-demuxer=flac,mov,ape,ogg,wavpack,mp3,aac \
  --enable-parser=flac,opus,vorbis,mpegaudio,aac
```

---

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
