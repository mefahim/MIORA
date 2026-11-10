# Security Policy & Vulnerability Reporting

## 🛡️ Supported Versions

Only the latest release and active beta channels receive security updates and bug fixes:

| Version | Supported | Notes |
| :--- | :--- | :--- |
| `0.1.0-beta.1` | ✅ Yes | Current Public Beta |
| `< 0.1.0` | ❌ No | Deprecated development prototypes |

---

## 🔒 Security Principles

MIORA is designed with security and privacy as first-class requirements:

1. **Zero Tracking & Telemetry**: MIORA does not contain any analytic libraries, tracking beacons, ads, or third-party user monitoring scripts.
2. **Local-First Audio Playback**: Your audio library is read directly from your local device filesystem and Android `MediaStore`. No media content or playback history is transmitted to any remote servers.
3. **Transparent Network Activity**: The only outbound network requests made by MIORA are to the official GitHub API (`api.github.com`) solely to check for published app updates when requested by the user.
4. **Cryptographic Release Verification**: All in-app updates verify cryptographic SHA-256 signatures before package installation to prevent tampering, corruption, or Man-in-the-Middle (MitM) attacks.
5. **Secure Package Permissions**: File sharing between the app and the system package installer uses Android `FileProvider` with scoped permissions (`FLAG_GRANT_READ_URI_PERMISSION`) rather than exposed world-readable files.

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability or security bug in MIORA:

1. **Do not open a public GitHub Issue.**
2. Send an advisory via GitHub Security Advisories or email the maintainer directly at: `mefahim21@gmail.com`.
3. Include:
   - Detailed description of the vulnerability.
   - Steps or proof of concept to reproduce.
   - Potential impact on the device or application.
4. Maintainers will acknowledge receipt within 48 hours and work with you to publish a fix before public disclosure.
