# Research Deliverable 17: Phase 1.2.1 Residual Technical Wording & Evidence Correction Report

**Repository**: `https://github.com/mefahim/poweramp-clone`  
**Branch**: `main`  
**Target APK**: `poweramp-music-player-build-1035-uni.apk`  
**Original APK SHA-256**: `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f`  
**Starting Commit**: `0971ce4`  
**Baseline Commit**: `1e04203`  
**Phase Status**: **COMPLETE** (Documentation & Research QA Only)  

---

## 1. Executive Summary

Phase 1.2.1 was executed as a targeted, documentation-only residual technical wording and evidence correction pass following Phase 1.2. Its purpose was to resolve residual technical overstatements, unqualified audio engineering assertions, and evidence-classification ambiguities identified across the research documentation suite.

In adherence to the core reverse-engineering principle that **documentation must never claim more than available evidence proves**, Phase 1.2.1 established strict boundaries between five evidential categories:
1. **Declared**: Manifest entries, class names, intent filters, layout resources, or string identifiers present in the package.
2. **Statically Observed**: Decompiled Dalvik bytecode (`.dex`), JNI export signatures, public C/C++ symbol tables, or SQLite schemas.
3. **Runtime Observed**: Dynamically inspected process execution, logcat output, or runtime debugger captures.
4. **Hardware Verified**: Physical audio output, DAC clock synchronization, and HAL behavior validated on physical target hardware.
5. **Inferred**: Plausible engineering deductions clearly identified as hypothesis rather than direct proof.

All residual overstatements regarding "100% verified via static decompilation", "64-bit DSP pipeline", "1500 dB dynamic headroom", "universal 48kHz conversion", "bit-perfect equalization", and "guaranteed LGPL compliance via dynamic linking" have been eliminated. Zero production code was written, the original APK remains cryptographically pristine, and all 70 canonical feature classifications are strictly preserved.

---

## 2. Commit Provenance

| Milestone | Commit SHA | Description | Branch / Remote Status |
| :--- | :--- | :--- | :--- |
| **Baseline Milestone** | `1e0420395f73f13ebeb86e78cbcea09bc6b796e9` | Phase 1 Initial Research Baseline | Ancestor in History |
| **Starting QA Milestone** | `0971ce4af373bdcad50c29aaa77b892945605187` | Phase 1.2 Hardened Audit & Validation | Verified Parent (`HEAD~1`) |
| **Phase 1.2.1 Final Milestone** | `cc3d44973ae1a970df823811bc6e4b27918bab58` | `docs(qa): complete Phase 1.2.1 residual technical evidence corrections` | Branch `main`, Verified on `origin/main` (PASS) |

---

## 3. Files Modified

| File | Primary Nature of Correction |
| :--- | :--- |
| [`research/09-RUNTIME-OBSERVATIONS.md`](09-RUNTIME-OBSERVATIONS.md) | Corrected overbroad static verification language; clearly stated that 0 features are runtime-verified; reframed manual test plans as forward-looking protocols on independent test builds. |
| [`research/08-CODE-PATH-ANALYSIS.md`](08-CODE-PATH-ANALYSIS.md) | Clarified DSP precision: `Coefs.java` outputs 64-bit double coefficients (`double[][]`), PCM buffers are 32-bit float, internal native accumulator precision is an undocumented detail; distinguished IEEE 754 exponent span from mantissa SNR; replaced "100% verified" claims with "directly confirmed via bytecode". |
| [`research/03-TECH-STACK-ANALYSIS.md`](03-TECH-STACK-ANALYSIS.md) | Aligned audio precision descriptions; qualified USB DAC bit-perfect routing as dependent on physical driver paths. |
| [`research/10-INDEPENDENT-ARCHITECTURE.md`](10-INDEPENDENT-ARCHITECTURE.md) | Eliminated universal 48kHz claims; qualified direct output flags as HAL requests; clarified that bit-perfect playback requires bypassing DSP/EQ; audited Section 4 comparison matrix (relabeled latency, battery, and compatibility metrics as design targets and estimates). |
| [`research/12-RISK-AND-LICENSING.md`](12-RISK-AND-LICENSING.md) | Completely overhauled FFmpeg LGPL v2.1 Section 6 guidance; removed claims that dynamic linking alone guarantees compliance; documented user relinking/replacement obligations and patent pool risks. |
| [`research/15-FEATURE-AUDIT-VALIDATION.md`](15-FEATURE-AUDIT-VALIDATION.md) | Replaced legacy "48kHz fixed resampler" phrasing with HAL-dependent audio policy direct routing request description. |
| [`research/16-QA-CORRECTION-LOG.md`](16-QA-CORRECTION-LOG.md) | Refined AudioFlinger resampling descriptions; logged Phase 1.2.1 corrections with link to this deliverable. |
| [`PHASE-1-FINAL-REPORT.md`](../PHASE-1-FINAL-REPORT.md) | Qualified float headroom vs bit-perfect output; updated pipeline summaries to reflect audio policy realities. |
| [`README.md`](../README.md) | Added Research Deliverable 17 to document index; confirmed clean-room boundaries and design targets. |

