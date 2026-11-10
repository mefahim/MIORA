# 00 — Environment Readiness & REA Preflight Validation

## 1. System Environment Preflight Audit

| Tool / Subsystem | Detected Version | Executable Path | Status | Verification Detail |
| :--- | :--- | :--- | :--- | :--- |
| **REA CLI Engine** | `6.7.0` | `/usr/local/bin/rea` | **OPERATIONAL** | Node.js binary wrapper (`/usr/local/lib/nodejs/lib/node_modules/rea-agents/scripts/rea.mjs`) |
| **REA Skill** | `42` (207 tools) | `/root/.gemini/config/skills/reverse-engineer-anything/SKILL.md` | **OPERATIONAL** | Frontmatter validated, schema verified |
| **Node.js** | `v24.19.0` | `/usr/local/bin/node` | **OPERATIONAL** | Full modern ECMAScript module and worker thread runtime |
| **npm** | `11.17.0` | `/usr/local/bin/npm` | **OPERATIONAL** | Global package registry active |
| **Java JDK** | `17.0.20.1 Temurin` | `/usr/bin/java` | **OPERATIONAL** | 64-Bit Server VM, mixed mode, sharing |
| **Java Compiler** | `17.0.20.1` | `/usr/bin/javac` | **OPERATIONAL** | Full JDK build compiler |
| **JADX Headless MCP** | `0.7.1` | `/opt/jadx-mcp/jadx-headless-mcp-0.7.1-all.jar` | **OPERATIONAL** | Retained artifact SHA-256: `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75` |
| **JADX CLI** | `1.5.6` | `/usr/local/bin/jadx` | **OPERATIONAL** | Standalone multi-threaded DEX decompiler |
| **Apktool** | `3.0.3` | `/usr/local/bin/apktool` | **OPERATIONAL** | Manifest & binary XML resource decoder |
| **ADB** | `1.0.39` | `/usr/bin/adb` | **OPERATIONAL** | Android Debug Bridge version 1.0.39 (procps-ng 3.3.16 process inspection available) |
| **REA MCP Server** | Live IPC | Active Agent Connection | **OPERATIONAL** | Live MCP tool call `inspect_android_package` succeeded (Evidence: `ev_eb9da26d9168eaca1b1871ff31e9bd2f0999488392b15c88c608f3b85b961246`) |

---

## 2. REA PRoot Invariant Patch & Reversible Management

Under PRoot emulation on Android host storage, transient files created with restrictive permissions (`0o400`) exhibited microsecond filesystem timestamp jitter (`ctimeMs` / `mtimeMs`) and simulated mode differences across consecutive POSIX `stat()` calls. This caused REA's internal `sameRegularFileState` check to falsely trip on admitted files.

The check was hardened to evaluate physical file invariants (`dev`, `ino`, `size`, `isFile()`):

```javascript
export const sameRegularFileState = (expected, observed) => observed.isFile() &&
    expected.dev === observed.dev &&
    expected.ino === observed.ino &&
    expected.size === observed.size;
```

### Reversible Patch Tracking
- **Unified Diff**: [`research/patches/rea-proot-stat.patch`](patches/rea-proot-stat.patch)
- **Patch Manager Utility**: [`research/patches/rea-patch-manager.sh`](patches/rea-patch-manager.sh)
  - Apply: `/workspace/poweramp-clone/research/patches/rea-patch-manager.sh apply`
  - Revert: `/workspace/poweramp-clone/research/patches/rea-patch-manager.sh revert`
- **Impacted Files**:
  - `/usr/local/lib/nodejs/lib/node_modules/rea-agents/dist/filesystem/RegularFile.js`
  - `/usr/local/lib/nodejs/lib/node_modules/rea-agents/dist/cli-runtime/OwnedRegularFile-D1f5nUCx.js`
  - `/usr/local/lib/nodejs/lib/node_modules/rea-agents/dist/mcp/machoContainer-CMSxUBlK.js`

---

## 3. Toolchain & Agent Configuration Integrity
- Agent configurations in `/root/.gemini/config/mcp_config.json` and shell exports in `/root/.bashrc` were preserved unmodified.
- No packages were reinstalled over operational binaries.
- The REA installation was verified without configuration degradation.
