# Release & Publishing Procedures

This guide provides the exact operational procedure for building, signing, tagging, and distributing new versions of MIORA.

---

## 📋 Versioning Scheme

MIORA uses Semantic Versioning 2.0.0 (`MAJOR.MINOR.PATCH[-PRERELEASE]`):
- **Development / Alpha**: `v0.1.0-alpha.1`
- **Beta / Public Preview**: `v0.1.0-beta.1`
- **Release Candidates**: `v0.1.0-rc.1`
- **Stable Releases**: `v0.1.0`, `v1.0.0`

In `app/build.gradle.kts`:
- Increment `versionCode` by 1 for every public build.
- Update `versionName` to match the intended release tag.
- Update `CURRENT_VERSION_NAME` and `CURRENT_VERSION_CODE` in `UpdateManager.kt`.

---

## 🔑 Signing & Keystore Management

### Production Signing Configuration
To sign official release binaries without committing private keys:

Set the following environment variables or local Gradle properties:
```bash
export MIORA_KEYSTORE_PATH="/path/to/secure/release.keystore"
export MIORA_KEYSTORE_PASSWORD="<store-password>"
export MIORA_KEY_ALIAS="miora-release"
export MIORA_KEY_PASSWORD="<key-password>"
```

### Reproducible Fallback (Beta / Local Builds)
If `MIORA_KEYSTORE_PATH` is unset or points to a non-existent file, Gradle automatically falls back to `~/.android/debug.keystore`:
- **Store Alias**: `androiddebugkey`
- **Store Password**: `android`
- **Certificate SHA-256 Fingerprint**: `1B:2D:98:17:73:4C:E3:96:C4:6A:81:02:57:5F:72:AC:42:27:C5:F8:14:BD:BE:2C:49:F1:F9:80:9E:18:CC:A0`

### Debug-to-Release Migration Notice
When transitioning from the initial public beta builds (signed with the reproducible distribution certificate) to a production production-only release key:
- Android requires packages with identical `applicationId` to be signed with the same key for in-place updates.
- If migrating to a new custom private key, users must either:
  1. Uninstall the beta version before installing the production release, or
  2. The release key must be enrolled into Android APK Signature Scheme v3/v3.1 key rotation.

---

## 🛠️ Step-by-Step Release Workflow

### Step 1: Update Version & Run Tests
1. Update `app/build.gradle.kts` (`versionCode` & `versionName`).
2. Update `UpdateManager.kt` (`CURRENT_VERSION_NAME` & `CURRENT_VERSION_CODE`).
3. Run all automated unit tests:
   ```bash
   timeout 5m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 testDebugUnitTest --console=plain
   ```

### Step 2: Assemble Official APK
Build the release artifact:
```bash
timeout 5m gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 --offline --init-script /root/.gradle/init.d/pocketdev-android.gradle -Pandroid.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2 assembleDebug --console=plain --stacktrace
```

### Step 3: Package & Generate Checksum
```bash
VERSION="0.1.0-beta.1"
cp app/build/outputs/apk/debug/app-debug.apk "MIORA-v${VERSION}.apk"
sha256sum "MIORA-v${VERSION}.apk" > "MIORA-v${VERSION}.apk.sha256"
```

### Step 4: Verify Signature with Apksigner
```bash
/root/android-sdk/build-tools/35.0.0/apksigner verify --verbose --print-certs "MIORA-v${VERSION}.apk"
```

### Step 5: Git Tag & Commit
```bash
git add .
git commit -m "Release v${VERSION}"
git tag -a "v${VERSION}" -m "MIORA Release v${VERSION}"
git push origin main
git push origin "v${VERSION}"
```

### Step 6: Publish via GitHub CLI
```bash
gh release create "v${VERSION}" "MIORA-v${VERSION}.apk" "MIORA-v${VERSION}.apk.sha256" \
  --title "MIORA v${VERSION} — Initial Public Beta" \
  --notes-file "CHANGELOG.md" \
  --prerelease
```

---

## 🤖 Automated CI/CD Release
Pushing any git tag formatted as `v*` (e.g., `git push origin v0.1.0-beta.1`) triggers the automated GitHub Actions workflow defined in [`.github/workflows/release.yml`](../.github/workflows/release.yml). The pipeline automatically executes tests, builds the APK, verifies checksums, and publishes the release assets to GitHub.
