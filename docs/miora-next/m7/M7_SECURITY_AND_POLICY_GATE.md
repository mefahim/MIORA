# MIORA NEXT — M7 Security, Intellectual Property & Policy Gate
## Cryptographic Standards, DRM Compliance, Licensing Realities & Distribution Posture

- **Document Version:** 1.0.0
- **Gate:** Pre-Implementation Security & Legal Gate (Milestone M7)
- **Target Branch:** `docs/miora-next-audit-roadmap`

---

## 1. Technical Protection Measures (TPMs) & DRM Boundaries

### 1.1 Prohibition of Circumvention Technologies
Under 17 U.S.C. § 1201 (Digital Millennium Copyright Act) and international WIPO copyright treaties:
> *"No person shall circumvent a technological measure that effectively controls access to a work protected under this title."*

YouTube employs two distinct technological measures on audio streams:
1. **Signature Cipher Scrambling & Throttling (`s` and `n` parameters):** Scrambles stream URLs to prevent automated scraping and forces bandwidth throttling down to 40 kbps unless deciphered by dynamic JavaScript functions.
2. **BotGuard / Proof of Origin (`poToken`):** An encrypted virtual machine challenge verifying that the playback client is an official, Google-approved browser or mobile app.

### 1.2 MIORA Next Policy Decision
- **Prohibited:** MIORA Next will **NOT** bundle native AST deciphering engines (`libquickjs.so`, `meriyah.js`, `astring.js`) or headless `WebView` BotGuard VM runners (`po_token.html`) designed to evade Google's bot detection and cipher protection.
- **Rationale:** Embedding circumvention binaries in MIORA exposes the project, contributors, and distribution channels to DMCA anti-circumvention notices, GitHub repository takedowns, and Google Play developer terminations.
- **Compliant Alternative:** MIORA relies strictly on authorized APIs (YouTube Data API v3, Subsonic open REST API, Open Audio repositories) and standard HTTP/HTTPS protocols.

---

## 2. Authentication Architecture & Credential Hardening

### 2.1 Formal Rejection of Session Cookie Sniffing (The Metrolist Anti-Pattern)
Our forensic reverse-engineering of Metrolist 13.7.0 identified that it intercepts master session cookies (`SAPISID`, `__Secure-3PAPISID`, `HSID`, `SSID`) through an in-app `WebView` directed at Google accounts, calculates `SAPISIDHASH` with SHA-1, and stores these raw credentials unencrypted in Android `DataStore` and plaintext JSON backup files.

**Security Failure Mode:**
- The `SAPISID` cookie grants unrestricted administrative access to the user's entire Google account (Gmail, Google Drive, YouTube, Google Photos, Cloud Platform).
- Any application with external storage read permission or any user sharing an exported backup file unwittingly surrenders their entire digital identity.
- This pattern violates Google OAuth policies and Android Secure Storage best practices.

### 2.2 MIORA Next Cryptographic Standard: RFC 7636 OAuth 2.0 PKCE + Android Keystore
MIORA Next strictly mandates hardware-backed cryptographic security:

```
+-----------------------------------------------------------------------------------+
|                           RFC 7636 OAuth 2.0 PKCE Flow                             |
+-----------------------------------------------------------------------------------+
| 1. App generates 128-byte cryptographic code_verifier                             |
| 2. Computes code_challenge = Base64Url(SHA-256(code_verifier))                     |
| 3. Launches Chrome Custom Tab: https://accounts.google.com/o/oauth2/v2/auth       |
|    - Scopes: https://www.googleapis.com/auth/youtube.readonly                     |
| 4. Handles redirect intent: com.fhf.miora://oauth2callback                        |
| 5. Exchanges auth_code + code_verifier for Access Token & Refresh Token           |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                   Android Keystore AES-256-GCM Hardware Encryption                |
+-----------------------------------------------------------------------------------+
| 1. Master Key generated in Android KeyStore (KeyProperties.PURPOSE_ENCRYPT/DECRYPT)|
| 2. Master Key is backed by hardware Secure Element / StrongBox where available    |
| 3. Tokens encrypted with AES-256-GCM (128-bit authentication tag, 12-byte IV)     |
| 4. Ciphertext persisted in SQLite miora_database; Master Key never leaves hardware |
| 5. Manifest excludes token database from Android Cloud Auto-Backup                |
+-----------------------------------------------------------------------------------+
```

