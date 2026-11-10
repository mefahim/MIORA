# GitHub Actions CI/CD Release Automation

This document provides the complete GitHub Actions release workflow specification for **MIORA**.

---

## ⚙️ Workflow Definition (`.github/workflows/release.yml`)

When your GitHub authentication token has the `workflow` scope enabled (`gh auth refresh -s workflow`), this workflow automates tests, build creation, SHA-256 checksum generation, and release publication on every version tag push (`v*`).

```yaml
name: Release MIORA

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  build-and-release:
    name: Build & Release Android APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --no-daemon --stacktrace

      - name: Build Android APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Package & Generate Checksums
        id: package
        run: |
          TAG_NAME="${GITHUB_REF#refs/tags/}"
          APK_NAME="MIORA-${TAG_NAME}.apk"
          SHA_NAME="${APK_NAME}.sha256"
          cp app/build/outputs/apk/debug/app-debug.apk "${APK_NAME}"
          sha256sum "${APK_NAME}" > "${SHA_NAME}"
          echo "tag_name=${TAG_NAME}" >> $GITHUB_OUTPUT
          echo "apk_name=${APK_NAME}" >> $GITHUB_OUTPUT
          echo "sha_name=${SHA_NAME}" >> $GITHUB_OUTPUT

      - name: Publish GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          name: "MIORA ${{ steps.package.outputs.tag_name }}"
          files: |
            ${{ steps.package.outputs.apk_name }}
            ${{ steps.package.outputs.sha_name }}
          draft: false
          prerelease: ${{ contains(steps.package.outputs.tag_name, 'beta') || contains(steps.package.outputs.tag_name, 'alpha') || contains(steps.package.outputs.tag_name, 'rc') }}
          generate_release_notes: true
```

---

## 🔑 GitHub OAuth Scope Note

If pushing to `.github/workflows/` is rejected with:
`refusing to allow an OAuth App to create or update workflow .github/workflows/release.yml without 'workflow' scope`

Run:
```bash
gh auth refresh -s workflow
```
Then commit and push `.github/workflows/release.yml` directly.