---

## 4. Corrections Made by Batch

### Batch 0 — Repository Integrity & Baseline Verification
- Verified clean git working tree at starting commit `0971ce4`.
- Located original target APK `poweramp-music-player-build-1035-uni.apk` (24,481,502 bytes, ~23.35 MB).
- Cryptographically recalculated SHA-256 digest: `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f` (100% identical match to baseline).
- Confirmed all existing research documents are present and intact.
- Confirmed zero production application code exists in the repository.

### Batch 1 — Runtime Verification Language Correction ([`research/09-RUNTIME-OBSERVATIONS.md`](09-RUNTIME-OBSERVATIONS.md))
- Removed misleading assertion: *"100% of functional requirements have been verified via deep static decompilation"*.
- Replaced with explicit qualification: Static decompilation provides structural proof of bytecode and declarations, but cannot establish runtime execution correctness or physical hardware behavior.
- Documented that runtime execution and hardware verification for the target APK remain unverified (0 features runtime verified) due to headless container execution boundaries and anti-tamper mechanisms.
- Reframed all manual test suites (ADB commands, audio routing, billing tests) as forward-looking validation procedures for future clean-room test builds on authorized development hardware.

### Batch 2 — DSP Precision & Floating-Point Evidence Correction ([`research/08-CODE-PATH-ANALYSIS.md`](08-CODE-PATH-ANALYSIS.md), [`research/03-TECH-STACK-ANALYSIS.md`](03-TECH-STACK-ANALYSIS.md))
- Removed the unsupported claim that *"filter state computation in 64-bit double precision is confirmed by the double[][] JNI argument"*.
- Clarified the exact boundary of observed evidence:
  - `com.maxmpz.audioplayer.player.equalizer.Coefs.java` outputs `double[][] outCoefs`, confirming that filter coefficient calculation produces 64-bit IEEE 754 double precision.
  - Native audio buffers stream 32-bit floating-point PCM (`float`).
  - The internal precision of downstream filter state delay lines, biquad accumulators, and proprietary SIMD assembly instructions inside `libpowerampcore.so` is an undocumented native internal detail that cannot be proven from Java JNI signatures alone.
- Replaced unqualified statements of "1500 dB headroom" with clear mathematical distinctions: IEEE 754 32-bit float provides a theoretical exponent dynamic span exceeding 1500 dB ($10^{-38}$ to $10^{38}$), but instantaneous signal-to-noise ratio is bounded by its 24-bit mantissa (~144 dB), and physical DAC dynamic range is governed by hardware analog circuitry.

### Batch 3 — AudioFlinger, Direct Output, & Sample-Rate Claims ([`research/08-CODE-PATH-ANALYSIS.md`](08-CODE-PATH-ANALYSIS.md), [`research/10-INDEPENDENT-ARCHITECTURE.md`](10-INDEPENDENT-ARCHITECTURE.md), [`research/03-TECH-STACK-ANALYSIS.md`](03-TECH-STACK-ANALYSIS.md), [`PHASE-1-FINAL-REPORT.md`](../PHASE-1-FINAL-REPORT.md))
- Removed erroneous assertions that Android universally resamples all audio to 16-bit 48kHz.
- Documented that Android's audio routing is controlled by device audio policy and HAL rules; `AudioFlinger` resamples audio to match the HAL mixer rate, which is commonly (but not universally) 48kHz.
- Clarified that `AUDIO_OUTPUT_FLAG_DIRECT` and `AUDIO_OUTPUT_FLAG_RAW` are *requests* submitted by the application, which the framework and HAL may accept or decline depending on route, hardware capabilities, and concurrency.
- Corrected sample-rate conversion guidance: integer-multiple sample rates are not inherently distortion-free without appropriate bandlimited anti-aliasing filtering. Documented the necessity of querying `AudioDeviceInfo.getSampleRates()` and implementing high-quality sinc resampling.
- Explicitly stated that bit-perfect reproduction is mutually exclusive with active DSP/EQ (any non-flat gain or filtering alters digital samples).