---

## 3. Intellectual Property, Licensing & Contagion Audit

### 3.1 Copyright Ownership of the MIORA Baseline
- **MIORA v0.1.0 Baseline:** Created and published under the **Apache License 2.0** by the original copyright holder (`mefahim`, repository `mefahim/MIORA`).
- **Milestone M0 Commit `d66cfc5`:** Modified repository headers and root `LICENSE` file to **GNU General Public License v3.0 (GPL-3.0)**.
- **Legal Reality:** Under copyright law, only the copyright owner (or authors with written copyright assignment) may legally relicense an original work. In this project, the repository owner is the original author; hence, transitioning the project from Apache-2.0 to GPL-3.0 for the combined hybrid release is legally valid provided contributor notices are preserved.

### 3.2 Third-Party Copyleft Obligations (InnerTubeX)
- If MIORA were to link directly against `com.github.MetrolistGroup.innertubex:innertubex`, GPL-3.0 Section 5 would legally compel the entire combined binary to be licensed under GPL-3.0.
- By adhering to our **Authorized Provider Contract** (`OnlineMediaProvider`), MIORA interacts with online sources via clean-room Kotlin interfaces, ensuring no proprietary or unvetted copyleft code infects the core audiophile local music player.

---

## 4. Multi-Channel Distribution Posture

| Distribution Channel | Policy Constraints | Permitted Streaming Capabilities | Required Posture |
| :--- | :--- | :--- | :--- |
| **Google Play Store** | Zero tolerance for YouTube backgrounding or downloading. Policy 4.4 strictly enforced. | Local MediaStore audio, Subsonic streaming, Open Audio, YouTube Data API v3 metadata search & playlist sync, External Intent Handoff. | **Strictly Compliant Build.** Must NOT include direct YouTube stream extractors. |
| **F-Droid / Obtainium** | Must be 100% Free & Open Source (FOSS). Requires reproducible build without proprietary tracking SDKs. | Local playback, Subsonic streaming, Open Audio, User-configured Piped/Invidious proxies, Synced LRCLIB lyrics. | **Recommended Primary Distribution.** Full open-source capabilities enabled. |
| **GitHub Releases** | Allows direct APK downloads. Must comply with DMCA terms and GitHub Acceptable Use Policies. | Local playback, Subsonic streaming, Open Audio, LRCLIB lyrics, Optional community proxy resolvers. | **Full Release Channel.** Release signed universal and split APKs directly on GitHub. |

---

## 5. Summary of Unresolved Decisions Requiring Owner Sign-Off

The following strategic questions require explicit owner determination before M7 code authoring begins:

1. **Owner Decision 1: Streaming Scope for YouTube Content**
   - *Option A (Recommended / 100% Compliant):* Keep YouTube content strictly as Discovery & Playlist Sync via Data API v3 (Provider A), with audio routed via `PlaybackRouter.kt` external intent, while implementing full in-app audio streaming via Subsonic/Navidrome (Provider E) and Open Audio (Provider F).
   - *Option B (F-Droid / Proxy Only):* Support in-app streaming of YouTube audio solely through user-configured Piped / Invidious proxy instances (Provider D), avoiding embedded BotGuard/QuickJS circumvention code.
   - *Option C (Metrolist InnerTube Clone):* Embed reverse-engineered InnerTube cipher solvers and BotGuard WebViews directly into MIORA. *(Engineering warning: High legal risk, frequent breakage, and prohibited from Google Play).*

2. **Owner Decision 2: Target Distribution Priority**
   - Is Google Play Store publishing a mandatory requirement for MIORA Next, or is GitHub Releases + F-Droid the primary intended distribution vehicle?
