# MIORA NEXT — Intellectual Property & Open-Source License Audit

- **Audit Date:** November 12, 2026
- **Auditor:** Antigravity Engineering & Legal Compliance Team
- **Scope:** License compatibility, copyleft obligations, third-party attribution, and clean-room engineering boundaries across MIORA v0.1.0, MIORA v0.2.0, Metrolist 13.7.0, and MIORA Next.

---

## 1. Baseline License Status

### MIORA v0.1.0 (Stable Release)
- **Repository License:** **Apache License 2.0** (`LICENSE` in commit `03fed47`).
- **Characteristics:** Permissive, grants broad patent licenses, allows proprietary and commercial sublicensing, requires preservation of copyright notices and modification disclaimers.

### MIORA v0.2.0 (Local Dev on `v0.2-dev`)
- **Repository Commit:** Milestone M0 commit `d66cfc5` updated repository headers and `LICENSE` to **GNU General Public License v3.0 (GPL-3.0)** in preparation for incorporating InnerTube/Metrolist technologies.
- **Current State:** The local repository tree is licensed under GPL-3.0, but unpushed to `origin/main`.

### Metrolist 13.7.0 & Upstream Reference
- **Metrolist Repository:** Licensed under **GNU General Public License v3.0 (GPL-3.0)** (`MetrolistGroup/metrolist`).
- **InnerTubeX (`MetrolistGroup/innertubex`):** Licensed under **GNU General Public License v3.0 (GPL-3.0)**.
- **Embedded JavaScript & Native Binaries:**
  - `QuickJS` (`libquickjs.so`): MIT License (Fabrice Bellard and Charlie Gordon).
  - `Meriyah` (JavaScript parser): MIT License.
  - `Astring` (JavaScript generator): MIT License.

---

## 2. Third-Party Dependency License Inventory

| Dependency / Component | Origin / Vendor | Upstream License | Linking Type in MIORA | Copyleft Contagion Risk |
| :--- | :--- | :--- | :--- | :--- |
| **AndroidX Media3 (1.5.0 / 1.10.1)** | Google LLC | **Apache-2.0** | Static Maven AAR | None (Permissive) |
| **Jetpack Compose / Material3** | Google LLC | **Apache-2.0** | Static Maven AAR | None (Permissive) |
| **Kotlin Coroutines / Serialization**| JetBrains s.r.o. | **Apache-2.0** | Static Maven JAR | None (Permissive) |
| **Ktor HTTP Client (2.x / 3.x)** | JetBrains s.r.o. | **Apache-2.0** | Static Maven JAR | None (Permissive) |
| **OkHttp 4.x** | Square, Inc. | **Apache-2.0** | Static Maven JAR | None (Permissive) |
| **Room Database (2.6.x)** | Google LLC | **Apache-2.0** | Static Maven AAR | None (Permissive) |
| **Coil / Coil3** | Coil Contributors | **Apache-2.0** | Static Maven AAR | None (Permissive) |
| **LRCLIB Client** | Community / Open API| Open REST API | Dynamic HTTP JSON | None |
| **InnerTubeX** | MetrolistGroup | **GPL-3.0** | Direct Maven / Source | **HIGH (Strong Copyleft)** |
| **QuickJS Native Library** | Fabrice Bellard | **MIT** | Native Shared Library (`.so`) | None (Permissive with notice) |
| **YouTube Data API v3 Client** | Google LLC | **Apache-2.0** | Static Maven JAR | None (Permissive) |

---

## 3. Copyleft Contagion Analysis (GPL-3.0 Section 5)

Under Section 5 of the GNU General Public License v3.0:
> *"You may convey a work based on the Program, or the modifications to produce it from the Program, in the form of source code under the terms of section 4, provided that you also meet all of these conditions: [...] c) You must license the entire work, as a whole, under this License to anyone who comes into possession of a copy."*

### Legal Consequences for MIORA Next:
1. **Direct Compilation / Static Linking:**
   If MIORA directly compiles against `com.github.MetrolistGroup.innertubex:innertubex` or copies any Kotlin/Java code from `MetrolistGroup/metrolist`, Android's compilation model links all bytecode into a single Dalvik Executable (`classes.dex`). Under copyright law and the FSF interpretation of GPL-3.0, the compiled application constitutes a single derivative work.
2. **License Compatibility:**
   Apache-2.0 code can be incorporated into a GPL-3.0 project (the FSF considers Apache-2.0 compatible with GPL-3.0). However, **GPL-3.0 code CANNOT be incorporated into an Apache-2.0 project**. The overall resulting combined binary must be licensed under GPL-3.0.
3. **Implication:**
   Attempting to keep MIORA v0.1.0's Apache-2.0 license while directly embedding Metrolist or InnerTubeX source/binaries is legally prohibited.

---

## 4. Architectural Alternatives: Clean-Room vs Full GPL Transition

We evaluate two distinct legal pathways for MIORA Next:

### Option 1: Clean-Room Architecture with Modular Separation
- **Concept:** Keep the core MIORA application (UI, local playback engine, database, and settings) under the original **Apache-2.0** license. Separate online streaming into an optional, dynamically loaded plugin or separate companion application licensed under GPL-3.0.
- **Pros:** Preserves Apache-2.0 for the core player; protects the original copyright holders.
- **Cons:** Extremely complex Android build configuration; dynamic code loading (DCL) or inter-process communication (IPC) introduces latency, IPC serialization overhead, and Google Play security flags.

### Option 2: Full Project Relicensing to GPL-3.0 (Milestone M0 Approach)
- **Concept:** Standardize the entire MIORA Next repository under **GNU General Public License v3.0 (GPL-3.0)**, exactly as executed in commit `d66cfc5`.
- **Pros:**
  - 100% legally compliant with Metrolist, InnerTubeX, NewPipe, and open-source Android music player ecosystem standards.
  - Zero architectural friction: can directly utilize Media3 extensions, InnerTube models, and shared components without IPC boundaries.
  - Guarantees that MIORA Next remains perpetually open-source, protecting user freedom and preventing proprietary closed-source forks from exploiting the project.
- **Cons:** Sublicensing under Apache-2.0 is no longer possible for downstream consumers of the complete binary.

---

## 5. Concrete Recommendation & Compliance Protocol

1. **Ratify Full GPL-3.0 Standardization for MIORA Next:**
   - Formalize the license transition initiated in Milestone M0 (`d66cfc5`).
   - Retain Apache-2.0 copyright notices on pre-existing MIORA v0.1.0 files authored prior to the fork, appending the GPL-3.0 header to indicate the combined work's license.
2. **Strict Clean-Room Implementation Rules:**
   - Even under GPL-3.0, **do not blindly copy-paste monolithic God classes** from Metrolist (e.g. do NOT copy the 3,950-line `MusicService.kt`).
   - Adhere strictly to MIORA's clean MVVM/MVI architecture, Coroutines/Flow state management, and modular domain repositories.
   - Utilize InnerTubeX strictly as an external library dependency or build a clean-room resolver that interfaces with standard AndroidX Media3 interfaces (`ResolvingDataSource`).
3. **Third-Party Attribution File:**
   - Maintain an explicit `open_source_licenses.md` or in-app About Screen acknowledging:
     - Metrolist Group (Metrolist & InnerTubeX, GPL-3.0)
     - Fabrice Bellard and Charlie Gordon (QuickJS, MIT)
     - Meriyah / Astring contributors (MIT)
     - Google LLC (AndroidX Media3, Jetpack Compose, Apache-2.0)
     - LRCLIB (Community open API)