### Batch 4 — FFmpeg LGPL Guidance & Risk Hardening ([`research/12-RISK-AND-LICENSING.md`](12-RISK-AND-LICENSING.md))
- Dismantled the misconception that building FFmpeg with `--disable-gpl --disable-nonfree` and loading it via `System.loadLibrary("ffmpeg")` alone guarantees LGPL compliance.
- Analyzed LGPL v2.1 Section 6 requirements: an application linking to an LGPL library must provide a practical mechanism allowing users to replace or modify the LGPL library and run the application with the modified library.
- Corrected the replacement mechanism analysis: on modern non-rooted Android, standard APK sandboxing prevents users from replacing shared libraries in `/data/app/`. Clarified that satisfying LGPL Section 6 requires distributing complete build scripts, toolchains, corresponding source code, and either dynamic loading from accessible storage or relinkable object code.
- Added explicit warnings regarding patent licensing exposure under MPEG LA and Via Licensing for AAC, MP3, and HEVC decoders, noting that copyright licenses (LGPL/BSD) do not grant patent indemnification.

### Batch 5 — Architecture Matrix & Numerical Claims Audit ([`research/10-INDEPENDENT-ARCHITECTURE.md`](10-INDEPENDENT-ARCHITECTURE.md))
- Inspected Section 4 trade-off comparison matrix and relabeled unmeasured metrics:
  - Latency: Replaced "Low to Medium (20–40ms)" and "Lowest (<10ms)" with clearly designated engineering targets: *"Estimated Standard: 50–100ms (buffer-dependent)"*, *"Design Target: Low (estimated 20–50ms)"*, and *"Theoretical Minimum (<10ms via AAudio exclusive mode, hardware-dependent)"*.
  - Battery Impact: Replaced unqualified "Lowest" / "Very Low" with *"Low (standard OS path; hardware DSP offloading when available)"* and *"Low to Moderate (software float DSP calculations on CPU)"*.
  - Device Compatibility: Replaced "99.5%" with *"Broad platform availability (Float AudioTrack supported in Android framework since API 21)"*.
  - Licensing: Replaced "Zero proprietary risk" with *"Clean-room open-source architecture (Apache 2.0 / MIT dependencies; external FFmpeg requires LGPL review)"*.

### Batch 6 — Cross-Document Consistency Audit
- Executed repository-wide automated regex scans across all Markdown documents for forbidden or misleading terms:
  - `"100% verified"` / `"fully verified"`: 0 active occurrences (all qualified as "directly confirmed via bytecode/prototypes").
  - `"1500 dB headroom"`: 0 active occurrences (all qualified as mathematical exponent range).
  - `"64-bit DSP pipeline"`: 0 active occurrences (coefficient calculation explicitly distinguished from PCM buffer streaming).
  - `"fixed 48kHz"`: 0 active occurrences (rephrased to reflect device HAL mixer rate and audio policy acceptance).
  - `"bit-perfect equalization"`: 0 active occurrences (bit-perfect mode confirmed as mutually exclusive with active DSP).
  - `"99.5%"`: 0 occurrences.
  - Legacy coarse classifications and root-level commands: 0 occurrences across research and operational documents.

### Batch 7 — QA Automation & Final Deliverable
- Automated Python QA test suite verified:
  - APK SHA-256 exact match.
  - Zero production application code.
  - Canonical 70-feature classifications exact match.
  - All 45 relative markdown links resolve.
- Generated this comprehensive correction report ([`research/17-PHASE-1.2.1-CORRECTION-REPORT.md`](17-PHASE-1.2.1-CORRECTION-REPORT.md)).
- Updated [`research/16-QA-CORRECTION-LOG.md`](16-QA-CORRECTION-LOG.md) and [`README.md`](../README.md).

---

## 5. Claims Removed or Qualified

| Previous Unqualified Claim | Reason for Removal / Revision | Revised Hardened Language |
| :--- | :--- | :--- |
| *"100% of functional requirements have been verified via deep static decompilation"* | Static bytecode analysis establishes structure and declarations, but cannot prove functional execution or hardware compatibility. | Qualified that static decompilation confirms implementation structure, while runtime execution and physical hardware behavior remain unverified (0 features runtime verified). |
| *"64-bit DSP pipeline confirmed by double[][] JNI argument"* | JNI parameter `double[][]` in `Coefs.java` proves only the coefficient calculation output; it does not prove internal accumulator or filter state register precision. | Stated that filter coefficient generation produces 64-bit double precision, PCM buffers stream in 32-bit float, while native accumulators inside `libpowerampcore.so` are undocumented native details. |
| *"1500 dB dynamic headroom prevents clipping"* | Confused IEEE 754 exponent representation range with real-world SNR and physical DAC dynamic range. | Clarified that float PCM provides scale-invariant intermediate arithmetic preventing internal digital clipping during multi-band boosts, but instantaneous mantissa SNR is ~144 dB and DAC analog dynamic range is hardware-bounded. |
| *"Android's AudioFlinger resamples all audio to 16-bit 48kHz"* | AudioFlinger mixing rate is determined by HAL configuration and audio policy; it is not hardcoded to 48kHz on all devices. | Described AudioFlinger as resampling to match the hardware mixer rate configured by HAL/policy (commonly 48kHz), and direct output flags as requests subject to HAL acceptance. |
| *"Bit-perfect parametric equalization"* | Mathematical contradiction: modifying frequency response or digital gain alters sample values, violating bit-perfect reproduction. | Confirmed that bit-perfect reproduction is strictly mutually exclusive with active DSP/EQ, and applies only to bit-perfect pass-through to external USB DACs when all DSP effects are bypassed. |
| *"Dynamic linking and --disable-gpl guarantee FFmpeg LGPL compliance"* | LGPL v2.1 Section 6 requires practical user relinking/replacement mechanisms and complete corresponding source distribution. | Documented user relinking requirements, packaging challenges on modern Android sandboxes, and patent pool risks under MPEG LA / Via Licensing. |
| *"99.5% device compatibility"* | Synthetic statistical figure not backed by empirical device fleet testing. | Replaced with *"Broad platform availability (Float AudioTrack supported in Android framework since API 21)"*. |
| *"Latency <10ms / 20–40ms"* | Unqualified ms figures implied empirical runtime measurements. | Labeled as *"Design Target: Low (estimated 20–50ms)"* and *"Theoretical Minimum (<10ms via AAudio exclusive mode, hardware-dependent)"*. |

---

## 6. Evidence Limitations That Remain

The following technical boundaries remain unresolved due to physical environment constraints:
1. **Physical Hardware Audio Routing**:
   - The behavior of vendor-specific audio HAL implementations (Qualcomm Snapdragon Sound, Samsung SoundAlive / UHQA, MediaTek Hi-Res) cannot be dynamically tested in a headless Linux environment.
   - Real-world USB DAC enumeration, descriptor negotiation, and asynchronous Isochronous USB audio streaming require physical Android devices and hardware DACs.
2. **Proprietary Native DSP Internals**:
   - `libpowerampcore.so` contains compiled ARM64 assembly with proprietary hand-tuned vector NEON routines. While public exported symbols and JNI entry points are cataloged, the internal register bit-depth of accumulators during recursive IIR filter execution remains unverified decompiled machine code.
3. **Physical Power & CPU Utilization**:
   - Software 32-bit float DSP processing vs hardware-accelerated DSP offloading battery consumption metrics cannot be empirically benchmarked without physical hardware power instrumentation.

---

## 7. QA Checks Executed and Actual Results

| QA Check Category | Target Criteria | Actual Result | Status |
| :--- | :--- | :--- | :--- |
| **APK Cryptographic Digest** | SHA-256: `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f` | Hash recalculated from file; identical match | **PASS** |
| **Production Code Boundary** | 0 `.kt`, `.java`, `.gradle`, `.c`, `.cpp` files outside `evidence/` | 0 files found | **PASS** |
| **Feature Inventory Counts** | Total: 70 (13 Static, 55 Unverified, 1 Partial, 1 Inferred, 0 Runtime) | Exactly 13 / 55 / 1 / 1 / 0 = 70 | **PASS** |
| **Screen Inventory Counts** | Total: 66 mapped screens across 6 categories | Exactly 8 / 22 / 6 / 8 / 5 / 17 = 66 | **PASS** |
| **Markdown Link Integrity** | All internal relative links must resolve to existing files | 76 relative links scanned across 20 Markdown documents (reconciled: 45 baseline links + 31 added across Deliverables 16, 17, and README); 0 broken links | **PASS** |
| **Unqualified Wording Scan** | 0 active instances of forbidden unqualified phrases | 0 active matches across all docs | **PASS** |
| **Security & Root Bypass** | 0 root commands (superuser execution), 0 clock tampering, 0 DRM bypass | Completely eliminated | **PASS** |

---

## 8. Confirmation of Unchanged Feature Classification

All 70 features cataloged in [`research/04-FEATURE-INVENTORY.md`](04-FEATURE-INVENTORY.md) and [`research/14-EVIDENCE-MATRIX.md`](14-EVIDENCE-MATRIX.md) maintain their strict canonical classifications established in Phase 1.2:

```
Total Features: 70 (100%)
├── STATIC_IMPLEMENTATION_CONFIRMED:      13 (18.6%)  [C.01-C.03, C.05-C.11, F.01, F.03, G.07]
├── IMPLEMENTED_BUT_RUNTIME_UNVERIFIED:   55 (78.6%)  [A.01-A.12, B.01-B.14, C.12, D.01-D.05, E.01-E.07, F.02, F.04, F.05, G.01-G.05]
├── PARTIALLY_IMPLEMENTED:                 1 ( 1.4%)  [C.04 AutoEq profile search]
├── INFERRED:                              1 ( 1.4%)  [G.06 Touch gesture velocity thresholds]
└── RUNTIME_VERIFIED:                      0 ( 0.0%)
```

No classifications were altered, downgraded, or fabricated during Phase 1.2.1.

---

## 9. Original APK Baseline Status

- **Location**: `./poweramp-music-player-build-1035-uni.apk`
- **File Size**: `24,481,502 bytes` (~23.35 MB)
- **Integrity Status**: Cryptographically verified via independent SHA-256 calculation.
- **SHA-256 Digest**: `3feff56552e2d3eeb4f58dc93b33db3d9895e22ad7638cd9feda3fa162cd307f`
- **Tampering / Repackaging**: None. The original APK has not been patched, modified, repackaged, or redistributed.

---

## 10. Confirmation of Zero Production Code

- No Android Studio project was created or initialized.
- No Gradle build scripts (`build.gradle`, `build.gradle.kts`, `settings.gradle`) were created.
- No Kotlin (`.kt`) or Java (`.java`) application source files were written.
- No Android XML layouts, drawables, or UI resources were added.
- All modifications are strictly restricted to Markdown research documentation and QA validation files.

---

## 11. Remaining Unresolved Technical or Legal Questions

1. **FFmpeg Distribution Architecture**:
   - Whether the clean-room implementation should include custom FFmpeg builds (and adopt the corresponding LGPL v2.1 Section 6 source/relinking distribution obligations) or rely entirely on AndroidX Media3 platform decoders plus permissive external codecs (e.g. libflac, libopus).
2. **Codec Patent Pool Exposure**:
   - Assessment of patent licensing liabilities under MPEG LA (MPEGLA/Via Licensing) for MP3/AAC decoders when distributed across commercial app stores in applicable jurisdictions.
3. **Android 14+ Foreground Service Policy**:
   - Qualification of Google Play policy requirements for continuous background playback using `foregroundServiceType="mediaPlayback"` and associated notification controls.

---

## 12. Explicit Boundary Statement

> [!IMPORTANT]
> **MANDATORY PHASE BOUNDARY**:
> Phase 1.2.1 is strictly completed. **Phase 2 (UI/UX design, Compose layouts, and implementation) has NOT started and is awaiting explicit user approval.** No UI wireframes, Compose components, or application source code have been produced.
